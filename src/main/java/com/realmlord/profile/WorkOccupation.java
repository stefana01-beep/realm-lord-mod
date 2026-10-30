package com.realmlord.profile;

/**
 * Work (life) occupations. They have no character level, only a skill level from 1 to 10,
 * and lose no experience on death. Skill levels map to the ranks the novel uses for
 * craftsmen: Junior (1-3), Intermediate (4-6), Advanced (7-9) and Master (10).
 */
public enum WorkOccupation {
    BLACKSMITH("blacksmith"),
    TAILOR("tailor"),
    PHYSICIAN("physician"),
    ARCHITECT("architect"),
    BREEDER("breeder"),
    FISHERMAN("fisherman"),
    FARMER("farmer"),
    VETERINARIAN("veterinarian"),
    WINEMAKER("winemaker"),
    COOK("cook"),
    CARPENTER("carpenter"),
    MASON("mason"),
    SHIPBUILDER("shipbuilder");

    public static final int MAX_LEVEL = 10;

    public final String id;

    WorkOccupation(String id) {
        this.id = id;
    }

    public String key() {
        return "realmlord.work." + id;
    }

    public String descKey() {
        return "realmlord.work." + id + ".desc";
    }

    public static WorkOccupation byId(int i) {
        WorkOccupation[] v = values();
        return i >= 0 && i < v.length ? v[i] : FARMER;
    }

    /** 0 Junior, 1 Intermediate, 2 Advanced, 3 Master. */
    public static int tier(int level) {
        if (level >= 10) return 3;
        if (level >= 7) return 2;
        if (level >= 4) return 1;
        return 0;
    }

    public static String tierKey(int level) {
        return "realmlord.work.tier." + tier(level);
    }

    /** Work experience needed to go from this level to the next. */
    public static int xpToNext(int level) {
        return 150 * level * level;
    }
}
