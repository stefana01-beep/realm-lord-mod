package com.realmlord.entity;

import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/** A thrown barrel of fire oil: on landing it sets everything close by alight. */
public class FireOilEntity extends ThrowableItemProjectile {
    public static final double RADIUS = 2.5;

    public FireOilEntity(EntityType<? extends FireOilEntity> type, Level level) {
        super(type, level);
    }

    public FireOilEntity(Level level, LivingEntity thrower) {
        super(ModEntities.FIRE_OIL.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIRE_OIL.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        burst(sl, getOwner() instanceof ServerPlayer sp ? sp : null);
        discard();
    }

    /** Flames on the ground around the burst (where the thrower may build), and every creature near it burning. */
    public void burst(ServerLevel sl, ServerPlayer thrower) {
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 0.7F);
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 40, 1.2, 0.4, 1.2, 0.02);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 12, 1.0, 0.4, 1.0, 0.01);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(RADIUS))) {
            if (e.distanceToSqr(this) > RADIUS * RADIUS * 1.6) continue;
            e.setSecondsOnFire(6);
            e.hurt(sl.damageSources().onFire(), 2.0F);
        }
        BlockPos c = blockPosition();
        int r = (int) Math.ceil(RADIUS);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (!sl.getBlockState(p).isAir() || !BaseFireBlock.canBePlacedAt(sl, p, net.minecraft.core.Direction.UP)) continue;
                    if (thrower != null && !com.realmlord.territory.TerritoryEvents.allowed(thrower, p, false)) continue;
                    sl.setBlockAndUpdate(p, BaseFireBlock.getState(sl, p));
                    break;
                }
            }
    }
}
