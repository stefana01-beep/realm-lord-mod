package com.realmlord.network;

/**
 * The client's copy of its own profile, filled by {@link SyncProfilePacket}. Plain fields
 * with no client-only types, so it is safe to load on a dedicated server.
 */
public final class ClientProfileData {
    private ClientProfileData() {}

    public static boolean synced;
    public static int mode;
    public static int combat = -1;
    public static int work = -1;
    public static int level = 1;
    public static long xp;
    public static long xpNext = 50;
    public static int points;
    public static int[] base = new int[4];
    public static int[] added = new int[4];
    public static long merit;
    public static int title;
    public static int workLevel = 1;
    public static long workXp;
    public static long workNext;
    public static int selectedSkill = -1;
    public static int[] skills = new int[0];
    public static int[] cooldownTicks = new int[0];
    public static long cooldownReceivedMs;
    public static boolean restriction;

    // Village Creation quest
    public static int questStage;
    public static int logs, stone, food;
    public static boolean hasCamp;
    public static int campX, campZ;
    public static int banditsLeft;
    public static int captives;
    public static boolean chiefKilled;
    public static boolean cacheFound;
    public static long questElapsed;
    public static long questLimit;
    public static int deaths;
    public static int questGrade = -1;

    // Territory
    public static boolean hasTerritory;
    public static String territoryName = "";
    public static int territoryLevel;
    public static int territoryGrade;
    public static int population;
    public static int cx, cy, cz;
    public static int radius;
    public static int foundedOrder;

    // World
    public static int villagesFounded;
    public static boolean goldClaimed;

    /** Remaining cooldown in ticks for the i-th unlocked skill, counting down since the sync. */
    public static int cooldownLeft(int index) {
        if (index < 0 || index >= cooldownTicks.length) return 0;
        long elapsed = (System.currentTimeMillis() - cooldownReceivedMs) / 50L;
        return (int) Math.max(0, cooldownTicks[index] - elapsed);
    }

    public static int stat(int i) {
        return base[i] + added[i];
    }
}
