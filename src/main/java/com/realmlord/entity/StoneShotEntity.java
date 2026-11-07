package com.realmlord.entity;

import com.realmlord.registry.ModCrafts;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.TerritoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A trebuchet's stone shot. It lands hard: everyone within two and a half blocks is struck and
 * thrown back, and outside any territory the timber, fences and wool where it lands are smashed,
 * so a trebuchet can break open a water bandits' palisade or a bandit camp.
 */
public class StoneShotEntity extends ThrowableItemProjectile {
    public static final double GRAVITY = 0.03;
    public static final double RADIUS = 2.5;
    private double power = 1.0;

    public StoneShotEntity(EntityType<? extends StoneShotEntity> type, Level level) {
        super(type, level);
    }

    public StoneShotEntity(Level level, double x, double y, double z) {
        super(ModEntities.STONE_SHOT.get(), x, y, z, level);
    }

    public void setPower(double p) {
        power = p;
    }

    @Override
    protected Item getDefaultItem() {
        return ModCrafts.STONE_SHOT.get();
    }

    /**
     * The launch velocity to land on a point: a high lob at 50 degrees, with the speed found by
     * flying the shot as the game will (gravity then drag each tick) for a range of speeds.
     */
    public static Vec3 aim(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double ang = Math.toRadians(50);
        double best = 1.0, bestErr = Double.MAX_VALUE;
        for (double v = 0.4; v <= 4.0; v += 0.02) {
            double h = v * Math.cos(ang), y = v * Math.sin(ang), px = 0, py = 0;
            for (int i = 0; i < 400; i++) {
                px += h;
                py += y;
                h *= 0.99;
                y = y * 0.99 - GRAVITY;
                if (y < 0 && py <= to.y - from.y) break;
            }
            double err = Math.abs(px - flat);
            if (err < bestErr) {
                bestErr = err;
                best = v;
            }
        }
        double h = best * Math.cos(ang);
        return new Vec3(dx / Math.max(0.001, flat) * h, best * Math.sin(ang), dz / Math.max(0.001, flat) * h);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 at = hit.getLocation();
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.2F, 0.7F);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), at.x, at.y + 0.3, at.z, 40, 1.0, 0.4, 1.0, 0.2);
        sl.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.3, at.z, 10, 1.0, 0.3, 1.0, 0.02);
        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(at)).inflate(RADIUS))) {
            double d = e.position().distanceTo(at);
            if (d > RADIUS + 0.5 || e instanceof SettlerEntity || e == owner) continue;
            float dmg = (float) ((14 - 3 * d) * power);
            if (dmg <= 0) continue;
            e.hurt(owner != null ? sl.damageSources().mobProjectile(this, owner) : sl.damageSources().fallingBlock(this), dmg);
            Vec3 push = e.position().subtract(at).normalize();
            e.knockback(1.0, -push.x, -push.z);
        }
        // Smash the timber walls of forts and camps, but nobody's territory.
        BlockPos c = BlockPos.containing(at);
        if (TerritoryData.get(sl.getServer()).at(sl.dimension(), c) == null) {
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                BlockState s = sl.getBlockState(p);
                if (s.isAir() || s.getDestroySpeed(sl, p) < 0 || s.getDestroySpeed(sl, p) > 3.0F) continue;
                if (s.is(net.minecraft.tags.BlockTags.PLANKS) || s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.FENCES)
                        || s.is(net.minecraft.tags.BlockTags.WOOL) || s.is(net.minecraft.tags.BlockTags.WOODEN_SLABS) || s.is(Blocks.HAY_BLOCK)
                        || s.is(Blocks.BARREL) || s.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)) {
                    sl.destroyBlock(p.immutable(), false);
                }
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Power", power);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        power = tag.contains("Power") ? tag.getDouble("Power") : 1.0;
    }
}
