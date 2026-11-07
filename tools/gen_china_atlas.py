"""
Draws the map of the Earth Online: China world shown when choosing where to begin.

It uses the same data as the world itself (gen_china.py): the Natural Earth coastline, the
mountain ranges, rivers, lakes and regions, so what the map shows is what the land does. On
top of that it draws the provinces with the names the novel gives them (Lingnan, Chuannan,
Shu, Zhongyuan and so on), following the modern provinces they stand for.

Usage: python3 tools/gen_china_atlas.py <land-50m.json> <countries-50m.json> <echarts china.json>
Writes assets/realmlord/textures/gui/china_map.png and realmlord/china/labels.json.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

sys.path.insert(0, os.path.dirname(__file__))
import gen_china as G  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
PNG = os.path.join(ROOT, "assets", "realmlord", "textures", "gui", "china_map.png")
LABELS = os.path.join(ROOT, "realmlord", "china", "labels.json")
S = 1  # image pixels per map pixel

# Modern province -> the novel's province (as on the fan maps of The World Online).
NOVEL = {
    "黑龙江": "Jin Country", "吉林": "Jin Country", "辽宁": "Beijiang",
    "北京": "Jingdu", "天津": "Jingdu", "河北": "Jingdu", "山东": "Ludong", "山西": "Taiyuan",
    "河南": "Zhongyuan", "陕西": "Guanxi", "宁夏": "Guanxi", "甘肃": "Guanxi",
    "江苏": "Donghai", "上海": "Donghai", "安徽": "Wannan", "浙江": "Jiangnan",
    "湖北": "Jingchu", "江西": "Jiangchuan", "湖南": "Xiangnan", "福建": "Minnan",
    "四川": "Shu", "重庆": "Shu", "贵州": "Chuanbei", "云南": "Yunnan",
    "广西": "Chuannan", "广东": "Lingnan", "香港": "Lingnan", "澳门": "Lingnan", "海南": "Qiongzhou",
    "内蒙古": "Mongolia", "青海": "Heidong", "西藏": "Tibet", "新疆": "West Turk", "台湾": "Yizhou",
}
# Where a province's name sits when its middle is a poor spot (lon, lat).
LABEL_AT = {
    "Jin Country": (127.5, 46.5), "Mongolia": (111.0, 42.6), "Guanxi": (107.2, 35.4), "Jingdu": (115.6, 39.0),
    "Chuannan": (108.6, 23.9), "Lingnan": (113.4, 23.7), "Yizhou": (121.0, 23.6), "Qiongzhou": (109.7, 19.2),
    "West Turk": (85.5, 41.8), "Tibet": (88.0, 31.8), "Heidong": (96.5, 35.6), "Donghai": (119.6, 33.0),
}
EXTRA = [  # (name, lon, lat, kind)
    ("Lianzhou Basin", 109.25, 22.62, "place"),
    # The basin's own features, shown close up.
    ("Friendship River", 109.03, 22.1, "local_water"), ("Qiushui River", 109.47, 22.08, "local_water"),
    ("West Lake", 108.9, 22.36, "local_water"), ("East Lake", 109.62, 22.37, "local_water"),
    ("Beihai Bay", 109.15, 21.4, "local_water"), ("Zhennan Pass", 109.32, 22.53, "local_land"),
    ("The Canyon", 109.29, 21.68, "local_land"), ("Shanhai", 109.16, 21.82, "local_land"),
    ("Hundred-thousand Mountains", 109.75, 22.55, "local_land"),
    ("Korea", 127.3, 37.4, "foreign"), ("Vietnam", 105.6, 20.6, "foreign"), ("India", 79.5, 24.5, "foreign"),
    ("Outer Mongolia", 103.0, 47.0, "foreign"), ("Japan", 136.0, 36.0, "foreign"),
    ("Yellow Sea", 123.5, 35.5, "sea"), ("East Sea", 124.5, 29.5, "sea"), ("South Sea", 114.5, 18.5, "sea"),
]


def dec_echarts(s, off, scale=1024):
    if s.startswith("@@"):
        s = s[2:]
    px, py = off
    out = []
    for i in range(0, len(s) - 1, 2):
        x, y = ord(s[i]) - 64, ord(s[i + 1]) - 64
        x = (x >> 1) ^ (-(x & 1))
        y = (y >> 1) ^ (-(y & 1))
        x += px
        y += py
        px, py = x, y
        out.append((x / scale, y / scale))
    return out


def provinces(path):
    d = json.load(open(path, encoding="utf-8"))
    out = []
    for f in d["features"]:
        name = f["properties"]["name"]
        g = f["geometry"]
        polys = [g["coordinates"]] if g["type"] == "Polygon" else g["coordinates"]
        offs = [g["encodeOffsets"]] if g["type"] == "Polygon" else g["encodeOffsets"]
        rings = []
        for poly, po in zip(polys, offs):
            for ring, ro in zip(poly, po):
                rings.append(dec_echarts(ring, ro))
        out.append((name, rings))
    return out


def hi(lon, lat):
    x, y = G.to_px(lon, lat)
    return x * S, y * S


def raster_polys(polys, size, fill_of):
    """Draws topojson polygons (outer ring filled, holes cut) at image scale, supersampled twice."""
    img = Image.new("L", (size[0] * 2, size[1] * 2), 0)
    d = ImageDraw.Draw(img)
    for poly in polys:
        for i, r in enumerate(poly):
            pts = []
            for lo, la in r:
                if 40 <= lo <= 180 and -5 <= la <= 75:
                    x, y = hi(lo, la)
                    pts.append((x * 2, y * 2))
            if len(pts) >= 3:
                d.polygon(pts, fill=fill_of if i == 0 else 0)
    return np.asarray(img.resize(size, Image.BILINEAR), dtype=np.float32) / 255.0


def lerp(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return a * (1 - t) + b * t


def color(hexs):
    return np.array([int(hexs[i:i + 2], 16) / 255 for i in (1, 3, 5)])


def main(land_path, dem_path, echarts_path):
    ch = G.build(land_path, dem_path, os.path.join(os.path.dirname(os.path.abspath(land_path)), "..", "..", "ne"))
    w, h = G.W * S, G.H * S
    up = lambda a: ndimage.zoom(a, S, order=1)[:h, :w]

    land = raster_polys(G.decode_topojson(land_path), (w, h), 255)
    is_land = land > 0.5
    elev, temp, veg = up(ch["elev"]), up(ch["temp"]), up(ch["veg"])

    # Real heights, shaded from the north-west.
    e = ndimage.gaussian_filter(np.maximum(elev, 0), 0.7)
    gy, gx = np.gradient(e)
    shade = np.clip(0.55 + (-gx - gy) / 900.0, 0, 1)
    # Colours by height, like an atlas: green lowland, yellow-brown upland, grey-brown
    # mountains, white high peaks; dry country tinted sand.
    stops = [(0, "#7DA35C"), (200, "#9AB66B"), (600, "#C8C584"), (1200, "#D2B67E"), (2200, "#B8916A"),
             (3500, "#9C7D66"), (4600, "#B7AAA0"), (5600, "#F2F0EC")]
    hs = np.array([s_[0] for s_ in stops], float)
    cs = np.array([color(s_[1]) for s_ in stops])
    base = np.stack([np.interp(e, hs, cs[:, k]) for k in range(3)], -1)
    dryness = np.clip((-veg - 0.35) * 1.6, 0, 1) * (temp > 0.0)
    base = lerp(base, color("#E2CD98"), dryness * np.clip(1 - e / 3500, 0, 1))
    base = base * (0.72 + 0.56 * shade[..., None])
    # Sea, deeper offshore.
    depth = np.clip(-elev / 4000, 0, 1)
    sea = lerp(color("#B6D7E8"), color("#5F92BD"), depth ** 0.5)
    rgb = lerp(sea, base, land)
    rgb = lerp(rgb, color("#E9DCB8"), np.full(rgb.shape[:2], 0.10))
    coast = ndimage.binary_dilation(is_land) & ~ndimage.binary_erosion(is_land)
    rgb[coast] = rgb[coast] * 0.55 + color("#4E6F86") * 0.45
    # Rivers and lakes, as they run in the world.
    rs = up(ch["river_str"])
    rd = up(ch["river_dist"])
    wet = (rd < 24 + 40 * rs) & is_land & (rs > 0.15)
    rgb[wet] = rgb[wet] * 0.25 + color("#3F78B5") * 0.75

    img = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(img, "RGBA")

    # The novel's provinces: thin borders inside, a strong line around the realm.
    provs = provinces(echarts_path)
    names = sorted(set(NOVEL.values()))
    gid = np.zeros((h, w), dtype=np.int32)
    idimg = Image.new("I", (w, h), 0)
    di = ImageDraw.Draw(idimg)
    for pname, rings in provs:
        g = names.index(NOVEL.get(pname, "Jingdu")) + 1
        for r in rings:
            pts = [hi(lo, la) for lo, la in r]
            if len(pts) >= 3:
                di.polygon(pts, fill=g)
    gid = np.asarray(idimg, dtype=np.int32)
    # Coastal slivers the coarse outlines miss belong to the nearest province.
    near_d, near_i = ndimage.distance_transform_edt(gid == 0, return_indices=True)
    fill = is_land & (gid == 0) & (near_d <= 10)
    gid = np.where(fill, gid[near_i[0], near_i[1]], gid)
    gid = np.where(is_land, gid, 0)
    edge = np.zeros_like(is_land)
    for dy, dx in ((0, 1), (1, 0)):
        a, b = gid[: h - dy, : w - dx], gid[dy:, dx:]
        diff = (a != b)
        edge[: h - dy, : w - dx] |= diff & (a > 0) & (b > 0)
    outer = np.zeros_like(is_land)
    for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0)):
        sh = np.roll(np.roll(gid, dy, 0), dx, 1)
        outer |= (gid > 0) & (sh == 0) & np.roll(np.roll(is_land, dy, 0), dx, 1)
    outer = ndimage.binary_dilation(outer)
    arr = np.asarray(img).astype(np.float32) / 255
    arr[edge] = arr[edge] * 0.45 + color("#7A5230") * 0.55
    arr[outer] = arr[outer] * 0.25 + color("#B23A32") * 0.75
    img = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGB")
    os.makedirs(os.path.dirname(PNG), exist_ok=True)
    img.save(PNG, optimize=True)

    # Labels, in block coordinates, drawn by the game so they stay sharp.
    labels = []
    for gname in names:
        if gname in LABEL_AT:
            lon, lat = LABEL_AT[gname]
            bx, bz = G.to_block(lon, lat)
        else:
            ys, xs = np.nonzero(gid == names.index(gname) + 1)
            if len(xs) == 0:
                continue
            bx = G.X0 + (xs.mean() / S) * G.PX
            bz = G.Z0 + (ys.mean() / S) * G.PX
        labels.append({"name": gname, "x": round(bx), "z": round(bz), "kind": "province"})
    for name, lon, lat, kind in EXTRA:
        bx, bz = G.to_block(lon, lat)
        labels.append({"name": name, "x": round(bx), "z": round(bz), "kind": kind})
    with open(LABELS, "w") as f:
        json.dump({"image": [w, h], "labels": labels}, f, indent=1)
    print("wrote", PNG, img.size, os.path.getsize(PNG) // 1024, "KB;", len(labels), "labels")


if __name__ == "__main__":
    main(*sys.argv[1:4])
