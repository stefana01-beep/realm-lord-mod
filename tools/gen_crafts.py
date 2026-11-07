"""Technical manuals for every work occupation, the things they teach players to make, and the
martial arts secret manuals, after the novel: "<Mingguang Armor Manufacturing Technology
Manual>: After use, gain knowledge of crafting the mingguang armor."

This one table is the source for:
  * the gated recipes (type realmlord:manual, wrapping an ordinary crafting recipe),
  * the Java tables (work/TechData.java and martial/ArtData.java),
  * names and descriptions,
  * the textures and models of every new item, and the new armour layers,
  * manual entries in the ruins' loot tables (via gen_structures).
"""
import json
import math
import os
import random

from PIL import Image

import gen_arms
from gen_goods import hexc, shade, blank

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "realmlord")
DATA = os.path.join(ROOT, "data", "realmlord")
JAVA = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "com", "realmlord")


# ---------------------------------------------------------------- recipe helpers

def ing(x):
    if isinstance(x, dict): return x
    if x.startswith("#"): return {"tag": x[1:] if ":" in x else "minecraft:" + x[1:]}
    if ":" not in x: x = "minecraft:" + x
    return {"item": x}


def full(x):
    return x if ":" in x else "minecraft:" + x


def shaped(pattern, key, result, count=1, nbt=None):
    r = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: ing(v) for k, v in key.items()},
         "result": {"item": full(result), "count": count}}
    if nbt: r["result"]["nbt"] = nbt
    return r


def shapeless(items, result, count=1):
    return {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [ing(i) for i in items],
            "result": {"item": full(result), "count": count}}


RL = lambda n: "realmlord:" + n
J, I, A, M = 0, 1, 2, 3     # Junior, Intermediate, Advanced, Master: the trade rank needed to learn
PRICE = {J: 5_000, I: 30_000, A: 100_000, M: 300_000}

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"]


def T(id, trade, tier, name, desc, recipes=None, passive=False, novel=False, structures=()):
    return {"id": id, "trade": trade, "tier": tier, "name": name, "desc": desc, "recipes": recipes or {}, "passive": passive, "novel": novel,
            "structures": list(structures)}


def S(id, trade, tier, name, desc, structure, novel=False):
    """A structure manual: its building plan comes with it."""
    return T(id, trade, tier, name, desc, novel=novel, structures=[structure])


