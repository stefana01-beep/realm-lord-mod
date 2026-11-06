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
        event.registerEntityRenderer(ModEntities.FIRE_OIL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WILD_BOAR.get(), net.minecraft.client.renderer.entity.HoglinRenderer::new);
        event.registerEntityRenderer(ModEntities.BOAR_KING.get(), ctx -> new net.minecraft.client.renderer.entity.HoglinRenderer(ctx) {
            @Override
            protected void scale(net.minecraft.world.entity.monster.hoglin.Hoglin e, com.mojang.blaze3d.vertex.PoseStack pose, float pt) {
                pose.scale(1.6F, 1.6F, 1.6F);
            }
        });
        event.registerEntityRenderer(ModEntities.GREY_WOLF.get(), net.minecraft.client.renderer.entity.WolfRenderer::new);
        event.registerEntityRenderer(ModEntities.MOUNTAIN_TIGER.get(), ctx -> new net.minecraft.client.renderer.entity.OcelotRenderer(ctx) {
            @Override
            protected void scale(net.minecraft.world.entity.animal.Ocelot e, com.mojang.blaze3d.vertex.PoseStack pose, float pt) {
                pose.scale(1.7F, 1.7F, 1.7F);
            }
        });
        event.registerEntityRenderer(ModEntities.SNOW_BEAR.get(), net.minecraft.client.renderer.entity.PolarBearRenderer::new);
        event.registerEntityRenderer(ModEntities.SETTLER.get(), ctx -> new HumanRenderer<>(ctx, HumanRenderer::settlerSkin));
    }
}
