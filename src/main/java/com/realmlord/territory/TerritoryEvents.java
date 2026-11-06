package com.realmlord.territory;

import com.realmlord.RealmConfig;
import com.realmlord.RealmLord;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeping a territory its Lord's: strangers can't break, build or open chests there, the Lord's
 * Manor chests are the Lord's alone, and no monsters spawn inside a finished fence.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerritoryEvents {
    private TerritoryEvents() {}

    /** The territory containing a spot, or null. */
    public static Territory at(ServerLevel level, BlockPos pos) {
        for (Territory t : TerritoryData.get(level.getServer()).all()) if (t.contains(level.dimension(), pos)) return t;
        return null;
    }

    /** Within the Lord's Manor and its yard. */
    public static boolean inManor(Territory t, BlockPos pos) {
        return Math.abs(pos.getX() - t.center.getX()) <= 8 && Math.abs(pos.getZ() - t.center.getZ()) <= 8
                && pos.getY() >= t.center.getY() - 3 && pos.getY() <= t.center.getY() + 18;
    }

    /** Whether a player may change things at a spot (and, with manor true, use the Lord's own chests). */
    public static boolean allowed(ServerPlayer p, BlockPos pos, boolean lordOnly) {
        if (p.hasPermissions(2)) return true;
        return allowed(p.serverLevel(), p.getUUID(), pos, lordOnly);
    }

    /** As above, for a player who is not an operator. */
    public static boolean allowed(ServerLevel level, java.util.UUID player, BlockPos pos, boolean lordOnly) {
        if (!RealmConfig.protectTerritories()) return true;
        Territory t = at(level, pos);
        if (t == null || t.owner == null) return true;
        if (lordOnly && inManor(t, pos)) return player.equals(t.owner);
        return t.trusts(player);
    }

    private static void refuse(ServerPlayer p, BlockPos pos) {
        Territory t = at(p.serverLevel(), pos);
        p.displayClientMessage(Component.translatable("realmlord.protect.denied", t == null ? "" : t.name), true);
    }

    @SubscribeEvent
    public static void broke(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p)) return;
        if (!allowed(p, event.getPos(), false)) {
            event.setCanceled(true);
            refuse(p, event.getPos());
        }
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!allowed(p, event.getPos(), false)) {
            event.setCanceled(true);
            refuse(p, event.getPos());
            p.inventoryMenu.sendAllDataToRemote();
        }
    }

    @SubscribeEvent
    public static void used(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        BlockPos pos = event.getPos();
        if (!(p.level().getBlockEntity(pos) instanceof Container)) return;
        if (!allowed(p, pos, true)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            refuse(p, pos);
        }
    }

    /** Monsters don't spawn inside a finished fence. */
    @SubscribeEvent
    public static void spawning(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!RealmConfig.safeInsideFence() || event.getEntityType().getCategory() != MobCategory.MONSTER) return;
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.PATROL) return;
        ServerLevel level = event.getLevel().getLevel();
        if (level != null && (fenced(level, event.getPos()) || graceAt(level, event.getPos()))) event.setResult(Event.Result.DENY);
    }

    /** How far beyond a new village's border its grace keeps the wilds back. */
    public static final int GRACE_MARGIN = 32;

    /** A village whose fence is not yet finished is under grace: its settlers are left alone. */
    public static boolean inGrace(Territory t) {
        int ring1 = com.realmlord.territory.TerritoryLevel.villageWall(1);
        for (Building b : t.buildings) {
            if (b.type != BuildingType.SIMPLE_FENCE || b.demolishing) continue;
            // The grace is for clearing the ground and raising the first, Grade 1 wall. A Lord who
            // goes straight for a greater wall gives it up at once.
            int r = b.fenceRadius > 0 ? b.fenceRadius : t.level.wall();
            if (r != ring1) return false;
            if (b.complete || b.placed > 0 && b.fenceStyle > 0 && b.progress >= b.workNeeded) return false;
        }
        return true;
    }

    /**
     * Within a new village's grace: inside its border or just beyond it, before its fence is
     * built. No beast or bandit spawns there, so a village has time to raise its wall.
     */
    public static boolean graceAt(ServerLevel level, BlockPos pos) {
        for (Territory t : TerritoryData.get(level.getServer()).all()) {
            if (!t.dimension.equals(level.dimension()) || !inGrace(t)) continue;
            long r = t.radius() + GRACE_MARGIN;
            long dx = pos.getX() - t.center.getX(), dz = pos.getZ() - t.center.getZ();
            if (dx * dx + dz * dz < r * r) return true;
        }
        return false;
    }

    /** Inside a territory's finished fence. */
    public static boolean fenced(ServerLevel level, BlockPos pos) {
        for (Territory t : TerritoryData.get(level.getServer()).all()) {
            if (!t.dimension.equals(level.dimension())) continue;
            for (Building b : t.buildings) {
                if (b.type != BuildingType.SIMPLE_FENCE || b.demolishing) continue;
                // A fence being refitted still keeps the village safe; a new one only once it is finished.
                if (!b.complete && b.fenceStyle == 0 && b.placed == 0) continue;
                if (!b.complete && b.progress < b.workNeeded) continue;
                int r = b.fenceRadius > 0 ? b.fenceRadius : t.level.wall();
                long dx = pos.getX() - t.center.getX(), dz = pos.getZ() - t.center.getZ();
                if (dx * dx + dz * dz < (long) r * r) return true;
            }
        }
        return false;
    }
}