TECHS = [
    # ------------------------------------------------------------ Blacksmith
    T("ironware_casting", "BLACKSMITH", J, "Ironware Casting Manual", "Casting chain, iron bars and cauldrons with less iron.", {
        "chain_cast": shapeless(["iron_ingot", "iron_ingot", "iron_nugget"], "chain", 6),
        "iron_bars_cast": shaped(["II", "II"], {"I": "iron_ingot"}, "iron_bars", 16),
        "cauldron_cast": shapeless(["iron_ingot"] * 4 + ["coal"], "cauldron")}),
    T("horse_barding", "BLACKSMITH", I, "Horse Barding Manual", "Iron, gold and diamond barding for war horses.", {
        "iron_horse_armor": shaped(["I I", "ILI", "I I"], {"I": "iron_ingot", "L": "leather"}, "iron_horse_armor"),
        "golden_horse_armor": shaped(["I I", "ILI", "I I"], {"I": "gold_ingot", "L": "leather"}, "golden_horse_armor"),
        "diamond_horse_armor": shaped(["I I", "ILI", "I I"], {"I": "diamond", "L": "leather"}, "diamond_horse_armor")}),
    T("perfusing_steel", "BLACKSMITH", I, "Perfusing Steel Technique", "The Northern and Southern dynasties' steelmaking: molten pig iron poured over wrought iron. Refined steel without tiger bone, and black iron straight from raw iron.", {
        "refined_steel_perfused": shapeless([RL("black_iron_ingot"), RL("black_iron_ingot"), "iron_ingot", "coal"], RL("refined_steel"), 2),
        "black_iron_from_raw": shapeless(["raw_iron", "raw_iron", "raw_iron", "charcoal"], RL("black_iron_ingot"), 2)}, novel=True),
    T("tang_sword_forging", "BLACKSMITH", I, "Tang Sword Forging Manual", "The Tang hengdao, straight and single-edged, and the great mo dao that cut down horse and rider.", {
        "tang_hengdao": shaped(["  R", " R ", "L  "], {"R": RL("refined_steel"), "L": "leather"}, RL("tang_hengdao")),
        "mo_dao": shaped(["  R", " RR", "S  "], {"R": RL("refined_steel"), "S": "stick"}, RL("mo_dao"))}, novel=True),
    T("buren_armour", "BLACKSMITH", I, "Song Dynasty Buren Armor Making Manual", "The Song infantry's heavy armour of nearly two thousand scales: hard to knock down.", {
        "buren_helmet": shaped(["PRP", "P P"], {"P": RL("lamellar_plates"), "R": RL("refined_steel")}, RL("buren_helmet")),
        "buren_armour": shaped(["P P", "RPR", "PPP"], {"P": RL("lamellar_plates"), "R": RL("refined_steel")}, RL("buren_armour")),
        "buren_greaves": shaped(["PRP", "P P", "P P"], {"P": RL("lamellar_plates"), "R": RL("refined_steel")}, RL("buren_greaves")),
        "buren_boots": shaped(["R R", "P P"], {"P": RL("lamellar_plates"), "R": RL("refined_steel")}, RL("buren_boots"))}, novel=True),
    T("mingguang_armour", "BLACKSMITH", A, "Mingguang Armor Manufacturing Technology Manual", "The bright-light armour of the Tang cavalry, two polished mirrors on the breast.", {
        "mingguang_helmet": shaped(["RGR", "R R"], {"R": RL("refined_steel"), "G": "gold_ingot"}, RL("mingguang_helmet")),
        "mingguang_armour": shaped(["R R", "GCG", "RRR"], {"R": RL("refined_steel"), "G": "gold_ingot", "C": RL("beast_core")}, RL("mingguang_armour")),
        "mingguang_greaves": shaped(["RGR", "R R", "R R"], {"R": RL("refined_steel"), "G": "gold_ingot"}, RL("mingguang_greaves")),
        "mingguang_boots": shaped(["R R", "G G"], {"R": RL("refined_steel"), "G": "gold_ingot"}, RL("mingguang_boots"))}, novel=True),
    T("hundred_refined", "BLACKSMITH", M, "Hundred-Refined Steel Manual", "Forge a steel blade again, folding in three more bars of refined steel and gold: it comes out Gold grade without a beast core.", {
        f"{w}_hundred_refined": shapeless([RL(w), RL("refined_steel"), RL("refined_steel"), RL("refined_steel"), "gold_ingot"], RL(w))
        for w in ["steel_jian", "guandao", "ji", "tang_hengdao", "mo_dao"]}),
    # ------------------------------------------------------------ Tailor
    T("tanning", "TAILOR", J, "Tanning Manual", "Curing pelts into good leather.", {
        "leather_from_wolf_pelt": shapeless([RL("wolf_pelt")], "leather", 3),
        "leather_from_boar_hide": shapeless([RL("boar_hide")], "leather", 3),
        "leather_from_leopard_pelt": shapeless([RL("leopard_pelt")], "leather", 4),
        "leather_from_tiger_pelt": shapeless([RL("tiger_pelt")], "leather", 5),
        "leather_from_rabbit_hide": shapeless(["rabbit_hide", "rabbit_hide"], "leather")}),
    T("tack_and_felting", "TAILOR", J, "Saddlery and Felting Manual", "Saddles and name tags.", {
        "saddle_sewn": shaped(["LLL", "LSL", " N "], {"L": "leather", "S": "string", "N": "iron_nugget"}, "saddle"),
        "name_tag_sewn": shapeless(["paper", "string", "iron_nugget"], "name_tag")}),
    T("silk_reeling", "TAILOR", I, "Silk Reeling and Dyeing Manual", "Reeling silk from cocoons, and dyeing the top-quality rainbow silk cloth.", {
        "fine_silk_reeled": shapeless([RL("silk_cocoon")] * 4, RL("fine_silk")),
        "rainbow_silk": shapeless([RL("fine_silk"), "red_dye", "yellow_dye", "blue_dye"], RL("rainbow_silk"))}, novel=True),
    T("brigandine", "TAILOR", I, "Cloth-Faced Armour Manual", "The Ming brigandine: plates riveted inside a padded linen coat, the brass studs showing.", {
        "brigandine_helmet": shaped(["LNL", "E E"], {"L": RL("linen"), "N": "iron_nugget", "E": "leather"}, RL("brigandine_helmet")),
        "brigandine_coat": shaped(["L L", "NEN", "LNL"], {"L": RL("linen"), "N": "iron_nugget", "E": "leather"}, RL("brigandine_coat")),
        "brigandine_trousers": shaped(["LNL", "N N", "L L"], {"L": RL("linen"), "N": "iron_nugget"}, RL("brigandine_trousers")),
        "brigandine_boots": shaped(["L L", "E E"], {"L": RL("linen"), "E": "leather"}, RL("brigandine_boots"))}),
    T("night_clothes", "TAILOR", A, "Night Clothes Pattern", "The night-walker's black hood and suit. Crouching in the dark, unseen beyond a few paces.", {
        "night_hood": shaped(["BLB", "L L"], {"B": "black_dye", "L": RL("linen")}, RL("night_hood")),
        "night_suit": shaped(["L L", "BLB", "LBL"], {"B": "black_dye", "L": RL("linen")}, RL("night_suit"))}),
    T("dragon_robe", "TAILOR", M, "Imperial Embroidery Manual", "The yellow dragon robe. Whoever wears it is received like a hero.", {
        "dragon_robe": shaped(["S S", "RGR", "SRS"], {"S": RL("fine_silk"), "R": RL("rainbow_silk"), "G": "gold_ingot"}, RL("dragon_robe"))}),
    # ------------------------------------------------------------ Physician
    T("herbal_formulary", "PHYSICIAN", J, "Basic Herbal Formulary", "Gathering herbs from wild flowers, and a detox draught against poison.", {
        "herbs_from_flowers": shapeless(["#small_flowers", "#small_flowers", "wheat_seeds"], RL("medicinal_herbs"), 2),
        "detox_draught": shapeless(["glass_bottle", RL("medicinal_herbs"), RL("medicinal_herbs"), "sugar"], RL("detox_draught"))}),
    T("clear_heart_pill", "PHYSICIAN", J, "Clear Heart Pill Formula", "A pill that clears the head and limbs of every sluggishness.", {
        "clarity_pill": shapeless([RL("medicinal_herbs"), RL("medicinal_herbs"), "sugar", "glowstone_dust"], RL("clarity_pill"), 2)}),
    T("acupuncture", "PHYSICIAN", I, "Yellow Emperor's Acupuncture Canon", "Needles that heal another (or yourself) and clear what ails them.", {
        "acupuncture_needles": shapeless(["iron_nugget", "iron_nugget", "iron_nugget", "gold_nugget", RL("medicinal_herbs")], RL("acupuncture_needles"))}),
    T("rhino_horn_pill", "PHYSICIAN", I, "Rhino Horn Poison Curing Pill Formula", "Bianque's antidote: cures poison and wither, and wards them off for two minutes.", {
        "rhino_horn_pill": shapeless([RL("ox_horn"), RL("medicinal_herbs"), RL("medicinal_herbs"), "gold_nugget"], RL("rhino_horn_pill"), 2)}, novel=True),
    T("peiyuan_pill", "PHYSICIAN", A, "Peiyuan Pill Formula", "Solidifies the primordial Qi: effective for the badly injured.", {
        "peiyuan_pill": shapeless([RL("healing_pill"), RL("healing_pill"), RL("bear_paw"), "gold_ingot"], RL("peiyuan_pill"), 2)}, novel=True),
    T("bianque_classic", "PHYSICIAN", M, "Bianque's Medical Classic", "The special pills without a beast core.", {
        "marrow_cleansing_pill_classic": shapeless([RL("tiger_bone"), RL("tiger_bone"), RL("healing_pill"), RL("healing_pill"), "gold_ingot", RL("medicinal_herbs")], RL("marrow_cleansing_pill")),
        "enlightenment_pill_classic": shapeless([RL("three_flower_wine"), RL("three_flower_wine"), RL("healing_pill"), RL("healing_pill"), "gold_ingot", RL("medicinal_herbs")], RL("enlightenment_pill"))}),
    # ------------------------------------------------------------ Architect
    T("rammed_earth", "ARCHITECT", J, "Rammed Earth Manual", "Packed earth walls from dirt, gravel and clay.", {
        "rammed_earth": shapeless(["dirt", "dirt", "gravel", "clay_ball"], "packed_mud", 4)}),
    T("lime_mortar", "ARCHITECT", J, "Lime Mortar Manual", "Stone bricks laid in lime mortar, no kiln needed.", {
        "stone_bricks_mortared": shaped(["CCC", "CBC", "CCC"], {"C": "cobblestone", "B": "bone_meal"}, "stone_bricks", 8)}),
    T("glass_making", "ARCHITECT", I, "Glass Making Technical Manual", "Glass from sand and coal, and more panes from each pane of glass.", {
        "glass_made": shapeless(["sand", "sand", "sand", "sand", "coal"], "glass", 4),
        "glass_panes_made": shaped(["GGG"], {"G": "glass"}, "glass_pane", 16)}, novel=True),
    T("glazed_tiles", "ARCHITECT", I, "Glazed Tile Manual", "Imperial yellow and jade-green glazed tiles, and slate roof tiles.", {
        "yellow_glazed_tiles": shapeless(["terracotta"] * 4 + ["yellow_dye", "bone_meal"], "yellow_glazed_terracotta", 4),
        "green_glazed_tiles": shapeless(["terracotta"] * 4 + ["green_dye", "bone_meal"], "green_glazed_terracotta", 4),
        "slate_roof_tiles": shapeless(["cobbled_deepslate"] * 4 + ["clay_ball"], "deepslate_tiles", 4)}),
    T("luopan", "ARCHITECT", I, "Luopan Geomancy Manual", "The geomancer's compass: reads the land wherever you stand.", {
        "luopan": shapeless(["compass", "gold_nugget", "#planks"], RL("luopan"))}),
    T("yingzao_fashi", "ARCHITECT", A, "Yingzao Fashi", "The Song court's building standards: one block in five you place is not used up.", passive=True),
    T("kaogong_ji", "ARCHITECT", M, "Kaogong Ji", "The Records of Trades of the Rites of Zhou: two more blocks of reach.", passive=True),
    # ------------------------------------------------------------ Breeder
    T("qimin_fodder", "BREEDER", J, "Qimin Yaoshu: Fodder", "Fodder that readies any farm animal to breed, or grows a young one.", {
        "fodder": shapeless(["wheat", "wheat", "carrot", "wheat_seeds"], RL("fodder"), 4)}),
    T("sericulture", "BREEDER", J, "Sericulture Manual", "Raising silkworms on leaves for their cocoons.", {
        "silk_cocoon": shapeless(["#leaves", "#leaves", "#leaves", "string"], RL("silk_cocoon"), 2)}),
    T("bole_horse_classic", "BREEDER", I, "Bole's Classic of Horses", "Horse feed: heals, quickens and gentles a horse.", {
        "horse_feed": shapeless(["wheat", "apple", "carrot", "sugar"], RL("horse_feed"), 4)}),
    T("panda_rearing", "BREEDER", A, "Panda Rearing Technical Manual", "Bamboo feed that makes pandas breed and cubs grow.", {
        "bamboo_feed": shapeless(["bamboo", "bamboo", "bamboo", "bamboo", "sugar_cane", "apple"], RL("bamboo_feed"), 2)}, novel=True),
    T("wild_beast_taming", "BREEDER", A, "Wild Beast Taming Technique", "The wild era's taming art: wolves, cats, parrots and horses take to you at the first try.", passive=True, novel=True),
    T("iron_armored_beast", "BREEDER", M, "Iron Armored Beast Domestication Manual", "Animals you tame come out tougher: +20 health and +6 armour.", passive=True, novel=True),
    # ------------------------------------------------------------ Fisherman
    T("net_weaving", "FISHERMAN", J, "Net Weaving Manual", "A casting net: throw it on water for fish.", {
        "fishing_net": shaped(["S S", "SSS", "S S"], {"S": "string"}, RL("fishing_net"))}),
    T("sea_salt", "FISHERMAN", J, "Salt from Sea Water", "Boiling sea water down for salt.", {
        "sea_salt": shapeless(["water_bucket", "coal"], RL("salt"), 4)}, novel=True),
    T("dried_fish", "FISHERMAN", J, "Salted Fish Manual", "Salted, dried fish that keeps and fills.", {
        "dried_cod": shapeless(["cod", RL("salt")], RL("dried_fish"), 2),
        "dried_salmon": shapeless(["salmon", RL("salt")], RL("dried_fish"), 2)}),
    T("star_guiding", "FISHERMAN", I, "Star Guiding Technique", "Zheng He's navigation by the stars: see in the dark on the water, and swim like a dolphin.", passive=True, novel=True),
    T("pearl_diving", "FISHERMAN", I, "Pearl Diving Manual", "Hold your breath far longer under water.", passive=True),
    T("cormorant_fishing", "FISHERMAN", A, "Cormorant Fishing Manual", "Trained cormorants bring in a third more fish.", passive=True),
    T("mazu_charts", "FISHERMAN", M, "Mazu's Sea Charts", "The sea goddess's favour: one catch in ten brings a treasure up with it.", passive=True),
    # ------------------------------------------------------------ Farmer
    T("compost", "FARMER", J, "Compost Manual", "Turning scraps and leaves into fertiliser.", {
        "compost": shapeless(["rotten_flesh", "#leaves", "wheat_seeds", "dirt"], "bone_meal", 6)}),
    T("mushroom_growing", "FARMER", J, "Underground Mushroom Growing Technical Manual", "Mushroom beds in the dark.", {
        "brown_mushroom_bed": shapeless(["brown_mushroom", "bone_meal", "dirt"], "brown_mushroom", 4),
        "red_mushroom_bed": shapeless(["red_mushroom", "bone_meal", "dirt"], "red_mushroom", 4)}, novel=True),
    T("tea_cultivation", "FARMER", I, "Tea Cultivation Manual", "Plucking and drying tea leaves.", {
        "tea_leaves": shapeless(["#leaves", "#leaves", "#leaves", "bone_meal"], RL("tea_leaves"), 2)}),
    T("seed_selection", "FARMER", I, "Seed Selection Manual", "Crops you harvest replant themselves.", passive=True),
    T("spring_weathering", "FARMER", A, "Spring Weathering Talisman", "A talisman that brings spring to the fields around it at once.", {
        "spring_talisman": shapeless(["paper", "bone_meal", "bone_meal", "bone_meal", "bone_meal", RL("medicinal_herbs"), "gold_nugget"], RL("spring_talisman"))}, novel=True),
    T("fan_shengzhi", "FARMER", M, "Fan Shengzhi Shu", "The Han agricultural classic: crops around you grow faster.", passive=True),
    # ------------------------------------------------------------ Veterinarian
    T("animal_salve", "VETERINARIAN", J, "Animal Salve Formula", "A salve that heals an animal fully.", {
        "animal_salve": shapeless([RL("medicinal_herbs"), RL("medicinal_herbs"), "honeycomb"], RL("animal_salve"), 2)}),
    T("horse_medicine", "VETERINARIAN", I, "Yuan Heng's Horse Remedies", "A tonic that makes a horse fast and sure-footed for five minutes.", {
        "horse_tonic": shapeless(["glass_bottle", RL("medicinal_herbs"), "golden_carrot", "sugar"], RL("horse_tonic"))}),
    T("beast_whistle", "VETERINARIAN", I, "Beast Whistle Manual", "A tusk whistle that calls all your animals to you.", {
        "beast_whistle": shapeless([RL("boar_tusk"), "gold_nugget", "string"], RL("beast_whistle"))}),
    T("guardian_charm", "VETERINARIAN", A, "Guardian Charm Manual", "Carried, it saves one of your animals from death once.", {
        "guardian_charm": shapeless([RL("beast_sinew"), "gold_ingot", RL("medicinal_herbs"), "red_dye"], RL("guardian_charm"))}),
    T("hundred_beasts", "VETERINARIAN", M, "Hundred Beasts Classic", "Your tamed animals strike a third harder and heal as they fight.", passive=True),
    # ------------------------------------------------------------ Winemaker
    T("three_flower_wine", "WINEMAKER", J, "Three Flower Wine Recipe", "Lianzhou's famous Three Flower Wine.", {
        "three_flower_wine": shapeless(["glass_bottle", "glass_bottle", "wheat", "sugar", "poppy", "dandelion", "oxeye_daisy"], RL("three_flower_wine"), 2)}, novel=True),
    T("soy_sauce", "WINEMAKER", J, "Sauce Brewing Manual", "Soy sauce for the cook.", {
        "soy_sauce": shapeless(["glass_bottle", "wheat", "wheat", RL("salt")], RL("soy_sauce"))}),
    T("medicinal_wine", "WINEMAKER", J, "Medicinal Wine Recipe", "Herbs steeped in wine: heals and steadies.", {
        "medicinal_wine": shapeless([RL("three_flower_wine"), RL("medicinal_herbs"), RL("medicinal_herbs")], RL("medicinal_wine"))}),
    T("tiger_bone_wine", "WINEMAKER", I, "Tiger Bone Wine Recipe", "A tiger bone steeped in wine.", {
        "tiger_bone_wine_steeped": shapeless([RL("three_flower_wine"), RL("three_flower_wine"), RL("tiger_bone")], RL("tiger_bone_wine"), 2)}),
    T("dukang_wine", "WINEMAKER", I, "Dukang Wine Recipe", "The wine of Du Kang, father of wine.", {
        "dukang_wine": shapeless(["glass_bottle", "glass_bottle", "wheat", "wheat", "wheat", "honey_bottle"], RL("dukang_wine"), 2)}, novel=True),
    T("maotai", "WINEMAKER", A, "Maotai Distilling Manual", "The fiery spirit of Maotai.", {
        "maotai": shapeless(["glass_bottle", "glass_bottle", "wheat", "wheat", "beetroot", "sugar", RL("medicinal_herbs")], RL("maotai"), 2)}, novel=True),
    T("immortal_brew", "WINEMAKER", M, "Drunken Immortal's Brew", "Li Bai's own: luck and strength for ten minutes.", {
        "immortal_wine": shapeless([RL("maotai"), RL("dukang_wine"), "glistering_melon_slice", "gold_ingot"], RL("immortal_wine"))}),
    # ------------------------------------------------------------ Cook
    T("dumplings", "COOK", J, "Dumpling Recipe", "Pork dumplings, four at a time.", {
        "dumplings": shapeless(["wheat", "wheat", "porkchop", "carrot"], RL("dumplings"), 4)}),
    T("rice_noodles", "COOK", J, "Rice Noodle Recipe", "A bowl of beef noodle soup.", {
        "noodle_soup": shapeless(["bowl", "wheat", "wheat", "cooked_beef", RL("soy_sauce")], RL("noodle_soup"))}, novel=True),
    T("tea_brewing", "COOK", J, "Tea Classic", "Lu Yu's art of tea: green tea that sharpens the hands.", {
        "green_tea": shapeless(["glass_bottle", RL("tea_leaves"), RL("tea_leaves")], RL("green_tea"))}, novel=True),
    T("mooncakes", "COOK", I, "Mooncake Recipe", "Mooncakes for the mid-autumn moon.", {
        "mooncake": shapeless(["wheat", "wheat", "sugar", "egg", "pumpkin_seeds"], RL("mooncake"), 4)}),
    T("military_grain_pill", "COOK", I, "Military Grain Pill Recipe", "One small pill sustains a soldier for a day.", {
        "military_grain_pill": shapeless(["wheat", "wheat", "wheat", "#realmlord:cooked_meats", RL("salt"), RL("medicinal_herbs")], RL("military_grain_pill"), 4)}, novel=True),
    T("roast_duck", "COOK", A, "Roast Duck Recipe", "Lacquered, glazed and roasted.", {
        "roast_duck": shapeless(["cooked_chicken", "honey_bottle", RL("soy_sauce"), "wheat"], RL("roast_duck"))}),
    T("manchu_han_feast", "COOK", M, "Manchu Han Imperial Feast", "The imperial banquet: one dish of it fills you and lends every strength.", {
        "imperial_feast": shapeless([RL("roast_duck"), RL("braised_bear_paw"), RL("mooncake"), RL("dumplings"), RL("noodle_soup"), RL("maotai")], RL("imperial_feast"))}),
    # ------------------------------------------------------------ Carpenter
    T("lu_ban_joinery", "CARPENTER", J, "Lu Ban's Joinery", "Two logs make twelve planks.", {
        f"{w}_planks_joined": shapeless([f"#{w}_logs", f"#{w}_logs"], f"{w}_planks", 12) for w in WOODS}),
    T("movable_type", "CARPENTER", J, "Movable Type Printing", "Bi Sheng's press: paper and books in quantity.", {
        "paper_pressed": shapeless(["sugar_cane", "sugar_cane", "sugar_cane", "water_bucket"], "paper", 6),
        "book_printed": shapeless(["paper", "paper", "paper", "leather", "ink_sac"], "book", 2)}),
    T("song_bow_crossbow", "CARPENTER", I, "Song Dynasty Bow and Crossbow Making Manual", "The composite bow of birch, sinew and fish glue, and Zhuge Liang's repeating crossbow.", {
        "composite_bow": shaped(["GBW", "S W", "GBW"], {"G": "slime_ball", "B": "birch_planks", "S": "stick", "W": RL("beast_sinew")}, RL("composite_bow")),
        "zhuge_crossbow": shaped(["BIB", "WRW", " B "], {"B": "bamboo", "I": "iron_ingot", "W": RL("beast_sinew"), "R": "repeater"}, "crossbow",
                                 nbt='{Enchantments:[{id:"minecraft:quick_charge",lvl:2s},{id:"minecraft:multishot",lvl:1s}],display:{Name:\'{"translate":"item.realmlord.zhuge_crossbow","italic":false}\'}}')}, novel=True),
    T("south_pointing_chariot", "CARPENTER", A, "South-Pointing Chariot Technical Manual", "The geared carriage whose figure always points south, and here also home.", {
        "south_pointing_chariot": shapeless(["compass", "gold_ingot", "#planks", "#planks", "redstone"], RL("south_pointing_chariot"))}, novel=True),
    T("wooden_ox", "CARPENTER", M, "Wooden Ox and Flowing Horse", "Zhuge Liang's pack carrier: a saddled, laden mule that follows you.", {
        "wooden_ox": shapeless(["chest", "#logs", "#logs", "#logs", "#logs", "redstone", "lead"], RL("wooden_ox"))}),
    T("substitute_doll", "CARPENTER", M, "Substitute Doll Carving", "A wooden doll that dies in your place, once.", {
        "substitute_doll": shapeless(["#logs", "gold_ingot", RL("beast_core"), RL("linen")], RL("substitute_doll"))}, novel=True),
    # ------------------------------------------------------------ Mason
    T("stone_dressing", "MASON", J, "Stone Dressing Manual", "Cobblestone dressed into bricks; cracked and carved bricks.", {
        "stone_bricks_dressed": shapeless(["cobblestone"] * 4, "stone_bricks", 4),
        "cracked_bricks_dressed": shapeless(["stone_bricks", "gravel"], "cracked_stone_bricks"),
        "chiseled_bricks_dressed": shapeless(["stone_bricks", "stone_bricks", "flint"], "chiseled_stone_bricks", 2)}),
    T("coal_mining", "MASON", J, "Coal Mining Technical Manual", "Coal, iron, copper and gold ore you mine now and then yield more.", passive=True, novel=True),
    T("gunpowder", "MASON", I, "Gunpowder Making Technical Manual", "Charcoal, saltpetre (bone meal) and flint.", {
        "gunpowder_made": shapeless(["charcoal", "charcoal", "bone_meal", "flint"], "gunpowder", 2)}, novel=True),
    T("porcelain", "MASON", I, "Jingdezhen Porcelain Manual", "Blue-and-white porcelain vases: the market pays well.", {
        "porcelain_vase": shapeless(["clay_ball", "clay_ball", "clay_ball", "clay_ball", "bone_meal", "blue_dye"], RL("porcelain_vase"), 2)}),
    T("jade_carving", "MASON", A, "Jade Carving Manual", "A jade pendant: held in the off hand, it brings luck and health.", {
        "jade_pendant": shapeless(["emerald", "gold_nugget", "string"], RL("jade_pendant"))}),
    T("great_wall", "MASON", M, "Great Wall Masonry", "Deepslate bricks by the eight, and stone breaks half again as fast in your hands.", {
        "deepslate_bricks_walled": shaped(["CCC", "CBC", "CCC"], {"C": "cobbled_deepslate", "B": "clay_ball"}, "deepslate_bricks", 8)}, passive=True),
    # ------------------------------------------------------------ Shipbuilder
    T("pine_tar", "SHIPBUILDER", J, "Pine Tar Caulking Manual", "Pine tar for hulls, and tar torches.", {
        "pine_tar": shapeless(["#spruce_logs", "#spruce_logs", "coal"], RL("pine_tar"), 4),
        "tar_torches": shapeless([RL("pine_tar"), "stick", "stick"], "torch", 8)}),
    T("boat_joinery", "SHIPBUILDER", J, "Boat Joinery Manual", "Tarred boats from three planks.", {
        f"{w}_boat_tarred": shapeless([f"{w}_planks"] * 3 + [RL("pine_tar")], f"{w}_boat") for w in WOODS}),
    T("rope_and_anchor", "SHIPBUILDER", J, "Rope and Rigging Manual", "Rope from kelp, and leads.", {
        "kelp_string": shapeless(["dried_kelp", "dried_kelp", "dried_kelp"], "string", 2),
        "lead_rigged": shapeless(["string", "string", "string"], "lead", 2)}),
    T("lifebuoy", "SHIPBUILDER", I, "Lifebuoy Manual", "Held in the off hand in water: breathe and swim fast.", {
        "lifebuoy": shaped([" W ", "WRW", " W "], {"W": "white_wool", "R": "red_dye"}, RL("lifebuoy"))}),
    T("ming_shipbuilding", "SHIPBUILDER", A, "Ming Dynasty Shipbuilding Technical Manual", "Harbour lights and the diving bell for hull work.", {
        "diving_bell": shaped(["III", "G G", "GGG"], {"I": "iron_ingot", "G": "glass"}, RL("diving_bell")),
        "sea_lantern_harbour": shapeless(["glass", "glowstone_dust", "glowstone_dust", "dried_kelp"], "sea_lantern", 2)}, novel=True),
    T("meng_chong", "SHIPBUILDER", A, "Meng Chong Warship Manufacturing Manual", "The rawhide-covered warship built to ram: four seats, four times as tough as a boat; it strikes and throws aside what it runs down.", {
        "mengchong_warship": shaped(["LIL", "PFP", "PTP"], {"L": "leather", "I": "iron_ingot", "F": RL("fishing_boat"), "P": "#planks", "T": RL("pine_tar")}, RL("mengchong_warship"))}, novel=True),
    T("fishing_boat", "SHIPBUILDER", J, "Fishing Boat Manual", "An ordinary fishing boat that carries two fishermen; fish caught from it come in a third more often.", {
        "fishing_boat": shaped(["P P", "PTP", "PPP"], {"P": "#planks", "T": RL("pine_tar")}, RL("fishing_boat"))}, novel=True),
    T("cima_boat", "SHIPBUILDER", I, "Cima Boat Manual", "The fleet's scout: a red-sailed boat half again as fast as any other.", {
        "cima_boat": shaped([" R ", "PTP", "PPP"], {"R": "red_wool", "P": "#planks", "T": RL("pine_tar")}, RL("cima_boat"))}, novel=True),
    T("merchant_ship", "SHIPBUILDER", I, "Merchant Ship Manual", "A trading ship: three seats and a hold.", {
        "merchant_ship": shaped(["WCW", "PTP", "PPP"], {"W": "white_wool", "C": "chest", "P": "#planks", "T": RL("pine_tar")}, RL("merchant_ship"))}, novel=True),
    T("turreted_ship", "SHIPBUILDER", M, "Turreted Ship Construction Manual", "The floating fortress with watertight bulkheads: six seats, seven times as tough, crossbowmen who shoot at foes and clapper poles that strike those close. Its master has Conduit Power in water.", {
        "turreted_ship": shaped(["RWR", "LML", "PTP"], {"R": RL("refined_steel"), "W": "red_wool", "L": "#logs", "M": RL("mengchong_warship"), "P": "#planks", "T": RL("pine_tar")}, RL("turreted_ship"))},
      passive=True, novel=True),
    # ------------------------------------------------------------ structure manuals: each comes with its building plan
    T("architects_drafting", "ARCHITECT", J, "Lu Ban's Square", "The architect's drafting square: draw what you have built into a plan, and raise it again anywhere.", {
        "drafting_square": shaped(["SI", "S "], {"S": "stick", "I": "iron_ingot"}, RL("drafting_square"))}),
    S("road_inn_plan", "ARCHITECT", I, "Road Inn Construction Drawings", "An inn for the roads between cities: a hall with tables, a counter and beds.", "road_inn"),
    S("relay_station_plan", "ARCHITECT", I, "Relay Station Drawings", "A courier relay: stalls, hay and water for the post horses.", "relay_station"),
    S("village_temple_plan", "ARCHITECT", I, "Village Temple Construction Drawings", "A Taoist temple on a raised terrace, red pillars and a tiled roof, for a priest to preside over.", "taoist_shrine", novel=True),
    S("granary_plan", "ARCHITECT", I, "Granary Drawings", "A raised granary that keeps the grain dry.", "granary"),
    S("courtyard_house_plan", "ARCHITECT", A, "Siheyuan Drawings", "A courtyard house: four wings around a garden, a red gate at the front.", "courtyard_house"),
    S("pagoda_plan", "ARCHITECT", M, "Pagoda Drawings", "A five-storey pagoda with eaves on every floor.", "pagoda"),
    S("village_well_plan", "MASON", J, "Village Well Drawings", "A stone well with a roof over it.", "village_well"),
    S("beacon_tower_plan", "MASON", I, "Beacon Tower Drawings", "A stone beacon tower with battlements and a signal fire.", "beacon_tower"),
    S("paifang_plan", "MASON", I, "Paifang Drawings", "A stone memorial archway with a golden tablet.", "paifang"),
    S("zhaozhou_bridge_plan", "MASON", A, "Zhaozhou Bridge Drawings", "Li Chun's stone arch bridge, seventeen blocks long: build it from one bank across the water.", "arch_bridge"),
    S("mail_post_plan", "CARPENTER", J, "Mail Post Drawings", "A roadside mail post.", "mail_post"),
    S("pavilion_plan", "CARPENTER", J, "Pavilion Drawings", "A roadside pavilion with red pillars and double eaves.", "pavilion"),
    S("arrow_tower_plan", "CARPENTER", I, "Arrow Tower Drawings", "A wooden arrow tower with slits and a railed platform.", "arrow_tower"),
    S("dock_plan", "SHIPBUILDER", J, "Dock Construction Drawings", "A wooden pier out into the water: build it from the shore.", "wharf", novel=True),
    S("ferry_plan", "SHIPBUILDER", J, "Ferry Construction Drawings", "Stone steps down to the water for a ferry.", "ferry_landing", novel=True),
    S("shipyard_plan", "SHIPBUILDER", I, "Shipyard Construction Drawings", "A slipway into the water, framed and craned, with the shipwright's benches.", "shipyard", novel=True),
    S("lighthouse_plan", "SHIPBUILDER", A, "Lighthouse Drawings", "A stone lighthouse for the harbour mouth.", "lighthouse"),
    S("irrigated_field_plan", "FARMER", J, "Irrigated Field Drawings", "A sown field with a water channel down the middle.", "irrigated_field"),
    S("herb_garden_plan", "PHYSICIAN", J, "Herb Garden Drawings", "Beds of medicinal flowers between paths.", "herb_garden"),
    S("stable_plan", "BREEDER", I, "Stable Construction Drawings", "A stable with stalls, hay and water troughs.", "stable"),
    S("animal_pen_plan", "VETERINARIAN", J, "Animal Pen Drawings", "A fenced pen with a gate, hay and water.", "animal_pen"),
    S("fishing_hut_plan", "FISHERMAN", J, "Fishing Hut Drawings", "A hut on stilts over the water.", "fishing_hut"),
    S("kitchen_plan", "COOK", J, "Kitchen Drawings", "An open kitchen: stove, smoker, water and a table.", "kitchen"),
    S("forge_plan", "BLACKSMITH", J, "Forge Drawings", "A smithy: blast furnace, anvil, grindstone and a chimney.", "forge"),
]

