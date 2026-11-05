#!/usr/bin/env python3
"""Generates Realm Lord's language file, item/block models, blockstates and textures.

Run from the repository root:  python3 tools/gen_assets.py
"""
import json
import math
import os
import random

from PIL import Image

import gen_skins
import lang_village

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "realmlord")
DATA = os.path.join(ROOT, "data", "realmlord")


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# --------------------------------------------------------------------------- language

GRADES = {"bronze": "Bronze", "black_iron": "Black Iron", "silver": "Silver", "gold": "Gold"}

TITLES = [
    ("none", "No Title", "Commoner"),
    ("baron_3", "Third Class Baron", "Baron III"),
    ("baron_2", "Second Class Baron", "Baron II"),
    ("baron_1", "First Class Baron", "Baron I"),
    ("viscount_3", "Third Class Viscount", "Viscount III"),
    ("viscount_2", "Second Class Viscount", "Viscount II"),
    ("viscount_1", "First Class Viscount", "Viscount I"),
    ("earl_3", "Third Class Earl", "Earl III"),
    ("earl_2", "Second Class Earl", "Earl II"),
    ("earl_1", "First Class Earl", "Earl I"),
    ("marquis_3", "Third Class Marquis", "Marquis III"),
    ("marquis_2", "Second Class Marquis", "Marquis II"),
    ("marquis_1", "First Class Marquis", "Marquis I"),
    ("duke", "Duke", "Duke"),
    ("prince", "Prince", "Prince"),
    ("son_of_heaven", "Son of Heaven", "Son of Heaven"),
]

CLASSES = {
    "strategist": ("Strategist",
                   "A master of stratagems who wins battles before they begin. Skills: Fire Stratagem, Confusion Array, Battle Insight."),
    "general": ("General",
                "A commander who leads from the front. Skills: War Cry, Cavalry Charge, Unyielding Will."),
    "chivalrous_expert": ("Chivalrous Expert",
                          "A wandering swordsman of the martial world, swift and deadly alone. Skills: Swift Sword, Lightness Skill, Hundred Strikes."),
    "priest": ("Priest",
               "A Taoist priest versed in the classics, talismans and elixirs, who later presides over temples. Skills: Thunder Talisman, Golden Elixir, Exorcism Seal."),
    "healer": ("Healer",
               "A physician of the battlefield who mends allies and pins foes with needles. Skills: Rejuvenate, Acupuncture Needle, Purify."),
    "bailiff": ("Bailiff",
                "A constable who hunts down outlaws and drags them to justice. Skills: Iron Shackles, Arrest, Patrol Sense."),
}

WORK = {
    "blacksmith": ("Blacksmith", "Learns by forging metal weapons, tools and armour, and smelting ingots. From Intermediate rank on, gear you forge comes out with Unbreaking."),
    "tailor": ("Tailor", "Learns by making leather armour, wool, carpets, beds and banners. From Intermediate rank on, leather armour you sew carries Protection."),
    "physician": ("Physician", "Learns by brewing potions. All healing you receive is stronger, and higher ranks sometimes brew a bonus potion."),
    "architect": ("Architect", "Learns by building. Reach grows with rank, and the Architect will matter for construction in your territory."),
    "breeder": ("Breeder", "Learns by breeding animals. Higher ranks are blessed with twins more and more often."),
    "fisherman": ("Fisherman", "Learns by fishing. Higher ranks often pull up a second catch."),
    "farmer": ("Farmer", "Learns by tilling and harvesting ripe crops. Higher ranks reap extra harvest."),
    "veterinarian": ("Veterinarian", "Learns by feeding, breeding and taming animals. Animals you feed recover, and your own animals take less damage."),
    "winemaker": ("Winemaker", "Learns by brewing and preparing sugar and honey. Potions you drink last longer."),
    "cook": ("Cook", "Learns by cooking and preparing food. Meals fill you more."),
    "carpenter": ("Carpenter", "Learns by felling trees and making wooden goods. You chop wood faster."),
    "mason": ("Mason", "Learns by quarrying and dressing stone. You cut stone faster."),
    "shipbuilder": ("Shipbuilder", "Learns by building boats. You swim faster and waste less timber."),
}

