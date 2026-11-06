# Realm Lord

A standalone Forge mod for **Minecraft 1.20.1** about ruling a territory, inspired by the structure of the web novel *The World Online*. Choose to be a **Lord** or an **Adventurer**. Lords win a Village Creation Token through a demanding quest, found a village, and raise it grade by grade into a kingdom while earning noble titles through merit. Adventurers roam free with combat skills or a trade.

Requires **Forge 47.x** for 1.20.1. No other mods needed. Runs on its own or alongside Gu Path.

**Download:** every push to `main` builds a jar and runs the in-game tests. Grab the newest from the [Releases page](../../releases).

**Status:** Phases 1 and 2a of 7: Lords and Adventurers, the Village Creation quest, and running a village from Grade 1 to Grade 3. See the roadmap below.

---

## Controls
| Key | Action |
|---|---|
| **J** | Character panel (Character, Quest and Territory tabs) |
| **M** | World map |
| **R** | Use the selected skill |
| **Y** | Next skill (sneak + Y: previous) |

Keys can be rebound under Options → Controls → Realm Lord.

## Choosing where to begin
Right after choosing your path (below), you choose where in the world to start. A Lord's quest clock only starts once you arrive.
- **Earth Online: China worlds:** a map of China fills the window, showing the novel's provinces (Lingnan, Chuannan, Shu, Zhongyuan, Jiangnan and the rest), the great rivers and the mountain ranges. Scroll to zoom and drag to move around. Nine places are recommended, starting with the **Lianzhou Basin (Shanhai)**, where the novel begins, then the Central Plains (Luoyang), Guanzhong (Chang'an), the Sichuan Basin (Chengdu), Jiangnan (Jianye), Jingxiang (Xiangyang), Youyan (Jingdu), the Fujian coast (Quanzhou) and Yunnan (Dali). Each comes with a line on what the land is like. Pick one, or click anywhere on land.
- **Other worlds:** the world spawn, plus the nearest open plains, forest, river valley, sea coast, savanna, taiga, hill country and jungle found within 3,000 blocks. Each shows how far away it is and in which direction.

You arrive on the nearest dry ground, and that spot becomes where you wake. Operators can offer the choice again with `/realm spawn`.

## Choosing your path
When you first join, choose a game mode. The choice is permanent. Choosing where to begin comes next.

- **Lord:** rules a territory. Takes the Village Creation quest, earns merit and titles, and picks a **sub-profession** (any combat or work occupation).
- **Adventurer:** roams freely and learns a quarter faster. Picks one occupation, either combat or work.

### Combat occupations
The six classes of the novel's Chinese region. Each has a character level (up to 100) and three skills, learned at levels 1, 10 and 25.

| Occupation | Skills |
|---|---|
| Strategist | Fire Stratagem, Confusion Array, Battle Insight |
| General | War Cry, Cavalry Charge, Unyielding Will |
| Chivalrous Expert | Swift Sword, Lightness Skill, Hundred Strikes |
| Priest (Taoist) | Thunder Talisman, Golden Elixir, Exorcism Seal (double damage to undead) |
| Healer | Rejuvenate, Acupuncture Needle, Purify |
| Bailiff | Iron Shackles, Arrest, Patrol Sense |

Experience follows the novel's formula: **EXP = 30(N³ + 5N) − 80** for level N (100 for level 1, 460 for level 2, and 10,000 takes a new character to level 6). Monsters give 4 experience per point of their health. Dying costs a fifth of a level's experience, never a whole level.

### Work occupations
Blacksmith, Tailor, Physician, Architect, Breeder, Fisherman, Farmer, Veterinarian, Winemaker, Cook, Carpenter, Mason and Shipbuilder. As in the novel, a work Adventurer has **no character level**, only a craft level from 1 to 10: **Junior** (1–3), **Intermediate** (4–6), **Advanced** (7–9) and **Master** (10). Work occupations never lose experience on death. Each trade learns by doing its work and gets a bonus that grows with rank: smiths forge Unbreaking gear, farmers reap extra, cooks are fed more, carpenters and masons cut faster, and so on.

