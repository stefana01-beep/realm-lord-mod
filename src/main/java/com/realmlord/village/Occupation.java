package com.realmlord.village;

/**
 * What a resident does for a living. Commoners are farmers; the rest are the "special
 * talents" a Lord needs to staff workshops and shops. Weight is the chance among immigrants.
 */
public enum Occupation {
    FARMER("farmer", 60, false),
    FRUIT_FARMER("fruit_farmer", 3, true),
    CARPENTER("carpenter", 5, true),
    MASON("mason", 5, true),
    BLACKSMITH("blacksmith", 2.5, true),
    GROCER("grocer", 2.5, true),
    TAILOR("tailor", 2, true),
    DOCTOR("doctor", 1.5, true),
    SCHOLAR("scholar", 1.5, true),
    MARTIAL_ARTIST("martial_artist", 1.5, true),
    POTTER("potter", 1.5, true),
    WINEMAKER("winemaker", 1.5, true),
    COOK("cook", 2, true),
    HUNTER("hunter", 4, true),
    FISHERMAN("fisherman", 3, true),
    MINER("miner", 3, true),
    STABLEMAN("stableman", 1.5, true),
    CIVIL_SERVANT("civil_servant", 1.5, true),
    OFFICER("officer", 1.5, true),
    /** Builds boats and ships at the shipyard (the novel's Zheng Dahai). */
    SHIPBUILDER("shipbuilder", 1.5, true),
    /** A Taoist priest: presides over the temple, refines pills and writes talismans. */
    PRIEST("priest", 0.6, true),
    /** A merchant: runs the Chamber of Commerce and its caravans. */
    MERCHANT("merchant", 1.5, true);

    public final String id;
    public final double weight;
    public final boolean special;

    Occupation(String id, double weight, boolean special) {
        this.id = id;
        this.weight = weight;
        this.special = special;
    }

    public String key() {
        return "realmlord.occupation." + id;
    }

    public static Occupation byId(int i) {
        Occupation[] v = values();
        return i >= 0 && i < v.length ? v[i] : FARMER;
    }
}
