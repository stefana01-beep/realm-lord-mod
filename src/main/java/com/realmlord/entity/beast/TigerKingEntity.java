package com.realmlord.entity.beast;

import com.realmlord.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The White Tiger King of the deep mountains: the most terrible beast in the wilds. */
public class TigerKingEntity extends MountainTigerEntity {
    private final KingBar bar = new KingBar("entity.realmlord.tiger_king", BossEvent.BossBarColor.PURPLE);

    public TigerKingEntity(EntityType<? extends Ocelot> type, Level level) {
        super(type, level);
        xpReward = 120;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    public float baseScale() {
        return 2.4F;
    }

    @Override
    public void customServerAiStep() {
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
        spawnAtLocation(new ItemStack(ModItems.WHITE_TIGER_PELT.get()));
        spawnAtLocation(new ItemStack(ModItems.TIGER_BONE.get(), 3));
        spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get(), 5));
        spawnAtLocation(com.realmlord.loot.Blueprints.random(random, true));
    }
}