### Attributes (as in the novel)
**Innate attributes** are rolled between 5 and 20 when you create your character, and points can't raise them:
- **Body Structure:** toughness and survival. Raises maximum health.
- **Comprehension:** how fast you learn. +1% to all experience per point above 5.
- **Luck:** windfalls and losses. Feeds the game's luck for fishing and loot.
- **Charm:** first impressions with NPCs. Matters once talents and merchants arrive.

**Acquired attributes** all start at **5**, with **2 free points** at creation, **2 points per level**, and **+1 to all four every fifth level**, up to **100**:
- **Command:** troop strength, troop count, recruiting and response time (from Phase 4). Already makes your tamed animals hit harder.
- **Force:** special-attack power and critical hits (0.25% chance per point, for half again the damage). Also adds to melee damage.
- **Intelligence:** longer buffs and controls and shorter cooldowns; later, troop vision and weapon development.
- **Politics:** policies on markets and farms, and diplomacy (from Phase 2).

Each craft level gives 2 points. Your **reputation** grows with deeds and is shown on the panel.

Characters made before this update had their attributes reset to these rules: all four go back to 5 (plus their every-fifth-level bonuses), all points are refunded, and innate attributes are rolled.

## Titles and merit
Lords earn **merit**, and titles rise automatically: Baron, Viscount, Earl and Marquis (third, second and first class each), then Duke, Prince and Son of Heaven. Third Class Baron needs 100 merit and each class doubles that, up to 204,800 for First Class Marquis; Duke needs 1,000,000, Prince 2,000,000 and Son of Heaven 5,000,000. Every territory upgrade needs a minimum title. Your title shows before your name in chat and the player list.

## The Village Creation quest (Lords only)
You have **10 days** (Minecraft days, 20 real minutes each). The bandits are strong, so get iron armour first.
1. **Gather supplies:** 64 logs, 64 stone and 32 food, handed over in the Quest tab.
2. **Free the settlers:** bandits hold them captive in a camp 150–220 blocks away, always on dry, fairly level ground (never on the sea, a lake, a river, a beach or a swamp): four swordsmen, two archers and their chief, all looking like players. Defeat them all.
3. **Claim your reward** in the Quest tab.

The token's grade depends only on how quickly you finish. The Quest tab shows the time left and the token you'd get right now.

| Finished within | Days left | Token |
|---|---|---|
| 3 days | 7–10 | **Gold** (only one per world; later fast finishes get Silver) |
| 5 days | 5–7 | Silver |
| 7.5 days | 2.5–5 | Black Iron |
| 10 days | 0–2.5 | Bronze |

As in the novel, the reward is the **village creation set**: a **Village Creation Token**, a **Summoning Token** and a **Resources Bag**. A Gold run (or Silver once the Gold token is gone) gets an **Emperor-rank** Summoning Token and an **Intermediate** bag (5,000 food, 500 wood, 500 stone, 500 iron ore); slower runs get a King-rank token and a Basic bag (a fifth of that). Using the bag empties it into your territory's stores. The Summoning Token will call a historical figure of its rank once talents arrive in Phase 3. There's also a completion bonus: Gold 10,000 experience, 500 reputation and 100 gold coins, as in the novel; Silver 5,000 / 250 / 50 gold; Black Iron 2,000 / 100 / 10 gold; Bronze 1,000 / 50 / 50 silver.

**Bandits** drop copper coins and sometimes a building blueprint or a Bronze-grade weapon or armour piece. The **chief** drops his Black Iron-grade sword, a purse of silver and his ledger. The ledger points to the **chief's buried treasure** (optional): gold and silver coins, two building blueprints, a Silver-grade weapon and a Black Iron-grade piece of armour. The camp's barrels hold stolen grain and supplies.

Freed settlers wait by the camp and move into your village when you found it, each with their own name. When you found the village, the novel's **founding tablet** rises before the Lord's Manor: a stone column three blocks tall bearing the village's name.

