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

    /** A novel day is a Minecraft day: 24,000 ticks, from one sunrise to the next. */
    public static final long TICKS_PER_DAY = 24000L;

    /** The day number for a count of ticks. */
    public static long day(long ticks) {
        return ticks / TICKS_PER_DAY;
    }

    /**
     * Today's novel day, from the time of day in the main world, so sleeping through the night
     * (or /time) brings the next day at once. If the daylight cycle is switched off, the days
     * follow the world's running time instead, so villages still live on.
     */
    public static long today(net.minecraft.server.MinecraftServer server) {
        net.minecraft.server.level.ServerLevel w = server.overworld();
        boolean cycle = w.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        return day(cycle ? w.getDayTime() : w.getGameTime());
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
