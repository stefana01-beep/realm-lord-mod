#!/usr/bin/env python3
"""Builds the China world map for Realm Lord (format 2).

The world is centred on Luoyang (block 0,0) at 50 m per block, so China is about 100,000
blocks across. The land is real: heights come from NOAA's ETOPO2 elevation grid (2 arc-minutes,
public domain), squeezed to fit a world whose top is y 576 (the plains sit a few blocks above
the sea, the Tibetan Plateau near y 370, the highest peaks above y 440). Rivers are traced from
the same heights, so they run down the real valleys. The coastline is Natural Earth 1:50m land.

One pixel per 64 blocks (3.2 km). Channels: surface height, continentalness, erosion,
temperature, humidity, relief, distance to the nearest river and that river's size.

Usage: python3 tools/gen_china.py <land-50m.json> <ETOPO2v2g_f4_LSB.flt> [preview.png]
(Natural Earth 10m rivers and lakes are read from ../../ne next to the land file.)
"""
import gzip
import heapq
import json
import math
import os
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
from scipy.spatial import cKDTree

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "realmlord", "china", "map.bin.gz")

ORIGIN_LON, ORIGIN_LAT = 112.45, 34.62          # Luoyang
M_PER_BLOCK = 50.0
KM_LON = 111.32 * math.cos(math.radians(35))    # km per degree of longitude
KM_LAT = 111.32
PX = 64                                          # blocks per pixel
LON0, LON1, LAT0, LAT1 = 67.8, 140.2, 13.8, 56.0  # the area mapped (west, east, south, north)
X0 = int(math.floor((LON0 - ORIGIN_LON) * KM_LON * 1000 / M_PER_BLOCK / PX)) * PX
X1 = int(math.ceil((LON1 - ORIGIN_LON) * KM_LON * 1000 / M_PER_BLOCK / PX)) * PX
Z0 = int(math.floor(-(LAT1 - ORIGIN_LAT) * KM_LAT * 1000 / M_PER_BLOCK / PX)) * PX
Z1 = int(math.ceil(-(LAT0 - ORIGIN_LAT) * KM_LAT * 1000 / M_PER_BLOCK / PX)) * PX
W, H = (X1 - X0) // PX, (Z1 - Z0) // PX
KM_PX = PX * M_PER_BLOCK / 1000.0
SEA_Y = 63
TOP_Y = 576


def to_block(lon, lat):
    return (lon - ORIGIN_LON) * KM_LON * 1000 / M_PER_BLOCK, -(lat - ORIGIN_LAT) * KM_LAT * 1000 / M_PER_BLOCK


def to_px(lon, lat):
    x, z = to_block(lon, lat)
    return (x - X0) / PX, (z - Z0) / PX


def height_y(elev_m):
    """Real height (m) to the block the surface stands at. Lowlands keep their shape (about one
    block per 9 m near the sea); high ground is squeezed so the Himalaya fit under y 576."""
    land = 64.0 + 440.0 * (1 - np.exp(-np.maximum(elev_m, 0) / 3800.0))
    sea = np.minimum(58.0, SEA_Y + np.maximum(-50.0, elev_m / 30.0))   # at least 5 blocks of water
    return np.where(elev_m > 0, land, sea)


# pixel centre -> lon/lat grids
xs = X0 + (np.arange(W) + 0.5) * PX
zs = Z0 + (np.arange(H) + 0.5) * PX
LON = ORIGIN_LON + xs[None, :] * M_PER_BLOCK / 1000 / KM_LON + np.zeros((H, 1))
LAT = ORIGIN_LAT - zs[:, None] * M_PER_BLOCK / 1000 / KM_LAT + np.zeros((1, W))


def decode_topojson(path):
    tj = json.load(open(path))
    sc, tr = tj["transform"]["scale"], tj["transform"]["translate"]
    arcs = []
    for arc in tj["arcs"]:
        x = y = 0
        pts = []
        for dx, dy in arc:
            x += dx
            y += dy
            pts.append((x * sc[0] + tr[0], y * sc[1] + tr[1]))
        arcs.append(pts)

    def ring(idx):
        out = []
        for a in idx:
            pts = arcs[a] if a >= 0 else list(reversed(arcs[~a]))
            out.extend(pts if not out else pts[1:])
        return out

    polys = []
    for obj in tj["objects"].values():
        geoms = obj["geometries"] if obj["type"] == "GeometryCollection" else [obj]
        for g in geoms:
            if g["type"] == "Polygon":
                polys.append([ring(r) for r in g["arcs"]])
            elif g["type"] == "MultiPolygon":
                for p in g["arcs"]:
                    polys.append([ring(r) for r in p])
    return polys


