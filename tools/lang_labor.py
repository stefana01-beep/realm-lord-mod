"""Text for working residents, site stages, demolition, the fence gates and choosing where to begin."""

TEXT = {
    # What residents are doing
    "realmlord.activity.idle": "Wandering the village",
    "realmlord.activity.home": "Resting at home",
    "realmlord.activity.build": "Building",
    "realmlord.activity.chop": "Felling trees",
    "realmlord.activity.gather": "Working the rock face",
    "realmlord.activity.farm": "Tending the fields",
    "realmlord.activity.staff": "Working in the workshop",
    "realmlord.activity.admin": "Handling village affairs",
    "realmlord.activity.deposit": "Carrying goods to the manor",
    "realmlord.resident.doing": "Now: %s",
    "realmlord.resident.pack_label": "Pack:",
    "realmlord.resident.pack_empty": "Nothing",
    "realmlord.resident.open_pack": "Open pack",
    "realmlord.resident.pack": "%s's pack",

    # Sites and demolition
    "realmlord.manor.clearing": "%s: clearing",
    "realmlord.manor.levelling": "%s: levelling",
    "realmlord.manor.demolishing": "Taking down %s",
    "realmlord.manor.cancel.tip": "Stop building. Three quarters of the wood and stone not yet used come back now; builders take down what stands and carry back three quarters of what it used.",
    "realmlord.build.demolishing": "Your builders will take down the %s and carry its materials back to the stores.",
    "realmlord.map.demolish": "Demolish",
    "realmlord.map.demolish.confirm": "Click again to confirm",
    "realmlord.map.demolish.tip": "Builders take it down from the top and carry three quarters of its wood and stone back to the stores, as planks and cobblestone.",
    "realmlord.map.demolishing": "%s (taking down, %s%% done)",
    "realmlord.map.state.built": "Built",
    "realmlord.map.state.site": "Under construction, %s%%",
    "realmlord.map.state.demolishing": "Being taken down",
    "realmlord.map.salvage": "Taking it down returns about %s wood and %s stone.",
    "realmlord.map.view_hint": "Click a building to select it; you can then demolish it. Ground you have not loaded nearby shows dark. Choose Place in the Construction tab to put a building down from here.",

    # The fence
    "realmlord.map.fence.lay": "Lay out fence",
    "realmlord.map.fence.set": "Set gates",
    "realmlord.map.fence.four": "Four gates",
    "realmlord.map.fence.edit": "Fence gates",
    "realmlord.map.fence.gates": "%s gates",
    "realmlord.map.fence.hint": "Click the ring to add a gate there, or click a gate (gold) to remove it. Each gateway is three posts wide.",
    "realmlord.fence.gates_set": "The fence now has %s gates.",

    # Choosing where to begin
    "realmlord.spawn.title": "Where Will You Begin?",
    "realmlord.spawn.begin": "Begin here",
    "realmlord.spawn.coords": "%s, %s",
    "realmlord.spawn.custom": "Your own choice (%s, %s)",
    "realmlord.spawn.custom.desc": "Anywhere on land. You will arrive on the nearest dry ground.",
    "realmlord.spawn.hint": "Scroll to zoom and drag to move the map. Click a red marker or anywhere on land, then press Begin here.",
    "realmlord.spawn.distance": "%s blocks %s, at %s, %s",
    "realmlord.spawn.arrived": "You arrive at %s, %s. This is now where you wake.",
    "realmlord.spawn.err.placed": "You have already chosen where to begin.",
    "realmlord.spawn.err.far": "That place is out of reach.",
    "realmlord.spawn.err.sea": "That is open water. Choose somewhere on land.",
    "realmlord.spawn.dir.here": "away (here)",
    "realmlord.spawn.dir.east": "east",
    "realmlord.spawn.dir.southeast": "south-east",
    "realmlord.spawn.dir.south": "south",
    "realmlord.spawn.dir.southwest": "south-west",
    "realmlord.spawn.dir.west": "west",
    "realmlord.spawn.dir.northwest": "north-west",
    "realmlord.spawn.dir.north": "north",
    "realmlord.spawn.dir.northeast": "north-east",
    "realmlord.spawn.world_spawn": "World spawn",
    "realmlord.spawn.world_spawn.desc": "Begin where the world begins.",
    "realmlord.cmd.spawn": "Choosing a starting place.",
}

CHINA = {
    "shanhai": ("Lianzhou Basin (Shanhai)",
                "Where Ouyang Shuo founded Shanhai Village in the novel: a warm, fertile basin in the far south, ringed by mountains "
                "and opening onto the sea. Far from the great powers, with room to grow."),
    "central_plains": ("Central Plains (Luoyang)",
                       "The heart of the land on the Yellow River: wide, flat, fertile farmland that is easy to build on. Open on every side, so easy to reach and hard to defend."),
    "guanzhong": ("Guanzhong (Chang'an)",
                  "The Wei River valley, ringed by mountains and guarded by passes. Fertile and easy to defend; many dynasties rose here."),
    "sichuan": ("Sichuan Basin (Chengdu)",
                "The Land of Abundance: rich farmland walled in by mountains on every side. Safe and wealthy, but far from everywhere."),
    "jiangnan": ("Jiangnan (Jianye)",
                 "The Yangtze delta: rivers, lakes and warm, wet country. Rich in food and close to the sea."),
    "jingxiang": ("Jingxiang (Xiangyang)",
                  "A crossroads on the Han River between north and south. Good land, but every army passes through."),
    "youyan": ("Youyan (Jingdu)",
               "The northern frontier beyond the plains: cooler country near the mountains, with room to grow."),
    "fujian": ("Fujian coast (Quanzhou)",
               "A hilly southern coast of harbours and green hills. Warm and out of the way."),
    "yunnan": ("Yunnan (Dali)",
               "Highland lakes and mountains in the far south-west. Remote, mild and beautiful."),
}

BIOMES = {
    "plains": ("Open plains", "Flat, open grassland: the easiest ground for a village to grow on."),
    "forest": ("Forest edge", "Woodland with plenty of timber, and clearings to build in."),
    "river": ("River valley", "Beside a river: water, fertile banks and easy travel."),
    "coast": ("Sea coast", "A beach by the sea, with fish and open views."),
    "savanna": ("Warm savanna", "Dry, warm grassland with scattered trees."),
    "taiga": ("Northern taiga", "Cool spruce forest: hard country, but rich in wood."),
    "hills": ("Hill country", "Hills and blossom groves, with stone close at hand."),
    "jungle": ("Southern jungle", "Hot, wet jungle, thick with growth."),
}


def add(L):
    L.update(TEXT)
    for k, (name, desc) in CHINA.items():
        L[f"realmlord.spawn.china.{k}"] = name
        L[f"realmlord.spawn.china.{k}.desc"] = desc
    for k, (name, desc) in BIOMES.items():
        L[f"realmlord.spawn.biome.{k}"] = name
        L[f"realmlord.spawn.biome.{k}.desc"] = desc
    L["realmlord.building.simple_fence.desc"] = (
        "A weak fortification that keeps out wild animals. Rings the whole territory border: each post is an oak "
        "plank set in the ground, two fence blocks and a plank rail on top. You choose where the gates go. +10 Security.")
    L["realmlord.map.place_hint"] = ("Click the map to place it here. Right-click turns it; the yellow edge is the door. "
                                     "Green fits, red does not. Builders will clear and level the ground first.")
