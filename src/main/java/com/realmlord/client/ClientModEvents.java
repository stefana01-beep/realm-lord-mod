package com.realmlord.client;

import com.realmlord.RealmLord;
import com.realmlord.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "realm_hud", RealmHud.OVERLAY);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BANDIT.get(), ctx -> new HumanRenderer<>(ctx, HumanRenderer::banditSkin));
        event.registerEntityRenderer(ModEntities.SETTLER.get(), ctx -> new HumanRenderer<>(ctx, HumanRenderer::settlerSkin));
    }
}