DESIGN_NAMES = {
    "road_inn": ("Road Inn", "A hall with tables, a counter and beds."), "relay_station": ("Relay Station", "Stalls, hay and water for post horses."),
    "taoist_shrine": ("Village Temple", "A Taoist temple on a terrace."), "granary": ("Granary", "A raised granary."),
    "courtyard_house": ("Siheyuan", "A courtyard house of four wings."), "pagoda": ("Pagoda", "Five storeys."),
    "village_well": ("Village Well", "A stone well with a roof."), "beacon_tower": ("Beacon Tower", "A signal fire on a stone tower."),
    "paifang": ("Paifang", "A stone memorial archway."), "arch_bridge": ("Zhaozhou Bridge", "A stone arch bridge; build it from one bank."),
    "mail_post": ("Mail Post", "A roadside mail post."), "pavilion": ("Pavilion", "A roadside pavilion."), "arrow_tower": ("Arrow Tower", "A wooden arrow tower."),
    "wharf": ("Dock", "A pier out into the water; build it from the shore."), "ferry_landing": ("Ferry Landing", "Steps down to the water."),
    "shipyard": ("Shipyard", "A slipway into the water."), "lighthouse": ("Lighthouse", "A light for the harbour mouth."),
    "irrigated_field": ("Irrigated Field", "A sown field with a channel."), "herb_garden": ("Herb Garden", "Beds of medicinal flowers."),
    "stable": ("Stable", "Stalls, hay and troughs."), "animal_pen": ("Animal Pen", "A fenced pen."), "fishing_hut": ("Fishing Hut", "A hut on stilts."),
    "kitchen": ("Kitchen", "An open kitchen."), "forge": ("Forge", "A smithy with a chimney."),
}

