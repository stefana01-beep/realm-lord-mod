# Realm Lord

A standalone Forge mod for **Minecraft 1.20.1** about ruling a territory, inspired by the structure of the web novel *The World Online*. Choose to be a **Lord** or an **Adventurer**. Lords win a Village Creation Token through a demanding quest, found a village, and raise it grade by grade into a kingdom while earning noble titles through merit. Adventurers roam free with combat skills or a trade.

Requires **Forge 47.x** for 1.20.1. No other mods needed. Runs on its own or alongside Gu Path.

**Download:** every push to `main` builds a jar and runs the in-game tests. Grab the newest from the [Releases page](../../releases).

**Status:** Phases 1 to 3 of 7: Lords and Adventurers, the Village Creation quest, running a village from Grade 1 to Grade 3, and the town from Grade 1 to Grade 3, up to the county trial. See the roadmap below.

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
  - **The storehouse:** the Lord's Manor keeps a storehouse of **ten pages of a double chest** (540 slots) for anything you want to keep, such as salvaged camp materials, tools, loot, and everything the builders dig up that isn't wood, stone, iron ore, sand, clay or earth (coal, copper, gold, gems, flint, saplings and so on). It **sorts itself** every time you open it (part stacks merged, like kept with like). At the top: **<** and **>** turn the page, **Sort** tidies it, **Take all** fills your inventory from the page, and **Store all** empties your inventory (not your hotbar) into it. Closing it takes you back to the manor panel. **Hand in from pack** puts every trade good you carry into the village stores (logs count 2 wood, planks and stone 1, crops 1 grain, and so on). **Sell storehouse goods...** opens a list of every good the storehouse holds, with how much it would fetch, so you choose what to sell (**Sell** for one good, or **Sell everything**). Things the market doesn't buy (tools, gear and so on) are never sold.

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

**Safe footing.** Residents only ever appear or move onto land that is loaded and running. One who falls out of the world anyway (land loading in under them, say) is caught before the void and set down at the manor; if no safe ground is loaded, they step out of the world for a moment and come back when the village is next shown, never lost.

**Panels.** If a panel doesn't fit the window (out of full screen, say), it's drawn smaller so that all of it shows. Closing a resident's card, the territory map, a building's window or the storehouse returns you to the manor panel.

**Testing.** In creative mode, the manor's Overview has a **Speed** button that runs the days and the village's work 5, 20 or 60 times as fast. Operators can use `/realm speed <n>`.

**Repairs.** Every two minutes or so the village looks its finished buildings and its fence over. One that has lost blocks (to fire, an explosion, a careless player) is marked damaged, and the builders go back and put back what was lost (nothing is charged). Anything you've put inside is left alone. Fields, the quarry and the mine aren't repaired, since they change as they're worked.

**Placing on the map.** The footprint follows the mouse and is drawn on the exact block grid; zoomed in, you see every block of it, with the block under the mouse marked. The side panel shows the exact corners it will cover. A building can't be put down on a path or paved area already laid; take the path up first.

**Room between buildings.** Buildings keep a lane of three clear blocks between them, room for a path. Towers and outposts need only one.

**Construction.** Starting a building takes its wood and stone from your stores, needs its blueprint to be known to your village, and needs a resident of the right trade where the novel requires one (a blacksmith for the Blacksmith Workshop, an officer for the Military Camp, and so on). Your builders walk to the site and work it by hand in three stages. First they **clear** it, felling trees and breaking whatever stands on the footprint. Everything useful goes **straight into the village's stores**: logs and planks as wood, stone, gravel and cobblestone as stone, iron ore, and sand, clay and earth as goods of their own; saplings go to the storehouse. The earth is used again to fill holes on other sites. Then they **level** it, filling holes and dips beneath it with earth. Then they **build** it block by block, each one in a builder's hands as it goes up, never skipping a block whose ground isn't loaded. Before a building counts as finished, they go over the whole design once more and put right anything missing or knocked out of place. If no builder is nearby (say you are away and your village is still loaded), or they are stuck for 30 seconds, the village carries on without them. Five builders take the novel's listed time (12 hours for a Dining Hall, one real minute per hour by default). There is **no limit on builders**: each one adds work, so ten builders finish twice as fast as five. Carpenters, masons and Master Builders work faster.
- **Blueprints are learned, not used up:** right-click a blueprint to teach it to your village for good, then build that kind of building as often as you like. A blueprint you carry but haven't learned is learned the first time you build with it. Spare copies can be given away or sold on the Special Items platform. The Build tab shows **Learned**, and the market marks blueprints you already know.
- **Sites under way:** the list under the buildings scrolls with the arrows or the mouse wheel, showing which sites you're looking at (for example "4-6 of 9"). Each site has its own crew controls.
- **Sharing builders:** builders are shared evenly among all the sites, so several buildings rise at once.
- **Crews:** in the Build tab, each site shows its builders with **−** and **+**. Setting a number yourself (shown in gold) gives that site a fixed crew, taking on idle residents if there aren't enough builders. Going below zero returns the site to sharing (grey). When a workshop is finished, a resident of its trade moves in to run it.

**Safety and protection.**
- **Safe inside the fence:** once the fence is finished, monsters no longer spawn inside it.
- **Your land is yours:** other players can't break or place blocks or open chests in your territory unless you trust them with `/realm trust <player>` (`/realm untrust <player>`, `/realm trusted` to list). The chests in the Lord's Manor are yours alone, even for trusted players.

**Records.** The manor's **Records** tab shows yesterday's ledger (grain in, food eaten, wood, stone and iron ore in, taxes, arrivals and departures) and how long the food will last. It also keeps the **village log**, the last 60 events with their day: buildings finished, residents arriving, leaving or dying, settlers and wanderers moving in, the fence being rebuilt and hunger. When the grain runs out, the village eats flour, then meat, then fish from the goods shelves before going hungry.

**Towers in the wall.** Build **as many** Arrow Towers, Sentry Posts and Stone Outposts as you like; there's no limit. They may stand anywhere in your territory, and also out on a wall beyond your border (up to ten blocks past the Grade 3 wall). An Arrow Tower or Sentry Post placed near the fence on the map snaps onto it, and the wall makes room for it; below the wall-top its sides are closed except the one facing into the village, so it's never a way through the wall, and above that its open sides join the wall-walk in towns. Taking the tower down closes the gap again. Nothing else may stand on the fence, and nothing may block a gateway.

**Paths.** On the territory map, click **Lay a path**, then click points along the way (up to 32; right-click removes the last). Choose a surface and a width of 1 to 7 blocks, and whether the way is **evened out** (ditches bridged, humps cut down) or **follows the land with steps** (it climbs or drops a block at a time, with a stair facing up the slope at each step, and steeper bits are cut back or banked up). - **Paved areas:** click **Pave an area** on the map and pick a space the same way as for clearing ground: click one corner, then the opposite one (the area follows your mouse until you do, red if over 48 × 48), click again to start over, choose the surface, and click **Pave**. Good for a square, a market place or a yard.
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

