package com.realmlord.profile;

/** The two game modes from the novel. NONE means the player has not chosen yet. */
public enum PlayMode {
    NONE("realmlord.mode.none"),
    LORD("realmlord.mode.lord"),
    ADVENTURER("realmlord.mode.adventurer");

    public final String key;

    PlayMode(String key) {
        this.key = key;
    }

    public static PlayMode byId(int i) {
        PlayMode[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }
}
