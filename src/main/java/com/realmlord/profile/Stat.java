package com.realmlord.profile;

/** The four attributes shown on every character sheet. */
public enum Stat {
    COMMAND("command"),
    FORCE("force"),
    INTELLIGENCE("intelligence"),
    POLITICS("politics");

    public final String id;

    Stat(String id) {
        this.id = id;
    }

    public String key() {
        return "realmlord.stat." + id;
    }

    public String descKey() {
        return "realmlord.stat." + id + ".desc";
    }

    public static Stat byId(int i) {
        Stat[] v = values();
        return i >= 0 && i < v.length ? v[i] : COMMAND;
    }
}
