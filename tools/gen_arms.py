"""Textures, models, recipes and names for the arms of the novel's world: beast sinew and cores,
bandit insignia, black iron and refined steel, the sabre, spear, straight sword, guandao and
tiger-bone bow, and hide and black iron armour (item icons and the worn armour layers)."""
import json
import os
import random
from PIL import Image

from gen_goods import hexc, shade, blank

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "realmlord")
DATA = os.path.join(ROOT, "data", "realmlord")

MATERIALS = ["beast_sinew", "beast_core", "bandit_insignia", "black_iron_ingot", "refined_steel"]
BLADES = ["bandit_sabre", "black_iron_sabre", "black_iron_spear", "steel_jian", "guandao"]
ARMOUR = {"hide": ["hide_helmet", "hide_jerkin", "hide_leggings", "hide_boots"],
          "black_iron": ["black_iron_helmet", "black_iron_cuirass", "black_iron_greaves", "black_iron_boots"],
          "lamellar": ["lamellar_helmet", "lamellar_coat", "lamellar_tassets", "lamellar_boots"],
          "mountain_pattern": ["mountain_helm", "mountain_armour", "mountain_greaves", "mountain_boots"],
          "tiger_war": ["tiger_hood", "tiger_war_coat", None, None]}
NEW_MATERIALS = ["lamellar_plates", "whetstone", "refining_stone", "golden_wound_powder", "healing_pill", "marrow_cleansing_pill",
                 "enlightenment_pill", "military_rations", "manual"]
NEW_BLADES = ["ring_dao", "ji", "tiger_soul_sabre"]

HIDE = hexc("8a6440")
BLACK_IRON = hexc("3a3d44")
STEEL = hexc("c9d2dc")
WOOD = hexc("6a4424")
RED = hexc("b0302a")


