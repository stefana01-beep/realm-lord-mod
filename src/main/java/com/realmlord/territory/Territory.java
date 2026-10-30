package com.realmlord.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** One Lord's territory: where it is, who rules it, how big it has grown. */
public class Territory {
    public int id;
    public UUID owner;
    public String ownerName = "";
    public String name = "";
    public TokenGrade grade = TokenGrade.BRONZE;
    public TerritoryLevel level = TerritoryLevel.VILLAGE_1;
    public BlockPos center = BlockPos.ZERO;
    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public int population;
    /** 1 for the first village founded in the world, 2 for the second, and so on. */
    public int foundedOrder;
    public long foundedTick;

    public int radius() {
        return level.radius;
    }

    public boolean contains(ResourceKey<Level> dim, BlockPos pos) {
        if (!dim.equals(dimension)) return false;
        long dx = pos.getX() - center.getX();
        long dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (long) radius() * radius();
    }

    public double distanceTo(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        if (owner != null) t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
        t.putString("name", name);
        t.putInt("grade", grade.ordinal());
        t.putInt("level", level.ordinal());
        t.put("center", NbtUtils.writeBlockPos(center));
        t.putString("dim", dimension.location().toString());
        t.putInt("population", population);
        t.putInt("order", foundedOrder);
        t.putLong("founded", foundedTick);
        return t;
    }

    public static Territory load(CompoundTag t) {
        Territory r = new Territory();
        r.id = t.getInt("id");
        if (t.hasUUID("owner")) r.owner = t.getUUID("owner");
        r.ownerName = t.getString("ownerName");
        r.name = t.getString("name");
        r.grade = TokenGrade.byId(t.getInt("grade"));
        r.level = TerritoryLevel.byId(t.getInt("level"));
        r.center = NbtUtils.readBlockPos(t.getCompound("center"));
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
        r.dimension = dim == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dim);
        r.population = t.getInt("population");
        r.foundedOrder = t.getInt("order");
        r.foundedTick = t.getLong("founded");
        return r;
    }
}