**Clearing and levelling ground.** On the territory map, click **Clear & level ground**, then click two opposite corners (up to 48 × 48 blocks; right-click starts again). The level starts at the usual ground height there; **Lower** and **Raise** move it up to 8 blocks either way. **Grade: slope** instead shapes an even ramp from the ground at the first corner to the ground at the second, for a way up onto raised ground. Clearing and grading work beyond your border too (anywhere up to 2,048 blocks from the manor, never in another Lord's land), for ramps up to a road, cuttings through a hill, or level ground for an outpost. Use the map's range button to mark out ground farther away; the ground there must be loaded when you order it. Builders fell the trees, cut away everything above the level and fill dips below it with earth, and everything useful goes to the stores.
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
- **A new village's grace:** while the ground is cleared and until the **Grade 1** wall is finished, no beast or bandit spawns inside its border or within 32 blocks of it, beasts and bandits that wander in ignore its settlers, and nothing they do can hurt them. Once that wall stands, the grace is over and the village must defend itself. Choosing a Grade 2 or Grade 3 wall instead ends the grace at once.
- **A guard from the start:** one of the first ten refugees is a martial artist on watch, strong enough to fight off the young beasts near a new village.
- **Raising the militia:** in a villager's **Duties** tab, **Enlist** makes them a soldier for 20 silver (paid from your purse, then the treasury). Up to three may enlist before there is a Military Camp; each camp adds ten and a Barracks fifty. **Kit** buys a soldier better armour, which they wear: leather (50 silver), then mail (2 gold), then iron (5 gold). Mail and iron also bring an iron sword in place of the stone one.
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
- **Jungles:** Minecraft's own ocelots don't spawn, and the jungle's leopards and tigers are few.
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

### Arms of beast and bandit
What beasts and bandits leave now goes into arms and armour of the novel's world.
- **New drops:**
  - **Beast Sinew:** from wolves, leopards, tigers and bears (about one in three), and four from a king.
  - **Beast Core:** always from a king, and rarely (1 in 25) from a beast of level 10 or more.
  - **Bandit Insignia:** from about a third of bandits, three from a chief.
  - **Bandit Sabre:** now and then from a bandit, graded Bronze (a chief's sometimes Black Iron).
- **Materials you make:**
  - **Black Iron Ingot:** two iron ingots and a coal.
  - **Refined Steel:** two black iron ingots and a tiger bone.
- **Weapons:**

  | Weapon | Made from | Grade | Damage |
  |---|---|---|---|
  | Black Iron Sabre | black iron | Black Iron | 6.5, quick |
  | Black Iron Spear | black iron | Black Iron | 7.5, slow |
  | Refined Steel Jian | refined steel and sinew | Silver | 7.5, quick |
  | Guandao | refined steel | Silver | 10.5, slow |
  | Tiger Bone Bow | tiger bones and sinew | Black Iron | arrows strike harder |

  Forge a Jian, Guandao or Black Iron Sabre again with a **Beast Core** and it comes out **Gold** grade.
- **Armour:**
  - **Hide** (any pelt and sinew): Bronze grade, between leather and mail.
  - **Black Iron:** Black Iron grade, between iron and diamond.

  Each piece is graded as it comes from the crafting table, with that grade's bonuses (and, at Silver and Gold, an effect).
- **The market:** on the Special Items platform, **Armoury...** sells smithing materials and graded arms and armour. **Sell special items...** lists everything in your pack the market buys (graded gear, blueprints, cores, sinew, insignia, black iron, refined steel) with its price, to sell one by one or all at once.
- **Creative tabs:** the mod's items are split into tabs: Realm Lord (tokens, coins, bags), Blueprints, Materials, Weapons & Armour (each piece also shown at its grade) and Creatures & Blocks.
- **Damage:** a beast's level adds 5% health but only 2% damage per level (at most +60%), and the kings hit less hard than before (Wolf King 7, Boar King 9, White Tiger King 11), so a high-level beast takes long to bring down without one blow undoing a player of its level.

### Armour of the dynasties, pills and manuals
More to make from beast and bandit materials, after the arms of China's dynasties and the novel's pills, manuals and grades.
- **Armour** (each painted after the real thing, worn and as an icon):

  | Set | Made from | Grade | Look |
  |---|---|---|---|
  | Hide | pelts and sinew | Bronze | fur with stitched seams and a pale fur hem |
  | Black Iron | black iron | Black Iron | riveted bands with gilt trim and a nasal helm |
  | **Lamellar** (helm, coat, tassets, boots) | lamellar plates, sinew, leather | Black Iron | rows of small scales laced with red cord, a polished heart mirror, a scale skirt over the thighs |
  | **Mountain-Pattern** (helm, armour, greaves, war boots) | refined steel, lamellar plates, red dye | Silver | interlocking 山-shaped steel scales, gilt mirror and trim, red silk sash, red-tasselled helm |
  | **Tiger War** (hood, war coat) | tiger pelts, refined steel, beast cores | Gold | tiger-striped hide with a white belly; the hood wears the tiger's face with the 王 on its brow |

  Mountain-pattern armour gives a little knockback resistance, and the tiger set more.
- **Weapons:** the **Ring-Pommel Dao** (black iron, Black Iron), the **Ji Halberd** (refined steel, Silver, 9.5 damage) and the **Tiger Soul Sabre** (steel, a beast core and a tiger bone, Gold; one blow in four roars, slowing and weakening the foe). Dao and ji can be re-forged with a beast core for Gold.
- **Lamellar Plates:** two black iron ingots and a sinew make four.
- **Sinew** also strings an ordinary bow or crossbow.
- **Whetstone** (flint and two cobblestone, makes 2): right-click with a blade or armour in your other hand to mend a quarter of its wear.
- **Refining Stone** (beast core, two black iron, sinew): right-click with gear in your other hand to raise it a grade. Plain gear becomes Bronze; Bronze to Black Iron holds 85% of the time, to Silver 60%, to Gold 35%. A failure costs only the stone.
- **Medicine:**
  - **Golden Wound Powder** (two medicinal herbs, bone meal, a red mushroom; makes 2): heals 2 hearts.
  - **Rejuvenation Pill** (two wound powders, sinew, a glistering melon slice; makes 2): 4 hearts at once, regeneration, a minute's shield.
  - **Marrow Cleansing Pill** and **Enlightenment Pill** (a beast core, two rejuvenation pills, a gold ingot, and a tiger bone or Three Flower Wine): as in the novel, the special pills that raise an innate attribute, Body Structure or Comprehension, by 1 each (up to 30).
- **Military Rations** (two bread, salt and any cooked meat; makes 4): a filling food for the march. Bandits sometimes carry them.
- **Bandit Insignia:** right-click them in your own territory to post the kills: 2 reputation and a 50 copper bounty each, and Security rises one for every five.
- **Manuals** (equipment blueprints): Field Rations, Lamellar Armour, Polearms, Pill Refining, Mountain-Pattern Armour and Tiger War Gear. Buy them in the Armoury (50 silver to 30 gold), or take them from bandit chiefs (one in four carries one). Right-click to teach your village's masters; this unlocks what the workshops can make (below). The market buys spare manuals at half price.

### More from every building
A building's window now lists everything it can do in rows of three. Choices that need a manual are greyed out (hover to see which). The window says what the chosen one makes, from what, and which storehouse materials it also uses and how many are there.
- **Blacksmith:** tools (stock), iron arms and armour, black iron ingots, whetstones, lamellar plates (uses sinew), black iron blades (sabre, dao), lamellar armour (manual; uses sinew), horse barding.
- **Weapons Workshop:** weapons (stock), fine weapons, arrows (16 at a time), refined steel, steel blades (jian, dao), tiger bone bows (uses tiger bones), polearms (manual: spear, guandao, ji), mountain-pattern armour (manual), the Tiger Soul Sabre (manual; uses a beast core).
- **Tailor:** linen (stock), leather armour, hide armour (uses sinew), banners, bedding, tiger war coats (manual; uses tiger pelts).
- **Clinic:** herbs (stock), healing draughts, golden wound powder, and with the Pill Refining manual, rejuvenation pills (uses sinew), marrow cleansing pills and enlightenment pills (each uses a beast core).
- **Wine Workshop:** wine (stock), tiger bone wine (uses tiger bones), bottled Three Flower Wine.
- **Grocery Shop:** salt (stock), military rations (manual), cured meat, dried fish.
- **Pottery Workshop:** pottery (stock), bricks, roof tiles (terracotta), pots.
- **Stable:** tend the horses, saddles, leads, leather barding.
- **Martial Arts Dojo**, what it teaches:
  - **Fist & palm:** +1 Force a day.
  - **Weapon forms:** twice the soldier's experience.
  - **Internal arts:** Force rises fast, up to 92; 25 days.
  - **Archery:** pupils become **hunters**.
  - **Qinggong:** a short course of 15 days.
- **Private School**, what it teaches:
  - **Classics:** scholars.
  - **Statecraft:** civil servants (Politics).
  - **Military strategy:** officers (Command).
  - **Medicine:** doctors.
  - **Accounts:** grocers.

  Choosing the discipline chooses the trade the students graduate into.
- **Military Camp**, the militia's daily orders:
  - **Drill:** as before.
  - **Formation drill:** much more soldier's experience, less Force, and officers gain Command faster.
  - **Weapons practice:** Force twice as fast.
  - **Archery drill:** a little more of both.
  - **Patrol:** Security rises each day.
  - **Rest:** loyalty rises.

Items a workshop makes are graded by the master as before. A piece that has a grade of its own (lamellar Black Iron, mountain-pattern Silver, tiger Gold) comes out at that grade: a Junior master sometimes falls one short, an Advanced one sometimes rises one above.

### Technical manuals for every trade
As in the novel, "<Mingguang Armor Manufacturing Technology Manual>: After use, gain knowledge of crafting the mingguang armor." A technical manual teaches you to make things at an ordinary crafting table. Until you have learned it, its recipes do nothing.

- **Who can learn them:** anyone can learn a Junior manual that only teaches recipes or a building (any player can craft with the right knowledge). Every other manual needs its trade at the manual's rank or higher.
- **Techniques:** some manuals teach a technique, which simply works for you once learned.
- **Structure manuals:** these come with a building plan (see Building plans below).
- **Your trade's bonus:** when you make something from a manual of your own trade, you gain work experience, and now and then you make one extra. At Advanced rank and above, your trade's arms and armour sometimes come out a grade higher.
- **Where to get them:** the market's **Manuals** page sells every manual (Junior 50 silver, Intermediate 3 gold, Advanced 10 gold, Master 30 gold). Bandit chiefs carry them, and ruins hold them.
- **Your manuals:** the **Arts & Manuals** tab of the character panel (J) lists what you know.

There are 111 manuals, at least six for every trade; ★ marks one taken from the novel.

**Blacksmith**

| Manual | Rank | What it teaches |
|---|---|---|
| Ironware Casting Manual | Junior | Casting chain, iron bars and cauldrons with less iron. |
| Horse Barding Manual | Intermediate | Iron, gold and diamond barding for war horses. |
| Perfusing Steel Technique ★ | Intermediate | The Northern and Southern dynasties' steelmaking: molten pig iron poured over wrought iron. Refined steel without tiger bone, and black iron straight from raw iron. |
| Tang Sword Forging Manual ★ | Intermediate | The Tang hengdao, straight and single-edged, and the great mo dao that cut down horse and rider. |
| Song Dynasty Buren Armor Making Manual ★ | Intermediate | The Song infantry's heavy armour of nearly two thousand scales: hard to knock down. |
| Mingguang Armor Manufacturing Technology Manual ★ | Advanced | The bright-light armour of the Tang cavalry, two polished mirrors on the breast. |
| Hundred-Refined Steel Manual | Master | Forge a steel blade again, folding in three more bars of refined steel and gold: it comes out Gold grade without a beast core. |
| Forge Drawings | Junior | A smithy: blast furnace, anvil, grindstone and a chimney. Comes with the plan for the Forge. |

**Tailor**

| Manual | Rank | What it teaches |
|---|---|---|
| Tanning Manual | Junior | Curing pelts into good leather. |
| Saddlery and Felting Manual | Junior | Saddles and name tags. |
| Silk Reeling and Dyeing Manual ★ | Intermediate | Reeling silk from cocoons, and dyeing the top-quality rainbow silk cloth. |
| Cloth-Faced Armour Manual | Intermediate | The Ming brigandine: plates riveted inside a padded linen coat, the brass studs showing. |
| Night Clothes Pattern | Advanced | The night-walker's black hood and suit. Crouching in the dark, unseen beyond a few paces. |
| Imperial Embroidery Manual | Master | The yellow dragon robe. Whoever wears it is received like a hero. |

**Physician**

| Manual | Rank | What it teaches |
|---|---|---|
| Basic Herbal Formulary | Junior | Gathering herbs from wild flowers, and a detox draught against poison. |
| Clear Heart Pill Formula | Junior | A pill that clears the head and limbs of every sluggishness. |
| Yellow Emperor's Acupuncture Canon | Intermediate | Needles that heal another (or yourself) and clear what ails them. |
| Rhino Horn Poison Curing Pill Formula ★ | Intermediate | Bianque's antidote: cures poison and wither, and wards them off for two minutes. |
| Peiyuan Pill Formula ★ | Advanced | Solidifies the primordial Qi: effective for the badly injured. |
| Bianque's Medical Classic | Master | The special pills without a beast core. |
| Herb Garden Drawings | Junior | Beds of medicinal flowers between paths. Comes with the plan for the Herb Garden. |

**Architect**

| Manual | Rank | What it teaches |
|---|---|---|
| Rammed Earth Manual | Junior | Packed earth walls from dirt, gravel and clay. |
| Lime Mortar Manual | Junior | Stone bricks laid in lime mortar, no kiln needed. |
| Glass Making Technical Manual ★ | Intermediate | Glass from sand and coal, and more panes from each pane of glass. |
| Glazed Tile Manual | Intermediate | Imperial yellow and jade-green glazed tiles, and slate roof tiles. |
| Luopan Geomancy Manual | Intermediate | The geomancer's compass: reads the land wherever you stand. |
| Yingzao Fashi | Advanced | The Song court's building standards: one block in five you place is not used up. |
| Kaogong Ji | Master | The Records of Trades of the Rites of Zhou: two more blocks of reach. |
| Lu Ban's Square | Junior | The architect's drafting square: draw what you have built into a plan, and raise it again anywhere. |
| Road Inn Construction Drawings | Intermediate | An inn for the roads between cities: a hall with tables, a counter and beds. Comes with the plan for the Road Inn. |
| Relay Station Drawings | Intermediate | A courier relay: stalls, hay and water for the post horses. Comes with the plan for the Relay Station. |
| Village Temple Construction Drawings ★ | Intermediate | A Taoist temple on a raised terrace, red pillars and a tiled roof, for a priest to preside over. Comes with the plan for the Village Temple. |
| Granary Drawings | Intermediate | A raised granary that keeps the grain dry. Comes with the plan for the Granary. |
| Siheyuan Drawings | Advanced | A courtyard house: four wings around a garden, a red gate at the front. Comes with the plan for the Siheyuan. |
| Pagoda Drawings | Master | A five-storey pagoda with eaves on every floor. Comes with the plan for the Pagoda. |

**Breeder**

| Manual | Rank | What it teaches |
|---|---|---|
| Qimin Yaoshu: Fodder | Junior | Fodder that readies any farm animal to breed, or grows a young one. |
| Sericulture Manual | Junior | Raising silkworms on leaves for their cocoons. |
| Bole's Classic of Horses | Intermediate | Horse feed: heals, quickens and gentles a horse. |
| Panda Rearing Technical Manual ★ | Advanced | Bamboo feed that makes pandas breed and cubs grow. |
| Wild Beast Taming Technique ★ | Advanced | The wild era's taming art: wolves, cats, parrots and horses take to you at the first try. |
| Iron Armored Beast Domestication Manual ★ | Master | Animals you tame come out tougher: +20 health and +6 armour. |
| Stable Construction Drawings | Intermediate | A stable with stalls, hay and water troughs. Comes with the plan for the Stable. |

**Fisherman**

| Manual | Rank | What it teaches |
|---|---|---|
| Net Weaving Manual | Junior | A casting net: throw it on water for fish. |
| Salt from Sea Water ★ | Junior | Boiling sea water down for salt. |
| Salted Fish Manual | Junior | Salted, dried fish that keeps and fills. |
| Star Guiding Technique ★ | Intermediate | Zheng He's navigation by the stars: see in the dark on the water, and swim like a dolphin. |
| Pearl Diving Manual | Intermediate | Hold your breath far longer under water. |
| Cormorant Fishing Manual | Advanced | Trained cormorants bring in a third more fish. |
| Mazu's Sea Charts | Master | The sea goddess's favour: one catch in ten brings a treasure up with it. |
| Fishing Hut Drawings | Junior | A hut on stilts over the water. Comes with the plan for the Fishing Hut. |

**Farmer**

| Manual | Rank | What it teaches |
|---|---|---|
| Compost Manual | Junior | Turning scraps and leaves into fertiliser. |
| Underground Mushroom Growing Technical Manual ★ | Junior | Mushroom beds in the dark. |
| Tea Cultivation Manual | Intermediate | Plucking and drying tea leaves. |
| Seed Selection Manual | Intermediate | Crops you harvest replant themselves. |
| Spring Weathering Talisman ★ | Advanced | A talisman that brings spring to the fields around it at once. |
| Fan Shengzhi Shu | Master | The Han agricultural classic: crops around you grow faster. |
| Irrigated Field Drawings | Junior | A sown field with a water channel down the middle. Comes with the plan for the Irrigated Field. |

**Veterinarian**

| Manual | Rank | What it teaches |
|---|---|---|
| Animal Salve Formula | Junior | A salve that heals an animal fully. |
| Yuan Heng's Horse Remedies | Intermediate | A tonic that makes a horse fast and sure-footed for five minutes. |
| Beast Whistle Manual | Intermediate | A tusk whistle that calls all your animals to you. |
| Guardian Charm Manual | Advanced | Carried, it saves one of your animals from death once. |
| Hundred Beasts Classic | Master | Your tamed animals strike a third harder and heal as they fight. |
| Animal Pen Drawings | Junior | A fenced pen with a gate, hay and water. Comes with the plan for the Animal Pen. |

**Winemaker**

| Manual | Rank | What it teaches |
|---|---|---|
| Three Flower Wine Recipe ★ | Junior | Lianzhou's famous Three Flower Wine. |
| Sauce Brewing Manual | Junior | Soy sauce for the cook. |
| Medicinal Wine Recipe | Junior | Herbs steeped in wine: heals and steadies. |
| Tiger Bone Wine Recipe | Intermediate | A tiger bone steeped in wine. |
| Dukang Wine Recipe ★ | Intermediate | The wine of Du Kang, father of wine. |
| Maotai Distilling Manual ★ | Advanced | The fiery spirit of Maotai. |
| Drunken Immortal's Brew | Master | Li Bai's own: luck and strength for ten minutes. |

**Cook**

| Manual | Rank | What it teaches |
|---|---|---|
| Dumpling Recipe | Junior | Pork dumplings, four at a time. |
| Rice Noodle Recipe ★ | Junior | A bowl of beef noodle soup. |
| Tea Classic ★ | Junior | Lu Yu's art of tea: green tea that sharpens the hands. |
| Mooncake Recipe | Intermediate | Mooncakes for the mid-autumn moon. |
| Military Grain Pill Recipe ★ | Intermediate | One small pill sustains a soldier for a day. |
| Roast Duck Recipe | Advanced | Lacquered, glazed and roasted. |
| Manchu Han Imperial Feast | Master | The imperial banquet: one dish of it fills you and lends every strength. |
| Kitchen Drawings | Junior | An open kitchen: stove, smoker, water and a table. Comes with the plan for the Kitchen. |

**Carpenter**

| Manual | Rank | What it teaches |
|---|---|---|
| Lu Ban's Joinery | Junior | Two logs make twelve planks. |
| Movable Type Printing | Junior | Bi Sheng's press: paper and books in quantity. |
| Song Dynasty Bow and Crossbow Making Manual ★ | Intermediate | The composite bow of birch, sinew and fish glue, and Zhuge Liang's repeating crossbow. |
| South-Pointing Chariot Technical Manual ★ | Advanced | The geared carriage whose figure always points south, and here also home. |
| Wooden Ox and Flowing Horse | Master | Zhuge Liang's pack carrier: a saddled, laden mule that follows you. |
| Substitute Doll Carving ★ | Master | A wooden doll that dies in your place, once. |
| Mail Post Drawings | Junior | A roadside mail post. Comes with the plan for the Mail Post. |
| Pavilion Drawings | Junior | A roadside pavilion with red pillars and double eaves. Comes with the plan for the Pavilion. |
| Arrow Tower Drawings | Intermediate | A wooden arrow tower with slits and a railed platform. Comes with the plan for the Arrow Tower. |

**Mason**

| Manual | Rank | What it teaches |
|---|---|---|
| Stone Dressing Manual | Junior | Cobblestone dressed into bricks; cracked and carved bricks. |
| Coal Mining Technical Manual ★ | Junior | Coal, iron, copper and gold ore you mine now and then yield more. |
| Gunpowder Making Technical Manual ★ | Intermediate | Charcoal, saltpetre (bone meal) and flint. |
| Jingdezhen Porcelain Manual | Intermediate | Blue-and-white porcelain vases: the market pays well. |
| Jade Carving Manual | Advanced | A jade pendant: held in the off hand, it brings luck and health. |
| Great Wall Masonry | Master | Deepslate bricks by the eight, and stone breaks half again as fast in your hands. |
| Village Well Drawings | Junior | A stone well with a roof over it. Comes with the plan for the Village Well. |
| Beacon Tower Drawings | Intermediate | A stone beacon tower with battlements and a signal fire. Comes with the plan for the Beacon Tower. |
| Paifang Drawings | Intermediate | A stone memorial archway with a golden tablet. Comes with the plan for the Paifang. |
| Zhaozhou Bridge Drawings | Advanced | Li Chun's stone arch bridge, seventeen blocks long: build it from one bank across the water. Comes with the plan for the Zhaozhou Bridge. |

**Shipbuilder**

| Manual | Rank | What it teaches |
|---|---|---|
| Pine Tar Caulking Manual | Junior | Pine tar for hulls, and tar torches. |
| Boat Joinery Manual | Junior | Tarred boats from three planks. |
| Rope and Rigging Manual | Junior | Rope from kelp, and leads. |
| Lifebuoy Manual | Intermediate | Held in the off hand in water: breathe and swim fast. |
| Ming Dynasty Shipbuilding Technical Manual ★ | Advanced | Harbour lights and the diving bell for hull work. |
| Meng Chong Warship Manufacturing Manual ★ | Advanced | The rawhide-covered warship built to ram: four seats, four times as tough as a boat; it strikes and throws aside what it runs down. |
| Fishing Boat Manual ★ | Junior | An ordinary fishing boat that carries two fishermen; fish caught from it come in a third more often. |
| Cima Boat Manual ★ | Intermediate | The fleet's scout: a red-sailed boat half again as fast as any other. |
| Merchant Ship Manual ★ | Intermediate | A trading ship: three seats and a hold. |
| Turreted Ship Construction Manual ★ | Master | The floating fortress with watertight bulkheads: six seats, seven times as tough, crossbowmen who shoot at foes and clapper poles that strike those close. Its master has Conduit Power in water. |
| Dock Construction Drawings ★ | Junior | A wooden pier out into the water: build it from the shore. Comes with the plan for the Dock. |
| Ferry Construction Drawings ★ | Junior | Stone steps down to the water for a ferry. Comes with the plan for the Ferry Landing. |
| Shipyard Construction Drawings ★ | Intermediate | A slipway into the water, framed and craned, with the shipwright's benches. Comes with the plan for the Shipyard. |
| Lighthouse Drawings | Advanced | A stone lighthouse for the harbour mouth. Comes with the plan for the Lighthouse. |

**New things to make:**
- **Armour:**
  - **Mingguang** (Gold): polished steel with two mirrors on the breast, a red plume.
  - **Song Buren** (Silver): dense dark scales, very hard to knock back.
  - **Brigandine** (Black Iron): red linen with brass studs.
  - **Night Clothes:** crouching in the dark, monsters lose sight of you beyond four blocks.
  - **Dragon Robe** (Gold): you are received as a Hero of the Village.
- **Weapons:**
  - **Tang Hengdao** (Silver).
  - **Mo Dao** (Silver).
  - **Composite Bow** (Black Iron, harder arrows).
  - **Zhuge Repeating Crossbow:** Quick Charge II and Multishot.
- **Medicine:**
  - **Detox Draught**, **Clear Heart Pill** and **Rhino Horn Poison Curing Pill** (two minutes of immunity to poison).
  - **Peiyuan Pill:** full heal.
  - **Acupuncture Needles:** heal someone else or, crouching, yourself.
- **Animal care:**
  - **Fodder** and **Bamboo Feed:** ready to breed, or the young grow.
  - **Horse Feed** and **Horse Tonic.**
  - **Animal Salve.**
  - **Beast Whistle:** calls your animals to you.
  - **Guardian Charm:** saves an animal once.
- **Tools and charms:**
  - **Luopan:** reads the land.
  - **South-Pointing Chariot:** points home.
  - **Fishing Net.**
  - **Spring Weathering Talisman:** the crops around grow at once.
  - **Wooden Ox:** a saddled pack mule.
  - **Substitute Doll:** dies in your place once.
  - **Lifebuoy.**
  - **Diving Bell.**
  - **Jade Pendant:** +2 Luck and +2 health in the off hand.
- **Food and wine:**
  - Dumplings, beef noodle soup, green tea, mooncakes and roast duck.
  - **Military Grain Pill:** a day's food in one pill.
  - **Manchu Han Imperial Feast:** fills you and gives every strength.
  - Three Flower Wine, Dukang, Maotai, medicinal wine, tiger bone wine and the Drunken Immortal Brew.
- **Goods the market buys:** rainbow silk, porcelain vases and jade.
- **Materials:** silk cocoons, tea leaves, soy sauce and pine tar.

### Building plans
A building plan raises a whole building from what you carry.
1. Right-click the ground with the plan. Sparks mark where the building will stand, in front of you with its entrance facing you, and chat lists what it needs and what you lack.
2. Crouch and right-click the same spot to build.

**While it builds:**
- The building goes up a couple of dozen blocks a tick: the plan's empty spaces are cleared (soft blocks only, which drop as items), then the walls, then everything that hangs on them.
- Every block is paid for from your pack. In creative, it is free.
- Whatever you can't pay for is left out and listed at the end.
- An architect's hand saves materials (5% per rank, plus 20% with the *Yingzao Fashi*).

**Ready-made buildings:** each structure manual comes with the plan for one of 24 buildings, and you need the manual to build from the plan.

| Trade | Buildings |
|---|---|
| Architect | Road Inn, Relay Station, Village Temple ★, Granary, Siheyuan courtyard house, five-storey Pagoda |
| Mason | Village Well, Beacon Tower, Paifang archway, Zhaozhou Bridge (17 long; build it from one bank) |
| Carpenter | Mail Post, Pavilion, Arrow Tower |
| Shipbuilder | Dock ★, Ferry Landing ★, Shipyard ★, Lighthouse |
| Farmer | Irrigated Field |
| Physician | Herb Garden |
| Breeder | Stable |
| Veterinarian | Animal Pen |
| Fisherman | Fishing Hut |
| Cook | Kitchen |
| Blacksmith | Forge |

The road inn, relay station and mail post are the start of the road network's buildings.

### The architect's own plans
Architects can draw their own plans with **Lu Ban's Square**, which the Junior manual of the same name teaches you to make from two sticks and an iron ingot.
1. Right-click two corners of something you built.
2. Crouch and right-click, with a sheet of paper in your pack, to draw it into a plan. The plan keeps the way round you drew it, and is raised the same way round, facing you.
3. Rename the plan on an anvil if you like.

How large a plan you can draw depends on your architect rank: Junior 9 blocks across, Intermediate 15, Advanced 21, Master 31. Drawing gives architect experience. Anyone you give a plan to can build from it, and it keeps working in that world.

### Ships
The shipbuilder's ships, after the novel's fleet: fishing boats for two fishermen, cima boats as scouts, merchant ships, Mengchong warships and the turreted ship. They are launched like a boat (right-click on water).

| Ship | Manual | Rank | Seats | Speed | Hull | What else it does |
|---|---|---|---|---|---|---|
| Fishing Boat ★ | Fishing Boat Manual | Junior | 2 | as a boat | as a boat | fish caught from it come in a third more often |
| Cima Boat ★ | Cima Boat Manual | Intermediate | 2 | half again as fast | as a boat | the red-sailed scout |
| Merchant Ship ★ | Merchant Ship Manual | Intermediate | 3 | a little slower | 2× | a hold of 27 slots (crouch and right-click) |
| Mengchong Warship ★ | Meng Chong Warship Manufacturing Manual | Advanced | 4 | a little faster | 4× | rams: what it runs down at speed is hurt and thrown aside |
| Turreted Ship ★ | Turreted Ship Construction Manual | Master | 6 | slower | 7× | its crossbowmen shoot the nearest foe within 18 blocks; clapper poles strike foes close by. Its manual's master has Conduit Power in water |

**How they are built:**
- Every ship is built from planks and pine tar.
- The Mengchong is built from a fishing boat, and the turreted ship from a Mengchong.

**How they look:**
- Each ship is drawn larger than a boat (the turreted ship twice as large), painted in its own wood.
- Each carries what stands on its deck: a mast and sail, a rawhide deckhouse, or a two-storey red tower.

### Martial arts secret manuals
Secret manuals for players, as in the novel.

**Grades and prices:**

| Grade | How you get it |
|---|---|
| Bronze | market, 50 silver |
| Black Iron | market, 2 gold |
| Silver | market, 10 gold |
| Gold | market, 50 gold |
| King | never sold: bandit chiefs, beast kings, ruins |
| Emperor | never sold: bandit chiefs, beast kings, ruins |
| God | never sold: bandit chiefs, beast kings, ruins |
| Saint | never sold: only in the rarest ruins |

**Learning and training:**
- **Learning:** right-click a manual to learn it. King rank needs Comprehension 10, Emperor 14, God 18 and Saint 22; the enlightenment pill helps.
- **Weapon arts:** train by fighting with their weapons.
- **Cultivation methods:** train by meditating (crouch and keep still), and a little each minute.
- **Lightness skills:** train by running.
- **Training speed:** Comprehension sets how fast you learn.
- **What each level gives:** its attributes (cultivation methods raise innate attributes, as in the novel), and weapon arts add damage with their weapons. The great arts unlock a special move.
- **Your arts:** the **Arts & Manuals** tab shows your arts, their levels, training progress and what they add.

| Art | Grade | Kind | Levels | Each level | Special move |
|---|---|---|---|---|---|
| Basic Breathing Method | Bronze | cultivation | 5 | Body +1 |  |
| Grass Floating Flight | Bronze | lightness | 4 | +2.0% speed | from level 1: Light Body: falls hurt less. |
| Basic Fist Technique | Bronze | fist | 5 | +4% damage |  |
| Basic Sabre Technique | Bronze | sword/sabre | 5 | +4% damage |  |
| Basic Spear Technique | Bronze | spear | 5 | +4% damage |  |
| Basic Archery | Bronze | bow | 5 | +4% damage |  |
| Five Animals Play | Black Iron | cultivation | 6 | Body +1, Force +1 |  |
| Taoist Daoyin Exercises | Black Iron | cultivation | 6 | Comprehension +1, Intelligence +1 |  |
| Taizu Long Fist | Black Iron | fist | 7 | Force +1, +5% damage |  |
| Ladder Cloud Step | Silver | lightness | 6 | +2.5% speed | from level 1: Light Body: falls hurt far less. |
| Six Harmonies Spear | Silver | spear | 9 | Force +1, +5% damage |  |
| Yuenu Sword Technique | Silver | sword/sabre | 9 | Force +1, +5% damage | from level 5: Flurry: one cut in seven strikes twice. |
| Muscle-Tendon Changing Classic | Silver | cultivation | 9 | Body +1, Force +2 |  |
| Hundred Pace Archery | Gold | bow | 9 | Force +1, +6% damage | from level 5: Hundred Paces: every arrow flies true and strikes as a critical hit. |
| Bajiquan | King | fist / spear | 9 | Force +2, +6% damage | from level 5: Cling to the Mountain: one blow in seven lands half again as hard and hurls the foe back. |
| War Soldier Code | King | cultivation | 9 | Body +1, Comprehension +1, Force +2 | from level 9: Fighting Intent: when badly hurt, killing intent rises: Strength II and Resistance for a minute (once every five). |
| Guicang Sword Technique | Emperor | sword/sabre | 12 | Force +1, Intelligence +1, +6% damage | from level 6: Return to the Hidden: one blow in seven against you is turned aside. |
| Killing Sword Sword Manual | Emperor | sword/sabre | 7 | Force +3, +10% damage | from level 3: Seven Kills: each kill feeds your strength for ten seconds. |
| Yang Family Spear Technique | Emperor | spear | 12 | Force +2, +6% damage | from level 6: Throat Killing Spear: one thrust in eight strikes double and leaves the foe weak and slow. |
| Yellow Emperor Internal Cultivation Technique | Emperor | cultivation | 12 | Body +2, Comprehension +1, Command +1, Force +1, Intelligence +1, Politics +1 | from level 6: Golden Qi Shield: when struck, a shield of Qi absorbs the next blows (every half minute). |
| Yellow Court Internal Scripture: Qi Cultivation (needs Yellow Emperor Internal Cultivation Technique at 12) | God | cultivation | 9 | Body +4, Comprehension +6, Luck +4, Charm +2 | from level 3: Nourishing Qi: wounds close by themselves. |
| Son of Heaven Dragon Fist | God | fist | 9 | Command +1, Force +2, +8% damage | from level 5: Dragon Strike: one punch in ten bursts outward, striking every foe around. |
| Wugou Heart Sutra | Saint | cultivation | 18 | Body +4, Comprehension +2, Luck +2, Charm +2, Command +2, Force +2, Intelligence +2, Politics +2 |  |

### Beasts' skins
The beasts are repainted pixel by pixel with fur rather than flat recolours. Each skin is shaded through a palette of shadow, base, highlight and pale belly, with grain, streaks running with the hair, and grizzled guard hairs on boars, wolves and the black bear.
- **Tigers** have wandering, tapering black stripes instead of even bands.
- **Leopards** have true rosettes.
- **The black bear** is near black.
- **The snow bear** is cream-furred.
- **The wild ox** is a deep gaur brown.

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

## The novel's realm: trade, learning, the navy and the frontier
Ten things from the novel that turn a village into a realm. Each has its building, its people and its dangers.

### Commissioning buildings from the village
A building plan can be built by the village's own craftsmen instead of from your pack, as Ouyang Shuo's workers raised his halls.
1. Stand inside your village and right-click the ground with the plan. Chat lists what the village would need: timber and stone from the stores, and roughly how many hours of work.
2. Right-click the same spot again to commission it. Crouch instead to build it yourself, as before.

**What the village needs:**
- You must be its Lord.
- The village must have a carpenter or a mason.
- It must have the timber and stone in its stores.

The builders then raise it like any other village building, block by block.

### Salt fields, copper and coal
| Building | Grade | Work | Yield per worker per day |
|---|---|---|---|
| Salt Field ★ | 2 | Salt Making (by the water) | about 15 salt |
| Copper Mine | 2 | Copper Mining | about 2 copper |
| Coal Mine | 2 | Coal Mining | about 4 coal |

Fishermen and grocers make better salt makers, and miners better miners. Salt sells at 20 copper a unit, the novel's price.

### Docks, shipyards and the navy
**The Dock ★** (Grade 2) is a pier out into the water, built from the shore with part of it over water.
- Fishing boats kept in the storehouse each take two fishermen out. A fisherman brings in about 25 fish a day, more for a skilled one.
- The dock holds ten sailors.

**The Shipyard ★** (Grade 2) is run by a **shipbuilder**. In its window, choose what it builds:

| Choice | Wood | Also needs | Time |
|---|---|---|---|
| Fishing boats | 50 | — | one a day |
| Cima boats | 80 | — | two days |
| Merchant ships | 300 | 2 pine tar | four days |
| Mengchong warships | 400 | 4 pine tar, *Warship Construction* manual | three days |
| Turreted ships | 1,200 | 10 pine tar, *Warship Construction* manual | ten days |

- A better shipbuilder, or a summoned figure, works faster.
- The window shows how far along the ship on the slipway is.
- Finished ships go into the storehouse.

**Sailors and marines:** enlist residents on the Duty tab of their card, for 3,000 copper each.
- Sailors fight like soldiers and drill at the dock every day.
- They don't count against the militia.
- Places: ten for each dock, twenty for each shipyard, and ten more for each summoned admiral (Zheng He).
- A sailor unit, as in the novel, is one turreted ship, four Mengchong warships, twelve cima boats and forty sailors.

The turreted ship, the Mengchong and the merchant ship are now drawn as real multi-deck ships, block by block: a deckhouse, masts and sails, and the turreted ship's three-storey tower.

### Temples and Taoist priests
The **Village Temple** (Grade 1) and the great **Temple** are kept by a Taoist priest. A temple with no priest sends for a wandering one, who comes for two gold from the treasury. In the temple's window, choose what the priest does:

| Service | Effect |
|---|---|
| Blessings | People's Thoughts +3 (+1 for each rank of the priest) |
| Warding | Security +4; raiders come 10–20% weaker |
| Praying for rain | Crops yield 10% more (+3% a rank) |
| Refining elixirs | Herbs become Clear Heart Pills and Healing Pills |
| Drawing talismans | Linen becomes Guardian Charms and Spring Weathering Talismans |

The temple also takes three apprentice priests.

### The Academy and the imperial examination
**The Academy** (Grade 3, needs an Intermediate scholar) is a great school of lecture halls and a library.
- It takes ten students and teaches what the private school teaches, half again as well.
- It adds *Examination Prep*, the classics for the examination.

**The imperial examination** is held once a year, in the eighth month, for the village's scholars and civil servants:

| Examination | Degree | Needs | Gain |
|---|---|---|---|
| County | Xiucai | a private school or an academy | +2 Intelligence and Politics |
| Provincial | Juren (from Xiucai) | an academy | +3; rises to Intermediate |
| Metropolitan and palace | Jinshi (from Juren) | an academy | +5; rises to Advanced |
| — | Zhuangyuan (the year's best Jinshi, sometimes) | — | Gold talent; the Lord gains 500 merit and a King-rank **Summoning Token** |

- Cleverer candidates pass more often.
- Studying at the academy helps, *Examination Prep* most of all, and so do summoned scholars.
- Degree holders raise the village's taxes (Juren +1%, Jinshi +3%, Zhuangyuan +5%, up to +30%).
- Each Jinshi adds a point of People's Thoughts.

### Chambers of Commerce and caravans
**The Chamber of Commerce** (Grade 2) is run by a **merchant**. In its window, choose one of the nine capitals to trade with.

**How a caravan works:**
- It loads up to half of each trade good in the storehouse: silk, linen, wine, tools, pottery, salt, herbs, leather, copper and coal.
- It goes there and back in days that grow with the distance.
- It sells for the market price plus a fifth, plus up to 120% more for the distance.
- A better merchant, or a summoned merchant (Fan Li), sells for more.
- The proceeds, less the system's 10% tax, go to the treasury.

**By sea:** with a dock and a merchant ship in the storehouse, caravans to Jingdu, Jianye and Quanzhou go by sea. They are 40% faster, carry three times the cargo, and only water bandits rob them. Mengchong warships see the water bandits off.

**The roads are dangerous:**
- There is a 15% chance that bandits rob a caravan.
- Every road inn and relay station the village has commissioned lowers it by 3%.
- So do militia to escort the caravan and strategists to plan the route.

The chamber's window shows where the caravan is, when it returns, and the odds of the next trip.

### The summoned figures' gifts and arts
A summoned figure with Loyalty 60 or more lends the village the gift of their kind:

| Kind | Gift (each) |
|---|---|
| General | +5 militia places (with a military camp or barracks) |
| Admiral | +10 sailors; better fishing and sea trade |
| Strategist | Security +3; raiders 10% weaker; safer caravans |
| Minister | +5% tax |
| Scholar | more pass the examination |
| Engineer, Carpenter | building 15% faster; siege engines hit 15% harder |
| Doctor | People's Thoughts +2 |
| Smith, Weaver | workshops make 5% more |
| Agronomist | crops +10% |
| Merchant | caravans earn 20% more |

**Their own arts:** at Loyalty 90, a figure will teach you their art once. Press *Teach their art* on their card, and you receive its secret manual.

| Figure | Art |
|---|---|
| Sun Wu, Sun Bin, Han Xin, Zhuge Liang, Jiang Ziya, Wu Qi | *Sun Tzu's Art of War* (Emperor): Command, Intelligence and Politics |
| Xiang Yu, Lu Bu | *Overlord's Halberd* (Emperor): one stroke in six sweeps every foe around |
| Guan Yu | *Green Dragon Crescent Blade Technique* (Emperor): a cleave that staggers |
| Bai Qi, Wang Jian, Meng Tian, Zhang Han | the Qin army's *Killing Sword* |
| Zhao Yun, Ma Chao, Zhang Fei | *Six Harmonies Spear* |
| Hua Tuo | *Five Animals Play* |
| Huang Zhong, Taishi Ci | *Hundred Pace Archery* |
| Lu Ban | *Ladder Cloud Step* |
| Other generals; other warriors; the learned | *War Soldier Code*; *Bajiquan*; *Daoyin* |

**More Summoning Tokens:**
- **Titles:** one each at Third Class Viscount and Earl (King), Marquis (Emperor), Duke (Divine) and Son of Heaven (Saint).
- **Den kings:** a chance from each slain king, best from the White Tiger King.
- **Chiefs:** a chance from barbarian chieftains and water bandit chiefs.
- **The examination:** every Zhuangyuan brings one.

### Water bandits
A village by the water, with a dock or on the coast, may find water bandits building a fort on stilts out on the water a few hundred blocks away. The fort has a palisade, a loot hut and a watchtower.

**While the fort stands:**
- Fishing brings in 40% less.
- Security falls by 5.
- Merchant ships are robbed far more often.
- Now and then the bandits raid the docks for fish and salt, unless the navy beats them off (ten sailors and a Mengchong warship).

**Breaking the fort:** kill the chief.
- You gain 120 merit.
- The chief may carry the *Warship Construction* manual or a Summoning Token.
- Their chest holds silver, fish, pine tar and sometimes a blueprint.

New bandits may take the water again after a couple of months.

### Barbarian raids: the county trial
As in the novel, the barbarian raiders come once: when a town applies to become a county. See *The county trial* below.

**The chieftain:**
- Slaying him is worth 150 merit.
- He carries gold, and may carry the **Hell Slaughterer True Manual** (King grade). It gives +3 Force, +1 Body, −1 Charm and −1 Politics a level. Its *Slaughter* makes blows bite deeper into the wounded, and every kill heals you and lends you strength.
- Now and then he carries a Summoning Token.

### Siege engines
The Carpenter's Intermediate **Siege Engine Construction Manual** ★ teaches:

| Item | Recipe |
|---|---|
| Bed Crossbow | crossbow, bow, sinew, planks and logs |
| Trebuchet | logs, planks, stone and a lead |
| 4 Heavy Bolts | iron ingot, stick and feather |
| 2 Stone Shot | 4 cobblestone |
| Siege Ladder | sticks and planks |

The weapons workshop can make all of them too. The engines themselves need the *Siege Engines* manual from the market.

**Bed crossbow:** three bows on a heavy frame.
- Place it facing out over the wall, and right-click with heavy bolts to load it (16).
- Inside a territory it shoots on its own every three seconds at bandits, raiders and beasts within 40 blocks in front of it.
- The bolt flies flat and fast and goes through several men.

**Trebuchet:**
- It holds eight stone shot and lobs one every eight seconds at foes 12 to 64 blocks off.
- The shot strikes everyone where it lands.
- Outside any territory it smashes timber, fences and wool, so it can break a water bandits' palisade or a bandit camp.

**Firing by hand:** crouch and right-click either engine to shoot where you look.

**Siege ladder:** right-click the side of a wall, and a ladder runs up it, as high as sixteen blocks.

## The Lianzhou Basin
The China world's Lianzhou Basin is drawn as the novel describes it, not as the real coastal plain behind Beihai:
- **The ring:** mountains close the basin in on every side, about 2,200 blocks across. They are highest in the north-east, where the basin meets Lingnan and the mountain barbarians live.
- **The way north:** the **Zhennan Pass**, a saddle through the northern ring.
- **The way to the sea:** **the canyon** in the south, "the only ocean opening into the Lianzhou Basin", running down to Beihai Bay.
- **The rivers:** the **Friendship River** runs from the **West Lake** in the north-west, and the **Qiushui River** from the **East Lake** in the north-east. Both cross the basin floor and meet at the head of the canyon, then flow through it to the sea.
- **Shanhai:** the starting place stands where the novel puts the village, on the west side of the canyon's entrance with the Friendship River to its north.

On the world map and the starting-place map, zoom in on the basin to see its rivers, lakes, pass and canyon named.

## The council
Officials don't just raise a number: they run their part of the territory and bring the Lord their proposals, as Fan Zhongyan and Shi Jun do in the novel. Each day every official looks at their charge and may propose something. The abler they are (by the stat their office uses), the more often they speak up and the bolder their plans.

| Official | Proposes |
|---|---|
| Steward | housing when beds run short; putting idle people to the work most needed |
| Agriculture | more farmers, new farmland or buying grain when the granary runs low; selling it when it overflows |
| Works | the next building the upgrade needs (when the village can afford it); more loggers or quarrymen; the upgrade itself once every condition is met |
| Treasurer | selling surplus goods when the treasury runs dry |
| Military | enlisting militia when the beastwave or the raiders are coming; barracks |
| Security | paid patrols when Security falls |
| Rites | a feast when spirits are low; a temple |
| Commerce | the best route for the next caravan; a Chamber of Commerce |
| Internal Affairs | housing for waiting refugees |
| Armory, Transport | the Arsenal's and the shipyard's work |
| Procuratorate, Auditing | a monthly audit that recovers money for the treasury |
| Meritorious Service | rewards for disgruntled soldiers |

**Answering a proposal:**
- Proposals come to you in chat with **[Approve]** and **[Dismiss]**, and wait on the Lord's Manor's new **Council** tab. They lapse after three days.
- Approved, the official carries it out. For a new building, the builders find open ground facing the manor and lay it out there.
- Dismissing an official's advice costs them a little loyalty.

**Letting officials decide:** the Council tab's switch lets them handle routine matters on their own: moving workers, buying and selling, militia, patrols, feasts and audits. They report what they did under **Lately**. New buildings and the upgrade always wait for you.

## Phase 3: the town
The novel's chapters 50 to 155: Shanhai Village becomes Shanhai Town, grows through three grades, builds a city wall, an army and a government, and wins its county upgrade against a raider attack.

### Becoming a town, and its grades
Upgrade from the Lord's Manor when every condition is met, as the system announced them to Ouyang Shuo:

| Upgrade | Population | Lord's title | Also needs |
|---|---|---|---|
| Grade 3 Village → Grade 1 Town | 1,000 | Third-class Viscount | every Grade 3 village building; all four indexes 30 or more |
| Grade 1 → Grade 2 Town | 2,000 | Second-class Viscount | the Grade 1 town buildings |
| Grade 2 → Grade 3 Town | 5,000 | First-class Viscount | the Grade 2 town buildings, including the Alchemy Workshop (which needs an alchemist) |
| Grade 3 Town → Grade 1 County | 10,000 | Third-class Earl | the Grade 3 town buildings; all four indexes 45 or more; then the **county trial** |

**Each upgrade brings:**
- Merit, more for being among the first in the world.
- The Lord picks one of three random buildings of the new grade, free.
- The Lord's Manor becomes the town-level manor, with a lookout pavilion on the roof and stone guardian lions at the steps.

**Town bonuses:**
- Production +20% at Grade 1 and +25% from Grade 2.
- Two, three and four times the migrants at Grades 1, 2 and 3, and a quarter more for each Inn and Relay Station.

### Town buildings
★ marks a building needed for the next upgrade.

| Grade | Building | Run by | What it does |
|---|---|---|---|
| Town 1 | City Wall ★ | — | A stone wall outside the old palisade, four blocks higher, with crenellations and a moat bridged at the gates. Security +20 |
| Town 1 | Brick Kiln ★ | potter | clay into bricks (stone) or roof tiles |
| Town 1 | Cemetery ★ | — | a walled cemetery with a memorial shrine; People's Thoughts +6 |
| Town 1 | Orchard ★ | — | farming |
| Town 1 | Public Toilet ★ | — | People's Thoughts +3 |
| Town 1 | Arsenal ★ | blacksmith | weapons stock, spears, crossbows, heavy bolts; needed for crossbowmen |
| Town 1 | Government Office ★ | civil servant | opens the town's departments |
| Town 1 | Residential Quarter | — | 400 beds |
| Town 1 | Recruitment Hall | civil servant | one Advanced specialist a month, for ten gold |
| Town 1 | City Defense Camp | officer | fifty soldiers of the standing army |
| Town 2 | Relay Station ★ | stableman | migrants, safer caravans, fees |
| Town 2 | Vegetable Field ★, Medicine Garden ★ | — | food; herbs |
| Town 2 | Pharmacy ★ | doctor | pills and remedies |
| Town 2 | Textile Mill ★ | tailor | eight linen a day, or silk |
| Town 2 | Bank ★ | merchant | interest on the treasury, up to 5,000 copper a day |
| Town 2 | Alchemy Workshop ★ | alchemist | jewelry, pills, refined iron |
| Town 3 | Inn ★ | cook | migrants, safer caravans, income, festival trade |
| Town 3 | Pawnshop ★, Farmers' Market ★ | merchant; — | daily income |
| Town 3 | Jewelry Shop ★ | alchemist | jewelry from copper |
| Town 3 | Garment Workshop ★ | tailor | garments from linen and silk |
| Town 3 | Vinegar ★, Soy Sauce ★ and Tofu ★ Workshops | cook | condiments; tofu feeds the town |
| Town 3 | Large Brewery ★ | winemaker | wine by the vat |
| Town 3 | Auction House | merchant | opens the Auction |

Build the City Wall from the Build tab. It rings the town ten blocks outside the palisade (or at the town's own ring), through the same gates.

### The county trial
In the novel, Shanhai Town applied to become a county and the system answered: *"3 days later there will be a large scale raider attack on Shanhai Town. Please get ready."*
1. With every condition for a county met, press Upgrade. Three days later the barbarian raiders attack.
2. They come in five waves over twelve hours: a scouting wave at three gates (north, west and east), then heavier ones, and last the elites and their chieftains, all at one gate.
3. **Hold:** if the manor holds for the twelve hours, or every raider is dead, the town has passed. The Lord gains 1,000 merit, and the town becomes a Grade 1 County.
4. **Fall:** if five raiders reach the manor at once, they break in and plunder a third of the treasury, food and goods. The town may apply again after seven days.

A trial nobody is near is fought unseen: the city wall, towers, soldiers, sailors and militia against the raiders' numbers. The Realm tab shows the trial's progress.

### Departments and the cabinet
The Government Office opens eight new departments, the novel's divisions, held from a resident's card like the village offices:

| Opens | Department | Effect |
|---|---|---|
| Town 1 | Security Division | Security |
| Town 1 | Meritorious Service Division | the army drills faster |
| Town 1 | Armory Division | production |
| Town 2 | Internal Affairs Department | refugees taken in; People's Thoughts |
| Town 2 | Bow and Crossbow Division | archers and crossbowmen fight harder |
| Town 2 | Transport Division | the economy |
| Town 3 | Procuratorate | taxes |
| Town 3 | Auditing Division | bank interest |

With eight offices held and a Government Office, the town has a **cabinet**: every index +5.

### The standing army
Enlist residents from the Duty tab of their card.

| Arm | Cost | Pay a day | Needs | Fights |
|---|---|---|---|---|
| Infantry | 2,000 | 20 | — | sword |
| Spearmen | 2,500 | 25 | — | spear |
| Archers | 3,000 | 30 | — | shoot from range |
| Crossbowmen | 4,000 | 40 | Arsenal | heavier bolts from range |
| Cavalry | 10,000 | 80 | Stable | fast and strong |
| Mountain Warriors | 3,000 | 30 | a mountain barbarian | elite infantry |

**Places:** fifty at each City Defense Camp, twenty at each Barracks, ten for each summoned general, and more under a good Military Affairs head.

**Drill and rank:** soldiers drill with the militia, a fifth faster at a camp. They rise from Soldier to Squad Leader, Platoon Leader, Company Commander and Battalion Commander.

**Pay:** comes from the treasury each day. Unpaid soldiers lose loyalty.

### People
- **Recruitment Hall:** once a month, for ten gold, an Advanced (one time in five a Master) specialist of the trade chosen in its window. It is the surest way to an alchemist.
- **Refugees:** a column comes every month to a town whose People's Thoughts are 50 or more. They camp outside and move in as beds allow, ten a day plus a fifth of the Internal Affairs head's Politics. They give up if People's Thoughts fall below 40.
- **Visitors:** a town whose Lord has 300 reputation draws talents. Some are wandering Silver and Gold talents, like the novel's Pei Donglai and Tian Wenjing. One time in three a visitor is a historical figure not yet summoned in the world: King rank, or Emperor rank from 3,000 reputation. They stay a month. Take them into service from the Realm tab: two gold for a talent, twenty or fifty for a figure.
- **Affiliated villages:** one for each grade of the town, founded from the Realm tab. It costs ten gold, 500 wood, 300 stone and fifty settlers. It grows by itself to 200, 500 and 1,000 people, rising a grade each time. Each day it sends a copper and half a grain a head, and takes the town's people when the town is over its limit.

### Festivals
Each is held on the first day of its month if the treasury can pay (five copper a head in a town, two in a village):
- the Spring Festival;
- the Lantern Festival;
- the Qingming Sacrificial Ritual;
- the Dragon Boat Festival (with a dock);
- the Mid-Autumn Festival.

Each festival raises People's Thoughts by 5 for the month (7 with an ancestral hall, cemetery or temple). It also gives the Lord reputation, and in a town, visitors spend more than the festival cost.

### The Auction
An Auction House opens the market's **Auction** platform. Five lots are open at a time for the whole world, each for a month:
- rare town blueprints;
- Gold-grade arms and secret manuals;
- technical manuals;
- warship and siege engine plans;
- now and then a Summoning Token.

**Bidding:**
- Each bid is a tenth over the last, paid at once from the treasury, and refunded if you are outbid.
- The world's rival bidders (Di Chen, Snow-War Rose, the great families) raise the price each day, up to what the lot is worth to them.
- When a lot closes, the prize goes to the winner's storehouse.

### Mountain barbarians, fortresses and ruins
**Mountain barbarian tribes:** around a town, a tribe may settle in the wilds: a log palisade, hide huts, a totem and a fire pit, held by warriors and their chieftain.
- They raid the fields unless thirty soldiers or militia guard them.
- Kill the chieftain (150 merit) and the tribe submits: twenty to forty of its people come down to the refugee camp.
- They are hardy hunters and farmers, and can be enlisted as Mountain Warriors.

**Bandit fortresses:** a great stronghold with stone walls, corner towers and a keep, held by twenty tier 4 and 5 bandits and their chief.

**The ruins of an ancient city:** broken walls and toppled columns, watched over by armoured Ancient Guardians.

Both hold two treasure chests: gold, Gold- and Silver-grade arms and manuals, town blueprints, and now and then a Summoning Token.

### The Realm tab
The Lord's Manor has a new **Realm** tab with the town's affairs:
- the county trial;
- the army by arm;
- the offices and cabinet;
- refugees waiting;
- business income and this month's festival;
- visitors, with a button to take them into service;
- affiliated villages, with a button to found one;
- any mountain tribe nearby.

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
   - **Still to come:** special markets (ship blueprints, graded weapons and armour), more hidden buildings, and more figures.
3. **Town grades 1–3** (done): the council of officials, the novel's Lianzhou Basin, the town's conditions and buildings, the city wall, the county trial, departments and the cabinet, the standing army, the Recruitment Hall, refugees, festivals, visitors, affiliated villages, the Auction, mountain barbarian tribes, bandit fortresses and ancient ruins, ships and the navy, water bandits.
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
- **Ships and weapons:** more kinds of boats and warships (siege engines done).

## Building from source
Java 17, then `./gradlew build` (jar in `build/libs`). Textures, models and the language file are generated by `python3 tools/gen_assets.py`. Every build runs in-game tests on a real server: choosing a role, titles, levels and points, every skill, every work rank, the full Village Creation quest, token grading, bandit strength and drops, loot items, founding a village with its settlers, dimension travel and syncing.

Original code, art and text. Inspired by the structure of *The World Online*; not affiliated with the novel or its author.
