package com.realmlord.world;

import com.realmlord.RealmLord;
import com.realmlord.entity.beast.BoarKingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The world's own clock: the beastwave, the dens, and the deaths that matter to them. */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldEvents {
    private WorldEvents() {}

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        Beastwave.tick(event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) return;
        Dens.tick(sp);
    }

    /** No sleeping through a beastwave: the beasts are at the gates. */
    @SubscribeEvent
    public static void sleeping(net.minecraftforge.event.entity.player.PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (WorldState.get(sp.server).waveDay >= 0) {
            event.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("realmlord.beastwave.no_sleep"), true);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        var killer = event.getSource().getEntity();
        Beastwave.killed(level.getServer(), event.getEntity(), killer);
        if (event.getEntity() instanceof BoarKingEntity king) Dens.kingSlain(level, king, killer instanceof ServerPlayer p ? p : null);
    }
}
