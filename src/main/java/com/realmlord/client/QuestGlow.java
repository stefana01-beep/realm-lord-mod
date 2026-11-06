package com.realmlord.client;

import java.util.HashSet;
import java.util.Set;

/** Which bandits this player is hunting: they are outlined, through leaves and hills, for this player only. */
public final class QuestGlow {
    private QuestGlow() {}

    private static final Set<Integer> IDS = new HashSet<>();

    public static void set(int[] ids) {
        IDS.clear();
        for (int id : ids) IDS.add(id);
    }

    public static boolean glows(int entityId) {
        return IDS.contains(entityId);
    }
}
