package com.realmlord.entity.beast;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;

/** The health bar a beast king shows those who face it. */
public final class KingBar {
    private final ServerBossEvent bar;

    public KingBar(String key, BossEvent.BossBarColor color) {
        bar = new ServerBossEvent(Component.translatable(key), color, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    public ServerBossEvent bar() {
        return bar;
    }

    public void update(LivingEntity e) {
        bar.setProgress(e.getHealth() / e.getMaxHealth());
        if (e.getCustomName() != null) bar.setName(e.getCustomName());
    }
}
