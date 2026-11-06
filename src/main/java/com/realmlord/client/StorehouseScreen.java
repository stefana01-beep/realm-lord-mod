package com.realmlord.client;

import com.realmlord.menu.StorehouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/** The manor storehouse: a double chest with pages, Sort, Take all and Store all. Closing it returns to the manor. */
public class StorehouseScreen extends ContainerScreen {
    public StorehouseScreen(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private StorehouseMenu store() {
        return (StorehouseMenu) menu;
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        int y = Math.max(2, topPos - 18), x = leftPos;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(StorehouseMenu.PREV)).bounds(x, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.storage.prev"))).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(StorehouseMenu.NEXT)).bounds(x + 18, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.storage.next"))).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.chest.sort"), b -> click(StorehouseMenu.SORT)).bounds(x + 38, y, 42, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.storage.sort.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.chest.take_all"), b -> click(StorehouseMenu.TAKE_ALL)).bounds(x + 82, y, 46, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.chest.take_all.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.chest.store_all"), b -> click(StorehouseMenu.STORE_ALL)).bounds(x + 130, y, 46, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.chest.store_all.tip"))).build());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        Component page = Component.translatable("realmlord.storage.page", store().page() + 1, store().pages);
        g.drawString(font, page, imageWidth - 8 - font.width(page), titleLabelY, 0x404040, false);
    }

    @Override
    public void onClose() {
        super.onClose();
        // Back to the manor panel, to keep working on the village.
        if (minecraft != null && ClientVillageData.manor != null) minecraft.setScreen(new ManorScreen());
    }
}
