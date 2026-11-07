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
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.<net.minecraft.world.inventory.ChestMenu, StorehouseScreen>register(
                com.realmlord.registry.ModMenus.STOREHOUSE.get(), StorehouseScreen::new));
        // The tiger-bone bow draws like a bow.
        event.enqueueWork(() -> {
            for (net.minecraft.world.item.Item bow : java.util.List.of(com.realmlord.registry.ModArms.TIGER_BONE_BOW.get(), com.realmlord.registry.ModArms.COMPOSITE_BOW.get())) {
                net.minecraft.client.renderer.item.ItemProperties.register(bow, new net.minecraft.resources.ResourceLocation("pull"),
                        (stack, level, e, seed) -> e == null || e.getUseItem() != stack ? 0.0F : (stack.getUseDuration() - e.getUseItemRemainingTicks()) / 20.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(bow, new net.minecraft.resources.ResourceLocation("pulling"),
                        (stack, level, e, seed) -> e != null && e.isUsingItem() && e.getUseItem() == stack ? 1.0F : 0.0F);
            }
        });
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "realm_hud", RealmHud.OVERLAY);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BANDIT.get(), ctx -> new HumanRenderer<>(ctx, HumanRenderer::banditSkin));
        event.registerEntityRenderer(ModEntities.FIRE_OIL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.STONE_SHOT.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 2.5F, false));
        BeastRenderers.register(event);
        event.registerEntityRenderer(ModEntities.SETTLER.get(), ctx -> new HumanRenderer<>(ctx, HumanRenderer::settlerSkin));
        event.registerEntityRenderer(ModEntities.FISHING_BOAT.get(), ctx -> new ShipRenderer<>(ctx, com.realmlord.ship.ShipKind.FISHING_BOAT));
        event.registerEntityRenderer(ModEntities.CIMA_BOAT.get(), ctx -> new ShipRenderer<>(ctx, com.realmlord.ship.ShipKind.CIMA_BOAT));
        event.registerEntityRenderer(ModEntities.MERCHANT_SHIP.get(), ctx -> new ShipRenderer<>(ctx, com.realmlord.ship.ShipKind.MERCHANT_SHIP));
        event.registerEntityRenderer(ModEntities.MENGCHONG.get(), ctx -> new ShipRenderer<>(ctx, com.realmlord.ship.ShipKind.MENGCHONG));
        event.registerEntityRenderer(ModEntities.TURRETED_SHIP.get(), ctx -> new ShipRenderer<>(ctx, com.realmlord.ship.ShipKind.TURRETED_SHIP));
    }
}
