package com.realmlord.profile;

/**
 * Innate attributes, rolled between 5 and 20 when a character is created. Level-up points
 * cannot raise them; only special pills or cultivation methods can.
 */
public enum Innate {
    BODY("body"),
    COMPREHENSION("comprehension"),
    LUCK("luck"),
    CHARM("charm");

    public static final int MIN = 5;
    public static final int MAX = 20;

    public final String id;

    Innate(String id) {
        this.id = id;
    }

    public String key() {
        return "realmlord.innate." + id;
    }

    public String descKey() {
        return "realmlord.innate." + id + ".desc";
    }
}