**The cleared camp is yours to keep or salvage.** Once the bandits are gone, the chat and the Quest tab offer **Keep the camp** or **Salvage it** (also `/realm camp keep` or `/realm camp salvage`).
- **Salvage it:** the whole camp is taken apart. Every tent pole, cloth, pen bar and log, the barrels, the hay and the campfire come back to you as items, along with anything still inside them. The trampled ground grows over, so no stray structure is left in the world.
- **Keep the camp:** it stays as it is.

You can decide any time after clearing the camp, even after claiming your reward.

Run out of time or abandon the quest and you lose the right to found a village, continuing as an Adventurer.

### Loot
- **Coins:** copper, silver and gold, as in the novel: 1 gold = 100 silver = 10,000 copper. Copper is everyday money, and a gold coin is a lot. Use a coin to break it into 100 of the coin below; sneak and use to change 100 coins into one of the coin above. Phase 2's market uses them.
- **Graded equipment:** Bronze, Black Iron, Silver and Gold grades, each with more damage (weapons) or armour and health (armour).
- **Blueprints:** many of the novel's buildings need one (Dining Hall, Small Residential Courtyard, Blacksmith Workshop and more). Keep them for Phase 2.

## Founding your village
Use the token on the ground. A Lord's Manor is raised there, and your territory begins as a **Grade 1 Village** with a 64-block border (80 at Grade 2, 96 at Grade 3, up to 384 for a capital). The first village founded in the world earns 1,100 merit, the second 1,000 and so on down to 100; the first ten are announced to everyone. Rename the token in an anvil first to name your village. The territory keeps its token grade forever. Use the manor seal to open your territory panel.

The Territory tab shows what the next upgrade needs. These are the novel's requirements:

| Level | Population | Title | Indexes |
|---|---|---|---|
| Village 2 | 200 | Second Class Baron | |
| Village 3 | 500 | First Class Baron | |
| Town 1 | 1,000 | Third Class Viscount | 30 |
| Town 2 | 2,000 | Second Class Viscount | |
| Town 3 | 5,000 | First Class Viscount | |
| County 1 | 10,000 | Third Class Earl | 45 |
| County 2 | 25,000 | Second Class Earl | |
| County 3 | 50,000 | First Class Earl | |
| Prefecture 1 | 100,000 | Third Class Marquis | 65 |
| Prefecture 2 | 500,000 | Second Class Marquis | |
| Prefecture 3 | 1,000,000 | First Class Marquis | |
| Capital | 2,000,000 | Duke | 90 |

## The China world: "Earth Online: China"
When you create a world, set **World Type** to **Earth Online: China** (More World Options). The overworld becomes the real land of China and its neighbours, with Luoyang at 0, 0. Caves, ores and the small structures are vanilla. Vanilla villages never generate (in any world): settlements are the players' own.

- **Heights are real:** they come from NOAA's ETOPO2 elevation data (one reading every 3.7 km).
  - The North China Plain lies a few blocks above the sea.
  - The Sichuan Basin sits in its ring of mountains near y 120.
  - The Loess Plateau and Yunnan rise past y 200.
  - The Tibetan Plateau stands near y 370, and the Himalaya climb above y 440.
- **Rivers and lakes are real:** they come from Natural Earth and are cut into the land.
  - Lowland rivers (the lower Yangtze, the Pearl, the Huai) fill from the sea.
  - Upland rivers and lakes (the Yellow River at Lanzhou, Qinghai Lake) get still water laid in their beds.
  - The Yangtze is about 50 blocks wide.
- **Climate:** follows latitude, altitude and distance from the sea. The Taklamakan and Gobi are desert, the plateau is snow, the north-east is forest and steppe, and the far south is jungle and mangrove.

