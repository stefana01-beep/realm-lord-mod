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
        if (!RealmConfig.protectTerritories() || p.hasPermissions(2)) return true;
        Territory t = at(p.serverLevel(), pos);
        if (t == null || t.owner == null) return true;
        if (lordOnly && inManor(t, pos)) return p.getUUID().equals(t.owner);
        return t.trusts(p.getUUID());
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
        if (level != null && fenced(level, event.getPos())) event.setResult(Event.Result.DENY);
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
                int r = b.fenceRadius > 0 ? b.fenceRadius : t.radius() - 2;
                long dx = pos.getX() - t.center.getX(), dz = pos.getZ() - t.center.getZ();
                if (dx * dx + dz * dz < (long) r * r) return true;
            }
        }
        return false;
    }
}
