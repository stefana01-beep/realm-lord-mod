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


def china(channel, detail, amount):
    return {"type": "minecraft:flat_cache", "argument": {
        "type": "realmlord:china_map", "channel": channel, "detail": detail, "detail_amount": amount}}


def main():
    # Climate inputs from the map, with vanilla's noise kept as fine detail.
    write(os.path.join(DF, "continents.json"), china("continents", "minecraft:overworld/continents", 0.12))
    write(os.path.join(DF, "erosion.json"), china("erosion", "minecraft:overworld/erosion", 0.2))
    write(os.path.join(DF, "ridges.json"), china("ridges", "minecraft:overworld/ridges", 1.0))
    # Vanilla's shaping functions, re-pointed at the map-driven inputs.
    for n in ["ridges_folded", "offset", "factor", "jaggedness", "depth", "sloped_cheese"]:
        src = rewire(json.load(open(os.path.join(V, "density_function", "overworld", n + ".json"))))
        if n == "offset":
            # Plateaus: Minecraft has no high flat land, so the map lifts it (1/128 per block).
            src = {"type": "minecraft:add", "argument1": src,
                   "argument2": {"type": "realmlord:china_map", "channel": "lift", "detail": 0.0, "detail_amount": 0.0}}
        write(os.path.join(DF, n + ".json"), src)
    ns = json.load(open(os.path.join(V, "noise_settings", "overworld.json")))
    ns = rewire(ns)
    r = ns["noise_router"]
    r["temperature"] = {"type": "realmlord:china_map", "channel": "temperature", "detail": r["temperature"], "detail_amount": 0.15}
    r["vegetation"] = {"type": "realmlord:china_map", "channel": "vegetation", "detail": r["vegetation"], "detail_amount": 0.15}
    write(os.path.join(OUT, "realmlord", "worldgen", "noise_settings", "china.json"), ns)
    preset = json.load(open(os.path.join(V, "world_preset", "normal.json")))
    preset["dimensions"]["minecraft:overworld"]["generator"]["settings"] = "realmlord:china"
    write(os.path.join(OUT, "realmlord", "worldgen", "world_preset", "china.json"), preset)
    write(os.path.join(OUT, "minecraft", "tags", "worldgen", "world_preset", "normal.json"), {"replace": False, "values": ["realmlord:china"]})
    print("china worldgen written")


main()