def land_mask(path):
    S = 4
    img = Image.new("L", (W * S, H * S), 0)
    d = ImageDraw.Draw(img)
    for poly in decode_topojson(path):
        for i, r in enumerate(poly):
            pts = []
            for lon, lat in r:
                if lon < 40 or lon > 180 or lat < -5 or lat > 75:
                    pts.append(None)
                    continue
                px, py = to_px(lon, lat)
                pts.append((px * S, py * S))
            pts = [p for p in pts if p is not None]
            if len(pts) >= 3:
                d.polygon(pts, fill=255 if i == 0 else 0)
    small = img.resize((W, H), Image.BILINEAR)
    return np.asarray(small, dtype=np.float32) / 255.0


# ------------------------------------------------------------------ features

def poly_mask(points, blur=2.0):
    img = Image.new("L", (W, H), 0)
    ImageDraw.Draw(img).polygon([to_px(lo, la) for lo, la in points], fill=255)
    m = np.asarray(img, dtype=np.float32) / 255.0
    return np.clip(ndimage.gaussian_filter(m, blur), 0, 1)


def line_field(points, width_km, samples_per_px=2):
    """exp(-(d/width)^2) distance falloff from a polyline."""
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    pts = [to_px(lo, la) for lo, la in points]
    d.line(pts, fill=0, width=1)
    dist = ndimage.distance_transform_edt(np.asarray(img) > 0) * KM_PX
    return np.exp(-(dist / width_km) ** 2)


def blend(base, target, w):
    return base * (1 - w) + target * w


MOUNTAINS = [  # (name, polyline lon/lat, strength, half-width km)
    ("Himalaya", [(73.5, 35.5), (75, 34.8), (77, 33.2), (78.5, 32.2), (81, 30.2), (84, 28.6), (86.9, 28.0), (89, 27.8), (92, 27.8), (95, 28.6), (96.8, 29.2)], 1.0, 110),
    ("Karakoram", [(74, 36.5), (76.5, 35.8), (78, 35.2)], 1.0, 80),
    ("Kunlun", [(75, 37.2), (78, 36.3), (82, 36.0), (86, 36.3), (90, 36.4), (93, 36.1), (96, 35.7), (99, 35.3)], 0.85, 90),
    ("Tianshan", [(70, 41.5), (74, 41.5), (77, 42), (80, 42.3), (83, 43), (86, 43.5), (89, 43.3), (92, 43), (95, 42.8)], 0.85, 90),
    ("Altai", [(84.5, 49.5), (87, 48.5), (89.5, 47.3), (91.5, 46.2), (94, 45.5)], 0.6, 70),
    ("Qilian", [(94, 39.3), (96.5, 38.8), (99, 38.3), (101.5, 37.6), (103, 37)], 0.7, 70),
    ("Hengduan", [(98, 32.5), (98.8, 30), (99.2, 28), (99.6, 26), (100.2, 24)], 0.75, 80),
    ("Qinling", [(104, 34.1), (106, 33.9), (108, 33.9), (110, 33.8), (112, 33.6)], 0.55, 45),
    ("Daba", [(106, 32.3), (108, 31.9), (110, 31.4)], 0.4, 40),
    ("Taihang", [(114.2, 40.3), (113.9, 38.5), (113.6, 36.8), (113, 35.3)], 0.45, 40),
    ("Yinshan", [(106.5, 41), (109, 41), (111.5, 41.2), (114, 41.5)], 0.4, 40),
    ("Greater Khingan", [(121.5, 53), (121.5, 50.5), (120.5, 48), (119.5, 45.5), (118, 43)], 0.45, 60),
    ("Lesser Khingan", [(126.5, 50), (128.5, 48.5), (129.5, 47.2)], 0.35, 50),
    ("Changbai", [(126.5, 44), (127.8, 42.6), (128.5, 41.8)], 0.5, 50),
    ("Nanling", [(109.5, 25.2), (111.5, 25.2), (113, 25.1), (115, 24.8)], 0.4, 50),
    ("Wuyi", [(115.5, 25), (116.8, 26.5), (117.8, 27.6), (118.6, 28.6)], 0.4, 50),
    ("Taiwan", [(120.8, 22.2), (121.1, 23.5), (121.4, 24.6)], 0.6, 25),
    ("Hindu Kush", [(68, 35), (70.5, 35.6), (73, 36.2)], 0.8, 70),
    # Inner ranges of Tibet and the north-west
    ("Gangdise", [(80.5, 31.3), (83, 30.8), (86, 30.4), (89.5, 30.3), (92, 30.1)], 0.6, 70),
    ("Tanggula", [(89, 33.0), (91.5, 32.8), (94, 32.6)], 0.5, 60),
    ("Helan", [(105.6, 39.3), (105.9, 38.6), (106.1, 38.0)], 0.4, 25),
    ("Luliang", [(111.2, 39.0), (111.3, 37.8), (111.0, 36.4)], 0.35, 30),
    # Central and southern China
    ("Dabie", [(113.8, 31.6), (115.2, 31.2), (116.6, 30.8)], 0.35, 35),
    ("Wuling", [(108.6, 29.6), (109.6, 28.8), (110.6, 28.2)], 0.35, 35),
    ("Xuefeng", [(110.2, 28.6), (110.6, 27.4), (110.9, 26.2)], 0.35, 30),
    ("Wumeng", [(103.6, 27.6), (104.2, 26.6), (104.8, 25.8)], 0.4, 35),
    ("Ailao", [(100.6, 25.2), (101.6, 24.0), (102.8, 22.8)], 0.45, 35),
    ("Dayao", [(109.8, 24.4), (110.2, 23.8)], 0.3, 25),
    # The ring round the Lianzhou Basin, where Shanhai Village stands in the novel
    ("Shiwan Dashan", [(107.3, 21.7), (107.8, 21.85), (108.3, 22.05), (108.7, 22.3)], 0.45, 30),
    ("Liuwan Dashan", [(109.3, 22.15), (109.6, 22.4), (109.9, 22.65)], 0.35, 22),
    ("Yunkai", [(110.4, 21.9), (110.9, 22.4), (111.4, 22.9), (111.8, 23.3)], 0.35, 30),
    ("Wuzhi (Five Finger)", [(109.2, 18.8), (109.6, 18.9), (109.9, 19.0)], 0.4, 20),
]

