#!/usr/bin/env python3
"""Builds the China world map for Realm Lord.

The world is centred on Luoyang (block 0,0), at 280 m per block, so China is about 20,000
blocks across. Six channels steer Minecraft's own terrain generator, one pixel per 32 blocks:
continentalness, erosion, temperature, humidity, mountain strength and river strength.

Coastline: Natural Earth 1:50m land (via the world-atlas npm package, public domain).
Everything else is drawn here from real longitudes and latitudes.

Usage: python3 tools/gen_china.py path/to/land-50m.json
"""
import gzip
import json
import math
import os
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "realmlord", "china", "map.bin.gz")

ORIGIN_LON, ORIGIN_LAT = 112.45, 34.62          # Luoyang
M_PER_BLOCK = 280.0
KM_LON = 111.32 * math.cos(math.radians(35))    # km per degree of longitude
KM_LAT = 111.32
PX = 32                                          # blocks per pixel
X0, X1 = -14528, 9024
Z0, Z1 = -8512, 8256
W, H = (X1 - X0) // PX, (Z1 - Z0) // PX
KM_PX = PX * M_PER_BLOCK / 1000.0


def to_block(lon, lat):
    return (lon - ORIGIN_LON) * KM_LON * 1000 / M_PER_BLOCK, -(lat - ORIGIN_LAT) * KM_LAT * 1000 / M_PER_BLOCK


def to_px(lon, lat):
    x, z = to_block(lon, lat)
    return (x - X0) / PX, (z - Z0) / PX


# pixel centre -> lon/lat grids
xs = X0 + (np.arange(W) + 0.5) * PX
zs = Z0 + (np.arange(H) + 0.5) * PX
LON = ORIGIN_LON + xs[None, :] * M_PER_BLOCK / 1000 / KM_LON + np.zeros((H, 1))
LAT = ORIGIN_LAT - zs[:, None] * M_PER_BLOCK / 1000 / KM_LAT + np.zeros((1, W))

# ------------------------------------------------------------------ land mask

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
    "pearl_delta": ([(112.6, 22.4), (113.2, 23.4), (114.1, 23.2), (114, 22.4)], {"cont": 0.02, "ero": 0.85}, 1.0),
    "indo_gangetic": ([(72, 30), (78, 29.5), (84, 27.3), (89, 26.2), (90, 23), (84, 23), (77, 25), (71, 27)],
                      {"cont": 0.15, "ero": 0.8, "veg": 0.4, "temp": 0.8}, 3.0),
}

CAPITALS = [  # the novel's nine Imperial Capitals
    ("Jingdu", 116.40, 39.90), ("Xianyang", 108.71, 34.33), ("Chengdu", 104.07, 30.66), ("Dali", 100.23, 25.61),
    ("Luoyang", 112.45, 34.62), ("Changan", 108.94, 34.26), ("Xiangyang", 112.14, 32.04), ("Jianye", 118.80, 32.06),
    ("Quanzhou", 118.59, 24.91),
]