TRADE_NAMES = {"BLACKSMITH": "Blacksmith", "TAILOR": "Tailor", "PHYSICIAN": "Physician", "ARCHITECT": "Architect", "BREEDER": "Breeder",
               "FISHERMAN": "Fisherman", "FARMER": "Farmer", "VETERINARIAN": "Veterinarian", "WINEMAKER": "Winemaker", "COOK": "Cook",
               "CARPENTER": "Carpenter", "MASON": "Mason", "SHIPBUILDER": "Shipbuilder"}
TIER_NAMES = ["Junior", "Intermediate", "Advanced", "Master"]


# ---------------------------------------------------------------- martial arts (secret manuals)

def ART(id, name, grade, kinds, max_level, innate=(0, 0, 0, 0), stats=(0, 0, 0, 0), dmg=0.0, speed=0.0, special="", special_level=0,
        requires="", requires_level=0, desc="", special_desc=""):
    return dict(id=id, name=name, grade=grade, kinds=kinds, max=max_level, innate=list(innate), stats=list(stats), dmg=dmg, speed=speed,
                special=special, special_level=special_level, requires=requires, requires_level=requires_level, desc=desc, special_desc=special_desc)


# innate: body, comprehension, luck, charm. stats: command, force, intelligence, politics.
ARTS = [
    ART("basic_breathing", "Basic Breathing Method", "BRONZE", ["INTERNAL"], 5, innate=(1, 0, 0, 0),
        desc="Breathing exercises every martial artist begins with."),
    ART("grass_floating", "Grass Floating Flight", "BRONZE", ["MOVEMENT"], 4, speed=0.02, special="light_fall", special_level=1,
        desc="A first lightness skill: run lighter and fall softer.", special_desc="Light Body: falls hurt less."),
    ART("basic_fist", "Basic Fist Technique", "BRONZE", ["FIST"], 5, dmg=0.04, desc="Stances and straight punches."),
    ART("basic_sabre", "Basic Sabre Technique", "BRONZE", ["BLADE"], 5, dmg=0.04, desc="Cuts and guards with the sabre and sword."),
    ART("basic_spear", "Basic Spear Technique", "BRONZE", ["SPEAR"], 5, dmg=0.04, desc="Thrusts and sweeps with spear and polearm."),
    ART("basic_archery", "Basic Archery", "BRONZE", ["BOW"], 5, dmg=0.04, desc="Stance, draw and loose."),
    ART("five_animals_play", "Five Animals Play", "BLACK_IRON", ["INTERNAL"], 6, innate=(1, 0, 0, 0), stats=(0, 1, 0, 0),
        desc="Hua Tuo's exercises after the tiger, deer, bear, ape and crane."),
    ART("daoyin", "Taoist Daoyin Exercises", "BLACK_IRON", ["INTERNAL"], 6, innate=(0, 1, 0, 0), stats=(0, 0, 1, 0),
        desc="Guiding and pulling the Qi: a clearer mind."),
    ART("taizu_long_fist", "Taizu Long Fist", "BLACK_IRON", ["FIST"], 7, stats=(0, 1, 0, 0), dmg=0.05,
        desc="Emperor Taizu of Song's long-range fist."),
    ART("ladder_cloud_step", "Ladder Cloud Step", "SILVER", ["MOVEMENT"], 6, speed=0.025, special="light_fall", special_level=1,
        desc="Climbing the air as if by a ladder of cloud.", special_desc="Light Body: falls hurt far less."),
    ART("liuhe_spear", "Six Harmonies Spear", "SILVER", ["SPEAR"], 9, stats=(0, 1, 0, 0), dmg=0.05,
        desc="The Liuhe spear, the spear Bajiquan pairs with."),
    ART("yuenu_sword", "Yuenu Sword Technique", "SILVER", ["BLADE"], 9, stats=(0, 1, 0, 0), dmg=0.05, special="flurry", special_level=5,
        desc="The Maiden of Yue's sword, quick as a falling leaf.", special_desc="Flurry: one cut in seven strikes twice."),
    ART("yijin_jing", "Muscle-Tendon Changing Classic", "SILVER", ["INTERNAL"], 9, innate=(1, 0, 0, 0), stats=(0, 2, 0, 0),
        desc="The Yijin Jing: tendons like iron."),
    ART("hundred_pace_archery", "Hundred Pace Archery", "GOLD", ["BOW"], 9, stats=(0, 1, 0, 0), dmg=0.06, special="hundred_pace", special_level=5,
        desc="Yang Youji pierced a willow leaf at a hundred paces.", special_desc="Hundred Paces: every arrow flies true and strikes as a critical hit."),
    ART("bajiquan", "Bajiquan", "KING", ["FIST", "SPEAR"], 9, stats=(0, 2, 0, 0), dmg=0.06, special="cling_to_mountain", special_level=5,
        desc="Eight Extremities Fist: hits like thunder, kicks like an earthquake. A great fit with the spear.",
        special_desc="Cling to the Mountain: one blow in seven lands half again as hard and hurls the foe back."),
    ART("war_soldier_code", "War Soldier Code", "KING", ["INTERNAL"], 9, innate=(1, 1, 0, 0), stats=(0, 2, 0, 0), special="fighting_intent", special_level=9,
        desc="A rare cultivation method made for battlefield soldiers.",
        special_desc="Fighting Intent: when badly hurt, killing intent rises: Strength II and Resistance for a minute (once every five)."),
    ART("guicang_sword", "Guicang Sword Technique", "EMPEROR", ["BLADE"], 12, stats=(0, 1, 1, 0), dmg=0.06, special="guicang_parry", special_level=6,
        desc="Gong Yangyu's sword art that gathers all sword arts.", special_desc="Return to the Hidden: one blow in seven against you is turned aside."),
    ART("killing_sword", "Killing Sword Sword Manual", "EMPEROR", ["BLADE"], 7, stats=(0, 3, 0, 0), dmg=0.10, special="seven_kills", special_level=3,
        desc="The Qin army's sword art, refined on the battlefield: seven kills.", special_desc="Seven Kills: each kill feeds your strength for ten seconds."),
    ART("yang_family_spear", "Yang Family Spear Technique", "EMPEROR", ["SPEAR"], 12, stats=(0, 2, 0, 0), dmg=0.06, special="throat_killing", special_level=6,
        desc="The Yang family's Lihua spear: attacks like a needle, returns like thread.",
        special_desc="Throat Killing Spear: one thrust in eight strikes double and leaves the foe weak and slow."),
    ART("yellow_emperor_internal", "Yellow Emperor Internal Cultivation Technique", "EMPEROR", ["INTERNAL"], 12, innate=(2, 1, 0, 0), stats=(1, 1, 1, 1),
        special="qi_shield", special_level=6, desc="One of the strongest internal arts: completed, one's Qi turns golden.",
        special_desc="Golden Qi Shield: when struck, a shield of Qi absorbs the next blows (every half minute)."),
    ART("yellow_court_scripture", "Yellow Court Internal Scripture: Qi Cultivation", "GOD", ["INTERNAL"], 9, innate=(4, 6, 4, 2),
        special="golden_qi", special_level=3, requires="yellow_emperor_internal", requires_level=12,
        desc="The Qi-cultivation scroll of the Yellow Court Scripture. Needs the Yellow Emperor technique trained to the full.",
        special_desc="Nourishing Qi: wounds close by themselves."),
    ART("son_of_heaven_fist", "Son of Heaven Dragon Fist", "GOD", ["FIST"], 9, stats=(1, 2, 0, 0), dmg=0.08, special="dragon_fist", special_level=5,
        desc="The dragon's fist of one who founds a country.", special_desc="Dragon Strike: one punch in ten bursts outward, striking every foe around."),
    ART("wugou_heart_sutra", "Wugou Heart Sutra", "SAINT", ["INTERNAL"], 18, innate=(4, 2, 2, 2), stats=(2, 2, 2, 2),
        desc="Unsullied. The heart pure and calm, no impure thoughts. To be cultivated from youth."),
]

