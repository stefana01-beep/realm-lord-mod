package com.realmlord.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/** One building in a territory: where it stands, how far construction has got, and farm state. */
public class Building {
    public int id;
    public BuildingType type;
    /** Lowest corner of the footprint, at ground level. */
    public BlockPos origin = BlockPos.ZERO;
    /** 0-3 quarter turns. */
    public int rotation;
    /** Construction progress in builder-ticks, out of {@link #workNeeded}. */
    public long progress;
    public long workNeeded;
    public boolean complete;
    /** How many blocks of the design have been placed so far. */
    public int placed;
    /** Granted free by a village upgrade. */
    public boolean free;

    // Farmland
    public boolean sown;
    public long sowDay;
    public float sowYield = 1.0F;

    public int sizeX() {
        return rotation % 2 == 0 ? type.width : type.depth;
    }

    public int sizeZ() {
        return rotation % 2 == 0 ? type.depth : type.width;
    }

    public BlockPos center() {
        return origin.offset(sizeX() / 2, 0, sizeZ() / 2);
    }

    public boolean overlaps(int x0, int z0, int sx, int sz, int gap) {
        return x0 - gap < origin.getX() + sizeX() && origin.getX() < x0 + sx + gap
                && z0 - gap < origin.getZ() + sizeZ() && origin.getZ() < z0 + sz + gap;
    }

    public double fraction() {
        return complete ? 1.0 : workNeeded <= 0 ? 0.0 : Math.min(1.0, progress / (double) workNeeded);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("type", type.id());
        t.put("origin", NbtUtils.writeBlockPos(origin));
        t.putInt("rot", rotation);
        t.putLong("progress", progress);
        t.putLong("need", workNeeded);
        t.putBoolean("complete", complete);
        t.putInt("placed", placed);
        t.putBoolean("free", free);
        t.putBoolean("sown", sown);
        t.putLong("sowDay", sowDay);
        t.putFloat("sowYield", sowYield);
        return t;
    }

    public static Building load(CompoundTag t) {
        BuildingType type = BuildingType.byId(t.getString("type"));
        if (type == null) return null;
        Building b = new Building();
        b.id = t.getInt("id");
        b.type = type;
        b.origin = NbtUtils.readBlockPos(t.getCompound("origin"));
        b.rotation = t.getInt("rot");
        b.progress = t.getLong("progress");
        b.workNeeded = t.getLong("need");
        b.complete = t.getBoolean("complete");
        b.placed = t.getInt("placed");
        b.free = t.getBoolean("free");
        b.sown = t.getBoolean("sown");
        b.sowDay = t.getLong("sowDay");
        b.sowYield = t.contains("sowYield") ? t.getFloat("sowYield") : 1.0F;
        return b;
    }
}
