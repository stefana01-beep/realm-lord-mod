"""The ready-made buildings the structure manuals teach: written here block by block, saved as
data/realmlord/designs/<id>.json for the mod to build.

Coordinates: x across, y up, z back from the front. The front (z = 0) is the entrance and faces
the player who builds it. Stairs "facing" is the side their tall back is on. A design may sit
lower than the clicked ground (yOff < 0) for foundations, wells and piers.

Format: {"size": [w, h, d], "yOff": n, "palette": [state, ...], "cells": [...]} where cells run
x fastest, then z, then y; -1 leaves the world as it is, 0 is air (cleared), others index the palette.
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "realmlord", "designs")

SKIP = None
AIR = "minecraft:air"


class D:
    def __init__(self, w, h, d, yoff=0):
        self.w, self.h, self.d, self.yoff = w, h, d, yoff
        self.c = {}

    def set(self, x, y, z, s):
        if 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d:
            self.c[(x, y, z)] = s

    def fill(self, x0, y0, z0, x1, y1, z1, s):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                for x in range(min(x0, x1), max(x0, x1) + 1):
                    self.set(x, y, z, s)

    def walls(self, x0, y0, z0, x1, y1, z1, s):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                for x in range(x0, x1 + 1):
                    if x in (x0, x1) or z in (z0, z1): self.set(x, y, z, s)

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def door(self, x, y, z, wood="oak", facing="north"):
        self.set(x, y, z, f"minecraft:{wood}_door[facing={facing},half=lower,hinge=left,open=false]")
        self.set(x, y + 1, z, f"minecraft:{wood}_door[facing={facing},half=upper,hinge=left,open=false]")

    def gable_roof(self, x0, x1, z0, z1, y, stairs, fill, ends=None):
        """A roof whose ridge runs along x: stairs rise from the front and back eaves."""
        k = 0
        while z0 + k <= z1 - k:
            za, zb = z0 + k, z1 - k
            for x in range(x0, x1 + 1):
                if za == zb: self.set(x, y + k, za, fill)
                else:
                    self.set(x, y + k, za, f"{stairs}[facing=south,half=bottom]")
                    self.set(x, y + k, zb, f"{stairs}[facing=north,half=bottom]")
            if ends and za < zb:
                for z in range(za + 1, zb):
                    self.set(x0 + 1, y + k, z, ends)
                    self.set(x1 - 1, y + k, z, ends)
            k += 1
        return y + k - 1

    def hip_ring(self, x0, z0, x1, z1, y, stairs):
        """A ring of eaves: stairs all round, tall side inward."""
        for x in range(x0, x1 + 1):
            self.set(x, y, z0, f"{stairs}[facing=south,half=bottom]")
            self.set(x, y, z1, f"{stairs}[facing=north,half=bottom]")
        for z in range(z0 + 1, z1):
            self.set(x0, y, z, f"{stairs}[facing=east,half=bottom]")
            self.set(x1, y, z, f"{stairs}[facing=west,half=bottom]")

    def save(self, name):
        palette = [AIR]
        idx = {AIR: 0}
        cells = []
        for y in range(self.h):
            for z in range(self.d):
                for x in range(self.w):
                    s = self.c.get((x, y, z))
                    if s is None: cells.append(-1); continue
                    if s not in idx:
                        idx[s] = len(palette)
                        palette.append(s)
                    cells.append(idx[s])
        os.makedirs(OUT, exist_ok=True)
        with open(os.path.join(OUT, name + ".json"), "w") as f:
            json.dump({"size": [self.w, self.h, self.d], "yOff": self.yoff, "palette": palette, "cells": cells}, f, separators=(",", ":"))
        return sum(1 for v in cells if v > 0)


M = lambda n: "minecraft:" + n
STONE_BRICKS, COBBLE, PLANKS, SPRUCE, DARK = M("stone_bricks"), M("cobblestone"), M("oak_planks"), M("spruce_planks"), M("dark_oak_planks")
LOG, SLOG, RED = M("oak_log"), M("spruce_log"), M("stripped_mangrove_log")
WATER, GLASS, LANTERN = M("water"), M("glass_pane"), M("lantern[hanging=false]")
HANG = M("lantern[hanging=true]")
TILES = M("deepslate_tiles")


def ladder(facing):
    return M(f"ladder[facing={facing}]")


DESIGNS = {}


def design(fn):
    DESIGNS[fn.__name__] = fn
    return fn


@design
def village_well():
    d = D(5, 6, 5, yoff=-2)
    for y in range(0, 3):
        d.walls(0, y, 0, 4, y, 4, STONE_BRICKS)
        d.fill(1, y, 1, 3, y, 3, WATER if y < 2 else AIR)
    d.fill(1, 0, 1, 3, 0, 3, WATER)
    d.fill(1, 1, 1, 3, 1, 3, WATER)
    for y in (3, 4):
        d.set(0, y, 2, M("oak_fence"))
        d.set(4, y, 2, M("oak_fence"))
    d.fill(0, 5, 1, 4, 5, 3, M("dark_oak_slab[type=bottom]"))
    d.set(2, 4, 2, M("chain[axis=y]"))
    d.set(2, 3, 2, M("bucket") if False else AIR)
    return d


@design
def pavilion():
    d = D(7, 7, 7)
    d.fill(1, 0, 1, 5, 0, 5, STONE_BRICKS)
    d.clear(1, 1, 1, 5, 4, 5)
    for x, z in [(1, 1), (5, 1), (1, 5), (5, 5)]:
        d.fill(x, 1, z, x, 3, z, RED)
    for x in (2, 3, 4):
        d.set(x, 1, 5, M("spruce_stairs[facing=south,half=bottom]"))
    d.hip_ring(0, 0, 6, 6, 4, M("dark_oak_stairs"))
    d.fill(1, 4, 1, 5, 4, 5, DARK)
    d.hip_ring(1, 1, 5, 5, 5, M("dark_oak_stairs"))
    d.fill(2, 5, 2, 4, 5, 4, DARK)
    d.set(3, 6, 3, M("dark_oak_slab[type=bottom]"))
    d.set(3, 3, 3, HANG)
    return d


@design
def mail_post():
    d = D(3, 4, 3)
    d.fill(0, 0, 0, 2, 0, 2, SPRUCE)
    d.clear(0, 1, 0, 2, 2, 2)
    for x, z in [(0, 0), (2, 0), (0, 2), (2, 2)]:
        d.fill(x, 1, z, x, 2, z, M("spruce_fence"))
    d.set(1, 1, 1, M("lectern[facing=north]"))
    d.set(1, 1, 2, M("barrel[facing=up]"))
    d.fill(0, 3, 0, 2, 3, 2, M("spruce_slab[type=bottom]"))
    return d


@design
def arrow_tower():
    d = D(5, 12, 5)
    d.fill(0, 0, 0, 4, 0, 4, COBBLE)
    d.clear(1, 1, 1, 3, 8, 3)
    for y in range(1, 9):
        d.walls(0, y, 0, 4, y, 4, SPRUCE)
        for x, z in [(0, 0), (4, 0), (0, 4), (4, 4)]: d.set(x, y, z, SLOG)
    for x in (2,):
        d.set(x, 5, 0, AIR); d.set(x, 5, 4, AIR); d.set(0, 5, 2, AIR); d.set(4, 5, 2, AIR)   # arrow slits
    d.door(2, 1, 0, "spruce")
    d.fill(0, 9, 0, 4, 9, 4, SPRUCE)
    for y in range(1, 10): d.set(2, y, 3, ladder("north"))
    d.clear(0, 10, 0, 4, 10, 4)
    d.walls(0, 10, 0, 4, 10, 4, M("spruce_fence"))
    for x, z in [(0, 0), (4, 0), (0, 4), (4, 4)]: d.set(x, 10, z, SLOG)
    d.fill(0, 11, 0, 4, 11, 4, M("spruce_slab[type=bottom]"))
    d.set(2, 10, 2, HANG)
    return d


@design
def beacon_tower():
    d = D(7, 14, 7)
    d.fill(0, 0, 0, 6, 0, 6, STONE_BRICKS)
    for y in range(1, 11):
        d.walls(0, y, 0, 6, y, 6, STONE_BRICKS)
        d.clear(1, y, 1, 5, y, 5)
    d.set(3, 1, 0, AIR); d.set(3, 2, 0, AIR)
    for y in range(1, 12): d.set(3, y, 5, ladder("north"))
    d.fill(0, 11, 0, 6, 11, 6, STONE_BRICKS)
    d.set(3, 11, 5, ladder("north"))
    for x in range(7):
        for z in range(7):
            if (x in (0, 6) or z in (0, 6)) and (x + z) % 2 == 0: d.set(x, 12, z, M("stone_brick_wall"))
    d.set(3, 12, 3, M("campfire[lit=true,facing=north]"))
    d.set(3, 12, 2, M("hay_block"))
    return d


@design
def paifang():
    d = D(9, 9, 3)
    d.fill(0, 0, 0, 8, 0, 2, STONE_BRICKS)
    for x in (2, 6):
        d.fill(x, 1, 1, x, 6, 1, M("polished_andesite"))
    for x in (0, 8):
        d.fill(x, 1, 1, x, 4, 1, M("polished_andesite"))
    d.fill(0, 5, 1, 8, 5, 1, STONE_BRICKS)
    d.fill(2, 6, 1, 6, 6, 1, STONE_BRICKS)
    d.set(4, 5, 0, M("gold_block"))
    for x in range(0, 9):
        d.set(x, 6 if x in (0, 1, 7, 8) else 7, 0, M("deepslate_tile_stairs[facing=south,half=bottom]"))
        d.set(x, 6 if x in (0, 1, 7, 8) else 7, 2, M("deepslate_tile_stairs[facing=north,half=bottom]"))
        d.set(x, 6 if x in (0, 1, 7, 8) else 7, 1, TILES)
    d.fill(2, 8, 1, 6, 8, 1, M("deepslate_tile_slab[type=bottom]"))
    d.clear(3, 1, 1, 5, 4, 1)
    d.clear(1, 1, 1, 1, 4, 1); d.clear(7, 1, 1, 7, 4, 1)
    return d


@design
def arch_bridge():
    # The Zhaozhou bridge: a single shallow arch, the deck rising to the middle and falling again.
    deck = [1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1]
    d = D(5, 7, len(deck))
    for z, y in enumerate(deck):
        d.fill(0, y, z, 4, y, z, STONE_BRICKS)
        if 4 <= z <= 12: d.fill(0, y - 1, z, 4, y - 1, z, M("stone_brick_slab[type=top]"))
        d.clear(1, y + 1, z, 3, y + 2, z)
        d.set(0, y + 1, z, M("stone_brick_wall")); d.set(4, y + 1, z, M("stone_brick_wall"))
        up = z + 1 < len(deck) and deck[z + 1] > y
        down = z > 0 and deck[z - 1] > y
        if up: d.fill(1, y + 1, z, 3, y + 1, z, M("stone_brick_stairs[facing=south,half=bottom]"))
        if down: d.fill(1, y + 1, z, 3, y + 1, z, M("stone_brick_stairs[facing=north,half=bottom]"))
    for z in (0, len(deck) - 1):
        d.fill(0, 0, z, 4, 0, z, STONE_BRICKS)
    for z in (3, 8, 13):
        d.set(0, deck[z] + 2, z, LANTERN); d.set(4, deck[z] + 2, z, LANTERN)
    return d


@design
def road_inn():
    d = D(11, 10, 9)
    d.fill(0, 0, 0, 10, 0, 8, STONE_BRICKS)
    d.clear(1, 1, 1, 9, 4, 7)
    for y in range(1, 5):
        d.walls(0, y, 0, 10, y, 8, SPRUCE)
        for x in (0, 5, 10):
            for z in (0, 8): d.set(x, y, z, LOG)
        for z in (0, 4, 8):
            for x in (0, 10): d.set(x, y, z, LOG)
    for x in (2, 3, 7, 8):
        d.set(x, 2, 0, GLASS); d.set(x, 2, 8, GLASS)
    d.set(0, 2, 2, GLASS); d.set(0, 2, 6, GLASS); d.set(10, 2, 2, GLASS); d.set(10, 2, 6, GLASS)
    d.door(5, 1, 0, "spruce")
    # Tables and stools, the counter, beds at the back.
    for x in (2, 8):
        d.set(x, 1, 3, M("spruce_fence")); d.set(x, 2, 3, M("spruce_pressure_plate"))
        d.set(x - 1, 1, 3, M("spruce_stairs[facing=west,half=bottom]")); d.set(x + 1, 1, 3, M("spruce_stairs[facing=east,half=bottom]"))
    for x in (4, 5, 6): d.set(x, 1, 5, M("barrel[facing=up]"))
    d.set(5, 2, 5, M("lantern[hanging=false]"))
    d.set(1, 1, 7, M("red_bed[facing=west,part=head]")); d.set(2, 1, 7, M("red_bed[facing=west,part=foot]"))
    d.set(9, 1, 7, M("red_bed[facing=east,part=head]")); d.set(8, 1, 7, M("red_bed[facing=east,part=foot]"))
    d.set(5, 1, 7, M("crafting_table"))
    d.fill(0, 5, 0, 10, 5, 8, SPRUCE)
    d.gable_roof(0, 10, 0, 8, 5, M("dark_oak_stairs"), DARK, ends=SPRUCE)
    for x in (3, 7): d.set(x, 4, 4, HANG)
    return d


@design
def relay_station():
    d = D(9, 10, 11)
    d.fill(0, 0, 0, 8, 0, 10, COBBLE)
    d.clear(0, 1, 0, 8, 4, 10)
    for x, z in [(0, 0), (8, 0), (0, 10), (8, 10), (0, 5), (8, 5)]:
        d.fill(x, 1, z, x, 4, z, LOG)
    d.walls(0, 1, 0, 8, 1, 10, M("oak_fence"))
    d.walls(0, 2, 6, 8, 3, 10, PLANKS)
    for z in (6, 7, 8, 9): d.set(4, 1, z, M("oak_fence"))   # stalls
    d.set(4, 1, 0, AIR); d.set(4, 1, 10, AIR)
    d.set(4, 1, 6, M("oak_fence_gate[facing=north,open=false]"))
    d.set(1, 1, 9, M("hay_block")); d.set(7, 1, 9, M("hay_block"))
    d.set(1, 1, 7, M("water_cauldron[level=3]")); d.set(7, 1, 7, M("water_cauldron[level=3]"))
    d.set(1, 1, 1, M("barrel[facing=up]")); d.set(2, 1, 1, M("crafting_table"))
    d.fill(0, 5, 0, 8, 5, 10, PLANKS)
    d.gable_roof(0, 8, 0, 10, 5, M("spruce_stairs"), SPRUCE) if False else None
    # A ridge along the depth.
    k = 0
    while k <= 4:
        for z in range(0, 11):
            if k == 4: d.set(4, 5 + k, z, SPRUCE)
            else:
                d.set(k, 5 + k, z, M("spruce_stairs[facing=east,half=bottom]"))
                d.set(8 - k, 5 + k, z, M("spruce_stairs[facing=west,half=bottom]"))
        k += 1
    for z in (2, 8): d.set(4, 4, z, HANG)
    return d


@design
def taoist_shrine():
    d = D(9, 11, 9)
    d.fill(0, 0, 0, 8, 0, 8, STONE_BRICKS)
    d.fill(3, 0, 0, 5, 0, 0, M("stone_brick_stairs[facing=south,half=bottom]"))
    d.clear(1, 1, 1, 7, 5, 7)
    for y in range(1, 6):
        d.walls(1, y, 2, 7, y, 7, M("white_terracotta"))
        for x, z in [(1, 2), (7, 2), (1, 7), (7, 7), (3, 2), (5, 2)]: d.set(x, y, z, RED)
    d.clear(4, 1, 2, 4, 3, 2)
    d.set(4, 1, 6, M("polished_deepslate")); d.set(4, 2, 6, M("candle[candles=3,lit=true]"))
    d.set(3, 1, 6, M("polished_deepslate")); d.set(5, 1, 6, M("polished_deepslate"))
    d.set(3, 2, 6, M("flower_pot")); d.set(5, 2, 6, M("brewing_stand"))
    d.set(2, 1, 4, M("red_carpet")); d.set(6, 1, 4, M("red_carpet"))
    d.hip_ring(0, 1, 8, 8, 6, M("dark_oak_stairs"))
    d.fill(1, 6, 2, 7, 6, 7, DARK)
    d.hip_ring(1, 2, 7, 7, 7, M("dark_oak_stairs"))
    d.fill(2, 7, 3, 6, 7, 6, TILES)
    d.hip_ring(2, 3, 6, 6, 8, M("deepslate_tile_stairs"))
    d.fill(3, 8, 4, 5, 8, 5, TILES)
    d.fill(3, 9, 4, 5, 9, 5, M("deepslate_tile_slab[type=bottom]"))
    d.set(4, 5, 4, HANG)
    return d


@design
def granary():
    d = D(7, 10, 7)
    for x, z in [(0, 0), (6, 0), (0, 6), (6, 6), (3, 3)]: d.set(x, 0, z, STONE_BRICKS)
    d.fill(0, 1, 0, 6, 1, 6, SPRUCE)
    d.clear(1, 2, 1, 5, 5, 5)
    for y in range(2, 6):
        d.walls(0, y, 0, 6, y, 6, SPRUCE)
        for x, z in [(0, 0), (6, 0), (0, 6), (6, 6)]: d.set(x, y, z, SLOG)
    d.door(3, 2, 0, "spruce")
    d.set(3, 0, 0, M("spruce_stairs[facing=south,half=bottom]"))
    for x in (1, 2, 4, 5):
        d.set(x, 2, 5, M("barrel[facing=up]")); d.set(x, 3, 5, M("barrel[facing=up]"))
    d.set(1, 2, 2, M("hay_block")); d.set(5, 2, 2, M("hay_block"))
    d.fill(0, 6, 0, 6, 6, 6, SPRUCE)
    d.gable_roof(0, 6, 0, 6, 6, M("dark_oak_stairs"), DARK, ends=SPRUCE)
    return d


@design
def courtyard_house():
    d = D(13, 8, 13)
    d.fill(0, 0, 0, 12, 0, 12, STONE_BRICKS)
    d.fill(4, 0, 4, 8, 0, 8, M("grass_block"))
    d.clear(0, 1, 0, 12, 5, 12)
    # Four wings, three deep, around the courtyard; a red gate at the front.
    for y in range(1, 5):
        d.walls(0, y, 0, 12, y, 12, M("polished_andesite"))
        d.walls(3, y, 3, 9, y, 9, M("white_terracotta"))
        for x, z in [(3, 3), (9, 3), (3, 9), (9, 9)]: d.set(x, y, z, RED)
    d.clear(4, 1, 3, 8, 4, 9)
    d.walls(3, 1, 3, 9, 4, 9, M("white_terracotta"))
    d.clear(4, 1, 4, 8, 4, 8)
    for x, z in [(3, 3), (9, 3), (3, 9), (9, 9)]: d.fill(x, 1, z, x, 4, z, RED)
    d.clear(6, 1, 0, 6, 2, 3)
    d.door(6, 1, 0, "crimson")
    for (x, z, f) in [(6, 9, "south"), (3, 6, "west"), (9, 6, "east")]:
        d.door(x, 1, z, "spruce", f)
    for x in (2, 10):
        d.set(x, 2, 0, GLASS); d.set(x, 2, 12, GLASS)
    for (x, z) in [(5, 9), (7, 9), (3, 5), (3, 7), (9, 5), (9, 7)]: d.set(x, 2, z, GLASS)
    d.set(6, 1, 6, M("flowering_azalea"))
    d.set(4, 1, 4, M("potted_red_tulip")); d.set(8, 1, 8, M("potted_red_tulip"))
    d.set(1, 1, 11, M("red_bed[facing=north,part=head]")); d.set(1, 1, 10, M("red_bed[facing=north,part=foot]"))
    d.set(11, 1, 11, M("crafting_table")); d.set(11, 1, 10, M("barrel[facing=up]"))
    d.fill(0, 5, 0, 12, 5, 12, DARK)
    d.fill(4, 5, 4, 8, 5, 8, AIR)
    d.hip_ring(0, 0, 12, 12, 6, M("deepslate_tile_stairs"))
    d.hip_ring(1, 1, 11, 11, 6, M("deepslate_tile_stairs"))
    d.walls(2, 6, 2, 10, 6, 10, TILES)
    d.walls(3, 7, 3, 9, 7, 9, M("deepslate_tile_slab[type=bottom]"))
    d.clear(4, 6, 4, 8, 7, 8)
    for (x, z) in [(1, 6), (11, 6), (6, 11)]: d.set(x, 4, z, HANG)
    return d


@design
def pagoda():
    d = D(7, 30, 7)
    d.fill(0, 0, 0, 6, 0, 6, STONE_BRICKS)
    for s in range(5):
        y0 = 1 + s * 5
        d.clear(1, y0, 1, 5, y0 + 3, 5)
        for y in range(y0, y0 + 4):
            d.walls(1, y, 1, 5, y, 5, M("white_terracotta"))
            for x, z in [(1, 1), (5, 1), (1, 5), (5, 5)]: d.set(x, y, z, RED)
        d.clear(3, y0, 1, 3, y0 + 1, 1) if s else d.door(3, y0, 1, "dark_oak")
        for (x, z) in [(1, 3), (5, 3), (3, 5)]: d.set(x, y0 + 1, z, GLASS)
        d.hip_ring(0, 0, 6, 6, y0 + 4, M("dark_oak_stairs"))
        d.fill(1, y0 + 4, 1, 5, y0 + 4, 5, DARK)
        for y in range(y0, y0 + 5): d.set(3, y, 4, ladder("north"))
        d.set(2, y0 + 3, 3, HANG)
    d.set(3, 26, 3, M("dark_oak_fence")); d.set(3, 27, 3, M("dark_oak_fence")); d.set(3, 28, 3, M("gold_block"))
    d.set(3, 29, 3, M("lightning_rod"))
    return d


@design
def wharf():
    d = D(5, 6, 14, yoff=-3)
    for z in range(14):
        d.fill(0, 2, z, 4, 2, z, SPRUCE)
        d.clear(0, 3, z, 4, 4, z)
        if z % 3 == 1:
            for x in (0, 4): d.fill(x, 0, z, x, 1, z, LOG)
        d.set(0, 3, z, M("spruce_fence")); d.set(4, 3, z, M("spruce_fence"))
    for z in (4, 7, 10):
        d.set(0, 3, z, AIR); d.set(4, 3, z, AIR)
    for z in (1, 13):
        d.set(0, 4, z, LANTERN); d.set(4, 4, z, LANTERN)
    d.set(2, 3, 13, M("barrel[facing=up]"))
    return d


@design
def ferry_landing():
    d = D(5, 5, 7, yoff=-3)
    for z in range(7):
        y = max(0, 2 - max(0, z - 3))
        d.fill(0, 0, z, 4, y, z, STONE_BRICKS)
        d.clear(0, y + 1, z, 4, y + 2, z)
        if z > 3: d.fill(0, y, z, 4, y, z, M("stone_brick_stairs[facing=north,half=bottom]"))
    d.set(0, 3, 0, M("oak_fence")); d.set(0, 4, 0, LANTERN)
    d.set(4, 3, 1, M("spruce_stairs[facing=west,half=bottom]"))
    return d


@design
def shipyard():
    d = D(11, 8, 16, yoff=-3)
    # A slipway sloping down into the water, framed by logs, with a crane and the shipwright's benches.
    for z in range(16):
        y = max(0, 3 - z // 4)
        d.fill(2, 0, z, 8, y, z, SPRUCE if y == 3 - z // 4 else COBBLE)
        d.clear(2, y + 1, z, 8, 6, z)
    for z in (0, 5, 10, 15):
        for x in (1, 9):
            top = max(0, 3 - z // 4) + 4
            d.fill(x, 0, z, x, top, z, LOG)
    for z in range(16):
        d.set(1, max(0, 3 - z // 4) + 4, z, M("spruce_fence")); d.set(9, max(0, 3 - z // 4) + 4, z, M("spruce_fence"))
    d.fill(0, 3, 0, 1, 3, 3, SPRUCE); d.fill(9, 3, 0, 10, 3, 3, SPRUCE)
    d.set(0, 4, 0, M("crafting_table")); d.set(0, 4, 1, M("smithing_table")); d.set(10, 4, 0, M("barrel[facing=up]"))
    d.set(10, 4, 1, M("loom[facing=west]")); d.set(10, 4, 2, M("grindstone[face=floor,facing=west]"))
    d.fill(5, 7, 0, 5, 7, 6, SLOG.replace("]", "") + "[axis=z]")
    d.set(5, 6, 6, M("chain[axis=y]")); d.set(5, 5, 6, M("chain[axis=y]"))
    return d


@design
def lighthouse():
    d = D(5, 17, 5)
    d.fill(0, 0, 0, 4, 0, 4, STONE_BRICKS)
    for y in range(1, 13):
        d.walls(0, y, 0, 4, y, 4, STONE_BRICKS if y % 4 else M("chiseled_stone_bricks"))
        d.clear(1, y, 1, 3, y, 3)
        d.set(2, y, 3, ladder("north"))
    d.set(2, 1, 0, AIR); d.set(2, 2, 0, AIR)
    d.fill(0, 13, 0, 4, 13, 4, STONE_BRICKS)
    d.set(2, 13, 3, ladder("north"))
    d.walls(0, 14, 0, 4, 15, 4, M("glass"))
    d.clear(1, 14, 1, 3, 15, 3)
    d.set(2, 14, 2, M("sea_lantern")); d.set(2, 15, 2, M("glowstone"))
    d.fill(0, 16, 0, 4, 16, 4, M("stone_brick_slab[type=bottom]"))
    return d


@design
def irrigated_field():
    d = D(9, 2, 9, yoff=-1)
    for x in range(9):
        for z in range(9):
            d.set(x, 0, z, WATER if x == 4 else M("farmland[moisture=7]"))
            d.set(x, 1, z, AIR if x == 4 else M("wheat[age=0]"))
    d.set(4, 1, 0, M("oak_slab[type=bottom]")); d.set(4, 1, 8, M("oak_slab[type=bottom]"))
    return d


@design
def herb_garden():
    d = D(9, 2, 9, yoff=-1)
    flowers = ["poppy", "dandelion", "allium", "cornflower", "lily_of_the_valley", "azure_bluet", "oxeye_daisy"]
    for x in range(9):
        for z in range(9):
            path = x in (0, 4, 8) or z in (0, 4, 8)
            d.set(x, 0, z, M("dirt_path") if path else M("grass_block"))
            d.set(x, 1, z, AIR if path else M(flowers[(x * 3 + z) % len(flowers)]))
    d.set(4, 1, 4, M("composter"))
    return d


@design
def stable():
    d = D(9, 10, 11)
    d.fill(0, 0, 0, 8, 0, 10, COBBLE)
    d.clear(1, 1, 1, 7, 4, 9)
    for y in range(1, 5):
        d.walls(0, y, 0, 8, y, 10, PLANKS)
        for x, z in [(0, 0), (8, 0), (0, 10), (8, 10), (0, 5), (8, 5)]: d.set(x, y, z, LOG)
    d.clear(3, 1, 0, 5, 3, 0)
    for z in (3, 6):
        for x in (1, 2, 6, 7): d.set(x, 1, z, M("oak_fence"))
    for (x, z) in [(1, 1), (7, 1), (1, 4), (7, 4), (1, 7), (7, 7)]: d.set(x, 1, z, M("hay_block"))
    for (x, z) in [(1, 9), (7, 9)]: d.set(x, 1, z, M("water_cauldron[level=3]"))
    d.set(4, 1, 9, M("barrel[facing=up]"))
    d.fill(0, 5, 0, 8, 5, 10, PLANKS)
    k = 0
    while k <= 4:
        for z in range(0, 11):
            if k == 4: d.set(4, 5 + k, z, SPRUCE)
            else:
                d.set(k, 5 + k, z, M("spruce_stairs[facing=east,half=bottom]"))
                d.set(8 - k, 5 + k, z, M("spruce_stairs[facing=west,half=bottom]"))
        k += 1
    d.set(4, 4, 5, HANG)
    return d


@design
def animal_pen():
    d = D(11, 3, 11)
    d.clear(0, 1, 0, 10, 2, 10)
    d.walls(0, 1, 0, 10, 1, 10, M("oak_fence"))
    d.set(5, 1, 0, M("oak_fence_gate[facing=north,open=false]"))
    for x, z in [(0, 0), (10, 0), (0, 10), (10, 10)]: d.set(x, 2, z, LANTERN)
    d.set(8, 1, 8, M("water_cauldron[level=3]")); d.set(2, 1, 8, M("hay_block")); d.set(3, 1, 8, M("hay_block"))
    d.set(5, 1, 9, M("composter"))
    return d


@design
def fishing_hut():
    d = D(7, 9, 7, yoff=-3)
    for x, z in [(0, 0), (6, 0), (0, 6), (6, 6)]: d.fill(x, 0, z, x, 2, z, LOG)
    d.fill(0, 3, 0, 6, 3, 6, SPRUCE)
    d.clear(1, 4, 2, 5, 6, 5)
    for y in range(4, 7):
        d.walls(1, y, 2, 5, y, 6, SPRUCE)
    d.door(3, 4, 2, "spruce")
    d.set(1, 5, 4, GLASS); d.set(5, 5, 4, GLASS)
    d.set(2, 4, 5, M("barrel[facing=up]")); d.set(4, 4, 5, M("smoker[facing=north,lit=false]"))
    d.set(1, 4, 0, M("spruce_fence")); d.set(5, 4, 0, M("spruce_fence"))
    d.fill(0, 7, 1, 6, 7, 6, M("spruce_slab[type=bottom]"))
    d.set(3, 6, 4, HANG)
    return d


@design
def kitchen():
    d = D(7, 6, 7)
    d.fill(0, 0, 0, 6, 0, 6, COBBLE)
    d.clear(0, 1, 0, 6, 4, 6)
    for x, z in [(0, 0), (6, 0), (0, 6), (6, 6)]: d.fill(x, 1, z, x, 3, z, LOG)
    d.walls(0, 1, 6, 6, 1, 6, M("bricks"))
    d.set(1, 1, 5, M("smoker[facing=north,lit=false]")); d.set(2, 1, 5, M("campfire[lit=true,facing=north]"))
    d.set(3, 1, 5, M("water_cauldron[level=3]")); d.set(4, 1, 5, M("barrel[facing=up]")); d.set(5, 1, 5, M("crafting_table"))
    d.set(3, 1, 2, M("spruce_fence")); d.set(3, 2, 2, M("spruce_pressure_plate"))
    d.fill(0, 4, 0, 6, 4, 6, M("spruce_slab[type=bottom]"))
    d.fill(2, 2, 6, 2, 5, 6, M("bricks"))
    d.set(3, 3, 3, HANG)
    return d


@design
def forge():
    d = D(7, 8, 7)
    d.fill(0, 0, 0, 6, 0, 6, STONE_BRICKS)
    d.clear(0, 1, 0, 6, 4, 6)
    for x, z in [(0, 0), (6, 0), (0, 6), (6, 6)]: d.fill(x, 1, z, x, 3, z, M("stone_brick_wall"))
    d.walls(0, 1, 6, 6, 3, 6, M("bricks"))
    d.set(3, 1, 5, M("blast_furnace[facing=north,lit=false]")); d.set(2, 1, 5, M("furnace[facing=north,lit=false]"))
    d.set(3, 1, 3, M("anvil[facing=east]")); d.set(5, 1, 4, M("smithing_table")); d.set(1, 1, 4, M("grindstone[face=floor,facing=east]"))
    d.set(5, 1, 1, M("water_cauldron[level=3]")); d.set(4, 1, 5, M("barrel[facing=up]"))
    d.fill(0, 4, 0, 6, 4, 6, M("stone_brick_slab[type=bottom]"))
    d.fill(3, 2, 5, 3, 7, 5, M("bricks")) if False else None
    d.fill(3, 4, 6, 3, 7, 6, M("bricks"))
    d.set(3, 3, 2, HANG)
    return d




# ---------------------------------------------------------------- village buildings from the novel

@design
def salt_pans():
    # Sea water let into shallow pans to dry in the sun; the salt raked into white heaps.
    d = D(11, 2, 11, yoff=-1)
    for x in range(11):
        for z in range(11):
            walk = x % 4 == 0 or z % 4 == 0
            d.set(x, 0, z, STONE_BRICKS if walk else WATER)
            d.set(x, 1, z, AIR)
    for (x, z) in [(2, 2), (6, 6), (10 - 1, 2)]:
        d.set(x, 0, z, M("calcite"))
    for (x, z) in [(0, 0), (4, 0), (8, 0)]:
        d.set(x, 1, z, M("snow[layers=4]"))
    d.set(10, 1, 10, M("barrel[facing=up]")); d.set(0, 1, 10, M("barrel[facing=up]"))
    return d


@design
def academy():
    d = D(15, 12, 13)
    d.fill(0, 0, 0, 14, 0, 12, STONE_BRICKS)
    d.fill(6, 0, 0, 8, 0, 0, M("stone_brick_stairs[facing=south,half=bottom]"))
    d.clear(1, 1, 1, 13, 6, 11)
    for y in range(1, 6):
        d.walls(1, y, 1, 13, y, 11, M("white_terracotta"))
        for x in (1, 4, 7, 10, 13):
            d.set(x, y, 1, RED); d.set(x, y, 11, RED)
        for z in (1, 6, 11):
            d.set(1, y, z, RED); d.set(13, y, z, RED)
    d.clear(6, 1, 1, 8, 3, 1)
    for x in (2, 3, 5, 9, 11, 12):
        d.set(x, 2, 1, GLASS); d.set(x, 3, 1, GLASS)
    for z in (3, 4, 8, 9):
        d.set(1, 3, z, GLASS); d.set(13, 3, z, GLASS)
    # Shelves of the classics along the back and sides; desks in rows facing the master.
    for x in range(2, 13):
        d.set(x, 1, 10, M("bookshelf")); d.set(x, 2, 10, M("bookshelf"))
    d.set(7, 1, 9, M("lectern[facing=south]"))
    d.set(7, 3, 10, M("chiseled_quartz_block"))
    for z in (4, 6):
        for x in (3, 5, 9, 11):
            d.set(x, 1, z, M("lectern[facing=north]"))
    d.set(2, 1, 2, M("potted_bamboo")); d.set(12, 1, 2, M("potted_bamboo"))
    d.fill(1, 6, 1, 13, 6, 11, DARK)
    d.hip_ring(0, 0, 14, 12, 6, M("dark_oak_stairs"))
    d.hip_ring(1, 1, 13, 11, 7, M("deepslate_tile_stairs"))
    d.fill(2, 7, 2, 12, 7, 10, TILES)
    d.hip_ring(2, 2, 12, 10, 8, M("deepslate_tile_stairs"))
    d.fill(3, 8, 3, 11, 8, 9, TILES)
    d.hip_ring(3, 3, 11, 9, 9, M("deepslate_tile_stairs"))
    d.fill(4, 9, 4, 10, 9, 8, TILES)
    d.fill(4, 10, 6, 10, 10, 6, M("deepslate_tile_slab[type=bottom]"))
    for x in (4, 10): d.set(x, 5, 6, HANG)
    return d


@design
def chamber_of_commerce():
    d = D(11, 12, 9)
    d.fill(0, 0, 0, 10, 0, 8, STONE_BRICKS)
    d.clear(1, 1, 1, 9, 8, 7)
    for y in range(1, 9):
        d.walls(0, y, 0, 10, y, 8, SPRUCE if y != 4 else DARK)
        for x, z in [(0, 0), (10, 0), (0, 8), (10, 8), (5, 0)]: d.set(x, y, z, RED)
    d.clear(4, 1, 0, 6, 3, 0)
    for x in (2, 8):
        d.set(x, 2, 0, GLASS); d.set(x, 6, 0, GLASS)
    d.set(4, 4, 0, M("red_banner[rotation=8]") if False else DARK)
    # The counting-house: counter of barrels, chests and ledgers; a stair to the upper floor.
    for x in range(2, 9): d.set(x, 1, 4, M("barrel[facing=up]"))
    d.set(5, 2, 4, M("lantern[hanging=false]"))
    d.set(1, 1, 7, M("chest[facing=south]")); d.set(2, 1, 7, M("chest[facing=south]"))
    d.set(9, 1, 7, M("crafting_table"))
    d.fill(1, 4, 1, 9, 4, 7, SPRUCE)
    for i, (x, y) in enumerate([(9, 1), (9, 2), (9, 3)]):
        d.set(x, y, 5 - i, M("spruce_stairs[facing=north,half=bottom]"))
        d.clear(9, 4, 3, 9, 4, 4)
    d.set(3, 5, 6, M("lectern[facing=south]")); d.set(7, 5, 6, M("lectern[facing=south]"))
    d.set(5, 5, 7, M("chest[facing=north]"))
    d.fill(0, 9, 0, 10, 9, 8, SPRUCE)
    d.gable_roof(0, 10, 0, 8, 9, M("dark_oak_stairs"), DARK, ends=SPRUCE)
    d.set(5, 8, 4, HANG); d.set(5, 3, 2, HANG)
    return d


@design
def water_fort():
    # The water bandits' lair: a stilted palisade fort out on the water, a watchtower and a hut.
    d = D(13, 12, 13, yoff=-4)
    for x in range(0, 13, 4):
        for z in range(0, 13, 4):
            d.fill(x, 0, z, x, 3, z, M("spruce_log"))
    d.fill(0, 3, 0, 12, 3, 12, SPRUCE)
    d.clear(0, 4, 0, 12, 7, 12)
    for y in (4, 5):
        d.walls(0, y, 0, 12, y, 12, M("spruce_fence") if y == 5 else M("stripped_spruce_log"))
    d.set(6, 4, 0, AIR); d.set(6, 5, 0, AIR)
    for x, z in [(0, 0), (12, 0), (0, 12), (12, 12)]:
        d.fill(x, 4, z, x, 6, z, M("spruce_log")); d.set(x, 7, z, M("campfire[lit=true,facing=north]"))
    d.clear(7, 4, 7, 11, 7, 11)
    for y in range(4, 7): d.walls(7, y, 7, 11, y, 11, M("dark_oak_planks"))
    d.door(9, 4, 7, "dark_oak")
    d.fill(7, 7, 7, 11, 7, 11, M("dark_oak_slab[type=bottom]"))
    d.set(10, 4, 10, M("chest[facing=north]"))
    d.set(8, 4, 10, M("barrel[facing=up]"))
    # Watchtower
    d.fill(1, 4, 9, 3, 9, 11, M("spruce_planks"))
    d.clear(2, 4, 10, 2, 9, 10)
    for y in range(4, 10): d.set(2, y, 10, ladder("south") if False else M("ladder[facing=north]"))
    d.clear(1, 10, 9, 3, 11, 11)
    d.walls(1, 10, 9, 3, 10, 11, M("spruce_fence"))
    d.set(2, 11, 10, M("red_wool"))
    for z in (1, 2, 3):
        d.set(5, 3, z, SPRUCE)
    d.set(4, 4, 3, M("hay_block")); d.set(3, 4, 3, M("barrel[facing=up]"))
    return d


# ---------------------------------------------------------------- the ships, drawn in blocks (assets/realmlord/ships)

SHIPS_OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "realmlord", "ships")


def hull(d, length, beam, deck_y, wood, keel=M("dark_oak_planks"), bow_taper=4, stern_taper=2):
    """A junk's hull, bow at z = 0: full sections amidships, narrowing to the bow and stern."""
    w = d.w
    cx = (w - 1) / 2
    for z in range(length):
        if z < bow_taper: half = max(0.5, (beam / 2) * (z + 1) / (bow_taper + 1))
        elif z > length - 1 - stern_taper: half = max(1.0, (beam / 2) * 0.8)
        else: half = beam / 2
        for x in range(w):
            if abs(x - cx) > half: continue
            for y in range(0, deck_y + 1):
                inner = abs(x - cx) <= half - 1 and y >= 1
                rise = (bow_taper - z) // 2 if z < bow_taper else 0
                if y == 0 and abs(x - cx) > half - 1: continue
                d.set(x, y + rise, z, keel if y == 0 else wood if not inner or y == deck_y else wood)
    return cx


