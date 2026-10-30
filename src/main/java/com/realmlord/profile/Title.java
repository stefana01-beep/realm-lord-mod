package com.realmlord.profile;

/**
 * Noble titles for Lord players, earned automatically from merit. Five orders of three
 * classes each (100 merit for the first, doubling every rank), then Duke, Prince and
 * Son of Heaven.
 */
public enum Title {
    NONE(0, "none"),
    BARON_3(100, "baron_3"),
    BARON_2(200, "baron_2"),
    BARON_1(400, "baron_1"),
    VISCOUNT_3(800, "viscount_3"),
    VISCOUNT_2(1_600, "viscount_2"),
    VISCOUNT_1(3_200, "viscount_1"),
    EARL_3(6_400, "earl_3"),
    EARL_2(12_800, "earl_2"),
    EARL_1(25_600, "earl_1"),
    MARQUIS_3(51_200, "marquis_3"),
    MARQUIS_2(102_400, "marquis_2"),
    MARQUIS_1(204_800, "marquis_1"),
    DUKE(1_000_000, "duke"),
    PRINCE(2_000_000, "prince"),
    SON_OF_HEAVEN(5_000_000, "son_of_heaven");

    public final long merit;
    public final String id;

    Title(long merit, String id) {
        this.merit = merit;
        this.id = id;
    }

    public String key() {
        return "realmlord.title." + id;
    }

    public String shortKey() {
        return "realmlord.title.short." + id;
    }

    public static Title forMerit(long merit) {
        Title best = NONE;
        for (Title t : values()) if (merit >= t.merit) best = t;
        return best;
    }

    public Title next() {
        Title[] v = values();
        return ordinal() + 1 < v.length ? v[ordinal() + 1] : null;
    }

    public boolean atLeast(Title other) {
        return ordinal() >= other.ordinal();
    }

    public static Title byId(int i) {
        Title[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }
}
