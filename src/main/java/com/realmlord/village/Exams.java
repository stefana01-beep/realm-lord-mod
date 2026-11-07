package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.item.SummoningTokenItem;
import com.realmlord.territory.Territory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * The imperial examination. Ouyang Shuo's ministers wanted it from the start ("to cultivate
 * talents we must run the basic education system and implement the imperial examination
 * system"), and in time Shanhai held its own. Here it is held once a year, in the eighth month,
 * for the village's scholars and civil servants:
 *
 * <ul>
 * <li>the county examination makes a <b>Xiucai</b> (needs a private school or an academy);</li>
 * <li>the provincial examination makes a Xiucai a <b>Juren</b> (needs an academy);</li>
 * <li>the metropolitan and palace examinations make a Juren a <b>Jinshi</b> (needs an academy);</li>
 * <li>and the year's best Jinshi may be named <b>Zhuangyuan</b>, first of the palace
 * examination: the Lord is granted merit and a King-rank Summoning Token.</li>
 * </ul>
 *
 * <p>The cleverer the candidate the likelier they pass; studying at the academy and scholars among
 * the summoned figures help. Each degree raises Intelligence and Politics; Juren and Jinshi rise in
 * rank; degree holders serving the village raise its taxes, and Jinshi its People's Thoughts.
 */
public final class Exams {
    private Exams() {}

    public static final String[] DEGREES = {"", "xiucai", "juren", "jinshi", "zhuangyuan"};
    public static final int EXAM_MONTH = 8;
    public static final long ZHUANGYUAN_MERIT = 500;

    public static int level(Resident r) {
        for (int i = 0; i < DEGREES.length; i++) if (DEGREES[i].equals(r.degree)) return i;
        return 0;
    }

    public static boolean candidate(Resident r) {
        return !r.isFigure() && (r.occupation == Occupation.SCHOLAR || r.occupation == Occupation.CIVIL_SERVANT) && level(r) < 3;
    }

    public static int holders(Territory t, int atLeast) {
        int n = 0;
        for (Resident r : t.residents) if (level(r) >= atLeast) n++;
        return n;
    }

    public static double taxBonus(Territory t) {
        double b = 0;
        for (Resident r : t.residents) {
            int lv = level(r);
            b += lv == 2 ? 0.01 : lv == 3 ? 0.03 : lv == 4 ? 0.05 : 0;
        }
        return 1.0 + Math.min(0.3, b);
    }

    public static int morale(Territory t) {
        return Math.min(5, holders(t, 3));
    }

    /** The chance a candidate passes the next examination. */
    public static double chance(Territory t, Resident r) {
        int lv = level(r);
        boolean academy = Village.countComplete(t, BuildingType.ACADEMY) > 0;
        if (lv == 0 && !academy && Village.countComplete(t, BuildingType.PRIVATE_SCHOOL) == 0) return 0;
        if (lv >= 1 && !academy) return 0;
        double base = lv == 0 ? 0.25 : lv == 1 ? 0.12 : 0.06;
        double p = base + (r.intelligence - 50 - 10 * lv) / 100.0;
        Building at = r.trainingAt >= 0 ? t.building(r.trainingAt) : null;
        if (at != null && at.type == BuildingType.ACADEMY) {
            Workshops.Mode m = Workshops.mode(at);
            p += m != null && m.id().equals("exam_prep") ? 0.25 : 0.15;
        }
        else if (academy) p += 0.05;
        p += 0.05 * FigureArts.count(t, "scholar");
        return Math.max(0.02, Math.min(0.85, p));
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (GaiaCalendar.month(day) != EXAM_MONTH) return;
        int year = GaiaCalendar.year(day);
        if (t.ext.getInt("examYear") == year) return;
        t.ext.putInt("examYear", year);
        int[] passed = new int[4];
        Resident best = null;
        for (Resident r : t.residents) {
            if (!candidate(r)) continue;
            double p = chance(t, r);
            if (p <= 0 || rnd.nextDouble() >= p) continue;
            int lv = level(r) + 1;
            r.degree = DEGREES[lv];
            passed[lv]++;
            int gain = lv == 1 ? 2 : lv == 2 ? 3 : 5;
            r.intelligence = Math.min(99, r.intelligence + gain);
            r.politics = Math.min(99, r.politics + gain);
            if (lv >= 2) r.rank = Math.max(r.rank, lv - 1);
            if (r.grade < 0) r.grade = 0;
            r.loyalty = Math.min(100, r.loyalty + 5);
            if (lv == 3 && (best == null || r.intelligence + r.politics > best.intelligence + best.politics)) best = r;
        }
        if (passed[1] + passed[2] + passed[3] == 0) {
            if (hasCandidates(t)) Village.tell(level, t, Component.translatable("realmlord.exam.none", t.name, year));
            return;
        }
        Village.tell(level, t, Component.translatable("realmlord.exam.results", t.name, year, passed[1], passed[2], passed[3]));
        if (best != null && rnd.nextDouble() < 0.25 + 0.05 * FigureArts.count(t, "scholar")) {
            best.degree = DEGREES[4];
            best.intelligence = Math.min(99, best.intelligence + 3);
            best.grade = Math.max(best.grade, 2);
            Village.tell(level, t, Component.translatable("realmlord.exam.zhuangyuan", best.name, t.name));
            Notify.world(level.getServer(), Component.translatable("realmlord.exam.zhuangyuan.world", best.name, t.name));
            t.storage.addItem(SummoningTokenItem.of(0));
            ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
            if (lord != null) {
                com.realmlord.profile.Profiles.grantMerit(lord, ZHUANGYUAN_MERIT, Component.translatable("realmlord.exam.merit"));
                Notify.player(lord, Component.translatable("realmlord.exam.token", t.name));
            }
        }
    }

    private static boolean hasCandidates(Territory t) {
        for (Resident r : t.residents) if (candidate(r) && chance(t, r) > 0) return true;
        return false;
    }
}