def save_ship(d, name):
    os.makedirs(SHIPS_OUT, exist_ok=True)
    palette = [AIR]
    idx = {AIR: 0}
    cells = []
    for y in range(d.h):
        for z in range(d.d):
            for x in range(d.w):
                s = d.c.get((x, y, z))
                if s is None: cells.append(-1); continue
                if s not in idx:
                    idx[s] = len(palette); palette.append(s)
                cells.append(idx[s])
    with open(os.path.join(SHIPS_OUT, name + ".json"), "w") as f:
        json.dump({"size": [d.w, d.h, d.d], "yOff": 0, "palette": palette, "cells": cells}, f, separators=(",", ":"))
    return sum(1 for v in cells if v > 0)


def ship_merchant():
    d = D(7, 13, 17)
    cx = hull(d, 17, 6, 3, M("spruce_planks"))
    c = int(cx)
    # The stern castle, a mast amidships with a battened junk sail, and cargo on deck.
    d.fill(1, 4, 13, 5, 5, 16, M("spruce_planks"))
    d.fill(1, 6, 13, 5, 6, 16, M("spruce_slab[type=bottom]"))
    d.set(3, 5, 13, M("glass_pane"))
    for y in range(4, 13): d.set(c, y, 7, M("stripped_spruce_log"))
    for y in range(6, 12):
        for z in range(4, 11):
            d.set(c, y, z, M("brown_wool") if y % 2 else M("white_wool"))
    d.set(1, 4, 4, M("barrel[facing=up]")); d.set(5, 4, 4, M("barrel[facing=up]")); d.set(5, 4, 10, M("chest[facing=north]"))
    d.set(c, 4, 0, M("spruce_fence")); d.set(c, 5, 0, M("lantern[hanging=false]"))
    return d


