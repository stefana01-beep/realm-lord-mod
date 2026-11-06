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
2. **Free the settlers:** bandits hold them captive in a camp in the **Bandit Lands**, a wild country of its own (each Lord's band has its own stretch of it). Go when you're ready: click **Travel to the bandit camp** in chat or in the Quest tab (or `/realm camp travel`), and you arrive 50 blocks from the camp. The band is four swordsmen, two archers and their chief, all looking like players. Defeat them all, then click **Return home** (or `/realm camp return`) to go back to where you set out from. The freed captives don't need leading anywhere: they join you when you found your village.
3. **Claim your reward** in the Quest tab.

While you hunt the bandits, the ones from your camp within 128 blocks **glow with an outline** that only you can see, so they can't hide in jungle or tall grass. If you already rule a village, the quest is not needed and can't be started.

The token's grade depends only on how quickly you finish. The Quest tab shows the time left and the token you'd get right now.

| Finished within | Days left | Token |
|---|---|---|
| 3 days | 7–10 | **Gold** (only one per world; later fast finishes get Silver) |
| 5 days | 5–7 | Silver |
| 7.5 days | 2.5–5 | Black Iron |
| 10 days | 0–2.5 | Bronze |

As in the novel, the reward is the **village creation set**: a **Village Creation Token**, a **Summoning Token** and a **Resources Bag**. A Gold run (or Silver once the Gold token is gone) gets an **Emperor-rank** Summoning Token and an **Intermediate** bag (5,000 food, 500 wood, 500 stone, 500 iron ore); slower runs get a King-rank token and a Basic bag (a fifth of that). Using the bag empties it into your territory's stores. The Summoning Token will call a historical figure of its rank once talents arrive in Phase 3. There's also a completion bonus: Gold 10,000 experience, 500 reputation and 100 gold coins, as in the novel; Silver 5,000 / 250 / 50 gold; Black Iron 2,000 / 100 / 10 gold; Bronze 1,000 / 50 / 50 silver.

**Bandits** drop copper coins and sometimes a building blueprint or a Bronze-grade weapon or armour piece. The **chief** drops his Black Iron-grade sword, a purse of silver and his ledger. The ledger points to the **chief's buried treasure** (optional): gold and silver coins, two building blueprints, a Silver-grade weapon and a Black Iron-grade piece of armour. The camp's barrels hold stolen grain and supplies.

**The bandits' spoils.** Clearing the camp gives you two Grade 1 village blueprints and 5 gold coins from the chief's strongbox. The supplies they had stolen (400 grain, 200 wood, 150 stone and 30 iron ore) are set aside and go straight into your village's stores when you found it.

Freed settlers move into your village when you found it, but only while there are beds: the Lord's Manor sleeps ten, and the novel's ten refugees take those. The rest wait and move in as soon as you build housing (a Small Residential Courtyard has 20 beds). When you found the village, the novel's **founding tablet** rises before the Lord's Manor: a stone column three blocks tall bearing the village's name.

**The cleared camp is yours to keep or salvage.** Once the bandits are gone, the chat and the Quest tab offer **Keep the camp** or **Salvage it** (also `/realm camp keep` or `/realm camp salvage`).
- **Salvage it:** the whole camp is taken apart. Its timber and stone go to your village's stores (or are set aside for them if you haven't founded it yet). The cloth, pen bars, barrels, hay and campfire come back to you as items, along with anything still inside them. The trampled ground grows over, so no stray structure is left in the world.
- **Keep the camp:** it stays as it is.

You can decide any time after clearing the camp, even after claiming your reward.

Run out of time or abandon the quest and you lose the right to found a village, continuing as an Adventurer.

### The novel's goods
- **Three Flower Wine:** drink for Strength (one minute) and a little healing. Too much makes your head spin.
- **Medicinal Herbs:** eat for a quick heal.
- **Barrel of Fire Oil:** throw it; where it lands, the ground bursts into flame and every creature close by catches fire. As in the novel's beastwave defence. Flames are only set where the thrower may build.
- **Salt, Roll of Linen, Roll of Fine Silk:** trade goods, worth their market price.

The village's workshops make them every day; the grocer sells salt and fire oil, the tailor linen and silk, the doctor herbs and the winemaker wine.

**The Lord's Handbook** comes with your role: a short book on founding, building, the map, the fence, people, the day, food, the market, goods and protecting your land. Lost it? `/realm guide`.

### Loot
- **Coins:** copper, silver and gold, as in the novel: 1 gold = 100 silver = 10,000 copper. Copper is everyday money, and a gold coin is a lot. Use a coin to break it into 100 of the coin below; sneak and use to change 100 coins into one of the coin above. Phase 2's market uses them.
- **Graded equipment:** Bronze, Black Iron, Silver and Gold grades, each with more damage (weapons) or armour and health (armour).
- **Blueprints:** many of the novel's buildings need one (Dining Hall, Small Residential Courtyard, Blacksmith Workshop and more). Keep them for Phase 2.

