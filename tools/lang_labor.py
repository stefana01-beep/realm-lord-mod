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
    "key.realmlord.world_map": "World map",
    # Learning blueprints
    "realmlord.build.err.blueprint": "Your village has not learned this blueprint. Buy one at the market (or take one from bandits) and right-click it.",
    "realmlord.blueprint.tooltip.learn": "Right-click to teach it to your village: build it as often as you like from then on.",
    "realmlord.blueprint.learn.done": "Your village has learned the %s blueprint. Build it as often as you like.",
    "realmlord.blueprint.learn.known": "Your village already knows the %s blueprint. Keep this one, give it away or sell it at the market.",
    "realmlord.blueprint.learn.no_village": "Only a Lord can teach a blueprint (%s) to their village; found yours first.",
    "realmlord.blueprint.learn.unknown": "This blueprint is blank.",
    "realmlord.manor.bp_learned": "Learned",
    "realmlord.market.learned": "Learned",
    "realmlord.market.buy_bp.tip": "You get the blueprint; right-click it to teach it to your village for good.",
    "realmlord.manor.sites_range": "%s-%s of %s",
    "realmlord.tablet.founded": "Founded",
    "realmlord.activity.sleeping": "Asleep",
    # Goods
    "realmlord.goods.grain": "Grain", "realmlord.goods.wood": "Wood", "realmlord.goods.stone": "Stone", "realmlord.goods.iron_ore": "Iron ore",
    "realmlord.goods.salt": "Salt", "realmlord.goods.meat": "Meat", "realmlord.goods.fish": "Fish", "realmlord.goods.leather": "Leather",
    "realmlord.goods.herbs": "Medicinal herbs", "realmlord.goods.flour": "Flour", "realmlord.goods.wine": "Three Flower Wine",
    "realmlord.goods.pottery": "Pottery", "realmlord.goods.tools": "Iron tools", "realmlord.goods.weapons": "Weapons",
    "realmlord.goods.linen": "Linen (roll)", "realmlord.goods.fine_silk": "Fine silk (roll)", "realmlord.goods.fire_oil": "Fire oil (barrel)",
    # The market's platforms
    "realmlord.market.platform.materials": "Materials",
    "realmlord.market.platform.special": "Special Items",
    "realmlord.market.platform.auction": "Auction",
    "realmlord.market.platform.negotiation": "Negotiation",
    "realmlord.market.platform.locked": "Not open yet. As in the novel, the Limited Time Auction and the Negotiation Cooperation platforms open as the market grows (town level).",
    "realmlord.market.materials_title": "Material Trading Platform",
    "realmlord.market.sell_all": "All",
    "realmlord.market.sell_all.tip": "Sell the whole stock, less the 20% tax.",
    "realmlord.market.goods_note": "Green goods come from your workshops (daily change in colour). Storehouse: %s stacks.",
    "realmlord.market.sell_special": "Sell gear and blueprints in your pack",
    "realmlord.market.sell_special.tip": "Graded weapons and armour (Bronze 20 silver, Black Iron 1 gold, Silver 5 gold, Gold 20 gold) and building blueprints (half their price), less the 20% tax.",
    "realmlord.market.special_note": "The Special Items Trading Platform deals in rare things: graded equipment and building blueprints. Sell what you carry, or buy the blueprints your village can build below.",
    "realmlord.market.sold_special": "The market bought your special items for %s.",
    "realmlord.market.nothing_special": "You carry nothing the Special Items platform buys.",
    # The manor storehouse
    "realmlord.storage.title": "%s Storehouse",
    "realmlord.storage.open": "Storehouse",
    "realmlord.storage.open.tip": "The Lord's Manor storehouse: 54 slots to keep anything for later (salvaged materials, tools, loot). Kept with your territory.",
    "realmlord.storage.hand_in": "Hand in from pack",
    "realmlord.storage.hand_in.tip": "Puts every log, plank, stone, iron ore, grain, meat, fish, leather and other trade good you carry into the village's stores, where builders and workshops use them and the market can sell them.",
    "realmlord.storage.sell": "Sell storehouse goods",
    "realmlord.storage.sell.tip": "Sells every trade good in the storehouse at market prices, less the 20% tax. Other items stay.",
    "realmlord.storage.handed_in": "%s items handed in to the village stores.",
    "realmlord.storage.sold": "The storehouse's goods sold for %s.",
    # The cleared bandit camp
    "realmlord.camp.choose": "What becomes of the bandit camp? %s  %s",
    "realmlord.camp.keep_button": "[Keep the camp]",
    "realmlord.camp.salvage_button": "[Salvage it]",
    "realmlord.camp.keep_tip": "Leave the tents, pen and campfire standing where they are.",
    "realmlord.camp.salvage_tip": "Take the whole camp apart: every pole, cloth, bar, log, barrel and the campfire comes back to you as materials, with anything left inside, and the ground grows over.",
    "realmlord.camp.kept": "The bandit camp stays standing.",
    "realmlord.camp.salvaged": "You take the bandit camp apart and carry off %s items of materials. Nothing of it remains.",
    "realmlord.camp.not_now": "There is no cleared bandit camp waiting for your decision.",
    # Building on marsh, and crews
    "realmlord.build.err.water": "Too deep. Settlers fill in ponds, marsh and shallow water up to 3 blocks deep, but not this.",
    "realmlord.manor.crew.tip": "Builders on this site. + and - set the number yourself (gold), taking on idle residents if needed; going below zero returns the site to sharing the free builders (grey). Every builder adds work, with no limit.",
    "realmlord.worldmap.title": "World Map",
    "realmlord.worldmap.me": "Find me",
    "realmlord.worldmap.whole": "Whole world",
    "realmlord.worldmap.at": "X %s, Z %s",
    "realmlord.worldmap.at_real": "X %s, Z %s  (%s°N, %s°E)",
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
    L["realmlord.building.small_residential_courtyard.desc"] = (
        "A walled courtyard in the old style: a hall across the back and a wing down each side around a yard. "
        "Straw beds for 20 residents (23 in all), who sleep here at night.")
    L["realmlord.map.place_hint"] = ("Click the map to place it here. Right-click turns it; the yellow edge is the door. "
                                     "Green fits, red does not. Builders will clear and level the ground first.")
