package com.realmlord.client;

import com.realmlord.network.ManorActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.village.Goods;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Choosing what to sell from the storehouse: each good it holds, what it would fetch, and a Sell button for each. */
public class SellScreen extends Screen {
    private static final int W = 300, H = 220, ROWS = 9;
    private int scroll;

    public SellScreen() {
        super(Component.translatable("realmlord.sell.title"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private static List<Goods> held(CompoundTag v) {
        long[] h = v == null ? new long[0] : v.getLongArray("storeGoods");
        List<Goods> out = new ArrayList<>();
        for (Goods g : Goods.values()) if (g.ordinal() < h.length && h[g.ordinal()] > 0) out.add(g);
        return out;
    }

    private void sell(int good) {
        ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.SELL_STORAGE, good, 0));
        ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.REFRESH, 0, 0));
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        int x = left(), y = top();
        CompoundTag v = ClientVillageData.manor;
        List<Goods> rows = held(v);
        scroll = Math.max(0, Math.min(scroll, rows.size() - ROWS));
        for (int i = 0; i < ROWS && i + scroll < rows.size(); i++) {
            Goods g = rows.get(i + scroll);
            addRenderableWidget(Button.builder(Component.translatable("realmlord.sell.one"), b -> sell(g.ordinal()))
                    .bounds(x + W - 58, y + 34 + i * 18, 48, 14).build());
        }
        Button all = Button.builder(Component.translatable("realmlord.sell.all"), b -> sell(-1)).bounds(x + 10, y + H - 24, 110, 16).build();
        all.active = !rows.isEmpty();
        addRenderableWidget(all);
        addRenderableWidget(Button.builder(Component.translatable("realmlord.workshop.back"), b -> onClose()).bounds(x + W - 76, y + H - 24, 66, 16).build());
    }

    public void refresh() {
        init();
    }

    @Override
    public void tick() {
        // Keep the list in step with the storehouse after each sale.
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 10 == 0) init();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        init();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + W / 2, y + 8, Ui.GOLD);
        CompoundTag v = ClientVillageData.manor;
        List<Goods> rows = held(v);
        long[] h = v == null ? new long[0] : v.getLongArray("storeGoods");
        g.drawString(font, Component.translatable("realmlord.sell.head"), x + 10, y + 22, Ui.MUTED, false);
        if (rows.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.sell.none"), x + 10, y + 40, W - 20, Ui.MUTED);
        for (int i = 0; i < ROWS && i + scroll < rows.size(); i++) {
            Goods gd = rows.get(i + scroll);
            long units = h[gd.ordinal()];
            int ry = y + 37 + i * 18;
            g.drawString(font, Component.translatable(gd.key()), x + 10, ry, Ui.INK, false);
            g.drawString(font, String.valueOf(units), x + 120, ry, Ui.MUTED, false);
            g.drawString(font, Ui.money(gd.sellIncome(units)), x + 160, ry, Ui.GOLD, false);
        }
        if (rows.size() > ROWS) g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, rows.size() - ROWS + 1), x + 130, y + H - 20, Ui.MUTED, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        if (ClientVillageData.manor != null) minecraft.setScreen(new ManorScreen());
        else super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        Ui.restoreScale();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