def ingot(col):
    img, px = blank()
    for y in range(6, 11):
        for x in range(3 + (10 - y) // 2, 13 - (10 - y) // 2):
            px[x, y] = shade(col, 1.15 if y == 6 else 0.8 if y == 10 else 1.0)
    for x in range(5, 11): px[x, 6] = shade(col, 1.35)
    return img


def sinew():
    img, px = blank()
    c = hexc("d8b9a0")
    for i in range(12):
        x, y = 2 + i, 8 + int(3 * __import__("math").sin(i / 1.8))
        px[x, y] = shade(c, 1.05)
        px[x, y + 1] = shade(c, 0.85)
    return img


def core():
    img, px = blank()
    for y in range(3, 13):
        for x in range(3, 13):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5:
                glow = 1.3 - d / 6
                px[x, y] = shade(hexc("c43f9a"), glow) if d > 2 else shade(hexc("f6c8ff"), 1.0)
    px[6, 5] = px[5, 6] = hexc("ffffff")
    return img


def insignia():
    img, px = blank()
    for y in range(3, 13):
        for x in range(4, 12):
            if abs(x - 7.5) + abs(y - 7.5) < 6: px[x, y] = shade(hexc("4a2e1a"), 1.0)
    for y in range(5, 11): px[7, y] = px[8, y] = RED
    for x in range(5, 11): px[x, 7] = RED
    return img


def blade(edge, length, width, hilt, guard=hexc("c8a24a"), curve=0.0, pole=False):
    """A diagonal blade from lower left to upper right."""
    img, px = blank()
    handle = 5 if pole else 3
    for i in range(handle):
        px[1 + i, 14 - i] = hilt
    gx, gy = 1 + handle, 14 - handle
    for d in (-1, 0, 1): px[gx + d, gy + d] = guard
    for i in range(1, length + 1):
        x, y = gx + i, gy - i
        bend = int(round(curve * (i / length) ** 2 * 2))
        for w in range(width):
            xx, yy = x + w - bend, y + w
            if 0 <= xx < 16 and 0 <= yy < 16:
                px[xx, yy] = shade(edge, 1.25 if w == 0 else 0.95 - 0.08 * w)
    return img


def spear_img(head, shaft):
    img, px = blank()
    for i in range(1, 12): px[i, 15 - i] = shaft
    for x, y in [(11, 4), (12, 3), (13, 2), (14, 1), (12, 4), (11, 3), (13, 3), (12, 2)]: px[x, y] = shade(head, 1.1 if (x + y) % 2 else 0.9)
    px[10, 6] = px[11, 6] = RED
    return img


def guandao_img():
    img, px = blank()
    for i in range(0, 10): px[1 + i, 15 - i] = WOOD
    # the crescent blade at the head
    for x, y in [(10, 6), (11, 5), (12, 4), (13, 3), (14, 2), (14, 3), (14, 4), (13, 5), (12, 6), (13, 4), (12, 5), (11, 6)]:
        px[x, y] = shade(STEEL, 1.2 if y < 4 else 0.95)
    px[9, 7] = px[10, 7] = hexc("c8a24a")
    px[8, 8] = RED
    return img


def bow_img(pull=0):
    img, px = blank()
    bone = hexc("e8dcc0")
    pts = [(3, 2), (4, 2), (5, 3), (6, 4), (7, 5), (8, 6), (9, 7), (10, 8), (11, 9), (12, 10), (13, 11), (13, 12), (13, 13)]
    for i, (x, y) in enumerate(pts):
        px[x, y] = shade(bone, 1.05 if i % 3 else 0.8)
    for (x, y) in [(5, 3), (9, 7), (12, 10)]: px[x, y] = hexc("3a2a1a")
    # the string, drawn back as it pulls
    for t in range(12):
        x, y = 3 + t * 10 // 11 - pull, 2 + t
        if 0 <= x < 16: px[x, min(15, y)] = hexc("d0d0d0")
    return img


# ---------------------------------------------------------------- armour styles
#
# Each style paints a surface pixel by pixel: pattern(face, x, y, w, h, rnd) gives the colour of
# the pixel at (x, y) on a face w by h, or None for bare. The same painters draw the worn layers
# (on the player model's unwrapped boxes) and the item icons, so the two match.

LACE = hexc("a3322a")
GOLD_TRIM = hexc("c8a24a")
BRONZE = hexc("9a6a2e")
LEATHER = hexc("5a3a22")


def fur(base, x, y, rnd, dark=0.78, light=1.12):
    """Fur: streaks running down, a little noise, lighter tips."""
    f = 1.0 + rnd.uniform(-0.07, 0.07)
    if (x * 5 + y * 3 + (x // 2) * 7) % 9 == 0: f *= dark
    if (x + 2 * y) % 11 == 0: f *= light
    return shade(base, f)


def style_hide(face, x, y, w, h, rnd):
    base = hexc("8a6440")
    c = fur(base, x, y, rnd)
    if face == "front" and w >= 8 and x in (w // 2 - 1, w // 2) and y % 2 == 0: return hexc("d8c098")   # toggles down the front
    if y == h - 1: return fur(hexc("cbb08a"), x, y, rnd, 0.9, 1.05)      # pale fur trim at the hem
    if (x == 0 or x == w - 1) and y % 2 == 1: return hexc("3a2616")          # stitching at the seams
    if face == "top": return fur(hexc("6e4c30"), x, y, rnd)
    return c


def style_black_iron(face, x, y, w, h, rnd):
    base = hexc("3a3d44")
    band = y % 4
    if band == 3: return shade(base, 0.55)                                # the gap under each plate
    f = 1.18 if band == 0 else 1.0 if band == 1 else 0.88
    c = shade(base, f + rnd.uniform(-0.04, 0.04))
    if band == 1 and (x % 4 == 1): return hexc("8c8e96")                   # rivets
    if y == 0 or y == h - 1: return GOLD_TRIM
    if face == "front" and w >= 8 and 3 <= y <= 6 and 2 <= x <= w - 3 and (x + y) % 2 == 0: return shade(GOLD_TRIM, 0.8)  # a gilt beast-face boss
    return c


def style_lamellar(face, x, y, w, h, rnd):
    """Rows of small black iron scales, each two pixels wide and three tall, the rows offset and
    laced together with red cord; a polished mirror over the heart."""
    iron = hexc("4a4f58")
    row = y // 3
    yy = y % 3
    col = (x + row) % 2
    if face == "front" and w >= 8 and 2 <= y <= 5 and w // 2 - 2 <= x <= w // 2 + 1:
        d = abs(x - (w / 2 - 0.5)) + abs(y - 3.5)
        return hexc("e8eef4") if d < 1.2 else hexc("b9c2cc") if d < 2.2 else GOLD_TRIM   # the heart-protecting mirror
    if yy == 2 and col == 0: return LACE                                     # lacing between the rows
    if yy == 2: return shade(iron, 0.5)
    if col == 0 and yy == 0: return shade(iron, 1.45)                        # light catching each scale's top
    if col == 1: return shade(iron, 0.85 + rnd.uniform(-0.04, 0.04))
    if y == 0: return LEATHER
    return shade(iron, 1.1 + rnd.uniform(-0.05, 0.05))


def style_mountain(face, x, y, w, h, rnd):
    """Mountain-pattern armour: steel scales shaped like the character 山, interlocking with no
    lacing; a gilt mirror on the chest and a red silk sash at the waist."""
    steel = hexc("aeb8c4")
    if face == "front" and w >= 8 and 2 <= y <= 5 and w // 2 - 2 <= x <= w // 2 + 1:
        d = ((x - (w / 2 - 0.5)) ** 2 + (y - 3.5) ** 2) ** 0.5
        return hexc("f4f6f8") if d < 1.0 else GOLD_TRIM
    if PART[0] == "body" and face in ("front", "back", "left", "right") and y in (h - 4, h - 3):
        return hexc("b02a26") if (x + y) % 3 else hexc("7a1a18")                # the sash
    if y == 0 or y == h - 1: return GOLD_TRIM
    tx, ty = x % 4, y % 4
    motif = ["#.#.", "###.", ".#..", "#.##"][ty][tx]
    if motif == "#":
        return shade(steel, 1.18 if ty == 0 else 1.0 + rnd.uniform(-0.04, 0.04))
    return shade(steel, 0.58)


def style_tiger(face, x, y, w, h, rnd):
    """A tiger's hide: tawny orange with black stripes; the belly and the inside white."""
    orange = hexc("d27a26")
    if face == "front" and w >= 8 and h >= 10 and w // 2 - 2 <= x <= w // 2 + 1 and y >= 2: base = hexc("f2e8d8")
    else: base = orange
    stripe = (x * 2 + y * 3 + (y // 3)) % 9 in (0, 1) and not (face == "front" and base != orange and y > 3)
    if stripe: return shade(hexc("1a1410"), 1.0 + rnd.uniform(-0.1, 0.1))
    if y == h - 1 and h > 4: return hexc("f2e8d8")
    return fur(base, x, y, rnd, 0.85, 1.08)


PART = ["body"]   # which part of the body is being painted (the icons count as the body)

STYLES = {"hide": style_hide, "black_iron": style_black_iron, "lamellar": style_lamellar,
          "mountain_pattern": style_mountain, "tiger_war": style_tiger}


def head_details(kind, px, rnd):
    """Helmets: the pieces that make each recognisable (faces at u 0-32, v 0-16)."""
    if kind == "mountain_pattern":
        for (x, y) in [(11, 3), (12, 3), (11, 4), (12, 4), (12, 2), (11, 2)]: px[x, y] = hexc("c42a24")   # red tassel on the crown
        for x in range(0, 32): px[x, 8] = GOLD_TRIM                                                       # gilt brim
    if kind == "lamellar":
        for (x, y) in [(11, 3), (12, 4), (12, 3), (11, 4)]: px[x, y] = hexc("8c8e96")                    # iron finial
    if kind == "black_iron":
        for y in range(8, 14): px[11, y] = px[12, y] = hexc("6e7078")                                    # nasal guard
        for x in range(10, 14): px[x, 3] = GOLD_TRIM; px[x, 4] = GOLD_TRIM                              # crest
    if kind == "hide":
        for (x, y) in [(9, 1), (14, 1), (9, 6), (14, 6)]: px[x, y] = hexc("cbb08a")
    if kind == "tiger_war":
        # A tiger's face over the brow: the 王 on the forehead, amber eyes, a white muzzle.
        for x in range(10, 14): px[x, 9] = px[x, 11] = hexc("1a1410")
        px[11, 10] = px[12, 10] = hexc("1a1410")
        for (x, y) in [(9, 12), (14, 12)]: px[x, y] = hexc("f0c030")
        for (x, y) in [(10, 12), (13, 12)]: px[x, y] = hexc("1a1410")
        for x in range(9, 15): px[x, 15] = hexc("f2e8d8")
        px[11, 14] = px[12, 14] = hexc("f2e8d8")
        for (x, y) in [(8, 0), (9, 0), (14, 0), (15, 0)]: px[x, y] = hexc("1a1410")   # ears on the crown


def armour_layer(kind, layer, seed):
    """The worn armour: the humanoid layout, filled in where the armour covers."""
    rnd = random.Random(seed)
    paint = STYLES[kind]
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()

    def box(u, v, w, h, d, keep=lambda f, x, y: True):
        PART[0] = {(0, 0): "head", (16, 16): "body", (40, 16): "arm", (0, 16): "leg"}[(u, v)]
        faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
                 ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
        for (name, x0, y0, ww, hh) in faces:
            for y in range(hh):
                for x in range(ww):
                    if not keep(name, x, y): continue
                    c = paint(name, x, y, ww, hh, rnd)
                    if c is not None and 0 <= x0 + x < 64 and 0 <= y0 + y < 32: px[x0 + x, y0 + y] = c

    sides = ("right", "front", "left", "back")
    if layer == 1:
        box(0, 0, 8, 8, 8, lambda f, x, y: not (f == "front" and kind != "tiger_war" and 2 <= y <= 6 and 1 <= x <= 6))   # face open
        head_details(kind, px, rnd)
        box(16, 16, 8, 12, 4)
        if kind == "lamellar":     # shoulder pieces only, the sleeves bare
            box(40, 16, 4, 12, 4, lambda f, x, y: f == "top" or f in sides and y < 5)
        elif kind == "mountain_pattern":
            box(40, 16, 4, 12, 4, lambda f, x, y: f == "top" or f in sides and y < 8)
        else:
            box(40, 16, 4, 12, 4)
        box(0, 16, 4, 12, 4, lambda f, x, y: f == "bottom" or f in sides and y >= 7)    # boots
    else:
        box(16, 16, 8, 12, 4, lambda f, x, y: f in sides and y >= 7 or f == "bottom")  # the waist
        if kind == "lamellar":     # tassets: a skirt of scales over the thighs, the shins bare
            box(0, 16, 4, 12, 4, lambda f, x, y: f == "top" or f in sides and y < 8)
        else:
            box(0, 16, 4, 12, 4)
    return img


SHAPES = {
    "helmet": ["................", "................", ".....######.....", "....########....", "...##########...", "...##########...",
               "...###....###...", "...##......##...", "...##......##...", "...#........#...", "................", "................",
               "................", "................", "................", "................"],
    "chest": ["................", "..####....####..", ".######..######.", ".##############.", ".##############.", ".##############.",
              "..############..", "...##########...", "...##########...", "...##########...", "...##########...", "...##########...",
              "...##########...", "...##########...", "................", "................"],
    "legs": ["................", "...##########...", "...##########...", "...##########...", "...####..####...", "...####..####...",
             "...####..####...", "...####..####...", "...####..####...", "...####..####...", "...####..####...", "...####..####...",
             "...####..####...", "................", "................", "................"],
    "boots": ["................", "................", "................", "................", "................", "................",
              "..####....####..", "..####....####..", "..####....####..", "..####....####..", ".#####...#####..", "######..######..",
              "######..######..", "................", "................", "................"],
}


def armour_icon(kind, slot):
    img, px = blank()
    rnd = random.Random(hash((kind, slot)) & 0xffff)
    shape = [row[:] for row in SHAPES[slot]]
    if kind == "lamellar" and slot == "legs":    # tassets: a flared skirt of scales
        shape = ["................", "...##########...", "..############..", "..############..", ".##############.", ".##############.",
                 ".######..######.", "..####....####..", "................", "................", "................", "................",
                 "................", "................", "................", "................"]
    if kind == "tiger_war" and slot == "helmet":
        shape = ["................", "...##......##...", "...###....###...", "...##########...", "..############..", "..############..",
                 "..############..", "..############..", "...##########...", "...##......##...", "................", "................",
                 "................", "................", "................", "................"]
    paint = STYLES[kind]
    ys = [y for y in range(16) if "#" in shape[y]]
    top, h = ys[0], len(ys)
    for y in range(16):
        for x in range(16):
            if shape[y][x] != "#": continue
            PART[0] = "body" if slot == "chest" else slot
            c = paint("front", x - 2, y - top, 12, max(h, 12) if slot == "chest" else h, rnd)
            px[x, y] = c if c else hexc("000000", 0)
    # outline in a darker tone
    for y in range(16):
        for x in range(16):
            if shape[y][x] != "#": continue
            if any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or shape[y + dy][x + dx] != "#" for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = shade(px[x, y], 0.62)
    if slot == "helmet":
        if kind == "mountain_pattern":
            for (x, y) in [(7, 0), (8, 0), (7, 1), (8, 1)]: px[x, y] = hexc("c42a24")
        if kind == "lamellar":
            px[7, 1] = px[8, 1] = hexc("8c8e96")
        if kind == "black_iron":
            for y in range(6, 10): px[7, y] = px[8, y] = hexc("6e7078")
        if kind == "tiger_war":
            for x in range(6, 10): px[x, 4] = px[x, 6] = hexc("1a1410")
            px[7, 5] = px[8, 5] = hexc("1a1410")
            px[5, 6] = px[10, 6] = hexc("f0c030")
            for x in range(6, 10): px[x, 8] = hexc("f2e8d8")
    return img


def lamellar_plates_img():
    img, px = blank()
    iron = hexc("4a4f58")
    for r in range(3):
        for c in range(4):
            x0, y0 = 2 + c * 3 + (r % 2), 3 + r * 4
            for dy in range(3):
                for dx in range(2):
                    px[x0 + dx, y0 + dy] = shade(iron, 1.45 if dy == 0 and dx == 0 else 1.0 if dx == 0 else 0.8)
            px[x0, y0 + 3] = LACE
    return img


def whetstone_img():
    img, px = blank()
    for i in range(10):
        for w in range(3):
            x, y = 3 + i, 10 - i // 2 + w - 1
            px[x, y] = shade(hexc("8a8478"), 1.15 if w == 0 else 0.95 if w == 1 else 0.75)
    return img


def refining_stone_img():
    img, px = blank()
    for y in range(4, 13):
        for x in range(3, 13):
            if abs(x - 7.5) / 5 + abs(y - 8.5) / 4.5 < 1.0:
                px[x, y] = shade(hexc("2e3036"), 1.0 + 0.1 * ((x * 3 + y) % 3 == 0))
    for (x, y) in [(5, 7), (6, 8), (7, 8), (8, 9), (9, 9), (10, 10), (7, 7), (8, 8)]: px[x, y] = hexc("d45aa8")
    px[7, 8] = px[8, 8] = hexc("ffd0f0")
    for x in range(5, 11): px[x, 5] = hexc("5a5e68")
    return img


def powder_img():
    img, px = blank()
    paper = hexc("e8dcb8")
    for y in range(4, 13):
        for x in range(3, 13):
            if abs(x - 7.5) + abs(y - 8.5) * 0.8 < 5.5: px[x, y] = shade(paper, 1.0 - 0.02 * y)
    for (x, y) in [(7, 3), (8, 3), (7, 4), (8, 4)]: px[x, y] = LACE
    for (x, y) in [(6, 8), (8, 9), (9, 7), (7, 10)]: px[x, y] = hexc("e0b030")
    return img


def pill_img(col, glow=None):
    img, px = blank()
    for y in range(4, 12):
        for x in range(4, 12):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.8: px[x, y] = shade(hexc(col), 1.3 - d / 5)
            elif glow and d < 4.8: px[x, y] = hexc(glow, 120)
    px[6, 6] = px[6, 5] = hexc("ffffff")
    return img


def rations_img():
    img, px = blank()
    for y in range(5, 12):
        for x in range(2, 14):
            px[x, y] = shade(hexc("b08850"), 1.1 - 0.04 * (y - 5) + (0.06 if (x + y) % 3 == 0 else 0))
    for y in range(5, 12): px[5, y] = px[10, y] = hexc("5a3a22")
    for x in range(2, 14): px[x, 4] = hexc("d8c098")
    return img


def manual_img():
    img, px = blank()
    for y in range(2, 14):
        for x in range(3, 13):
            px[x, y] = shade(hexc("2e4a7a"), 1.05 - 0.02 * y)
    for y in range(2, 14): px[3, y] = hexc("e8dcb8"); px[12, y] = hexc("1a2a48")
    for y in (4, 7, 10, 12): px[4, y] = hexc("c8c0a0")                   # stitched binding
    for y in range(4, 11): px[9, y] = px[10, y] = hexc("e8dcb8")            # the title slip
    px[9, 6] = px[10, 8] = hexc("3a2a1a")
    return img


def ring_dao_img():
    img = blade(shade(BLACK_IRON, 1.4), 9, 2, hexc("3a2a1a"), guard=hexc("6a6a6a"), curve=0.6)
    px = img.load()
    for (x, y) in [(0, 13), (0, 14), (1, 15), (2, 15), (1, 12), (2, 12)]: px[x, y] = GOLD_TRIM   # the ring pommel
    return img


def ji_img():
    img = spear_img(STEEL, WOOD)
    px = img.load()
    for (x, y) in [(9, 4), (8, 3), (8, 4), (9, 5), (7, 3)]: px[x, y] = shade(STEEL, 1.1)    # the crescent side blade
    return img


def tiger_soul_img():
    img = blade(hexc("6a6e78"), 9, 2, hexc("d27a26"), guard=GOLD_TRIM, curve=1.6)
    px = img.load()
    for i in range(1, 10):
        x, y = 4 + i, 11 - i
        if 0 <= x < 16 and 0 <= y < 16 and px[x, y][3]: px[x, y] = hexc("f08a30")    # the edge glows like embers
    for (x, y) in [(1, 14), (2, 13), (3, 12)]: px[x, y] = hexc("1a1410") if (x % 2) else hexc("d27a26")
    return img


EXTRA_ICONS = {"lamellar_plates": lamellar_plates_img, "whetstone": whetstone_img, "refining_stone": refining_stone_img,
               "golden_wound_powder": powder_img, "healing_pill": lambda: pill_img("a83a2a"),
               "marrow_cleansing_pill": lambda: pill_img("f4ecd0", "f0d060"), "enlightenment_pill": lambda: pill_img("6a5ad0", "a8c8ff"),
               "military_rations": rations_img, "manual": manual_img, "ring_dao": ring_dao_img, "ji": ji_img,
               "tiger_soul_sabre": tiger_soul_img}


def textures():
    item = os.path.join(ASSETS, "textures", "item")
    os.makedirs(item, exist_ok=True)
    sinew().save(os.path.join(item, "beast_sinew.png"))
    core().save(os.path.join(item, "beast_core.png"))
    insignia().save(os.path.join(item, "bandit_insignia.png"))
    ingot(BLACK_IRON).save(os.path.join(item, "black_iron_ingot.png"))
    ingot(STEEL).save(os.path.join(item, "refined_steel.png"))
    blade(hexc("a8a8a8"), 8, 2, WOOD, guard=hexc("6a6a6a"), curve=1.5).save(os.path.join(item, "bandit_sabre.png"))
    blade(shade(BLACK_IRON, 1.4), 9, 2, hexc("2a1a10"), curve=1.8).save(os.path.join(item, "black_iron_sabre.png"))
    spear_img(shade(BLACK_IRON, 1.5), WOOD).save(os.path.join(item, "black_iron_spear.png"))
    blade(STEEL, 10, 1, hexc("3a2a5a")).save(os.path.join(item, "steel_jian.png"))
    guandao_img().save(os.path.join(item, "guandao.png"))
    bow_img(0).save(os.path.join(item, "tiger_bone_bow.png"))
    for i, pull in enumerate([1, 2, 3]):
        bow_img(pull).save(os.path.join(item, f"tiger_bone_bow_pulling_{i}.png"))
    for kind, names in ARMOUR.items():
        for n, slot in zip(names, ["helmet", "chest", "legs", "boots"]):
            if n: armour_icon(kind, slot).save(os.path.join(item, n + ".png"))
    armour = os.path.join(ASSETS, "textures", "models", "armor")
    os.makedirs(armour, exist_ok=True)
    for i, kind in enumerate(ARMOUR):
        armour_layer(kind, 1, 2 * i + 1).save(os.path.join(armour, kind + "_layer_1.png"))
        armour_layer(kind, 2, 2 * i + 2).save(os.path.join(armour, kind + "_layer_2.png"))
    for name, fn in EXTRA_ICONS.items():
        fn().save(os.path.join(item, name + ".png"))


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def models():
    m = os.path.join(ASSETS, "models", "item")
    for n in MATERIALS + NEW_MATERIALS + [a for names in ARMOUR.values() for a in names if a]:
        write(os.path.join(m, n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{n}"}})
    for n in BLADES + NEW_BLADES:
        write(os.path.join(m, n + ".json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": f"realmlord:item/{n}"}})
    write(os.path.join(m, "tiger_bone_bow.json"), {
        "parent": "minecraft:item/bow", "textures": {"layer0": "realmlord:item/tiger_bone_bow"},
        "overrides": [
            {"predicate": {"pulling": 1}, "model": "realmlord:item/tiger_bone_bow_pulling_0"},
            {"predicate": {"pulling": 1, "pull": 0.65}, "model": "realmlord:item/tiger_bone_bow_pulling_1"},
            {"predicate": {"pulling": 1, "pull": 0.9}, "model": "realmlord:item/tiger_bone_bow_pulling_2"}]})
    for i in range(3):
        write(os.path.join(m, f"tiger_bone_bow_pulling_{i}.json"),
              {"parent": "realmlord:item/tiger_bone_bow", "textures": {"layer0": f"realmlord:item/tiger_bone_bow_pulling_{i}"}})


def shaped(name, pattern, key, result, count=1):
    write(os.path.join(DATA, "recipes", name + ".json"),
          {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": key,
           "result": {"item": result, "count": count}})


def shapeless(name, items, result, count=1):
    write(os.path.join(DATA, "recipes", name + ".json"),
          {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": items, "result": {"item": result, "count": count}})


def recipes():
    I = lambda i: {"item": i}
    write(os.path.join(DATA, "tags", "items", "pelts.json"), {"replace": False, "values": [
        "realmlord:wolf_pelt", "realmlord:boar_hide", "realmlord:leopard_pelt", "realmlord:tiger_pelt"]})
    P, S, B, R, ST = {"tag": "realmlord:pelts"}, I("realmlord:beast_sinew"), I("realmlord:black_iron_ingot"), I("realmlord:refined_steel"), I("minecraft:stick")
    shapeless("black_iron_ingot", [I("minecraft:iron_ingot"), I("minecraft:iron_ingot"), I("minecraft:coal")], "realmlord:black_iron_ingot")
    shapeless("refined_steel", [B, B, I("realmlord:tiger_bone")], "realmlord:refined_steel")
    shaped("black_iron_sabre", [" B", " B", "S "], {"B": B, "S": ST}, "realmlord:black_iron_sabre")
    shaped("black_iron_spear", ["  B", " S ", "S  "], {"B": B, "S": ST}, "realmlord:black_iron_spear")
    shaped("steel_jian", [" R ", " R ", "WSW"], {"R": R, "S": ST, "W": S}, "realmlord:steel_jian")
    shaped("guandao", ["RR ", "RS ", "  S"], {"R": R, "S": ST}, "realmlord:guandao")
    shaped("tiger_bone_bow", [" TW", "T W", " TW"], {"T": I("realmlord:tiger_bone"), "W": S}, "realmlord:tiger_bone_bow")
    shapeless("steel_jian_core", [I("realmlord:steel_jian"), I("realmlord:beast_core")], "realmlord:steel_jian")
    shapeless("guandao_core", [I("realmlord:guandao"), I("realmlord:beast_core")], "realmlord:guandao")
    shapeless("black_iron_sabre_core", [I("realmlord:black_iron_sabre"), I("realmlord:beast_core")], "realmlord:black_iron_sabre")
    shaped("hide_helmet", ["PPP", "PWP"], {"P": P, "W": S}, "realmlord:hide_helmet")
    shaped("hide_jerkin", ["PWP", "PPP", "PPP"], {"P": P, "W": S}, "realmlord:hide_jerkin")
    shaped("hide_leggings", ["PPP", "PWP", "P P"], {"P": P, "W": S}, "realmlord:hide_leggings")
    shaped("hide_boots", ["PWP", "P P"], {"P": P, "W": S}, "realmlord:hide_boots")
    shaped("black_iron_helmet", ["BBB", "B B"], {"B": B}, "realmlord:black_iron_helmet")
    shaped("black_iron_cuirass", ["B B", "BBB", "BBB"], {"B": B}, "realmlord:black_iron_cuirass")
    shaped("black_iron_greaves", ["BBB", "B B", "B B"], {"B": B}, "realmlord:black_iron_greaves")
    shaped("black_iron_boots", ["B B", "B B"], {"B": B}, "realmlord:black_iron_boots")
    # Lamellar: scales punched from black iron and laced with sinew.
    L, C, T, TB = I("realmlord:lamellar_plates"), I("realmlord:beast_core"), I("realmlord:tiger_pelt"), I("realmlord:tiger_bone")
    LE = I("minecraft:leather")
    shapeless("lamellar_plates", [B, B, S], "realmlord:lamellar_plates", 4)
    shaped("lamellar_helmet", ["LLL", "LWL"], {"L": L, "W": S}, "realmlord:lamellar_helmet")
    shaped("lamellar_coat", ["LWL", "LLL", "LEL"], {"L": L, "W": S, "E": LE}, "realmlord:lamellar_coat")
    shaped("lamellar_tassets", ["ELE", "LWL", "L L"], {"L": L, "W": S, "E": LE}, "realmlord:lamellar_tassets")
    shaped("lamellar_boots", ["LWL", "E E"], {"L": L, "W": S, "E": LE}, "realmlord:lamellar_boots")
    # Mountain pattern: interlocking refined steel scales over lamellar, a red silk tassel.
    D = I("minecraft:red_dye")
    shaped("mountain_helm", ["RDR", "R R"], {"R": R, "D": D}, "realmlord:mountain_helm")
    shaped("mountain_armour", ["R R", "RLR", "RDR"], {"R": R, "L": L, "D": D}, "realmlord:mountain_armour")
    shaped("mountain_greaves", ["RLR", "R R", "R R"], {"R": R, "L": L}, "realmlord:mountain_greaves")
    shaped("mountain_boots", ["L L", "R R"], {"R": R, "L": L}, "realmlord:mountain_boots")
    # The tiger set: the pelt over steel, with a beast core sewn in.
    shaped("tiger_hood", ["TCT", "T T"], {"T": T, "C": C}, "realmlord:tiger_hood")
    shaped("tiger_war_coat", ["T T", "RCR", "TRT"], {"T": T, "C": C, "R": R}, "realmlord:tiger_war_coat")
    # Weapons.
    shaped("ring_dao", ["  B", " B ", "N  "], {"B": B, "N": I("minecraft:gold_nugget")}, "realmlord:ring_dao")
    shaped("ji", [" RR", " SR", "S  "], {"R": R, "S": ST}, "realmlord:ji")
    shaped("tiger_soul_sabre", [" R", "CR", "T "], {"R": R, "C": C, "T": TB}, "realmlord:tiger_soul_sabre")
    shapeless("ring_dao_core", [I("realmlord:ring_dao"), C], "realmlord:ring_dao")
    shapeless("ji_core", [I("realmlord:ji"), C], "realmlord:ji")
    # Sinew strings bows and crossbows.
    shaped("bow_from_sinew", [" TW", "T W", " TW"], {"T": ST, "W": S}, "minecraft:bow")
    shaped("crossbow_from_sinew", ["TIT", "WHW", " T "], {"T": ST, "I": I("minecraft:iron_ingot"), "W": S, "H": I("minecraft:tripwire_hook")}, "minecraft:crossbow")
    # Tools of the trade and medicine.
    shapeless("whetstone", [I("minecraft:flint"), I("minecraft:cobblestone"), I("minecraft:cobblestone")], "realmlord:whetstone", 2)
    shapeless("refining_stone", [C, B, B, S], "realmlord:refining_stone")
    H = I("realmlord:medicinal_herbs")
    shapeless("golden_wound_powder", [H, H, I("minecraft:bone_meal"), I("minecraft:red_mushroom")], "realmlord:golden_wound_powder", 2)
    G = I("realmlord:golden_wound_powder")
    shapeless("healing_pill", [G, G, S, I("minecraft:glistering_melon_slice")], "realmlord:healing_pill", 2)
    shapeless("marrow_cleansing_pill", [C, I("realmlord:healing_pill"), I("realmlord:healing_pill"), TB, I("minecraft:gold_ingot")], "realmlord:marrow_cleansing_pill")
    shapeless("enlightenment_pill", [C, I("realmlord:healing_pill"), I("realmlord:healing_pill"), I("realmlord:three_flower_wine"), I("minecraft:gold_ingot")],
              "realmlord:enlightenment_pill")
    write(os.path.join(DATA, "tags", "items", "cooked_meats.json"), {"replace": False, "values": [
        "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_mutton", "minecraft:cooked_chicken", "minecraft:cooked_rabbit"]})
    shapeless("military_rations", [I("minecraft:bread"), I("minecraft:bread"), I("realmlord:salt"), {"tag": "realmlord:cooked_meats"}], "realmlord:military_rations", 4)


LANG = {
    "item.realmlord.beast_sinew": "Beast Sinew",
    "item.realmlord.beast_core": "Beast Core",
    "item.realmlord.bandit_insignia": "Bandit Insignia",
    "item.realmlord.black_iron_ingot": "Black Iron Ingot",
    "item.realmlord.refined_steel": "Refined Steel",
    "item.realmlord.bandit_sabre": "Bandit Sabre",
    "item.realmlord.black_iron_sabre": "Black Iron Sabre",
    "item.realmlord.black_iron_spear": "Black Iron Spear",
    "item.realmlord.steel_jian": "Refined Steel Jian",
    "item.realmlord.guandao": "Guandao",
    "item.realmlord.tiger_bone_bow": "Tiger Bone Bow",
    "item.realmlord.hide_helmet": "Hide Cap",
    "item.realmlord.hide_jerkin": "Hide Jerkin",
    "item.realmlord.hide_leggings": "Hide Leggings",
    "item.realmlord.hide_boots": "Hide Boots",
    "item.realmlord.black_iron_helmet": "Black Iron Helm",
    "item.realmlord.black_iron_cuirass": "Black Iron Cuirass",
    "item.realmlord.black_iron_greaves": "Black Iron Greaves",
    "item.realmlord.black_iron_boots": "Black Iron Boots",
    "itemGroup.realmlord.blueprints": "Realm Lord: Blueprints",
    "itemGroup.realmlord.materials": "Realm Lord: Materials",
    "itemGroup.realmlord.arms": "Realm Lord: Weapons & Armour",
    "itemGroup.realmlord.creatures": "Realm Lord: Creatures & Blocks",
    "realmlord.market.armoury": "Armoury...",
    "realmlord.market.armoury.tip": "Buy smithing materials and graded arms and armour.",
    "realmlord.market.special.title": "Special Items",
    "realmlord.market.special.buy_tab": "Armoury",
    "realmlord.market.special.sell_tab": "Sell from pack",
    "realmlord.market.special.nothing": "Nothing in your pack is wanted here. The market buys graded gear, blueprints, manuals, beast cores, sinew, bandit insignia, black iron, refined steel, plates, pills and rations.",
    "realmlord.market.sell_special": "Sell special items...",
    "realmlord.market.sell_special.tip": "Choose which graded gear, blueprints and materials from your pack to sell, one by one or all at once.",
    "item.realmlord.ring_dao": "Ring-Pommel Dao",
    "item.realmlord.ji": "Ji Halberd",
    "item.realmlord.tiger_soul_sabre": "Tiger Soul Sabre",
    "item.realmlord.lamellar_plates": "Lamellar Plates",
    "item.realmlord.whetstone": "Whetstone",
    "item.realmlord.refining_stone": "Refining Stone",
    "item.realmlord.manual": "Manual",
    "item.realmlord.manual.named": "Manual: %s",
    "item.realmlord.golden_wound_powder": "Golden Wound Powder",
    "item.realmlord.healing_pill": "Rejuvenation Pill",
    "item.realmlord.marrow_cleansing_pill": "Marrow Cleansing Pill",
    "item.realmlord.enlightenment_pill": "Enlightenment Pill",
    "item.realmlord.military_rations": "Military Rations",
    "item.realmlord.lamellar_helmet": "Lamellar Helm",
    "item.realmlord.lamellar_coat": "Lamellar Coat",
    "item.realmlord.lamellar_tassets": "Lamellar Tassets",
    "item.realmlord.lamellar_boots": "Lamellar Boots",
    "item.realmlord.mountain_helm": "Mountain-Pattern Helm",
    "item.realmlord.mountain_armour": "Mountain-Pattern Armour",
    "item.realmlord.mountain_greaves": "Mountain-Pattern Greaves",
    "item.realmlord.mountain_boots": "Mountain-Pattern War Boots",
    "item.realmlord.tiger_hood": "Tiger-Head Hood",
    "item.realmlord.tiger_war_coat": "Tiger-Hide War Coat",
    # Manuals
    "realmlord.plan.rations": "Field Rations",
    "realmlord.plan.rations.desc": "How an army's dry rations are baked, salted and packed. The grocer can make Military Rations.",
    "realmlord.plan.lamellar": "Lamellar Armour",
    "realmlord.plan.lamellar.desc": "Scales punched, rowed and laced. The smithy can make lamellar armour.",
    "realmlord.plan.polearms": "Polearms",
    "realmlord.plan.polearms.desc": "Spear, guandao and ji. The weapons workshop can forge polearms.",
    "realmlord.plan.alchemy": "Pill Refining",
    "realmlord.plan.alchemy.desc": "A Taoist's furnace notes. The clinic can refine Rejuvenation, Marrow Cleansing and Enlightenment pills.",
    "realmlord.plan.mountain_pattern": "Mountain-Pattern Armour",
    "realmlord.plan.mountain_pattern.desc": "The generals' armour of interlocking steel. The weapons workshop can make it.",
    "realmlord.plan.tiger_war": "Tiger War Gear",
    "realmlord.plan.tiger_war.desc": "The Tiger Soul Sabre and the tiger-hide war coat. Needs beast cores and tiger pelts in the storehouse.",
    "realmlord.plan.tooltip": "Right-click to teach it to your village's masters.",
    "realmlord.plan.learned": "Your masters have learned the manual: %s.",
    "realmlord.plan.known": "Your village already knows %s.",
    # Items in use
    "realmlord.pill.wound_powder.desc": "Closes wounds: heals 2 hearts and a little more.",
    "realmlord.pill.healing.desc": "Heals 4 hearts at once, keeps healing, and shields for a minute.",
    "realmlord.pill.marrow.desc": "A special pill: raises Body Structure by 1 (up to 30).",
    "realmlord.pill.enlightenment.desc": "A special pill: raises Comprehension by 1 (up to 30).",
    "realmlord.pill.raised": "%s rises to %s.",
    "realmlord.pill.capped": "%s is already %s; the pill can do no more.",
    "realmlord.whetstone.tip": "Right-click with a blade or armour in the other hand: mends a quarter of its wear.",
    "realmlord.whetstone.nothing": "Hold something worn in your other hand.",
    "realmlord.refine.tip": "Right-click with gear in the other hand to raise it a grade (ungraded becomes Bronze). The higher the grade, the likelier it fails; failure costs only the stone.",
    "realmlord.refine.nothing": "Hold a weapon or armour below Gold grade in your other hand.",
    "realmlord.refine.done": "Refined: %s.",
    "realmlord.refine.failed": "The refining fails; the stone crumbles. Your gear is unharmed.",
    "realmlord.insignia.tip": "Right-click in your own territory to post the kills: 2 reputation and a 50 copper bounty each; Security rises.",
    "realmlord.insignia.no_village": "Only a Lord with a village can post bounties.",
    "realmlord.insignia.not_here": "Post them in your own territory.",
    "realmlord.insignia.done": "Posted %s bandit insignia: +%s reputation, %s bounty, Security +%s.",
    # Building windows
    "realmlord.workshop.teaches_what": "What it does",
    "realmlord.workshop.needs_plan": "Needs the manual: %s",
    "realmlord.workshop.err.plan": "Your village hasn't learned the manual for that.",
    "realmlord.workshop.and_kinds": "%s and %s other kind(s)",
    "realmlord.workshop.makes_named": "Each day: %s %s into the storehouse.",
    "realmlord.workshop.makes_named_from": "Each day: %s %s into the storehouse, from %s %s each time. Arms and armour are graded by the master's skill.",
    "realmlord.workshop.also_needs": "Also uses %s %s from the storehouse each time (%s there).",
    "realmlord.workshop.mode.black_iron": "Black iron ingots",
    "realmlord.workshop.mode.whetstones": "Whetstones",
    "realmlord.workshop.mode.lamellar_plates": "Lamellar plates",
    "realmlord.workshop.mode.black_iron_arms": "Black iron blades",
    "realmlord.workshop.mode.lamellar_armour": "Lamellar armour",
    "realmlord.workshop.mode.horse_armour": "Horse barding",
    "realmlord.workshop.mode.arrows": "Arrows",
    "realmlord.workshop.mode.refined_steel": "Refined steel",
    "realmlord.workshop.mode.steel_blades": "Steel blades",
    "realmlord.workshop.mode.bows": "Tiger bone bows",
    "realmlord.workshop.mode.polearms": "Polearms",
    "realmlord.workshop.mode.mountain_armour": "Mountain armour",
    "realmlord.workshop.mode.tiger_soul": "Tiger Soul Sabre",
    "realmlord.workshop.mode.hide_armour": "Hide armour",
    "realmlord.workshop.mode.banners": "Banners",
    "realmlord.workshop.mode.bedding": "Bedding",
    "realmlord.workshop.mode.tiger_war_coat": "Tiger war coats",
    "realmlord.workshop.mode.wound_powder": "Wound powder",
    "realmlord.workshop.mode.healing_pills": "Rejuvenation pills",
    "realmlord.workshop.mode.marrow_pills": "Marrow pills",
    "realmlord.workshop.mode.enlightenment_pills": "Enlightenment pills",
    "realmlord.workshop.mode.wine": "Wine (stock)",
    "realmlord.workshop.mode.tiger_bone_wine": "Tiger bone wine",
    "realmlord.workshop.mode.bottled_wine": "Bottled wine",
    "realmlord.workshop.mode.salt": "Salt (stock)",
    "realmlord.workshop.mode.rations": "Military rations",
    "realmlord.workshop.mode.cured_meat": "Cured meat",
    "realmlord.workshop.mode.dried_fish": "Dried fish",
    "realmlord.workshop.mode.pottery": "Pottery (stock)",
    "realmlord.workshop.mode.bricks": "Bricks",
    "realmlord.workshop.mode.roof_tiles": "Roof tiles",
    "realmlord.workshop.mode.flower_pots": "Pots",
    "realmlord.workshop.mode.tend_horses": "Tend the horses",
    "realmlord.workshop.mode.tend_horses.desc": "The stableman looks after the village's horses and makes nothing else.",
    "realmlord.workshop.mode.saddles": "Saddles",
    "realmlord.workshop.mode.leads": "Leads",
    "realmlord.workshop.mode.leather_barding": "Leather barding",
    "realmlord.workshop.mode.fist_and_palm": "Fist & palm",
    "realmlord.workshop.mode.fist_and_palm.desc": "The foundation: pupils gain 1 Force a day and steady soldier's experience. They become martial artists.",
    "realmlord.workshop.mode.weapon_forms": "Weapon forms",
    "realmlord.workshop.mode.weapon_forms.desc": "Sabre and spear forms: twice the soldier's experience, Force rises more slowly. They become martial artists.",
    "realmlord.workshop.mode.internal_arts": "Internal arts",
    "realmlord.workshop.mode.internal_arts.desc": "Breathing and Qi cultivation: Force rises fast (up to 92), little soldier's experience, and the lessons take 25 days.",
    "realmlord.workshop.mode.archery": "Archery",
    "realmlord.workshop.mode.archery.desc": "The bow: some Force and soldier's experience. Pupils become hunters.",
    "realmlord.workshop.mode.qinggong": "Qinggong",
    "realmlord.workshop.mode.qinggong.desc": "Lightness skill: a short course of 15 days. Pupils become martial artists sooner but less strong.",
    "realmlord.workshop.mode.classics": "Classics",
    "realmlord.workshop.mode.classics.desc": "The Four Books: Intelligence and Politics. Students become scholars.",
    "realmlord.workshop.mode.statecraft": "Statecraft",
    "realmlord.workshop.mode.statecraft.desc": "Law and government: Politics above all. Students become civil servants, fit for office.",
    "realmlord.workshop.mode.strategy": "Military strategy",
    "realmlord.workshop.mode.strategy.desc": "Sun Tzu and the military classics: Command and Intelligence, 25 days. Students become officers.",
    "realmlord.workshop.mode.medicine": "Medicine",
    "realmlord.workshop.mode.medicine.desc": "Herbs and pulse-reading: Intelligence. Students become doctors for the clinic.",
    "realmlord.workshop.mode.accounts": "Accounts",
    "realmlord.workshop.mode.accounts.desc": "The abacus and the ledger. Students become grocers.",
    "realmlord.workshop.mode.drill": "Drill",
    "realmlord.workshop.mode.drill.desc": "The ordinary daily drill: Force and soldier's experience for the militia.",
    "realmlord.workshop.mode.formation": "Formation drill",
    "realmlord.workshop.mode.formation.desc": "Marching in ranks: far more soldier's experience, less Force; officers' Command grows faster.",
    "realmlord.workshop.mode.weapons_practice": "Weapons practice",
    "realmlord.workshop.mode.weapons_practice.desc": "Sparring: Force grows twice as fast, a little less experience.",
    "realmlord.workshop.mode.archery_drill": "Archery drill",
    "realmlord.workshop.mode.archery_drill.desc": "At the butts: a little more of both Force and experience.",
    "realmlord.workshop.mode.patrol": "Patrol",
    "realmlord.workshop.mode.patrol.desc": "The militia walks the lanes and the wall: Security rises each day, but they drill little.",
    "realmlord.workshop.mode.rest": "Rest",
    "realmlord.workshop.mode.rest.desc": "A day off: no drill, and every soldier's loyalty rises.",
}


def main():
    textures()
    models()
    recipes()
    print("arms written")


if __name__ == "__main__":
    main()
