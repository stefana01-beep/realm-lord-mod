package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * The Recruitment Hall. In the novel it "recruits one people with special talents once a month":
 * when no alchemist would come to Shanhai, Fan Zhongyan reminded Ouyang Shuo of the hall, and for
 * ten gold it brought an advanced alchemist.
 *
 * <p>Once a month each Recruitment Hall, kept by a civil servant, recruits one talent of the trade
 * chosen in its window: an Advanced (one time in five a Master) specialist with a talent grade, for ten
 * gold from the treasury. They need a bed to come to.
 */
public final class Recruits {
    private Recruits() {}

    public static final long COST = 100_000;
    public static final Occupation[] TRADES = {Occupation.ALCHEMIST, Occupation.SCHOLAR, Occupation.DOCTOR, Occupation.OFFICER,
            Occupation.BLACKSMITH, Occupation.MERCHANT, Occupation.SHIPBUILDER, Occupation.PRIEST, Occupation.MARTIAL_ARTIST,
            Occupation.CIVIL_SERVANT, Occupation.CARPENTER, Occupation.MASON};

    public static Occupation wanted(Building b) {
        return TRADES[Math.max(0, Math.min(TRADES.length - 1, b.mode))];
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        int month = GaiaCalendar.year(day) * 12 + GaiaCalendar.month(day);
        for (Building b : t.buildings) {
            if (b.type != BuildingType.RECRUITMENT_HALL || !b.complete || b.demolishing || Village.staffOf(t, b) == null) continue;
            String k = "recruited" + b.id;
            if (t.ext.getInt(k) == month) continue;
            if (t.treasury < COST || Village.openBeds(t) <= 0) continue;
            t.ext.putInt(k, month);
            t.treasury -= COST;
            Resident r = Resident.random(rnd, wanted(b), t.grade.ordinal());
            r.rank = rnd.nextInt(5) == 0 ? 3 : 2;
            if (r.grade < 0) r.grade = rnd.nextInt(3);
            r.loyalty = Math.max(r.loyalty, 70);
            Village.add(t, r);
            Village.tell(level, t, Component.translatable("realmlord.recruit.came", r.name,
                    Component.translatable("realmlord.work.tier." + r.rank), Component.translatable(r.occupation.key()), t.name, Coins.format(COST)));
        }
    }
}
