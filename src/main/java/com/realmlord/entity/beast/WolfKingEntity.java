package com.realmlord.entity.beast;

import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** The Wolf King: a great black-grey wolf that leads the biggest packs of the northern woods. */
public class WolfKingEntity extends GreyWolfEntity {
    private final KingBar bar = new KingBar("entity.realmlord.wolf_king", BossEvent.BossBarColor.WHITE);

    public WolfKingEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public float baseScale() {
        return 1.8F;
    }

    /** He never runs alone: three of his pack come with him. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance d, MobSpawnType reason, SpawnGroupData data, net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, d, reason, data, tag);
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
            for (int i = 0; i < 3; i++) {
                GreyWolfEntity w = ModEntities.GREY_WOLF.get().create(level.getLevel());
                if (w == null) continue;
                w.moveTo(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2, random.nextFloat() * 360F, 0F);
                level.addFreshEntity(w);
            }
        }
        return out;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.update(this);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.bar().addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.bar().removePlayer(p);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        spawnAtLocation(new ItemStack(ModItems.WOLF_PELT.get(), 3));
        spawnAtLocation(new ItemStack(ModItems.WOLF_KING_FANG.get()));
        spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get(), 1));
        if (random.nextFloat() < 0.4F) spawnAtLocation(com.realmlord.loot.Blueprints.random(random, true));
    }
}
