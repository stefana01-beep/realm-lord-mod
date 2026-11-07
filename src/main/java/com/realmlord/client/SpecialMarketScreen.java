package com.realmlord.client;

import com.realmlord.loot.Gear;
import com.realmlord.network.ManorActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.Market;
import com.realmlord.village.Village;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The market's special-items platform: the Armoury, selling materials and graded arms and
 * armour; and the Lord's pack, each special item in it with its price and its own Sell button.
 */
public class SpecialMarketScreen extends Screen {
    private static final int W = 330, H = 232, ROWS = 9;
    private boolean buying;
    private int scroll;
    /** Pictures of the wares at their grades (what is sold is made fresh, with its own bonuses). */
    private final List<ItemStack> shown = new ArrayList<>();

    public SpecialMarketScreen(boolean buying) {
        super(Component.translatable("realmlord.market.special.title"));
        this.buying = buying;
        RandomSource r = RandomSource.create(7);
        for (Market.Ware w : Market.WARES) shown.add(w.stack(r));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    /** Inventory slots holding something the market buys. */
    private List<Integer> sellable() {
        List<Integer> out = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) return out;
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (Village.specialPrice(inv.getItem(i)) > 0) out.add(i);
        return out;
    }

    private void send(ManorActionPacket.Action a, int arg) {
        ModNetwork.toServer(new ManorActionPacket(a, arg, 0));
        ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.REFRESH, 0, 0));
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        int x = left(), y = top();
        Button buy = Button.builder(Component.translatable("realmlord.market.special.buy_tab"), b -> {
            buying = true;
            scroll = 0;
            init();
        }).bounds(x + 10, y + 22, 100, 16).build();
        buy.active = !buying;
        addRenderableWidget(buy);
        Button sell = Button.builder(Component.translatable("realmlord.market.special.sell_tab"), b -> {
            buying = false;
            scroll = 0;
            init();
        }).bounds(x + 114, y + 22, 100, 16).build();
        sell.active = buying;
        addRenderableWidget(sell);
        if (buying) {
            scroll = Math.max(0, Math.min(scroll, Market.WARES.size() - ROWS));
            for (int i = 0; i < ROWS && i + scroll < Market.WARES.size(); i++) {
                int idx = i + scroll;
                addRenderableWidget(Button.builder(Component.translatable("realmlord.market.buy"), b -> send(ManorActionPacket.Action.BUY_WARE, idx))
                        .bounds(x + W - 58, y + 46 + i * 18, 48, 16).build());
            }
        } else {
            List<Integer> slots = sellable();
            scroll = Math.max(0, Math.min(scroll, slots.size() - ROWS));
            for (int i = 0; i < ROWS && i + scroll < slots.size(); i++) {
                int slot = slots.get(i + scroll);
                addRenderableWidget(Button.builder(Component.translatable("realmlord.sell.one"), b -> send(ManorActionPacket.Action.SELL_SPECIAL, slot))
                        .bounds(x + W - 58, y + 46 + i * 18, 48, 16).build());
            }
            Button all = Button.builder(Component.translatable("realmlord.sell.all"), b -> send(ManorActionPacket.Action.SELL_SPECIAL, -1))
                    .bounds(x + 10, y + H - 24, 110, 16).build();
            all.active = !slots.isEmpty();
            addRenderableWidget(all);
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.workshop.back"), b -> onClose()).bounds(x + W - 76, y + H - 24, 66, 16).build());
    }

    @Override
    public void tick() {
        if (!buying && minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 10 == 0) init();
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
        long purse = com.realmlord.network.ClientProfileData.purse;
        Component p = Component.translatable("realmlord.manor.purse", Ui.money(purse));
        g.drawString(font, p, x + W - 12 - font.width(p), y + 26, Ui.MUTED, false);
        ItemStack hovered = ItemStack.EMPTY;
        if (buying) {
            for (int i = 0; i < ROWS && i + scroll < Market.WARES.size(); i++) {
                Market.Ware w = Market.WARES.get(i + scroll);
                ItemStack st = shown.get(i + scroll);
                int ry = y + 46 + i * 18;
                g.renderItem(st, x + 10, ry);
                if (w.count() > 1) g.drawString(font, "x" + w.count(), x + 26, ry + 8, Ui.INK, false);
                g.drawString(font, st.getHoverName(), x + 40, ry + 4, Ui.INK, false);
                TokenGrade gr = w.grade();
                if (gr != null) g.drawString(font, Component.translatable(gr.key()), x + 170, ry + 4, Ui.JADE, false);
                g.drawString(font, Ui.money(w.price()), x + 222, ry + 4, Ui.GOLD, false);
                if (mx >= x + 10 && mx < x + 26 && my >= ry && my < ry + 16) hovered = st;
            }
            if (Market.WARES.size() > ROWS) g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, Market.WARES.size() - ROWS + 1), x + 12, y + H - 20, Ui.MUTED, false);
        } else if (minecraft != null && minecraft.player != null) {
            List<Integer> slots = sellable();
            if (slots.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.market.special.nothing"), x + 12, y + 50, W - 24, Ui.MUTED);
            var inv = minecraft.player.getInventory();
            for (int i = 0; i < ROWS && i + scroll < slots.size(); i++) {
                ItemStack st = inv.getItem(slots.get(i + scroll));
                int ry = y + 46 + i * 18;
                g.renderItem(st, x + 10, ry);
                g.renderItemDecorations(font, st, x + 10, ry);
                g.drawString(font, st.getHoverName(), x + 32, ry + 4, Ui.INK, false);
                TokenGrade gr = Gear.gradeOf(st);
                if (gr != null) g.drawString(font, Component.translatable(gr.key()), x + 170, ry + 4, Ui.JADE, false);
                long each = Math.round(Village.specialPrice(st) * st.getCount() * (1.0 - Market.SALES_TAX));
                g.drawString(font, Ui.money(each), x + 222, ry + 4, Ui.GOLD, false);
                if (mx >= x + 10 && mx < x + 26 && my >= ry && my < ry + 16) hovered = st;
            }
        }
        super.render(g, mx, my, pt);
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mx, my);
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
