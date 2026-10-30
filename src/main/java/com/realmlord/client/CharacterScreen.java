package com.realmlord.client;

import com.realmlord.network.ClientProfileData;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.ProfileActionPacket;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.Skill;
import com.realmlord.profile.Stat;
import com.realmlord.profile.Title;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.quest.VillageQuest;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The character panel (J): Character, Village Creation quest and Territory tabs. */
public class CharacterScreen extends Screen {
    public static final int TAB_CHARACTER = 0, TAB_QUEST = 1, TAB_TERRITORY = 2;
    private static final int W = 340, H = 226;

    private int tab;
    private final List<Button> plus = new ArrayList<>();
    private Button submit, claim, abandon;
    private boolean confirmAbandon;

    public CharacterScreen(int tab) {
        super(Component.translatable("realmlord.panel.title"));
        this.tab = tab;
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
        plus.clear();
        int x = left(), y = top();
        String[] tabs = {"realmlord.panel.tab.character", "realmlord.panel.tab.quest", "realmlord.panel.tab.territory"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = idx;
                confirmAbandon = false;
                init();
            }).bounds(x + 8 + i * 84, y + 20, 80, 16).build();
            b.active = tab != i;
            addRenderableWidget(b);
        }
        if (tab == TAB_CHARACTER) {
            for (Stat s : Stat.values()) {
                final int idx = s.ordinal();
                Button b = Button.builder(Component.literal("+"), btn -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.ALLOCATE, idx)))
                        .bounds(x + 150, y + 158 + idx * 14, 12, 12).build();
                plus.add(addRenderableWidget(b));
            }
        } else if (tab == TAB_QUEST) {
            submit = addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.quest.submit"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.QUEST_SUBMIT))).bounds(x + 10, y + H - 26, 120, 18).build());
            claim = addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.quest.claim"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.QUEST_CLAIM))).bounds(x + 10, y + H - 26, 120, 18).build());
            abandon = addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.quest.abandon"), b -> {
                if (!confirmAbandon) {
                    confirmAbandon = true;
                    b.setMessage(Component.translatable("realmlord.panel.quest.abandon_confirm").withStyle(ChatFormatting.RED));
                } else {
                    confirmAbandon = false;
                    ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.QUEST_ABANDON));
                }
            }).bounds(x + W - 130, y + H - 26, 120, 18).build());
        }
        updateButtons();
    }

    @Override
    public void tick() {
        updateButtons();
    }

    private void updateButtons() {
        boolean canSpend = ClientProfileData.points > 0;
        for (Button b : plus) b.visible = canSpend;
        if (tab == TAB_QUEST && submit != null) {
            int st = ClientProfileData.questStage;
            boolean lord = ClientProfileData.mode == PlayMode.LORD.ordinal();
            submit.visible = lord && st == VillageQuest.Stage.SUPPLIES.ordinal();
            claim.visible = lord && st == VillageQuest.Stage.CLEARED.ordinal();
            abandon.visible = lord && (st == VillageQuest.Stage.SUPPLIES.ordinal() || st == VillageQuest.Stage.BANDITS.ordinal());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + W / 2, y + 7, Ui.GOLD);
        switch (tab) {
            case TAB_CHARACTER -> renderCharacter(g, x, y, mx, my);
            case TAB_QUEST -> renderQuest(g, x, y);
            default -> renderTerritory(g, x, y);
        }
        super.render(g, mx, my, pt);
        if (tab == TAB_CHARACTER) tooltips(g, x, y, mx, my);
    }

    // ------------------------------------------------------------------ character tab

    private Component occupationName() {
        if (ClientProfileData.combat >= 0) return Component.translatable(CombatClass.byId(ClientProfileData.combat).key());
        if (ClientProfileData.work >= 0) return Component.translatable(WorkOccupation.byId(ClientProfileData.work).key());
        return Component.literal("-");
    }

    private void renderCharacter(GuiGraphics g, int x, int y, int mx, int my) {
        int lx = x + 10, ly = y + 42;
        Ui.inset(g, lx - 3, ly - 3, 160, H - 48);
        boolean lord = ClientProfileData.mode == PlayMode.LORD.ordinal();
        g.drawString(font, minecraft.player.getName(), lx, ly, Ui.GOLD, false);
        Component modeLine = Component.translatable(lord ? "realmlord.panel.lord_line" : "realmlord.panel.adventurer_line", occupationName());
        g.drawString(font, modeLine, lx, ly + 11, Ui.INK, false);

        int yy = ly + 25;
        if (lord) {
            Title t = Title.byId(ClientProfileData.title);
            g.drawString(font, Component.translatable(t.key()), lx, yy, Ui.GOLD, false);
            Title next = t.next();
            long prev = t.merit;
            if (next != null) {
                double frac = (ClientProfileData.merit - prev) / (double) (next.merit - prev);
                Ui.bar(g, lx, yy + 11, 150, 3, frac, 0xFFE8C15A);
                g.drawString(font, Component.translatable("realmlord.panel.merit", ClientProfileData.merit, next.merit), lx, yy + 16, Ui.MUTED, false);
            } else {
                g.drawString(font, Component.translatable("realmlord.panel.merit_max", ClientProfileData.merit), lx, yy + 16, Ui.MUTED, false);
            }
            yy += 30;
        }
        boolean pureWork = !lord && ClientProfileData.work >= 0;
        if (!pureWork) {
            g.drawString(font, Component.translatable("realmlord.panel.level", ClientProfileData.level), lx, yy, Ui.INK, false);
            Ui.bar(g, lx, yy + 10, 150, 3, ClientProfileData.xp / (double) Math.max(1, ClientProfileData.xpNext), 0xFF6FCF97);
            yy += 17;
        }
        if (ClientProfileData.work >= 0) {
            int wl = ClientProfileData.workLevel;
            g.drawString(font, Component.translatable("realmlord.panel.work_level",
                    Component.translatable(WorkOccupation.tierKey(wl)), occupationName(), wl), lx, yy, Ui.INK, false);
            double frac = wl >= WorkOccupation.MAX_LEVEL ? 1.0 : ClientProfileData.workXp / (double) Math.max(1, ClientProfileData.workNext);
            Ui.bar(g, lx, yy + 10, 150, 3, frac, 0xFF5FA8D3);
            yy += 17;
        }

        // Attributes (the + buttons sit at y + 158 + i*14)
        int sy = y + 146;
        g.drawString(font, Component.translatable("realmlord.panel.attributes"), lx, sy, Ui.GOLD, false);
        if (ClientProfileData.points > 0) {
            Component pts = Component.translatable("realmlord.panel.points", ClientProfileData.points);
            g.drawString(font, pts, lx + 150 - font.width(pts), sy, Ui.JADE, false);
        }
        for (Stat s : Stat.values()) {
            int ry = y + 160 + s.ordinal() * 14;
            g.drawString(font, Component.translatable(s.key()), lx, ry, Ui.INK, false);
            String v = String.valueOf(ClientProfileData.stat(s.ordinal()));
            g.drawString(font, v, lx + 120 - font.width(v), ry, Ui.GOLD, false);
            int added = ClientProfileData.added[s.ordinal()];
            if (added > 0) g.drawString(font, "+" + added, lx + 123, ry, Ui.MUTED, false);
        }

        // Skills
        int rx = x + 178, ry = y + 42;
        Ui.inset(g, rx - 3, ry - 3, W - 185, H - 48);
        g.drawString(font, Component.translatable("realmlord.panel.skills"), rx, ry, Ui.GOLD, false);
        if (ClientProfileData.combat < 0) {
            Ui.wrap(g, font, Component.translatable("realmlord.panel.no_skills"), rx, ry + 14, W - 195, Ui.MUTED);
        } else {
            int sy2 = ry + 14;
            for (Skill s : Skill.of(CombatClass.byId(ClientProfileData.combat))) {
                boolean unlocked = ClientProfileData.level >= s.unlockLevel;
                boolean sel = s.ordinal() == ClientProfileData.selectedSkill;
                if (sel) g.fill(rx - 2, sy2 - 2, rx + W - 191, sy2 + 30, 0x40E8C15A);
                g.drawString(font, Component.translatable(s.key()), rx, sy2, unlocked ? (sel ? Ui.GOLD : Ui.INK) : Ui.GREY, false);
                Component meta = unlocked ? Component.translatable("realmlord.panel.cooldown", s.cooldownSeconds)
                        : Component.translatable("realmlord.panel.unlocks", s.unlockLevel);
                g.drawString(font, meta, rx + W - 193 - font.width(meta), sy2, Ui.MUTED, false);
                Ui.wrap(g, font, Component.translatable(s.descKey()), rx, sy2 + 10, W - 195, unlocked ? Ui.MUTED : Ui.GREY);
                sy2 += 46;
            }
            g.drawString(font, Component.translatable("realmlord.panel.skill_keys",
                    KeyBindings.USE_SKILL.getTranslatedKeyMessage(), KeyBindings.CYCLE_SKILL.getTranslatedKeyMessage()),
                    rx, y + H - 14, Ui.MUTED, false);
        }
    }

    private void tooltips(GuiGraphics g, int x, int y, int mx, int my) {
        for (Stat s : Stat.values()) {
            int ry = y + 160 + s.ordinal() * 14;
            if (Ui.inside(mx, my, x + 10, ry - 2, 135, 12)) {
                g.renderTooltip(font, font.split(Component.translatable(s.descKey()), 200), mx, my);
            }
        }
    }

    // ------------------------------------------------------------------ quest tab

    private void renderQuest(GuiGraphics g, int x, int y) {
        int lx = x + 12, ly = y + 44, w = W - 24;
        Ui.inset(g, lx - 4, ly - 4, w + 8, H - 78);
        g.drawString(font, Component.translatable("realmlord.panel.quest.name"), lx, ly, Ui.GOLD, false);
        boolean lord = ClientProfileData.mode == PlayMode.LORD.ordinal();
        int st = ClientProfileData.questStage;
        VillageQuest.Stage stage = VillageQuest.Stage.values()[Math.max(0, Math.min(st, VillageQuest.Stage.values().length - 1))];
        int yy = ly + 14;
        if (!lord && stage != VillageQuest.Stage.FAILED) {
            Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.adventurer"), lx, yy, w, Ui.MUTED);
            return;
        }
        switch (stage) {
            case NOT_STARTED -> Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.not_started"), lx, yy, w, Ui.MUTED);
            case FAILED -> Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.failed"), lx, yy, w, Ui.RED);
            case CLAIMED -> {
                TokenGrade gr = TokenGrade.byId(Math.max(0, ClientProfileData.questGrade));
                Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.claimed", Component.translatable(gr.key())), lx, yy, w, Ui.INK);
            }
            default -> {
                yy = objective(g, lx, yy, w, stage.ordinal() > VillageQuest.Stage.SUPPLIES.ordinal(),
                        Component.translatable("realmlord.panel.quest.obj_supplies",
                                ClientProfileData.logs, VillageQuest.LOGS_NEEDED, ClientProfileData.stone, VillageQuest.STONE_NEEDED,
                                ClientProfileData.food, VillageQuest.FOOD_NEEDED));
                Component campObj = ClientProfileData.hasCamp
                        ? Component.translatable("realmlord.panel.quest.obj_bandits", ClientProfileData.campX, ClientProfileData.campZ, ClientProfileData.banditsLeft)
                        : Component.translatable("realmlord.panel.quest.obj_bandits_hidden");
                yy = objective(g, lx, yy, w, stage == VillageQuest.Stage.CLEARED, campObj);
                if (ClientProfileData.chiefKilled || ClientProfileData.cacheFound) {
                    yy = objective(g, lx, yy, w, ClientProfileData.cacheFound, Component.translatable("realmlord.panel.quest.obj_hidden"));
                }
                if (stage == VillageQuest.Stage.CLEARED) {
                    yy = Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.ready"), lx, yy + 4, w, Ui.JADE);
                }
                long days = ClientProfileData.questElapsed / 24000L;
                long hours = (ClientProfileData.questElapsed % 24000L) / 1000L;
                double limit = ClientProfileData.questLimit / 24000.0;
                g.drawString(font, Component.translatable("realmlord.panel.quest.time", days, hours, String.format("%.1f", limit)),
                        lx, y + H - 46, Ui.MUTED, false);
                g.drawString(font, Component.translatable("realmlord.panel.quest.deaths", ClientProfileData.deaths), lx + 200, y + H - 46,
                        ClientProfileData.deaths == 0 ? Ui.MUTED : Ui.RED, false);
            }
        }
    }

    private int objective(GuiGraphics g, int x, int y, int w, boolean done, Component text) {
        g.drawString(font, done ? "✔" : "○", x, y, done ? Ui.JADE : Ui.GOLD, false);
        return Ui.wrap(g, font, text, x + 12, y, w - 12, done ? Ui.MUTED : Ui.INK) + 3;
    }

    // ------------------------------------------------------------------ territory tab

    private void renderTerritory(GuiGraphics g, int x, int y) {
        int lx = x + 12, ly = y + 44, w = W - 24;
        Ui.inset(g, lx - 4, ly - 4, w + 8, H - 52);
        if (!ClientProfileData.hasTerritory) {
            boolean lord = ClientProfileData.mode == PlayMode.LORD.ordinal();
            Ui.wrap(g, font, Component.translatable(lord ? "realmlord.panel.territory.none_lord" : "realmlord.panel.territory.none_adventurer"),
                    lx, ly, w, Ui.MUTED);
            g.drawString(font, Component.translatable("realmlord.panel.territory.world", ClientProfileData.villagesFounded,
                    Component.translatable(ClientProfileData.goldClaimed ? "realmlord.panel.territory.gold_taken" : "realmlord.panel.territory.gold_free")),
                    lx, y + H - 20, Ui.MUTED, false);
            return;
        }
        TerritoryLevel lvl = TerritoryLevel.byId(ClientProfileData.territoryLevel);
        TokenGrade grade = TokenGrade.byId(ClientProfileData.territoryGrade);
        g.drawString(font, Component.literal(ClientProfileData.territoryName), lx, ly, Ui.GOLD, false);
        g.drawString(font, Component.translatable("realmlord.panel.territory.kind", Component.translatable(grade.key()),
                Component.translatable(lvl.kindKey()), lvl.grade), lx, ly + 12, grade.color, false);
        int yy = ly + 28;
        yy = line(g, lx, yy, "realmlord.panel.territory.population", ClientProfileData.population);
        yy = line(g, lx, yy, "realmlord.panel.territory.seat", ClientProfileData.cx, ClientProfileData.cy, ClientProfileData.cz);
        yy = line(g, lx, yy, "realmlord.panel.territory.border", ClientProfileData.radius);
        yy = line(g, lx, yy, "realmlord.panel.territory.order", ClientProfileData.foundedOrder);
        TerritoryLevel next = lvl.next();
        if (next != null) {
            yy += 6;
            g.drawString(font, Component.translatable("realmlord.panel.territory.next", Component.translatable(next.kindKey()), next.grade), lx, yy, Ui.GOLD, false);
            yy += 12;
            boolean popOk = ClientProfileData.population >= next.population;
            boolean titleOk = ClientProfileData.title >= next.title.ordinal();
            yy = check(g, lx, yy, popOk, Component.translatable("realmlord.panel.territory.req_pop", ClientProfileData.population, next.population));
            yy = check(g, lx, yy, titleOk, Component.translatable("realmlord.panel.territory.req_title", Component.translatable(next.title.key())));
            yy = check(g, lx, yy, false, Component.translatable("realmlord.panel.territory.req_infra"));
            if (next.minIndex > 0) yy = check(g, lx, yy, false, Component.translatable("realmlord.panel.territory.req_index", next.minIndex));
        }
        Ui.wrap(g, font, Component.translatable("realmlord.panel.territory.coming"), lx, y + H - 30, w, Ui.GREY);
    }

    private int line(GuiGraphics g, int x, int y, String key, Object... args) {
        g.drawString(font, Component.translatable(key, args), x, y, Ui.INK, false);
        return y + 11;
    }

    private int check(GuiGraphics g, int x, int y, boolean ok, Component text) {
        g.drawString(font, ok ? "✔" : "✖", x, y, ok ? Ui.JADE : Ui.RED, false);
        g.drawString(font, text, x + 12, y, ok ? Ui.MUTED : Ui.INK, false);
        return y + 11;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KeyBindings.OPEN_PANEL.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
