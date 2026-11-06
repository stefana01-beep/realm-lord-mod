"""Novel-themed ruins for the wilds of Gaia: a roadside shrine, a ruined watchtower and an
ancient tomb. Writes the structure templates (NBT), their worldgen (structure, structure set,
template pool, biome tags) and their chests' loot tables."""
import json, os, random
import nbtlib
from nbtlib import Compound, List, Int, String

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "realmlord")
DATA_VERSION = 3465


class Template:
    def __init__(self, sx, sy, sz, seed):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.r = random.Random(seed)

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, props)

    def brick(self, x, y, z, base="stone_bricks"):
        k = self.r.random()
        self.set(x, y, z, "mossy_stone_bricks" if k < 0.25 else "cracked_stone_bricks" if k < 0.4 else base)

    def chest(self, x, y, z, table, facing="north"):
        self.set(x, y, z, "chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 Compound({"id": String("minecraft:chest"), "LootTable": String(f"realmlord:chests/{table}")}))

    def save(self, name):
        palette, index, blocks = [], {}, []
        for pos, (bname, props, nbt) in sorted(self.blocks.items()):
            key = (bname, props)
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": String(bname if ":" in bname else "minecraft:" + bname)}
                if props:
                    entry["Properties"] = Compound({k: String(v) for k, v in props})
                palette.append(Compound(entry))
            b = {"pos": List[Int]([Int(pos[0]), Int(pos[1]), Int(pos[2])]), "state": Int(index[key])}
            if nbt is not None:
                b["nbt"] = nbt
            blocks.append(Compound(b))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List[Int]([Int(v) for v in self.size]),
            "palette": List[Compound](palette),
            "blocks": List[Compound](blocks),
            "entities": List[Compound]([]),
        })
        os.makedirs(os.path.join(ROOT, "structures"), exist_ok=True)
        nbtlib.File(root, root_name="").save(os.path.join(ROOT, "structures", f"{name}.nbt"), gzipped=True)
        return len(blocks)