GRADES = {"BRONZE": "Bronze", "BLACK_IRON": "Black Iron", "SILVER": "Silver", "GOLD": "Gold", "KING": "King", "EMPEROR": "Emperor", "GOD": "God", "SAINT": "Saint"}


# ---------------------------------------------------------------- new items: names and pictures

def px_img():
    return blank()


def bottle(liquid, cork="8a6a40", label=None):
    img, px = blank()
    glass = hexc("d8e8f0", 160)
    for y in range(5, 15):
        for x in range(4, 12):
            if (x - 7.5) ** 2 / 16 + (y - 10.5) ** 2 / 20 < 1.0:
                px[x, y] = shade(hexc(liquid), 1.15 - 0.05 * (y - 5)) if y > 7 else glass
    for y in range(2, 6):
        px[7, y] = px[8, y] = glass
    px[7, 1] = px[8, 1] = hexc(cork)
    px[7, 2] = px[8, 2] = hexc(cork)
    if label:
        for x in range(6, 10): px[x, 10] = px[x, 11] = hexc(label)
    px[6, 9] = hexc("ffffff", 200)
    return img


def pill(col, glow=None):
    return gen_arms.pill_img(col, glow)


def bowl(soup, top=None):
    img, px = blank()
    for x in range(2, 14):
        for y in range(8, 13):
            if (x - 7.5) ** 2 / 36 + (y - 8) ** 2 / 25 < 1.0:
                px[x, y] = shade(hexc("e8e0d0"), 1.0 - 0.04 * (y - 8))
    for x in range(3, 13): px[x, 8] = hexc(soup)
    for x in range(4, 12): px[x, 7] = shade(hexc(soup), 1.1)
    if top:
        for (x, y) in [(5, 6), (6, 6), (9, 6), (10, 7), (7, 5)]: px[x, y] = hexc(top)
    for x in range(5, 11): px[x, 13] = hexc("8a6a40")
    return img


def round_cake(col, rim, mark):
    img, px = blank()
    for y in range(3, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 8.5)
            if d < 5.5: px[x, y] = shade(hexc(col), 1.15 - d / 16) if d < 4.2 else hexc(rim)
    for (x, y) in [(6, 7), (9, 7), (7, 9), (8, 9), (7, 8), (8, 8), (6, 10), (9, 10)]: px[x, y] = hexc(mark)
    return img


def dumplings():
    img, px = blank()
    for cx, cy in [(5, 6), (11, 7), (8, 11)]:
        for y in range(cy - 3, cy + 3):
            for x in range(cx - 4, cx + 4):
                if 0 <= x < 16 and 0 <= y < 16 and (x - cx) ** 2 / 14 + (y - cy) ** 2 / 6 < 1 and y >= cy - 2:
                    px[x, y] = shade(hexc("f2ecdc"), 1.0 - 0.05 * (y - cy + 2))
        for x in range(cx - 2, cx + 3, 2):
            if 0 <= x < 16: px[x, cy - 2] = hexc("c8bca0")
    return img


def roast_duck():
    img, px = blank()
    for y in range(4, 13):
        for x in range(2, 14):
            if (x - 7) ** 2 / 25 + (y - 9) ** 2 / 12 < 1: px[x, y] = shade(hexc("a8501e"), 1.2 - 0.05 * (y - 4))
    for (x, y) in [(12, 6), (13, 5), (13, 4), (12, 4)]: px[x, y] = hexc("8a3a14")
    for (x, y) in [(5, 7), (6, 6), (8, 7)]: px[x, y] = hexc("e8a050")
    for x in range(1, 15): px[x, 13] = hexc("e8e0d0")
    return img


def fish(col):
    img, px = blank()
    for x in range(2, 13):
        h = int(3 * math.sin((x - 2) / 10 * math.pi)) + 1
        for y in range(8 - h, 8 + h):
            px[x, y] = shade(hexc(col), 1.1 - 0.06 * abs(y - 8))
    for (x, y) in [(13, 5), (13, 6), (14, 4), (13, 10), (13, 9), (14, 11), (13, 7), (13, 8)]: px[x, y] = shade(hexc(col), 0.8)
    px[4, 7] = hexc("1a1a1a")
    for x in range(6, 11, 2): px[x, 8] = hexc("e8e0d0")
    return img


def grain_pills():
    img, px = blank()
    for cx, cy in [(5, 7), (10, 6), (7, 11), (11, 11)]:
        for y in range(cy - 2, cy + 2):
            for x in range(cx - 2, cx + 2):
                if math.hypot(x - cx + 0.5, y - cy + 0.5) < 2: px[x, y] = shade(hexc("c89a50"), 1.2 - 0.1 * (y - cy + 2))
    return img


def feast():
    img, px = blank()
    for y in range(6, 14):
        for x in range(1, 15):
            if (x - 7.5) ** 2 / 49 + (y - 10) ** 2 / 12 < 1: px[x, y] = hexc("c8a24a") if (x - 7.5) ** 2 / 49 + (y - 10) ** 2 / 12 > 0.7 else hexc("b02a26")
    for (x, y, c) in [(4, 8, "a8501e"), (5, 8, "a8501e"), (8, 7, "f2ecdc"), (9, 7, "f2ecdc"), (11, 9, "6a8a3a"), (6, 10, "e8c060"), (10, 11, "8a3a14")]:
        px[x, y] = hexc(c)
    return img


