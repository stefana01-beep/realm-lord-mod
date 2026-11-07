package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The council: the officials at work. In the novel Ouyang Shuo's officials do not wait to be told:
 * Shi Jun brings him the militia's needs, Fan Zhongyan drafts the town's plans and sends memorials
 * on housing, grain and refugees, Gu Hong's Commerce office sets the trade routes, and once a plan
 * is approved they carry it out themselves.
 *
 * <p>Every day each official looks at their charge and may put a proposal to the Lord: the Steward
 * when beds run short or people stand idle, the Agriculture head when grain runs low or piles up,
 * the Works head with the next building the upgrade needs (and when it is all done, the upgrade
 * itself), the Treasurer when the treasury runs dry, the Military head when the beastwave or the
 * raiders are coming, the Rites head when spirits are low, and the town's departments likewise.
 * Abler officials (by the stat their office uses) propose more often and more boldly.
 *
 * <p>Proposals wait in the Lord's Manor (the Council tab) and come to the Lord in chat with Approve
 * and Dismiss; approved, the official carries it out: the builders lay out the building on a good
 * spot, people are moved to the work, grain is bought or sold, militia enlisted, a workshop's work
 * changed, a festival held. They lapse after three days. The Lord may let the officials handle
 * routine matters on their own; then all but new buildings and the upgrade are done at once and
 * reported.
 */
public final class Council {
    private Council() {}

    public static final int MAX = 8;
    public static final int EXPIRES = 3;

    public static ListTag list(Territory t) {
        return t.ext.getList("council", Tag.TAG_COMPOUND);
    }

    public static ListTag log(Territory t) {
        return t.ext.getList("councilLog", Tag.TAG_STRING);
    }

    public static boolean auto(Territory t) {
        return t.ext.getBoolean("councilAuto");
    }

    public static void setAuto(Territory t, boolean on) {
        t.ext.putBoolean("councilAuto", on);
    }

    /** A proposal: who, what they say, and what to do. */
    private record Plan(String key, String act, Object... args) {}

    // ------------------------------------------------------------------ what each official sees

    private static int idle(Territory t) {
        int n = 0;
        for (Resident r : t.residents) if (free(t, r)) n++;
        return n;
    }

    private static boolean free(Territory t, Resident r) {
        return r.job == Job.IDLE && !r.militia && r.office < 0 && !r.follows && r.trainingAt < 0 && !Village.isStaffing(t, r) && !r.isFigure();
    }

    private static double foodDays(Territory t) {
        long use = Math.max(1, Village.foodUse(t));
        return t.food / (double) use;
    }

    private static boolean known(Territory t, BuildingType type) {
        return !type.blueprint || t.blueprints.contains(type.id());
    }

    private static boolean affordable(Territory t, BuildingType type) {
        return type.grade <= t.level.buildGrade() && t.wood >= type.wood && t.stone >= type.stone && known(t, type)
                && (type.specialist == null || Village.hasSpecialist(t, type.specialist, type.specialistRank));
    }

    private static boolean building(Territory t, BuildingType type) {
        for (Building b : t.buildings) if (b.type == type && !b.complete && !b.demolishing) return true;
        return false;
    }

    private static BuildingType housing(Territory t) {
        BuildingType[] order = {BuildingType.RESIDENTIAL_QUARTER, BuildingType.LARGE_RESIDENTIAL_COURTYARD, BuildingType.MEDIUM_RESIDENTIAL_COURTYARD,
                BuildingType.SMALL_RESIDENTIAL_COURTYARD};
        for (BuildingType b : order) if (affordable(t, b)) return b;
        return null;
    }

    /** The job most wanted now, with room for more. */
    private static Job neediest(Territory t) {
        if (foodDays(t) < 15 && Village.places(t, Job.FARMING) > Village.count(t, Job.FARMING)) return Job.FARMING;
        if (t.wood < 300 && Village.places(t, Job.LOGGING) > Village.count(t, Job.LOGGING)) return Job.LOGGING;
        if (t.stone < 200 && Village.places(t, Job.QUARRYING) > Village.count(t, Job.QUARRYING)) return Job.QUARRYING;
        for (Job j : new Job[]{Job.FARMING, Job.CONSTRUCTION, Job.LOGGING, Job.QUARRYING, Job.FISHING, Job.SALT_MAKING, Job.MINING}) {
            if (Village.places(t, j) > Village.count(t, j)) return j;
        }
        return null;
    }