SKILLS = {
    "fire_stratagem": ("Fire Stratagem", "Set the spot you look at ablaze, burning and harming every foe there."),
    "confusion_array": ("Confusion Array", "Foes near your target lose their quarry and are slowed and weakened."),
    "battle_insight": ("Battle Insight", "Every foe within 32 blocks glows, and your mind and eyes sharpen."),
    "war_cry": ("War Cry", "You and nearby allies gain Strength and Resistance; nearby foes are weakened."),
    "cavalry_charge": ("Cavalry Charge", "Charge forward, striking and scattering the foes in your path."),
    "unyielding_will": ("Unyielding Will", "Steel yourself with heavy Absorption and Resistance."),
    "swift_sword": ("Swift Sword", "Dash and cut through the enemies ahead."),
    "lightness_skill": ("Lightness Skill", "Move like the wind: speed, high leaps and a feather's fall."),
    "hundred_strikes": ("Hundred Strikes", "A storm of blows on every foe within reach."),
    "thunder_talisman": ("Thunder Talisman", "Burn a talisman to call thunder down on your target, harming and slowing it."),
    "golden_elixir": ("Golden Elixir", "Share an elixir: you and nearby allies regenerate, resist fire and toughen."),
    "exorcism_seal": ("Exorcism Seal", "Seal the area you look at, harming and weakening every foe there. Undead suffer double."),
    "rejuvenate": ("Rejuvenate", "Heal yourself and nearby allies."),
    "acupuncture_needle": ("Acupuncture Needle", "A needle to the pressure points pins your target in place."),
    "purify": ("Purify", "Cleanse harmful effects and fire from yourself and nearby allies."),
    "iron_shackles": ("Iron Shackles", "Bind your target so it can barely move."),
    "arrest": ("Arrest", "Haul your target to your feet."),
    "patrol_sense": ("Patrol Sense", "Every foe within 48 blocks is revealed, and you quicken your step."),
}

STATS = {
    "command": ("Command", "Command: how strong your troops are on the battlefield, how many you can lead, how easily you recruit and how quickly they respond. Now: your animals hit harder."),
    "force": ("Force", "Force: the power of special attacks and the chance of critical hits (0.25% per point, for half again the damage). Also adds to melee damage and strengthens physical skills."),
    "intelligence": ("Intelligence", "Intelligence: lengthens buffs and controls, shortens skill cooldowns, widens your troops' vision and speeds weapon development."),
    "politics": ("Politics", "Politics: how well your policies work on markets and farms, and how diplomacy goes. Matters once your territory grows."),
}

INNATE = {
    "body": ("Body Structure", "Body", "Body Structure: toughness and survival, adapting to the environment, and storing Qi. Raises your maximum health."),
    "comprehension": ("Comprehension", "Compr.", "Comprehension: how quickly and deeply you learn. Raises all experience you earn by 1% per point above 5."),
    "luck": ("Luck", "Luck", "Luck: unexpected windfalls and losses. Feeds the game's luck, for better fishing and loot."),
    "charm": ("Charm", "Charm", "Charm: first impressions with NPCs and how easily sudden encounters go your way. Matters once you deal with talents and merchants."),
}

DIRS = {"east": "east", "southeast": "southeast", "south": "south", "southwest": "southwest",
        "west": "west", "northwest": "northwest", "north": "north", "northeast": "northeast"}

BUILDINGS = {
    "dining_hall": "Dining Hall", "bathroom": "Bathroom", "small_residential_courtyard": "Small Residential Courtyard",
    "blacksmith_workshop": "Blacksmith Workshop", "grocery_shop": "Grocery Shop", "military_camp": "Military Camp",
    "ancestral_hall": "Ancestral Hall", "clinic": "Clinic", "private_school": "Private School",
    "martial_arts_dojo": "Martial Arts Dojo", "granary": "Granary", "tailor_workshop": "Tailor Workshop",
}

TERRITORY_KINDS = {"village": "Village", "town": "Town", "county": "County", "prefecture": "Prefecture", "capital": "Capital City"}


