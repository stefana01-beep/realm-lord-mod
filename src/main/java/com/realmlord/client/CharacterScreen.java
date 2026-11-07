package com.realmlord.client;

import com.realmlord.network.ClientProfileData;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.ProfileActionPacket;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Innate;
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
    public static final int TAB_CHARACTER = 0, TAB_QUEST = 1, TAB_TERRITORY = 2, TAB_ARTS = 3;
    private static final int W = 340, H = 254;

    private int tab;
    private final List<Button> plus = new ArrayList<>();
    private Button submit, claim, abandon, keepCamp, salvageCamp, travel;
    private boolean confirmAbandon;
    private int artScroll;

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
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        plus.clear();
        int x = left(), y = top();
        String[] tabs = {"realmlord.panel.tab.character", "realmlord.panel.tab.quest", "realmlord.panel.tab.territory", "realmlord.panel.tab.arts"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = idx;
                confirmAbandon = false;
                init();
            }).bounds(x + 6 + i * 82, y + 20, 80, 16).build();
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
        } else if (tab == TAB_TERRITORY && ClientProfileData.hasTerritory) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.territory.map"),
                    b -> ModNetwork.toServer(new com.realmlord.network.ManorActionPacket(com.realmlord.network.ManorActionPacket.Action.MAP, 0, 1)))
                    .bounds(x + W - 110, y + 44, 96, 16).build());
        } else if (tab == TAB_QUEST) {
            submit = addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.quest.submit"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.QUEST_SUBMIT))).bounds(x + 10, y + H - 26, 120, 18).build());
            claim = addRenderableWidget(Button.builder(Component.translatable("realmlord.panel.quest.claim"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.QUEST_CLAIM))).bounds(x + 10, y + H - 26, 120, 18).build());
            keepCamp = addRenderableWidget(Button.builder(Component.translatable("realmlord.camp.keep_button"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.CAMP_KEEP)))
                    .bounds(x + 136, y + H - 26, 96, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("realmlord.camp.keep_tip"))).build());
            salvageCamp = addRenderableWidget(Button.builder(Component.translatable("realmlord.camp.salvage_button"),
                    b -> ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.CAMP_SALVAGE)))
                    .bounds(x + 236, y + H - 26, 96, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("realmlord.camp.salvage_tip"))).build());
            boolean there = minecraft.player != null && minecraft.player.level().dimension().location().getPath().equals("bandit_lands");
            travel = addRenderableWidget(Button.builder(Component.translatable(there ? "realmlord.lands.return_button" : "realmlord.lands.travel_button"),
                    b -> minecraft.player.connection.sendCommand(there ? "realm camp return" : "realm camp travel"))
                    .bounds(x + 136, y + H - 26, 120, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("realmlord.lands.travel_tip", 50))).build());
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
            keepCamp.visible = salvageCamp.visible = lord && ClientProfileData.campChoicePending;
            boolean there = minecraft.player != null && minecraft.player.level().dimension().location().getPath().equals("bandit_lands");
            travel.visible = lord && !keepCamp.visible && (st == VillageQuest.Stage.BANDITS.ordinal() || there);
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
            case TAB_ARTS -> renderArts(g, x, y, mx, my);
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
        Component purse = Component.translatable("realmlord.panel.purse", Ui.money(ClientProfileData.purse));
        g.drawString(font, purse, x + W - 12 - font.width(purse), ly, Ui.GOLD, false);

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
            g.drawString(font, Component.translatable("realmlord.panel.level", ClientProfileData.level, ClientProfileData.reputation), lx, yy, Ui.INK, false);
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

        // Innate attributes: fixed at creation, raised only by pills or cultivation methods.
        g.drawString(font, Component.translatable("realmlord.panel.innate"), lx, y + 220, Ui.GOLD, false);
        for (Innate i : Innate.values()) {
            int ix = lx + (i.ordinal() % 2) * 78, iy = y + 232 + (i.ordinal() / 2) * 11;
            g.drawString(font, Component.translatable(i.key() + ".short"), ix, iy, Ui.INK, false);
            String v = String.valueOf(ClientProfileData.innate[i.ordinal()]);
            g.drawString(font, v, ix + 70 - font.width(v), iy, Ui.GOLD, false);
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
        for (Innate i : Innate.values()) {
            int ix = x + 10 + (i.ordinal() % 2) * 78, iy = y + 232 + (i.ordinal() / 2) * 11;
            if (Ui.inside(mx, my, ix, iy - 1, 72, 10)) {
                g.renderTooltip(font, font.split(Component.translatable(i.descKey()), 200), mx, my);
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
                    yy = objective(g, lx, yy, w, ClientProfileData.cacheFound, Component.translatable("realmlord.panel.quest.obj_treasure"));
                }
                if (stage == VillageQuest.Stage.CLEARED) {
                    yy = Ui.wrap(g, font, Component.translatable("realmlord.panel.quest.ready"), lx, yy + 4, w, Ui.JADE);
                }
                long left = Math.max(0, ClientProfileData.questLimit - ClientProfileData.questElapsed);
                long days = left / 24000L;
                long hours = (left % 24000L) / 1000L;
                Component timeLine = stage == VillageQuest.Stage.CLEARED
                        ? Component.translatable("realmlord.panel.quest.time_done", ClientProfileData.questElapsed / 24000L, (ClientProfileData.questElapsed % 24000L) / 1000L)
                        : Component.translatable("realmlord.panel.quest.time_left", days, hours);
                g.drawString(font, timeLine, lx, y + H - 58, Ui.INK, false);
                if (ClientProfileData.prospectGrade >= 0) {
                    TokenGrade pg = TokenGrade.byId(ClientProfileData.prospectGrade);
                    g.drawString(font, Component.translatable(stage == VillageQuest.Stage.CLEARED ? "realmlord.panel.quest.earned" : "realmlord.panel.quest.prospect",
                            Component.translatable(pg.key())), lx, y + H - 46, pg.color, false);
                }
            }
        }
    }

    private int objective(GuiGraphics g, int x, int y, int w, boolean done, Component text) {
        g.drawString(font, done ? "✔" : "○", x, y, done ? Ui.JADE : Ui.GOLD, false);
        return Ui.wrap(g, font, text, x + 12, y, w - 12, done ? Ui.MUTED : Ui.INK) + 3;
    }

    // ------------------------------------------------------------------ territory tab

    // ------------------------------------------------------------------ arts and manuals tab

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == TAB_ARTS) {
            artScroll = Math.max(0, artScroll - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void renderArts(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, Component.translatable("realmlord.panel.arts.title"), x + 12, y + 42, Ui.GOLD, false);
        var arts = ClientProfileData.arts;
        List<String> ids = new ArrayList<>(arts.getAllKeys());
        int rows = 6;
        artScroll = Math.max(0, Math.min(artScroll, Math.max(0, ids.size() - rows)));
        if (ids.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.panel.arts.none"), x + 12, y + 56, W - 24, Ui.MUTED);
        Component hover = null;
        for (int i = 0; i < rows && i + artScroll < ids.size(); i++) {
            String id = ids.get(i + artScroll);
            var a = com.realmlord.martial.Arts.byId(id);
            if (a == null) continue;
            var c = arts.getCompound(id);
            int ry = y + 56 + i * 16;
            int lv = c.getInt("lv");
            g.drawString(font, font.plainSubstrByWidth(Component.translatable(a.key()).getString(), 170), x + 12, ry, a.grade().color | 0xFF000000, false);
            String lvl = Component.translatable("realmlord.panel.arts.line", lv, a.maxLevel()).getString();
            g.drawString(font, lvl, x + 190, ry, Ui.INK, false);
            // Practice toward the next level.
            int bx = x + 240, bw = 86;
            g.fill(bx, ry + 2, bx + bw, ry + 7, 0xFF5A4A36);
            if (lv >= a.maxLevel()) g.fill(bx, ry + 2, bx + bw, ry + 7, 0xFFC8A24A);
            else if (c.getInt("need") > 0) g.fill(bx, ry + 2, bx + Math.min(bw, bw * c.getInt("p") / c.getInt("need")), ry + 7, 0xFF4AA07A);
            if (mx >= x + 12 && mx < x + W - 12 && my >= ry - 2 && my < ry + 12)
                hover = Component.translatable("realmlord.art.tooltip.per_level", com.realmlord.item.SecretManualItem.perLevel(a));
        }
        int[] b = ClientProfileData.artBonus;
        if (b.length == 8 && !ids.isEmpty()) {
            String[] names = {"body", "comprehension", "luck", "charm", "command", "force", "intelligence", "politics"};
            net.minecraft.network.chat.MutableComponent sum = Component.empty();
            boolean any = false;
            for (int i = 0; i < 8; i++) {
                if (b[i] <= 0) continue;
                if (any) sum.append(", ");
                sum.append(Component.translatable("realmlord.art.stat." + names[i], b[i]));
                any = true;
            }
            if (any) Ui.wrap(g, font, Component.translatable("realmlord.panel.arts.bonus", sum), x + 12, y + 156, W - 24, Ui.JADE);
        }
        // Technical manuals known, by trade.
        var known = com.realmlord.work.TechManuals.CLIENT_KNOWN;
        g.drawString(font, Component.translatable("realmlord.panel.techs.title", known.size()), x + 12, y + 180, Ui.GOLD, false);
        if (known.isEmpty()) Ui.wrap(g, font, Component.translatable("realmlord.panel.techs.none"), x + 12, y + 194, W - 24, Ui.MUTED);
        else {
            net.minecraft.network.chat.MutableComponent line = Component.empty();
            int n = 0;
            for (var t : com.realmlord.work.TechData.ALL) {
                if (!known.contains(t.id())) continue;
                if (n++ > 0) line.append(" · ");
                line.append(Component.translatable(t.key()));
            }
            Ui.wrap(g, font, line, x + 12, y + 194, W - 24, Ui.INK);
        }
        if (hover != null) g.renderTooltip(font, hover, mx, my);
    }

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
        int sx = lx + 190, sy = ly + 28;
        g.drawString(font, Component.translatable("realmlord.panel.territory.stores"), sx, sy, Ui.GOLD, false);
        line(g, sx, sy + 12, "realmlord.panel.territory.food", ClientProfileData.storeFood);
        line(g, sx, sy + 23, "realmlord.panel.territory.wood", ClientProfileData.storeWood);
        line(g, sx, sy + 34, "realmlord.panel.territory.stone", ClientProfileData.storeStone);
        line(g, sx, sy + 45, "realmlord.panel.territory.iron_ore", ClientProfileData.storeIronOre);
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

    @Override
    public void removed() {
        super.removed();
        Ui.restoreScale();
    }
}