RIVERS = [  # (name, polyline, strength at the mouth)
    ("Yellow River", [(96, 34.9), (98.5, 34.6), (100.5, 35.6), (102.5, 35.9), (103.8, 36.1), (105.2, 37.4), (106.3, 38.5), (106.8, 39.8),
                      (108, 40.7), (110, 40.6), (111.2, 40.1), (111, 38.8), (110.6, 37.2), (110.4, 35.6), (110.3, 34.6), (111.3, 34.8),
                      (112.5, 34.85), (113.7, 34.9), (115, 35.2), (116.4, 36.1), (117, 36.7), (118.2, 37.4), (119.1, 37.8)], 1.0),
    ("Yangtze", [(91, 33.6), (94, 33.2), (96.5, 32.5), (98, 31), (99.2, 29), (99.7, 27.3), (100.3, 26.8), (101.6, 26.3), (103, 26.4),
                 (104.6, 28.8), (105.5, 29), (106.5, 29.6), (108, 30.5), (110, 31), (111.3, 30.7), (112.4, 30.2), (113.2, 29.5),
                 (114.3, 30.6), (115.5, 29.8), (116, 29.75), (117, 30.5), (118.4, 31.3), (118.8, 32.05), (119.5, 32.2), (120.5, 31.9),
                 (121.8, 31.35)], 1.0),
    ("Pearl River", [(103.8, 25.2), (106, 24.8), (108.3, 23.4), (109.5, 23.4), (111.3, 23.5), (112.5, 23.1), (113.5, 22.4)], 0.8),
    ("Amur", [(121, 53.3), (124, 53.2), (126.5, 51.5), (127.5, 49.8), (130, 48.8), (132.5, 47.7), (134.8, 48.3), (137, 50.5), (140.5, 52.9)], 0.8),
    ("Huai", [(113.5, 32.4), (116, 32.6), (117.5, 33), (119, 33.3), (120.5, 33.7)], 0.55),
    ("Han", [(106.5, 33.1), (108.5, 32.7), (110.5, 32.6), (112, 32), (113.5, 30.8), (114.3, 30.6)], 0.55),
    ("Wei", [(104.5, 34.6), (106.8, 34.4), (108.9, 34.4), (110.2, 34.6)], 0.5),
    ("Songhua", [(126.6, 42.5), (126.5, 45.5), (126.6, 45.8), (129.5, 46.5), (132.5, 47.7)], 0.55),
    ("Min", [(103.6, 32.5), (103.8, 31), (104, 30), (104.6, 28.8)], 0.45),
    ("Jialing", [(106.5, 33.5), (106, 31.8), (106.5, 29.6)], 0.4),
    ("Liao", [(118.5, 42.5), (122, 42.5), (122.8, 41.2), (122.2, 40.7)], 0.45),
    ("Tarim", [(80, 40.6), (83, 41.1), (86, 41), (88, 40.2)], 0.4),
    ("Xiang", [(110.6, 25.6), (111.6, 26.4), (112.6, 27.8), (112.9, 29.3)], 0.4),
    ("Gan", [(115.2, 25.9), (115.0, 27.2), (115.8, 28.6), (116.2, 29.2)], 0.4),
    ("Yongjiang", [(107.0, 22.6), (108.0, 22.8), (108.3, 22.85), (109.0, 23.2), (109.5, 23.4)], 0.3),
    ("Nanliu", [(110.0, 22.7), (109.7, 22.3), (109.4, 21.9), (109.15, 21.65)], 0.3),
    ("Qin", [(108.4, 22.4), (108.6, 22.1), (108.6, 21.85)], 0.2),
]

