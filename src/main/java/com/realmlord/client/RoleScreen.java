package com.realmlord.client;

import com.realmlord.network.ChooseRolePacket;
import com.realmlord.network.ClientProfileData;
import com.realmlord.network.ModNetwork;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.WorkOccupation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * First-join screen: pick Lord or Adventurer, then an occupation (for a Lord, the
 * sub-profession). Combat occupations have levels and skills; work occupations have only
 * a craft level.
 */
public class RoleScreen extends Screen {
    private static final int W = 340, H = 220;

    private int step;              // 0 = mode, 1 = occupation
    private PlayMode mode = PlayMode.LORD;
    private boolean combatTab = true;
    private int selected = -1;

    public RoleScreen() {
        super(Component.translatable("realmlord.role.title"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = left(), y = top();
        if (step == 0) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.role.pick_lord"), b -> pick(PlayMode.LORD))
                    .bounds(x + 12, y + H - 30, 150, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.role.pick_adventurer"), b -> pick(PlayMode.ADVENTURER))
                    .bounds(x + W - 162, y + H - 30, 150, 20).build());
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.role.tab_combat"), b -> tab(true))
                .bounds(x + 12, y + 34, 90, 16).build()).active = !combatTab;
        addRenderableWidget(Button.builder(Component.translatable("realmlord.role.tab_work"), b -> tab(false))
                .bounds(x + 106, y + 34, 90, 16).build()).active = combatTab;
        int count = combatTab ? CombatClass.values().length : WorkOccupation.values().length;
        int cols = combatTab ? 2 : 2;
        int bw = 88, bh = 16;
        for (int i = 0; i < count; i++) {
            final int idx = i;
            Component name = Component.translatable(combatTab ? CombatClass.byId(i).key() : WorkOccupation.byId(i).key());
            Button b = Button.builder(name, btn -> {
                selected = idx;
                init();
            }).bounds(x + 12 + (i % cols) * (bw + 4), y + 56 + (i / cols) * (bh + 2), bw, bh).build();
            b.active = selected != i;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.role.back"), b -> {
            step = 0;
            selected = -1;
            init();
        }).bounds(x + 12, y + H - 26, 70, 18).build());
        Button confirm = Button.builder(Component.translatable("realmlord.role.confirm"), b -> confirm())
                .bounds(x + W - 132, y + H - 26, 120, 18).build();
        confirm.active = selected >= 0;
        addRenderableWidget(confirm);
    }

    private void pick(PlayMode m) {
        mode = m;
        step = 1;
        selected = -1;
        init();
    }

    private void tab(boolean combat) {
        combatTab = combat;
        selected = -1;
        init();
    }

    private void confirm() {
        if (selected < 0) return;
        ModNetwork.toServer(new ChooseRolePacket(mode, combatTab, selected));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + W / 2, y + 8, Ui.GOLD);
        if (step == 0) {
            Ui.centered(g, font, Component.translatable("realmlord.role.subtitle"), x + W / 2, y + 20, Ui.MUTED);
            int bw = (W - 36) / 2;
            box(g, x + 12, y + 36, bw, H - 74, Component.translatable("realmlord.mode.lord"), lordText());
            box(g, x + 24 + bw, y + 36, bw, H - 74, Component.translatable("realmlord.mode.adventurer"),
                    Component.translatable("realmlord.role.adventurer_desc"));
        } else {
            Component heading = Component.translatable(mode == PlayMode.LORD ? "realmlord.role.choose_sub" : "realmlord.role.choose_occ");
            Ui.centered(g, font, heading, x + W / 2, y + 20, Ui.MUTED);
            int dx = x + 200, dw = W - 212;
            Ui.inset(g, dx, y + 34, dw, H - 70);
            if (selected >= 0) {
                Component name = Component.translatable(combatTab ? CombatClass.byId(selected).key() : WorkOccupation.byId(selected).key());
                Component desc = Component.translatable(combatTab ? CombatClass.byId(selected).descKey() : WorkOccupation.byId(selected).descKey());
                g.drawString(font, name, dx + 5, y + 39, Ui.GOLD, false);
                Ui.wrap(g, font, desc, dx + 5, y + 52, dw - 10, Ui.INK);
            } else {
                Ui.wrap(g, font, Component.translatable(combatTab ? "realmlord.role.combat_desc" : "realmlord.role.work_desc"),
                        dx + 5, y + 39, dw - 10, Ui.MUTED);
            }
        }
        super.render(g, mx, my, pt);
    }

    private Component lordText() {
        Component base = Component.translatable("realmlord.role.lord_desc");
        if (ClientProfileData.restriction) return base.copy().append(" ").append(Component.translatable("realmlord.role.lord_restricted"));
        return base;
    }

    private void box(GuiGraphics g, int x, int y, int w, int h, Component head, Component body) {
        Ui.inset(g, x, y, w, h);
        Ui.centered(g, font, head, x + w / 2, y + 5, Ui.GOLD);
        Ui.wrap(g, font, body, x + 5, y + 19, w - 10, Ui.INK);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
