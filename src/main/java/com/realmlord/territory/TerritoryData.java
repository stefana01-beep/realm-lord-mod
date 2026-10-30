package com.realmlord.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Every territory in the world, plus world-wide records such as the one Gold token. */
public class TerritoryData extends SavedData {
    private static final String NAME = "realmlord_territories";

    private final Map<Integer, Territory> territories = new LinkedHashMap<>();
    private int nextId = 1;
    private int villagesFounded;
    private boolean goldTokenClaimed;
    private String goldTokenHolder = "";

    public static TerritoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TerritoryData::load, TerritoryData::new, NAME);
    }

    public Collection<Territory> all() {
        return territories.values();
    }

    public Territory byId(int id) {
        return territories.get(id);
    }

    public Territory ownedBy(UUID player) {
        for (Territory t : territories.values()) if (player.equals(t.owner)) return t;
        return null;
    }

    public Territory at(ResourceKey<Level> dim, BlockPos pos) {
        for (Territory t : territories.values()) if (t.contains(dim, pos)) return t;
        return null;
    }

    /** The territory whose border is nearest this spot, and how far that border is. */
    public double gapToNearest(ResourceKey<Level> dim, BlockPos pos, int newRadius) {
        double best = Double.MAX_VALUE;
        for (Territory t : territories.values()) {
            if (!t.dimension.equals(dim)) continue;
            best = Math.min(best, t.distanceTo(pos) - t.radius() - newRadius);
        }
        return best;
    }

    public Territory add(Territory t) {
        t.id = nextId++;
        villagesFounded++;
        t.foundedOrder = villagesFounded;
        territories.put(t.id, t);
        setDirty();
        return t;
    }

    public void remove(int id) {
        if (territories.remove(id) != null) setDirty();
    }

    public int villagesFounded() {
        return villagesFounded;
    }

    public boolean goldTokenClaimed() {
        return goldTokenClaimed;
    }

    public String goldTokenHolder() {
        return goldTokenHolder;
    }

    public void claimGoldToken(String holder) {
        goldTokenClaimed = true;
        goldTokenHolder = holder;
        setDirty();
    }

    public void touch() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Territory t : territories.values()) list.add(t.save());
        tag.put("territories", list);
        tag.putInt("nextId", nextId);
        tag.putInt("founded", villagesFounded);
        tag.putBoolean("gold", goldTokenClaimed);
        tag.putString("goldHolder", goldTokenHolder);
        return tag;
    }

    public static TerritoryData load(CompoundTag tag) {
        TerritoryData d = new TerritoryData();
        ListTag list = tag.getList("territories", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Territory t = Territory.load(list.getCompound(i));
            d.territories.put(t.id, t);
        }
        d.nextId = Math.max(1, tag.getInt("nextId"));
        d.villagesFounded = tag.getInt("founded");
        d.goldTokenClaimed = tag.getBoolean("gold");
        d.goldTokenHolder = tag.getString("goldHolder");
        return d;
    }

    public java.util.List<Territory> list() {
        return new ArrayList<>(territories.values());
    }
}