def lang():
    L = {
        "itemGroup.realmlord": "Realm Lord",
        "key.categories.realmlord": "Realm Lord",
        "key.realmlord.panel": "Character panel",
        "key.realmlord.use_skill": "Use selected skill",
        "key.realmlord.cycle_skill": "Next skill (sneak: previous)",

        "block.realmlord.lords_manor": "Lord's Manor Seal",
        "item.realmlord.bandit_ledger": "Bandit Chief's Ledger",

        "realmlord.system.prefix": "[System]",
        "realmlord.system.world": "[World Notice]",
        "realmlord.ordinal": "%s",

        "realmlord.mode.none": "Undecided",
        "realmlord.mode.lord": "Lord",
        "realmlord.mode.adventurer": "Adventurer",

        "realmlord.work.tier.0": "Junior",
        "realmlord.work.tier.1": "Intermediate",
        "realmlord.work.tier.2": "Advanced",
        "realmlord.work.tier.3": "Master",

        "entity.realmlord.bandit": "Bandit",
        "entity.realmlord.bandit_archer": "Bandit Archer",
        "entity.realmlord.bandit_chief": "Bandit Chief",
        "entity.realmlord.settler": "Settler",
        "entity.realmlord.captive_settler": "Captive Settler",
        "item.realmlord.bandit_spawn_egg": "Bandit Spawn Egg",
        "item.realmlord.settler_spawn_egg": "Settler Spawn Egg",

        "item.realmlord.copper_coin": "Copper Coin",
        "item.realmlord.silver_coin": "Silver Coin",
        "item.realmlord.gold_coin": "Gold Coin",
        "realmlord.coin.rate": "1 gold = 100 silver = 10,000 copper",
        "realmlord.coin.break": "Use: break one coin into 100 of the coin below",
        "realmlord.coin.combine": "Sneak + use: change 100 coins into one of the coin above",
        "realmlord.coin.need": "You need %s %s to change them up.",
        "realmlord.coins.gold": "%s gold coins",
        "realmlord.coins.silver": "%s silver coins",
        "item.realmlord.blueprint": "Blueprint",
        "item.realmlord.blueprint.named": "Blueprint: %s",
        "item.realmlord.resources_bag": "Resources Bag",
        "item.realmlord.resources_bag.named": "%s Resources Bag",
        "item.realmlord.summoning_token": "Summoning Token",
        "item.realmlord.summoning_token.named": "Summoning Token (%s)",
        "realmlord.rank.king": "King rank",
        "realmlord.rank.emperor": "Emperor rank",
        "realmlord.rank.divine": "Divine rank",
        "realmlord.rank.saint": "Saint rank",
        "realmlord.bag.tier.basic": "Basic",
        "realmlord.bag.tier.intermediate": "Intermediate",
        "realmlord.bag.tier.advanced": "Advanced",
        "realmlord.bag.tooltip.use": "Use to empty it into your territory's stores.",
        "realmlord.bag.no_territory": "A Resources Bag empties into a territory's stores. Found your village first.",
        "realmlord.blueprint.tooltip.grade": "A Grade %s Village building",
        "realmlord.blueprint.tooltip.keep": "Needed to construct it. Construction arrives in Phase 2; keep it safe.",
        "realmlord.bag.opened": "%s Resources Bag emptied: %s food, %s wood, %s stone and %s iron ore added to the stores of %s.",
        "realmlord.bag.tooltip": "Holds %s food, %s wood, %s stone and %s iron ore.",
        "realmlord.summoning.tooltip": "Summons a random %s historical figure to serve you.",
        "realmlord.summoning.tooltip2": "Talents arrive in Phase 3. Keep this token until then.",
        "realmlord.summoning.not_yet": "No talent answers the call yet. Keep the Summoning Token: it will work once historical figures arrive (Phase 3).",
        "realmlord.gear.named": "%s-grade %s",
        "realmlord.gear.grade": "Grade: %s",

        # Messages
        "realmlord.msg.welcome": "Welcome to the world. Choose your path: rule as a Lord or roam as an Adventurer. (Press J if the window closed.)",
        "realmlord.msg.chose_lord": "You have chosen the path of a Lord, with %s as your sub-profession. The Village Creation quest has begun.",
        "realmlord.msg.chose_adventurer": "You have chosen the path of an Adventurer, as a %s. The world is yours to roam.",
        "realmlord.msg.level_up": "You reached level %s. Unspent attribute points: %s (press J).",
        "realmlord.msg.work_up": "Your %s rose to level %s (%s). Unspent attribute points: %s.",
        "realmlord.msg.skill_unlocked": "New skill learned: %s.",
        "realmlord.msg.merit": "+%s merit (%s).",
        "realmlord.msg.reputation": "+%s reputation.",
        "realmlord.msg.title": "Your merit has earned you the title %s.",
        "realmlord.msg.title_world": "%s has been raised to %s!",
        "realmlord.msg.death_penalty": "You lost %s experience in death.",

        "realmlord.reason.founding": "founding a village",
        "realmlord.reason.quest": "Village Creation quest",
        "realmlord.reason.admin": "granted by an administrator",
        "realmlord.reason.treasure": "finding the chief's treasure",

        # Quest
        "realmlord.quest.start": "Village Creation quest: settlers are waiting for a lord to lead them. First gather supplies for them: %s logs, %s stone and %s food, then hand them over in the Quest tab of your panel (J). You have %s days. Finish within %s days for a Gold token; the slower you are, the lower the grade. The bandits are strong: get yourself iron armour first.",
        "realmlord.quest.nothing": "You carry nothing the settlers need. Supplies so far: logs %s/%s, stone %s/%s, food %s/%s.",
        "realmlord.quest.supplied": "Supplies delivered. Logs %s/%s, stone %s/%s, food %s/%s.",
        "realmlord.quest.bandits": "The supplies are ready, but bandits have seized the settlers! Their camp lies near X %s, Z %s, to the %s, about %s blocks away. Drive them out.",
        "realmlord.quest.camp_found": "You have found the bandit camp. Defeat every bandit and their chief.",
        "realmlord.quest.chief_down": "The Bandit Chief has fallen. He dropped a ledger.",
        "realmlord.quest.cleared": "The camp is cleared and %s captive families go free: %s people will follow you. They wait by the camp until you found your village. Open the Quest tab (J) to claim your reward.",
        "realmlord.quest.cache": "You found the Bandit Chief's buried treasure.",
        "realmlord.quest.claimed": "Village Creation quest complete! You received the village creation set: a %s Village Creation Token, a Summoning Token (%s) and a %s Resources Bag. Use the token on the ground where your village should stand.",
        "realmlord.quest.bonus": "Completion bonus: %s experience, %s reputation and %s.",
        "realmlord.quest.gold_world": "%s has earned the only Gold Village Creation Token in the world!",
        "realmlord.quest.warning": "Less than a day remains to finish the Village Creation quest.",
        "realmlord.quest.failed_time": "You ran out of time. The settlers have scattered.",
        "realmlord.quest.failed_abandon": "You abandoned the Village Creation quest.",
        "realmlord.quest.failed_now_adventurer": "Without settlers you cannot found a village. You continue as an Adventurer.",

        "realmlord.ledger.read": "The ledger's last page, in a crude hand: \"The chief's share lies buried under the fire.\" (near X %s, Z %s)",
        "realmlord.ledger.done": "You have already dug up the chief's treasure.",
        "realmlord.ledger.cipher": "The pages are in a bandit cipher that means nothing to you.",
        "realmlord.ledger.tooltip": "Taken from a Bandit Chief. Use to read.",

        # Founding
        "realmlord.found.not_lord": "Only a Lord can found a village.",
        "realmlord.found.already": "You already rule a territory.",
        "realmlord.found.overworld": "Villages can only be founded in the main world.",
        "realmlord.found.too_close": "Too close to another territory. Move at least %s blocks further away.",
        "realmlord.found.world": "Congratulations to %s for founding village no. %s in the world: %s, of %s grade! Special reward: %s merit.",
        "realmlord.found.private": "You founded %s, a %s grade village.",
        "realmlord.territory.default_name": "%s Village",
        "realmlord.manor.unclaimed": "This seal belongs to no territory.",
        "realmlord.manor.visitor": "This is %s, ruled by %s.",

        "realmlord.token.tooltip.use": "Use on the ground to found your village there.",
        "realmlord.token.tooltip.name": "Rename it in an anvil to name your village.",
        "realmlord.token.tooltip.bronze": "The most common token. Most villages begin this way.",
        "realmlord.token.tooltip.black_iron": "Earned by finishing the quest within 7.5 days.",
        "realmlord.token.tooltip.silver": "Earned by finishing the quest within 5 days.",
        "realmlord.token.tooltip.gold": "Earned by finishing within 3 days. There is only one in all the world.",

        "realmlord.restriction.warning": "A Lord may not leave their territory outside of war. Return within %s seconds or be sent home.",
        "realmlord.restriction.sent_home": "You have been returned to your territory.",

        # Skills
        "realmlord.skill.none": "You have no skill to use.",
        "realmlord.skill.cooldown": "%s is ready in %ss.",
        "realmlord.skill.no_target": "No target in sight.",

        # HUD
        "realmlord.hud.choose": "Press %s to choose your path",
        "realmlord.hud.level": "Lv %s %s",
        "realmlord.hud.work": "%s %s, level %s",
        "realmlord.hud.points": "+%s points (%s)",
        "realmlord.hud.ready": "  ready [%s]",
        "realmlord.hud.quest_supplies": "Supplies: logs %s/%s, stone %s/%s, food %s/%s",
        "realmlord.hud.quest_camp": "Find the bandit camp near X %s, Z %s",
        "realmlord.hud.quest_fight": "Bandits remaining: %s",
        "realmlord.hud.quest_claim": "Claim your token (%s, Quest tab)",
        "realmlord.hud.quest_found": "Use your token to found your village",
        "realmlord.hud.time_left": "%s days %s hours left",

        # Role screen
        "realmlord.role.title": "Choose Your Path",
        "realmlord.role.subtitle": "This choice cannot be undone.",
        "realmlord.role.lord_desc": "Rule a territory. Earn a Village Creation Token through a demanding quest, found your village and raise it, grade by grade, into a town, county, prefecture and beyond. Merit raises your noble title, which every upgrade requires. You also pick a sub-profession.",
        "realmlord.role.lord_restricted": "On this world, Lords may not leave their territory except for war.",
        "realmlord.role.adventurer_desc": "Roam the world freely. Choose a combat occupation with levels and skills, or a trade that grows from Junior to Master. Adventurers learn a quarter faster, and their strength is entirely their own. Every character rolls innate Body Structure, Comprehension, Luck and Charm.",
        "realmlord.role.pick_lord": "Become a Lord",
        "realmlord.role.pick_adventurer": "Become an Adventurer",
        "realmlord.role.choose_sub": "Choose your sub-profession",
        "realmlord.role.choose_occ": "Choose your occupation",
        "realmlord.role.tab_combat": "Combat",
        "realmlord.role.tab_work": "Work",
        "realmlord.role.combat_desc": "Combat occupations have a character level (up to 100) and three skills each, learned at levels 1, 10 and 25. Each level gives 2 attribute points and every fifth level raises all four by 1. Experience is lost on death.",
        "realmlord.role.work_desc": "Work occupations have no character level, only a craft level from 1 to 10 (Junior, Intermediate, Advanced, Master). They never lose experience on death. A Lord with a trade still has a character level.",
        "realmlord.role.back": "Back",
        "realmlord.role.confirm": "Confirm",

        # Character panel
        "realmlord.panel.title": "Character",
        "realmlord.panel.tab.character": "Character",
        "realmlord.panel.tab.quest": "Quest",
        "realmlord.panel.tab.territory": "Territory",
        "realmlord.panel.lord_line": "Lord, sub-profession: %s",
        "realmlord.panel.adventurer_line": "Adventurer: %s",
        "realmlord.panel.merit": "Merit %s / %s",
        "realmlord.panel.merit_max": "Merit %s (highest title)",
        "realmlord.panel.level": "Level %s · Reputation %s",
        "realmlord.panel.innate": "Innate",
        "realmlord.panel.work_level": "%s %s, level %s",
        "realmlord.panel.attributes": "Attributes",
        "realmlord.panel.points": "%s to spend",
        "realmlord.panel.skills": "Skills",
        "realmlord.panel.no_skills": "Trades have no combat skills. Their bonuses work on their own as you practise.",
        "realmlord.panel.cooldown": "%ss",
        "realmlord.panel.unlocks": "Lv %s",
        "realmlord.panel.skill_keys": "%s: use · %s: next",

        "realmlord.panel.quest.name": "Village Creation Quest",
        "realmlord.panel.quest.adventurer": "Only Lords take the Village Creation quest. Adventurers are free to roam.",
        "realmlord.panel.quest.not_started": "The quest has not begun.",
        "realmlord.panel.quest.failed": "You failed the Village Creation quest and lost the right to found a village.",
        "realmlord.panel.quest.claimed": "Complete. You earned a %s Village Creation Token.",
        "realmlord.panel.quest.obj_supplies": "Gather supplies: logs %s/%s, stone %s/%s, food %s/%s",
        "realmlord.panel.quest.obj_bandits": "Free the settlers from the bandit camp near X %s, Z %s (%s bandits left)",
        "realmlord.panel.quest.obj_bandits_hidden": "Free the settlers from the bandits",
        "realmlord.panel.quest.obj_treasure": "Optional: find the Bandit Chief's buried treasure",
        "realmlord.panel.quest.ready": "The settlers are free. Claim your reward.",
        "realmlord.panel.quest.time_left": "Time left: %s days %s hours",
        "realmlord.panel.quest.time_done": "Finished in %s days %s hours",
        "realmlord.panel.quest.prospect": "Finish now for a %s token",
        "realmlord.panel.quest.earned": "Earns a %s token",
        "realmlord.panel.quest.submit": "Hand over supplies",
        "realmlord.panel.quest.claim": "Claim reward",
        "realmlord.panel.quest.abandon": "Abandon quest",
        "realmlord.panel.quest.abandon_confirm": "Click again to abandon",

        "realmlord.panel.territory.none_lord": "You have no territory yet. Complete the Village Creation quest, then use your Village Creation Token on the ground where your village should stand. The first villages in the world earn the most merit.",
        "realmlord.panel.territory.none_adventurer": "Adventurers rule no territory.",
        "realmlord.panel.territory.world": "Villages in the world: %s · Gold token: %s",
        "realmlord.panel.territory.gold_taken": "taken",
        "realmlord.panel.territory.gold_free": "unclaimed",
        "realmlord.panel.territory.kind": "%s grade %s, grade %s",
        "realmlord.panel.territory.population": "Population: %s",
        "realmlord.panel.territory.seat": "Lord's Manor: %s, %s, %s",
        "realmlord.panel.territory.border": "Border: %s blocks",
        "realmlord.panel.territory.order": "Founded: village no. %s in the world",
        "realmlord.panel.territory.stores": "Stores",
        "realmlord.panel.territory.food": "Food: %s",
        "realmlord.panel.territory.wood": "Wood: %s",
        "realmlord.panel.territory.stone": "Stone: %s",
        "realmlord.panel.territory.iron_ore": "Iron ore: %s",
        "realmlord.panel.territory.next": "To become %s grade %s:",
        "realmlord.panel.territory.req_pop": "Population %s / %s",
        "realmlord.panel.territory.req_title": "Lord title of at least %s",
        "realmlord.panel.territory.req_infra": "All required buildings complete",
        "realmlord.panel.territory.req_index": "Politics, economy, culture and military indexes of %s",
        "realmlord.panel.territory.coming": "Buildings, residents and upgrades arrive in Phase 2.",

        # Commands
        "realmlord.cmd.level": "Level set to %s.",
        "realmlord.cmd.worklevel": "Work level set to %s.",
        "realmlord.cmd.points": "Attribute points set to %s.",
        "realmlord.cmd.merit": "Merit is now %s.",
        "realmlord.cmd.reset": "Reset %s. They will choose their path again.",
        "realmlord.cmd.info.none": "%s has not chosen a path yet.",
        "realmlord.cmd.info.head": "%s: %s (%s), level %s, %s, %s merit",
        "realmlord.cmd.info.stats": "Command %s, Force %s, Intelligence %s, Politics %s, %s points unspent",
        "realmlord.cmd.info.innate": "Body Structure %s, Comprehension %s, Luck %s, Charm %s, reputation %s",
        "realmlord.cmd.info.territory": "Territory: %s (%s %s grade %s), population %s, at %s, %s",
        "realmlord.cmd.territories.none": "No villages have been founded yet.",
        "realmlord.cmd.territories.line": "#%s %s, ruled by %s (%s %s grade %s), population %s",
    }
    for bid, name in BUILDINGS.items():
        L[f"realmlord.building.{bid}"] = name
    for gid, name in GRADES.items():
        L[f"realmlord.grade.{gid}"] = name
        L[f"item.realmlord.{gid}_village_token"] = f"{name} Village Creation Token"
    for tid, full, short in TITLES:
        L[f"realmlord.title.{tid}"] = full
        L[f"realmlord.title.short.{tid}"] = short
    for cid, (name, desc) in CLASSES.items():
        L[f"realmlord.class.{cid}"] = name
        L[f"realmlord.class.{cid}.desc"] = desc
    for wid, (name, desc) in WORK.items():
        L[f"realmlord.work.{wid}"] = name
        L[f"realmlord.work.{wid}.desc"] = desc
    for sid, (name, desc) in SKILLS.items():
        L[f"realmlord.skill.{sid}"] = name
        L[f"realmlord.skill.{sid}.desc"] = desc
    for sid, (name, desc) in STATS.items():
        L[f"realmlord.stat.{sid}"] = name
        L[f"realmlord.stat.{sid}.desc"] = desc
    for iid, (name, short, desc) in INNATE.items():
        L[f"realmlord.innate.{iid}"] = name
        L[f"realmlord.innate.{iid}.short"] = short
        L[f"realmlord.innate.{iid}.desc"] = desc
    for did, name in DIRS.items():
        L[f"realmlord.dir.{did}"] = name
    for kid, name in TERRITORY_KINDS.items():
        L[f"realmlord.territory.{kid}"] = name
    lang_village.add(L)
    return dict(sorted(L.items()))