    /** The next building the upgrade still needs that the village can raise now. */
    private static BuildingType nextRequired(Territory t) {
        for (BuildingType bt : BuildingType.values()) {
            if (bt.requiredFor != t.level.buildGrade() || bt.requiredFor == 0 || !bt.placed()) continue;
            if (Village.countComplete(t, bt) + (building(t, bt) ? 1 : 0) >= bt.requiredCount) continue;
            if (building(t, bt)) continue;
            if (affordable(t, bt)) return bt;
        }
        return null;
    }

    private static Building first(Territory t, BuildingType type) {
        for (Building b : t.buildings) if (b.type == type && b.complete && !b.demolishing) return b;
        return null;
    }

    /** What an official proposes today, or null. */
    private static Plan think(ServerLevel level, Territory t, Office o, Resident by, RandomSource rnd) {
        int skill = o.statOf(by);
        switch (o) {
            case STEWARD -> {
                if (!held(t, Office.WORKS) && Planning.due(level, t)) {
                    Plan plan = planProposal(level, t);
                    if (plan != null) return plan;
                }
                int open = Village.openBeds(t);
                if (open < Math.max(5, t.residents.size() / 20)) {
                    BuildingType h = housing(t);
                    if (h != null && !building(t, h) && !Planning.active(t)) return new Plan("housing", "build:" + h.name(), open, Component.translatable(h.key()));
                }
                int idle = idle(t);
                Job j = neediest(t);
                if (idle >= 2 && j != null) {
                    int n = Math.min(idle, Math.max(2, skill / 10));
                    return new Plan("idle", "jobs:" + j.name() + ":" + n, idle, n, Component.translatable(j.key()));
                }
            }
            case AGRICULTURE -> {
                double days = foodDays(t);
                if (days < 10) {
                    int room = Village.places(t, Job.FARMING) - Village.count(t, Job.FARMING);
                    if (room > 0 && idle(t) > 0) {
                        int n = Math.min(room, Math.min(idle(t), 2 + skill / 15));
                        return new Plan("farmers", "jobs:FARMING:" + n, Math.round(days), n);
                    }
                    if (!building(t, BuildingType.FARMLAND) && affordable(t, BuildingType.FARMLAND)) {
                        return new Plan("farmland", "build:FARMLAND", Math.round(days));
                    }
                    long units = Math.max(100, Village.foodUse(t) * 7);
                    if (t.treasury >= Market.buyCost(Market.FOOD, units)) {
                        return new Plan("buy_grain", "buy_food:" + units, Math.round(days), units, Coins.format(Market.buyCost(Market.FOOD, units)));
                    }
                } else if (days > 80 && t.food > 6000) {
                    long units = t.food / 3;
                    return new Plan("sell_grain", "sell_food:" + units, units, Coins.format(Market.sellIncome(Market.FOOD, units)));
                }
            }
            case WORKS -> {
                ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
                if (lord != null && t.level.next() != null && !com.realmlord.world.Trials.underway(t)
                        && Village.upgradeMissing(t, com.realmlord.profile.Profiles.get(lord)).isEmpty()) {
                    return new Plan("upgrade", "upgrade", Component.translatable(t.level.next().kindKey()), t.level.next().grade);
                }
                if (Planning.due(level, t)) {
                    Plan plan = planProposal(level, t);
                    if (plan != null) return plan;
                }
                BuildingType bt = Planning.active(t) ? null : nextRequired(t);
                if (bt != null) return new Plan("required", "build:" + bt.name(), Component.translatable(bt.key()), bt.wood, bt.stone);
                if (t.wood < 150 && Village.places(t, Job.LOGGING) > Village.count(t, Job.LOGGING) && idle(t) > 0) {
                    return new Plan("wood", "jobs:LOGGING:" + Math.min(idle(t), 3), t.wood);
                }
                if (t.stone < 100 && Village.places(t, Job.QUARRYING) > Village.count(t, Job.QUARRYING) && idle(t) > 0) {
                    return new Plan("stone", "jobs:QUARRYING:" + Math.min(idle(t), 3), t.stone);
                }
            }
            case TREASURER -> {
                if (t.treasury < 3_000) {
                    Goods best = null;
                    long worth = 0;
                    for (Goods g : Goods.values()) {
                        if (g == Goods.GRAIN || g == Goods.WOOD || g == Goods.STONE || g == Goods.IRON_ORE) continue;
                        long w = g.stock(t) / 2 * g.price;
                        if (w > worth) {
                            worth = w;
                            best = g;
                        }
                    }
                    if (best != null && worth >= 2_000) {
                        long units = best.stock(t) / 2;
                        return new Plan("sell_goods", "sell:" + best.name() + ":" + units, Coins.format(t.treasury), units,
                                Component.translatable(best.key()), Coins.format(best.sellIncome(units)));
                    }
                }
            }
            case MILITARY -> {
                boolean threat = t.ext.getLong("trialDay") > 0 || com.realmlord.world.WorldState.get(level.getServer()).warnedYear
                        == GaiaCalendar.year(GaiaCalendar.today(level.getServer())) || t.security < 50;
                int room = Village.militiaCap(t) - Village.militiaCount(t);
                if (threat && room > 0 && idle(t) + 2 > 0) {
                    int n = Math.min(room, 3 + skill / 20);
                    return new Plan("militia", "militia:" + n, n);
                }
                if (Village.countComplete(t, BuildingType.BARRACKS) == 0 && !building(t, BuildingType.BARRACKS) && affordable(t, BuildingType.BARRACKS)) {
                    return new Plan("barracks", "build:BARRACKS");
                }
            }
            case SECURITY -> {
                if (t.security < 55 && t.ext.getLong("patrolUntil") < GaiaCalendar.today(level.getServer()) && t.treasury >= 1_000) {
                    return new Plan("patrols", "patrol", t.security);
                }
            }
            case RITES -> {
                if (t.peopleThoughts < 60 && t.ext.getInt("festivalMorale") == 0) {
                    long cost = t.residents.size() * 3L;
                    if (t.treasury >= cost) return new Plan("festival", "festival", t.peopleThoughts, Coins.format(cost));
                }
                if (Village.countComplete(t, BuildingType.VILLAGE_TEMPLE) + Village.countComplete(t, BuildingType.TEMPLE) == 0
                        && !building(t, BuildingType.VILLAGE_TEMPLE) && affordable(t, BuildingType.VILLAGE_TEMPLE)) {
                    return new Plan("temple", "build:VILLAGE_TEMPLE");
                }
            }
            case COMMERCE -> {
                Building c = first(t, BuildingType.CHAMBER_OF_COMMERCE);
                if (c != null && Trade.caravanOf(t, c.id) == null) {
                    int best = Trade.destination(c);
                    double score = -1;
                    for (int i = 0; i < com.realmlord.worldgen.ChinaMap.CAPITALS.length; i++) {
                        double s = Trade.markup(t, i) * (1 - Trade.risk(t, i)) / Math.max(2, Trade.days(t, i));
                        if (s > score) {
                            score = s;
                            best = i;
                        }
                    }
                    if (best != c.mode) return new Plan("route", "mode:" + c.id + ":" + best, com.realmlord.worldgen.ChinaMap.CAPITALS[best]);
                }
                if (c == null && t.level.buildGrade() >= 2 && !building(t, BuildingType.CHAMBER_OF_COMMERCE) && affordable(t, BuildingType.CHAMBER_OF_COMMERCE)) {
                    return new Plan("chamber", "build:CHAMBER_OF_COMMERCE");
                }
            }
            case INTERNAL_AFFAIRS -> {
                if (Refugees.waiting(t) > Village.openBeds(t) + 10) {
                    BuildingType h = housing(t);
                    if (h != null && !building(t, h)) return new Plan("refugee_housing", "build:" + h.name(), Refugees.waiting(t), Component.translatable(h.key()));
                }
            }
            case ARMORY -> {
                Building a = first(t, BuildingType.ARSENAL);
                if (a != null && Army.count(t) > 0 && Goods.WEAPONS.stock(t) < Army.count(t) / 2 && a.mode != 0) {
                    return new Plan("arms", "mode:" + a.id + ":0", Army.count(t));
                }
            }
            case TRANSPORT -> {
                Building y = first(t, BuildingType.SHIPYARD);
                if (y != null && Navy.hasDock(t) && Navy.ships(t, com.realmlord.registry.ModCrafts.FISHING_BOAT.get()) < 2 && y.mode != 0) {
                    return new Plan("boats", "mode:" + y.id + ":0");
                }
            }
            case PROCURATORATE, AUDITING -> {
                int month = GaiaCalendar.year(GaiaCalendar.today(level.getServer())) * 12 + GaiaCalendar.month(GaiaCalendar.today(level.getServer()));
                if (t.ext.getInt("auditMonth") != month && t.treasury > 10_000) {
                    return new Plan("audit", "audit", Component.translatable(o.key()));
                }
            }
            case MERIT -> {
                int unhappy = 0;
                for (Resident r : t.residents) if (r.unit >= 0 && r.loyalty < 60) unhappy++;
                if (unhappy >= 3 && t.treasury >= unhappy * 1_000L) return new Plan("reward", "reward", unhappy, Coins.format(unhappy * 1_000L));
            }
            default -> {
            }
        }
        return null;
    }

