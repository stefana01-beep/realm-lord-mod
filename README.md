# Realm Lord

A standalone Forge mod for **Minecraft 1.20.1** about ruling a territory, inspired by the structure of the web novel *The World Online*. Choose to be a **Lord** or an **Adventurer**. Lords win a Village Creation Token through a demanding quest, found a village, and raise it grade by grade into a kingdom while earning noble titles through merit. Adventurers roam free with combat skills or a trade.

Requires **Forge 47.x** for 1.20.1. No other mods needed. Runs on its own or alongside Gu Path.

**Download:** every push to `main` builds a jar and runs the in-game tests. Grab the newest from the [Releases page](../../releases).

**Status:** Phase 1 of 7 (Lords, Adventurers, occupations, titles, the Village Creation quest and founding a village). See the roadmap below.

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

- **Lord:** rules a territory. Takes the Village Creation quest, earns merit and titles, and picks a **sub-profession** (any combat or work occupation). Lords start with extra Command and Politics.
- **Adventurer:** roams freely and learns a quarter faster. Picks one occupation, either combat or work.

### Combat occupations
Each has a character level (up to 100) and three skills, learned at levels 1, 10 and 25.

| Occupation | Leans on | Skills |
|---|---|---|
| Strategist | Intelligence | Fire Stratagem, Confusion Array, Battle Insight |
| General | Command, Force | War Cry, Cavalry Charge, Unyielding Will |
| Chivalrous Expert | Force | Swift Sword, Lightness Skill, Hundred Strikes |
| Alchemist | Intelligence | Toxic Flask, Vitality Elixir, Thunder Pill |
| Healer | Intelligence, Politics | Rejuvenate, Acupuncture Needle, Purify |
| Bailiff | Force, Command | Iron Shackles, Arrest, Patrol Sense |

Combat experience comes from defeating monsters. Dying costs a fifth of a level's experience (never a whole level).

### Work occupations
Blacksmith, Tailor, Physician, Architect, Breeder, Fisherman, Farmer, Veterinarian, Winemaker, Cook, Carpenter, Mason and Shipbuilder. As in the novel, a work Adventurer has **no character level**, only a craft level from 1 to 10: **Junior** (1–3), **Intermediate** (4–6), **Advanced** (7–9) and **Master** (10). Work occupations never lose experience on death. Each trade learns by doing its work and gets a bonus that grows with rank: smiths forge Unbreaking gear, farmers reap extra, cooks are fed more, carpenters and masons cut faster, and so on.

### Attributes
- **Command:** troop strength, troop count, recruiting and response time (from Phase 4). Already makes your tamed animals hit harder.
- **Force:** personal might. Raises health and melee damage, and strengthens physical skills.
- **Intelligence:** longer buffs and controls, shorter cooldowns, and later troop vision and weapon development.
- **Politics:** policies on markets and farms, and diplomacy (from Phase 2).

Lords get 3 points per level, Adventurers 4, and every craft level gives 3.

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

As in the novel, the reward is the **Village Creation Token**, a **Summoning Token** (calls a talent once historical figures arrive in Phase 3), a **Resources Bag** (logs, stone, wheat, bread and gold, more for higher grades) and **gold coins** (20 / 40 / 80 / 150).

**Bandits** drop gold coins and sometimes Bronze-grade gear. The **chief** drops his Black Iron-grade sword, a pile of gold and his ledger. The ledger points to the **chief's buried treasure** (optional): gold, two building blueprints, a Silver-grade weapon and a Black Iron-grade piece of armour. The camp's barrels hold stolen grain and supplies.

Freed settlers wait by the camp and move into your village when you found it, each with their own name.

Run out of time or abandon the quest and you lose the right to found a village, continuing as an Adventurer.

### Loot
- **Gold coins:** the world's currency, as in the novel. Phase 2's market uses them.
- **Graded equipment:** Bronze, Black Iron, Silver and Gold grades, each with more damage (weapons) or armour and health (armour).
- **Blueprints:** many of the novel's buildings need one (Dining Hall, Small Residential Courtyard, Blacksmith Workshop and more). Keep them for Phase 2.

## Founding your village
Use the token on the ground. A Lord's Manor is raised there, and your territory begins as a **Grade 1 Village** with a 32-block border. The first village founded in the world earns 1,100 merit, the second 1,000 and so on down to 100; the first ten are announced to everyone. Rename the token in an anvil first to name your village. The territory keeps its token grade forever. Use the manor seal to open your territory panel.

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

## Server settings
`config/realmlord-common.toml`:
- `lordRestriction` (default **off**): the novel's rule that Lords cannot leave their territory except for war. Other dimensions are exempt.
- `ticksPerNovelHour` (default 1200, one real minute): scales construction times from Phase 2 on.
- `daysToComplete` (10), `goldWithinDays` (3), `silverWithinDays` (5), `blackIronWithinDays` (7.5), and an experience multiplier.

## Admin commands (op)
`/realm` · `/realm info [player]` · `/realm territories` · `/realm role` (anyone) · `/realm reset [player]` · `/realm level <n>` · `/realm worklevel <n>` · `/realm points <n>` · `/realm merit add|set <n>` · `/realm token <grade>` · `/realm quest start|supplies|clear`

## Roadmap
1. **Lord and Adventurer cores** (this release)
2. Village grades 1–3: every building from the novel with its costs, blueprints and specialists, resource jobs, the farming calendar, the market, militia, first dungeons and beast waves
3. Historical generals and ministers, departments and the four indexes
4. Town grades 1–3, troops, formations, raids, more dungeons
5. Battlefield maps, rival lords and war
6. County and Prefecture, secret conditions, hidden buildings
7. Capital and Dynasty

## Building from source
Java 17, then `./gradlew build` (jar in `build/libs`). Textures, models and the language file are generated by `python3 tools/gen_assets.py`. Every build runs in-game tests on a real server: choosing a role, titles, levels and points, every skill, every work rank, the full Village Creation quest, token grading, bandit strength and drops, loot items, founding a village with its settlers, dimension travel and syncing.

Original code, art and text. Inspired by the structure of *The World Online*; not affiliated with the novel or its author.