def build(land_path):
    land = land_mask(land_path)
    is_land = land > 0.5
    dist_sea = ndimage.distance_transform_edt(is_land) * KM_PX       # km inland
    dist_land = ndimage.distance_transform_edt(~is_land) * KM_PX     # km offshore

    # Continentalness: oceans deepen offshore, land rises inland.
    cont = np.where(is_land, -0.05 + 0.35 * np.minimum(1, dist_sea / 150) + 0.25 * np.minimum(1, np.maximum(0, dist_sea - 150) / 600),
                    -0.2 - 0.45 * np.minimum(1, dist_land / 250))
    cont = ndimage.gaussian_filter(cont, 1.0)
    ero = np.full((H, W), 0.3, dtype=np.float64)
    lift = np.zeros((H, W))
    temp = 0.8 - (LAT - 18) * 0.034
    # Humidity: wet south-east, dry north-west.
    veg = 0.65 - 0.0011 * np.where(is_land, dist_sea, 0) + np.clip((30 - LAT) * 0.05, 0, 0.6) - np.maximum(0, LAT - 40) * 0.015
    veg = np.where(is_land, veg, 0.3)

    for name, (poly, targets, blur) in REGIONS.items():
        w = poly_mask(poly, blur) * np.clip(land * 1.2, 0, 1)
        for ch, val in targets.items():
            if ch == "cont":
                cont = blend(cont, val, w)
            elif ch == "ero":
                ero = blend(ero, val, w)
            elif ch == "temp":
                temp = blend(temp, val, w)
            elif ch == "veg":
                veg = blend(veg, val, w)
            elif ch == "lift":
                lift = np.maximum(lift, val * w)

    mountain = np.zeros((H, W))
    for name, line, strength, width in MOUNTAINS:
        mountain = np.maximum(mountain, strength * line_field(line, width))
    mountain *= np.clip(land * 1.3, 0, 1)
    ero = ero - 1.25 * mountain
    cont = cont + 0.3 * mountain * (cont > -0.1)
    temp = temp - 0.45 * mountain
    lift = np.minimum(110, lift + 30 * mountain)

    river = np.zeros((H, W))
    for name, line, strength in RIVERS:
        n = len(line)
        # Stronger towards the mouth.
        for i in range(n - 1):
            s = strength * (0.55 + 0.45 * (i / max(1, n - 2)))
            river = np.maximum(river, s * line_field(line[i:i + 2], 9))
    for lon, lat, km in LAKES:
        px, py = to_px(lon, lat)
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.hypot(xx - px, yy - py) * KM_PX
        river = np.maximum(river, np.clip(1.4 - d / km, 0, 1))
    river *= np.clip(land * 1.5, 0, 1)

    cont = np.clip(cont, -1.0, 1.0)
    ero = np.clip(ero, -1.0, 1.0)
    temp = np.clip(temp, -1.0, 1.0)
    veg = np.clip(veg, -1.0, 1.0)
    lift = lift * (1 - river * 0.6)
    return {"cont": cont, "ero": ero, "temp": temp, "veg": veg, "mountain": np.clip(mountain, 0, 1), "river": np.clip(river, 0, 1),
            "lift": np.clip(lift / 200.0, 0, 1), "land": land}


def save(ch):
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    order = ["cont", "ero", "temp", "veg", "mountain", "river", "lift"]
    buf = bytearray(b"RLCN")
    buf += struct.pack(">iiiii", 1, W, H, X0, Z0)
    buf += struct.pack(">ii", PX, len(order))
    for name in order:
        q = np.clip(np.round(ch[name] * 100), -127, 127).astype(np.int8)
        buf += q.tobytes()
    with gzip.open(OUT, "wb", compresslevel=9) as f:
        f.write(bytes(buf))
    print("wrote", OUT, W, "x", H, len(buf) // 1024, "KB raw")


def preview(ch, path):
    """A rough look at what the terrain should do: blue sea, green lowland, brown hills, white peaks, yellow desert."""
    cont, ero, temp, veg, mtn, riv = (ch[k] for k in ["cont", "ero", "temp", "veg", "mountain", "river"])
    rgb = np.zeros((H, W, 3))
    sea = cont < -0.11
    height = np.clip((cont + 0.1) * 0.8 + (0.5 - ero) * 0.5, 0, 1.5)
    green = np.stack([0.35 + 0.0 * height, 0.6 - 0.2 * height, 0.3 + 0.0 * height], -1)
    brown = np.array([0.55, 0.45, 0.3])
    desert = np.array([0.88, 0.8, 0.55])
    snow = np.array([0.95, 0.95, 0.97])
    hill = np.clip(height - 0.35, 0, 1)[..., None]
    rgb = green * (1 - hill) + brown * hill
    dry = np.clip(-veg - 0.5, 0, 1)[..., None] * (temp[..., None] > 0.2)
    rgb = rgb * (1 - dry) + desert * dry
    cold = np.clip(-temp - 0.45, 0, 1)[..., None] * 2
    rgb = rgb * (1 - np.clip(cold, 0, 1)) + snow * np.clip(cold, 0, 1)
    rgb[sea] = [0.15, 0.3, 0.55]
    r = np.clip(riv, 0, 1)[..., None]
    rgb = rgb * (1 - r * (~sea)[..., None]) + np.array([0.25, 0.45, 0.8]) * r * (~sea)[..., None]
    img = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(img)
    for name, lon, lat in CAPITALS:
        x, y = to_px(lon, lat)
        d.rectangle([x - 2, y - 2, x + 2, y + 2], fill=(200, 30, 30))
    img.save(path)


if __name__ == "__main__":
    ch = build(sys.argv[1])
    save(ch)
    if len(sys.argv) > 2:
        preview(ch, sys.argv[2])
    for name, lon, lat in CAPITALS:
        x, z = to_block(lon, lat)
        print(f"{name}: X {round(x)}, Z {round(z)}")