    private static boolean held(Territory t, Office o) {
        for (Resident r : t.residents) if (r.office == o.ordinal()) return true;
        return false;
    }

    /** The planner's draft, kept until the Lord decides, as a proposal. */
    private static Plan planProposal(ServerLevel level, Territory t) {
        ListTag d = Planning.draft(level, t);
        t.ext.putLong("planDay", GaiaCalendar.today(level.getServer()));
        if (d.size() < 2) return null;
        t.ext.put("planDraft", d);
        return new Plan("plan", "plan", d.size(), Planning.summary(t, d));
    }

    /** The Lord asks for a plan now: the planner draws one up and brings it to the council. */
    public static String requestPlan(ServerPlayer p, Territory t) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        Resident by = null;
        Office o = Office.WORKS;
        for (Resident r : t.residents) if (r.office == Office.WORKS.ordinal()) by = r;
        if (by == null) {
            o = Office.STEWARD;
            for (Resident r : t.residents) if (r.office == Office.STEWARD.ordinal()) by = r;
        }
        if (by == null) return "realmlord.plan.err.planner";
        ListTag l = list(t);
        for (int i = l.size() - 1; i >= 0; i--) if (l.getCompound(i).getString("key").equals("plan")) l.remove(i);
        Plan plan = planProposal(p.serverLevel(), t);
        if (plan == null) return "realmlord.plan.err.nothing";
        submit(p.serverLevel(), t, o, by, plan, GaiaCalendar.today(p.serverLevel().getServer()), p);
        return null;
    }

    private static CompoundTag submit(ServerLevel level, Territory t, Office o, Resident r, Plan plan, long day, ServerPlayer lord) {
        ListTag l = list(t);
        CompoundTag s = new CompoundTag();
        s.putInt("id", t.ext.getInt("councilNext") + 1);
        t.ext.putInt("councilNext", s.getInt("id"));
        s.putInt("office", o.ordinal());
        s.putString("by", r.name);
        s.putString("key", plan.key());
        s.putString("act", plan.act());
        ListTag args = new ListTag();
        for (Object a : plan.args()) args.add(StringTag.valueOf(a instanceof Component c ? Component.Serializer.toJson(c)
                : Component.Serializer.toJson(Component.literal(String.valueOf(a)))));
        s.put("args", args);
        s.putLong("day", day);
        if (auto(t) && !plan.act().startsWith("build:") && !plan.act().equals("upgrade") && !plan.act().equals("plan")) {
            String err = carry(level, lord, t, s);
            record(t, Component.translatable(err == null ? "realmlord.council.done_auto" : "realmlord.council.failed", r.name, text(s),
                    err == null ? "" : Component.translatable(err)));
            return s;
        }
        l.add(s);
        t.ext.put("council", l);
        if (lord != null) Notify.player(lord, offer(t, s));
        return s;
    }

    // ------------------------------------------------------------------ the officials' own people

    /** Whether the officials run the workers under their offices (on unless the Lord takes it over). */
    public static boolean manages(Territory t) {
        return !t.ext.getBoolean("councilHandsOff");
    }

    public static void setManages(Territory t, boolean on) {
        t.ext.putBoolean("councilHandsOff", !on);
    }

    /** May an official move this resident off their work? (Not militia, officials, staff, followers, trainees or figures.) */
    private static boolean movable(Territory t, Resident r, Job from) {
        return r.job == from && !r.militia && r.office < 0 && !r.follows && r.trainingAt < 0 && !Village.isStaffing(t, r) && !r.isFigure();
    }

    /** Moves up to n residents from one job to another; how many moved. */
    private static int shift(Territory t, Job from, Job to, int n) {
        int moved = 0;
        for (Resident r : t.residents) {
            if (moved >= n) break;
            if (to != Job.IDLE && to != Job.CONSTRUCTION && Village.places(t, to) <= Village.count(t, to)) break;
            if (from == Job.IDLE ? !free(t, r) : !movable(t, r, from)) continue;
            r.job = to;
            r.workplace = -1;
            moved++;
        }
        return moved;
    }

    /** The woods or the quarry, whichever the stores lack more (and has room), else idle. */
    private static Job stores(Territory t) {
        Job poor = t.wood < t.stone * 1.5 ? Job.LOGGING : Job.QUARRYING;
        if (Village.places(t, poor) > Village.count(t, poor)) return poor;
        poor = poor == Job.LOGGING ? Job.QUARRYING : Job.LOGGING;
        return Village.places(t, poor) > Village.count(t, poor) ? poor : Job.IDLE;
    }

    private static int sites(Territory t) {
        int n = 0;
        for (Building b : t.buildings) if (!b.demolishing && (!b.complete || b.damaged)) n++;
        return n;
    }

    /**
     * Each day the officials see to the people under their offices, as the novel's department
     * heads do: the head of Works keeps enough builders on the sites and shares the rest between
     * the woods and the quarry by what the stores lack; the head of Agriculture sends people to the
     * fields when the granary runs low (borrowing loggers and quarrymen if it is dire) and lets
     * them go when it overflows; the Steward finds work for the idle; the Transport head mans the
     * fishing boats. How many each moves a day goes by their ability. Each says what they did.
     */
    public static void manage(ServerLevel level, Territory t, RandomSource rnd) {
        for (Resident by : new ArrayList<>(t.residents)) {
            if (by.office < 0) continue;
            Office o = Office.values()[Math.min(by.office, Office.values().length - 1)];
            int n = 1 + o.statOf(by) / 15;
            switch (o) {
                case WORKS -> {
                    int sites = sites(t), builders = Village.count(t, Job.CONSTRUCTION);
                    int want = sites == 0 ? 2 : Math.min(20, 4 + sites * 3);
                    if (builders < want) {
                        int moved = shift(t, Job.IDLE, Job.CONSTRUCTION, Math.min(n, want - builders));
                        Job rich = t.wood >= t.stone * 1.5 ? Job.LOGGING : Job.QUARRYING;
                        if (moved < Math.min(n, want - builders)) moved += shift(t, rich, Job.CONSTRUCTION, Math.min(n, want - builders) - moved);
                        if (moved > 0) told(t, by, o, "realmlord.manage.builders", moved, sites);
                    } else if (builders > want + 2) {
                        Job poor = t.wood < t.stone * 1.5 ? Job.LOGGING : Job.QUARRYING;
                        if (Village.places(t, poor) <= Village.count(t, poor)) poor = poor == Job.LOGGING ? Job.QUARRYING : Job.LOGGING;
                        int moved = shift(t, Job.CONSTRUCTION, poor, Math.min(n, builders - want));
                        if (moved > 0) told(t, by, o, "realmlord.manage.to_stores", moved, Component.translatable(poor.key()));
                    } else {
                        // Even out the woods and the quarry by what the stores lack.
                        int logs = Village.count(t, Job.LOGGING), quarry = Village.count(t, Job.QUARRYING);
                        if (t.wood < t.stone && quarry > 2 && t.wood < 500) {
                            if (shift(t, Job.QUARRYING, Job.LOGGING, 1) > 0) told(t, by, o, "realmlord.manage.balance", Component.translatable(Job.LOGGING.key()), t.wood);
                        } else if (t.stone < t.wood / 2 && logs > 2 && t.stone < 400) {
                            if (shift(t, Job.LOGGING, Job.QUARRYING, 1) > 0) told(t, by, o, "realmlord.manage.balance", Component.translatable(Job.QUARRYING.key()), t.stone);
                        }
                    }
                }
                case AGRICULTURE -> {
                    double days = foodDays(t);
                    if (days < 15) {
                        int moved = shift(t, Job.IDLE, Job.FARMING, n);
                        if (days < 7 && moved < n) {
                            moved += shift(t, Job.HERB_GROWING, Job.FARMING, n - moved);
                            Job rich = t.wood >= t.stone ? Job.LOGGING : Job.QUARRYING;
                            if (moved < n) moved += shift(t, rich, Job.FARMING, n - moved);
                        }
                        if (moved > 0) told(t, by, o, "realmlord.manage.farmers", moved, Math.round(days));
                    } else if (days > 90 && Village.count(t, Job.FARMING) > Math.max(5, t.residents.size() / 4)) {
                        Job to = stores(t);
                        int moved = to == Job.IDLE ? 0 : shift(t, Job.FARMING, to, n);
                        if (moved > 0) told(t, by, o, "realmlord.manage.release", moved, Math.round(days), Component.translatable(to.key()));
                    }
                }
                case STEWARD -> {
                    Job j = neediest(t);
                    if (j == Job.FARMING && foodDays(t) > 60) j = sites(t) > 0 ? Job.CONSTRUCTION : stores(t);
                    if (j != null && j != Job.IDLE && idle(t) > 0) {
                        int moved = shift(t, Job.IDLE, j, n);
                        if (moved > 0) told(t, by, o, "realmlord.manage.idle", moved, Component.translatable(j.key()));
                    }
                }
                case TRANSPORT -> {
                    int room = Village.places(t, Job.FISHING) - Village.count(t, Job.FISHING);
                    if (room > 0) {
                        int moved = shift(t, Job.IDLE, Job.FISHING, Math.min(n, room));
                        if (moved > 0) told(t, by, o, "realmlord.manage.fishers", moved);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void told(Territory t, Resident by, Office o, String key, Object... args) {
        record(t, Component.translatable("realmlord.manage.by", by.name, Component.translatable(o.key()), Component.translatable(key, args)));
    }

    // ------------------------------------------------------------------ the day

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        ListTag l = list(t);
        for (int i = l.size() - 1; i >= 0; i--) if (l.getCompound(i).getLong("day") + EXPIRES <= day) l.remove(i);
        ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        for (Resident r : new ArrayList<>(t.residents)) {
            if (r.office < 0 || l.size() >= MAX) continue;
            Office o = Office.values()[Math.min(r.office, Office.values().length - 1)];
            int skill = o.statOf(r);
            if (rnd.nextInt(100) >= 35 + skill / 2) continue;
            Plan plan = think(level, t, o, r, rnd);
            if (plan == null) continue;
            boolean pending = false;
            for (Tag tg : l) if (((CompoundTag) tg).getString("key").equals(plan.key())) pending = true;
            if (pending) continue;
            submit(level, t, o, r, plan, day, lord);
            l = list(t);
        }
        if (manages(t)) manage(level, t, rnd);
        l = list(t);
        t.ext.put("council", l);
    }

    /** The proposal as said: "Fan Zhongyan (Steward): ...". */
    public static Component text(CompoundTag s) {
        ListTag args = s.getList("args", Tag.TAG_STRING);
        Object[] a = new Object[args.size()];
        for (int i = 0; i < a.length; i++) a[i] = Component.Serializer.fromJson(args.getString(i));
        return Component.translatable("realmlord.council." + s.getString("key"), a);
    }

    public static Component who(CompoundTag s) {
        Office o = Office.values()[Math.min(s.getInt("office"), Office.values().length - 1)];
        return Component.translatable("realmlord.council.who", s.getString("by"), Component.translatable(o.key()));
    }

    /** The chat message, with Approve and Dismiss to click. */
    private static Component offer(Territory t, CompoundTag s) {
        MutableComponent m = Component.empty().append(who(s).copy().withStyle(ChatFormatting.GOLD)).append(" ").append(text(s)).append(" ");
        m.append(Component.translatable("realmlord.council.approve").withStyle(st -> st.withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/realm council approve " + s.getInt("id")))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("realmlord.council.approve.tip")))));
        m.append(" ");
        m.append(Component.translatable("realmlord.council.dismiss").withStyle(st -> st.withColor(ChatFormatting.GRAY).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/realm council dismiss " + s.getInt("id")))));
        return m;
    }

    private static void record(Territory t, Component line) {
        ListTag log = log(t);
        log.add(0, StringTag.valueOf(Component.Serializer.toJson(line)));
        while (log.size() > 8) log.remove(log.size() - 1);
        t.ext.put("councilLog", log);
    }

    private static CompoundTag find(Territory t, int id) {
        for (Tag tg : list(t)) if (((CompoundTag) tg).getInt("id") == id) return (CompoundTag) tg;
        return null;
    }

    private static void drop(Territory t, int id) {
        ListTag l = list(t);
        for (int i = l.size() - 1; i >= 0; i--) if (l.getCompound(i).getInt("id") == id) l.remove(i);
        t.ext.put("council", l);
    }

    /** The Lord approves a proposal: the official carries it out. Returns an error key or null. */
    public static String approve(ServerPlayer p, Territory t, int id) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        CompoundTag s = find(t, id);
        if (s == null) return "realmlord.council.err.gone";
        String err = carry(p.serverLevel(), p, t, s);
        drop(t, id);
        record(t, Component.translatable(err == null ? "realmlord.council.done" : "realmlord.council.failed", s.getString("by"), text(s),
                err == null ? "" : Component.translatable(err)));
        if (err == null) Notify.player(p, Component.translatable("realmlord.council.carried", s.getString("by"), text(s)));
        return err;
    }

    public static String dismiss(ServerPlayer p, Territory t, int id) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        CompoundTag s = find(t, id);
        if (s == null) return "realmlord.council.err.gone";
        drop(t, id);
        // Officials whose advice is waved away grow a little less keen.
        for (Resident r : t.residents) if (r.name.equals(s.getString("by")) && r.office >= 0) r.loyalty = Math.max(0, r.loyalty - 1);
        return null;
    }

    /** Carries out a proposal. The Lord may be null (routine matters handled on the officials' own authority). */
    static String carry(ServerLevel level, ServerPlayer lord, Territory t, CompoundTag s) {
        String[] a = s.getString("act").split(":");
        switch (a[0]) {
            case "build" -> {
                BuildingType type = BuildingType.valueOf(a[1]);
                int[] at = site(level, t, type);
                if (at == null) return "realmlord.council.err.site";
                return Village.start(level, lord, t, type, new BlockPos(at[0], t.center.getY(), at[1]), at[2], null, 0);
            }
            case "plan" -> {
                return Planning.adopt(level, t);
            }
            case "jobs" -> {
                Job j = Job.valueOf(a[1]);
                int n = Integer.parseInt(a[2]), moved = 0;
                for (Resident r : t.residents) {
                    if (moved >= n || Village.places(t, j) <= Village.count(t, j)) break;
                    if (!free(t, r)) continue;
                    r.job = j;
                    r.workplace = -1;
                    moved++;
                }
                return moved == 0 ? "realmlord.council.err.nobody" : null;
            }
            case "buy_food" -> {
                long units = Long.parseLong(a[1]), cost = Market.buyCost(Market.FOOD, units);
                if (t.treasury < cost) return "realmlord.market.cannot_afford";
                t.treasury -= cost;
                t.food += units;
                return null;
            }
            case "sell_food" -> {
                long units = Math.min(Long.parseLong(a[1]), t.food);
                t.food -= units;
                t.treasury += Market.sellIncome(Market.FOOD, units);
                return null;
            }
            case "sell" -> {
                Goods g = Goods.valueOf(a[1]);
                long units = Math.min(Long.parseLong(a[2]), g.stock(t));
                if (units <= 0) return "realmlord.council.err.nothing";
                g.add(t, -units);
                t.treasury += g.sellIncome(units);
                return null;
            }
            case "militia" -> {
                int n = Integer.parseInt(a[1]), done = 0;
                for (Resident r : t.residents) {
                    if (done >= n || Village.militiaCount(t) >= Village.militiaCap(t)) break;
                    if (r.militia || r.office >= 0 || r.isFigure() || Village.isStaffing(t, r) || r.follows) continue;
                    if (Village.enlist(t, r, true) == null) done++;
                }
                return done == 0 ? "realmlord.council.err.nobody" : null;
            }
            case "mode" -> {
                Building b = t.building(Integer.parseInt(a[1]));
                if (b == null) return "realmlord.council.err.gone";
                b.mode = Integer.parseInt(a[2]);
                b.modeCarry = 0;
                return null;
            }
            case "patrol" -> {
                if (t.treasury < 1_000) return "realmlord.market.cannot_afford";
                t.treasury -= 1_000;
                t.security = Math.min(100, t.security + 10);
                t.ext.putLong("patrolUntil", GaiaCalendar.today(level.getServer()) + 5);
                return null;
            }
            case "festival" -> {
                long cost = t.residents.size() * 3L;
                if (t.treasury < cost) return "realmlord.market.cannot_afford";
                t.treasury -= cost;
                t.ext.putInt("festivalMorale", 5);
                t.peopleThoughts = Math.min(100, t.peopleThoughts + 3);
                return null;
            }
            case "audit" -> {
                int month = GaiaCalendar.year(GaiaCalendar.today(level.getServer())) * 12 + GaiaCalendar.month(GaiaCalendar.today(level.getServer()));
                t.ext.putInt("auditMonth", month);
                long found = Math.round(t.treasury * (0.01 + 0.02 * level.getRandom().nextDouble()));
                t.treasury += found;
                record(t, Component.translatable("realmlord.council.audit_found", Coins.format(found)));
                return null;
            }
            case "reward" -> {
                long paid = 0;
                for (Resident r : t.residents) if (r.unit >= 0 && r.loyalty < 60 && t.treasury >= 1_000) {
                    t.treasury -= 1_000;
                    paid += 1_000;
                    r.loyalty = Math.min(100, r.loyalty + 10);
                }
                return paid == 0 ? "realmlord.market.cannot_afford" : null;
            }
            case "upgrade" -> {
                if (lord == null) return "realmlord.council.err.lord";
                return Village.upgrade(lord, t) ? null : "realmlord.council.err.not_ready";
            }
            default -> {
                return "realmlord.council.err.gone";
            }
        }
    }

    /**
     * A good spot for a building: rings out from the manor, the building facing in towards it, the
     * first place the builders could start on. Returns {x, z, rotation} or null.
     */
    public static int[] site(ServerLevel level, Territory t, BuildingType type) {
        int[] at = site(level, t, type, true);
        return at != null ? at : site(level, t, type, false);
    }

    private static int[] site(ServerLevel level, Territory t, BuildingType type, boolean zoned) {
        int reach = Math.max(20, t.radius() - 6);
        // Fields go out beyond the wall; everything else inside it, nearest the manor first.
        Building wall = Planning.wall(t);
        int ring = wall == null ? 0 : wall.fenceRadius > 0 ? wall.fenceRadius : t.level.wall();
        int from = 14;
        if (zoned && type.fieldJob() != null && ring > 0 && ring + 10 < reach) from = ring + 8;
        for (int r = from; r <= reach; r += 3) {
            int steps = Math.max(8, (int) (2 * Math.PI * r / 4));
            int start = level.getRandom().nextInt(steps);
            for (int k = 0; k < steps; k++) {
                double ang = 2 * Math.PI * ((k + start) % steps) / steps;
                int cx = t.center.getX() + (int) Math.round(Math.cos(ang) * r), cz = t.center.getZ() + (int) Math.round(Math.sin(ang) * r);
                // The door facing the manor if it fits that way, else any way round (rotation 0
                // puts the door on the north side, 1 east, 2 south, 3 west).
                int face = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? (Math.cos(ang) > 0 ? 3 : 1) : (Math.sin(ang) > 0 ? 0 : 2);
                for (int turn = 0; turn < 4; turn++) {
                    int rot = (face + turn) & 3;
                    int sx = rot % 2 == 0 ? type.width : type.depth, sz = rot % 2 == 0 ? type.depth : type.width;
                    BlockPos o = new BlockPos(cx - sx / 2, t.center.getY(), cz - sz / 2);
                    if (!level.isLoaded(o)) break;
                    if (zoned && type.fieldJob() == null && ring > 0 && r + Math.max(sx, sz) / 2 + 4 > ring) return null;
                    if (Village.checkPlacement(level, t, type, o, rot) == null) return new int[]{o.getX(), o.getZ(), rot};
                }
            }
        }
        return null;
    }

    /** For the Council tab: the officials, what they propose, and what has been done. */
    public static List<CompoundTag> pending(Territory t) {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag tg : list(t)) out.add((CompoundTag) tg);
        return out;
    }

    static boolean town(Territory t) {
        return t.level.atLeast(TerritoryLevel.TOWN_1);
    }
}
