"""The siege engines' block models, blockstates and loot tables: the bed crossbow (three bows on a
heavy frame, a spear-long bolt on the rail, a winch at the back) and the counterweight trebuchet
(an A-frame on a sled, the throwing arm cocked, a stone counterweight)."""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "realmlord")
D = os.path.join(ROOT, "data", "realmlord")

PLANK = "minecraft:block/spruce_planks"
LOG = "minecraft:block/spruce_log"
DARK = "minecraft:block/dark_oak_planks"
IRON = "minecraft:block/iron_block"
WOOL = "minecraft:block/white_wool"
STONE = "minecraft:block/cobblestone"
ROPE = "minecraft:block/brown_wool"


def box(fr, to, tex, rot=None):
    faces = {f: {"texture": "#" + tex, "uv": uv(f, fr, to)} for f in ("north", "south", "east", "west", "up", "down")}
    e = {"from": fr, "to": to, "faces": faces}
    if rot: e["rotation"] = rot
    return e


def uv(face, fr, to):
    x0, y0, z0 = fr
    x1, y1, z1 = to
    clamp = lambda v: max(0, min(16, v))
    if face in ("north", "south"): u = (x0, 16 - y1, x1, 16 - y0)
    elif face in ("east", "west"): u = (z0, 16 - y1, z1, 16 - y0)
    else: u = (x0, z0, x1, z1)
    u = [clamp(v) for v in u]
    if u[0] == u[2]: u[2] = min(16, u[0] + 1)
    if u[1] == u[3]: u[3] = min(16, u[1] + 1)
    return u


def bed_crossbow():
    el = [
        box([2, 0, 2], [4, 3, 15], "plank"), box([12, 0, 2], [14, 3, 15], "plank"),
        box([2, 3, 9], [14, 5, 11], "plank"), box([2, 3, 3], [14, 5, 5], "plank"),
        box([7, 5, 0], [9, 7, 16], "dark"),
        # Three bows, the front one facing the enemy, the back one reversed.
        box([0, 7, 2], [16, 8.5, 3.5], "log"), box([1, 7, 5.5], [15, 8.5, 7], "log"), box([2, 7, 9], [14, 8.5, 10.5], "log"),
        box([0.5, 7.3, 12], [15.5, 7.8, 12.5], "wool"),
        # The bolt, its iron head out over the front.
        box([7.5, 7, -5], [8.5, 8, 12], "plank"), box([7.2, 6.8, -7], [8.8, 8.2, -5], "iron"),
        # The winch.
        box([4, 3, 13], [12, 5, 15], "log"), box([3, 2, 13.5], [4, 6, 14.5], "dark"), box([12, 2, 13.5], [13, 6, 14.5], "dark"),
    ]
    return {"parent": "minecraft:block/block", "textures": {"particle": PLANK, "plank": PLANK, "log": LOG, "dark": DARK, "iron": IRON, "wool": WOOL},
            "elements": el, "display": display()}


def trebuchet():
    el = [
        box([1, 0, 0], [3, 2, 16], "dark"), box([13, 0, 0], [15, 2, 16], "dark"),
        box([1, 0, 2], [15, 2, 4], "plank"), box([1, 0, 12], [15, 2, 14], "plank"),
        # The A-frame and its axle.
        box([2, 2, 7], [4, 20, 9], "log"), box([12, 2, 7], [14, 20, 9], "log"),
        box([2, 2, 3], [3.5, 14, 4.5], "plank", {"origin": [3, 2, 4], "axis": "x", "angle": 22.5}),
        box([12.5, 2, 3], [14, 14, 4.5], "plank", {"origin": [13, 2, 4], "axis": "x", "angle": 22.5}),
        box([2, 2, 11.5], [3.5, 14, 13], "plank", {"origin": [3, 2, 12], "axis": "x", "angle": -22.5}),
        box([12.5, 2, 11.5], [14, 14, 13], "plank", {"origin": [13, 2, 12], "axis": "x", "angle": -22.5}),
        box([1, 18, 7], [15, 20, 9], "iron"),
        # The arm, cocked: the long end down at the front, the counterweight up behind.
        box([7, 18, -6], [9, 20, 22], "dark", {"origin": [8, 19, 8], "axis": "x", "angle": -22.5}),
        box([5, 20, 15], [11, 26, 21], "stone", {"origin": [8, 19, 8], "axis": "x", "angle": -22.5}),
        box([7.5, 10, -4], [8.5, 18, -3], "rope", {"origin": [8, 19, 8], "axis": "x", "angle": -22.5}),
        box([6.5, 8, -5], [9.5, 10, -2], "rope", {"origin": [8, 19, 8], "axis": "x", "angle": -22.5}),
    ]
    return {"parent": "minecraft:block/block", "textures": {"particle": PLANK, "plank": PLANK, "log": LOG, "dark": DARK, "iron": IRON,
                                                            "stone": STONE, "rope": ROPE},
            "elements": el, "display": display()}


def display():
    return {"gui": {"rotation": [30, 225, 0], "scale": [0.5, 0.5, 0.5]}, "ground": {"scale": [0.3, 0.3, 0.3]},
            "fixed": {"scale": [0.5, 0.5, 0.5]}}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def main():
    for name, model in (("bed_crossbow", bed_crossbow()), ("trebuchet", trebuchet())):
        write(os.path.join(A, "models", "block", name + ".json"), model)
        write(os.path.join(A, "blockstates", name + ".json"), {"variants": {
            f"facing={f}": {"model": f"realmlord:block/{name}", "y": y} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(os.path.join(D, "loot_tables", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"realmlord:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    print("siege engines written")


if __name__ == "__main__":
    main()
