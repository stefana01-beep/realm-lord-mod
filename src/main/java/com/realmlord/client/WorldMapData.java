package com.realmlord.client;

import com.realmlord.network.WorldInfoPacket;

import java.util.ArrayList;
import java.util.List;

/** What the client knows for the world map: whether this is the China world, and the territories. */
public final class WorldMapData {
    private WorldMapData() {}

    public static boolean china;
    public static List<WorldInfoPacket.Place> territories = new ArrayList<>();

    public static void accept(WorldInfoPacket p) {
        china = p.china;
        territories = p.territories;
    }

    public static void clear() {
        china = false;
        territories = new ArrayList<>();
    }
}
