"""Text for Phase 2: the village, its buildings, residents, market and screens."""

BUILDINGS = {
    "simple_fence": ("Simple Fence", "A weak fortification that keeps out wild animals. Rings the whole territory border, with a gate on each side. +10 Security."),
    "dining_hall": ("Dining Hall", "Where residents eat together. Lifts People's Thoughts; a cook can work here."),
    "bathroom": ("Bathroom", "Improves sanitation and health. Lifts People's Thoughts."),
    "small_residential_courtyard": ("Small Residential Courtyard", "A walled courtyard with a house for 20 residents. Immigrants only come when there is room."),
    "blacksmith_workshop": ("Blacksmith Workshop", "Makes farm tools such as axes and chisels. Needs a junior blacksmith. A blacksmith working here sells iron tools and weapons."),
    "grocery_shop": ("Grocery Shop", "Sells daily necessities. Needs a grocer, who then sells torches, candles and goods."),
    "logging_field": ("Logging Field", "Speeds up timber: 10 wood per worker per day. Holds 10 workers."),
    "quarry": ("Quarry", "Speeds up stone: 5 stone per worker per day. Holds 10 workers."),
    "farmland": ("Farmland", "10 mu of fields, 200 food per mu each harvest. Sow at Qingming (month 4) or early July (month 7); harvest three months later. Five farmers work it."),
    "military_camp": ("Military Camp", "The basic barracks, where farmers become militia (Phase 2b). Needs an officer."),
    "ancestral_hall": ("Ancestral Hall", "Where residents honour their ancestors. Raises culture and People's Thoughts."),
    "clinic": ("Clinic", "Medical treatment for the villagers. Needs a doctor."),
    "private_school": ("Private School", "Teaches the village's children. Needs a scholar. Raises culture."),
    "martial_arts_dojo": ("Martial Arts Dojo", "A training hall for martial arts. Needs a martial artist."),
    "granary": ("Granary", "Storage for the harvest. Raises the economy."),
    "tailor_workshop": ("Tailor Workshop", "Sews clothing. Needs a tailor, who then sells clothing and cloth."),
    "mining_field": ("Mining Field", "Speeds up iron ore: 2 ore per worker per day. Holds 10 workers."),
    "arrow_tower": ("Arrow Tower", "A weak defensive tower. +5 Security."),
    "stone_outpost": ("Stone Outpost", "A sturdy stone outpost with an early-warning post. +5 Security."),
    "temple": ("Temple", "A place of worship. Lifts People's Thoughts and culture."),
    "weapons_workshop": ("Weapons Workshop", "Makes weapons and armour. Needs an advanced blacksmith."),
    "pottery_workshop": ("Pottery Workshop", "Makes pottery. Needs a potter."),
    "mill": ("Mill", "Grinds grain."),
    "mulberry_shed": ("Mulberry Shed", "Grows mulberries for silkworms."),
    "wine_workshop": ("Wine Workshop", "Brews drinks. Needs a winemaker."),
    "sentry_post": ("Sentry Post", "Warns the territory when enemies approach. +5 Security."),
    "stable": ("Stable", "Keeps horses. A stableman working here sells saddles and leads."),
}

OCCUPATIONS = {
    "farmer": ("Farmer", "A plain, hard-working farmer who knows the soil."),
    "fruit_farmer": ("Fruit Farmer", "Tends orchards and knows every tree by its fruit."),
    "carpenter": ("Carpenter", "Good with timber; fast at logging and building."),
    "mason": ("Mason", "Cuts and lays stone; fast at quarrying and building."),
    "blacksmith": ("Blacksmith", "Forges tools and weapons, given a forge."),
    "grocer": ("Grocer", "Keeps a shop of daily necessities."),
    "tailor": ("Tailor", "Sews clothing and cloth."),
    "doctor": ("Doctor", "Treats the sick; runs a clinic."),
    "scholar": ("Scholar", "Learned in the classics; can teach or help administer."),
    "martial_artist": ("Martial Artist", "Skilled in the martial arts; can run a dojo."),
    "potter": ("Potter", "Shapes and fires pottery."),
    "winemaker": ("Winemaker", "Brews wine and spirits."),
    "cook": ("Cook", "Feeds the village from the dining hall."),
    "hunter": ("Hunter", "Lives off the forest; handy at logging."),
    "fisherman": ("Fisherman", "Fishes the rivers and the sea."),
    "miner": ("Miner", "Knows where ore lies; fast at mining."),
    "stableman": ("Stableman", "Cares for horses."),
    "civil_servant": ("Civil Servant", "Handles administration; raises the political index when given work."),
    "officer": ("Officer", "A minor officer, needed to run a military camp."),
}

