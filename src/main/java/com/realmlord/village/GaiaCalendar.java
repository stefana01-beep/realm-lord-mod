package com.realmlord.village;

import com.realmlord.RealmConfig;

/**
 * The novel's Gaia calendar, starting at Year 1, January 1. Grain is sown at Qingming (month
 * 4) and in early July (month 7); a sowing missed by a month or more yields 10-50% less.
 */
public final class GaiaCalendar {
    private GaiaCalendar() {}

    public static final int[] SOWING_MONTHS = {4, 7};
    public static final int GROWING_MONTHS = 3;

    public static long day(long gameTime) {
        return gameTime / RealmConfig.ticksPerNovelDay();
    }

    public static int year(long day) {
        return (int) (day / (12L * RealmConfig.daysPerMonth())) + 1;
    }

    public static int month(long day) {
        return (int) ((day / RealmConfig.daysPerMonth()) % 12) + 1;
    }

    public static int dayOfMonth(long day) {
        return (int) (day % RealmConfig.daysPerMonth()) + 1;
    }

    public static long daysPerMonth() {
        return RealmConfig.daysPerMonth();
    }

    /** Months between this month and the nearest sowing month. */
    public static int monthsFromSowing(int month) {
        int best = 12;
        for (int m : SOWING_MONTHS) {
            int d = Math.abs(month - m);
            best = Math.min(best, Math.min(d, 12 - d));
        }
        return best;
    }

    /** Yield for crops sown this month: full in a sowing month, 10-50% less the further off. */
    public static float sowingYield(int month) {
        return switch (monthsFromSowing(month)) {
            case 0 -> 1.0F;
            case 1 -> 0.9F;
            case 2 -> 0.75F;
            default -> 0.5F;
        };
    }

    public static boolean isSowingMonth(int month) {
        return monthsFromSowing(month) == 0;
    }
}