def ship_mengchong():
    d = D(7, 8, 19)
    cx = hull(d, 19, 6, 3, M("acacia_planks"), bow_taper=3)
    c = int(cx)
    # Covered in rawhide from bow to stern, with oar ports and arrow slits; an iron ram at the bow.
    for z in range(3, 18):
        for x in range(1, 6):
            d.set(x, 4, z, M("brown_terracotta"))
            d.set(x, 5, z, M("brown_terracotta") if x in (1, 5) else M("brown_terracotta"))
        d.set(0, 3, z, M("acacia_planks")); d.set(6, 3, z, M("acacia_planks"))
        if z % 3 == 0:
            d.set(0, 3, z, M("dark_oak_fence")); d.set(6, 3, z, M("dark_oak_fence"))
            d.set(1, 5, z, M("black_terracotta")); d.set(5, 5, z, M("black_terracotta"))
    d.fill(2, 6, 6, 4, 6, 14, M("brown_terracotta"))
    d.set(c, 1, 0, M("iron_block")); d.set(c, 2, 0, M("iron_block"))
    d.set(c, 7, 10, M("red_wool"))
    return d


def ship_turreted():
    d = D(11, 20, 27)
    cx = hull(d, 27, 10, 4, M("dark_oak_planks"), bow_taper=5, stern_taper=3)
    c = int(cx)
    # Three storeys of tower with red railings and tiled eaves: the floating fortress.
    for i, (x0, x1, z0, z1) in enumerate([(1, 9, 6, 22), (2, 8, 8, 20), (3, 7, 10, 18)]):
        y0 = 5 + i * 4
        for y in range(y0, y0 + 3):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    edge = x in (x0, x1) or z in (z0, z1)
                    if edge: d.set(x, y, z, M("red_terracotta") if (x + z) % 3 else M("dark_oak_planks"))
        for x in range(x0 - 1, x1 + 2):
            d.set(x, y0 + 3, z0 - 1, M("deepslate_tile_slab[type=bottom]")); d.set(x, y0 + 3, z1 + 1, M("deepslate_tile_slab[type=bottom]"))
        for z in range(z0 - 1, z1 + 2):
            d.set(x0 - 1, y0 + 3, z, M("deepslate_tile_slab[type=bottom]")); d.set(x1 + 1, y0 + 3, z, M("deepslate_tile_slab[type=bottom]"))
        d.fill(x0, y0 + 3, z0, x1, y0 + 3, z1, M("dark_oak_planks"))
        for x in range(x0, x1 + 1, 2):
            d.set(x, y0 + 1, z0, M("black_terracotta"))
    d.fill(4, 17, 12, 6, 17, 16, M("deepslate_tiles"))
    d.set(c, 18, 14, M("red_wool")); d.set(c, 19, 14, M("red_wool"))
    # Fore and main masts with red sails; six clapper poles over the sides.
    for (z, top) in [(3, 15), (24, 13)]:
        for y in range(5, top): d.set(c, y, z, M("stripped_dark_oak_log"))
        for y in range(top - 6, top - 1):
            for dz in range(-2, 3): d.set(c, y, z + dz, M("red_wool"))
    for z in (8, 14, 20):
        for x in (0, 10):
            d.set(x, 6, z, M("dark_oak_fence")); d.set(x, 7, z, M("dark_oak_fence"))
            d.set(x, 8, z, M("cobblestone"))
    for z in range(5, 23, 2):
        d.set(0, 5, z, M("red_terracotta")); d.set(10, 5, z, M("red_terracotta"))
    return d


