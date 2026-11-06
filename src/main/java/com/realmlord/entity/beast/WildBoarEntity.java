package com.realmlord.entity.beast;

import com.realmlord.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A wild boar of Gaia's forests: bad-tempered, charges anyone who comes near, good eating. */
public class WildBoarEntity extends Hoglin implements Sized {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> SIZE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(WildBoarEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

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

    public WildBoarEntity(EntityType<? extends Hoglin> type, Level level) {
        super(type, level);
        setImmuneToZombification(true);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Beasts.think(this);
    }

    @Override
    public boolean canBeHunted() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        spawnAtLocation(new ItemStack(Items.PORKCHOP, 1 + random.nextInt(2 + looting)));
        spawnAtLocation(new ItemStack(ModItems.BOAR_HIDE.get()));
        if (random.nextFloat() < 0.35F + 0.1F * looting) spawnAtLocation(new ItemStack(ModItems.BOAR_TUSK.get()));
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return Beasts.raid(this) == null && !getTags().contains(Beasts.DEN_TAG);
    }
}
