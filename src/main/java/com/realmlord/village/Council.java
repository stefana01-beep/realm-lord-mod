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
                int open = Village.openBeds(t);
                if (open < Math.max(5, t.residents.size() / 20)) {
                    BuildingType h = housing(t);
                    if (h != null && !building(t, h)) return new Plan("housing", "build:" + h.name(), open, Component.translatable(h.key()));
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
                BuildingType bt = nextRequired(t);
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
            if (auto(t) && !plan.act().startsWith("build:") && !plan.act().equals("upgrade")) {
                String err = carry(level, lord, t, s);
                record(t, Component.translatable(err == null ? "realmlord.council.done_auto" : "realmlord.council.failed", r.name, text(s),
                        err == null ? "" : Component.translatable(err)));
                continue;
            }
            l.add(s);
            if (lord != null) Notify.player(lord, offer(t, s));
        }
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
                if (lord == null) return "realmlord.council.err.lord";
                BuildingType type = BuildingType.valueOf(a[1]);
                int[] at = site(level, t, type);
                if (at == null) return "realmlord.council.err.site";
                return Village.start(lord, t, type, new BlockPos(at[0], t.center.getY(), at[1]), at[2]);
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
        int reach = Math.max(20, t.radius() - 6);
        for (int r = 14; r <= reach; r += 3) {
            int steps = Math.max(8, (int) (2 * Math.PI * r / 4));
            int start = level.getRandom().nextInt(steps);
            for (int k = 0; k < steps; k++) {
                double ang = 2 * Math.PI * ((k + start) % steps) / steps;
                int cx = t.center.getX() + (int) Math.round(Math.cos(ang) * r), cz = t.center.getZ() + (int) Math.round(Math.sin(ang) * r);
                // Facing the manor if it fits that way, else any way round.
                int face = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? (Math.cos(ang) > 0 ? 1 : 3) : (Math.sin(ang) > 0 ? 2 : 0);
                for (int turn = 0; turn < 4; turn++) {
                    int rot = (face + turn) & 3;
                    int sx = rot % 2 == 0 ? type.width : type.depth, sz = rot % 2 == 0 ? type.depth : type.width;
                    BlockPos o = new BlockPos(cx - sx / 2, t.center.getY(), cz - sz / 2);
                    if (!level.isLoaded(o)) break;
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
