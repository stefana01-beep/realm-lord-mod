package com.realmlord.village;

import java.util.ArrayList;
import java.util.List;

/**
 * Every village building from the novel, with its construction conditions: the village grade
 * that unlocks it, whether it needs a blueprint, the specialist who must be present, timber,
 * stone and build time in novel hours. Footprints, jobs and effects are the mod's.
 *
 * <p>requiredFor is the village grade (1 or 2) whose upgrade needs this building complete;
 * requiredCount is how many (the novel's Grade 2 village had ten residential courtyards).
 */
public enum BuildingType {
    // ---------------------------------------------------------------- Grade 1 village
    SIMPLE_FENCE(1, false, null, 0, 100, 0, 24, 0, 0, Style.FENCE, 1, 1),
    DINING_HALL(1, true, null, 0, 30, 20, 12, 9, 7, Style.HALL, 1, 1),
    BATHROOM(1, true, null, 0, 10, 10, 6, 5, 5, Style.HUT, 1, 1),
    SMALL_RESIDENTIAL_COURTYARD(1, true, null, 0, 40, 20, 12, 17, 15, Style.COURTYARD, 1, 10),
    BLACKSMITH_WORKSHOP(1, true, Occupation.BLACKSMITH, 0, 20, 10, 12, 7, 7, Style.WORKSHOP, 1, 1),
    GROCERY_SHOP(1, true, Occupation.GROCER, 0, 20, 10, 12, 7, 6, Style.SHOP, 1, 1),
    LOGGING_FIELD(1, false, null, 0, 30, 0, 6, 11, 11, Style.LOGGING, 1, 1),
    QUARRY(1, false, null, 0, 20, 10, 6, 9, 9, Style.QUARRY, 1, 1),
    FARMLAND(1, false, null, 0, 0, 0, 6, 13, 13, Style.FARM, 1, 1),
    MILITARY_CAMP(1, true, Occupation.OFFICER, 0, 100, 40, 24, 11, 9, Style.CAMP, 1, 1),
    // ---------------------------------------------------------------- Grade 2 village
    ANCESTRAL_HALL(2, true, null, 0, 80, 40, 24, 9, 9, Style.TEMPLE, 2, 1),
    CLINIC(2, true, Occupation.DOCTOR, 0, 60, 20, 24, 9, 7, Style.HALL, 2, 1),
    PRIVATE_SCHOOL(2, true, Occupation.SCHOLAR, 0, 150, 80, 48, 11, 9, Style.HALL, 2, 1),
    MARTIAL_ARTS_DOJO(2, true, Occupation.MARTIAL_ARTIST, 0, 100, 100, 48, 11, 11, Style.DOJO, 2, 1),
    GRANARY(2, true, null, 0, 400, 200, 72, 9, 9, Style.GRANARY, 2, 1),
    TAILOR_WORKSHOP(2, true, Occupation.TAILOR, 0, 40, 20, 24, 7, 7, Style.WORKSHOP, 2, 1),
    MINING_FIELD(2, false, null, 0, 40, 20, 24, 9, 9, Style.MINE, 2, 1),
    ARROW_TOWER(2, false, null, 0, 80, 10, 24, 3, 3, Style.TOWER, 2, 1),
    STONE_OUTPOST(2, false, null, 0, 20, 200, 24, 5, 5, Style.OUTPOST, 0, 0),
    MEDIUM_RESIDENTIAL_COURTYARD(2, true, null, 0, 120, 80, 24, 21, 19, Style.COURTYARD, 0, 0),
    // ---------------------------------------------------------------- Grade 3 village
    TEMPLE(3, true, null, 0, 400, 350, 48, 11, 11, Style.TEMPLE, 3, 1),
    WEAPONS_WORKSHOP(3, true, Occupation.BLACKSMITH, 2, 400, 200, 48, 9, 9, Style.WORKSHOP, 3, 1),
    POTTERY_WORKSHOP(3, true, Occupation.POTTER, 0, 200, 500, 48, 7, 7, Style.WORKSHOP, 3, 1),
    MILL(3, false, null, 0, 150, 100, 24, 5, 5, Style.HUT, 3, 1),
    MULBERRY_SHED(3, false, null, 0, 400, 100, 48, 9, 7, Style.SHED, 3, 1),
    WINE_WORKSHOP(3, true, Occupation.WINEMAKER, 0, 400, 200, 48, 9, 7, Style.WORKSHOP, 3, 1),
    SENTRY_POST(3, false, null, 0, 40, 200, 24, 3, 3, Style.TOWER, 3, 1),
    STABLE(3, true, null, 0, 200, 50, 24, 11, 7, Style.SHED, 3, 1),
    LARGE_RESIDENTIAL_COURTYARD(3, true, null, 0, 250, 200, 48, 27, 25, Style.COURTYARD, 0, 0),
    // ---------------------------------------------------------------- laid on the map, any grade
    PATH(1, false, null, 0, 0, 0, 1, 0, 0, Style.PATH, 0, 0),
    /** Ground the builders clear and level on the Lord's order. */
    CLEARING(1, false, null, 0, 0, 0, 1, 0, 0, Style.CLEARING, 0, 0);