## Founding your village
Use the token on the ground and **name your village** in the box that opens (an anvil name on the token fills it in). A Lord's Manor is raised there, and your territory begins as a **Grade 1 Village** with an 88-block border (104 at Grade 2, 120 at Grade 3, up to 384 for a capital). As in the novel, the territory reaches past the village wall: each grade's wall stands 26 blocks inside its border (62 blocks out at Grade 1, 78 at Grade 2 and 94 at Grade 3), so fields and approaches outside the wall are still yours.

**Levelling the grounds.** As soon as the village is founded, your builders start bringing the whole site to the level of the Lord's Manor floor, out to **104 blocks**: ten past where the Grade 3 wall will stand, even where that lies beyond your border.
- **How:** they work outwards from the manor, ring by ring. Hills, rock and trees above the floor are cut away, and everything cut goes into the stores. Hollows, ponds and ravines below it are filled with the land's own earth: dirt with grass on top in most places, sandstone with sand on top in deserts, beaches and seas, red sandstone and red sand in badlands. Buried trunks, leaves, flowers and water below the floor are replaced with earth too, so the floor is earth all the way down to rock (hollows up to 64 blocks deep). However large the difference, the result is one level, right up to the manor hall.
- **What is spared:** buildings and sites (with a block around them), paths, the manor hall, a wall already rising, and any column holding something that isn't natural ground (planks, cobblestone, a chest), so nothing you have built is cut away.
- **Speed:** every builder adds to it, so more builders level faster. Buildings placed on the grounds while it goes on are set at the manor's height, and a wall not yet begun waits until the ground is level.
- **Stopping and starting:** the territory map's **Stop levelling** button calls it off (what's done stays done), and **Level the grounds** starts it again, for example for a village founded before this existed. `levelOnFounding` in the config turns off the automatic start. The first village founded in the world earns 1,100 merit, the second 1,000 and so on down to 100; the first ten are announced to everyone. The territory keeps its token grade forever. Use the manor seal to open your territory panel.

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
  - Every river runs unbroken along its real course, traced to within a block or two of its line.
  - Lowland rivers (the lower Yangtze, the Pearl, the Huai) fill from the sea and run 8 to 12 blocks deep, deep enough for ships.
  - Upland rivers and lakes (the Yellow River at Lanzhou, Qinghai Lake) get still water laid in their beds, as deep as their banks allow (up to four blocks).
  - Widths follow each river's size: the Yangtze and the Yellow River run about 70 blocks across, middling rivers 20 to 30, and the least about 8.
  - The river changes apply to land generated from now on; land already explored keeps its old rivers.
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
  - **Material Trading Platform:** 20 goods with your stock, the daily change from your workshops, and Buy 10 / Buy 100 / Sell 10 / Sell all. Prices follow the novel where it gives them: grain 10 copper, wood 20, stone 30, iron ore 100, salt 20, Three Flower Wine 1 silver, a roll of linen 10 silver, fine silk 50 silver, fire oil 1 silver a barrel. Meat, fish, leather, herbs, flour, pottery, iron tools and weapons are priced alongside. Every sale pays a 20% tax.
    - **Take** puts goods from the stores into your pack as items (shift-click for a stack): grain as wheat, wood as logs, flour as bread, wine, salt, linen, silk, herbs and fire oil as their own items, sand, clay and earth as sand, clay balls and dirt. **Hand In** puts them back.
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
  - **The storehouse:** the Lord's Manor keeps a storehouse of **ten pages of a double chest** (540 slots) for anything you want to keep, such as salvaged camp materials, tools, loot, and everything the builders dig up that isn't wood, stone, iron ore, sand, clay or earth (coal, copper, gold, gems, flint, saplings and so on). It **sorts itself** every time you open it (part stacks merged, like kept with like). At the top: **<** and **>** turn the page, **Sort** tidies it, **Take all** fills your inventory from the page, and **Store all** empties your inventory (not your hotbar) into it. Closing it takes you back to the manor panel. **Hand in from pack** puts every trade good you carry into the village stores (logs count 2 wood, planks and stone 1, crops 1 grain, and so on). **Sell storehouse goods** sells the trade goods in the storehouse.

- **Map:** a top-down map of your territory, drawn like a vanilla map, showing your border, the Lord's Manor, every building and site, the ground being levelled (with how far the work has reached), and you. **Scroll** (or the **+** and **-** buttons, or the + and - keys) to zoom in up to eight times, and drag the map to move about when zoomed in (or drag with the middle button while placing something). Point at a building to see what it is, or click it to select it and **Demolish** it. **Fence gates** lets you move the fence's gates at any time; the posts change over at once. Choosing **Place** in Construction opens this map with the building's footprint following your mouse: green where it fits, red where it doesn't, with the door side marked. Click to place, right-click to turn, or switch to placing it in the world. You can also open the map anywhere from the **Territory map** button on your character panel (J).

