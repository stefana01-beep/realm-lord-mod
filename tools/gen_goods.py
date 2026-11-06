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
