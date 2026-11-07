package com.realmlord.client;

import com.realmlord.network.ClientProfileData;
import com.realmlord.network.ManorActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.profile.Title;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Goods;
import com.realmlord.village.Job;
import com.realmlord.village.Market;
import com.realmlord.village.Occupation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lord's Manor Seal: the village's command centre. Overview (the novel's territory panel,
 * treasury and upgrade), Construction (choose a building, then place it), People (jobs and
 * talents) and Market (resources and blueprints).
 */
public class ManorScreen extends Screen {
    public static final int OVERVIEW = 0, BUILD = 1, PEOPLE = 2, MARKET = 3, RECORDS = 4;
    private static final int W = 400, H = 248;
    private static final int ROW = 20;

    private static int tab = OVERVIEW;
    private int scroll;
    /** First site shown in the Construction tab's list of sites under way. */
    private int siteScroll;
    private static final int SITE_ROWS = 3;
    private int refreshTimer;

    public ManorScreen() {
        super(Component.translatable("realmlord.manor.title"));
    }

    private CompoundTag v() {
        return ClientVillageData.manor;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private static void send(ManorActionPacket.Action a, int x, long y) {
        ModNetwork.toServer(new ManorActionPacket(a, x, y));
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        CompoundTag v = v();
        if (v == null) return;
        int x = left(), y = top();
        String[] tabs = {"realmlord.manor.tab.overview", "realmlord.manor.tab.build", "realmlord.manor.tab.people", "realmlord.manor.tab.market", "realmlord.manor.tab.records"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = idx;
                scroll = 0;
                init();
            }).bounds(x + 8 + i * 64, y + 20, 62, 16).build();
            b.active = tab != i;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.manor.tab.map"), btn -> minecraft.setScreen(new TerritoryMapScreen(null)))
                .bounds(x + 8 + 5 * 64, y + 20, 64, 16).build());
        switch (tab) {
            case OVERVIEW -> initOverview(v, x, y);
            case BUILD -> initBuild(v, x, y);
            case PEOPLE -> initPeople(v, x, y);
            case RECORDS -> {
            }
            default -> initMarket(v, x, y);
        }
    }

    @Override
    public void tick() {
        if (++refreshTimer >= 40) {
            refreshTimer = 0;
            send(ManorActionPacket.Action.REFRESH, 0, 0);
        }
    }

    public void refresh() {
        init();
    }

    // ------------------------------------------------------------------ overview

    private void initOverview(CompoundTag v, int x, int y) {
        long treasury = v.getLong("treasury");
        Button wd = Button.builder(Component.translatable("realmlord.manor.withdraw"), b -> send(ManorActionPacket.Action.WITHDRAW, 0, treasury))
                .bounds(x + 120, y + 150, 70, 14).build();
        wd.active = treasury > 0;
        addRenderableWidget(wd);
        long purse = com.realmlord.network.ClientProfileData.purse;
        Button dep = Button.builder(Component.translatable("realmlord.manor.deposit"), b -> send(ManorActionPacket.Action.DEPOSIT, 0, purse))
                .bounds(x + 120, y + 166, 70, 14).tooltip(Tooltip.create(Component.translatable("realmlord.manor.deposit.tip"))).build();
        dep.active = purse > 0;
        addRenderableWidget(dep);
        if (minecraft.player != null && minecraft.player.isCreative()) {
            // Testing: run time faster (x1, x5, x20, x60).
            int now = v.getInt("speed"), next = now >= 60 ? 1 : now >= 20 ? 60 : now >= 5 ? 20 : 5;
            addRenderableWidget(Button.builder(Component.translatable("realmlord.speed.button", Math.max(1, now)), b -> send(ManorActionPacket.Action.SPEED, next, 0))
                    .bounds(x + W - 174, y + H - 24, 80, 16).tooltip(Tooltip.create(Component.translatable("realmlord.speed.tip"))).build());
        }
        Button up = Button.builder(Component.translatable("realmlord.manor.upgrade"), b -> send(ManorActionPacket.Action.UPGRADE, 0, 0))
                .bounds(x + W - 90, y + H - 24, 80, 16).build();
        up.active = v.getBoolean("canUpgrade");
        addRenderableWidget(up);
        int[] choices = v.getIntArray("freeChoices");
        for (int i = 0; i < choices.length; i++) {
            BuildingType bt = BuildingType.byOrdinal(choices[i]);
            if (bt == null) continue;
            final int ord = choices[i];
            addRenderableWidget(Button.builder(Component.translatable(bt.key()), b -> send(ManorActionPacket.Action.CHOOSE_FREE, ord, 0))
                    .bounds(x + 200 + i * 64, y + 180, 62, 14).tooltip(Tooltip.create(Component.translatable(bt.descKey()))).build());
        }
    }

    private void renderOverview(GuiGraphics g, CompoundTag v, int x, int y) {
        int lx = x + 10, ly = y + 42;
        Ui.inset(g, lx - 4, ly - 4, 186, 160);
        TerritoryLevel lvl = TerritoryLevel.byId(v.getInt("level"));
        TokenGrade grade = TokenGrade.byId(v.getInt("grade"));
        g.drawString(font, v.getString("name"), lx, ly, Ui.GOLD, false);
        g.drawString(font, Component.translatable("realmlord.panel.territory.kind", Component.translatable(grade.key()),
                Component.translatable(lvl.kindKey()), lvl.grade), lx, ly + 11, grade.color, false);
        g.drawString(font, Component.translatable("realmlord.manor.lord", minecraft.player.getName(),
                Component.translatable(Title.byId(ClientProfileData.title).key())), lx, ly + 22, Ui.INK, false);
        int yy = ly + 36;
        yy = stat(g, lx, yy, "realmlord.manor.thoughts", v.getInt("thoughts"));
        yy = stat(g, lx, yy, "realmlord.manor.security", v.getInt("security"));
        yy = line(g, lx, yy, Component.translatable("realmlord.manor.population", v.getInt("pop"), v.getInt("cap"), v.getInt("housing")));
        yy = line(g, lx, yy, Component.translatable("realmlord.manor.immigration", String.format("%.1f", v.getDouble("immigration"))));
        int[] date = v.getIntArray("date");
        if (date.length == 3) {
            boolean sowing = date[1] == 4 || date[1] == 7;
            yy = line(g, lx, yy, Component.translatable("realmlord.manor.date", date[0], date[1], date[2])
                    .append(sowing ? Component.translatable("realmlord.manor.sowing").withStyle(ChatFormatting.GREEN) : Component.empty()));
        }
        yy += 4;
        yy = line(g, lx, yy, Component.translatable("realmlord.manor.treasury", Ui.money(v.getLong("treasury"))));
        yy = line(g, lx, yy, Component.translatable("realmlord.manor.tax", Ui.money(v.getLong("tax"))));
        line(g, lx, yy + 4, Component.translatable("realmlord.manor.purse", Ui.money(com.realmlord.network.ClientProfileData.purse)));

        // Stores and indexes
        int rx = x + 202, ry = y + 42;
        Ui.inset(g, rx - 4, ry - 4, W - 208, 128);
        g.drawString(font, Component.translatable("realmlord.panel.territory.stores"), rx, ry, Ui.GOLD, false);
        long[] out = v.getLongArray("output");
        long use = v.getLong("foodUse");
        store(g, rx, ry + 12, "realmlord.panel.territory.food", v.getLong("food"), -use);
        store(g, rx, ry + 23, "realmlord.panel.territory.wood", v.getLong("wood"), out.length > 0 ? out[0] : 0);
        store(g, rx, ry + 34, "realmlord.panel.territory.stone", v.getLong("stone"), out.length > 1 ? out[1] : 0);
        store(g, rx, ry + 45, "realmlord.panel.territory.iron_ore", v.getLong("ore"), out.length > 2 ? out[2] : 0);
        int[] ix = v.getIntArray("indexes");
        String[] names = {"political", "economic", "cultural", "military"};
        g.drawString(font, Component.translatable("realmlord.manor.indexes"), rx, ry + 62, Ui.GOLD, false);
        for (int i = 0; i < 4 && i < ix.length; i++) {
            int iy = ry + 74 + i * 11;
            g.drawString(font, Component.translatable("realmlord.manor.index." + names[i]), rx, iy, Ui.INK, false);
            Ui.bar(g, rx + 70, iy + 2, 90, 4, ix[i] / 100.0, 0xFFE8C15A);
            g.drawString(font, String.valueOf(ix[i]), rx + 166, iy, Ui.MUTED, false);
        }

        // Upgrade and free building
        int uy = y + 176;
        TerritoryLevel next = lvl.next();
        if (v.getIntArray("freeChoices").length > 0) {
            g.drawString(font, Component.translatable("realmlord.manor.free_choice"), x + 200, uy - 10, Ui.JADE, false);
        }
        g.drawString(font, next == null ? Component.translatable("realmlord.manor.max_level")
                : Component.translatable("realmlord.manor.next", Component.translatable(next.kindKey()), next.grade), lx, uy + 22, Ui.GOLD, false);
        ListTag missing = v.getList("missing", Tag.TAG_STRING);
        int my = uy + 33;
        if (missing.isEmpty() && next != null) g.drawString(font, Component.translatable("realmlord.manor.ready"), lx, my, Ui.JADE, false);
        for (int i = 0; i < missing.size() && i < 3; i++) {
            Component c = Component.Serializer.fromJson(missing.getString(i));
            if (c != null) g.drawString(font, Component.literal("✖ ").append(c), lx, my + i * 10, Ui.RED, false);
        }
        if (missing.size() > 3) g.drawString(font, Component.translatable("realmlord.manor.more", missing.size() - 3), lx + 250, my + 20, Ui.MUTED, false);
    }

    private int stat(GuiGraphics g, int x, int y, String key, int value) {
        g.drawString(font, Component.translatable(key), x, y, Ui.INK, false);
        Ui.bar(g, x + 92, y + 2, 60, 4, value / 100.0, value >= 60 ? 0xFF6FCF97 : value >= 30 ? 0xFFE8C15A : 0xFFC8423B);
        g.drawString(font, String.valueOf(value), x + 158, y, Ui.MUTED, false);
        return y + 11;
    }

    private int line(GuiGraphics g, int x, int y, Component c) {
        g.drawString(font, c, x, y, Ui.INK, false);
        return y + 11;
    }

    private void store(GuiGraphics g, int x, int y, String key, long value, long perDay) {
        g.drawString(font, Component.translatable(key, value), x, y, Ui.INK, false);
        if (perDay != 0) {
            String s = (perDay > 0 ? "+" : "") + perDay + "/d";
            g.drawString(font, s, x + 180 - font.width(s), y, perDay > 0 ? Ui.JADE : Ui.RED, false);
        }
    }

    // ------------------------------------------------------------------ construction

    private List<CompoundTag> buildRows(CompoundTag v) {
        int grade = Math.max(1, TerritoryLevel.byId(v.getInt("level")).villageGrade());
        if (TerritoryLevel.byId(v.getInt("level")).villageGrade() == 0) grade = 3;
        List<CompoundTag> rows = new ArrayList<>();
        ListTag opts = v.getList("options", Tag.TAG_COMPOUND);
        for (int i = 0; i < opts.size(); i++) {
            CompoundTag o = opts.getCompound(i);
            BuildingType bt = BuildingType.byOrdinal(o.getInt("type"));
            if (bt != null && bt.grade <= grade + 1) rows.add(o);
        }
        return rows;
    }

    private static final int BUILD_ROWS = 7;

    private void initBuild(CompoundTag v, int x, int y) {
        List<CompoundTag> rows = buildRows(v);
        scroll = Math.max(0, Math.min(scroll, rows.size() - BUILD_ROWS));
        for (int i = 0; i < BUILD_ROWS && i + scroll < rows.size(); i++) {
            CompoundTag o = rows.get(i + scroll);
            BuildingType bt = BuildingType.byOrdinal(o.getInt("type"));
            boolean free = o.getBoolean("free");
            String err = o.getString("err");
            Component label = Component.translatable(free ? "realmlord.manor.place_free" : bt.placed() ? "realmlord.manor.place" : "realmlord.manor.build");
            Button b = Button.builder(label, btn -> {
                // The fence is laid out on the map too, choosing its gates.
                minecraft.setScreen(new TerritoryMapScreen(bt));
            }).bounds(x + W - 62, y + 44 + i * ROW, 52, 16).build();
            b.active = err.isEmpty();
            if (!err.isEmpty()) b.setTooltip(Tooltip.create(Component.translatable(err)));
            else b.setTooltip(Tooltip.create(Component.translatable(bt.descKey())));
            addRenderableWidget(b);
        }
        // Sites under construction
        List<CompoundTag> sites = sites(v);
        siteScroll = Math.max(0, Math.min(siteScroll, sites.size() - SITE_ROWS));
        if (sites.size() > SITE_ROWS) {
            addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                siteScroll--;
                init();
            }).bounds(x + 128, y + 184, 12, 10).build()).active = siteScroll > 0;
            addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                siteScroll++;
                init();
            }).bounds(x + 142, y + 184, 12, 10).build()).active = siteScroll + SITE_ROWS < sites.size();
        }
        int shown = 0;
        for (int i = siteScroll; i < sites.size() && shown < SITE_ROWS; i++) {
            CompoundTag b = sites.get(i);
            if (b.getBoolean("demolish")) {
                shown++;
                continue;
            }
            final int id = b.getInt("id");
            addRenderableWidget(Button.builder(Component.translatable("realmlord.manor.cancel"), btn -> send(ManorActionPacket.Action.CANCEL_SITE, id, 0))
                    .bounds(x + 184, y + 196 + shown * 12, 40, 11).tooltip(Tooltip.create(Component.translatable("realmlord.manor.cancel.tip"))).build());
            // Builders on this site: fewer, more.
            addRenderableWidget(Button.builder(Component.literal("-"), btn -> send(ManorActionPacket.Action.CREW, id, -1))
                    .bounds(x + 228, y + 196 + shown * 12, 11, 11).tooltip(Tooltip.create(Component.translatable("realmlord.manor.crew.tip"))).build());
            addRenderableWidget(Button.builder(Component.literal("+"), btn -> send(ManorActionPacket.Action.CREW, id, 1))
                    .bounds(x + 266, y + 196 + shown * 12, 11, 11).tooltip(Tooltip.create(Component.translatable("realmlord.manor.crew.tip"))).build());
            shown++;
        }
        int job = Job.CONSTRUCTION.ordinal();
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> send(ManorActionPacket.Action.SHIFT_JOB, job, -1)).bounds(x + W - 74, y + 196, 30, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> send(ManorActionPacket.Action.SHIFT_JOB, job, 1)).bounds(x + W - 42, y + 196, 30, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+5"), b -> send(ManorActionPacket.Action.SHIFT_JOB, job, 5)).bounds(x + W - 42, y + 212, 30, 14).build());
    }

    private void renderBuild(GuiGraphics g, CompoundTag v, int x, int y) {
        List<CompoundTag> rows = buildRows(v);
        int grade = TerritoryLevel.byId(v.getInt("level")).villageGrade();
        Ui.inset(g, x + 6, y + 40, W - 12, BUILD_ROWS * ROW + 4);
        for (int i = 0; i < BUILD_ROWS && i + scroll < rows.size(); i++) {
            CompoundTag o = rows.get(i + scroll);
            BuildingType bt = BuildingType.byOrdinal(o.getInt("type"));
            int ry = y + 44 + i * ROW;
            boolean locked = grade > 0 && bt.grade > grade;
            g.drawString(font, Component.translatable(bt.key()), x + 10, ry, locked ? Ui.GREY : Ui.INK, false);
            g.drawString(font, Component.translatable("realmlord.manor.grade_short", bt.grade), x + 10, ry + 9, Ui.MUTED, false);
            String cost = (bt.wood > 0 ? bt.wood + "W " : "") + (bt.stone > 0 ? bt.stone + "S " : "") + bt.hours + "h";
            g.drawString(font, cost, x + 150, ry + 4, Ui.MUTED, false);
            int cx = x + 210;
            if (bt.blueprint) {
                boolean have = o.getBoolean("blueprint"), learned = o.getBoolean("learned");
                g.drawString(font, Component.literal(have ? "✔ " : "✖ ").append(Component.translatable(learned ? "realmlord.manor.bp_learned" : "realmlord.manor.bp")),
                        cx, ry, have ? Ui.JADE : Ui.RED, false);
            }
            if (bt.specialist != null) {
                boolean have = o.getBoolean("specialist");
                g.drawString(font, Component.literal(have ? "✔ " : "✖ ").append(Component.translatable(bt.specialist.key())), cx, ry + 9, have ? Ui.JADE : Ui.RED, false);
            }
            int built = o.getInt("built");
            if (bt.requiredFor > 0 && bt.requiredFor == Math.max(1, grade)) {
                g.drawString(font, built + "/" + bt.requiredCount, x + W - 92, ry + 4, built >= bt.requiredCount ? Ui.JADE : Ui.GOLD, false);
            } else if (built > 0) {
                g.drawString(font, "x" + built, x + W - 92, ry + 4, Ui.MUTED, false);
            }
        }
        if (rows.size() > BUILD_ROWS) {
            g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, rows.size() - BUILD_ROWS + 1), x + W - 120, y + 186, Ui.MUTED, false);
        }
        // Sites
        g.drawString(font, Component.translatable("realmlord.manor.sites"), x + 10, y + 186, Ui.GOLD, false);
        List<CompoundTag> sites = sites(v);
        if (sites.size() > SITE_ROWS) {
            g.drawString(font, Component.translatable("realmlord.manor.sites_range", siteScroll + 1, Math.min(sites.size(), siteScroll + SITE_ROWS), sites.size()),
                    x + 158, y + 186, Ui.MUTED, false);
        }
        int shown = 0;
        for (int i = siteScroll; i < sites.size(); i++) {
            CompoundTag b = sites.get(i);
            if (shown < SITE_ROWS) {
                BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
                int sy = y + 197 + shown * 12;
                boolean down = b.getBoolean("demolish");
                Component label = down ? Component.translatable("realmlord.manor.demolishing", Component.translatable(bt.key()))
                        : b.getInt("stage") == 0 ? Component.translatable("realmlord.manor.clearing", Component.translatable(bt.key()))
                        : b.getInt("stage") == 1 ? Component.translatable("realmlord.manor.levelling", Component.translatable(bt.key()))
                        : Component.translatable(bt.key());
                g.drawString(font, font.plainSubstrByWidth(label.getString(), 108), x + 10, sy, down ? Ui.RED : Ui.INK, false);
                Ui.bar(g, x + 120, sy + 2, 60, 4, b.getDouble("fraction"), down ? 0xFFC8423B : 0xFF6FCF97);
                // Builders here, gold when the Lord set the number.
                String n = String.valueOf(b.getInt("builders"));
                g.drawString(font, n, x + 253 - font.width(n) / 2, sy + 2, b.getInt("crew") >= 0 ? Ui.GOLD : Ui.MUTED, false);
            }
            shown++;
        }
        if (shown == 0) g.drawString(font, Component.translatable("realmlord.manor.no_sites"), x + 10, y + 197, Ui.MUTED, false);
        else if (shown > 3) g.drawString(font, Component.translatable("realmlord.manor.more", shown - 3), x + 10, y + 233, Ui.MUTED, false);
        int[] jobs = v.getIntArray("jobs");
        int builders = jobs.length > Job.CONSTRUCTION.ordinal() ? jobs[Job.CONSTRUCTION.ordinal()] : 0;
        g.drawString(font, Component.translatable("realmlord.manor.builders", builders), x + W - 150, y + 186, builders > 0 ? Ui.INK : Ui.RED, false);
    }

    // ------------------------------------------------------------------ people

    private static final Job[] SHIFTABLE = {Job.CONSTRUCTION, Job.LOGGING, Job.QUARRYING, Job.FARMING, Job.MINING, Job.ADMINISTRATION};
    private static final int TALENT_ROWS = 8;
    /** How the talents list is ordered and which of them it shows (kept while the game runs). */
    private static int talentSort, talentFilter;
    private static final String[] SORTS = {"grade", "trade", "job", "name"};
    private static final String[] FILTERS = {"all", "figures", "soldiers", "specialists"};

    /** The talents, filtered and sorted as chosen: historical figures and the best grades first by default. */
    private static List<CompoundTag> talents(CompoundTag v) {
        ListTag all = v.getList("talents", Tag.TAG_COMPOUND);
        List<CompoundTag> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            CompoundTag r = all.getCompound(i);
            boolean keep = switch (talentFilter) {
                case 1 -> r.getBoolean("fig");
                case 2 -> r.getBoolean("mil");
                case 3 -> !r.getBoolean("fig") && Occupation.byId(r.getInt("occ")).special;
                default -> true;
            };
            if (keep) out.add(r);
        }
        java.util.Comparator<CompoundTag> byGrade = java.util.Comparator.comparing((CompoundTag r) -> !r.getBoolean("fig"))
                .thenComparing(r -> -r.getInt("grade")).thenComparing(r -> -r.getInt("rank"));
        java.util.Comparator<CompoundTag> byName = java.util.Comparator.comparing(r -> r.getString("name"));
        java.util.Comparator<CompoundTag> c = switch (talentSort) {
            case 1 -> java.util.Comparator.comparingInt((CompoundTag r) -> r.getInt("occ")).thenComparing(byGrade);
            case 2 -> java.util.Comparator.comparingInt((CompoundTag r) -> r.getInt("job")).thenComparing(byGrade);
            case 3 -> byName;
            default -> byGrade;
        };
        out.sort(c.thenComparing(byName));
        return out;
    }

    private void initPeople(CompoundTag v, int x, int y) {
        for (int i = 0; i < SHIFTABLE.length; i++) {
            final int job = SHIFTABLE[i].ordinal();
            int ry = y + 56 + i * 18;
            int[] deltas = {-5, -1, 1, 5};
            for (int k = 0; k < 4; k++) {
                final int d = deltas[k];
                addRenderableWidget(Button.builder(Component.literal(d > 0 ? "+" + d : String.valueOf(d)), b -> send(ManorActionPacket.Action.SHIFT_JOB, job, d))
                        .bounds(x + 110 + k * 22, ry - 3, 20, 14).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.workshop.open_list"),
                b -> ModNetwork.toServer(new com.realmlord.network.BuildingActionPacket(-1, com.realmlord.network.BuildingActionPacket.Action.OPEN, 0)))
                .bounds(x + 10, y + 216, 196, 14).tooltip(Tooltip.create(Component.translatable("realmlord.workshop.open_list.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.manor.sort." + SORTS[talentSort]), btn -> {
            talentSort = (talentSort + 1) % SORTS.length;
            init();
        }).bounds(x + 214, y + 54, 86, 14).tooltip(Tooltip.create(Component.translatable("realmlord.manor.sort.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.manor.filter." + FILTERS[talentFilter]), btn -> {
            talentFilter = (talentFilter + 1) % FILTERS.length;
            scroll = 0;
            init();
        }).bounds(x + 302, y + 54, W - 312, 14).tooltip(Tooltip.create(Component.translatable("realmlord.manor.filter.tip"))).build());
        List<CompoundTag> talents = talents(v);
        scroll = Math.max(0, Math.min(scroll, talents.size() - TALENT_ROWS));
        for (int i = 0; i < TALENT_ROWS && i + scroll < talents.size(); i++) {
            CompoundTag r = talents.get(i + scroll);
            final int id = r.getInt("id");
            Button b = Button.builder(Component.literal(r.getString("name")), btn -> send(ManorActionPacket.Action.OPEN_RESIDENT, id, 0))
                    .bounds(x + 214, y + 72 + i * 18, 84, 14).build();
            addRenderableWidget(b);
        }
    }

    private void renderPeople(GuiGraphics g, CompoundTag v, int x, int y) {
        int[] jobs = v.getIntArray("jobs"), places = v.getIntArray("places");
        Ui.inset(g, x + 6, y + 40, 200, 172);
        g.drawString(font, Component.translatable("realmlord.manor.jobs"), x + 10, y + 43, Ui.GOLD, false);
        for (int i = 0; i < SHIFTABLE.length; i++) {
            Job j = SHIFTABLE[i];
            int ry = y + 56 + i * 18;
            g.drawString(font, Component.translatable(j.key()), x + 10, ry, Ui.INK, false);
            int c = jobs.length > j.ordinal() ? jobs[j.ordinal()] : 0;
            int p = places.length > j.ordinal() ? places[j.ordinal()] : 0;
            String s = p >= 9999 ? String.valueOf(c) : c + "/" + p;
            g.drawString(font, s, x + 106 - font.width(s), ry, p == 0 && j != Job.CONSTRUCTION ? Ui.GREY : Ui.MUTED, false);
        }
        int idle = jobs.length > 0 ? jobs[Job.IDLE.ordinal()] : 0;
        g.drawString(font, Component.translatable("realmlord.manor.idle", idle, v.getInt("pop")), x + 10, y + 168, Ui.INK, false);
        g.drawString(font, Component.translatable("realmlord.manor.staffed", jobs.length > Job.STAFF.ordinal() ? jobs[Job.STAFF.ordinal()] : 0), x + 10, y + 180, Ui.MUTED, false);
        Ui.wrap(g, font, Component.translatable("realmlord.manor.people_hint"), x + 10, y + 194, 192, Ui.GREY);

        Ui.inset(g, x + 210, y + 40, W - 216, 172);
        List<CompoundTag> talents = talents(v);
        g.drawString(font, Component.translatable("realmlord.manor.talents", talents.size()), x + 214, y + 43, Ui.GOLD, false);
        for (int i = 0; i < TALENT_ROWS && i + scroll < talents.size(); i++) {
            CompoundTag r = talents.get(i + scroll);
            int ry = y + 75 + i * 18;
            Occupation occ = Occupation.byId(r.getInt("occ"));
            int grade = r.getInt("grade");
            Component what = r.getBoolean("mil") ? Component.translatable("realmlord.manor.soldier", Component.translatable(occ.key()), r.getInt("slv"))
                    : Component.translatable(occ.key());
            g.drawString(font, what, x + 302, ry - 3, grade >= 0 ? TokenGrade.byId(grade).color : Ui.INK, false);
            Job j = Job.byId(r.getInt("job"));
            Component work = r.contains("work") ? Component.translatable(BuildingType.byOrdinal(r.getInt("work")).key()) : Component.translatable(j.key());
            g.drawString(font, work, x + 302, ry + 6, Ui.MUTED, false);
        }
        if (talents.size() > TALENT_ROWS) {
            g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, talents.size() - TALENT_ROWS + 1), x + 214, y + 216, Ui.MUTED, false);
        }
    }

    // ------------------------------------------------------------------ market

    private static final int BP_ROWS = 5;

    /** Every site being built or taken down, oldest first. */
    private static List<CompoundTag> sites(CompoundTag v) {
        List<CompoundTag> out = new ArrayList<>();
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) if (!bs.getCompound(i).getBoolean("complete")) out.add(bs.getCompound(i));
        return out;
    }

    private List<BuildingType> blueprintRows(CompoundTag v) {
        int grade = TerritoryLevel.byId(v.getInt("level")).villageGrade();
        if (grade == 0) grade = 3;
        List<BuildingType> out = new ArrayList<>();
        for (BuildingType bt : BuildingType.values()) if (bt.blueprint && bt.grade <= grade + 1) out.add(bt);
        return out;
    }

    /** Market platforms, as in the novel: materials, special items, and two that open later. */
    private int platform;
    private static final int GOOD_ROWS = 9;

    private void initMarket(CompoundTag v, int x, int y) {
        String[] names = {"realmlord.market.platform.materials", "realmlord.market.platform.special", "realmlord.market.platform.auction", "realmlord.market.platform.negotiation"};
        for (int k = 0; k < 4; k++) {
            final int pk = k;
            Button b = Button.builder(Component.translatable(names[k]), btn -> {
                platform = pk;
                scroll = 0;
                init();
            }).bounds(x + 8 + k * 96, y + 38, 94, 14).build();
            b.active = k < 2 && k != platform;
            if (k >= 2) b.setTooltip(Tooltip.create(Component.translatable("realmlord.market.platform.locked")));
            addRenderableWidget(b);
        }
        if (platform == 0) {
            int n = Goods.values().length;
            scroll = Math.max(0, Math.min(scroll, n - GOOD_ROWS));
            for (int r = 0; r < GOOD_ROWS && r + scroll < n; r++) {
                Goods g = Goods.values()[r + scroll];
                int ry = y + 70 + r * 15;
                long[][] acts = {{10, 1}, {100, 1}, {10, 0}, {-1, 0}};
                int[] bx = {222, 264, 310, 352};
                int[] bw = {40, 44, 40, 40};
                for (int k = 0; k < 4; k++) {
                    final long units = acts[k][0];
                    final boolean buy = acts[k][1] == 1;
                    Component label = buy ? Component.translatable("realmlord.market.buy_n", units)
                            : units < 0 ? Component.translatable("realmlord.market.sell_all") : Component.translatable("realmlord.market.sell_n", units);
                    Component tip = buy ? Component.translatable("realmlord.market.cost", Ui.money(g.buyCost(units)))
                            : units < 0 ? Component.translatable("realmlord.market.sell_all.tip") : Component.translatable("realmlord.market.income", Ui.money(g.sellIncome(units)));
                    addRenderableWidget(Button.builder(label, b -> send(buy ? ManorActionPacket.Action.BUY_GOOD : ManorActionPacket.Action.SELL_GOOD, g.ordinal(), units))
                            .bounds(x + bx[k], ry - 3, bw[k], 13).tooltip(Tooltip.create(tip)).build());
                }
            }
            for (int r = 0; r < GOOD_ROWS && r + scroll < n; r++) {
                Goods g = Goods.values()[r + scroll];
                int ry = y + 70 + r * 15;
                addRenderableWidget(Button.builder(Component.translatable("realmlord.market.take"),
                                b -> send(ManorActionPacket.Action.TAKE_GOOD, g.ordinal(), hasShiftDown() ? new net.minecraft.world.item.ItemStack(g.itemOf()).getMaxStackSize() : 1))
                        .bounds(x + 190, ry - 3, 30, 13).tooltip(Tooltip.create(Component.translatable("realmlord.market.take.tip",
                                new net.minecraft.world.item.ItemStack(g.itemOf()).getHoverName(), g.item().units()))).build());
            }
            int by = y + H - 30;
            addRenderableWidget(Button.builder(Component.translatable("realmlord.storage.open"), b -> send(ManorActionPacket.Action.OPEN_STORAGE, 0, 0))
                    .bounds(x + 8, by, 120, 16).tooltip(Tooltip.create(Component.translatable("realmlord.storage.open.tip"))).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.storage.hand_in"), b -> send(ManorActionPacket.Action.HAND_IN, 0, 0))
                    .bounds(x + 132, by, 128, 16).tooltip(Tooltip.create(Component.translatable("realmlord.storage.hand_in.tip"))).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.storage.sell"), b -> minecraft.setScreen(new SellScreen()))
                    .bounds(x + 264, by, 128, 16).tooltip(Tooltip.create(Component.translatable("realmlord.storage.sell.tip"))).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.market.sell_special"), b -> minecraft.setScreen(new SpecialMarketScreen(false)))
                    .bounds(x + 8, y + 58, 180, 16).tooltip(Tooltip.create(Component.translatable("realmlord.market.sell_special.tip"))).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.market.armoury"), b -> minecraft.setScreen(new SpecialMarketScreen(true)))
                    .bounds(x + 194, y + 58, W - 204, 16).tooltip(Tooltip.create(Component.translatable("realmlord.market.armoury.tip"))).build());
            List<BuildingType> bps = blueprintRows(v);
            scroll = Math.max(0, Math.min(scroll, bps.size() - BP_ROWS));
            for (int i = 0; i < BP_ROWS && i + scroll < bps.size(); i++) {
                BuildingType bt = bps.get(i + scroll);
                boolean learned = java.util.Arrays.asList(v.getString("learned").split(",")).contains(bt.id());
                Button buy = Button.builder(Component.translatable(learned ? "realmlord.market.learned" : "realmlord.market.buy"),
                        b -> send(ManorActionPacket.Action.BUY_BLUEPRINT, bt.ordinal(), 0)).bounds(x + W - 62, y + 152 + i * 16, 52, 14).build();
                buy.active = !learned;
                if (!learned) buy.setTooltip(Tooltip.create(Component.translatable("realmlord.market.buy_bp.tip")));
                addRenderableWidget(buy);
            }
        }
    }

    private void renderMarket(GuiGraphics g, CompoundTag v, int x, int y) {
        Component coins = Component.translatable("realmlord.market.your_coins", Ui.money(v.getLong("coins")));
        if (platform == 0) {
            Ui.inset(g, x + 6, y + 54, W - 12, GOOD_ROWS * 15 + 18);
            g.drawString(font, Component.translatable("realmlord.market.materials_title"), x + 10, y + 57, Ui.GOLD, false);
            g.drawString(font, coins, x + W - 12 - font.width(coins), y + 57, Ui.INK, false);
            ListTag goods = v.getList("goods", Tag.TAG_COMPOUND);
            int n = Goods.values().length;
            for (int r = 0; r < GOOD_ROWS && r + scroll < n; r++) {
                Goods gd = Goods.values()[r + scroll];
                CompoundTag gt = r + scroll < goods.size() ? goods.getCompound(r + scroll) : new CompoundTag();
                int ry = y + 70 + r * 15;
                g.drawString(font, Component.translatable(gd.key()), x + 10, ry, gd.basic() ? Ui.INK : Ui.JADE, false);
                long stock = gt.getLong("stock");
                double daily = gt.getDouble("daily");
                String st = String.valueOf(stock);
                g.drawString(font, st, x + 128 - font.width(st), ry, Ui.INK, false);
                if (Math.abs(daily) >= 0.05) {
                    String d = (daily > 0 ? "+" : "") + (Math.abs(daily) < 1 ? String.format("%.1f", daily) : String.valueOf(Math.round(daily)));
                    g.drawString(font, d, x + 132, ry, daily > 0 ? 0xFF6FCF97 : Ui.RED, false);
                }
                g.drawString(font, Ui.money(gd.price), x + 162, ry, Ui.MUTED, false);
            }
            if (n > GOOD_ROWS) g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, n - GOOD_ROWS + 1), x + 10, y + 70 + GOOD_ROWS * 15, Ui.MUTED, false);
            g.drawString(font, Component.translatable("realmlord.market.goods_note", v.getInt("stored")), x + 10, y + H - 44, Ui.GREY, false);
        } else {
            Ui.inset(g, x + 6, y + 54, W - 12, 78);
            g.drawString(font, coins, x + W - 12 - font.width(coins), y + 62, Ui.INK, false);
            Ui.wrap(g, font, Component.translatable("realmlord.market.special_note"), x + 10, y + 80, W - 24, Ui.INK);
            Ui.inset(g, x + 6, y + 136, W - 12, BP_ROWS * 16 + 18);
            g.drawString(font, Component.translatable("realmlord.market.blueprints"), x + 10, y + 139, Ui.GOLD, false);
            List<BuildingType> bps = blueprintRows(v);
            for (int i = 0; i < BP_ROWS && i + scroll < bps.size(); i++) {
                BuildingType bt = bps.get(i + scroll);
                int ry = y + 155 + i * 16;
                g.drawString(font, Component.translatable("item.realmlord.blueprint.named", Component.translatable(bt.key())), x + 10, ry, Ui.INK, false);
                g.drawString(font, Ui.money(bt.blueprintPrice()), x + 220, ry, Ui.GOLD, false);
            }
            if (bps.size() > BP_ROWS) g.drawString(font, Component.translatable("realmlord.manor.scroll", scroll + 1, bps.size() - BP_ROWS + 1), x + 10, y + 236, Ui.MUTED, false);
        }
    }

    // ------------------------------------------------------------------ records

    private static final int LOG_ROWS = 12;

    /** Yesterday's ledger and the food outlook on the left; the village log, newest first, on the right. */
    private void renderRecords(GuiGraphics g, CompoundTag v, int x, int y) {
        int lx = x + 10, ly = y + 42;
        Ui.inset(g, lx - 4, ly - 4, 150, H - 54);
        long[] l = v.getLongArray("ledger");
        g.drawString(font, Component.translatable("realmlord.records.ledger"), lx, ly, Ui.GOLD, false);
        if (l.length < 10 || l[9] == 0 && l[1] == 0) {
            Ui.wrap(g, font, Component.translatable("realmlord.records.no_ledger"), lx, ly + 14, 140, Ui.MUTED);
        } else {
            int yy = ly + 14;
            yy = line(g, lx, yy, Component.translatable("realmlord.records.grain", signed(l[0])));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.eaten", l[1], l[8]));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.wood", signed(l[2])));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.stone", signed(l[3])));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.ore", signed(l[4])));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.tax", Ui.money(l[5])));
            yy = line(g, lx, yy, Component.translatable("realmlord.records.people", l[6], l[7]));
        }
        int yy = ly + 104;
        g.drawString(font, Component.translatable("realmlord.records.food"), lx, yy, Ui.GOLD, false);
        yy += 12;
        yy = line(g, lx, yy, Component.translatable("realmlord.records.stock", v.getLong("food"), v.getLong("otherFood")));
        yy = line(g, lx, yy, Component.translatable("realmlord.records.need", v.getLong("foodNeed")));
        long days = v.getLong("foodDays");
        Component outlook = days < 0 ? Component.translatable("realmlord.records.growing").withStyle(ChatFormatting.GREEN)
                : Component.translatable("realmlord.records.lasts", days).withStyle(days < 5 ? ChatFormatting.RED : days < 15 ? ChatFormatting.GOLD : ChatFormatting.WHITE);
        Ui.wrap(g, font, outlook, lx, yy, 140, Ui.INK);
        // The log.
        int rx = x + 166, rw = W - 176;
        Ui.inset(g, rx - 4, ly - 4, rw + 4, H - 54);
        g.drawString(font, Component.translatable("realmlord.records.log"), rx, ly, Ui.GOLD, false);
        ListTag log = v.getList("log", Tag.TAG_COMPOUND);
        if (log.isEmpty()) {
            g.drawString(font, Component.translatable("realmlord.records.empty"), rx, ly + 14, Ui.MUTED, false);
            return;
        }
        int first = Math.min(scroll, Math.max(0, log.size() - 1));
        int ry = ly + 14;
        for (int i = first; i < log.size() && ry < y + H - 22; i++) {
            CompoundTag e = log.getCompound(i);
            Component msg;
            try {
                msg = Component.Serializer.fromJson(e.getString("m"));
            } catch (Exception ex) {
                msg = null;
            }
            if (msg == null) continue;
            g.drawString(font, Component.translatable("realmlord.records.day", e.getLong("d")), rx, ry, Ui.MUTED, false);
            ry = Ui.wrap(g, font, msg, rx + 40, ry, rw - 44, Ui.INK) + 2;
        }
        if (log.size() > 1) Ui.centered(g, font, Component.translatable("realmlord.records.scroll", first + 1, log.size()), rx + rw / 2, y + H - 18, Ui.MUTED);
    }

    private static String signed(long n) {
        return n > 0 ? "+" + n : String.valueOf(n);
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        CompoundTag v = v();
        Ui.centered(g, font, title, x + W / 2, y + 7, Ui.GOLD);
        if (v != null) {
            switch (tab) {
                case OVERVIEW -> renderOverview(g, v, x, y);
                case BUILD -> renderBuild(g, v, x, y);
                case PEOPLE -> renderPeople(g, v, x, y);
                case RECORDS -> renderRecords(g, v, x, y);
                default -> renderMarket(g, v, x, y);
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == BUILD && my >= top() + 182) {
            siteScroll = Math.max(0, siteScroll - (int) Math.signum(delta));
            init();
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        init();
        return true;
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
