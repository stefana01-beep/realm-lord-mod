package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.world.WorldState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Talents who come of their own accord. In the novel a town's fame drew people: Fan Zhongyan and
 * Bai Qi came to Shanhai through quests, and native talents like Pei Donglai, Tian Wenjing and Mulan
 * Yue offered their service once they heard of the town. Here a town's reputation brings visitors.
 *
 * <p>Once a month a town whose Lord has 300 reputation or more may receive a visitor: one time in
 * three a historical figure not yet summoned in the world (King rank, Emperor rank from 3,000
 * reputation), otherwise a wandering talent of Silver or Gold grade. They stay a month. The Lord
 * may take them into service from the Lord's Manor for a fee (two gold for a talent, twenty for a
 * King-rank figure, fifty for an Emperor-rank one).
 */
public final class Visitors {
    private Visitors() {}

    public static final long MIN_REPUTATION = 300;
    public static final long EMPEROR_REPUTATION = 3_000;

    private static final String[] TALENTS = {"Pei Donglai", "Tian Wenjing", "Mulan Yue", "Lin Yi", "Shi Jun", "Shang Shu", "Ou Ye", "Gu Hong",
            "Song Jia", "Wen Qian", "Ye Rong", "Yan Hai"};
    private static final Occupation[] TALENT_TRADES = {Occupation.OFFICER, Occupation.CIVIL_SERVANT, Occupation.MARTIAL_ARTIST, Occupation.SCHOLAR,
            Occupation.DOCTOR, Occupation.MERCHANT, Occupation.BLACKSMITH, Occupation.ALCHEMIST};

    public static boolean present(Territory t) {
        return t.ext.contains("visitorName");
    }

    public static long fee(Territory t) {
        String f = t.ext.getString("visitorFigure");
        if (f.isEmpty()) return 20_000;
        Figures.Figure fig = Figures.byId(f);
        return fig != null && fig.rank() >= 1 ? 500_000 : 200_000;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1)) return;
        if (present(t) && t.ext.getLong("visitorUntil") <= day) {
            Village.tell(level, t, Component.translatable("realmlord.visitor.left", t.ext.getString("visitorName"), t.name));
            clear(t);
        }
        int month = GaiaCalendar.year(day) * 12 + GaiaCalendar.month(day);
        if (present(t) || t.ext.getInt("visitorMonth") == month) return;
        t.ext.putInt("visitorMonth", month);
        ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (lord != null) t.ext.putLong("lordRep", Profiles.get(lord).getReputation());
        long rep = t.ext.getLong("lordRep");
        if (rep < MIN_REPUTATION || rnd.nextInt(2) != 0) return;
        Figures.Figure fig = null;
        if (rnd.nextInt(3) == 0) fig = Figures.pick(WorldState.get(level.getServer()), rep >= EMPEROR_REPUTATION && rnd.nextBoolean() ? 1 : 0, rnd);
        if (fig != null && fig.rank() > (rep >= EMPEROR_REPUTATION ? 1 : 0)) fig = null;
        if (fig != null) {
            t.ext.putString("visitorFigure", fig.id());
            t.ext.putString("visitorName", fig.name());
        } else {
            t.ext.putString("visitorFigure", "");
            t.ext.putString("visitorName", TALENTS[rnd.nextInt(TALENTS.length)]);
            t.ext.putInt("visitorTrade", TALENT_TRADES[rnd.nextInt(TALENT_TRADES.length)].ordinal());
            t.ext.putInt("visitorGrade", rnd.nextInt(4) == 0 ? 3 : 2);
        }
        t.ext.putLong("visitorUntil", day + GaiaCalendar.daysPerMonth());
        Village.tell(level, t, Component.translatable("realmlord.visitor.came", t.ext.getString("visitorName"), t.name, Coins.format(fee(t))));
    }

    private static void clear(Territory t) {
        for (String k : new String[]{"visitorFigure", "visitorName", "visitorTrade", "visitorGrade", "visitorUntil"}) t.ext.remove(k);
    }

    /** The Lord takes the visitor into service. Returns an error key or null. */
    public static String recruit(ServerPlayer p, Territory t) {
        if (!present(t)) return "realmlord.visitor.err.none";
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        if (Village.openBeds(t) <= 0) return "realmlord.visitor.err.beds";
        String fid0 = t.ext.getString("visitorFigure");
        if (!fid0.isEmpty() && WorldState.get(p.server).summoned.contains(fid0)) {
            // Another Lord won them over while they were here.
            clear(t);
            return "realmlord.visitor.err.taken";
        }
        long fee = fee(t);
        if (!Coins.payWithTreasury(p, fee)) return "realmlord.market.cannot_afford";
        RandomSource rnd = p.getRandom();
        Resident r;
        String fid = t.ext.getString("visitorFigure");
        if (!fid.isEmpty() && Figures.byId(fid) != null) {
            WorldState w = WorldState.get(p.server);
            w.summoned.add(fid);
            w.touch();
            r = Figures.resident(Figures.byId(fid), rnd);
        } else {
            r = Resident.random(rnd, Occupation.byId(t.ext.getInt("visitorTrade")), t.grade.ordinal());
            r.name = t.ext.getString("visitorName");
            r.grade = t.ext.getInt("visitorGrade");
            r.rank = 2;
            r.loyalty = 80;
            int boost = r.grade == 3 ? 25 : 15;
            r.command = Math.min(99, r.command + boost);
            r.force = Math.min(99, r.force + boost);
            r.intelligence = Math.min(99, r.intelligence + boost);
            r.politics = Math.min(99, r.politics + boost);
        }
        Village.add(t, r);
        Notify.player(p, Component.translatable("realmlord.visitor.joined", r.name, t.name));
        clear(t);
        return null;
    }
}