def pouch(col, tie="8a6a40", spots=None):
    img, px = blank()
    for y in range(5, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 25 < 1: px[x, y] = shade(hexc(col), 1.1 - 0.03 * y)
    for x in range(6, 10): px[x, 4] = hexc(tie)
    px[7, 3] = px[8, 3] = shade(hexc(col), 0.9)
    if spots:
        for (x, y) in [(6, 9), (9, 11), (7, 12), (10, 8)]: px[x, y] = hexc(spots)
    return img


def cocoon():
    img, px = blank()
    for cx, cy in [(6, 8), (10, 10)]:
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 / 9 + (y - cy) ** 2 / 5 < 1: px[x, y] = shade(hexc("f4f0e0"), 1.05 - 0.04 * abs(y - cy))
    return img


def silk_bolt():
    img, px = blank()
    cols = ["d03a30", "e8a030", "e8d040", "40a050", "3060c0", "8040a0"]
    for y in range(3, 13):
        for x in range(2, 14):
            px[x, y] = shade(hexc(cols[(x - 2) // 2]), 1.1 - 0.03 * (y - 3))
    for x in range(2, 14): px[x, 13] = shade(hexc(cols[(x - 2) // 2]), 0.7)
    return img


def tea_leaves():
    img, px = blank()
    for cx, cy, a in [(6, 6, 0.6), (10, 9, -0.5), (7, 11, 0.2)]:
        for t in range(-4, 5):
            for w in range(-1, 2):
                x = int(cx + t * math.cos(a) - w * math.sin(a)); y = int(cy + t * math.sin(a) + w * math.cos(a))
                if 0 <= x < 16 and 0 <= y < 16 and abs(w) <= 1 - abs(t) // 4: px[x, y] = shade(hexc("4a8a3a"), 1.15 if w == 0 else 0.9)
    return img


def jar(col, lid="8a6a40"):
    img, px = blank()
    for y in range(5, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 30 < 1: px[x, y] = shade(hexc(col), 1.15 - 0.04 * (y - 5))
    for x in range(5, 11): px[x, 4] = hexc(lid); px[x, 3] = shade(hexc(lid), 1.2)
    px[5, 8] = hexc("ffffff", 120)
    return img


def vase():
    img, px = blank()
    for y in range(1, 15):
        w = 1.5 if y < 4 else 2 + 3.5 * math.sin((y - 3) / 11 * math.pi)
        for x in range(16):
            if abs(x - 7.5) < w: px[x, y] = hexc("f2f4f8") if not ((x + y) % 4 == 0 and 5 < y < 12 or y in (4, 13)) else hexc("2a4aa0")
    return img


def jade():
    img, px = blank()
    for y in range(4, 15):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 9.5)
            if 1.4 < d < 4.8: px[x, y] = shade(hexc("4ab07a"), 1.2 - d / 12)
    for y in range(0, 5): px[7, y] = hexc("c42a24")
    return img


def needles():
    img, px = blank()
    for y in range(4, 14):
        for x in range(3, 13): px[x, y] = hexc("7a3a2a") if y in (4, 13) or x in (3, 12) else hexc("a8503a")
    for x in range(5, 12, 2):
        for y in range(6, 12): px[x, y] = hexc("d8dce4") if y > 6 else hexc("c8a24a")
    return img


def luopan():
    img, px = blank()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 7: px[x, y] = hexc("c8a24a") if d > 5.6 else hexc("b02a26") if d > 4.4 else hexc("e8d8a0") if d > 2 else hexc("2a2a2a")
    for (x, y) in [(7, 3), (8, 4), (7, 12), (3, 7), (12, 8)]: px[x, y] = hexc("1a1a1a")
    return img


def net():
    img, px = blank()
    for y in range(2, 15):
        for x in range(2, 15):
            if (x + y) % 3 == 0 or (x - y) % 3 == 0:
                if (x - 8) ** 2 + (y - 8) ** 2 < 42: px[x, y] = hexc("c8b890")
    for (x, y) in [(3, 8), (13, 8), (8, 3), (8, 13)]: px[x, y] = hexc("6a6a70")
    return img


def talisman():
    img, px = blank()
    for y in range(1, 15):
        for x in range(4, 12): px[x, y] = shade(hexc("e8c840"), 1.05 - 0.02 * y)
    for y in range(3, 13, 1): px[7, y] = hexc("c42a24")
    for (x, y) in [(6, 4), (9, 4), (5, 7), (10, 7), (6, 10), (9, 10), (8, 5), (8, 9)]: px[x, y] = hexc("c42a24")
    return img


def chariot():
    img, px = blank()
    for x in range(2, 14):
        for y in range(8, 11): px[x, y] = shade(hexc("8a5a30"), 1.1 - 0.05 * (y - 8))
    for cx in (4, 11):
        for y in range(10, 15):
            for x in range(cx - 2, cx + 3):
                if 1.2 < math.hypot(x - cx, y - 12) < 2.6: px[x, y] = hexc("4a3020")
    for y in range(2, 8): px[8, y] = hexc("c8a24a")
    for x in range(8, 12): px[x, 3] = hexc("b02a26")
    return img


def wooden_ox():
    img, px = blank()
    for y in range(5, 11):
        for x in range(3, 13): px[x, y] = shade(hexc("9a6a3a"), 1.1 - 0.05 * (y - 5)) if (x + y) % 5 else hexc("6a4424")
    for (x, y) in [(13, 4), (14, 4), (14, 5), (13, 6), (14, 6)]: px[x, y] = hexc("9a6a3a")
    for lx in (4, 6, 10, 12):
        for y in range(11, 15): px[lx, y] = hexc("6a4424")
    return img


def doll():
    img, px = blank()
    for y in range(2, 6):
        for x in range(6, 10): px[x, y] = hexc("c8965a")
    for y in range(6, 13):
        for x in range(5, 11): px[x, y] = hexc("b02a26") if y < 9 else hexc("c8965a")
    for y in range(13, 15): px[6, y] = px[9, y] = hexc("8a6a40")
    px[7, 3] = px[8, 3] = hexc("2a1a10")
    return img


def lifebuoy():
    img, px = blank()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 3.0 < d < 6.8: px[x, y] = hexc("e8e8e8") if int(math.degrees(math.atan2(y - 7.5, x - 7.5)) // 45) % 2 else hexc("d03a30")
    return img


def diving_bell():
    img, px = blank()
    for y in range(3, 15):
        w = 2 + (y - 3) * 0.45
        for x in range(16):
            if abs(x - 7.5) < w: px[x, y] = hexc("6a6e78") if y in (3, 4, 14) or abs(x - 7.5) > w - 1 else hexc("a8d8e8", 200)
    px[7, 2] = px[8, 2] = hexc("6a6e78")
    return img


def whistle():
    img, px = blank()
    for i in range(10):
        x, y = 3 + i, 11 - int(4 * math.sin(i / 9 * math.pi))
        px[x, y] = hexc("efe6cc"); px[x, y + 1] = hexc("d8ccaa")
    px[6, 9] = px[9, 8] = hexc("3a2a1a")
    for y in range(12, 15): px[3, y] = hexc("c8a24a")
    return img


def charm():
    img, px = blank()
    for y in range(2, 11):
        for x in range(4, 12):
            if abs(x - 7.5) + abs(y - 6) < 4.5: px[x, y] = hexc("c42a24") if (x + y) % 3 else hexc("8a1a18")
    for y in range(11, 15): px[6, y] = px[8, y] = px[10, y] = hexc("c42a24")
    px[7, 6] = px[8, 6] = hexc("e8c840")
    return img


def salve():
    img, px = blank()
    for y in range(7, 13):
        for x in range(3, 13): px[x, y] = hexc("9aa0a8") if y in (7, 12) else hexc("b8bec6")
    for x in range(4, 12): px[x, 6] = hexc("6aa060")
    return img


def book(cover, band, mark):
    img, px = blank()
    for y in range(2, 14):
        for x in range(3, 13): px[x, y] = shade(hexc(cover), 1.05 - 0.02 * y)
    for y in range(2, 14): px[3, y] = hexc("e8dcb8"); px[12, y] = shade(hexc(cover), 0.6)
    for y in (4, 7, 10, 12): px[4, y] = hexc("c8c0a0")
    for y in range(3, 12): px[9, y] = px[10, y] = hexc(band)
    for (x, y) in mark: px[x, y] = hexc("2a1a10")
    return img


def tang_hengdao():
    return gen_arms.blade(gen_arms.STEEL, 10, 1, hexc("3a2a1a"), guard=hexc("6a6a6a"), curve=0.0)


def mo_dao():
    img, px = blank()
    for i in range(0, 6): px[1 + i, 14 - i] = gen_arms.WOOD
    for i in range(6, 14):
        for w in range(2):
            x, y = 1 + i + w, 14 - i
            if 0 <= x < 16 and 0 <= y < 16: px[x, y] = shade(gen_arms.STEEL, 1.2 if w == 0 else 0.9)
    px[7, 9] = px[6, 8] = px[8, 10] = hexc("6a6a6a")
    return img


def composite_bow(pull=0):
    img = gen_arms.bow_img(pull)
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if c[3] and c[:3] != (0xd0, 0xd0, 0xd0):
                px[x, y] = hexc("2a1a10") if c[:3] == (0x3a, 0x2a, 0x1a) else shade(hexc("8a5a30"), 1.1 if (x + y) % 3 else 0.8)
    return img


def plan_img():
    img, px = blank()
    for y in range(2, 14):
        for x in range(2, 14): px[x, y] = shade(hexc("d8e4f0"), 1.0 - 0.01 * y)
    for x in range(2, 14): px[x, 2] = px[x, 13] = hexc("2a4a8a")
    for y in range(2, 14): px[2, y] = px[13, y] = hexc("2a4a8a")
    for (x, y) in [(4, 10), (5, 9), (6, 8), (7, 7), (8, 8), (9, 9), (10, 10), (5, 10), (10, 11), (5, 11), (5, 12), (10, 12)]: px[x, y] = hexc("2a4a8a")
    for x in range(4, 12): px[x, 6] = hexc("c42a24")
    return img


def square_img():
    img, px = blank()
    for i in range(2, 14):
        px[3, i] = px[4, i] = hexc("8a5a30")
        px[i, 12] = px[i, 13] = hexc("8a5a30")
    for i in range(4, 14, 2): px[4, i] = px[i, 12] = hexc("e8d8a0")
    px[3, 13] = px[4, 13] = hexc("c8a24a")
    return img


def ship_overlay(kind):
    img, px = blank()
    col = {"fishing_boat": None, "cima_boat": "c42a24", "merchant_ship": "f0f0f0", "mengchong_warship": "8a5a30", "turreted_ship": "c42a24"}[kind]
    if kind == "fishing_boat":
        for y in range(3, 8): px[8, y] = hexc("6a4424")
        for (x, y) in [(9, 3), (10, 4), (9, 4), (9, 5)]: px[x, y] = hexc("e8dcc0")
        return img
    for y in range(1, 9): px[7, y] = hexc("4a3020")
    if kind == "turreted_ship":
        for y in range(3, 9):
            for x in range(4, 11): px[x, y] = hexc("6a1a14") if y % 2 else hexc(col)
        for x in range(3, 12): px[x, 3] = hexc("2a2a30")
        return img
    if kind == "mengchong_warship":
        for y in range(5, 9):
            for x in range(3, 12): px[x, y] = shade(hexc(col), 1.1 - 0.05 * y)
        for x in range(3, 12, 2): px[x, 5] = hexc("8c8e96")
        return img
    for y in range(2, 8):
        for x in range(8, 13 - max(0, y - 5)): px[x, y] = shade(hexc(col), 1.0 - 0.03 * y)
    return img


ITEMS = {
    # id: (name, description, painter)
    "rainbow_silk": ("Rainbow Silk", "Top-quality dyed silk cloth. The market pays well.", silk_bolt),
    "silk_cocoon": ("Silk Cocoon", "Reel four into fine silk.", cocoon),
    "pine_tar": ("Pine Tar", "Caulking for boats; burns in torches.", lambda: jar("1a1410", "6a4424")),
    "tea_leaves": ("Tea Leaves", "Brew them into green tea.", tea_leaves),
    "soy_sauce": ("Soy Sauce", "The cook's seasoning.", lambda: bottle("2a1408", label="c42a24")),
    "porcelain_vase": ("Blue-and-White Vase", "Jingdezhen porcelain. Sells for 25 silver.", vase),
    "jade_pendant": ("Jade Pendant", "In the off hand: +2 Luck and +2 health.", jade),
    "detox_draught": ("Detox Draught", "Cures poison.", lambda: bottle("6aa060")),
    "clarity_pill": ("Clear Heart Pill", "Clears nausea, blindness, slowness, weakness, fatigue and hunger.", lambda: pill("6ab0c0", "c8f0ff")),
    "acupuncture_needles": ("Acupuncture Needles", "Use on someone (or on yourself) to heal 3 hearts and clear harmful effects. A physician heals more.", needles),
    "rhino_horn_pill": ("Rhino Horn Poison Curing Pill", "Cures poison and wither, and wards them off for two minutes.", lambda: pill("5a4a3a", "a89070")),
    "peiyuan_pill": ("Peiyuan Pill", "Heals fully, with Resistance II and regeneration.", lambda: pill("e8c040", "fff0a0")),
    "fodder": ("Fodder", "Use on a farm animal: ready to breed, or a young one grows.", lambda: pouch("c8a860", spots="6a8a3a")),
    "horse_feed": ("Horse Feed", "Use on a horse: heals it, quickens it and makes it trust you.", lambda: pouch("a8783a", spots="d03a30")),
    "bamboo_feed": ("Bamboo Feed", "Use on a panda: ready to breed, or a cub grows.", lambda: pouch("6aa04a", spots="e8e0a0")),
    "animal_salve": ("Animal Salve", "Use on an animal: heals it fully and clears harmful effects.", salve),
    "horse_tonic": ("Horse Tonic", "Use on a horse: Speed II, Jump Boost II and regeneration for five minutes.", lambda: bottle("c87a30", label="e8d8a0")),
    "beast_whistle": ("Beast Whistle", "Calls all your tamed animals within 128 blocks to you.", whistle),
    "guardian_charm": ("Guardian Charm", "Carried: saves one of your animals from death, once.", charm),
    "luopan": ("Luopan", "The geomancer's compass: right-click to read the land here.", luopan),
    "fishing_net": ("Fishing Net", "Right-click on water to cast for fish.", net),
    "spring_talisman": ("Spring Weathering Talisman", "Use on the ground: the crops around grow at once.", talisman),
    "south_pointing_chariot": ("South-Pointing Chariot", "Right-click: points the way to your village.", chariot),
    "wooden_ox": ("Wooden Ox", "Right-click to set loose a saddled pack mule that is yours.", wooden_ox),
    "substitute_doll": ("Substitute Doll", "Carried: dies in your place, once.", doll),
    "lifebuoy": ("Lifebuoy", "In the off hand in water: breathe, and swim like a dolphin.", lifebuoy),
    "diving_bell": ("Diving Bell", "Right-click: three minutes of water breathing and sight under water.", diving_bell),
    "dried_fish": ("Salted Dried Fish", "Keeps and fills.", lambda: fish("b89060")),
    "dumplings": ("Dumplings", "Quick to eat.", dumplings),
    "noodle_soup": ("Beef Noodle Soup", "Filling, and warms you: Speed for a while.", lambda: bowl("c87a40", "6aa04a")),
    "green_tea": ("Green Tea", "Haste for three minutes; settles the stomach.", lambda: bottle("8ab040")),
    "mooncake": ("Mooncake", "A mid-autumn sweet: a little absorption.", lambda: round_cake("c8883a", "a8682a", "8a4a1a")),
    "military_grain_pill": ("Military Grain Pill", "One small pill sustains a soldier for a day.", grain_pills),
    "roast_duck": ("Roast Duck", "Rich and restoring.", roast_duck),
    "imperial_feast": ("Manchu Han Imperial Feast", "Fills you entirely, with Strength, Resistance, Haste and regeneration.", feast),
    "dukang_wine": ("Dukang Wine", "Strength II for two minutes.", lambda: bottle("e8c070", label="c42a24")),
    "maotai": ("Maotai", "Strength, Speed and regeneration for three minutes.", lambda: bottle("f0f0e8", "c42a24", label="c42a24")),
    "medicinal_wine": ("Medicinal Wine", "Regeneration and a shield of health.", lambda: bottle("8a3a2a", label="6aa060")),
    "immortal_wine": ("Drunken Immortal Brew", "Luck II and Strength for ten minutes.", lambda: bottle("d8b0f0", "c8a24a", label="c8a24a")),
    "tang_hengdao": ("Tang Hengdao", "", tang_hengdao),
    "mo_dao": ("Mo Dao", "", mo_dao),
    "composite_bow": ("Composite Bow", "", lambda: composite_bow(0)),
    "building_plan": ("Building Plan", "", lambda: plan_img()),
    "drafting_square": ("Lu Ban's Square", "", lambda: square_img()),
    "tech_manual": ("Technical Manual", "", lambda: book("6a4424", "e8dcb8", [(9, 5), (10, 7), (9, 9)])),
    "secret_manual": ("Secret Manual", "", lambda: book("2e4a7a", "c8a24a", [(9, 5), (10, 6), (9, 8), (10, 9)])),
}

SHIPS = {
    "fishing_boat": ("Fishing Boat", "Two fishermen; fish caught from it come in a third more often."),
    "cima_boat": ("Cima Boat", "The red-sailed scout: the fastest thing on the water."),
    "merchant_ship": ("Merchant Ship", "Three seats and a hold (crouch and right-click it to open)."),
    "mengchong_warship": ("Mengchong Warship", "Covered in rawhide: four seats, very tough, and it rams what it runs down."),
    "turreted_ship": ("Turreted Ship", "A floating fortress: six seats, crossbowmen and clapper poles."),
}

# New armour: kind -> (piece ids for helmet, chest, legs, boots, display names)
NEW_ARMOUR = {
    "mingguang": (["mingguang_helmet", "mingguang_armour", "mingguang_greaves", "mingguang_boots"],
                  ["Mingguang Helm", "Mingguang Armour", "Mingguang Greaves", "Mingguang Boots"]),
    "buren": (["buren_helmet", "buren_armour", "buren_greaves", "buren_boots"],
              ["Buren Helm", "Buren Armour", "Buren Greaves", "Buren Boots"]),
    "brigandine": (["brigandine_helmet", "brigandine_coat", "brigandine_trousers", "brigandine_boots"],
                   ["Brigandine Cap", "Brigandine Coat", "Brigandine Trousers", "Brigandine Boots"]),
    "night_clothes": (["night_hood", "night_suit", None, None], ["Night Hood", "Night Suit", None, None]),
    "dragon_robe": ([None, "dragon_robe", None, None], [None, "Dragon Robe", None, None]),
}


def style_mingguang(face, x, y, w, h, rnd):
    steel = hexc("d4dce6")
    if face == "front" and w >= 8 and gen_arms.PART[0] == "body" and 2 <= y <= 6:
        for cx in (w / 2 - 2.5, w / 2 + 1.5):
            d = math.hypot(x - cx, y - 4)
            if d < 1.0: return hexc("ffffff")
            if d < 2.0: return hexc("e8eef4")
            if d < 2.6: return gen_arms.GOLD_TRIM
    if gen_arms.PART[0] == "body" and y in (h - 4, h - 3): return hexc("b02a26")
    if y == 0 or y == h - 1: return gen_arms.GOLD_TRIM
    band = y % 3
    if band == 2: return shade(steel, 0.62)
    return shade(steel, (1.2 if band == 0 else 1.0) + rnd.uniform(-0.04, 0.04))


def style_buren(face, x, y, w, h, rnd):
    iron = hexc("3e4148")
    if y == 0: return hexc("5a3a22")
    row = y // 2
    if (x + row) % 3 == 0: return shade(iron, 0.55)
    if y % 2 == 0: return shade(iron, 1.35 + rnd.uniform(-0.05, 0.05))
    if (x * 3 + row) % 7 == 0: return hexc("8a6a40")
    return shade(iron, 1.0 + rnd.uniform(-0.05, 0.05))


def style_brigandine(face, x, y, w, h, rnd):
    cloth = hexc("9a2a24")
    if y == 0 or y == h - 1: return hexc("4a1a14")
    if y % 3 == 1 and x % 2 == 0: return hexc("e8c860")   # brass studs
    return shade(cloth, 1.0 + rnd.uniform(-0.06, 0.06) - (0.08 if x % 4 == 3 else 0))


def style_night(face, x, y, w, h, rnd):
    c = shade(hexc("1c1c22"), 1.0 + rnd.uniform(-0.1, 0.1))
    if (x * 2 + y) % 7 == 0: c = hexc("2c2c34")
    if y == h - 1: c = hexc("101014")
    return c


def style_dragon(face, x, y, w, h, rnd):
    gold = hexc("e8c030")
    if y >= h - 2: return hexc("2a5ab0") if (x + y) % 2 else hexc("e8e8f0")      # sea waves at the hem
    if y == 0: return hexc("b02a26")
    t = math.sin(x * 0.9 + y * 0.5) + math.sin(y * 0.7 - x * 0.3)
    if t > 1.3: return hexc("c42a24")           # the dragon's coils
    if t > 1.0: return hexc("f8e8a0")
    return shade(gold, 1.0 + rnd.uniform(-0.05, 0.05))


gen_arms.STYLES.update({"mingguang": style_mingguang, "buren": style_buren, "brigandine": style_brigandine,
                        "night_clothes": style_night, "dragon_robe": style_dragon})

_old_head = gen_arms.head_details


def head_details(kind, px, rnd):
    _old_head(kind, px, rnd)
    if kind == "mingguang":
        for (x, y) in [(11, 2), (12, 2), (11, 3), (12, 3), (11, 1), (12, 1)]: px[x, y] = hexc("c42a24")    # red plume
        for x in range(0, 32): px[x, 8] = gen_arms.GOLD_TRIM
    if kind == "buren":
        for (x, y) in [(11, 3), (12, 3)]: px[x, y] = hexc("8c8e96")
    if kind == "night_clothes":
        for x in range(9, 15): px[x, 11] = hexc("2a2a30")       # the eye slit stays open (see armour_layer)


gen_arms.head_details = head_details


# ---------------------------------------------------------------- writers

def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def textures_and_models():
    item = os.path.join(ASSETS, "textures", "item")
    models = os.path.join(ASSETS, "models", "item")
    for n, (_, _, fn) in ITEMS.items():
        fn().save(os.path.join(item, n + ".png"))
        parent = "minecraft:item/handheld" if n in ("tang_hengdao", "mo_dao") else "minecraft:item/generated"
        if n != "composite_bow": write(os.path.join(models, n + ".json"), {"parent": parent, "textures": {"layer0": f"realmlord:item/{n}"}})
    for i, pull in enumerate([1, 2, 3]):
        composite_bow(pull).save(os.path.join(item, f"composite_bow_pulling_{i}.png"))
        write(os.path.join(models, f"composite_bow_pulling_{i}.json"),
              {"parent": "realmlord:item/composite_bow", "textures": {"layer0": f"realmlord:item/composite_bow_pulling_{i}"}})
    write(os.path.join(models, "composite_bow.json"), {
        "parent": "minecraft:item/bow", "textures": {"layer0": "realmlord:item/composite_bow"},
        "overrides": [{"predicate": {"pulling": 1}, "model": "realmlord:item/composite_bow_pulling_0"},
                      {"predicate": {"pulling": 1, "pull": 0.65}, "model": "realmlord:item/composite_bow_pulling_1"},
                      {"predicate": {"pulling": 1, "pull": 0.9}, "model": "realmlord:item/composite_bow_pulling_2"}]})
    vanilla_boat = {"fishing_boat": "birch_boat", "cima_boat": "mangrove_boat", "merchant_ship": "spruce_chest_boat",
                    "mengchong_warship": "acacia_boat", "turreted_ship": "dark_oak_boat"}
    for k, v in vanilla_boat.items():
        ship_overlay(k).save(os.path.join(item, k + "_overlay.png"))
        write(os.path.join(models, k + ".json"), {"parent": "minecraft:item/generated",
                                                  "textures": {"layer0": f"minecraft:item/{v}", "layer1": f"realmlord:item/{k}_overlay"}})
    armour = os.path.join(ASSETS, "textures", "models", "armor")
    for k, (ids, _) in NEW_ARMOUR.items():
        for n, slot in zip(ids, ["helmet", "chest", "legs", "boots"]):
            if n:
                gen_arms.armour_icon(k, slot).save(os.path.join(item, n + ".png"))
                write(os.path.join(models, n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{n}"}})
        seed = sum(map(ord, k))
        gen_arms.armour_layer(k, 1, seed).save(os.path.join(armour, k + "_layer_1.png"))
        gen_arms.armour_layer(k, 2, seed + 1).save(os.path.join(armour, k + "_layer_2.png"))


def recipes():
    seen = set()
    for t in TECHS:
        for name, r in t["recipes"].items():
            assert name not in seen, name
            seen.add(name)
            write(os.path.join(DATA, "recipes", "manual", name + ".json"), {"type": "realmlord:manual", "manual": t["id"], "recipe": r})


def outputs(t):
    out = []
    for r in t["recipes"].values():
        it = r["result"]["item"]
        if it not in out: out.append(it)
    return out


def java():
    lines = []
    for t in TECHS:
        outs = ", ".join(f'"{o}"' for o in outputs(t))
        sts = ", ".join(f'"{o}"' for o in t["structures"])
        lines.append(f'            new Tech("{t["id"]}", WorkOccupation.{t["trade"]}, {t["tier"]}, {PRICE[t["tier"]]}L, new String[]{{{outs}}}, {str(t["passive"]).lower()}, {str(t["novel"]).lower()}, new String[]{{{sts}}}),')
    src = f'''package com.realmlord.work;

import com.realmlord.profile.WorkOccupation;

import java.util.List;

/** Generated by tools/gen_crafts.py: every technical manual, its trade, the rank needed, its price and what it teaches. */
public final class TechData {{
    private TechData() {{}}

    /** @param tier the trade rank needed to learn it (0 Junior .. 3 Master); outputs: item ids it teaches to make. */
    public record Tech(String id, WorkOccupation trade, int tier, long price, String[] outputs, boolean passive, boolean novel, String[] structures) {{
        public String key() {{
            return "realmlord.tech." + id;
        }}
    }}

    public static final List<Tech> ALL = List.of(
{chr(10).join(lines)[:-1]});
}}
'''
    with open(os.path.join(JAVA, "work", "TechData.java"), "w") as f:
        f.write(src)

    lines = []
    for a in ARTS:
        kinds = ", ".join("Kind." + k for k in a["kinds"])
        lines.append(f'            new Art("{a["id"]}", ArtGrade.{a["grade"]}, java.util.EnumSet.of({kinds}), {a["max"]}, '
                     f'new int[]{{{", ".join(map(str, a["innate"]))}}}, new int[]{{{", ".join(map(str, a["stats"]))}}}, {a["dmg"]}, {a["speed"]}, '
                     f'"{a["special"]}", {a["special_level"]}, "{a["requires"]}", {a["requires_level"]}),')
    src = f'''package com.realmlord.martial;

import java.util.List;

/** Generated by tools/gen_crafts.py: the martial arts secret manuals. */
public final class ArtData {{
    private ArtData() {{}}

    public enum Kind {{ INTERNAL, FIST, BLADE, SPEAR, BOW, MOVEMENT }}

    /**
     * innate: per level, Body, Comprehension, Luck, Charm; stats: per level, Command, Force,
     * Intelligence, Politics; dmg: damage per level with a matching weapon; speed: movement per level.
     */
    public record Art(String id, ArtGrade grade, java.util.EnumSet<Kind> kinds, int maxLevel, int[] innate, int[] stats, double dmg, double speed,
                      String special, int specialLevel, String requires, int requiresLevel) {{
        public String key() {{
            return "realmlord.art." + id;
        }}
    }}

    public static final List<Art> ALL = List.of(
{chr(10).join(lines)[:-1]});
}}
'''
    os.makedirs(os.path.join(JAVA, "martial"), exist_ok=True)
    with open(os.path.join(JAVA, "martial", "ArtData.java"), "w") as f:
        f.write(src)


def lang():
    L = {}
    for n, (name, desc, _) in ITEMS.items():
        L[f"item.realmlord.{n}"] = name
        if desc: L[f"item.realmlord.{n}.desc"] = desc
    for k, (ids, names) in NEW_ARMOUR.items():
        for n, nm in zip(ids, names):
            if n: L[f"item.realmlord.{n}"] = nm
    L["item.realmlord.zhuge_crossbow"] = "Zhuge Repeating Crossbow"
    for k, (n, desc) in DESIGN_NAMES.items():
        L[f"realmlord.design.{k}"] = n
        L[f"realmlord.design.{k}.desc"] = desc
    for k, (n, desc) in SHIPS.items():
        L[f"item.realmlord.{k}"] = n
        L[f"item.realmlord.{k}.desc"] = desc
    L["item.realmlord.building_plan"] = "Building Plan"
    L["item.realmlord.building_plan.named"] = "Building Plan: %s"
    L["item.realmlord.building_plan.drawn"] = "Architect's Plan (%s)"
    L["item.realmlord.drafting_square"] = "Lu Ban's Square"
    L["entity.realmlord.fishing_boat"] = "Fishing Boat"
    L["entity.realmlord.cima_boat"] = "Cima Boat"
    L["entity.realmlord.merchant_ship"] = "Merchant Ship"
    L["entity.realmlord.mengchong_warship"] = "Mengchong Warship"
    L["entity.realmlord.turreted_ship"] = "Turreted Ship"
    L["item.realmlord.tech_manual.named"] = "<%s>"
    L["item.realmlord.secret_manual.named"] = "<%s>"
    for t in TECHS:
        L[f"realmlord.tech.{t['id']}"] = t["name"]
        L[f"realmlord.tech.{t['id']}.desc"] = t["desc"]
    for k, v in TRADE_NAMES.items():
        L[f"realmlord.tech.trade.{k.lower()}"] = v
    for i, v in enumerate(TIER_NAMES):
        L[f"realmlord.tech.tier.{i}"] = v
    for a in ARTS:
        L[f"realmlord.art.{a['id']}"] = a["name"]
        L[f"realmlord.art.{a['id']}.desc"] = a["desc"]
        if a["special"]: L[f"realmlord.art.{a['id']}.special"] = a["special_desc"]
    for k, v in GRADES.items():
        L[f"realmlord.art.grade.{k.lower()}"] = v
    for k in ["INTERNAL", "FIST", "BLADE", "SPEAR", "BOW", "MOVEMENT"]:
        L[f"realmlord.art.kind.{k.lower()}"] = {"INTERNAL": "Cultivation method", "FIST": "Fist art", "BLADE": "Sword and sabre art",
                                                 "SPEAR": "Spear art", "BOW": "Archery", "MOVEMENT": "Lightness skill"}[k]
    L.update({
        "itemGroup.realmlord.manuals": "Realm Lord: Manuals",
        # Technical manuals
        "realmlord.tech.tooltip.trade": "%s · %s and above",
        "realmlord.tech.tooltip.anyone": "%s · anyone may learn it",
        "realmlord.tech.tooltip.teaches": "Teaches: %s",
        "realmlord.tech.tooltip.passive": "A technique: works for you once learned.",
        "realmlord.tech.tooltip.novel": "From the novel.",
        "realmlord.tech.tooltip.known": "You know this manual.",
        "realmlord.tech.tooltip.use": "Right-click to learn it (the manual is used up).",
        "realmlord.tech.learned": "You have learned <%s>.",
        "realmlord.tech.known": "You already know <%s>.",
        "realmlord.tech.need_trade": "Only a %s of %s rank or higher can learn this.",
        "realmlord.tech.recipe_unknown": "You don't know how to make that: it needs a technical manual.",
        # Secret manuals and arts
        "realmlord.art.tooltip.grade": "%s · %s",
        "realmlord.art.tooltip.levels": "%s levels",
        "realmlord.art.tooltip.per_level": "Each level: %s",
        "realmlord.art.tooltip.damage": "+%s%% damage with the art's weapons per level",
        "realmlord.art.tooltip.speed": "+%s%% speed per level",
        "realmlord.art.tooltip.special": "From level %s: %s",
        "realmlord.art.tooltip.requires": "Requires %s at level %s.",
        "realmlord.art.tooltip.comprehension": "Needs Comprehension %s.",
        "realmlord.art.tooltip.use": "Right-click to learn it (the manual is used up). Then train it: fight with its weapons, or meditate (crouch and keep still) for cultivation methods; run for lightness skills.",
        "realmlord.art.tooltip.known": "You have learned this art.",
        "realmlord.art.learned": "You begin to train <%s>.",
        "realmlord.art.known": "You have already learned <%s>.",
        "realmlord.art.need_comprehension": "Your Comprehension (%s) is too low for a %s-rank art: it needs %s.",
        "realmlord.art.need_art": "You must first train %s to level %s.",
        "realmlord.art.level_up": "<%s> rises to level %s.",
        "realmlord.art.mastered": "You have mastered <%s>!",
        "realmlord.art.special": "%s!",
        "realmlord.art.stat.body": "Body +%s", "realmlord.art.stat.comprehension": "Comprehension +%s", "realmlord.art.stat.luck": "Luck +%s",
        "realmlord.art.stat.charm": "Charm +%s", "realmlord.art.stat.command": "Command +%s", "realmlord.art.stat.force": "Force +%s",
        "realmlord.art.stat.intelligence": "Intelligence +%s", "realmlord.art.stat.politics": "Politics +%s",
        "realmlord.art.special.fighting_intent": "Fighting Intent", "realmlord.art.special.guicang_parry": "Return to the Hidden",
        "realmlord.art.special.throat_killing": "Throat Killing Spear", "realmlord.art.special.cling_to_mountain": "Cling to the Mountain",
        "realmlord.art.special.dragon_fist": "Dragon Strike", "realmlord.art.special.seven_kills": "Seven Kills",
        "realmlord.art.special.qi_shield": "Golden Qi Shield", "realmlord.art.special.flurry": "Flurry",
        # Character panel
        "realmlord.panel.tab.arts": "Arts & Manuals",
        "realmlord.panel.arts.title": "Martial arts",
        "realmlord.panel.arts.none": "No martial arts yet. Secret manuals are sold on the market's Manuals page, carried by bandit chiefs and beast kings, and found in ruins.",
        "realmlord.panel.arts.line": "Lv %s/%s",
        "realmlord.panel.arts.mastered": "Mastered",
        "realmlord.panel.arts.bonus": "From your arts: %s",
        "realmlord.panel.techs.title": "Technical manuals known: %s",
        "realmlord.panel.techs.none": "None yet. Technical manuals are sold on the market's Manuals page and carried by bandit chiefs.",
        "realmlord.panel.techs.trade": "%s: %s",
        # Market
        "realmlord.market.special.books_tab": "Manuals",
        # Item effects
        "realmlord.craft.antidote": "Poison can't take hold of you for two minutes.",
        "realmlord.craft.healed": "Healed %s.",
        "realmlord.craft.luopan": "%s · height %s · %s",
        "realmlord.craft.luopan.territory": "In %s's land, %s blocks from the manor",
        "realmlord.craft.luopan.wild": "Wilderness",
        "realmlord.craft.chariot": "Your manor lies %s blocks to the %s.",
        "realmlord.craft.chariot.none": "The figure points south, as ever. (You have no village to point to.)",
        "realmlord.craft.dir.0": "south", "realmlord.craft.dir.1": "south-west", "realmlord.craft.dir.2": "west", "realmlord.craft.dir.3": "north-west",
        "realmlord.craft.dir.4": "north", "realmlord.craft.dir.5": "north-east", "realmlord.craft.dir.6": "east", "realmlord.craft.dir.7": "south-east",
        "realmlord.craft.whistle": "%s animal(s) answer your whistle.",
        "realmlord.craft.charm": "The guardian charm saves %s.",
        "realmlord.craft.doll": "The substitute doll dies in your place.",
        "realmlord.craft.net_empty": "The net comes up empty.",
        "realmlord.craft.net_water": "Cast the net on water.",
        "realmlord.craft.ox_name": "Wooden Ox",
        "realmlord.craft.talisman": "Spring comes to the fields.",
        "realmlord.craft.tame": "%s takes to you at once.",
        # Trade crafting
        "realmlord.tech.bonus": "Your trade shows: one more.",
        "realmlord.tech.tooltip.builds": "Builds: %s (its plan comes with the manual)",
        "realmlord.tech.plan_given": "You receive the plan: %s.",
        # Building from plans
        "realmlord.build.preview": "%s: %s×%s×%s, %s blocks. The sparks mark where it will stand, its entrance facing you.",
        "realmlord.build.needs": "Needs: %s",
        "realmlord.build.lacking": "You lack: %s (those parts will be left out).",
        "realmlord.build.confirm": "Crouch and right-click the same spot to build.",
        "realmlord.build.started": "Building %s (%s blocks)...",
        "realmlord.build.done": "%s is built: %s blocks placed.",
        "realmlord.build.missing": "Left out for want of materials: %s",
        "realmlord.build.more": " and %s more",
        "realmlord.build.no_design": "This plan's design is lost.",
        "realmlord.build.need_manual": "You must learn <%s> to build from this plan.",
        "realmlord.build.busy": "You are already building something.",
        "realmlord.build.tooltip.size": "%s wide, %s high, %s deep",
        "realmlord.build.tooltip.manual": "Needs <%s>",
        "realmlord.build.tooltip.author": "Drawn by %s",
        "realmlord.build.tooltip.use": "Right-click the ground to see where it will stand and what it needs; crouch and right-click the same spot to build it from what you carry. An architect saves materials.",
        # The architect's square
        "realmlord.draft.tip": "Right-click two corners of something you built; crouch and right-click (with paper) to draw it into a plan.",
        "realmlord.draft.sizes": "Largest plan by rank: Junior %s, Intermediate %s, Advanced %s, Master %s blocks across.",
        "realmlord.draft.architects_only": "Only an architect can draw plans.",
        "realmlord.draft.first": "First corner: %s %s %s",
        "realmlord.draft.second": "Marked %s×%s×%s (your largest: %s). Crouch and right-click to draw it.",
        "realmlord.draft.mark_first": "Mark two corners first.",
        "realmlord.draft.too_big": "Too big to draw: your largest plan is %s blocks across.",
        "realmlord.draft.paper": "You need a sheet of paper to draw on.",
        "realmlord.draft.drawn": "Drawn: a plan %s×%s×%s, %s blocks. Rename it on an anvil.",
        # Ships
        "realmlord.ship.stats": "%s seats · speed %s%% · hull %s%% of a boat's toughness",
    })
    return L


LANG = lang()


def loot_entries():
    """Entries for gen_structures' tables: manuals in the ruins."""
    def manual(kind, w, max_grade):
        return {"type": "minecraft:item", "name": f"realmlord:{kind}_manual", "weight": w,
                "functions": [{"function": "realmlord:manual", "kind": kind, "max": max_grade}]}
    return manual


def main():
    textures_and_models()
    recipes()
    java()
    print(f"{len(TECHS)} technical manuals, {sum(len(t['recipes']) for t in TECHS)} recipes, {len(ARTS)} martial arts, {len(ITEMS)} items")
    from collections import Counter
    print(Counter(t["trade"] for t in TECHS))


if __name__ == "__main__":
    main()