**Coins.** Coins no longer take up inventory space: coins you pick up or carry go into your **purse**, shown on your character panel and the manor's Overview. Everything you buy is paid from the purse. In the manor's Overview, **Deposit** puts your purse into the manor's **treasury** (where taxes collect too), and **Withdraw** takes it back. When your purse runs short at your own village's market, the treasury makes up the difference.

**Chests.** Every chest, barrel and shulker box has **Sort**, **Take all** and **Store all** buttons above it.

**Workshops, schools and halls.** Click **Workshops & schools...** in the People tab (or select a finished building on the map and click **Manage**) to open a building's own window. It shows the building's master, and:
- **What it makes,** where there's a choice:
  - **Blacksmith:** tools for the market, or graded iron arms and armour (12 iron ore each).
  - **Weapons workshop:** weapons stock, or fine graded blades and bows.
  - **Tailor:** linen, or leather armour.
  - **Clinic:** herbs, or healing draughts.

  Made items go into the storehouse. The master's skill sets their grade: a Junior makes Bronze, an Intermediate Black Iron, an Advanced master Silver and now and then Gold.
- **Training:** the Private School takes up to ten students and the Martial Arts Dojo ten pupils; every other workshop with a master takes three apprentices. Send people from the **Could learn** list, and call them back with **x**. Learners spend their days at lessons instead of other work.
  - **Students** grow in Intelligence and Politics and become scholars in 20 days.
  - **Pupils** grow in Force and as soldiers, and become martial artists in 20 days.
  - **Apprentices** learn the master's trade in 15 days. Someone already of that trade rises a rank in 30 days.
  - A better master (and a historical figure) teaches faster. The master must be at work.

**People.** The People tab's talent list can be **sorted** (grade, trade, work or name; figures and the best grades come first by default) and **filtered** (everyone, figures, soldiers or specialists), and every list scrolls with the mouse wheel. Anyone with a job but nowhere to do it shows *Has no place to work* on their card. A worker who can't find a way to their work turns up there all the same.

**Panels.** If a panel doesn't fit the window (out of full screen, say), it's drawn smaller so that all of it shows. Closing a resident's card, the territory map, a building's window or the storehouse returns you to the manor panel.

**Testing.** In creative mode, the manor's Overview has a **Speed** button that runs the days and the village's work 5, 20 or 60 times as fast. Operators can use `/realm speed <n>`.

**Room between buildings.** Buildings keep a lane of three clear blocks between them, room for a path. Towers and outposts need only one.

**Construction.** Starting a building takes its wood and stone from your stores, needs its blueprint to be known to your village, and needs a resident of the right trade where the novel requires one (a blacksmith for the Blacksmith Workshop, an officer for the Military Camp, and so on). Your builders walk to the site and work it by hand in three stages. First they **clear** it, felling trees and breaking whatever stands on the footprint. Everything useful goes **straight into the village's stores**: logs and planks as wood, stone, gravel and cobblestone as stone, iron ore, and sand, clay and earth as goods of their own; saplings go to the storehouse. The earth is used again to fill holes on other sites. Then they **level** it, filling holes and dips beneath it with earth. Then they **build** it block by block, each one in a builder's hands as it goes up, never skipping a block whose ground isn't loaded. Before a building counts as finished, they go over the whole design once more and put right anything missing or knocked out of place. If no builder is nearby (say you are away and your village is still loaded), or they are stuck for 30 seconds, the village carries on without them. Five builders take the novel's listed time (12 hours for a Dining Hall, one real minute per hour by default). There is **no limit on builders**: each one adds work, so ten builders finish twice as fast as five. Carpenters, masons and Master Builders work faster.
- **Blueprints are learned, not used up:** right-click a blueprint to teach it to your village for good, then build that kind of building as often as you like. A blueprint you carry but haven't learned is learned the first time you build with it. Spare copies can be given away or sold on the Special Items platform. The Construction tab shows **Learned**, and the market marks blueprints you already know.
- **Sites under way:** the list under the buildings scrolls with the arrows or the mouse wheel, showing which sites you're looking at (for example "4-6 of 9"). Each site has its own crew controls.
- **Sharing builders:** builders are shared evenly among all the sites, so several buildings rise at once.
- **Crews:** in the Construction tab, each site shows its builders with **−** and **+**. Setting a number yourself (shown in gold) gives that site a fixed crew, taking on idle residents if there aren't enough builders. Going below zero returns the site to sharing (grey). When a workshop is finished, a resident of its trade moves in to run it.

**Safety and protection.**
- **Safe inside the fence:** once the fence is finished, monsters no longer spawn inside it.
- **Your land is yours:** other players can't break or place blocks or open chests in your territory unless you trust them with `/realm trust <player>` (`/realm untrust <player>`, `/realm trusted` to list). The chests in the Lord's Manor are yours alone, even for trusted players.

