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
    public static final ForgeConfigSpec.DoubleValue QUEST_BLACK_IRON_DAYS;
    public static final ForgeConfigSpec.DoubleValue QUEST_SILVER_DAYS;
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;

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
        b.pop();

        b.push("village_creation_quest");
        QUEST_FAIL_DAYS = b.comment("Minecraft days a Lord has to finish the Village Creation quest before losing the right to found a village.")
                .defineInRange("failDays", 7.0, 0.5, 100.0);
        QUEST_BLACK_IRON_DAYS = b.comment("Finish within this many days to earn at least a Black Iron token.")
                .defineInRange("blackIronDays", 3.0, 0.1, 100.0);
        QUEST_SILVER_DAYS = b.comment("Finish within this many days, without dying, and find the hidden cache to earn a Silver token (Gold for the first in the world).")
                .defineInRange("silverDays", 1.5, 0.1, 100.0);
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
        return SPEC.isLoaded() ? QUEST_FAIL_DAYS.get() : 7.0;
    }

    public static double questBlackIronDays() {
        return SPEC.isLoaded() ? QUEST_BLACK_IRON_DAYS.get() : 3.0;
    }

    public static double questSilverDays() {
        return SPEC.isLoaded() ? QUEST_SILVER_DAYS.get() : 1.5;
    }

    public static double xpMultiplier() {
        return SPEC.isLoaded() ? XP_MULTIPLIER.get() : 1.0;
    }
}