SHIP_MODELS = {"merchant_ship": ship_merchant, "mengchong_warship": ship_mengchong, "turreted_ship": ship_turreted}


@design
def cemetery():
    # A walled cemetery (the novel's chapter 61): rows of graves with headstones, cypress, a shrine.
    d = D(13, 6, 11)
    d.fill(0, -1 + 1, 0, 12, 0, 10, M("grass_block")) if False else None
    d.walls(0, 0, 0, 12, 0, 10, M("stone_brick_wall"))
    d.set(6, 0, 0, AIR)
    d.set(5, 0, 0, M("stone_bricks")); d.set(7, 0, 0, M("stone_bricks"))
    d.set(5, 1, 0, LANTERN); d.set(7, 1, 0, LANTERN)
    for z in range(1, 8):
        d.set(6, 0, z, M("gravel")) if False else d.set(6, 0, z, AIR)
    for gz in (2, 4, 6):
        for gx in (2, 4, 8, 10):
            d.set(gx, 0, gz, M("stone_brick_wall"))
            d.set(gx, 0, gz - 1, M("coarse_dirt")) if False else d.set(gx, 0, gz - 1, M("moss_carpet"))
    for x, z in [(1, 9), (11, 9), (1, 1), (11, 1)]:
        d.fill(x, 0, z, x, 2, z, M("spruce_log"))
        d.set(x, 3, z, M("spruce_leaves[persistent=true]"))
    # The memorial shrine at the back.
    d.fill(4, 0, 8, 8, 0, 9, M("polished_andesite"))
    d.fill(5, 1, 9, 7, 1, 9, M("chiseled_stone_bricks"))
    d.set(6, 2, 9, M("chiseled_stone_bricks"))
    d.set(6, 1, 8, M("candle[candles=3,lit=true]"))
    for x in (4, 8):
        d.fill(x, 1, 8, x, 2, 8, RED)
    d.fill(4, 3, 7, 8, 3, 10, M("deepslate_tile_slab[type=bottom]"))
    return d