**Records.** The manor's **Records** tab shows yesterday's ledger (grain in, food eaten, wood, stone and iron ore in, taxes, arrivals and departures) and how long the food will last. It also keeps the **village log**, the last 60 events with their day: buildings finished, residents arriving, leaving or dying, settlers and wanderers moving in, the fence being rebuilt and hunger. When the grain runs out, the village eats flour, then meat, then fish from the goods shelves before going hungry.

**Towers in the wall.** Build **as many** Arrow Towers, Sentry Posts and Stone Outposts as you like; there's no limit. They may stand anywhere in your territory, and also out on a wall beyond your border (up to ten blocks past the Grade 3 wall). An Arrow Tower or Sentry Post placed near the fence on the map snaps onto it, and the wall makes room for it; below the wall-top its sides are closed except the one facing into the village, so it's never a way through the wall, and above that its open sides join the wall-walk in towns. Taking the tower down closes the gap again. Nothing else may stand on the fence, and nothing may block a gateway.

**Paths.** On the territory map, click **Lay a path**, then click points along the way (up to 32; right-click removes the last). Choose a surface and a width of 1 to 7 blocks, and whether the way is **evened out** (ditches bridged, humps cut down) or **follows the land with steps** (it climbs or drops a block at a time, with a stair facing up the slope at each step, and steeper bits are cut back or banked up). - **Paved areas:** switch **Shape** to *paved area* and click two opposite corners (up to 48 × 48) to pave a square, a market place or a yard.
- **Roads:** a path can run on past your grounds into the wilds as a road, up to 2,048 blocks from the manor, so long as it keeps out of other Lords' lands. Use the map's range button (**Map: village / country / region**) to see 512 or 1,536 blocks around for laying long roads. Builders lay a road as the land along it is loaded, so it grows as you travel along it.
- **Bridges:** wherever a path or road crosses water two or more blocks deep, it becomes a bridge, inside the village or out: a deck just above the water, piers down to the bed under every fourth block of its sides, and rails along the sides of a bridge three or more blocks wide. Dirt, gravel and plank ways get a timber bridge, cobble and stone ways a stone one.
- **Outposts along the roads:** Stone Outposts, Arrow Towers and Sentry Posts may stand within 16 blocks of any of your paths or roads, however far out.

Surfaces:

| Surface | Cost per block |
|---|---|
| Dirt | free |
| Gravel | 1 stone per 2 blocks |
| Plank | 1 wood |
| Cobblestone | 1 stone |
| Stone brick | 2 stone |

The cost is paid when you lay it out. Builders lay the path like any other site, keeping the way even: a ditch is bridged with earth and a hump is cut down. Paths go around buildings and the manor and stop short of the fence. Click a path on the map to select it; **Demolish** turns it back to grass. Paths don't speed up walking yet.

**Clearing and levelling ground.** On the territory map, click **Clear & level ground**, then click two opposite corners (up to 48 × 48 blocks; right-click starts again). The level starts at the usual ground height there; **Lower** and **Raise** move it up to 8 blocks either way. **Grade: slope** instead shapes an even ramp from the ground at the first corner to the ground at the second, for a way up onto raised ground. Builders fell the trees, cut away everything above the level and fill dips below it with earth, and everything useful goes to the stores.
- **No cliffs:** for up to 8 blocks beyond the edges, the ground is cut back and banked up in a slope, one block per block.
- **Finished work is spared:** completed buildings (and a block around them), paths, the fence and the manor yard inside the area are left exactly as they are. Only a site still being built gets in the way.
- Nothing can be built on a clearing until it's done. When the ground is level, the clearing disappears from the map; select one and **Demolish** to call it off (what's done stays done).

**Buildings.** Every Village Grade 1–3 building from the novel:
- **Grade 1:** Simple Fence, Dining Hall, Bathroom, Small Residential Courtyard (17 × 15, straw beds for 20), Blacksmith Workshop, Grocery Shop, Logging Field, Quarry, Farmland and Military Camp.
- **Grade 2:** Ancestral Hall, Clinic, Private School, Martial Arts Dojo, Granary, Tailor Workshop, Mining Field, Arrow Tower, Stone Outpost and Medium Residential Courtyard (21 × 19, a two-storey hall, 50 straw beds).
- **Grade 3:** Temple, Weapons Workshop, Pottery Workshop, Mill, Mulberry Shed, Wine Workshop, Sentry Post, Stable and Large Residential Courtyard (27 × 25, two storeys all round, 100 straw beds).

Each has its own look, in a style of stone plinths, red posts, white walls and dark tiled roofs.

