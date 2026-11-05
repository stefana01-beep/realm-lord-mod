package com.realmlord.territory;

import com.realmlord.profile.Title;

/**
 * Territory levels and their upgrade requirements, following the novel: each level needs a
 * minimum population, all required infrastructure and a minimum Lord title, and some also
 * need the four indexes (politics, economy, culture, military) at a minimum score.
 * Radius is the mod's own: how far the territory's border reaches, in blocks.
 */
public enum TerritoryLevel {
    VILLAGE_1("village", 1, 0, Title.NONE, 0, 64),
    VILLAGE_2("village", 2, 200, Title.BARON_2, 0, 80),
    VILLAGE_3("village", 3, 500, Title.BARON_1, 0, 96),
    TOWN_1("town", 1, 1_000, Title.VISCOUNT_3, 30, 128),
    TOWN_2("town", 2, 2_000, Title.VISCOUNT_2, 0, 144),
    TOWN_3("town", 3, 5_000, Title.VISCOUNT_1, 0, 160),
    COUNTY_1("county", 1, 10_000, Title.EARL_3, 45, 192),
    COUNTY_2("county", 2, 25_000, Title.EARL_2, 0, 208),
    COUNTY_3("county", 3, 50_000, Title.EARL_1, 0, 224),
    PREFECTURE_1("prefecture", 1, 100_000, Title.MARQUIS_3, 65, 256),
    PREFECTURE_2("prefecture", 2, 500_000, Title.MARQUIS_2, 0, 288),
    PREFECTURE_3("prefecture", 3, 1_000_000, Title.MARQUIS_1, 0, 320),
    CAPITAL("capital", 1, 2_000_000, Title.DUKE, 90, 384);

    public final String kind;
    public final int grade;
    public final int population;
    public final Title title;
    public final int minIndex;
    public final int radius;

    TerritoryLevel(String kind, int grade, int population, Title title, int minIndex, int radius) {
        this.kind = kind;
        this.grade = grade;
        this.population = population;
        this.title = title;
        this.minIndex = minIndex;
        this.radius = radius;
    }

    public String kindKey() {
        return "realmlord.territory." + kind;
    }

    /** Population cap at this level: the next level's requirement (Village 1 holds 200). */
    public int populationCap() {
        TerritoryLevel n = next();
        return n == null ? Integer.MAX_VALUE : n.population;
    }

    /** Village grade 1-3, or 0 for towns and above. */
    public int villageGrade() {
        return kind.equals("village") ? grade : 0;
    }

    public TerritoryLevel next() {
        TerritoryLevel[] v = values();
        return ordinal() + 1 < v.length ? v[ordinal() + 1] : null;
    }

    public static TerritoryLevel byId(int i) {
        TerritoryLevel[] v = values();
        return i >= 0 && i < v.length ? v[i] : VILLAGE_1;
    }
}
