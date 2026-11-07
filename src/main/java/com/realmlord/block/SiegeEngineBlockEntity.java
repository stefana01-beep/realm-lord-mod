package com.realmlord.block;

import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.entity.StoneShotEntity;
import com.realmlord.registry.ModBlocks;
import com.realmlord.registry.ModCrafts;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A siege engine's store of ammunition and its rhythm of fire. A bed crossbow holds sixteen heavy
 * bolts and shoots every three seconds at whatever comes within 40 blocks in front of it: the bolt
 * flies flat and fast and goes through several men. A trebuchet holds eight stone shot and lobs
 * one every eight seconds at foes 12 to 64 blocks off: it smashes everything where it lands, and
 * outside any territory it breaks timber walls. Engineers and carpenters among the summoned
 * figures make them hit harder.
 */
public class SiegeEngineBlockEntity extends BlockEntity {
    private int ammo, cooldown;

    public SiegeEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SIEGE_ENGINE.get(), pos, state);
    }

    public SiegeEngineBlock.Kind kind() {
        return getBlockState().getBlock() instanceof SiegeEngineBlock b ? b.kind : SiegeEngineBlock.Kind.BED_CROSSBOW;
    }

    public boolean crossbow() {
        return kind() == SiegeEngineBlock.Kind.BED_CROSSBOW;
    }

    public Item ammoItem() {
        return crossbow() ? ModCrafts.HEAVY_BOLT.get() : ModCrafts.STONE_SHOT.get();
    }

    public int capacity() {
        return crossbow() ? 16 : 8;
    }

    public int ammo() {
        return ammo;
    }

    public double minRange() {
        return crossbow() ? 3 : 12;
    }

    public double maxRange() {
        return crossbow() ? 40 : 64;
    }

    /** Loads up to n rounds; returns how many were taken. */
    public int load(int n) {
        int take = Math.max(0, Math.min(n, capacity() - ammo));
        ammo += take;
        setChanged();
        return take;
    }

    private Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(HorizontalDirectionalBlock.FACING) ? s.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    private Territory territory() {
        if (!(level instanceof ServerLevel sl)) return null;
        return TerritoryData.get(sl.getServer()).at(sl.dimension(), worldPosition);
    }

    private double power() {
        Territory t = territory();
        if (t == null) return 1.0;
        return 1.0 + 0.15 * (com.realmlord.village.FigureArts.count(t, "engineer") + com.realmlord.village.FigureArts.count(t, "carpenter"));
    }

    public void serverTick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (ammo <= 0 || level == null || level.getGameTime() % 10 != 0) return;
        Territory t = territory();
        if (t == null) return;
        LivingEntity target = findTarget(t);
        if (target == null) return;
        fireAt(target.position().add(0, target.getBbHeight() * 0.5, 0), target, owner(t));
    }

    private ServerPlayer owner(Territory t) {
        return t == null || t.owner == null || !(level instanceof ServerLevel sl) ? null : sl.getServer().getPlayerList().getPlayer(t.owner);
    }

    /** A foe in front of the engine, in range, that it can see. */
    private LivingEntity findTarget(Territory t) {
        Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
        Direction f = facing();
        Vec3 fwd = new Vec3(f.getStepX(), 0, f.getStepZ());
        double max = maxRange(), min = minRange();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(max), e -> e.isAlive() && hostile(e, t))) {
            Vec3 to = m.position().subtract(c);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat < min || flat > max) continue;
            if ((to.x * fwd.x + to.z * fwd.z) / Math.max(0.001, flat) < 0.5) continue;
            if (crossbow() && level.clip(new ClipContext(c.add(fwd.scale(0.8)), m.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
                    .getType() != HitResult.Type.MISS) continue;
            if (flat < bestD) {
                bestD = flat;
                best = m;
            }
        }
        return best;
    }

    private static boolean hostile(Mob m, Territory t) {
        if (m instanceof SettlerEntity) return false;
        if (m instanceof BanditEntity) return true;
        if (m.getTags().contains(com.realmlord.entity.beast.Beasts.WAVE_TAG)) return true;
        return m instanceof Enemy;
    }

    /** Shoots at where the player looks. */
    public String fireWhereLooking(ServerPlayer p) {
        if (ammo <= 0) return "realmlord.siege.empty";
        if (cooldown > 0) return "realmlord.siege.reloading";
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(maxRange()));
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        if (!crossbow() && at.distanceTo(Vec3.atCenterOf(worldPosition)) < minRange()) return "realmlord.siege.too_close";
        fireAt(at, null, p);
        return null;
    }

    private void fireAt(Vec3 at, LivingEntity target, ServerPlayer owner) {
        if (!(level instanceof ServerLevel sl)) return;
        Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
        Direction f = facing();
        Vec3 muzzle = c.add(f.getStepX() * 0.9, crossbow() ? 0.1 : 1.2, f.getStepZ() * 0.9);
        double power = power();
        if (crossbow()) {
            Arrow bolt = new Arrow(sl, muzzle.x, muzzle.y, muzzle.z);
            if (owner != null) bolt.setOwner(owner);
            Vec3 d = at.subtract(muzzle);
            double flat = Math.sqrt(d.x * d.x + d.z * d.z);
            bolt.shoot(d.x, d.y + flat * 0.02, d.z, 3.2F, 0.5F);
            bolt.setBaseDamage(4.5 * power);
            bolt.setPierceLevel((byte) 4);
            bolt.setKnockback(2);
            bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
            bolt.addTag("realmlord_heavy_bolt");
            sl.addFreshEntity(bolt);
            sl.playSound(null, worldPosition, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.6F, 0.55F);
            cooldown = 60;
        } else {
            StoneShotEntity shot = new StoneShotEntity(sl, muzzle.x, muzzle.y, muzzle.z);
            if (owner != null) shot.setOwner(owner);
            shot.setPower(power);
            Vec3 v = StoneShotEntity.aim(muzzle, at);
            shot.setDeltaMovement(v);
            sl.addFreshEntity(shot);
            sl.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.4F, 0.5F);
            cooldown = 160;
        }
        ammo--;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Ammo", ammo);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ammo = tag.getInt("Ammo");
        cooldown = tag.getInt("Cooldown");
    }
}
