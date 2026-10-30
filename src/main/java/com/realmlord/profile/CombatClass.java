package com.realmlord.profile;

/**
 * The six combat occupations of the Chinese region. Each leans toward two attributes and
 * has three skills, unlocked at levels 1, 10 and 25.
 */
public enum CombatClass {
    STRATEGIST("strategist", new int[]{2, 0, 8, 2}),
    GENERAL("general", new int[]{6, 5, 0, 1}),
    CHIVALROUS_EXPERT("chivalrous_expert", new int[]{0, 8, 3, 1}),
    ALCHEMIST("alchemist", new int[]{0, 2, 7, 3}),
    HEALER("healer", new int[]{1, 1, 6, 4}),
    BAILIFF("bailiff", new int[]{4, 5, 2, 1});

    public final String id;
    /** Starting attribute bonus, in {@link Stat} order. */
    public final int[] bias;

    CombatClass(String id, int[] bias) {
        this.id = id;
        this.bias = bias;
    }

    public String key() {
        return "realmlord.class." + id;
    }

    public String descKey() {
        return "realmlord.class." + id + ".desc";
    }

    public static CombatClass byId(int i) {
        CombatClass[] v = values();
        return i >= 0 && i < v.length ? v[i] : GENERAL;
    }
}