def shrine():
    t = Template(9, 8, 9, 7)
    for x in range(9):
        for z in range(9):
            t.brick(x, 0, z)
            for y in range(1, 6):
                t.set(x, y, z, "air")
    # Red pillars, one broken.
    for (x, z, h) in ((1, 1, 4), (7, 1, 4), (1, 7, 4), (7, 7, 2)):
        for y in range(1, h + 1):
            t.set(x, y, z, "stripped_mangrove_log", {"axis": "y"})
    # The altar, its offerings and the chest of the shrine's keeper.
    t.set(4, 1, 6, "chiseled_stone_bricks")
    t.set(4, 2, 6, "lantern", {"hanging": "false", "waterlogged": "false"})
    t.set(3, 1, 6, "polished_andesite_stairs", {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    t.set(5, 1, 6, "polished_andesite_stairs", {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    t.chest(4, 1, 7, "ancient_shrine")
    for x in (2, 6):
        t.set(x, 1, 7, "stone_brick_wall", {"east": "none", "north": "none", "south": "none", "west": "none", "up": "true", "waterlogged": "false"})
        t.set(x, 2, 7, "flower_pot")
    # The roof: a tiled hip, fallen in places.
    for x in range(9):
        for z in range(9):
            if t.r.random() < 0.72:
                t.set(x, 5, z, "deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})
    for x in range(2, 7):
        for z in range(2, 7):
            if t.r.random() < 0.8:
                t.set(x, 5, z, "deepslate_tiles")
                if 3 <= x <= 5 and 3 <= z <= 5:
                    t.set(x, 6, z, "deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(4, 7, 4, "gold_block") if t.r.random() < 0.0 else None
    # Steps up at the front.
    for x in range(3, 6):
        t.set(x, 0, 0, "stone_brick_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    return t


def watchtower():
    t = Template(7, 15, 7, 11)
    for x in range(7):
        for z in range(7):
            t.set(x, 0, z, "cobblestone" if t.r.random() < 0.5 else "mossy_cobblestone")
    for y in range(1, 14):
        for x in range(7):
            for z in range(7):
                wall = x in (0, 6) or z in (0, 6)
                if wall:
                    # The top is broken away unevenly.
                    if y > 10 and t.r.random() < (y - 10) * 0.22:
                        continue
                    t.brick(x, y, z, "stone_bricks" if y > 3 else "cobblestone")
                else:
                    t.set(x, y, z, "air")
    # Door, windows.
    for y in (1, 2):
        t.set(3, y, 0, "air")
    for y in (7, 8):
        t.set(3, y, 6, "air")
        t.set(0, y, 3, "air")
    # Floors with a ladder well.
    for y in (5, 10):
        for x in range(1, 6):
            for z in range(1, 6):
                if not (x == 1 and z == 1):
                    t.set(x, y, z, "spruce_planks")
    for y in range(1, 11):
        t.set(1, y, 1, "ladder", {"facing": "east", "waterlogged": "false"})
    t.chest(5, 1, 5, "watchtower_supplies")
    t.chest(5, 11, 5, "watchtower")
    t.set(3, 11, 3, "campfire", {"facing": "north", "lit": "false", "signal_fire": "false", "waterlogged": "false"})
    return t


def tomb():
    # Template y 0 is the tomb floor, seven blocks under the ground; the arch stands above it.
    t = Template(15, 11, 15, 23)
    for x in range(15):
        for z in range(15):
            for y in range(0, 7):
                edge = x in (0, 14) or z in (0, 14) or y in (0, 6)
                if edge:
                    if y == 0:
                        t.set(x, y, z, "polished_andesite" if (x + z) % 2 else "andesite")
                    else:
                        t.brick(x, y, z)
                else:
                    t.set(x, y, z, "air")
    # Pillars and their lamps.
    for (x, z) in ((3, 4), (11, 4), (3, 9), (11, 9)):
        for y in range(1, 6):
            t.set(x, y, z, "chiseled_stone_bricks" if y in (1, 5) else "stone_bricks")
        t.set(x + 1, 3, z, "soul_lantern", {"hanging": "false", "waterlogged": "false"}) if False else None
        t.set(x, 5, z + 1, "air")
    for (x, z) in ((4, 4), (10, 4), (4, 9), (10, 9)):
        t.set(x, 1, z, "lantern", {"hanging": "false", "waterlogged": "false"})
    # The coffin of a lord of old.
    for z in (8, 9):
        t.set(7, 1, z, "polished_blackstone_bricks")
    t.set(7, 2, 8, "polished_blackstone_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(7, 2, 9, "polished_blackstone_slab", {"type": "bottom", "waterlogged": "false"})
    # Grave goods.
    t.chest(3, 1, 12, "ancient_tomb")
    t.chest(11, 1, 12, "ancient_tomb")
    t.chest(7, 1, 12, "tomb_treasure")
    t.set(6, 1, 12, "gold_block")
    t.set(8, 1, 12, "gold_block")
    # The stair down from the surface, three wide, and the opening above it.
    for k in range(6):
        y, z = 6 - k, 1 + k
        for x in (6, 7, 8):
            t.set(x, y, z, "stone_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for yy in range(y + 1, 10):
                t.set(x, yy, z, "air")
    for x in (6, 7, 8):
        for yy in range(7, 10):
            t.set(x, yy, 0, "air")
    # The arch over the way down.
    for y in range(7, 10):
        t.set(5, y, 1, "stone_bricks")
        t.set(9, y, 1, "stone_bricks")
    for x in range(5, 10):
        t.set(x, 10, 1, "deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})
    t.set(5, 10, 1, "lantern", {"hanging": "false", "waterlogged": "false"})
    t.set(9, 10, 1, "lantern", {"hanging": "false", "waterlogged": "false"})
    return t


def item(name, w=1, lo=1, hi=1, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": w}
    fs = list(functions or [])
    if hi > 1 or lo != 1:
        fs.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if fs:
        e["functions"] = fs
    return e


def graded(name, w, grade="random"):
    return item(name, w, functions=[{"function": "realmlord:graded", "grade": grade}])


def blueprint(w, max_grade):
    return item("realmlord:blueprint", w, functions=[{"function": "realmlord:blueprint", "max_grade": max_grade}])


def table(*pools):
    return {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries} for lo, hi, entries in pools]}


WEAPONS = ["minecraft:iron_sword", "minecraft:iron_axe", "minecraft:bow"]
ARMOUR = ["minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
          "minecraft:chainmail_chestplate", "minecraft:leather_chestplate"]

LOOT = {
    "ancient_shrine": table(
        (3, 5, [item("realmlord:copper_coin", 20, 10, 40), item("realmlord:silver_coin", 10, 1, 5), item("minecraft:iron_ingot", 8, 1, 4),
                item("minecraft:gold_nugget", 8, 2, 8), item("minecraft:wheat", 8, 4, 12), item("realmlord:medicinal_herbs", 8, 1, 4),
                item("realmlord:three_flower_wine", 5, 1, 2), item("realmlord:salt", 6, 2, 6), item("minecraft:candle", 4, 1, 3)]),
        (1, 2, [blueprint(4, 1), graded("minecraft:iron_sword", 2), graded("minecraft:iron_helmet", 2), {"type": "minecraft:empty", "weight": 3}]),
    ),
    "watchtower_supplies": table(
        (3, 6, [item("minecraft:arrow", 12, 6, 16), item("minecraft:bread", 10, 2, 6), item("minecraft:oak_log", 8, 4, 12),
                item("minecraft:cobblestone", 8, 8, 24), item("minecraft:torch", 8, 4, 12), item("realmlord:copper_coin", 10, 10, 30)]),
    ),
    "watchtower": table(
        (2, 4, [item("realmlord:silver_coin", 12, 2, 8), item("minecraft:iron_ingot", 10, 2, 6), item("realmlord:fire_oil", 6, 1, 3),
                item("minecraft:spyglass", 2), item("minecraft:arrow", 8, 8, 24)]),
        (1, 2, [graded(w, 3) for w in WEAPONS] + [graded(a, 2) for a in ARMOUR] + [blueprint(4, 2)]),
    ),
    "ancient_tomb": table(
        (4, 7, [item("realmlord:silver_coin", 16, 4, 12), item("realmlord:gold_coin", 4, 1, 2), item("minecraft:gold_ingot", 10, 2, 6),
                item("minecraft:emerald", 6, 1, 4), item("realmlord:fine_silk", 4, 1, 2), item("realmlord:linen", 8, 1, 3),
                item("minecraft:jade_placeholder", 0)]),
        (2, 3, [graded(w, 3) for w in WEAPONS + ["minecraft:diamond_sword"]] + [graded(a, 2) for a in ARMOUR + ["minecraft:diamond_chestplate"]]
         + [blueprint(5, 3)]),
    ),
    "tomb_treasure": table(
        (1, 1, [item("realmlord:gold_coin", 1, 3, 8)]),
        (2, 3, [graded("minecraft:diamond_sword", 2, "gold"), graded("minecraft:iron_chestplate", 3, "silver"), graded("minecraft:iron_sword", 3, "silver"),
                blueprint(6, 3), item("realmlord:summoning_token", 2, functions=[{"function": "realmlord:summoning", "rank": 0}]),
                item("minecraft:enchanted_golden_apple", 1)]),
    ),
}
# (no placeholder items in real tables)
for k, v in LOOT.items():
    for p in v["pools"]:
        p["entries"] = [e for e in p["entries"] if e.get("weight", 1) > 0 and "placeholder" not in e.get("name", "")]


STRUCTURES = {
    # name: (template, biomes, spacing, separation, salt, sink, spawns, adaptation)
    "ancient_shrine": (shrine, ["#minecraft:is_forest", "minecraft:plains", "minecraft:meadow", "#minecraft:is_taiga", "#minecraft:is_jungle", "minecraft:cherry_grove",
                                "minecraft:savanna", "minecraft:sunflower_plains", "minecraft:flower_forest"], 28, 10, 81731, -1, None, "beard_thin"),
    "ruined_watchtower": (watchtower, ["#minecraft:is_hill", "#minecraft:is_mountain", "minecraft:plains", "minecraft:savanna", "#minecraft:is_taiga",
                                       "minecraft:windswept_hills", "minecraft:stony_peaks", "minecraft:meadow"], 40, 14, 91277, -1,
                          {"monster": {"bounding_box": "piece", "spawns": [{"type": "realmlord:bandit", "weight": 1, "minCount": 1, "maxCount": 2}]}}, "beard_thin"),
    "ancient_tomb": (tomb, ["minecraft:plains", "minecraft:desert", "#minecraft:is_badlands", "minecraft:savanna", "#minecraft:is_forest", "minecraft:meadow",
                            "minecraft:sunflower_plains"], 64, 24, 66119, -7,
                     {"monster": {"bounding_box": "piece", "spawns": [{"type": "realmlord:bandit", "weight": 1, "minCount": 1, "maxCount": 3}]}}, "none"),
}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=1)


def main():
    for name, (build, biomes, spacing, sep, salt, sink, spawns, adapt) in STRUCTURES.items():
        n = build().save(name)
        write(os.path.join(ROOT, "tags", "worldgen", "biome", "has_structure", f"{name}.json"), {"replace": False, "values": biomes})
        write(os.path.join(ROOT, "worldgen", "template_pool", f"{name}.json"), {
            "name": f"realmlord:{name}", "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"realmlord:{name}",
                                                   "processors": "minecraft:empty", "projection": "rigid"}}]})
        write(os.path.join(ROOT, "worldgen", "structure", f"{name}.json"), {
            "type": "minecraft:jigsaw", "biomes": f"#realmlord:has_structure/{name}", "step": "surface_structures",
            "spawn_overrides": spawns or {}, "terrain_adaptation": adapt, "start_pool": f"realmlord:{name}", "size": 1,
            "start_height": {"absolute": sink}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "max_distance_from_center": 80, "use_expansion_hack": False})
        write(os.path.join(ROOT, "worldgen", "structure_set", f"{name}.json"), {
            "structures": [{"structure": f"realmlord:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})
        print(name, n, "blocks")
    for name, t in LOOT.items():
        write(os.path.join(ROOT, "loot_tables", "chests", f"{name}.json"), t)


if __name__ == "__main__":
    main()
