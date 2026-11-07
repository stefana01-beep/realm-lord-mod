package com.realmlord.village;

/**
 * What a resident is assigned to. Field jobs need a finished field building with free places;
 * STAFF ties a resident to one workshop or shop.
 */
public enum Job {
    IDLE("idle"),
    CONSTRUCTION("construction"),
    LOGGING("logging"),
    QUARRYING("quarrying"),
    FARMING("farming"),
    MINING("mining"),
    ADMINISTRATION("administration"),
    STAFF("staff"),
    HUNTING("hunting"),
    SALT_MAKING("salt_making"),
    COPPER_MINING("copper_mining"),
    COAL_MINING("coal_mining"),
    FISHING("fishing"),
    HERB_GROWING("herb_growing");

    public final String id;

    Job(String id) {
        this.id = id;
    }

    public String key() {
        return "realmlord.job." + id;
    }

    public static Job byId(int i) {
        Job[] v = values();
        return i >= 0 && i < v.length ? v[i] : IDLE;
    }
}