@design
def barbarian_village():
    # A mountain barbarian village: a log palisade, hide huts round a totem and a fire pit.
    d = D(17, 8, 17)
    for x in range(17):
        for z in range(17):
            if x in (0, 16) or z in (0, 16):
                d.fill(x, 0, z, x, 2, z, M("spruce_log"))
                if (x + z) % 2 == 0: d.set(x, 3, z, M("spruce_fence"))
    d.set(8, 0, 0, AIR); d.set(8, 1, 0, AIR); d.set(8, 2, 0, AIR)
    d.set(7, 3, 0, M("skeleton_skull[rotation=8]")); d.set(9, 3, 0, M("skeleton_skull[rotation=8]"))
    for hx, hz in [(3, 3), (11, 3), (3, 11), (11, 11)]:
        d.walls(hx - 1, 0, hz - 1, hx + 1, 1, hz + 1, M("brown_wool"))
        d.set(hx, 0, hz - 1 if hz > 8 else hz + 1, AIR); d.set(hx, 1, hz - 1 if hz > 8 else hz + 1, AIR)
        d.fill(hx - 1, 2, hz - 1, hx + 1, 2, hz + 1, M("hay_block"))
        d.set(hx, 3, hz, M("hay_block"))
    d.set(3, 0, 3, M("chest[facing=south]"))
    # The totem and the fire.
    d.fill(8, 0, 8, 8, 3, 8, M("stripped_dark_oak_log"))
    d.set(8, 4, 8, M("carved_pumpkin[facing=south]"))
    d.set(8, 5, 8, M("bone_block"))
    d.set(8, 0, 11, M("campfire[lit=true,facing=north]"))
    d.set(6, 0, 6, M("bone_block[axis=x]")); d.set(10, 0, 10, M("bone_block[axis=z]"))
    return d


