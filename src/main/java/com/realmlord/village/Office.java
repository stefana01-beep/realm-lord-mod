package com.realmlord.village;

import com.realmlord.territory.Territory;

/**
 * The offices of the Lord's administration, the secretaries who run village affairs while the
 * Lord is away, as Shanhai's grew in the novel into departments. Each is held by one resident;
 * its effect grows with the stat that matters for it (a figure from history does it best).
 */
public enum Office {
    STEWARD("steward", 1, Stat.POLITICS),
    TREASURER("treasurer", 1, Stat.POLITICS),
    WORKS("works", 2, Stat.INTELLIGENCE),
    AGRICULTURE("agriculture", 2, Stat.INTELLIGENCE),
    MILITARY("military", 2, Stat.COMMAND),
    COMMERCE("commerce", 3, Stat.POLITICS),
    RITES("rites", 3, Stat.INTELLIGENCE),
    // The town's departments (the novel's divisions), opened by the Government Office.
    SECURITY("security", 4, Stat.COMMAND),
    MERIT("merit", 4, Stat.POLITICS),
    ARMORY("armory", 4, Stat.INTELLIGENCE),
    INTERNAL_AFFAIRS("internal_affairs", 5, Stat.POLITICS),
    BOW_CROSSBOW("bow_crossbow", 5, Stat.COMMAND),
    TRANSPORT("transport", 5, Stat.INTELLIGENCE),
    PROCURATORATE("procuratorate", 6, Stat.POLITICS),
    AUDITING("auditing", 6, Stat.INTELLIGENCE);

    /** A full cabinet: eight offices or more held, and the town runs as a government. */
    public static final int CABINET = 8;

    public static int held(Territory t) {
        int n = 0;
        for (Resident r : t.residents) if (r.office >= 0) n++;
        return n;
    }

    public static boolean cabinet(Territory t) {
        return held(t) >= CABINET && Village.countComplete(t, BuildingType.GOVERNMENT_OFFICE) > 0;
    }

    public enum Stat { COMMAND, FORCE, INTELLIGENCE, POLITICS }

    public final String id;
    /** The village grade that opens the office. */
    public final int grade;
    public final Stat stat;

    Office(String id, int grade, Stat stat) {
        this.id = id;
        this.grade = grade;
        this.stat = stat;
    }

    public String key() {
        return "realmlord.office." + id;
    }

    public static Office byId(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    public int statOf(Resident r) {
        return switch (stat) {
            case COMMAND -> r.command;
            case FORCE -> r.force;
            case INTELLIGENCE -> r.intelligence;
            case POLITICS -> r.politics;
        };
    }

    /** The grade the territory has reached (villages 1-3; towns and beyond count as 3 or more). */
    public static int grade(Territory t) {
        return Math.min(9, t.level.ordinal() + 1);
    }

    public boolean open(Territory t) {
        return grade(t) >= grade && (grade < 4 || Village.countComplete(t, BuildingType.GOVERNMENT_OFFICE) > 0);
    }

    public static Resident holder(Territory t, Office o) {
        for (Resident r : t.residents) if (r.office == o.ordinal()) return r;
        return null;
    }

    /** The holder's stat, or 0 if the office is empty. */
    public static int power(Territory t, Office o) {
        Resident r = holder(t, o);
        return r == null ? 0 : o.statOf(r);
    }

    /** A multiplier of 1 + stat / divisor for the office's effect. */
    public static double boost(Territory t, Office o, double divisor) {
        return 1.0 + power(t, o) / divisor;
    }

    /** Appoints a resident (or, with r null, clears the office). Returns an error key or null. */
    public static String appoint(Territory t, Office o, Resident r) {
        if (o == null) return "realmlord.office.err.none";
        if (!o.open(t)) return "realmlord.office.err.grade";
        Resident cur = holder(t, o);
        if (cur != null) cur.office = -1;
        if (r == null) return null;
        r.office = o.ordinal();
        // An official leaves the ranks and the ships.
        r.unit = -1;
        r.sailor = false;
        r.militia = false;
        r.follows = false;
        r.job = Job.IDLE;
        r.workplace = -1;
        return null;
    }
}