**Residents.** Your village begins as the novel's did, with ten refugees: a village chief (a Bronze civil servant), two fruit farmers, a carpenter, a mason and five farmers. Settlers you freed from the bandits join them as beds allow.
- **Wandering refugees** roam the wilds in ones and twos. Any who come within 48 blocks of a village's border settle there if it has a free bed; if not, they move on and are gone after two days.
- **Immigrants:** about 10 a day (more with a better token and happier people), but only into free housing (the manor holds 10 at Grade 1 only; a small courtyard 20, a medium one 50 and a large one 100) and up to your grade's limit (200, 500, 1,000).
- **Talents:** some immigrants are special talents (blacksmiths, grocers, tailors, doctors, scholars, martial artists, potters, winemakers, cooks, hunters, fishermen, miners, stablemen, civil servants and officers). They're graded Bronze, Black Iron or Silver, with better stats and a specialty.
- **Their card:** right-click a resident to open it. **Card** shows their grade, occupation and rank, loyalty, Command, Force, Intelligence, Politics, specialty and an evaluation. **Work** (Lord only) sets their job or the workshop they run. **Trade** lets anyone buy and sell with them, priced in copper. Craftsmen sell their best goods only while they work in their workshop; a visitor's purchases go into the village treasury.
- **Who you see:** up to 48 residents walk around the village at once (talents first; a server setting). The rest live on in the records.
- **Deaths are for good:** a resident who is killed is gone from the village, their pack falls where they lie, and the village log records it.
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
- **Taking buildings down:** select a finished building on the map and click **Demolish** (twice, to confirm). Builders take it down from the top and bring back three quarters of its wood and stone as planks and cobblestone. Do this to move a building. Cancelling a site returns three quarters of what it had not yet used straight away; whatever was already built is taken down the same way.
- **Which ring:** when you lay out the fence on the map, the **Ring** button picks which grade's wall to raise: Grade 1 at 62 blocks, Grade 2 at 78 or Grade 3 at 94. Any of them can be built at any village grade, even beyond your border, so you can raise the Grade 3 wall from the start. Choosing a different ring later moves the wall: one not yet begun is simply laid out again, and one already standing is taken down (three quarters comes back) and the new one ordered.
- **The Simple Fence** is a wooden palisade: oak planks three blocks high, set one block into the ground, with a spruce top course and slab cap. A stripped spruce post stands every six blocks, with a lantern on every fourth post.
  - **Gateways** are three blocks wide: spruce gates under a dark oak beam with a little tiled roof and a hanging lantern, between tall posts topped with lanterns.
  - **Towns** (from Town Grade 1): the palisade gains battlements and a wall-walk on its inside, level with the top, with ladders up beside each gate, ready for arrow towers later.
  - **Even line:** heights are smoothed so a lone bump or dip doesn't break the line, and earth is packed under any stretch whose ground falls away.
  - **No gaps:** a stretch lower than its neighbours stands taller so the wall never leaves a gap, and trees in the way are felled whole.
  - **Rebuilding:** moving gates, a fence built to the older post-and-rail design, and a village becoming a town all send the builders back to change the wall over, at no cost.

**The day.** A novel day is one Minecraft day, from sunrise to sunrise (20 real minutes). **Sleeping through the night starts the next day at once**, with everything below happening as you wake. Every day:
- Loggers cut 10 wood each, quarrymen 5 stone and miners 2 iron ore, more with the right trade and your token's production bonus.
- Everyone eats 1 food.
- Taxes come in.
- People's Thoughts and Security drift towards what your buildings support. Hunger hurts, and a hungry or unhappy village loses people.

**Farming.** Farmland is sown as soon as it has farmers. Sowing at Qingming (month 4) or early July (month 7) gives the full yield; a month or more off gives 10–50% less. The harvest comes three months later: 200 food per mu, more with a better token. Crops visibly grow in the field.

**Upgrading.** As in the novel:
- **Village Grade 2** needs 200 people, a Second Class Baron and every Grade 1 building, plus courtyard beds for 200: the novel's ten small courtyards, or any mix of small, medium and large ones.
- **Village Grade 3** needs 500 people, a First Class Baron and every Grade 2 building.

Each upgrade pays merit by world order (1,100 for the first), widens your border, and offers three random buildings of the new grade, one of which you get free.

**The Lord's residence.** At Grade 1 the Lord's Manor is the refugees' hall, with ten straw beds. At **Village Grade 2** it is rebuilt as the Lord's own two-storey residence on the same spot, and residents no longer sleep there; they live in the residential courtyards, and anyone without a bed waits outside the manor's yard. Anything kept in the old manor goes to the storehouse, and anyone inside is moved out of the door.
- **Ground floor:** a hall with the manor seal and the Lord's seat, a storeroom of chests and barrels, and a study with bookshelves, a lectern and a map table.
- **Upstairs:** the Lord's bedchamber with a wide red bed and wardrobes, two guest beds and a reading corner.
- **Grade 3 and above** add more: double rows of chests, an armoury corner (anvil, smithing table, grindstone), an enchanting table and brewing stand in the study, four guest beds, a balcony over the door, banners, eave lanterns and a higher roof.

Villages already at Grade 2 or 3 get their residence the next time the manor is loaded.

## Historical figures, the militia and the wilds (Phase 2b)