### The scale
| | |
|---|---|
| Horizontal | **1 block = 50 m** (1:50). One map pixel = 64 blocks = 3.2 km. |
| The mapped area | 132,096 × 94,016 blocks (X −81,472 to 50,624, Z −47,616 to 46,400): from Central Asia to Japan, Lake Baikal to the South China Sea |
| China itself | about 107,000 blocks east to west (Kashgar to the Ussuri) and 79,000 north to south (Mohe to Sanya) |
| Distances from Luoyang | Chang'an 6,450 · Jianye 12,900 · Jingdu 13,800 · Chengdu 17,600 · Shanhai 29,100 blocks |
| Walking | Luoyang to Chang'an about 25 minutes; to Jingdu about 55; across China about 7 hours |
| East–west | exact at 35°N; about 27% stretched at 50°N and 13% squeezed at 20°N (one flat grid for the whole map) |
| Height | the world runs from y −64 to y 576. Height above the sea is squeezed more the higher it goes: 50 m → y 70, 100 m → y 75, 500 m → y 118, 1,000 m → y 166, 2,000 m → y 244, 3,000 m → y 304, 4,500 m → y 369, 7,500 m → y 443 |
| Seas | 30 m of depth per block, at least 5 blocks deep, deepest floors at y 13 |

At true 1:1 scale China would be about 5 million blocks across, a two-week walk, so 1:50 keeps the real shapes while leaving room for roads and travel.

