package com.realmlord.profile;

/**
 * The six combat occupations of the Chinese region in the novel. Each has three skills,
 * unlocked at levels 1, 10 and 25.
 */
public enum CombatClass {
    STRATEGIST("strategist"),
    GENERAL("general"),
    CHIVALROUS_EXPERT("chivalrous_expert"),
    PRIEST("priest"),
    HEALER("healer"),
    BAILIFF("bailiff");

    public final String id;

    CombatClass(String id) {
        this.id = id;
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