    public enum Style { PATH, CLEARING, FENCE, HALL, HUT, COURTYARD, WORKSHOP, SHOP, LOGGING, QUARRY, FARM, MINE, CAMP, TEMPLE, DOJO, GRANARY, TOWER, OUTPOST, SHED }

    /** Village grade (1-3) that unlocks it. */
    public final int grade;
    public final boolean blueprint;
    public final Occupation specialist;
    /** Minimum craft rank of the specialist: 0 Junior, 2 Advanced. */
    public final int specialistRank;
    public final int wood, stone, hours;
    public final int width, depth;
    public final Style style;
    /** The upgrade (from this village grade) that requires it, or 0. */
    public final int requiredFor;
    public final int requiredCount;

    BuildingType(int grade, boolean blueprint, Occupation specialist, int specialistRank, int wood, int stone, int hours,
                 int width, int depth, Style style, int requiredFor, int requiredCount) {
        this.grade = grade;
        this.blueprint = blueprint;
        this.specialist = specialist;
        this.specialistRank = specialistRank;
        this.wood = wood;
        this.stone = stone;
        this.hours = hours;
        this.width = width;
        this.depth = depth;
        this.style = style;
        this.requiredFor = requiredFor;
        this.requiredCount = requiredCount;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String key() {
        return "realmlord.building." + id();
    }

    public String descKey() {
        return "realmlord.building." + id() + ".desc";
    }

    /** Placed by the player on the map (the fence is built around the border instead). */
    public boolean placed() {
        return this != SIMPLE_FENCE && this != PATH && this != CLEARING;
    }

    /** Not one of the novel's buildings: kept out of the building lists (paths are laid from the map). */
    public boolean hidden() {
        return this == PATH || this == CLEARING;
    }

    // ---------------------------------------------------------------- what it does once built

    public int housing() {
        return switch (this) {
            case SMALL_RESIDENTIAL_COURTYARD -> 20;
            case MEDIUM_RESIDENTIAL_COURTYARD -> 50;
            case LARGE_RESIDENTIAL_COURTYARD -> 100;
            default -> 0;
        };
    }

    /** The field job it provides, and how many places. */
    public Job fieldJob() {
        return switch (this) {
            case LOGGING_FIELD -> Job.LOGGING;
            case QUARRY -> Job.QUARRYING;
            case FARMLAND -> Job.FARMING;
            case MINING_FIELD -> Job.MINING;
            default -> null;
        };
    }

    public int fieldPlaces() {
        return this == FARMLAND ? 5 : fieldJob() != null ? 10 : 0;
    }

    /** The occupation that staffs it (one person), or null. */
    public Occupation staff() {
        return switch (this) {
            case DINING_HALL -> Occupation.COOK;
            case BLACKSMITH_WORKSHOP, WEAPONS_WORKSHOP -> Occupation.BLACKSMITH;
            case GROCERY_SHOP -> Occupation.GROCER;
            case MILITARY_CAMP -> Occupation.OFFICER;
            case CLINIC -> Occupation.DOCTOR;
            case PRIVATE_SCHOOL -> Occupation.SCHOLAR;
            case MARTIAL_ARTS_DOJO -> Occupation.MARTIAL_ARTIST;
            case TAILOR_WORKSHOP -> Occupation.TAILOR;
            case POTTERY_WORKSHOP -> Occupation.POTTER;
            case WINE_WORKSHOP -> Occupation.WINEMAKER;
            case STABLE -> Occupation.STABLEMAN;
            default -> null;
        };
    }

    /** {political, economic, cultural, military} added when built; staffed adds as much again. */
    public int[] indexes() {
        return switch (this) {
            case SIMPLE_FENCE -> new int[]{0, 0, 0, 5};
            case DINING_HALL -> new int[]{3, 0, 0, 0};
            case BATHROOM -> new int[]{2, 0, 0, 0};
            case SMALL_RESIDENTIAL_COURTYARD, MEDIUM_RESIDENTIAL_COURTYARD, LARGE_RESIDENTIAL_COURTYARD -> new int[]{0, 0, 0, 0};
            case BLACKSMITH_WORKSHOP -> new int[]{0, 3, 0, 2};
            case GROCERY_SHOP -> new int[]{0, 4, 0, 0};
            case LOGGING_FIELD, QUARRY, FARMLAND, MINING_FIELD -> new int[]{0, 1, 0, 0};
            case MILITARY_CAMP -> new int[]{0, 0, 0, 5};
            case ANCESTRAL_HALL -> new int[]{3, 0, 5, 0};
            case CLINIC -> new int[]{2, 0, 2, 0};
            case PRIVATE_SCHOOL -> new int[]{0, 0, 6, 0};
            case MARTIAL_ARTS_DOJO -> new int[]{0, 0, 2, 4};
            case GRANARY -> new int[]{0, 3, 0, 0};
            case TAILOR_WORKSHOP -> new int[]{0, 3, 0, 0};
            case ARROW_TOWER, STONE_OUTPOST, SENTRY_POST -> new int[]{0, 0, 0, 3};
            case TEMPLE -> new int[]{2, 0, 6, 0};
            case WEAPONS_WORKSHOP -> new int[]{0, 2, 0, 4};
            case POTTERY_WORKSHOP, WINE_WORKSHOP -> new int[]{0, 4, 0, 0};
            case MILL, MULBERRY_SHED -> new int[]{0, 3, 0, 0};
            case STABLE -> new int[]{0, 0, 0, 3};
            case PATH, CLEARING -> new int[]{0, 0, 0, 0};
        };
    }

    /** People's Thoughts the building supports. */
    public int morale() {
        return switch (this) {
            case DINING_HALL, CLINIC, TEMPLE -> 5;
            case BATHROOM, ANCESTRAL_HALL -> 3;
            default -> 0;
        };
    }

    /** Security (Law and Order) the building adds. */
    public int security() {
        return switch (this) {
            case SIMPLE_FENCE -> 10;
            case ARROW_TOWER, STONE_OUTPOST, SENTRY_POST -> 5;
            case MILITARY_CAMP -> 5;
            default -> 0;
        };
    }

    /** Blueprint price in copper: 2 gold for basic village plans, 5 gold for Grade 2, 10 gold for Grade 3. */
    public long blueprintPrice() {
        return switch (grade) {
            case 1 -> 20_000L;
            case 2 -> 50_000L;
            default -> 100_000L;
        };
    }

    public static List<BuildingType> ofGrade(int g) {
        List<BuildingType> out = new ArrayList<>();
        for (BuildingType t : values()) if (t.grade == g && !t.hidden()) out.add(t);
        return out;
    }

    public static BuildingType byId(String id) {
        for (BuildingType t : values()) if (t.id().equals(id)) return t;
        return null;
    }

    public static BuildingType byOrdinal(int i) {
        BuildingType[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