JOBS = {
    "idle": "Idle", "construction": "Construction", "logging": "Logging", "quarrying": "Quarrying",
    "farming": "Farming", "mining": "Mining", "administration": "Administration", "staff": "Workshop",
}

SPECIALTIES = {
    "green_thumb": ("Green Thumb", "Grows more on the farmland."),
    "timber_sense": ("Timber Sense", "Cuts more wood in the logging field."),
    "stonecutter": ("Stonecutter", "Cuts more stone in the quarry."),
    "ore_seeker": ("Ore Seeker", "Digs more ore in the mining field."),
    "master_builder": ("Master Builder", "Builds faster on construction."),
    "capable_administrator": ("Capable Administrator", "Raises the political index more in administration."),
    "skilled_hands": ("Skilled Hands", "Works better in a workshop."),
}

TEXT = {
    "realmlord.grade.common": "Commoner",
    "realmlord.money.gold": "%sg",
    "realmlord.money.silver": "%ss",
    "realmlord.money.copper": "%sc",
    "realmlord.reason.upgrade": "upgrading the village",
    "realmlord.panel.territory.coming": "Right-click the Lord's Manor Seal to run your village: build, assign people, trade at the market and upgrade.",

    # Village events
    "realmlord.village.immigrants": "%s immigrants have arrived in %s.",
    "realmlord.village.hungry": "%s has run out of food! People's Thoughts are falling and residents will leave.",

    # Construction
    "realmlord.build.started": "Construction of the %s has begun. It takes %s hours with five builders; you have %s.",
    "realmlord.build.done": "The %s in %s is complete!",
    "realmlord.build.staffed": "%s has started work at the new %s.",
    "realmlord.build.err.not_placed": "That building is not placed by hand.",
    "realmlord.build.err.border": "It must stand inside your territory's border.",
    "realmlord.build.err.manor": "Leave the Lord's Manor yard clear.",
    "realmlord.build.err.overlap": "Too close to another building.",
    "realmlord.build.err.water": "You cannot build on water.",
    "realmlord.build.err.slope": "The ground is too steep there.",
    "realmlord.build.err.grade": "Your village is not yet of the grade to build this.",
    "realmlord.build.err.resources": "Not enough wood or stone in the stores.",
    "realmlord.build.err.blueprint": "You need its blueprint (bandits drop them, or buy one at the market).",
    "realmlord.build.err.specialist": "You need a resident with the right trade to build this.",
    "realmlord.build.err.fence_exists": "The fence is already built or under construction.",

    # Jobs
    "realmlord.job.err.militia": "Militia cannot take other work.",
    "realmlord.job.err.no_building": "That workplace is not built.",
    "realmlord.job.err.no_staff": "Nobody works in that building.",
    "realmlord.job.err.wrong_trade": "That workplace needs a different trade.",
    "realmlord.job.err.staffed": "Someone already works there.",
    "realmlord.job.err.admin": "Only civil servants and scholars can do administration.",
    "realmlord.job.err.full": "No free places for that job. Build more of its fields.",

    # Upgrades
    "realmlord.upgrade.need_pop": "Population %s / %s",
    "realmlord.upgrade.need_title": "Lord title of at least %s",
    "realmlord.upgrade.need_building": "%s: %s / %s",
    "realmlord.upgrade.need_index": "All four indexes at %s or more",
    "realmlord.upgrade.town_later": "Upgrading to a town arrives in Phase 4",
    "realmlord.upgrade.world": "%s has made the world's no. %s %s of grade %s! Reward: %s merit.",
    "realmlord.upgrade.done": "%s is now a %s of grade %s! The Lord's Manor grows with it. Choose one of three buildings to receive for free.",
    "realmlord.upgrade.free_chosen": "You will receive a %s for free. Place it from the Construction tab.",

    # Manor screen
    "realmlord.manor.title": "Lord's Manor",
    "realmlord.manor.tab.overview": "Overview",
    "realmlord.manor.tab.build": "Construction",
    "realmlord.manor.tab.people": "People",
    "realmlord.manor.tab.market": "Market",
    "realmlord.manor.lord": "Lord: %s (%s)",
    "realmlord.manor.thoughts": "People's Thoughts",
    "realmlord.manor.security": "Security",
    "realmlord.manor.population": "Population %s / %s (housing %s)",
    "realmlord.manor.immigration": "Immigrants: %s per day",
    "realmlord.manor.date": "Gaia Year %s, month %s, day %s",
    "realmlord.manor.sowing": " - sowing season!",
    "realmlord.manor.treasury": "Treasury: %s",
    "realmlord.manor.tax": "Taxes: %s per day",
    "realmlord.manor.withdraw": "Withdraw",
    "realmlord.manor.indexes": "Indexes",
    "realmlord.manor.index.political": "Political",
    "realmlord.manor.index.economic": "Economic",
    "realmlord.manor.index.cultural": "Cultural",
    "realmlord.manor.index.military": "Military",
    "realmlord.manor.next": "To become %s grade %s:",
    "realmlord.manor.max_level": "Your territory is at its highest level.",
    "realmlord.manor.ready": "All conditions met. Upgrade!",
    "realmlord.manor.more": "+%s more",
    "realmlord.manor.upgrade": "Upgrade",
    "realmlord.manor.free_choice": "Choose a free building:",
    "realmlord.manor.place": "Place",
    "realmlord.manor.place_free": "Free",
    "realmlord.manor.build": "Build",
    "realmlord.manor.bp": "Blueprint",
    "realmlord.manor.grade_short": "Grade %s",
    "realmlord.manor.scroll": "Scroll: %s / %s",
    "realmlord.manor.sites": "Under construction",
    "realmlord.manor.no_sites": "Nothing is being built.",
    "realmlord.manor.cancel": "Cancel",
    "realmlord.manor.cancel.tip": "Pull down the site. Half of its wood and stone come back.",
    "realmlord.manor.builders": "Builders: %s",
    "realmlord.manor.jobs": "Jobs",
    "realmlord.manor.idle": "Idle: %s of %s residents",
    "realmlord.manor.staffed": "Working in workshops: %s",
    "realmlord.manor.people_hint": "The best-suited idle residents take a job first. Open a talent's card to set their job or workplace yourself.",
    "realmlord.manor.talents": "Talents (%s)",

    # Map
    "realmlord.manor.tab.map": "Map",
    "realmlord.panel.territory.map": "Territory map",
    "realmlord.map.title": "Territory Map",
    "realmlord.map.loading": "Drawing the map...",
    "realmlord.map.legend": "Legend",
    "realmlord.map.legend.border": "Territory border",
    "realmlord.map.legend.manor": "Lord's Manor",
    "realmlord.map.legend.built": "Built",
    "realmlord.map.legend.site": "Under construction",
    "realmlord.map.legend.you": "You",
    "realmlord.map.view_hint": "Point at a building to see what it is. Ground you have not loaded nearby shows dark. Choose Place in the Construction tab to put a building down from here.",
    "realmlord.map.placing": "Placing",
    "realmlord.map.place_hint": "Click the map to place it here. Right-click turns it; the yellow edge is the door. Green fits, red does not.",
    "realmlord.map.rotate": "Turn",
    "realmlord.map.in_world": "Place in the world",
    "realmlord.map.cancel": "Cancel",
    "realmlord.map.refresh": "Refresh",
    "realmlord.map.manor": "Manor",
    "realmlord.map.coords": "X %s, Z %s",
    "realmlord.map.building": "%s (%s%% built)",
    "realmlord.map.staffed": "%s (run by %s)",

    # Placement
    "realmlord.place.start": "Placing: %s",
    "realmlord.place.hint": "Right-click: place · Sneak + right-click: turn · Left-click: cancel",
    "realmlord.place.cancelled": "Placement cancelled.",

    # Market
    "realmlord.market.title": "Basic Market",
    "realmlord.market.your_coins": "Your coins: %s",
    "realmlord.market.unit": "%sc a unit",
    "realmlord.market.buy_n": "Buy %s",
    "realmlord.market.sell_n": "Sell %s",
    "realmlord.market.cost": "Costs %s",
    "realmlord.market.income": "Pays %s after the 20%% tax",
    "realmlord.market.tax_note": "Buys go into your stores and sales come out of them. Sales pay a 20%% transaction tax.",
    "realmlord.market.blueprints": "Building blueprints",
    "realmlord.market.buy": "Buy",
    "realmlord.market.cannot_afford": "You cannot afford that.",
    "realmlord.market.not_enough": "Your stores do not hold that much.",

    # Resident screen
    "realmlord.resident.title": "Resident",
    "realmlord.resident.tab.card": "Card",
    "realmlord.resident.tab.work": "Work",
    "realmlord.resident.tab.trade": "Trade",
    "realmlord.resident.name": "Name: %s (%s)",
    "realmlord.resident.identity": "Identity: villager of %s",
    "realmlord.resident.occupation": "Occupation: %s",
    "realmlord.resident.occupation_ranked": "%s %s",
    "realmlord.resident.loyalty": "Loyalty: %s",
    "realmlord.resident.specialty": "Specialty: %s (+%s%%)",
    "realmlord.resident.job": "Working at: %s",
    "realmlord.resident.militia": "Serving in the militia",
    "realmlord.resident.evaluation": "Evaluation: %s",
    "realmlord.resident.jobs": "Jobs",
    "realmlord.resident.workplaces": "Workplaces for this trade",
    "realmlord.resident.no_workplace": "No finished workplace needs a %s yet.",
    "realmlord.resident.staffed_by": "%s already works here",
    "realmlord.resident.skill_hint": "Their trade, rank and specialty make them %s%% better at their current work.",

    # Trade
    "realmlord.trade.buy": "Buy",
    "realmlord.trade.sell": "Sell",
    "realmlord.trade.costs": "costs %s",
    "realmlord.trade.pays": "pays %s",
    "realmlord.trade.none": "Nothing to trade.",
    "realmlord.trade.no_workshop": "Needs a workshop to work in first.",
    "realmlord.trade.need_items": "You do not have enough to sell.",
    "realmlord.trade.bought": "Bought %s %s for %s.",
    "realmlord.trade.sold": "Sold %s %s for %s.",
}


def add(L):
    L.update(TEXT)
    for bid, (name, desc) in BUILDINGS.items():
        L[f"realmlord.building.{bid}"] = name
        L[f"realmlord.building.{bid}.desc"] = desc
    for oid, (name, ev) in OCCUPATIONS.items():
        L[f"realmlord.occupation.{oid}"] = name
        L[f"realmlord.occupation.{oid}.eval"] = ev
    for jid, name in JOBS.items():
        L[f"realmlord.job.{jid}"] = name
    for sid, (name, desc) in SPECIALTIES.items():
        L[f"realmlord.specialty.{sid}"] = name
        L[f"realmlord.specialty.{sid}.desc"] = desc