LAKES = [(100.2, 36.9, 45), (116.3, 29.0, 40), (112.9, 29.3, 30), (120.2, 31.2, 25), (102.7, 24.85, 12), (117.6, 31.5, 15), (87.5, 41.9, 20)]

REGIONS = {  # name: (polygon, {channel: target}, blur)
    "tibet": ([(78, 35.5), (80, 36.4), (84, 36.0), (90, 36.2), (94, 35.8), (98, 36.0), (101, 35.3), (102.5, 33.5), (102, 30.5),
               (100.5, 28), (97.5, 28), (95, 28.7), (92, 28.1), (88, 28.1), (85, 28.8), (82, 30.3), (79.2, 31.5), (78, 33.5)],
              {"cont": 0.95, "ero": -0.42, "temp": -0.75, "veg": -0.25, "lift": 95}, 3.0),
    "yungui": ([(100, 23), (104, 22.8), (108, 24.5), (109.3, 27), (107, 28.6), (103.5, 28.6), (100.6, 27)],
               {"cont": 0.55, "ero": -0.05, "veg": 0.45, "lift": 35}, 2.5),
    "loess": ([(101.5, 36), (104, 37.6), (108, 38.6), (111, 39.4), (112.8, 38), (111.6, 35.6), (108, 34.6), (104, 35)],
              {"cont": 0.55, "ero": -0.1, "veg": -0.35, "lift": 25}, 2.5),
    "inner_mongolia": ([(97, 42), (105, 42.6), (111, 43.2), (116, 44.3), (120, 45.5), (117.5, 42.3), (111, 40.9), (105, 41.2)],
                       {"cont": 0.6, "ero": 0.6, "veg": -0.5, "lift": 20}, 3.0),
    "gobi": ([(93, 43.8), (100, 44.5), (106, 45), (112, 45.5), (112, 42.5), (104, 41.5), (97, 40.8), (93, 41.5)],
             {"cont": 0.55, "ero": 0.5, "temp": 0.6, "veg": -0.95, "lift": 18}, 3.0),
    "tarim": ([(76, 39.5), (78, 41), (82, 41.6), (86, 41.6), (89, 40.6), (89.5, 38.3), (84, 37.2), (79, 37.6)],
              {"cont": 0.35, "ero": 0.7, "temp": 0.65, "veg": -1.0}, 2.0),
    "junggar": ([(82.5, 45), (85, 46.6), (89, 46.2), (90.5, 44.6), (87, 44.2), (84, 44.5)],
                {"cont": 0.3, "ero": 0.6, "temp": 0.45, "veg": -0.7}, 2.0),
    "qaidam": ([(90, 37.5), (93, 38.4), (97, 37.6), (96.2, 36.4), (92, 36.6)],
               {"cont": 0.85, "ero": 0.55, "temp": 0.3, "veg": -0.9, "lift": 70}, 1.5),
    "sichuan": ([(103.5, 30.5), (104.5, 32.1), (106.5, 32.2), (108.5, 30.6), (107, 28.8), (104.5, 28.8)],
                {"cont": 0.2, "ero": 0.45, "temp": 0.4, "veg": 0.5}, 2.0),
    "north_china_plain": ([(113.6, 39.6), (117, 40.2), (118.6, 39.3), (118.2, 38), (117.6, 36.6), (116.6, 34), (117.6, 32.6),
                           (115, 32), (113.2, 33.5), (113.6, 36)], {"cont": 0.04, "ero": 0.85, "veg": 0.0}, 2.0),
    "guanzhong": ([(106.8, 34.6), (108.5, 34.8), (110.2, 34.8), (110, 34.2), (107, 34.1)], {"cont": 0.15, "ero": 0.8}, 1.0),
    "northeast_plain": ([(122, 47.6), (126, 48.1), (128.5, 46.6), (126.6, 43.4), (123.5, 41.6), (121.6, 43.6)],
                        {"cont": 0.1, "ero": 0.8, "veg": 0.1}, 2.5),
    "yangtze_plain": ([(111, 30.6), (114.5, 31.6), (118, 32.6), (121.6, 32.1), (121.1, 30.7), (117, 29.4), (113, 28.9)],
                      {"cont": 0.03, "ero": 0.85, "veg": 0.5}, 2.0),
    "shandong_hills": ([(116.6, 36.6), (118, 37.3), (120.5, 37.6), (122.5, 37.4), (120.5, 36), (118.5, 35.2), (117, 35.6)],
                       {"cont": 0.15, "ero": 0.05}, 1.5),
    "southeast_hills": ([(108, 30), (113, 29), (117, 29.4), (121, 30.4), (121, 28), (119.5, 25.5), (116.5, 23), (112, 22),
                         (109, 21.8), (106.5, 23), (108, 26.5)], {"ero": 0.0, "veg": 0.55}, 3.0),
    "lianzhou_basin": ([(108.6, 21.75), (108.9, 22.25), (109.4, 22.35), (109.8, 22.2), (109.6, 21.65), (109.0, 21.6)],
                       {"cont": 0.04, "ero": 0.8, "veg": 0.45}, 1.0),
    "pearl_delta": ([(112.6, 22.4), (113.2, 23.4), (114.1, 23.2), (114, 22.4)], {"cont": 0.02, "ero": 0.85}, 1.0),
    "indo_gangetic": ([(72, 30), (78, 29.5), (84, 27.3), (89, 26.2), (90, 23), (84, 23), (77, 25), (71, 27)],
                      {"cont": 0.15, "ero": 0.8, "veg": 0.4, "temp": 0.8}, 3.0),
}

