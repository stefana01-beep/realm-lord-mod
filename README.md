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
| **R** | Use the selected skill |
| **Y** | Next skill (sneak + Y: previous) |

Keys can be rebound under Options → Controls → Realm Lord.

## Choosing your path
When you first join, choose a game mode. The choice is permanent.

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
2. **Free the settlers:** bandits hold them captive in a camp 150–220 blocks away: four swordsmen, two archers and their chief, all looking like players. Defeat them all.
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

Freed settlers wait by the camp and move into your village when you found it, each with their own name.

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

## Running your village (Phase 2)
**Right-click the Lord's Manor Seal** to open the Manor screen. It has four tabs:

- **Overview:** the novel's territory panel.
  - Name and grade, People's Thoughts, Security, population against the cap and housing, and immigrants per day.
  - The Gaia calendar, flagged in sowing season.
  - The treasury (with Withdraw) and taxes.
  - The four stores, each with its daily change.
  - The political, economic, cultural and military indexes.
  - What the next upgrade still needs, and the Upgrade button.
- **Construction:** every building of your grade and the next, with its wood, stone, time, blueprint and specialist, and how many the upgrade needs. Click **Place** to put it down on the map, or choose to place it in the world: an outline follows your crosshair, green where it fits and red where it doesn't. **Right-click** places it, **sneak + right-click** turns it, **left-click** cancels. The gold edge marks the door side. The Simple Fence has no placement; it rings your whole border.
- **People:** assign residents to Construction, Logging, Quarrying, Farming, Mining and Administration with the +/- buttons; the best-suited idle residents go first. Open any talent's card from the list.
- **Market:** the novel's Basic Market. Buy and sell food (10 copper a unit), wood (20), stone (30) and iron ore (100) for your stores, with a 20% tax on sales. Building blueprints cost 2 gold (Grade 1), 5 gold (Grade 2) and 10 gold (Grade 3).

- **Map:** a top-down map of your territory, drawn like a vanilla map, showing your border, the Lord's Manor, every building and site, and you. Point at a building to see what it is. Choosing **Place** in Construction opens this map with the building's footprint following your mouse: green where it fits, red where it doesn't, with the door side marked. Click to place, right-click to turn, or switch to placing it in the world. You can also open the map anywhere from the **Territory map** button on your character panel (J).

**Construction.** Starting a building takes its wood and stone from your stores, its blueprint from your inventory, and needs a resident of the right trade where the novel requires one (a blacksmith for the Blacksmith Workshop, an officer for the Military Camp, and so on). The ground is levelled and the building rises block by block as your builders work. Five builders take the novel's listed time (12 hours for a Dining Hall, one real minute per hour by default); fewer take longer, and carpenters, masons and Master Builders work faster. All builders work on the oldest site first. When a workshop is finished, a resident of its trade moves in to run it.

**Buildings.** Every Village Grade 1–3 building from the novel:
- **Grade 1:** Simple Fence, Dining Hall, Bathroom, Small Residential Courtyard (houses 20), Blacksmith Workshop, Grocery Shop, Logging Field, Quarry, Farmland and Military Camp.
- **Grade 2:** Ancestral Hall, Clinic, Private School, Martial Arts Dojo, Granary, Tailor Workshop, Mining Field, Arrow Tower and Stone Outpost.
- **Grade 3:** Temple, Weapons Workshop, Pottery Workshop, Mill, Mulberry Shed, Wine Workshop, Sentry Post and Stable.

Each has its own look, in a style of stone plinths, red posts, white walls and dark tiled roofs.

**Residents.** Your village begins as the novel's did, with ten refugees: a village chief (a Bronze civil servant), two fruit farmers, a carpenter, a mason and five farmers. Settlers you freed from the bandits join them.
- **Immigrants:** about 10 a day (more with a better token and happier people), but only into free housing (the manor holds 10, each courtyard 20) and up to your grade's limit (200, 500, 1,000).
- **Talents:** some immigrants are special talents (blacksmiths, grocers, tailors, doctors, scholars, martial artists, potters, winemakers, cooks, hunters, fishermen, miners, stablemen, civil servants and officers). They're graded Bronze, Black Iron or Silver, with better stats and a specialty.
- **Their card:** right-click a resident to open it. **Card** shows their grade, occupation and rank, loyalty, Command, Force, Intelligence, Politics, specialty and an evaluation. **Work** (Lord only) sets their job or the workshop they run. **Trade** lets anyone buy and sell with them, priced in copper. Craftsmen sell their best goods only while they work in their workshop; a visitor's purchases go into the village treasury.
- **Who you see:** up to 24 residents walk around the village at once (talents first), working at their workplace by day and gathering at the manor by night. The rest live on in the records.

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
`/realm` · `/realm info [player]` · `/realm territories` · `/realm role` (anyone) · `/realm reset [player]` · `/realm level <n>` · `/realm worklevel <n>` · `/realm points <n>` · `/realm merit add|set <n>` · `/realm token <grade>` · `/realm quest start|supplies|clear`

## Roadmap
1. **Lord and Adventurer cores** (done)
2. **Village grades 1–3** (2a done: every building from the novel with its costs, blueprints and specialists, placement and construction, residents with jobs, talents and trade, resource jobs, the farming calendar, the market and upgrades). 2b next: militia, beast waves and the first dungeons
3. Historical generals and ministers, departments and the four indexes
4. Town grades 1–3, troops, formations, raids, more dungeons
5. Battlefield maps, rival lords and war
6. County and Prefecture, secret conditions, hidden buildings
7. Capital and Dynasty

## Building from source
Java 17, then `./gradlew build` (jar in `build/libs`). Textures, models and the language file are generated by `python3 tools/gen_assets.py`. Every build runs in-game tests on a real server: choosing a role, titles, levels and points, every skill, every work rank, the full Village Creation quest, token grading, bandit strength and drops, loot items, founding a village with its settlers, dimension travel and syncing.

Original code, art and text. Inspired by the structure of *The World Online*; not affiliated with the novel or its author.