### Summoning historical figures
Use a **Summoning Token** to summon a figure from China's history to your village. The roster has 171 figures across four ranks: King 54, Emperor 64, Divine 39 and Saint 14. They include generals, warriors, strategists, ministers, scholars, physicians, engineers, craftsmen and admirals, among them Bai Qi, Han Xin, Zhuge Liang, Guan Zhong, Lu Ban, Hua Tuo, Zheng He and Yue Fei.
- **What you get:** a figure of the token's rank. Each figure answers only once in the whole world, as in the novel. Divine and Saint summons are announced to everyone.
- **What they are:** figures join as Silver-grade (King rank) or Gold-grade talents, with high Command, Force, Intelligence and Politics. They need no bed to come, and their card says who they were.

### Companions
On a talent's card, the **Duties** tab has **Follow me**. Up to three talents can walk with you.
- **Keeping up:** they catch up if you get far ahead.
- **Fighting:** they attack whatever hurts you or whatever you fight, plus any monster near you. Gold-grade talents carry a diamond sword, the rest iron. Their health and strength grow with Force.
- **Wounded, not killed:** a historical figure struck down is carried home to recover until the next day.

### Offices
Also on the **Duties** tab, appoint residents to the Lord's offices: the secretaries who run the village while you are away. Each office's effect grows with the stat that matters for it, so a historical figure does it best.

| Office | Opens at | Stat | Effect |
|---|---|---|---|
| Steward | Grade 1 | Politics | More newcomers, happier people, Political index |
| Treasurer | Grade 1 | Politics | More taxes, Political and Economic index |
| Works Officer | Grade 2 | Intelligence | Builders work faster |
| Agriculture Officer | Grade 2 | Intelligence | Bigger harvests and hunts, Economic index |
| Military Officer | Grade 2 | Command | More militia places, faster drill, Security, Military index |
| Commerce Officer | Grade 3 | Politics | Less tax on market sales, Economic index |
| Rites Officer | Grade 3 | Intelligence | Happier people, Cultural index |

Scholars, especially historical ones, also raise the Cultural index.

### Defending the village
Beasts and bandits now go for villagers as well as players. Someone set upon calls for help: soldiers and historical figures within 24 blocks turn on the attacker, a soldier or figure fights back even off duty, and everyone else runs. Anyone who can't fight runs for the manor when a beast comes near.
- **Sheltering until the fence stands:** until the village's fence is finished, everyone spends the night inside the Lord's Manor (those with a bed there sleep in it) instead of out where the beasts roam. Once the fence stands, they go home to their own beds.
- **Soldiers' levels:** every soldier has a level, shown in the People tab. A day at the drill and every kill bring experience; each level adds health, harder blows and a point of Force. A soldier's kill also gives the Lord half its experience.

### The militia and hunters
- **Militia:** enlist residents from the **Duties** tab. A Military Camp holds 10, each **Barracks** (Grade 2) 50, and a Military Officer adds more.
  - **By day,** half drill at the camp and half walk the fence. **At night,** half keep watch.
  - They fight any monster or beast in the territory, and every day of drill raises their Force.
- **Hunting** is a new job. Hunters go out beyond the border to hunt wild animals, and the village gets meat and hides each day (more from a Hunter by trade).

### Wild beasts and what they leave
The wilds now hold the novel's beasts. They roam by day and night, but never inside a finished fence. (For levels, the newer beasts and the bosses, see "The wilds of Gaia" below.)

| Beast | Where | Drops |
|---|---|---|
| Wild Boar | forests, taiga, jungle, savanna | porkchops, Boar Hide, sometimes a Boar Tusk |
| Grey Wolf (packs) | taiga, forests, snow, mountains | Wolf Pelt, bones |
| Mountain Tiger | jungle, forests, mountains | Tiger Pelt, often a Tiger Bone |
| Snow Bear | snow, taiga, mountains | Bear Paw, beef, leather |
| Boar King | dens | Boar King's Tusk, hides, tusks, meat, 3 gold coins, often a blueprint |

What you can do with the drops:
- **Hides and pelts:** craft into leather (boar and wolf 2, tiger 4), or sell them as leather.
- **Tusk Spear:** a boar tusk on two sticks; hits harder than an iron sword.
- **Boar King's Spear:** the king's tusk with iron and a stick; better than diamond.
- **Tiger Bone Wine:** Three Flower Wine with a tiger bone. Gives Strength II, Regeneration II and Resistance.
- **Braised Bear Paw:** cook a bear paw. A feast that heals and gives absorption.
- **Selling:** tiger bone sells as herbs, and bear paw as meat.

### Beast dens
Now and then a **beast den** appears in the wilds 120 to 220 blocks from a player, at most one in each stretch of country, and never within 250 blocks of the world's spawn. It's a burrow under a mound of earth and roots, and whose lair it is depends on the country:
- **Boar King** with four to six boars: anywhere.
- **Wolf King** with his pack: in snow, taiga and mountains, 600 blocks or more from spawn.
- **White Tiger King** with his leopards: in jungle and mountains, 1,500 blocks or more from spawn.
- **Finding one:** you're told its coordinates when you first come near.
- **The king:** a boss with a health bar. Slaying him clears the den and earns merit (100, 200 or 400). He returns six days later.