CAPITALS = [  # the novel's nine Imperial Capitals
    ("Jingdu", 116.40, 39.90), ("Xianyang", 108.71, 34.33), ("Chengdu", 104.07, 30.66), ("Dali", 100.23, 25.61),
    ("Luoyang", 112.45, 34.62), ("Changan", 108.94, 34.26), ("Xiangyang", 112.14, 32.04), ("Jianye", 118.80, 32.06),
    ("Quanzhou", 118.59, 24.91),
]


def load_dem(path):
    """ETOPO2v2g: 10801 x 5401 floats, 2 arc-minutes, rows from 90N, columns from 180W."""
    dem = np.memmap(path, dtype="<f4", mode="r", shape=(5401, 10801))
    r0, r1 = int((90 - LAT1) * 30) - 4, int((90 - LAT0) * 30) + 4
    c0, c1 = int((LON0 + 180) * 30) - 4, int((LON1 + 180) * 30) + 4
    sub = np.asarray(dem[r0:r1, c0:c1], dtype=np.float32)
    rows = (90 - LAT) * 30 - r0
    cols = (LON + 180) * 30 - c0
    return ndimage.map_coordinates(sub, [rows, cols], order=1, mode="nearest")


NB = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]


# Basins whose rivers never reach the sea. Anywhere else a hollow in the coarse heights is a
# gorge the grid cannot see (the Three Gorges, the Yellow River's canyons), and water goes on.
ENDORHEIC = [
    [(74, 37), (76, 41), (82, 42.5), (88, 42), (91, 40.5), (91, 38.5), (86, 37), (78, 36.5)],           # Tarim
    [(80.5, 44), (83, 47.5), (88, 47.8), (91.5, 45.5), (89, 43.6), (84, 43.8)],                          # Junggar
    [(88.5, 43.5), (90.5, 43.6), (91, 42.3), (88, 42.2)],                                                # Turpan
    [(89.5, 37.2), (93, 39), (98, 38.2), (99.5, 36.8), (96, 35.8), (91, 36.3)],                          # Qaidam
    [(98.5, 37.6), (100.8, 38), (101, 36.4), (99, 36.3)],                                               # Qinghai Lake
    [(79, 33.5), (80, 36), (86, 36.3), (92, 35.8), (93, 33.5), (91, 31.5), (86, 31), (81, 32)],           # Changtang
    [(94, 41), (97, 44.5), (106, 46), (113, 46.5), (115, 43.5), (110, 41.8), (104, 41), (99, 40.5)],      # Gobi
    [(88, 46), (90, 50.5), (97, 51), (100, 48.5), (95, 45.5)],                                          # Great Lakes of Mongolia
]


