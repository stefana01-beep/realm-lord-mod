"""Textures, models and text for the novel's goods (salt, herbs, Three Flower Wine, linen,
fine silk, fire oil) and the Lord's Handbook."""
import os
from PIL import Image

NAMES = ["salt", "medicinal_herbs", "three_flower_wine", "linen", "fine_silk", "fire_oil", "village_guide"]


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def blank():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, img.load()


def salt():
    img, px = blank()
    cloth, tie = hexc("c9b48a"), hexc("7a5a33")
    for y in range(5, 15):
        half = 3 + min(y - 5, 3) if y < 12 else 6 - (y - 12)
        for x in range(8 - half, 8 + half):
            px[x, y] = shade(cloth, 1.08 - 0.05 * abs(x - 7) / 3)
    for x in range(5, 11): px[x, 5] = tie
    for x, y in [(6, 3), (7, 2), (8, 3), (9, 4), (7, 4), (8, 4)]: px[x, y] = hexc("f4f4f0")   # salt heaped at the mouth
    for x, y in [(6, 9), (9, 10), (7, 12), (10, 8)]: px[x, y] = shade(cloth, 0.8)
    return img


def herbs():
    img, px = blank()
    leaf, dark, stem, string = hexc("5d9b3a"), hexc("3d6f22"), hexc("8a6a3a"), hexc("c8a96a")
    for i in range(6):
        for t in range(9):
            x, y = 3 + i + (t // 3) * (1 if i % 2 else -1) // 2, 3 + t
            if 0 <= x < 16: px[x, y] = leaf if (t + i) % 3 else dark
    for y in range(11, 15):
        for x in range(6, 10): px[x, y] = stem
    for x in range(5, 11): px[x, 11] = string
    for x, y in [(4, 2), (9, 1), (11, 3), (2, 4)]: px[x, y] = leaf
    return img


def wine():
    img, px = blank()
    jar, rim, wine_c, label = hexc("d8d0c0"), hexc("8a6a3a"), hexc("9e2f45"), hexc("c8423b")
    for y in range(4, 15):
        half = 2 if y < 6 else 4 if y < 13 else 3
        for x in range(8 - half, 8 + half):
            light = 1.1 if x < 7 else 0.9
            px[x, y] = shade(jar, light)
    for x in range(6, 10): px[x, 3] = rim
    for x in range(7, 9): px[x, 2] = shade(rim, 1.2)
    for y in range(8, 12):
        for x in range(6, 10): px[x, y] = label
    px[7, 9] = px[8, 10] = hexc("f2d06b")                       # the brand: three flowers
    px[6, 10] = hexc("f2d06b")
    px[5, 6] = shade(wine_c, 1.0)
    return img


def roll(base, sheen, gold=False):
    img, px = blank()
    for y in range(4, 13):
        for x in range(2, 14):
            band = (x - 2) % 4
            c = shade(base, 1.0 + (0.12 if band == 0 else -0.06 if band == 2 else 0))
            if y in (4, 12): c = shade(base, 0.78)
            px[x, y] = c
    for y in range(5, 12):
        px[2, y] = shade(base, 0.7)
        px[13, y] = shade(sheen, 1.0)
    for x in range(3, 13, 3): px[x, 6] = sheen
    if gold:
        for x in range(2, 14): px[x, 8] = hexc("e8c15a")
    return img


def fire_oil():
    img, px = blank()
    wood, hoop, flame = hexc("8a5a2e"), hexc("4b4b4b"), hexc("ff7a1a")
    for y in range(5, 15):
        bulge = 1 if 7 <= y <= 12 else 0
        for x in range(4 - bulge, 12 + bulge):
            px[x, y] = shade(wood, 1.1 if x < 7 else 0.9 if x > 9 else 1.0)
        if y in (6, 13): 
            for x in range(4 - bulge, 12 + bulge): px[x, y] = hoop
    for x, y, c in [(7, 4, flame), (8, 4, flame), (8, 3, hexc("ffd23a")), (7, 2, hexc("ffd23a")), (9, 3, flame), (6, 3, hexc("e04a1a"))]:
        px[x, y] = c
    for y in range(8, 12): px[8, y] = hexc("c8423b")             # a red seal: fire
    return img


def guide():
    img, px = blank()
    cover, edge, pages, gold = hexc("7a2a24"), hexc("4f1915"), hexc("efe6cf"), hexc("e8c15a")
    for y in range(2, 15):
        for x in range(3, 13):
            px[x, y] = cover
        px[3, y] = edge
        px[13, y] = pages
    for x in range(4, 13): px[x, 14] = pages
    for x in range(6, 11):                                         # a square seal on the cover
        for y in range(5, 10):
            if x in (6, 10) or y in (5, 9): px[x, y] = gold
    px[8, 7] = gold
    for x in range(5, 12): px[x, 3] = shade(cover, 1.25)
    return img


def textures(item_dir):
    salt().save(os.path.join(item_dir, "salt.png"))
    herbs().save(os.path.join(item_dir, "medicinal_herbs.png"))
    wine().save(os.path.join(item_dir, "three_flower_wine.png"))
    roll(hexc("e8e0cc"), hexc("ffffff")).save(os.path.join(item_dir, "linen.png"))
    roll(hexc("b8283a"), hexc("ff9aa8"), gold=True).save(os.path.join(item_dir, "fine_silk.png"))
    fire_oil().save(os.path.join(item_dir, "fire_oil.png"))
    guide().save(os.path.join(item_dir, "village_guide.png"))


def models(write, assets):
    for n in NAMES:
        write(os.path.join(assets, "models", "item", f"{n}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{n}"}})


