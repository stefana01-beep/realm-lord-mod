package com.realmlord.entity.beast;

import com.realmlord.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A mountain tiger of the southern forests: a lone hunter, and the most dangerous beast in them. */
public class MountainTigerEntity extends Ocelot implements Sized {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> SIZE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MountainTigerEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

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

    public MountainTigerEntity(EntityType<? extends Ocelot> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    public float baseScale() {
        return 1.7F;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.5F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Never shy of people, unlike its little cousin. */
    @Override
    protected void reassessTrustingGoals() {
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public InteractionResult mobInteract(Player p, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        Beasts.think(this);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return Beasts.raid(this) == null && !getTags().contains(Beasts.DEN_TAG);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        spawnAtLocation(new ItemStack(ModItems.TIGER_PELT.get()));
        if (random.nextFloat() < 0.5F + 0.1F * looting) spawnAtLocation(new ItemStack(ModItems.TIGER_BONE.get()));
    }
}
