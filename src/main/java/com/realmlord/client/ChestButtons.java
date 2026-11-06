package com.realmlord.client;

import com.realmlord.RealmLord;
import com.realmlord.network.ChestActionPacket;
import com.realmlord.network.ModNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sort, Take all and Store all on every chest, barrel and shulker box. */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChestButtons {
    private ChestButtons() {}

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> s) || s instanceof StorehouseScreen) return;
        var menu = s.getMenu();
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) return;
        int x = s.getGuiLeft() + s.getXSize() - 138, y = Math.max(2, s.getGuiTop() - 18);
        event.addListener(Button.builder(Component.translatable("realmlord.chest.sort"), b -> send(menu.containerId, ChestActionPacket.SORT))
                .bounds(x, y, 42, 16).tooltip(Tooltip.create(Component.translatable("realmlord.chest.sort.tip"))).build());
        event.addListener(Button.builder(Component.translatable("realmlord.chest.take_all"), b -> send(menu.containerId, ChestActionPacket.TAKE_ALL))
                .bounds(x + 44, y, 46, 16).tooltip(Tooltip.create(Component.translatable("realmlord.chest.take_all.tip"))).build());
        event.addListener(Button.builder(Component.translatable("realmlord.chest.store_all"), b -> send(menu.containerId, ChestActionPacket.STORE_ALL))
                .bounds(x + 92, y, 46, 16).tooltip(Tooltip.create(Component.translatable("realmlord.chest.store_all.tip"))).build());
    }

    private static void send(int id, int action) {
        ModNetwork.toServer(new ChestActionPacket(id, action));
    }
}
