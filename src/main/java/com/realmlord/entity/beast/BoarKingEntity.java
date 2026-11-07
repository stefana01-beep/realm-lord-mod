package com.realmlord.entity.beast;

import com.realmlord.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** The Boar King, lord of a den: huge, savage, and rich in spoils. */
public class BoarKingEntity extends WildBoarEntity {
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.realmlord.boar_king"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public BoarKingEntity(EntityType<? extends Hoglin> type, Level level) {
        super(type, level);
        xpReward = 80;
        setPersistenceRequired();
    }

    @Override
    public float baseScale() {
        return 1.6F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        if (getCustomName() != null) bar.setName(getCustomName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        spawnAtLocation(new ItemStack(ModItems.BOAR_KING_TUSK.get()));
        spawnAtLocation(new ItemStack(ModItems.BOAR_HIDE.get(), 4));
        spawnAtLocation(new ItemStack(ModItems.BOAR_TUSK.get(), 2));
        spawnAtLocation(new ItemStack(Items.PORKCHOP, 8));
        spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get(), 3));
        if (random.nextFloat() < 0.6F) spawnAtLocation(com.realmlord.loot.Blueprints.random(random, true));
    }
}
