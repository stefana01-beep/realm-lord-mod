package com.realmlord.client;

import com.realmlord.network.BuildingActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Job;
import com.realmlord.village.Occupation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/**
 * A workshop's, school's or dojo's own window: its master, what it makes (and the choice of what
 * to make), the people learning there, and who else could be sent. Without a building chosen,
 * the list of buildings that have a window. Closing it returns to the manor.
 */
public class BuildingScreen extends Screen {
    private static final int W = 360, H = 236, ROWS = 7;
    private int scroll, learnerScroll;

    public BuildingScreen() {
        super(Component.translatable("realmlord.workshop.title"));
    }

    private CompoundTag v() {
        return ClientVillageData.building;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    public void refresh() {
        init();
    }

    private void send(BuildingActionPacket.Action a, int building, int arg) {
        ModNetwork.toServer(new BuildingActionPacket(building, a, arg));
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        CompoundTag v = v();
        int x = left(), y = top();
        addRenderableWidget(Button.builder(Component.translatable("realmlord.workshop.back"), b -> onClose())
                .bounds(x + W - 74, y + H - 22, 66, 16).build());
        if (v == null) return;
        if (!v.contains("id")) {
            // The list of buildings with windows.
            ListTag list = v.getList("list", Tag.TAG_COMPOUND);
            scroll = Math.max(0, Math.min(scroll, list.size() - ROWS * 2));
            for (int i = 0; i < ROWS * 2 && i + scroll < list.size(); i++) {
                CompoundTag c = list.getCompound(i + scroll);
                BuildingType bt = BuildingType.byOrdinal(c.getInt("type"));
                int id = c.getInt("id");
                int col = i / ROWS, row = i % ROWS;
                addRenderableWidget(Button.builder(Component.translatable(bt.key()), b -> send(BuildingActionPacket.Action.OPEN, id, 0))
                        .bounds(x + 10 + col * 172, y + 40 + row * 26, 164, 16).build());
            }
            return;
        }
        int id = v.getInt("id");
        addRenderableWidget(Button.builder(Component.translatable("realmlord.workshop.all"), b -> send(BuildingActionPacket.Action.OPEN, -1, 0))
                .bounds(x + 8, y + H - 22, 90, 16).build());
        // What it makes.
        ListTag modes = v.getList("modes", Tag.TAG_COMPOUND);
        for (int i = 0; i < modes.size(); i++) {
            final int m = i;
            Button b = Button.builder(Component.translatable(modes.getCompound(i).getString("key")), btn -> send(BuildingActionPacket.Action.MODE, id, m))
                    .bounds(x + 10 + i * 112, y + 50, 108, 16).build();
            b.active = v.getInt("mode") != i;
            addRenderableWidget(b);
        }
        if (v.getInt("places") <= 0) return;
        int ly = modes.isEmpty() ? y + 46 : y + 96;
        // Learners: call one back.
        ListTag ls = v.getList("learners", Tag.TAG_COMPOUND);
        learnerScroll = Math.max(0, Math.min(learnerScroll, ls.size() - rows(ly)));
        for (int i = 0; i < rows(ly) && i + learnerScroll < ls.size(); i++) {
            int rid = ls.getCompound(i + learnerScroll).getInt("id");
            addRenderableWidget(Button.builder(Component.literal("x"), b -> send(BuildingActionPacket.Action.DISMISS, id, rid))
                    .bounds(x + 156, ly + 12 + i * 16, 14, 13).tooltip(Tooltip.create(Component.translatable("realmlord.workshop.dismiss"))).build());
        }
        // Who else could learn: send one.
        ListTag cs = v.getList("candidates", Tag.TAG_COMPOUND);
        boolean full = ls.size() >= v.getInt("places");
        scroll = Math.max(0, Math.min(scroll, cs.size() - rows(ly)));
        for (int i = 0; i < rows(ly) && i + scroll < cs.size(); i++) {
            int rid = cs.getCompound(i + scroll).getInt("id");
            Button b = Button.builder(Component.translatable("realmlord.workshop.enrol"), btn -> send(BuildingActionPacket.Action.ENROL, id, rid))
                    .bounds(x + W - 52, ly + 12 + i * 16, 42, 13).build();
            b.active = !full;
            addRenderableWidget(b);
        }
    }

    private int rows(int ly) {
        return Math.max(1, (top() + H - 30 - (ly + 12)) / 16);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int d = -(int) Math.signum(delta);
        if (mx < left() + 178) learnerScroll = Math.max(0, learnerScroll + d);
        else scroll = Math.max(0, scroll + d);
        init();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        CompoundTag v = v();
        if (v == null) {
            super.render(g, mx, my, pt);
            return;
        }
        if (!v.contains("id")) {
            Ui.centered(g, font, Component.translatable("realmlord.workshop.list"), x + W / 2, y + 8, Ui.GOLD);
            ListTag list = v.getList("list", Tag.TAG_COMPOUND);
            if (list.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.workshop.none"), x + 12, y + 40, W - 24, Ui.MUTED);
            for (int i = 0; i < ROWS * 2 && i + scroll < list.size(); i++) {
                CompoundTag c = list.getCompound(i + scroll);
                int col = i / ROWS, row = i % ROWS;
                Component line = c.contains("staff") ? Component.translatable("realmlord.workshop.list_line", c.getString("staff"), c.getInt("learners"), c.getInt("places"))
                        : Component.translatable("realmlord.workshop.no_master");
                g.drawString(font, line, x + 12 + col * 172, y + 57 + row * 26, Ui.MUTED, false);
            }
            if (list.size() > ROWS * 2) g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, list.size() - ROWS * 2 + 1), x + 12, y + H - 18, Ui.MUTED, false);
            super.render(g, mx, my, pt);
            return;
        }
        BuildingType bt = BuildingType.byOrdinal(v.getInt("type"));
        Ui.centered(g, font, Component.translatable(bt.key()), x + W / 2, y + 8, Ui.GOLD);
        Component master = v.contains("staff")
                ? Component.translatable("realmlord.workshop.master", v.getString("staff"), Component.translatable("realmlord.work.tier." + v.getInt("staffRank")))
                : v.contains("staffOcc") ? Component.translatable("realmlord.workshop.no_master_of", Component.translatable(Occupation.byId(v.getInt("staffOcc")).key()))
                : Component.empty();
        g.drawString(font, master, x + 10, y + 24, v.contains("staff") ? Ui.INK : Ui.RED, false);
        ListTag modes = v.getList("modes", Tag.TAG_COMPOUND);
        if (!modes.isEmpty()) {
            g.drawString(font, Component.translatable("realmlord.workshop.makes"), x + 10, y + 38, Ui.GOLD, false);
            CompoundTag m = modes.getCompound(Math.max(0, Math.min(modes.size() - 1, v.getInt("mode"))));
            Component what = m.getBoolean("items")
                    ? Component.translatable("realmlord.workshop.makes_items", fmt(m.getDouble("outN")), m.contains("in") ? Component.translatable(m.getString("in")) : Component.empty(), fmt(m.getDouble("inN")))
                    : m.contains("in") ? Component.translatable("realmlord.workshop.makes_stock_from", fmt(m.getDouble("outN")), Component.translatable(m.getString("out")), fmt(m.getDouble("inN")), Component.translatable(m.getString("in")))
                    : Component.translatable("realmlord.workshop.makes_stock", fmt(m.getDouble("outN")), Component.translatable(m.getString("out")));
            Ui.wrap(g, font, what, x + 10, y + 70, W - 20, Ui.MUTED);
        }
        if (v.getInt("places") > 0) {
            int ly = modes.isEmpty() ? y + 46 : y + 96;
            Occupation teaches = Occupation.byId(v.getInt("teaches"));
            ListTag ls = v.getList("learners", Tag.TAG_COMPOUND);
            Ui.inset(g, x + 6, ly - 4, 168, y + H - 26 - ly);
            Ui.inset(g, x + 180, ly - 4, W - 186, y + H - 26 - ly);
            g.drawString(font, Component.translatable("realmlord.workshop.learners", ls.size(), v.getInt("places"), Component.translatable(teaches.key())), x + 10, ly, Ui.GOLD, false);
            for (int i = 0; i < rows(ly) && i + learnerScroll < ls.size(); i++) {
                CompoundTag c = ls.getCompound(i + learnerScroll);
                int ry = ly + 14 + i * 16;
                g.drawString(font, c.getString("name"), x + 10, ry, Ui.INK, false);
                String days = (c.getInt("half") / 2) + "/" + (c.getInt("need") / 2);
                g.drawString(font, days, x + 150 - font.width(days), ry, Ui.MUTED, false);
            }
            if (ls.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.workshop.no_learners"), x + 10, ly + 14, 160, Ui.GREY);
            ListTag cs = v.getList("candidates", Tag.TAG_COMPOUND);
            g.drawString(font, Component.translatable("realmlord.workshop.candidates", cs.size()), x + 184, ly, Ui.GOLD, false);
            for (int i = 0; i < rows(ly) && i + scroll < cs.size(); i++) {
                CompoundTag c = cs.getCompound(i + scroll);
                int ry = ly + 14 + i * 16;
                Occupation occ = Occupation.byId(c.getInt("occ"));
                g.drawString(font, c.getString("name"), x + 184, ry, occ == teaches ? Ui.JADE : Ui.INK, false);
                Component job = Component.translatable(Job.byId(c.getInt("job")).key());
                g.drawString(font, font.plainSubstrByWidth(Component.translatable(occ.key()).getString() + " · " + job.getString(), 70), x + 250, ry, Ui.MUTED, false);
            }
        }
        super.render(g, mx, my, pt);
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.1f", d);
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