@design
def bandit_fortress():
    # A great bandit stronghold: stone walls with corner towers, a gatehouse, barracks and the chief's keep.
    d = D(21, 11, 21)
    MS, CS = M("mossy_cobblestone"), M("cobblestone")
    for x in range(21):
        for z in range(21):
            if x in (0, 20) or z in (0, 20):
                d.fill(x, 0, z, x, 3, z, CS if (x * 7 + z * 3) % 4 else MS)
                if (x + z) % 2 == 0: d.set(x, 4, z, M("cobblestone_wall"))
    for x, z in [(0, 0), (20, 0), (0, 20), (20, 20)]:
        d.fill(x - (1 if x else 0), 0, z - (1 if z else 0), x + (0 if x else 1), 6, z + (0 if z else 1), CS)
        d.set(x, 7, z, M("campfire[lit=true,facing=north]"))
    d.clear(9, 0, 0, 11, 2, 0)
    d.fill(9, 3, 0, 11, 3, 0, M("spruce_planks"))
    # Barracks along the west wall.
    d.walls(2, 0, 3, 6, 2, 12, M("spruce_planks"))
    d.set(6, 0, 7, AIR); d.set(6, 1, 7, AIR)
    d.fill(2, 3, 3, 6, 3, 12, M("spruce_slab[type=bottom]"))
    for z in (5, 8, 11): d.set(3, 0, z, M("red_wool"))
    # The keep.
    d.walls(12, 0, 12, 18, 4, 18, M("stone_bricks"))
    d.set(15, 0, 12, AIR); d.set(15, 1, 12, AIR)
    d.fill(12, 5, 12, 18, 5, 18, M("stone_brick_slab[type=bottom]"))
    d.set(16, 0, 16, M("chest[facing=north]"))
    d.set(14, 0, 16, M("chest[facing=north]"))
    d.set(15, 2, 13, M("lantern[hanging=true]"))
    d.set(10, 0, 10, M("campfire[lit=true,facing=north]"))
    return d


