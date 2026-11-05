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
        list.sort(Comparator.comparing((Resident r) -> !r.isTalent()).thenComparingInt(r -> r.id));
        return list.subList(0, Math.min(list.size(), RealmConfig.visibleResidents()));
    }

    public static void sync(ServerLevel level, Territory t) {
        if (!level.isLoaded(t.center)) return;
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
            BlockPos at = near(level, t.center, level.getRandom().nextInt(4), 9);
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

    /** Where a resident spends the working day. */
    public static BlockPos workplace(Territory t, Resident r) {
        switch (r.job) {
            case STAFF -> {
                Building b = t.building(r.workplace);
                if (b != null) return b.center();
            }
            case CONSTRUCTION -> {
                for (Building b : t.buildings) if (!b.complete) return b.type.placed() ? b.center() : t.center;
            }
            case LOGGING, QUARRYING, FARMING, MINING -> {
                List<Building> fields = new ArrayList<>();
                for (Building b : t.buildings) if (b.complete && b.type.fieldJob() == r.job) fields.add(b);
                if (!fields.isEmpty()) return fields.get(Math.floorMod(r.id, fields.size())).center();
            }
            default -> { }
        }
        return null;
    }

    public static Component title(Resident r) {
        return Component.literal(r.name);
    }
}