GUIDE = [
    "§lThe Lord's Handbook§r\n\nGaia is a world where Lords found villages, raise towns and gather the talents of history.\n\nThis book covers the village: founding it, building it and keeping it.",
    "§lFounding§r\n\nFinish the Village Creation quest (Quest tab), then use the token on open ground and name your village.\n\nThe Lord's Manor rises there. Right-click its seal to run your land.",
    "§lThe Manor§r\n\nOverview: your people, mood, safety, treasury and the next upgrade.\n\nConstruction, People, Market and Records, and the Map button, are along the top.",
    "§lBuilding§r\n\nPick a building in Construction and place it on the map or in the world. Wood and stone come from the stores.\n\nBuilders clear, level and raise it block by block. More builders, faster work.",
    "§lBlueprints§r\n\nMost buildings need a blueprint. Right-click one to teach it to your village for good; then build as many as you like.\n\nBuy them on the market's Special Items platform.",
    "§lThe Map§r\n\nPress M for the world map.\n\nThe manor's map shows your land. From it: lay paths, clear and level ground, set fence gates, and pick a building to demolish.",
    "§lThe Fence§r\n\nA plank palisade around your border, with gates where you choose. Once it stands, no monsters spawn inside.\n\nArrow towers placed near it stand in the wall. Towns add a walkway on top.",
    "§lPeople§r\n\nResidents need beds: ten in the manor at first, then 20, 50 or 100 per courtyard. Newcomers only come to a free bed.\n\nRight-click a resident for their card.",
    "§lThe Day§r\n\nA village day is a Minecraft day. Sleep and the next day comes at once: work is paid, food eaten, taxes in, newcomers arrive.\n\nThe Records tab shows yesterday's ledger and the village log.",
    "§lFood§r\n\nEveryone eats one food a day: grain first, then flour, meat and fish.\n\nSow farmland in month 4 or 7 for a full harvest three months later. Watch Records for how long the food lasts.",
    "§lThe Market§r\n\nBuy and sell goods at the novel's prices. Take puts goods from the stores into your pack (shift for a stack); Hand In puts them back.\n\nThe storehouse chest keeps anything else.",
    "§lGoods§r\n\nThree Flower Wine: drink for strength.\nMedicinal Herbs: eat to heal.\nFire Oil: throw it to set the ground alight.\nSalt, linen and fine silk: trade goods.\n\nWorkshops make them each day.",
    "§lYour Land§r\n\nOnly you and those you trust may build or open chests here.\n\n/realm trust <name>\n/realm untrust <name>\n/realm trusted\n\nThe manor's chests are yours alone.",
    "§lGrowing§r\n\nUpgrade from the Overview when you have the people, title and buildings. At Grade 2 the manor becomes your own residence.\n\nLost this book? /realm guide",
]