def trace_rivers(elev, is_land):
    """Rivers from the heights: water flows from every cell to the sea (priority flood), and
    cells that drain enough land are rivers. Deep closed basins (the Tarim, Qaidam, Turpan)
    keep their water instead of spilling over the mountains. Returns flow accumulation and
    each cell's downstream neighbour."""
    h, w = elev.shape
    n = h * w
    flat = elev.ravel().astype(np.float64)
    land = is_land.ravel()
    done = ~land.copy()
    filled = flat.copy()
    recv = np.full(n, -1, dtype=np.int64)
    heap = []
    # Outlets: land next to the sea or the map's edge.
    edge = np.zeros((h, w), bool)
    edge[0, :] = edge[-1, :] = edge[:, 0] = edge[:, -1] = True
    coast = is_land & (ndimage.binary_dilation(~is_land) | edge)
    for idx in np.flatnonzero(coast.ravel()):
        heapq.heappush(heap, (flat[idx], int(idx)))
        done[idx] = True
    order = []
    while heap:
        e, i = heapq.heappop(heap)
        order.append(i)
        y, x = divmod(i, w)
        for dy, dx in NB:
            yy, xx = y + dy, x + dx
            if yy < 0 or yy >= h or xx < 0 or xx >= w:
                continue
            j = yy * w + xx
            if done[j]:
                continue
            done[j] = True
            fe = max(flat[j], e + 1e-3)
            filled[j] = fe
            recv[j] = i
            heapq.heappush(heap, (fe, j))
    depth = filled - flat
    closed = np.zeros((h, w), np.float32)
    for poly in ENDORHEIC:
        closed = np.maximum(closed, poly_mask(poly, 0.5))
    closed = (closed > 0.5).ravel()
    acc = np.ones(n, dtype=np.float64)
    acc[~land] = 0
    for i in reversed(order):
        r = recv[i]
        # Water in a closed basin's hollow stays there (a terminal lake or desert).
        if r >= 0 and not (closed[i] and depth[i] > 30):
            acc[r] += acc[i]
    return acc.reshape(h, w), recv.reshape(h, w), depth.reshape(h, w)


def ne_features(path):
    """(geometry type, coordinates list) of Natural Earth features inside the mapped area."""
    out = []
    for f in json.load(open(path))["features"]:
        g = f["geometry"]
        if g is None:
            continue
        parts = g["coordinates"] if g["type"].startswith("Multi") else [g["coordinates"]]
        for part in parts:
            out.append((g["type"].replace("Multi", ""), part, f["properties"]))
    return out


def in_area(lon, lat):
    return LON0 - 1 <= lon <= LON1 + 1 and LAT0 - 1 <= lat <= LAT1 + 1


