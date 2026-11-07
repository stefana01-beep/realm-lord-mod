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
    BARRACKS(2, true, null, 0, 200, 120, 24, 11, 9, Style.CAMP, 0, 0),
    /** Hidden: revealed to a village by the sea. */
    MAZU_TEMPLE(2, false, null, 0, 300, 250, 48, 11, 11, Style.TEMPLE, 0, 0),
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
    // ---------------------------------------------------------------- more of the novel's buildings
    /** The village temple: a Taoist priest presides, blesses the village, refines pills and writes talismans. */
    VILLAGE_TEMPLE(1, true, null, 0, 120, 80, 24, 9, 9, Style.DESIGN, 0, 0),
    /** The dock: a pier for the village's boats; sailors are recruited here. */
    DOCK(2, true, null, 0, 400, 200, 72, 5, 14, Style.DESIGN, 0, 0),
    /** The shipyard: its shipbuilder builds a fishing boat a day, warships more slowly. */
    SHIPYARD(2, true, Occupation.SHIPBUILDER, 0, 100, 50, 48, 11, 16, Style.DESIGN, 0, 0),
    /** Salt fields: sea water dried in pans by the shore. */
    SALT_FIELD(2, true, null, 0, 60, 40, 24, 11, 11, Style.DESIGN, 0, 0),
    COPPER_MINE(2, false, null, 0, 60, 40, 24, 9, 9, Style.MINE, 0, 0),
    COAL_MINE(2, false, null, 0, 60, 40, 24, 9, 9, Style.MINE, 0, 0),
    /** The Chamber of Commerce: its merchants send caravans and merchant ships to the capitals. */
    CHAMBER_OF_COMMERCE(2, true, Occupation.MERCHANT, 0, 200, 150, 48, 11, 9, Style.DESIGN, 0, 0),
    /** The academy: the private school grown great; its scholars sit the imperial examination. */
    ACADEMY(3, true, Occupation.SCHOLAR, 2, 600, 400, 72, 15, 13, Style.DESIGN, 0, 0),
    // ---------------------------------------------------------------- Grade 1 town (the novel's chapters 50-75)
    /** The town's stone city wall, outside the old palisade, with gatehouses and a moat. */
    CITY_WALL(4, false, null, 0, 600, 4000, 120, 0, 0, Style.FENCE, 4, 1),
    BRICK_KILN(4, true, Occupation.POTTER, 0, 400, 600, 48, 9, 9, Style.WORKSHOP, 4, 1),
    CEMETERY(4, false, null, 0, 200, 300, 24, 13, 11, Style.DESIGN, 4, 1),
    ORCHARD(4, false, null, 0, 100, 0, 24, 13, 13, Style.FARM, 4, 1),
    PUBLIC_TOILET(4, true, null, 0, 60, 60, 12, 5, 5, Style.HUT, 4, 1),
    ARSENAL(4, true, Occupation.BLACKSMITH, 1, 600, 400, 48, 11, 9, Style.WORKSHOP, 4, 1),
    /** A walled ward of courtyard houses: 400 beds. */
    RESIDENTIAL_QUARTER(4, true, null, 0, 600, 500, 48, 27, 25, Style.COURTYARD, 0, 0),
    /** The Recruitment Hall: one special talent a month, for 10 gold. */
    RECRUITMENT_HALL(4, true, Occupation.CIVIL_SERVANT, 1, 300, 200, 48, 9, 9, Style.HALL, 0, 0),
    /** The town's government offices: the departments sit here. */
    GOVERNMENT_OFFICE(4, true, Occupation.CIVIL_SERVANT, 0, 500, 400, 48, 13, 11, Style.HALL, 4, 1),
    // ---------------------------------------------------------------- Grade 2 town
    RELAY_STATION(5, true, Occupation.STABLEMAN, 0, 1500, 800, 72, 11, 9, Style.SHED, 5, 1),
    VEGETABLE_FIELD(5, false, null, 0, 400, 200, 48, 13, 13, Style.FARM, 5, 1),
    MEDICINE_GARDEN(5, false, null, 0, 400, 200, 48, 11, 11, Style.FARM, 5, 1),
    PHARMACY(5, true, Occupation.DOCTOR, 0, 800, 200, 48, 7, 7, Style.SHOP, 5, 1),
    TEXTILE_MILL(5, true, Occupation.TAILOR, 0, 1800, 600, 72, 11, 9, Style.WORKSHOP, 5, 1),
    BANK(5, true, Occupation.MERCHANT, 0, 2000, 1000, 72, 11, 9, Style.HALL, 5, 1),
    /** The novel's sticking point: it needs an alchemist, and Shanhai waited weeks for one. */
    ALCHEMY_WORKSHOP(5, true, Occupation.ALCHEMIST, 0, 1500, 1800, 72, 9, 9, Style.WORKSHOP, 5, 1),
    /** The City Defense Camp: the standing army's barracks and drill ground. */
    CITY_DEFENSE_CAMP(4, true, Occupation.OFFICER, 1, 800, 1200, 72, 15, 13, Style.CAMP, 0, 0),
    // ---------------------------------------------------------------- Grade 3 town
    INN(6, true, Occupation.COOK, 0, 800, 400, 48, 11, 9, Style.HALL, 6, 1),
    PAWNSHOP(6, true, Occupation.MERCHANT, 0, 600, 400, 48, 7, 7, Style.SHOP, 6, 1),
    JEWELRY_SHOP(6, true, Occupation.ALCHEMIST, 0, 600, 600, 48, 7, 7, Style.SHOP, 6, 1),
    GARMENT_WORKSHOP(6, true, Occupation.TAILOR, 1, 800, 400, 48, 9, 7, Style.WORKSHOP, 6, 1),
    FARMERS_MARKET(6, false, null, 0, 400, 200, 24, 11, 11, Style.SHED, 6, 1),
    VINEGAR_WORKSHOP(6, true, Occupation.COOK, 0, 400, 200, 48, 7, 7, Style.WORKSHOP, 6, 1),
    SOY_SAUCE_WORKSHOP(6, true, Occupation.COOK, 0, 400, 200, 48, 7, 7, Style.WORKSHOP, 6, 1),
    TOFU_WORKSHOP(6, true, Occupation.COOK, 0, 300, 150, 24, 7, 7, Style.WORKSHOP, 6, 1),
    LARGE_BREWERY(6, true, Occupation.WINEMAKER, 1, 1200, 600, 72, 11, 9, Style.WORKSHOP, 6, 1),
    AUCTION_HOUSE(6, true, Occupation.MERCHANT, 1, 1500, 1200, 72, 13, 11, Style.HALL, 0, 0),
    // ---------------------------------------------------------------- laid on the map, any grade
    PATH(1, false, null, 0, 0, 0, 1, 0, 0, Style.PATH, 0, 0),
    /** Ground the builders clear and level on the Lord's order. */
    CLEARING(1, false, null, 0, 0, 0, 1, 0, 0, Style.CLEARING, 0, 0),
    /** The whole village ground levelled to the manor, out to ten blocks past the Grade 3 wall. */
    GROUNDS(1, false, null, 0, 0, 0, 1, 0, 0, Style.CLEARING, 0, 0),
    /** A building from a plan (a structure manual's, or an architect's own), raised by the village's builders. */
    COMMISSION(1, false, null, 0, 0, 0, 1, 0, 0, Style.DESIGN, 0, 0);

    public enum Style { PATH, CLEARING, FENCE, HALL, HUT, COURTYARD, WORKSHOP, SHOP, LOGGING, QUARRY, FARM, MINE, CAMP, TEMPLE, DOJO, GRANARY, TOWER, OUTPOST, SHED, DESIGN }

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
        return style != Style.FENCE && this != PATH && this != CLEARING && this != GROUNDS;
    }

    /** Not one of the novel's buildings: kept out of the building lists (paths are laid from the map). */
    /** A hidden building, offered only once the village has revealed it (see Village.revealHidden). */
    public boolean secret() {
        return this == MAZU_TEMPLE;
    }

    public boolean hidden() {
        return this == PATH || this == CLEARING || this == GROUNDS || this == COMMISSION;
    }

    /** The drawn design (data/realmlord/designs) a building of this kind is built to, or null. */
    public String designId() {
        return switch (this) {
            case VILLAGE_TEMPLE -> "taoist_shrine";
            case DOCK -> "wharf";
            case SHIPYARD -> "shipyard";
            case SALT_FIELD -> "salt_pans";
            case CHAMBER_OF_COMMERCE -> "chamber_of_commerce";
            case ACADEMY -> "academy";
            case CEMETERY -> "cemetery";
            default -> null;
        };
    }

    /** Built at the water's edge: it reaches into the water, and the ground is not levelled for it. */
    public boolean waterside() {
        return this == DOCK || this == SHIPYARD || this == SALT_FIELD;
    }

    // ---------------------------------------------------------------- what it does once built

    public int housing() {
        return switch (this) {
            case SMALL_RESIDENTIAL_COURTYARD -> 20;
            case RESIDENTIAL_QUARTER -> 400;
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
            case SALT_FIELD -> Job.SALT_MAKING;
            case ORCHARD, VEGETABLE_FIELD -> Job.FARMING;
            case MEDICINE_GARDEN -> Job.HERB_GROWING;
            case COPPER_MINE -> Job.COPPER_MINING;
            case COAL_MINE -> Job.COAL_MINING;
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
            case VILLAGE_TEMPLE, TEMPLE -> Occupation.PRIEST;
            case SHIPYARD -> Occupation.SHIPBUILDER;
            case CHAMBER_OF_COMMERCE -> Occupation.MERCHANT;
            case ACADEMY -> Occupation.SCHOLAR;
            case BRICK_KILN -> Occupation.POTTER;
            case ARSENAL -> Occupation.BLACKSMITH;
            case RECRUITMENT_HALL, GOVERNMENT_OFFICE -> Occupation.CIVIL_SERVANT;
            case RELAY_STATION -> Occupation.STABLEMAN;
            case PHARMACY -> Occupation.DOCTOR;
            case TEXTILE_MILL, GARMENT_WORKSHOP -> Occupation.TAILOR;
            case BANK, PAWNSHOP, AUCTION_HOUSE -> Occupation.MERCHANT;
            case ALCHEMY_WORKSHOP, JEWELRY_SHOP -> Occupation.ALCHEMIST;
            case CITY_DEFENSE_CAMP -> Occupation.OFFICER;
            case INN, VINEGAR_WORKSHOP, SOY_SAUCE_WORKSHOP, TOFU_WORKSHOP -> Occupation.COOK;
            case LARGE_BREWERY -> Occupation.WINEMAKER;
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
            case PATH, CLEARING, GROUNDS -> new int[]{0, 0, 0, 0};
            case BARRACKS -> new int[]{0, 0, 0, 6};
            case MAZU_TEMPLE -> new int[]{2, 2, 6, 0};
            case VILLAGE_TEMPLE -> new int[]{1, 0, 4, 0};
            case DOCK -> new int[]{0, 3, 0, 2};
            case SHIPYARD -> new int[]{0, 3, 0, 3};
            case SALT_FIELD -> new int[]{0, 4, 0, 0};
            case COPPER_MINE, COAL_MINE -> new int[]{0, 2, 0, 0};
            case CHAMBER_OF_COMMERCE -> new int[]{1, 6, 0, 0};
            case ACADEMY -> new int[]{2, 0, 10, 0};
            case COMMISSION -> new int[]{0, 0, 0, 0};
            case CITY_WALL -> new int[]{0, 0, 0, 12};
            case BRICK_KILN -> new int[]{0, 5, 0, 0};
            case CEMETERY -> new int[]{3, 0, 5, 0};
            case ORCHARD, VEGETABLE_FIELD, MEDICINE_GARDEN -> new int[]{0, 3, 0, 0};
            case PUBLIC_TOILET -> new int[]{3, 0, 1, 0};
            case ARSENAL -> new int[]{0, 1, 0, 8};
            case RESIDENTIAL_QUARTER -> new int[]{0, 0, 0, 0};
            case RECRUITMENT_HALL -> new int[]{4, 0, 2, 0};
            case GOVERNMENT_OFFICE -> new int[]{10, 2, 2, 0};
            case RELAY_STATION -> new int[]{3, 4, 0, 2};
            case PHARMACY -> new int[]{2, 3, 2, 0};
            case TEXTILE_MILL, GARMENT_WORKSHOP -> new int[]{0, 7, 0, 0};
            case BANK -> new int[]{2, 10, 0, 0};
            case ALCHEMY_WORKSHOP -> new int[]{0, 6, 2, 2};
            case CITY_DEFENSE_CAMP -> new int[]{0, 0, 0, 12};
            case INN -> new int[]{2, 5, 2, 0};
            case PAWNSHOP, JEWELRY_SHOP -> new int[]{0, 6, 0, 0};
            case FARMERS_MARKET -> new int[]{1, 6, 0, 0};
            case VINEGAR_WORKSHOP, SOY_SAUCE_WORKSHOP, TOFU_WORKSHOP -> new int[]{0, 4, 0, 0};
            case LARGE_BREWERY -> new int[]{0, 8, 0, 0};
            case AUCTION_HOUSE -> new int[]{0, 8, 3, 0};
        };
    }

    /** People's Thoughts the building supports. */
    public int morale() {
        return switch (this) {
            case DINING_HALL, CLINIC, TEMPLE, MAZU_TEMPLE -> 5;
            case VILLAGE_TEMPLE, INN -> 4;
            case CEMETERY -> 6;
            case BATHROOM, ANCESTRAL_HALL, PUBLIC_TOILET, PHARMACY -> 3;
            default -> 0;
        };
    }

    /** Security (Law and Order) the building adds. */
    public int security() {
        return switch (this) {
            case SIMPLE_FENCE -> 10;
            case ARROW_TOWER, STONE_OUTPOST, SENTRY_POST -> 5;
            case MILITARY_CAMP -> 5;
            case CITY_WALL -> 20;
            case CITY_DEFENSE_CAMP, ARSENAL -> 6;
            default -> 0;
        };
    }

    /** Blueprint price in copper: 2 gold for basic village plans, 5 gold for Grade 2, 10 gold for Grade 3. */
    public long blueprintPrice() {
        return switch (grade) {
            case 1 -> 20_000L;
            case 2 -> 50_000L;
            case 3 -> 100_000L;
            case 4 -> 200_000L;
            case 5 -> 400_000L;
            default -> 800_000L;
        };
    }

    public static List<BuildingType> ofGrade(int g) {
        List<BuildingType> out = new ArrayList<>();
        for (BuildingType t : values()) if (t.grade == g && !t.hidden() && !t.secret()) out.add(t);
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
