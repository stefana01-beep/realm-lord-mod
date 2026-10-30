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
1. **Gather supplies:** 64 logs, 64 stone and 32 food, handed over in the Quest tab.
2. **Free the settlers:** bandits hold them captive in a camp 150–220 blocks away. Defeat the bandits and their chief.
3. **Claim your token** in the Quest tab.

The token's grade depends on how well you did:
- **Gold:** finished within 1.5 days, never died, and found the bandits' buried cache. **Only one in the whole world;** anyone after that gets Silver.
- **Silver:** the same, after the Gold token is taken.
- **Black Iron:** finished within 3 days.
- **Bronze:** finished within 7 days.

Run out of time or abandon the quest and you lose the right to found a village, continuing as an Adventurer. (Hint: the chief's ledger is worth reading.)

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
- Quest time limits and an experience multiplier.

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
Java 17, then `./gradlew build` (jar in `build/libs`). Textures, models and the language file are generated by `python3 tools/gen_assets.py`. Every build runs in-game tests on a real server: choosing a role, titles, levels and points, every skill, every work rank, the full Village Creation quest, founding a village, dimension travel and syncing.

Original code, art and text. Inspired by the structure of *The World Online*; not affiliated with the novel or its author.