def build(land_path, dem_path, ne_dir):
    land = land_mask(land_path)
    is_land = land > 0.5
    elev = load_dem(dem_path)
    elev = ndimage.gaussian_filter(elev, 0.5)
    # The coastline follows Natural Earth; land below sea level (Turpan) stays dry land.
    elev = np.where(is_land, np.maximum(elev, 2.0), np.minimum(elev, -3.0))
    surf = height_y(elev)

    dist_sea = ndimage.distance_transform_edt(is_land) * KM_PX       # km inland
    dist_land = ndimage.distance_transform_edt(~is_land) * KM_PX     # km offshore
    # Continentalness (for biomes): coast, near inland, far inland; oceans deepen offshore.
    cont = np.where(is_land, -0.08 + 0.35 * np.minimum(1, dist_sea / 120) + 0.3 * np.minimum(1, np.maximum(0, dist_sea - 120) / 500),
                    -0.2 - 0.6 * np.minimum(1, dist_land / 200))
    # Relief: how rugged the land is within ~8 km.
    rel = ndimage.maximum_filter(elev, 5) - ndimage.minimum_filter(elev, 5)
    relief = np.clip(ndimage.gaussian_filter(rel, 0.8) / 1600.0, 0, 1) * is_land
    ero = np.clip(0.55 - 1.45 * relief, -1, 1)
    # High, smooth plateaus (the Tibetan and Yunnan uplands) are not mountain peaks.
    plateau = (elev > 2500) & (relief < 0.35)
    ero = np.where(plateau, np.maximum(ero, 0.05), ero)

    # Climate: warm south, cold north and cold heights; wet south-east, dry north-west.
    ekm = np.maximum(elev, 0) / 1000.0
    temp = 0.85 - (LAT - 18) * 0.036 - 0.17 * ekm
    veg = 0.7 - 0.0010 * np.where(is_land, dist_sea, 0) + np.clip((30 - LAT) * 0.05, 0, 0.6) - np.maximum(0, LAT - 40) * 0.015
    veg = veg - 0.3 * np.clip((elev - 3800) / 800, 0, 1)
    veg = np.where(is_land, veg, 0.3)
    for name, (poly, targets, blur) in REGIONS.items():
        wgt = poly_mask(poly, blur * 2.8) * np.clip(land * 1.2, 0, 1)
        if "temp" in targets:
            temp = blend(temp, targets["temp"] - 0.17 * ekm * 0.5, wgt)
        if "veg" in targets:
            veg = blend(veg, targets["veg"], wgt)

    # Rivers: the real courses (Natural Earth), cut into the heights so that water traced
    # across the land follows them; each river's size is how much land drains into it.
    rivers = [(c, p) for t, c, p in ne_features(os.path.join(ne_dir, "ne_10m_rivers_lake_centerlines.geojson"))
              if t == "LineString" and any(in_area(lo, la) for lo, la in c)]
    burn = Image.new("L", (W, H), 0)
    db = ImageDraw.Draw(burn)
    for c, p in rivers:
        db.line([to_px(lo, la) for lo, la in c], fill=255, width=1)
    burnt = np.asarray(burn) > 0
    acc, recv, depth = trace_rivers(np.where(burnt & is_land, elev - 150, elev), is_land)
    acc_near = ndimage.maximum_filter(acc, 3)
    MIN_CELLS = 400
    pts, strength = [], []
    hgt, wid = elev.shape
    for c, p in rivers:
        line = [to_px(lo, la) for lo, la in c]
        for (ax, ay), (bx, by) in zip(line, line[1:]):
            n = max(1, int(math.hypot(bx - ax, by - ay) / 0.3))
            for t in np.arange(n) / n:
                x, y = ax + (bx - ax) * t, ay + (by - ay) * t
                if not (0 <= x < wid and 0 <= y < hgt):
                    continue
                a = acc_near[int(y), int(x)]
                s_ = 0.12 + 0.88 * min(1.0, max(0.0, math.log(max(a, 1) / MIN_CELLS) / math.log(400)))
                # Natural Earth's own rank of the river (1 = the greatest) as a floor.
                rank = p.get("scalerank") or 9
                s_ = max(s_, {0: 0.9, 1: 0.85, 2: 0.65, 3: 0.5, 4: 0.4, 5: 0.32, 6: 0.25, 7: 0.2}.get(int(rank), 0.12))
                pts.append((x, y))
                strength.append(s_)
    print(f"{len(rivers)} river lines")
    # Lakes, filled to their real shapes.
    lakes = Image.new("L", (W * 4, H * 4), 0)
    dl = ImageDraw.Draw(lakes)
    for t, c, p in ne_features(os.path.join(ne_dir, "ne_10m_lakes.geojson")):
        if t != "Polygon" or not any(in_area(lo, la) for lo, la in c[0]):
            continue
        dl.polygon([(to_px(lo, la)[0] * 4, to_px(lo, la)[1] * 4) for lo, la in c[0]], fill=255)
    lake_px = np.argwhere(np.asarray(lakes) > 0)
    for yy, xx in lake_px[::3]:
        pts.append(((xx + 0.5) / 4, (yy + 0.5) / 4))
        strength.append(1.0)
    print(f"{len(lake_px)} lake samples")
    tree = cKDTree(np.array(pts))
    gy, gx = np.mgrid[0:hgt, 0:wid]
    centres = np.stack([gx.ravel() + 0.5, gy.ravel() + 0.5], 1)
    d, k = tree.query(centres, distance_upper_bound=17)
    river_dist = np.where(np.isfinite(d), d * PX, 1e9).reshape(hgt, wid)       # blocks
    river_str = np.where(np.isfinite(d), np.array(strength + [0.0])[np.minimum(k, len(strength))], 0).reshape(hgt, wid)
    print(f"{len(pts)} river points")

    return {"surf": surf, "cont": np.clip(cont, -1, 1), "ero": ero, "temp": np.clip(temp, -1, 1), "veg": np.clip(veg, -1, 1),
            "relief": relief, "river_dist": river_dist, "river_str": river_str, "elev": elev, "land": land, "acc": acc}