def lang(L):
    L.update({
        "item.realmlord.salt": "Salt",
        "item.realmlord.medicinal_herbs": "Medicinal Herbs",
        "item.realmlord.three_flower_wine": "Three Flower Wine",
        "item.realmlord.linen": "Roll of Linen",
        "item.realmlord.fine_silk": "Roll of Fine Silk",
        "item.realmlord.fire_oil": "Barrel of Fire Oil",
        "item.realmlord.village_guide": "Lord's Handbook",
        "entity.realmlord.fire_oil": "Fire Oil",
        "realmlord.guide.tooltip": "Right-click to read how to found, build and run a village.",
        "realmlord.market.take": "Take",
        "realmlord.market.take.tip": "Take one %s (%s units) into your pack. Shift-click for a stack.",
        "realmlord.market.taken": "Took %s × %s from the stores.",
    })
    for i, page in enumerate(GUIDE, 1):
        L[f"realmlord.guide.page.{i}"] = page


# --------------------------------------------------------------------------- beast drops

BEAST_ITEMS = ["boar_hide", "boar_tusk", "boar_king_tusk", "wolf_pelt", "tiger_pelt", "tiger_bone", "bear_paw",
               "braised_bear_paw", "tiger_bone_wine"]
WEAPONS = ["tusk_spear", "boar_king_spear"]
EGGS = ["wild_boar", "boar_king", "grey_wolf", "mountain_tiger", "snow_bear"]