# --------------------------------------------------------------------------- textures

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def token_texture(base, glow):
    """A round coin with a square hole and a raised rim, lit from the top left."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 7.4:
                continue
            light = 1.0 + 0.18 * ((cx - x) + (cy - y)) / 7.5
            if d > 6.2:
                c = shade(base, 0.75 * light)            # rim
            elif d > 5.4:
                c = shade(base, 1.18 * light)            # rim highlight
            else:
                c = shade(base, 0.98 * light)
            if 5 <= x <= 10 and 5 <= y <= 10:
                if 6 <= x <= 9 and 6 <= y <= 9:
                    c = (0, 0, 0, 0)                     # square hole
                else:
                    c = shade(base, 0.6)                 # hole bevel
            px[x, y] = c
    # Four stamped characters around the hole, as dots.
    for (x, y) in [(7, 2), (8, 2), (2, 7), (2, 8), (13, 7), (13, 8), (7, 13), (8, 13)]:
        px[x, y] = shade(base, 0.62)
    if glow:
        for (x, y) in [(3, 3), (4, 2), (12, 4), (11, 12)]:
            px[x, y] = (255, 255, 235, 255)
    return img


def ledger_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    leather = hexc("6b4426")
    pages = hexc("e8dcc0")
    for y in range(2, 15):
        for x in range(3, 13):
            c = leather
            if x == 3 or y == 2 or y == 14:
                c = shade(leather, 0.7)
            if x == 12:
                c = pages if y not in (2, 14) else shade(leather, 0.7)
            if x == 4:
                c = shade(leather, 1.2)
            px[x, y] = c
    for y in range(2, 15):
        px[8, y] = hexc("a3282b")                       # red cord
    for x in range(5, 12):
        px[x, 8] = hexc("a3282b")
    px[8, 8] = hexc("d9a441")
    return img


def manor_textures():
    rnd = random.Random(7)
    lacquer = hexc("8e1f1b")
    trim = hexc("d4a840")
    wood = hexc("3b2417")
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for y in range(16):
        for x in range(16):
            c = shade(lacquer, 0.9 + rnd.random() * 0.15)
            if y in (0, 15) or x in (0, 15):
                c = shade(wood, 0.9 + rnd.random() * 0.2)
            elif y in (1, 14) or x in (1, 14):
                c = trim
            px[x, y] = c
    # A gold medallion in the middle.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 2.6 < d < 3.8:
                px[x, y] = trim
            elif d <= 1.2:
                px[x, y] = shade(trim, 1.15)
    top = Image.new("RGBA", (16, 16))
    pt = top.load()
    for y in range(16):
        for x in range(16):
            c = shade(lacquer, 0.85 + rnd.random() * 0.15)
            if y in (0, 15) or x in (0, 15):
                c = shade(wood, 0.9)
            elif y in (1, 14) or x in (1, 14):
                c = trim
            pt[x, y] = c
    # Seal: a square frame with a cross, like a carved official seal.
    for i in range(4, 12):
        for (x, y) in [(i, 4), (i, 11), (4, i), (11, i)]:
            pt[x, y] = trim
    for i in range(6, 10):
        pt[i, 7] = trim
        pt[i, 8] = trim
        pt[7, i] = trim
        pt[8, i] = trim
    bottom = Image.new("RGBA", (16, 16))
    pb = bottom.load()
    for y in range(16):
        for x in range(16):
            pb[x, y] = shade(wood, 0.85 + rnd.random() * 0.25)
    return side, top, bottom


def coin_texture(col="e8b931"):
    gold = hexc(col)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Three stacked coins.
    for (cx, cy) in [(5.5, 10.5), (10.5, 10.5), (8, 6)]:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d <= 3.6:
                    light = 1.0 + 0.2 * ((cx - x) + (cy - y)) / 3.6
                    c = shade(gold, (0.72 if d > 2.9 else 1.0) * light)
                    if abs(x - cx) <= 0.6 and abs(y - cy) <= 0.6:
                        c = shade(gold, 0.45)
                    px[x, y] = c
    return img


def blueprint_texture():
    paper = hexc("3d6fb0")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(3, 13):
        for x in range(2, 14):
            c = shade(paper, 0.95 + 0.1 * ((x + y) % 2))
            if (x in (4, 11) and 5 <= y <= 10) or (y in (5, 10) and 4 <= x <= 11) or (x == 7 and 5 <= y <= 10):
                c = hexc("d9e6f5")
            px[x, y] = c
    for y in range(2, 14):
        for x in (1, 14):
            px[x, y] = hexc("8a5a2e")
        px[0, y] = hexc("6b4426") if 3 <= y <= 12 else (0, 0, 0, 0)
        px[15, y] = hexc("6b4426") if 3 <= y <= 12 else (0, 0, 0, 0)
    return img


def bag_texture():
    hemp = hexc("b49a6a")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 15):
        half = 3 + min(y - 4, 4) if y < 12 else 7 - (y - 12)
        for x in range(8 - half, 8 + half):
            light = 1.0 + 0.15 * ((8 - x) / 7.0) - 0.1 * ((y - 4) / 10.0)
            px[x, y] = shade(hemp, light)
    for x in range(6, 10):
        px[x, 4] = hexc("8c1f1c")
    for (x, y) in [(6, 2), (7, 3), (9, 2), (8, 3), (7, 1), (9, 1)]:
        px[x, y] = shade(hemp, 0.85)
    for (x, y) in [(7, 8), (8, 8), (7, 9), (8, 9)]:
        px[x, y] = hexc("e8b931")
    return img


def summoning_texture():
    jade = hexc("3fa37a")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(1, 15):
        for x in range(4, 12):
            if (y in (1, 14)) and x in (4, 11):
                continue
            light = 1.0 + 0.2 * ((8 - x) + (8 - y)) / 8.0
            c = shade(jade, light)
            if x in (4, 11) or y in (1, 14):
                c = hexc("c9a13e")
            px[x, y] = c
    for (x, y) in [(7, 4), (8, 4), (6, 5), (9, 5), (7, 6), (8, 6), (7, 8), (8, 8), (7, 9), (8, 9), (6, 10), (9, 10), (7, 11), (8, 11)]:
        px[x, y] = hexc("e8d27a")
    return img


def textures():
    item_dir = os.path.join(ASSETS, "textures", "item")
    block_dir = os.path.join(ASSETS, "textures", "block")
    os.makedirs(item_dir, exist_ok=True)
    os.makedirs(block_dir, exist_ok=True)
    colors = {"bronze": ("b8743a", False), "black_iron": ("5a5f69", False), "silver": ("cfd6e0", True), "gold": ("f0c23c", True)}
    for gid, (col, glow) in colors.items():
        token_texture(hexc(col), glow).save(os.path.join(item_dir, f"{gid}_village_token.png"))
    ledger_texture().save(os.path.join(item_dir, "bandit_ledger.png"))
    coin_texture().save(os.path.join(item_dir, "gold_coin.png"))
    coin_texture("cfd6e0").save(os.path.join(item_dir, "silver_coin.png"))
    coin_texture("c27a3e").save(os.path.join(item_dir, "copper_coin.png"))
    blueprint_texture().save(os.path.join(item_dir, "blueprint.png"))
    bag_texture().save(os.path.join(item_dir, "resources_bag.png"))
    summoning_texture().save(os.path.join(item_dir, "summoning_token.png"))
    entity_dir = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(entity_dir, exist_ok=True)
    for name, img in gen_skins.all_skins().items():
        img.save(os.path.join(entity_dir, name + ".png"))
    side, top, bottom = manor_textures()
    side.save(os.path.join(block_dir, "lords_manor_side.png"))
    top.save(os.path.join(block_dir, "lords_manor_top.png"))
    bottom.save(os.path.join(block_dir, "lords_manor_bottom.png"))


# --------------------------------------------------------------------------- models

def models():
    for gid in GRADES:
        w(os.path.join(ASSETS, "models", "item", f"{gid}_village_token.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{gid}_village_token"}})
    for name in ("copper_coin", "silver_coin", "gold_coin", "blueprint", "resources_bag", "summoning_token"):
        w(os.path.join(ASSETS, "models", "item", f"{name}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"realmlord:item/{name}"}})
    for name in ("bandit_spawn_egg", "settler_spawn_egg"):
        w(os.path.join(ASSETS, "models", "item", f"{name}.json"), {"parent": "minecraft:item/template_spawn_egg"})
    w(os.path.join(ASSETS, "models", "item", "bandit_ledger.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": "realmlord:item/bandit_ledger"}})
    w(os.path.join(ASSETS, "models", "block", "lords_manor.json"),
      {"parent": "minecraft:block/cube_bottom_top", "textures": {
          "side": "realmlord:block/lords_manor_side", "top": "realmlord:block/lords_manor_top",
          "bottom": "realmlord:block/lords_manor_bottom"}})
    w(os.path.join(ASSETS, "models", "item", "lords_manor.json"), {"parent": "realmlord:block/lords_manor"})
    w(os.path.join(ASSETS, "blockstates", "lords_manor.json"), {"variants": {"": {"model": "realmlord:block/lords_manor"}}})


if __name__ == "__main__":
    w(os.path.join(ASSETS, "lang", "en_us.json"), lang())
    textures()
    models()
    print("assets written")
