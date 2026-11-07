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
          "black_iron": ["black_iron_helmet", "black_iron_cuirass", "black_iron_greaves", "black_iron_boots"]}

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


def armour_icon(kind, slot, col):
    img, px = blank()
    trim = hexc("c8a24a") if kind == "black_iron" else hexc("4a3020")
    shapes = {
        "helmet": [(x, y) for y in range(3, 10) for x in range(3, 13) if not (y > 6 and 5 <= x <= 10)],
        "chest": [(x, y) for y in range(2, 14) for x in range(2, 14) if not (y < 5 and 5 <= x <= 10) and not (y > 6 and (x < 4 or x > 11))],
        "legs": [(x, y) for y in range(2, 15) for x in range(3, 13) if not (y > 5 and 7 <= x <= 8)],
        "boots": [(x, y) for y in range(7, 14) for x in range(2, 14) if not (6 <= x <= 9) and not (y < 10 and (x < 3 or x > 12))],
    }[slot]
    for (x, y) in shapes:
        px[x, y] = shade(col, 1.15 - 0.03 * y + (0.06 if (x * 7 + y * 3) % 5 == 0 else 0))
    edge = {"helmet": 3, "chest": 2, "legs": 2, "boots": 7}[slot]
    for (x, y) in shapes:
        if y == edge: px[x, y] = trim
    return img


def armour_layer(col, trim, layer, seed):
    """The worn armour: the humanoid layout, filled in where the armour covers."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()

    def box(u, v, w, h, d):
        # A cuboid's unwrapped faces (as in the player model): top/bottom row, then the four sides.
        regions = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)]
        for (x0, y0, ww, hh) in regions:
            for y in range(y0, y0 + hh):
                for x in range(x0, x0 + ww):
                    if 0 <= x < 64 and 0 <= y < 32:
                        f = 1.0 + rnd.uniform(-0.08, 0.08) - (0.15 if y in (y0, y0 + hh - 1) else 0)
                        px[x, y] = shade(col, f)
            for x in range(x0, x0 + ww):
                if 0 <= x < 64 and 0 <= y0 + hh - 1 < 32: px[x, y0 + hh - 1] = trim

    if layer == 1:
        box(0, 0, 8, 8, 8)       # head
        box(16, 16, 8, 12, 4)    # body
        box(40, 16, 4, 12, 4)    # arm
        box(0, 16, 4, 12, 4)     # leg (boots)
        # keep only the lower part of the legs for boots
        for y in range(16, 32):
            for x in range(0, 16):
                if y < 26 and px[x, y][3]: px[x, y] = (0, 0, 0, 0)
    else:
        box(16, 16, 8, 12, 4)    # body (the waist of the leggings)
        box(0, 16, 4, 12, 4)     # leg
        for y in range(16, 32):
            for x in range(16, 40):
                if y > 22 and px[x, y][3]: px[x, y] = (0, 0, 0, 0)
    return img


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
        col = HIDE if kind == "hide" else BLACK_IRON
        for n, slot in zip(names, ["helmet", "chest", "legs", "boots"]):
            armour_icon(kind, slot, col).save(os.path.join(item, n + ".png"))
    armour = os.path.join(ASSETS, "textures", "models", "armor")
    os.makedirs(armour, exist_ok=True)
    armour_layer(HIDE, hexc("4a3020"), 1, 1).save(os.path.join(armour, "hide_layer_1.png"))
    armour_layer(HIDE, hexc("4a3020"), 2, 2).save(os.path.join(armour, "hide_layer_2.png"))
    armour_layer(BLACK_IRON, hexc("c8a24a"), 1, 3).save(os.path.join(armour, "black_iron_layer_1.png"))
    armour_layer(BLACK_IRON, hexc("c8a24a"), 2, 4).save(os.path.join(armour, "black_iron_layer_2.png"))


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def models():
    m = os.path.join(ASSETS, "models", "item")
    for n in MATERIALS + ARMOUR["hide"] + ARMOUR["black_iron"]:
        write(os.path.join(m, n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{n}"}})
    for n in BLADES:
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
    "realmlord.market.special.nothing": "Nothing in your pack is wanted here. The market buys graded gear, blueprints, beast cores, sinew, bandit insignia, black iron and refined steel.",
    "realmlord.market.sell_special": "Sell special items...",
    "realmlord.market.sell_special.tip": "Choose which graded gear, blueprints and materials from your pack to sell, one by one or all at once.",
}


def main():
    textures()
    models()
    recipes()
    print("arms written")


if __name__ == "__main__":
    main()
