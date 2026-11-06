#!/usr/bin/env python3
"""Writes the China world's worldgen data: copies of vanilla's terrain shaping functions that
read continentalness, erosion, ridges, temperature and humidity from the China map instead of
pure noise, a noise settings file, and the world preset.

Usage: python3 tools/gen_china_worldgen.py path/to/vanilla/data/minecraft/worldgen
"""
import copy
import json
import os
import sys

V = sys.argv[1]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data")
DF = os.path.join(OUT, "realmlord", "worldgen", "density_function", "china")

REWIRED = ["continents", "erosion", "ridges", "ridges_folded", "offset", "factor", "jaggedness", "depth", "sloped_cheese"]


def rewire(node):
    if isinstance(node, str):
        for n in REWIRED:
            if node == f"minecraft:overworld/{n}":
                return f"realmlord:china/{n}"
        return node
    if isinstance(node, list):
        return [rewire(x) for x in node]
    if isinstance(node, dict):
        return {k: rewire(v) for k, v in node.items()}
    return node


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def china(channel, detail, amount, fallback=None):
    f = {"type": "realmlord:china_map", "channel": channel, "detail": detail, "detail_amount": amount}
    if fallback is not None:
        f["fallback"] = fallback
    return {"type": "minecraft:flat_cache", "argument": f}


TOP = 576          # the China world's build height (vanilla: 320)
MIN_Y = -64


def raise_ceiling(node):
    """Vanilla fades terrain out between y 240 and 256; the taller world fades it at 528-544."""
    if isinstance(node, list):
        return [raise_ceiling(x) for x in node]
    if isinstance(node, dict):
        if node.get("type") == "minecraft:y_clamped_gradient" and node.get("from_y") == 240 and node.get("to_y") == 256:
            return dict(node, from_y=TOP - 48, to_y=TOP - 32)
        return {k: raise_ceiling(v) for k, v in node.items()}
    return node


def main():
    # Climate inputs from the map, with vanilla's noise kept as fine detail.
    write(os.path.join(DF, "continents.json"), china("continents", "minecraft:overworld/continents", 0.12))
    write(os.path.join(DF, "erosion.json"), china("erosion", "minecraft:overworld/erosion", 0.2))
    write(os.path.join(DF, "ridges.json"), china("ridges", "minecraft:overworld/ridges", 1.0))
    # Vanilla's shaping functions, re-pointed at the map-driven inputs.
    for n in ["ridges_folded", "offset", "factor", "jaggedness", "depth", "sloped_cheese"]:
        src = rewire(json.load(open(os.path.join(V, "density_function", "overworld", n + ".json"))))
        if n == "offset":
            # The land's real height; vanilla's own shaping only beyond the map's edges.
            src = china("offset", "minecraft:overworld/ridges", 0.0, src)
        if n == "factor":
            # Flat land keeps its real height; only rugged country gets vanilla's full roughness.
            src = china("factor", src, 0.0)
        if n == "depth":
            # The same slope as vanilla (a surface at y has offset y/128 - 1), up to the taller top.
            src["argument1"] = {"type": "minecraft:y_clamped_gradient", "from_y": MIN_Y, "from_value": 1.5,
                                "to_y": TOP, "to_value": 1.5 - (TOP - MIN_Y) / 128.0}
        write(os.path.join(DF, n + ".json"), src)
    ns = json.load(open(os.path.join(V, "noise_settings", "overworld.json")))
    ns = raise_ceiling(rewire(ns))
    ns["noise"]["min_y"] = MIN_Y
    ns["noise"]["height"] = TOP - MIN_Y
    r = ns["noise_router"]
    r["temperature"] = {"type": "realmlord:china_map", "channel": "temperature", "detail": r["temperature"], "detail_amount": 0.15}
    r["vegetation"] = {"type": "realmlord:china_map", "channel": "vegetation", "detail": r["vegetation"], "detail_amount": 0.15}
    write(os.path.join(OUT, "realmlord", "worldgen", "noise_settings", "china.json"), ns)
    # A taller overworld for the China world: y -64 to 576.
    write(os.path.join(OUT, "realmlord", "dimension_type", "china.json"), {
        "ambient_light": 0.0, "bed_works": True, "coordinate_scale": 1.0, "effects": "minecraft:overworld",
        "has_ceiling": False, "has_raids": True, "has_skylight": True, "height": TOP - MIN_Y,
        "infiniburn": "#minecraft:infiniburn_overworld", "logical_height": TOP - MIN_Y, "min_y": MIN_Y,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"max_inclusive": 7, "min_inclusive": 0}},
        "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "ultrawarm": False})
    preset = json.load(open(os.path.join(V, "world_preset", "normal.json")))
    preset["dimensions"]["minecraft:overworld"]["generator"]["settings"] = "realmlord:china"
    preset["dimensions"]["minecraft:overworld"]["type"] = "realmlord:china"
    # No ready-made villages: settlements are the players' own (and, later, the great capitals).
    for v in ["plains", "desert", "savanna", "snowy", "taiga"]:
        write(os.path.join(OUT, "minecraft", "tags", "worldgen", "biome", "has_structure", f"village_{v}.json"), {"replace": True, "values": []})
    # Water in upland rivers, which sit above the sea's level.
    write(os.path.join(OUT, "realmlord", "worldgen", "configured_feature", "river_water.json"), {"type": "realmlord:river_water", "config": {}})
    write(os.path.join(OUT, "realmlord", "worldgen", "placed_feature", "river_water.json"), {"feature": "realmlord:river_water", "placement": []})
    write(os.path.join(OUT, "realmlord", "forge", "biome_modifier", "river_water.json"),
          {"type": "forge:add_features", "biomes": "#minecraft:is_overworld", "features": "realmlord:river_water", "step": "lakes"})
    write(os.path.join(OUT, "realmlord", "worldgen", "world_preset", "china.json"), preset)
    write(os.path.join(OUT, "minecraft", "tags", "worldgen", "world_preset", "normal.json"), {"replace": False, "values": ["realmlord:china"]})
    print("china worldgen written")


main()