def save(ch):
    """Format 2: header, then each plane as row-wise differences (it compresses far better)."""
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    buf = bytearray(b"RLCN")
    buf += struct.pack(">iiiii", 2, W, H, X0, Z0)
    buf += struct.pack(">i", PX)
    buf += struct.pack(">dddd", ORIGIN_LON, ORIGIN_LAT, M_PER_BLOCK, KM_LON)
    # Surface height in eighths of a block (int16).
    q = np.clip(np.round(ch["surf"] * 8), -32768, 32767).astype(np.int16)
    dq = np.diff(q.astype(np.int32), axis=1, prepend=0).astype(np.int16)
    buf += dq.astype(">i2").tobytes()
    planes = [np.round(ch[k] * 100) for k in ("cont", "ero", "temp", "veg", "relief")]
    planes.append(np.minimum(255, np.round(ch["river_dist"] / 4)))       # 4 blocks per unit
    planes.append(np.round(ch["river_str"] * 100))
    buf += struct.pack(">i", len(planes))
    for p in planes:
        a = np.clip(p, -128, 255).astype(np.int32)
        da = np.diff(a, axis=1, prepend=0)
        buf += (da & 0xFF).astype(np.uint8).tobytes()
    with gzip.open(OUT, "wb", compresslevel=9) as f:
        f.write(bytes(buf))
    print("wrote", OUT, W, "x", H, len(buf) // 1024, "KB raw,", os.path.getsize(OUT) // 1024, "KB packed")


def preview(ch, path):
    """Heights as a shaded relief, with the traced rivers, to check the data by eye."""
    e = ch["elev"]
    shade = np.clip(0.5 + np.gradient(ndimage.gaussian_filter(e, 1), axis=1) / 600, 0, 1)
    rgb = np.zeros(e.shape + (3,))
    t = np.clip(e / 5000, 0, 1)
    rgb[..., 0] = 0.35 + 0.55 * t
    rgb[..., 1] = 0.55 + 0.3 * t - 0.2 * t * t
    rgb[..., 2] = 0.3 + 0.55 * t * t
    rgb *= (0.7 + 0.6 * shade)[..., None]
    rgb[e <= 0] = [0.2, 0.35, 0.6]
    riv = ch["river_dist"] < (3 + 22 * ch["river_str"]) * 2
    rgb[riv & (e > 0)] = [0.2, 0.45, 0.9]
    img = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(img)
    for name, lon, lat in CAPITALS:
        x, y = to_px(lon, lat)
        d.rectangle([x - 3, y - 3, x + 3, y + 3], outline=(200, 30, 30), width=2)
    img.save(path)


if __name__ == "__main__":
    ch = build(sys.argv[1], sys.argv[2], os.path.join(os.path.dirname(os.path.abspath(sys.argv[1])), "..", "..", "ne"))
    save(ch)
    if len(sys.argv) > 3:
        preview(ch, sys.argv[3])
    for name, lon, lat in CAPITALS + [("Shanhai", 109.1, 21.85)]:
        x, z = to_block(lon, lat)
        print(f"{name}: X {round(x)}, Z {round(z)}")