`/realm capitals` lists the coordinates of the historic capitals (Jingdu, Chang'an, Luoyang, Jianye, Chengdu, Xiangyang, Dali, Quanzhou and Xianyang). Existing worlds are unchanged; only new worlds with this world type use the map, and worlds made with an older version of the mod keep their old terrain only where it is already generated.

## The world map (M)
Press **M** anywhere to open the world map. Scroll to zoom, drag to move around, **Space** or **Find me** to return to yourself, and **Whole world** to see everything.
- **On the China world:** zoomed out, it is an atlas of the whole land. It shows real heights, rivers and lakes, the novel's provinces (Lingnan, Chuannan, Shu, Zhongyuan, Jiangnan and the rest), seas and neighbouring lands, and the nine Imperial Capitals. Hovering shows block coordinates and the real latitude and longitude.
- **Zoomed in:** the land you have seen this session is drawn block by block, like a vanilla map.
- **Territories:** every territory is ringed, yours in green.
- **Other worlds:** the map shows the land you have seen.

## Running your village (Phase 2)
**Right-click the Lord's Manor Seal** to open the Manor screen. It has four tabs:

- **Overview:** the novel's territory panel.
  - Name and grade, People's Thoughts, Security, population against the cap and housing, and immigrants per day.
  - The Gaia calendar, flagged in sowing season.
  - The treasury (with Withdraw) and taxes.
  - The four stores, each with its daily change.
  - The political, economic, cultural and military indexes.
  - What the next upgrade still needs, and the Upgrade button.
- **Construction:** every building of your grade and the next, with its wood, stone, time, blueprint and specialist, and how many the upgrade needs. Click **Place** to put it down on the map, or choose to place it in the world: an outline follows your crosshair, green where it fits and red where it doesn't. **Right-click** places it, **sneak + right-click** turns it, **left-click** cancels. The gold edge marks the door side. Choosing a spot puts nothing down; your builders go there and clear and level it themselves. Ponds, marsh and shallow water up to 3 blocks deep can be built on, because the settlers fill them in with earth; deeper water cannot. Trees, mangrove roots, giant mushrooms and plants don't count as ground: the builders clear them, and sites may rise or fall up to 10 blocks across, which the builders cut and fill level. The Simple Fence is laid out on the map too: its ring follows your border, and you click the ring to put gates wherever you want them.
- **People:** assign residents to Construction, Logging, Quarrying, Farming, Mining and Administration with the +/- buttons; the best-suited idle residents go first. Open any talent's card from the list.
- **Market:** the novel's Basic Market, with its trading platforms.
  - **Material Trading Platform:** 17 goods with your stock, the daily change from your workshops, and Buy 10 / Buy 100 / Sell 10 / Sell all. Prices follow the novel where it gives them: grain 10 copper, wood 20, stone 30, iron ore 100, salt 20, Three Flower Wine 1 silver, a roll of linen 10 silver, fine silk 50 silver, fire oil 1 silver a barrel. Meat, fish, leather, herbs, flour, pottery, iron tools and weapons are priced alongside. Every sale pays a 20% tax.
  - **Workshops make goods each day** (with their specialist at work; inputs come from the stores):
    - Wine Workshop: 60 grain → 20 Three Flower Wine (3 to 1, as in the novel).
    - Mill: 100 grain → 100 flour.
    - Pottery Workshop: 20 stone → 20 pottery.
    - Blacksmith Workshop: 10 iron ore → 5 iron tools.
    - Weapons Workshop: 9 iron ore → 3 weapons.
    - Tailor Workshop: 2 rolls of linen.
    - Mulberry Shed: a roll of fine silk every two days.
    - Clinic: 10 herbs.
    - Grocery Shop: 20 salt.
  - **Special Items Trading Platform:** sell the graded gear and blueprints you carry (Bronze 20 silver, Black Iron 1 gold, Silver 5 gold, Gold 20 gold; blueprints at half price), and buy the building blueprints your village can use (2, 5 or 10 gold by grade).
  - **Auction** and **Negotiation** are shown but locked, as in the novel's early market.
  - **The storehouse:** the Lord's Manor keeps a 54-slot storehouse for anything you want to keep, such as salvaged camp materials, tools and loot. **Hand in from pack** puts every trade good you carry into the village stores (logs count 2 wood, planks and stone 1, crops 1 grain, and so on). **Sell storehouse goods** sells the trade goods in the storehouse.

- **Map:** a top-down map of your territory, drawn like a vanilla map, showing your border, the Lord's Manor, every building and site, and you. Point at a building to see what it is, or click it to select it and **Demolish** it. **Fence gates** lets you move the fence's gates at any time; the posts change over at once. Choosing **Place** in Construction opens this map with the building's footprint following your mouse: green where it fits, red where it doesn't, with the door side marked. Click to place, right-click to turn, or switch to placing it in the world. You can also open the map anywhere from the **Territory map** button on your character panel (J).

**Construction.** Starting a building takes its wood and stone from your stores, needs its blueprint to be known to your village, and needs a resident of the right trade where the novel requires one (a blacksmith for the Blacksmith Workshop, an officer for the Military Camp, and so on). Your builders walk to the site and work it by hand in three stages. First they **clear** it, felling trees and breaking whatever stands on the footprint, and keep the logs and stone they get. Then they **level** it, filling holes and dips beneath it with earth. Then they **build** it block by block, each one in a builder's hands as it goes up, never skipping a block whose ground isn't loaded. Before a building counts as finished, they go over the whole design once more and put right anything missing or knocked out of place. If no builder is nearby (say you are away and your village is still loaded), or they are stuck for 30 seconds, the village carries on without them. Five builders take the novel's listed time (12 hours for a Dining Hall, one real minute per hour by default). There is **no limit on builders**: each one adds work, so ten builders finish twice as fast as five. Carpenters, masons and Master Builders work faster.
- **Blueprints are learned, not used up:** right-click a blueprint to teach it to your village for good, then build that kind of building as often as you like. A blueprint you carry but haven't learned is learned the first time you build with it. Spare copies can be given away or sold on the Special Items platform. The Construction tab shows **Learned**, and the market marks blueprints you already know.
- **Sites under way:** the list under the buildings scrolls with the arrows or the mouse wheel, showing which sites you're looking at (for example "4-6 of 9"). Each site has its own crew controls.
- **Sharing builders:** builders are shared evenly among all the sites, so several buildings rise at once.
- **Crews:** in the Construction tab, each site shows its builders with **−** and **+**. Setting a number yourself (shown in gold) gives that site a fixed crew, taking on idle residents if there aren't enough builders. Going below zero returns the site to sharing (grey). When a workshop is finished, a resident of its trade moves in to run it.

**Buildings.** Every Village Grade 1–3 building from the novel:
- **Grade 1:** Simple Fence, Dining Hall, Bathroom, Small Residential Courtyard (17 × 15, straw beds for 20), Blacksmith Workshop, Grocery Shop, Logging Field, Quarry, Farmland and Military Camp.
- **Grade 2:** Ancestral Hall, Clinic, Private School, Martial Arts Dojo, Granary, Tailor Workshop, Mining Field, Arrow Tower and Stone Outpost.
- **Grade 3:** Temple, Weapons Workshop, Pottery Workshop, Mill, Mulberry Shed, Wine Workshop, Sentry Post and Stable.

Each has its own look, in a style of stone plinths, red posts, white walls and dark tiled roofs.

**Residents.** Your village begins as the novel's did, with ten refugees: a village chief (a Bronze civil servant), two fruit farmers, a carpenter, a mason and five farmers. Settlers you freed from the bandits join them.
- **Immigrants:** about 10 a day (more with a better token and happier people), but only into free housing (the manor holds 10, each courtyard 20) and up to your grade's limit (200, 500, 1,000).
- **Talents:** some immigrants are special talents (blacksmiths, grocers, tailors, doctors, scholars, martial artists, potters, winemakers, cooks, hunters, fishermen, miners, stablemen, civil servants and officers). They're graded Bronze, Black Iron or Silver, with better stats and a specialty.
- **Their card:** right-click a resident to open it. **Card** shows their grade, occupation and rank, loyalty, Command, Force, Intelligence, Politics, specialty and an evaluation. **Work** (Lord only) sets their job or the workshop they run. **Trade** lets anyone buy and sell with them, priced in copper. Craftsmen sell their best goods only while they work in their workshop; a visitor's purchases go into the village treasury.
- **Who you see:** up to 24 residents walk around the village at once (talents first). The rest live on in the records.
- **Their day:** by day, each goes to their work.
  - Builders go to the site.
  - Loggers fell real trees around their Logging Field, log by log, and replant a sapling of the same kind.
  - **Fields take any number of workers:** a Logging Field, Quarry, Mining Field or Farmland is no longer full at its nominal size, and every extra worker adds their full share of wood, stone, ore or harvest.
  - Quarrymen and miners work the rock face in the pit.
  - Farmers walk their fields.
  - Craftsmen stay in their workshop, and administrators stay by the manor.
  - Anyone without a job wanders a different part of the village every while instead of crowding the manor.
  - At night they go to bed. The first ten sleep on the straw beds in the Lord's Manor hall, and twenty more in each residential courtyard; each resident has their own bed. They lie down in it and get up for work in the morning. Anyone without a bed waits by the manor.
- **Their pack:** every resident has a 9-slot pack, shown on their card. The Lord can open it with **Open pack** to take things out or hand things over.
  - Logs, planks, stone and cobblestone, raw iron and wheat they carry are handed in at the manor when the pack fills or at day's end.
  - What a worker carries in counts towards their day's work. The rest of their quota still comes in at the end of the day, so nothing is lost while you are away.
- **Taking buildings down:** select a finished building on the map and click **Demolish** (twice, to confirm). Builders take it down from the top and bring back three quarters of its wood and stone as planks and cobblestone. Do this to move a building, or to rebuild the fence on your wider border after an upgrade. Cancelling a site returns three quarters of what it had not yet used straight away; whatever was already built is taken down the same way.
- **The Simple Fence:** each post is an oak plank set into the ground, two oak fence blocks, and an oak plank on top, so the tops join into a rail. Each gateway is three posts wide, with a fence gate under the rail.
  - **Even line:** post heights are smoothed so a lone bump or dip doesn't break the line, and earth is packed under any post whose ground falls away.
  - **No gaps:** a post lower than its neighbours stands taller so the fence never leaves a gap, and trees in the way are felled whole.

**The day.** Every novel day (24 real minutes by default):
- Loggers cut 10 wood each, quarrymen 5 stone and miners 2 iron ore, more with the right trade and your token's production bonus.
- Everyone eats 1 food.
- Taxes come in.
- People's Thoughts and Security drift towards what your buildings support. Hunger hurts, and a hungry or unhappy village loses people.

**Farming.** Farmland is sown as soon as it has farmers. Sowing at Qingming (month 4) or early July (month 7) gives the full yield; a month or more off gives 10–50% less. The harvest comes three months later: 200 food per mu, more with a better token. Crops visibly grow in the field.

**Upgrading.** As in the novel:
- **Village Grade 2** needs 200 people, a Second Class Baron and every Grade 1 building, including ten courtyards.
- **Village Grade 3** needs 500 people, a First Class Baron and every Grade 2 building.

Each upgrade pays merit by world order (1,100 for the first), widens your border, and offers three random buildings of the new grade, one of which you get free.

## Server settings
`config/realmlord-common.toml`:
- `lordRestriction` (default **off**): the novel's rule that Lords cannot leave their territory except for war. Other dimensions are exempt.
- `ticksPerNovelHour` (default 1200, one real minute): construction times and the length of a novel day.
- `daysPerMonth` (default 2): the Gaia calendar's month length, which sets how often you can sow and harvest.
- `visibleResidents` (default 24): how many residents walk around a village at once.
- `daysToComplete` (10), `goldWithinDays` (3), `silverWithinDays` (5), `blackIronWithinDays` (7.5), and an experience multiplier.

## Admin commands (op)
`/realm` · `/realm info [player]` · `/realm territories` · `/realm capitals` · `/realm role` (anyone) · `/realm spawn` · `/realm reset [player]` · `/realm level <n>` · `/realm worklevel <n>` · `/realm points <n>` · `/realm merit add|set <n>` · `/realm token <grade>` · `/realm quest start|supplies|clear`

## Roadmap
The order follows the novel: Ouyang Shuo summons his first general, sets up the militia and his secretaries, and beats off the beastwave while Shanhai is still a village. The campaign battlefields only open once enough towns stand.

1. **Lord and Adventurer cores** (done)
2. **Village grades 1–3** (2a done, with a territory map and the China world type: every building from the novel with its costs, blueprints and specialists, placement and construction, residents with jobs, talents and trade, resource jobs, the farming calendar, the market and upgrades).
   - **2b next:**
     - Using the Summoning Token calls your first historical general or minister.
     - The militia team and the Barracks (Grade 2).
     - The village's Political, Economic and Cultural indexes.
     - The first secretaries running village affairs.
     - Beast lairs to hunt, such as the wild boar king and its den (the first dungeons).
     - The **beastwave** world quest, with gold for the best defenders.
     - Hidden buildings such as the Mazu Temple.
3. **Town grades 1–3:**
   - More historical generals and ministers, recruited through quests and reputation.
   - Departments growing into a cabinet.
   - Troops beyond the militia.
   - Mountain-barbarian tribes to defeat or take in.
   - Ruins and deeper lairs to explore.
4. **County:**
   - The regular army and formations.
   - Raids between lords.
   - Harder dungeons.
5. **The campaign system:** once enough towns stand (100 in the novel), historical battlefield maps open, starting with the Battle of Zhuolu and the Battle of Muye.
   - Lords may leave their territories there to fight.
   - Lords win famous generals.
   - Rival lords and alliances.
6. **Prefecture and province:**
   - The Military index.
   - Secret upgrade conditions.
   - Wars on the frontier against the Xiongnu.
   - Greater hidden buildings.
7. **Capital, kingdom and dynasty:** the road to Son of Heaven.

**Planned for the world** (to be fitted into the phases above):
- **The great capitals:** the nine Imperial Capitals built as massive walled cities, the only ready-made settlements in the world.
- **Bandit camps from the start:** small, medium and large camps scattered across the land, harder and richer as they grow.
- **Native tribes:** mountain barbarians and others, as in the novel, to trade with or make war on.
- **Roads:** built between territories, speeding up travel along them.
- **Quick travel:** journeys that cost in-game time for the traveller (days on the road, food eaten) without changing the time for anyone else.
- **Ships and weapons:** more kinds of boats and warships, and the novel's weapons and siege engines.

## Building from source
Java 17, then `./gradlew build` (jar in `build/libs`). Textures, models and the language file are generated by `python3 tools/gen_assets.py`. Every build runs in-game tests on a real server: choosing a role, titles, levels and points, every skill, every work rank, the full Village Creation quest, token grading, bandit strength and drops, loot items, founding a village with its settlers, dimension travel and syncing.

Original code, art and text. Inspired by the structure of *The World Online*; not affiliated with the novel or its author.