def hide(base, stripes=None):
    img, px = blank()
    for y in range(3, 14):
        w = 5 if y in (3, 13) else 6
        for x in range(8 - w, 8 + w):
            c = shade(base, 1.05 - 0.04 * abs(x - 8) / 3 + (0.06 if (x + y) % 3 == 0 else 0))
            if stripes and (x + y // 2) % 4 == 0: c = stripes
            px[x, y] = c
    for x, y in [(2, 3), (13, 3), (2, 13), (13, 13), (1, 6), (14, 9)]: px[x, y] = shade(base, 0.8)   # the legs of the hide
    return img


def tusk(col, big=False):
    img, px = blank()
    pts = [(3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 4), (12, 3)]
    for i, (x, y) in enumerate(pts):
        px[x, y] = shade(col, 1.1)
        if i < len(pts) - 2 or big: px[x, y + 1] = shade(col, 0.85)
        if big and i < len(pts) - 3: px[x + 1, y + 1] = shade(col, 0.95)
    px[2, 13] = px[3, 13] = hexc("8a6a3a")
    if big:
        for x, y in [(4, 13), (2, 12)]: px[x, y] = hexc("e8c15a")
    return img


def bone():
    img, px = blank()
    c = hexc("ece4cc")
    for i in range(4, 12): px[i, 15 - i] = c; px[i + 1, 15 - i] = shade(c, 0.85)
    for x, y in [(3, 12), (3, 11), (4, 13), (11, 3), (12, 4), (12, 3)]: px[x, y] = c
    return img


def paw(base, cooked=False):
    img, px = blank()
    for y in range(6, 14):
        for x in range(4, 12):
            if (x - 7.5) ** 2 + (y - 10) ** 2 < 15: px[x, y] = shade(base, 1.0 + 0.1 * ((x + y) % 2))
    for cx in (4, 7, 10, 12):
        for x, y in [(cx, 4), (cx, 5)]:
            if 0 <= x < 16: px[x, y] = shade(base, 0.85)
    if cooked:
        for x, y in [(6, 9), (9, 11), (7, 12)]: px[x, y] = hexc("f2d06b")
    return img


def spear(head, big=False):
    img, px = blank()
    shaft = hexc("8a5a2e")
    for i in range(2, 12): px[i, 15 - i] = shaft
    for x, y in [(11, 4), (12, 3), (13, 2), (12, 2), (13, 3), (14, 1)]: px[x, y] = head
    if big:
        for x, y in [(10, 3), (14, 4), (11, 2), (13, 4)]: px[x, y] = shade(head, 0.9)
        px[9, 6] = px[10, 5] = hexc("c8423b")
    return img


def beast_textures(item_dir):
    hide(hexc("6b4a2e")).save(os.path.join(item_dir, "boar_hide.png"))
    tusk(hexc("efe6cf")).save(os.path.join(item_dir, "boar_tusk.png"))
    tusk(hexc("f6edd2"), big=True).save(os.path.join(item_dir, "boar_king_tusk.png"))
    hide(hexc("8a8a8a")).save(os.path.join(item_dir, "wolf_pelt.png"))
    hide(hexc("e08a2a"), stripes=hexc("1e1a16")).save(os.path.join(item_dir, "tiger_pelt.png"))
    bone().save(os.path.join(item_dir, "tiger_bone.png"))
    paw(hexc("3a2a22")).save(os.path.join(item_dir, "bear_paw.png"))
    paw(hexc("9a4a22"), cooked=True).save(os.path.join(item_dir, "braised_bear_paw.png"))
    img = wine()
    p = img.load()
    for y in range(8, 12):
        for x in range(6, 10): p[x, y] = hexc("2a2a2a")
    p[7, 9] = p[8, 10] = hexc("ece4cc")
    img.save(os.path.join(item_dir, "tiger_bone_wine.png"))
    spear(hexc("efe6cf")).save(os.path.join(item_dir, "tusk_spear.png"))
    spear(hexc("f6edd2"), big=True).save(os.path.join(item_dir, "boar_king_spear.png"))


def beast_models(write, assets):
    for n in BEAST_ITEMS:
        write(os.path.join(assets, "models", "item", f"{n}.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{n}"}})
    for n in WEAPONS:
        write(os.path.join(assets, "models", "item", f"{n}.json"),
              {"parent": "minecraft:item/handheld", "textures": {"layer0": f"realmlord:item/{n}"}})
    for n in EGGS:
        write(os.path.join(assets, "models", "item", f"{n}_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})


def beast_lang(L):
    L.update({
        "entity.realmlord.wild_boar": "Wild Boar",
        "entity.realmlord.boar_king": "Boar King",
        "entity.realmlord.grey_wolf": "Grey Wolf",
        "entity.realmlord.mountain_tiger": "Mountain Tiger",
        "entity.realmlord.snow_bear": "Snow Bear",
        "item.realmlord.boar_hide": "Boar Hide",
        "item.realmlord.boar_tusk": "Boar Tusk",
        "item.realmlord.boar_king_tusk": "Boar King's Tusk",
        "item.realmlord.wolf_pelt": "Wolf Pelt",
        "item.realmlord.tiger_pelt": "Tiger Pelt",
        "item.realmlord.tiger_bone": "Tiger Bone",
        "item.realmlord.bear_paw": "Bear Paw",
        "item.realmlord.braised_bear_paw": "Braised Bear Paw",
        "item.realmlord.tiger_bone_wine": "Tiger Bone Wine",
        "item.realmlord.tusk_spear": "Tusk Spear",
        "item.realmlord.boar_king_spear": "Boar King's Spear",
        "item.realmlord.wild_boar_spawn_egg": "Wild Boar Spawn Egg",
        "item.realmlord.boar_king_spawn_egg": "Boar King Spawn Egg",
        "item.realmlord.grey_wolf_spawn_egg": "Grey Wolf Spawn Egg",
        "item.realmlord.mountain_tiger_spawn_egg": "Mountain Tiger Spawn Egg",
        "item.realmlord.snow_bear_spawn_egg": "Snow Bear Spawn Egg",
    })
