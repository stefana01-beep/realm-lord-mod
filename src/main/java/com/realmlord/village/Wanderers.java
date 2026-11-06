package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Wandering refugees, as on Gaia's roads in the novel: small groups who drift through the
 * wilds. Any who come near a village with a free bed settle there; with no bed for them they
 * move on, and after a couple of days they are gone.
 */
public final class Wanderers {
    private Wanderers() {}

    public static final String TAG = "realmlord_wanderer";
    private static final String BORN = "realmlord_born", TOLD = "realmlord_told";
    /** How long a wanderer stays about: two Minecraft days. */
    public static final long LIFETIME = 48000;
    /** How close to a village's border a wanderer must come to settle there. */
    public static final int REACH = 48;

    /** Now and then (about once in five minutes) a few wanderers appear in the wilds near a player. */
    public static void tick(ServerPlayer sp) {
        if (sp.tickCount % 1200 != 600 || sp.isSpectator()) return;
        ServerLevel level = sp.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        RandomSource r = level.getRandom();
        if (r.nextInt(5) != 0) return;
        AABB near = sp.getBoundingBox().inflate(160, 64, 160);
        if (level.getEntitiesOfClass(SettlerEntity.class, near, e -> e.getTags().contains(TAG)).size() >= 4) return;
        double a = r.nextDouble() * Math.PI * 2, d = 40 + r.nextInt(30);
        int x = (int) (sp.getX() + Math.cos(a) * d), z = (int) (sp.getZ() + Math.sin(a) * d);
        BlockPos at = new BlockPos(x, 64, z);
        if (!level.isLoaded(at)) return;
        for (Territory t : TerritoryData.get(level.getServer()).all()) {
            if (t.dimension.equals(level.dimension()) && t.center.distSqr(at) < (double) t.radius() * t.radius()) return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y, z);
        if (!level.getFluidState(ground.below()).isEmpty() || !level.getFluidState(ground).isEmpty()) return;
        spawn(level, ground, 1 + r.nextInt(2));
    }

    public static int spawn(ServerLevel level, BlockPos at, int count) {
        int n = 0;
        for (int i = 0; i < count; i++) {
            SettlerEntity s = ModEntities.SETTLER.get().create(level);
            if (s == null) continue;
            s.moveTo(at.getX() + 0.5 + i, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            s.addTag(TAG);
            s.getPersistentData().putLong(BORN, level.getGameTime());
            level.addFreshEntity(s);
            n++;
        }
        return n;
    }

    /** A wanderer's look around: settles in a village nearby with a free bed, or leaves when its time is up. */
    public static void check(ServerLevel level, SettlerEntity s) {
        long born = s.getPersistentData().getLong(BORN);
        if (born == 0) s.getPersistentData().putLong(BORN, born = level.getGameTime());
        Territory full = null;
        for (Territory t : TerritoryData.get(level.getServer()).all()) {
            if (!t.dimension.equals(level.dimension()) || !t.settled) continue;
            double reach = t.radius() + REACH;
            if (s.distanceToSqr(t.center.getX() + 0.5, s.getY(), t.center.getZ() + 0.5) > reach * reach) continue;
            if (t.waitingSettlers > 0 || Village.openBeds(t) <= 0) {
                full = t;
                continue;
            }
            Resident r = Resident.random(level.getRandom(), Resident.randomOccupation(level.getRandom(), 1.0), t.grade.ordinal());
            if (!s.getSettlerName().isEmpty()) r.name = s.getSettlerName();
            r.skin = s.getSkin();
            Village.add(t, r);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1, s.getZ(), 10, 0.4, 0.6, 0.4, 0.0);
            ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
            if (owner != null) Notify.player(owner, Component.translatable("realmlord.village.wanderer_joined", r.name, t.name));
            TerritoryData.get(level.getServer()).touch();
            s.discard();
            return;
        }
        if (full != null && !s.getPersistentData().getBoolean(TOLD)) {
            s.getPersistentData().putBoolean(TOLD, true);
            ServerPlayer owner = full.owner == null ? null : level.getServer().getPlayerList().getPlayer(full.owner);
            if (owner != null) Notify.player(owner, Component.translatable("realmlord.village.wanderer_no_bed", full.name));
        }
        if (level.getGameTime() - born > LIFETIME) s.discard();
    }
}
