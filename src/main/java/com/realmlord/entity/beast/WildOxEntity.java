package com.realmlord.entity.beast;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/** A wild ox of the grasslands: grazes in peace, but gores whoever attacks the herd. */
public class WildOxEntity extends Cow implements Sized {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> SIZE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(WildOxEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SIZE, 1.0F);
    }

    @Override
    public float getSize() {
        return entityData.get(SIZE);
    }

    @Override
    public void setSize(float size) {
        entityData.set(SIZE, size);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return super.getDimensions(pose).scale(getSize());
    }

    public WildOxEntity(EntityType<? extends Cow> type, Level level) {
        super(type, level);
        xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Cow.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public InteractionResult mobInteract(Player p, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        spawnAtLocation(new ItemStack(Items.BEEF, 2 + random.nextInt(3 + looting)));
        spawnAtLocation(new ItemStack(Items.LEATHER, 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.4F) spawnAtLocation(new ItemStack(com.realmlord.registry.ModItems.OX_HORN.get()));
    }
}