### The beastwave
On every other lunar new year (Gaia years 3, 5, 7 and so on), the beasts come against every village at once, in three waves through the night.
- **Warnings:** three days before, one day before, and on the day.
- **The night:** beasts gather beyond the border and drive on the manor. Bigger villages draw more, and from Grade 2 a Boar King leads the last wave.
- **Their strength:** the first beastwaves are young beasts a new village can hold off (around levels 1 to 6, the king a few levels above them). Each wave of the night is a little stronger, and each beastwave, and each village grade, adds a few levels more. A finished fence keeps them out while defenders fight from the walls and gates.
- **No sleeping:** you can't sleep through a beastwave.
- **Villages no one is near** fight it unseen: a fence and enough militia keep them safe; otherwise they lose food and some residents.
- **Rewards:** at dawn the best defenders get 10, 6 and 4 gold coins, plus merit for every kill. Anyone else with five kills gets 1 gold. Militia kills count for their Lord.

### Hidden buildings
Some of the novel's buildings stay out of every list until a village earns them. The **Mazu Temple**, to the goddess of the sea, is revealed to a village of Grade 2 or more that lies by the sea. It gives +5 People's Thoughts and +6 Cultural.

## The wilds of Gaia (levels, horses and ruins)

### Novel creatures only
Minecraft's monsters no longer appear in the overworld: no zombies, skeletons, creepers, spiders, endermen, witches, phantoms, slimes, drowned or raiders. Dungeons with monster spawners are gone too. The wilds belong to the novel's beasts and to bandits, who now roam the open land everywhere (one in twenty bandits is a chief). The Nether and the End keep their own creatures. Turn this off with `novelMobsOnly = false`.

### Levels over every head
Every beast and bandit shows its level above its head, for example **Lv.4 Grey Wolf**. Each level adds 6% health and 4% damage, and higher-level beasts grow bigger. As in the novel, the wilds near where players start are gentle: levels rise slowly with distance from the world's spawn (+1 per 1,500 blocks, up to +10).
- **Fewer of them:** a few beasts about at a time (two by day and three by night within 64 blocks), in small groups, and thinner still near the world's spawn (a third as many within 500 blocks, two thirds within 1,500).
- **Kings keep to their lairs:** beast kings never wander the wilds. They are found only in dens (see below).
- **Experience:** every kill of a beast or bandit gives character experience, more for higher levels and for foes above your own level, less for ones far below it.

| Creature | Level | Where | Drops |
|---|---|---|---|
| Wild Boar | 1–4 | forests, taiga, jungle, savanna | porkchops, Boar Hide, sometimes a Boar Tusk |
| Wild Ox (fights back if attacked) | 1–5 | plains, savanna, meadows | beef, leather, sometimes an Ox Horn |
| Grey Wolf (packs) | 2–6 | taiga, forests, snow, mountains | Wolf Pelt, bones |
| Bandit (chief +4) | 3–8 | everywhere | coins, gear |
| Leopard | 4–9 | jungle, savanna, forests, mountains | Leopard Pelt |
| Black Bear | 5–10 | forests, taiga | Bear Paw, beef, leather |
| Snow Bear | 6–12 | snow, taiga, mountains | Bear Paw, beef, leather |
| Mountain Tiger | 8–14 | jungle, forests, mountains | Tiger Pelt, often a Tiger Bone |
| **Wolf King** (boss) | 14–20 | dens in snow, taiga and mountains, 600+ blocks out | 3 wolf pelts, Wolf King's Fang, a gold coin, sometimes a blueprint. Calls three grey wolves to his side. |
| **Boar King** (boss) | 10–15 | dens | as before |
| **White Tiger King** (boss) | 20–30 | dens in jungle and mountains, 1,500+ blocks out | White Tiger Pelt, 3 tiger bones, 5 gold coins, a blueprint |

The bosses have a health bar. New uses for the drops:
- **Leopard Pelt:** crafts into 3 leather.
- **Fang Dagger:** made from the Wolf King's Fang. A fast blade.
- **Horn Bow:** two ox horns and string make a bow.
- **White Tiger Pelt:** a rare treasure that sells as fine silk.

### Graded horses
Every horse has a grade and a breed, shown above its head in the grade's colour, for example **Silver Dawan Horse**. A name you give it with a name tag is kept.