@design
def ruined_city():
    # The ruins of an ancient city: broken walls, toppled columns, a roofless hall with its treasury.
    d = D(25, 8, 25)
    MS, MB, CB = M("mossy_stone_bricks"), M("cracked_stone_bricks"), M("chiseled_stone_bricks")
    for x in range(25):
        for z in range(25):
            if x in (0, 24) or z in (0, 24):
                h = (x * 13 + z * 7) % 4
                for y in range(h): d.set(x, y, z, MS if (x + y + z) % 3 else MB)
    for x, z in [(6, 6), (6, 18), (18, 6), (18, 18), (12, 4), (4, 12), (20, 12)]:
        h = 1 + (x + z) % 5
        d.fill(x, 0, z, x, h, z, M("stone_brick_wall") if h < 3 else MB)
        if h >= 4: d.set(x, h + 1, z, CB)
    d.walls(9, 0, 9, 15, 2, 15, MS)
    d.clear(12, 0, 9, 12, 1, 9)
    d.set(10, 0, 14, M("chest[facing=north]"))
    d.set(14, 0, 14, M("chest[facing=north]"))
    d.fill(11, 0, 13, 13, 0, 13, M("polished_andesite"))
    d.set(12, 1, 13, M("skeleton_skull[rotation=0]"))
    for x, z in [(3, 3), (21, 21), (3, 21), (21, 3)]:
        d.set(x, 0, z, M("cobweb"))
    return d


def main():
    for name, fn in SHIP_MODELS.items():
        print(f"ship {name}: {save_ship(fn(), name)} voxels")
    total = 0
    for name, fn in DESIGNS.items():
        n = fn().save(name)
        total += n
        print(f"{name}: {n} blocks")
    print(f"{len(DESIGNS)} designs, {total} blocks")


if __name__ == "__main__":
    main()
