package com.realmlord.village;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Refugees. In the novel the towns drew refugees by the thousand (from the beast tides, the
 * wars between lords, the mountains); Shanhai set up a refugee camp outside the walls, screened
 * them for raiders, and took them in household by household.
 *
 * <p>From Grade 1 Town a column of refugees comes every month, more for a town whose People's
 * Thoughts are high and whose Internal Affairs Division is well led. They camp outside the walls
 * and eat a little from the granary while they wait; each day as many move in as there are beds,
 * up to ten plus a fifth of the Internal Affairs head's Politics. If People's Thoughts fall below
 * 40 they give up and move on. Submitted mountain barbarians wait in the same camp.
 */
public final class Refugees {
    private Refugees() {}

    public static int waiting(Territory t) {
        return t.ext.getInt("refugees") + t.ext.getInt("mountainRefugees");
    }

    public static void add(Territory t, int n, boolean mountain) {
        String k = mountain ? "mountainRefugees" : "refugees";
        t.ext.putInt(k, t.ext.getInt(k) + n);
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1)) return;
        int month = GaiaCalendar.year(day) * 12 + GaiaCalendar.month(day);
        if (t.ext.getInt("refugeeMonth") != month && t.peopleThoughts >= 50) {
            t.ext.putInt("refugeeMonth", month);
            int n = (int) Math.round((40 + rnd.nextInt(110)) * Office.boost(t, Office.INTERNAL_AFFAIRS, 150) * (t.peopleThoughts / 70.0));
            add(t, n, false);
            Village.tell(level, t, Component.translatable("realmlord.refugees.arrived", n, t.name));
        }
        int waiting = waiting(t);
        if (waiting <= 0) return;
        if (t.peopleThoughts < 40) {
            t.ext.remove("refugees");
            if (t.ext.getInt("mountainRefugees") > 0) t.ext.putInt("mountainRefugees", t.ext.getInt("mountainRefugees") / 2);
            Village.tell(level, t, Component.translatable("realmlord.refugees.left", t.name));
            return;
        }
        t.food = Math.max(0, t.food - waiting);
        int take = Math.min(Village.openBeds(t), 10 + Office.power(t, Office.INTERNAL_AFFAIRS) / 5);
        int moved = 0;
        while (moved < take && t.ext.getInt("mountainRefugees") > 0) {
            Resident r = Village.add(t, mountaineer(rnd));
            r.loyalty = 50 + rnd.nextInt(20);
            t.ext.putInt("mountainRefugees", t.ext.getInt("mountainRefugees") - 1);
            moved++;
        }
        while (moved < take && t.ext.getInt("refugees") > 0) {
            Village.add(t, Resident.random(rnd, Resident.randomOccupation(rnd, 1.0), t.grade.ordinal()));
            t.ext.putInt("refugees", t.ext.getInt("refugees") - 1);
            moved++;
        }
        if (moved > 0) Village.tell(level, t, Component.translatable("realmlord.refugees.settled", moved, t.name, waiting(t)));
    }

    /** A mountain barbarian: a hunter, strong and hardy, ready to be an elite warrior. */
    public static Resident mountaineer(RandomSource rnd) {
        Resident r = Resident.random(rnd, rnd.nextInt(3) == 0 ? Occupation.FARMER : Occupation.HUNTER, 0);
        r.mountain = true;
        r.force = Math.max(r.force, 55 + rnd.nextInt(21));
        r.command = Math.max(r.command, 30 + rnd.nextInt(20));
        return r;
    }
}