| Grade | Chance | Breed | Speed | Jump | Health |
|---|---|---|---|---|---|
| Bronze | 62% | Mongolian Horse | slow | low | 18–22 |
| Black Iron | 26% | Hequ Horse | good | fair | 22–26 |
| Silver | 9% | Dawan Horse | fast | high | 26–30 |
| Gold | 3% | Bloodsweat Horse (the novel's Hanxue BaoMa) | fastest | highest | 30–36 |

Foals take after their parents: two Gold horses give at least a Silver foal, and sometimes better than either parent.

### Ancient ruins
Pillager outposts, ruined portals, woodland mansions, igloos, witch huts, jungle temples, ocean monuments and ancient cities no longer generate. Shipwrecks, ocean ruins, trail ruins, mineshafts and strongholds remain. Desert temples remain but are about three times rarer. In their place, the land holds ruins from China's past:
- **Ancient Shrine** (plains, forests, taiga, jungle, savanna, cherry groves): a roofed stone shrine with an altar chest of coins, materials, graded gear and blueprints.
- **Ruined Watchtower** (hills, mountains, plains, savanna, taiga, meadows): a broken beacon tower held by bandits, with a supply chest at the foot and a richer one at the top.
- **Ancient Tomb** (deserts, badlands, plains, savanna, forests): a sunken tomb watched by tomb robbers. Its inner treasure chest holds gold, high-grade gear, blueprints up to Grade 3, and sometimes a summoning token.

### Graded gear and effects
Silver gear found in ruins has a 50% chance of a special effect, and Gold gear always has one. The effect is listed on the tooltip.
- **Weapons:**
  - **Lifesteal:** heals you for 15% of the damage you deal.
  - **Flame:** sets the target on fire.
  - **Frost:** slows the target.
  - **Beastbane:** +30% damage against beasts.
- **Armour:**
  - **Swift:** faster movement.
  - **Stalwart:** resists knockback and adds toughness.
  - **Vigor:** extra health.

## Server settings
`config/realmlord-common.toml`:
- `lordRestriction` (default **off**): the novel's rule that Lords cannot leave their territory except for war. Other dimensions are exempt.
- `ticksPerNovelHour` (default 1200, one real minute): construction times. (A novel day is always one Minecraft day; with the daylight cycle switched off, days follow the world's running time.)
- `daysPerMonth` (default 2): the Gaia calendar's month length, which sets how often you can sow and harvest.
- `visibleResidents` (default 48): how many residents walk around a village at once.
- `protectTerritories` (default **on**): only a territory's Lord and the players they trust may build, break or open chests there; the Lord's Manor chests are the Lord's alone. Operators are exempt.
- `safeInsideFence` (default **on**): monsters don't spawn inside a finished fence.
- `levelOnFounding` (default **on**): a new village's builders level the grounds to the manor straight away.
- `novelMobsOnly` (default **on**): no vanilla monsters in the overworld; only the novel's beasts and bandits.
- `daysToComplete` (10), `goldWithinDays` (3), `silverWithinDays` (5), `blackIronWithinDays` (7.5), and an experience multiplier.

## Admin commands (op)
`/realm speed <n>` · `/realm camp travel|return` (anyone on the quest) · `/realm` · `/realm info [player]` · `/realm territories` · `/realm capitals` · `/realm role` (anyone) · `/realm spawn` · `/realm reset [player]` · `/realm level <n>` · `/realm worklevel <n>` · `/realm points <n>` · `/realm merit add|set <n>` · `/realm token <grade>` · `/realm quest start|supplies|clear` · `/realm beastwave [start|stop]` (with no argument: when the next one comes) · `/realm den`

## Roadmap
The order follows the novel: Ouyang Shuo summons his first general, sets up the militia and his secretaries, and beats off the beastwave while Shanhai is still a village. The campaign battlefields only open once enough towns stand.

1. **Lord and Adventurer cores** (done)
2. **Village grades 1–3** (done):
   - **2a:** a territory map and the China world type, with every building from the novel and its costs, blueprints and specialists; placement and construction; residents with jobs, talents and trade; resource jobs, the farming calendar, the market and upgrades.
   - **2b:** 171 summonable historical figures, companions, the offices, the militia and Barracks, hunters, the wild beasts and their drops, Boar King dens, the beastwave and the Mazu Temple.
   - **Running the town:** building windows (what workshops make; schools, the dojo and apprentices), the coin purse and treasury, a ten-page self-sorting storehouse, chest sorting, villagers who shelter and fight back, soldiers' levels, experience from every kill, the gentler wilds and dens of three kings, the Bandit Lands, stepped paths and graded slopes.
   - **The village ground:** the grounds levelled to the manor on founding, walls on the ring of any grade, the territory reaching past the wall, unlimited defences on the wall, and a zoomable territory map.
   - **The wilds:** levels on every beast and bandit, graded horses, novel creatures only, three bosses, and ancient shrines, watchtowers and tombs in place of vanilla structures.
   - **Still to come:** the Auction House and special markets (ship blueprints, graded weapons and armour), more hidden buildings, and more figures.
3. **Town grades 1–3:**
   - More historical generals and ministers, recruited through quests and reputation.
   - Departments growing into a cabinet.
   - Troops beyond the militia.
   - Ships and docks: fishermen take boats out to fish on their own.
   - Mountain-barbarian tribes to defeat or take in.
   - Deeper lairs and larger ruins to explore.
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
