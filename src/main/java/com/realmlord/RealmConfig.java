package com.realmlord;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-wide settings, stored in config/realmlord-common.toml. */
public final class RealmConfig {
    private RealmConfig() {}

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue LORD_RESTRICTION;
    public static final ForgeConfigSpec.IntValue LORD_ROAM_MARGIN;
    public static final ForgeConfigSpec.IntValue TICKS_PER_NOVEL_HOUR;
    public static final ForgeConfigSpec.DoubleValue QUEST_FAIL_DAYS;
    public static final ForgeConfigSpec.DoubleValue QUEST_GOLD_DAYS;
    public static final ForgeConfigSpec.DoubleValue QUEST_SILVER_DAYS;
    public static final ForgeConfigSpec.DoubleValue QUEST_BLACK_IRON_DAYS;
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DAYS_PER_MONTH;
    public static final ForgeConfigSpec.IntValue VISIBLE_RESIDENTS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("lords");
        LORD_RESTRICTION = b.comment("The novel's rule: Lords may not leave their territory in the main world (other dimensions, such as battlefields, are exempt). Off by default.")
                .define("lordRestriction", false);
        LORD_ROAM_MARGIN = b.comment("With the restriction on, how many blocks past the territory border a Lord may wander before being sent home.")
                .defineInRange("lordRoamMargin", 16, 0, 512);
        b.pop();

        b.push("time");
        TICKS_PER_NOVEL_HOUR = b.comment("Game ticks per hour of novel time, used for construction times and other timers. 1200 ticks = 1 real minute, so a 24-hour building takes about 24 minutes.")
                .defineInRange("ticksPerNovelHour", 1200, 20, 72000);
        DAYS_PER_MONTH = b.comment("Novel days in each month of the Gaia calendar. Sowing happens in months 4 and 7, harvest three months later. With 2, a year is 24 novel days (about 10 real hours).")
                .defineInRange("daysPerMonth", 2, 1, 30);
        b.pop();

        b.push("village");
        VISIBLE_RESIDENTS = b.comment("Most residents of one territory shown walking around at once; the rest live on in the records. Talents are always shown first.")
                .defineInRange("visibleResidents", 24, 4, 200);
        b.pop();

        b.push("village_creation_quest");
        QUEST_FAIL_DAYS = b.comment("Minecraft days a Lord has to finish the Village Creation quest before losing the right to found a village.")
                .defineInRange("daysToComplete", 10.0, 0.5, 100.0);
        QUEST_GOLD_DAYS = b.comment("Finish within this many days for a Gold token. Only one Gold token exists per world; later fast finishes get Silver.")
                .defineInRange("goldWithinDays", 3.0, 0.1, 100.0);
        QUEST_SILVER_DAYS = b.comment("Finish within this many days for a Silver token.")
                .defineInRange("silverWithinDays", 5.0, 0.1, 100.0);
        QUEST_BLACK_IRON_DAYS = b.comment("Finish within this many days for a Black Iron token. Anything slower (before the deadline) earns Bronze.")
                .defineInRange("blackIronWithinDays", 7.5, 0.1, 100.0);
        b.pop();

        b.push("progression");
        XP_MULTIPLIER = b.comment("Multiplier for character and occupation experience.")
                .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        b.pop();
        SPEC = b.build();
    }

    /** Safe reads: game tests and early code can run before the config file loads. */
    public static boolean lordRestriction() {
        return SPEC.isLoaded() ? LORD_RESTRICTION.get() : false;
    }

    public static int roamMargin() {
        return SPEC.isLoaded() ? LORD_ROAM_MARGIN.get() : 16;
    }

    public static int ticksPerNovelHour() {
        return SPEC.isLoaded() ? TICKS_PER_NOVEL_HOUR.get() : 1200;
    }

    public static double questFailDays() {
        return SPEC.isLoaded() ? QUEST_FAIL_DAYS.get() : 10.0;
    }

    public static double questGoldDays() {
        return SPEC.isLoaded() ? QUEST_GOLD_DAYS.get() : 3.0;
    }

    public static double questSilverDays() {
        return SPEC.isLoaded() ? QUEST_SILVER_DAYS.get() : 5.0;
    }

    public static double questBlackIronDays() {
        return SPEC.isLoaded() ? QUEST_BLACK_IRON_DAYS.get() : 7.5;
    }

    public static int daysPerMonth() {
        return SPEC.isLoaded() ? DAYS_PER_MONTH.get() : 2;
    }

    public static int visibleResidents() {
        return SPEC.isLoaded() ? VISIBLE_RESIDENTS.get() : 24;
    }

    public static long ticksPerNovelDay() {
        return 24L * ticksPerNovelHour();
    }

    public static double xpMultiplier() {
        return SPEC.isLoaded() ? XP_MULTIPLIER.get() : 1.0;
    }
}
