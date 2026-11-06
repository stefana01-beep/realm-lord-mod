package com.realmlord.village;

import com.realmlord.RealmConfig;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the residents walking around the village in step with the records: talents first,
 * then everyone else, up to the configured number shown at once.
 */
public final class Residents {
    private Residents() {}

    /** Residents who should be shown, talents first. */
    public static List<Resident> shown(Territory t) {
        List<Resident> list = new ArrayList<>(t.residents);
        // Companions and the militia first (they must be out), then figures and talents.
        list.sort(Comparator.comparing((Resident r) -> !(r.follows || r.militia)).thenComparing(r -> !r.isFigure())
                .thenComparing(r -> !r.isTalent()).thenComparingInt(r -> r.id));
        return list.subList(0, Math.min(list.size(), RealmConfig.visibleResidents()));
    }

    public static void sync(ServerLevel level, Territory t) {
        if (!level.isLoaded(t.center)) return;
        if (FURNISHED.add(t.id) && t.manorTier <= 1 && Village.manorHousing(t) > 0) com.realmlord.territory.Founding.furnishOld(level, t.center);
        Set<Integer> show = new HashSet<>();
        for (Resident r : shown(t)) show.add(r.id);
        int spawned = 0;
        for (Resident r : t.residents) {
            Entity e = r.entity == null ? null : level.getEntity(r.entity);
            if (!show.contains(r.id)) {
                if (e != null) e.discard();
                r.entity = null;
                continue;
            }
            if (e instanceof SettlerEntity s && s.isAlive()) {
                s.routine(t, r);
                continue;
            }
            if (spawned >= 3) continue;
            SettlerEntity s = ModEntities.SETTLER.get().create(level);
            if (s == null) continue;
            // They appear at home by night and at work by day, not all bunched at the manor.
            BlockPos base = level.isDay() && workplace(t, r) != null ? workplace(t, r) : home(t, r);
            net.minecraft.server.level.ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
            if (r.follows && lord != null && lord.level() == level) base = lord.blockPosition();
            BlockPos at = SettlerEntity.groundNear(level, base, level.getRandom(), 3);
            s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            s.link(t, r);
            level.addFreshEntity(s);
            r.entity = s.getUUID();
            spawned++;
        }
    }

    /** A spot on the ground a set distance from a centre, on one of four sides. */
    public static BlockPos near(ServerLevel level, BlockPos c, int side, int dist) {
        int x = c.getX() + (side == 0 ? dist : side == 1 ? -dist : 0);
        int z = c.getZ() + (side == 2 ? dist : side == 3 ? -dist : 0);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Where a resident spends the working day, or null if they have no fixed place. */
    public static BlockPos workplace(Territory t, Resident r) {
        switch (r.job) {
            case STAFF -> {
                Building b = t.building(r.workplace);
                if (b != null) return b.center();
            }
            case CONSTRUCTION -> {
                Building b = siteOf(t, r);
                if (b != null) return b.footprint() ? b.center() : t.center;
            }
            case LOGGING, QUARRYING, FARMING, MINING -> {
                Building f = field(t, r);
                if (f != null) return f.center();
            }
            case ADMINISTRATION -> {
                return manorSpot(t, r);
            }
            default -> { }
        }
        return null;
    }

    private record Beds(long at, List<BlockPos> heads) {}

    private static final java.util.Map<Integer, Beds> BEDS = new java.util.HashMap<>();
    private static final java.util.Set<Integer> FURNISHED = new java.util.HashSet<>();

    /**
     * Every bed in the village, the manor's first and then each courtyard's (the heads, in a
     * fixed order). Looked up again every half minute.
     */
    public static List<BlockPos> beds(ServerLevel level, Territory t) {
        Beds b = BEDS.get(t.id);
        if (b != null && level.getGameTime() - b.at() < 600) return b.heads();
        List<BlockPos> heads = new ArrayList<>();
        if (Village.manorHousing(t) > 0 && t.manorTier <= 1) scanBeds(level, t.center.offset(-4, 0, -4), 9, 9, heads);
        for (Building bd : t.buildings) {
            if (bd.complete && bd.type.housing() > 0) scanBeds(level, bd.origin, bd.sizeX(), bd.sizeZ(), heads);
        }
        BEDS.put(t.id, new Beds(level.getGameTime(), heads));
        return heads;
    }

    /** Looks the beds up again next time (after the manor is rebuilt). */
    public static void forgetBeds(Territory t) {
        BEDS.remove(t.id);
    }

    private static void scanBeds(ServerLevel level, BlockPos o, int sx, int sz, List<BlockPos> out) {
        for (int x = 0; x < sx; x++)
            for (int z = 0; z < sz; z++)
                for (int y = 0; y <= 5; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.isLoaded(p)) continue;
                    var s = level.getBlockState(p);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.BedBlock
                            && s.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) out.add(p);
                }
    }

    /** A resident's own bed: the n-th bed for the n-th resident, or null if there are not enough. */
    public static BlockPos bedOf(ServerLevel level, Territory t, Resident r) {
        List<BlockPos> beds = beds(level, t);
        int idx = t.residents.indexOf(r);
        return idx >= 0 && idx < beds.size() ? beds.get(idx) : null;
    }

    /** The site a builder is working on: the one the village gave them, else the oldest. */
    public static Building siteOf(Territory t, Resident r) {
        Building b = t.building(r.workplace);
        if (b != null && !b.complete) return b;
        return Sites.active(t);
    }

    /** The finished field a resident with a field job works at (spread across the fields). */
    public static Building field(Territory t, Resident r) {
        List<Building> fields = new ArrayList<>();
        for (Building b : t.buildings) if (b.complete && b.type.fieldJob() == r.job && r.job != Job.IDLE) fields.add(b);
        return fields.isEmpty() ? null : fields.get(Math.floorMod(r.id, fields.size()));
    }

    /** A spot in the manor yard, each resident their own. */
    public static BlockPos manorSpot(Territory t, Resident r) {
        double a = (r.id * 137.508) % 360 * Math.PI / 180;
        // Outside the walls: once the manor is the Lord's residence, the yard beyond it.
        double rad = t.manorTier >= 2 ? 9.5 : 6.5;
        int x = t.center.getX() + (int) Math.round(Math.cos(a) * rad), z = t.center.getZ() + (int) Math.round(Math.sin(a) * rad);
        return new BlockPos(x, t.center.getY(), z);
    }

    /**
     * Where a resident sleeps: the first ten in the Lord's Manor, then twenty to each finished
     * residential courtyard. Anyone without a bed stays by the manor.
     */
    public static BlockPos home(Territory t, Resident r) {
        int idx = t.residents.indexOf(r);
        int manor = Village.manorHousing(t);
        if (idx < manor) return manorSpot(t, r);
        idx -= manor;
        for (Building b : t.buildings) {
            if (!b.complete || b.type.housing() <= 0) continue;
            if (idx < b.type.housing()) {
                BlockPos c = b.center();
                return c.offset(Math.floorMod(r.id, 5) - 2, 0, Math.floorMod(r.id / 5, 5) - 2);
            }
            idx -= b.type.housing();
        }
        return manorSpot(t, r);
    }

    public static Component title(Resident r) {
        return Component.literal(r.name);
    }
}
