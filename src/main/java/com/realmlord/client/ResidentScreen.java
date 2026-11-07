package com.realmlord.client;

import com.realmlord.network.ModNetwork;
import com.realmlord.network.ResidentActionPacket;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Job;
import com.realmlord.village.Resident;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A resident's character card, their work (for the Lord) and their trades. */
public class ResidentScreen extends Screen {
    public static final int CARD = 0, WORK = 1, TRADE = 2, DUTY = 3;
    private static final int W = 320, H = 250;
    private static final Job[] JOBS = {Job.IDLE, Job.CONSTRUCTION, Job.LOGGING, Job.QUARRYING, Job.FARMING, Job.MINING, Job.SALT_MAKING, Job.COPPER_MINING, Job.COAL_MINING, Job.FISHING, Job.ADMINISTRATION, Job.HUNTING};

    private int tab = CARD;
    private int scroll;

    /** Opened from the manor panel: closing it goes back there. */
    private final boolean fromManor = net.minecraft.client.Minecraft.getInstance().screen instanceof ManorScreen
            || net.minecraft.client.Minecraft.getInstance().screen instanceof BuildingScreen;

    @Override
    public void onClose() {
        if (fromManor && ClientVillageData.manor != null) minecraft.setScreen(new ManorScreen());
        else super.onClose();
    }

    public ResidentScreen() {
        super(Component.translatable("realmlord.resident.title"));
    }

    private CompoundTag v() {
        return ClientVillageData.resident;
    }

    private Resident res() {
        return Resident.load(v().getCompound("resident"));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private void send(ResidentActionPacket.Action a, int arg) {
        CompoundTag v = v();
        ModNetwork.toServer(new ResidentActionPacket(a, v.getInt("territory"), res().id, arg));
    }

    public void refresh() {
        init();
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        clearWidgets();
        if (v() == null) return;
        int x = left(), y = top();
        boolean owner = v().getBoolean("owner");
        String[] tabs = {"realmlord.resident.tab.card", "realmlord.resident.tab.work", "realmlord.resident.tab.trade", "realmlord.resident.tab.duty"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = idx;
                scroll = 0;
                init();
            }).bounds(x + 8 + i * 76, y + 20, 74, 16).build();
            b.active = tab != i && (i != WORK && i != DUTY || owner);
            addRenderableWidget(b);
        }
        if (tab == CARD && owner) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.resident.open_pack"), b -> send(ResidentActionPacket.Action.INVENTORY, 0))
                    .bounds(x + W - 92, y + H - 28, 80, 16).build());
            if (v().contains("figureArt") && !res().taught) {
                Button teach = Button.builder(Component.translatable("realmlord.figure.teach"), b -> send(ResidentActionPacket.Action.TEACH, 0))
                        .bounds(x + W - 196, y + H - 28, 100, 16)
                        .tooltip(Tooltip.create(Component.translatable("realmlord.figure.teach.tip", Component.translatable(v().getString("figureArt")),
                                com.realmlord.village.FigureArts.TEACHES_AT))).build();
                teach.active = v().getBoolean("canTeach");
                addRenderableWidget(teach);
            }
        }
        if (tab == WORK && owner) initWork(x, y);
        if (tab == TRADE) initTrade(x, y);
        if (tab == DUTY && owner) initDuty(x, y);
    }

    private void initDuty(int x, int y) {
        CompoundTag v = v();
        Resident r = res();
        Button follow = Button.builder(Component.translatable(r.follows ? "realmlord.companion.stay" : "realmlord.companion.follow"),
                b -> send(ResidentActionPacket.Action.FOLLOW, r.follows ? 0 : 1)).bounds(x + W - 130, y + 52, 120, 16).build();
        follow.active = r.follows || r.isTalent() && r.office < 0 && !r.militia && !v.getBoolean("wounded");
        addRenderableWidget(follow);
        Button militia = Button.builder(r.militia ? Component.translatable("realmlord.militia.leave")
                        : Component.translatable("realmlord.militia.join_cost", Ui.money(com.realmlord.village.Village.ENLIST_COST)),
                b -> send(ResidentActionPacket.Action.MILITIA, r.militia ? 0 : 1)).bounds(x + W - 130, y + 92, 120, 16)
                .tooltip(Tooltip.create(Component.translatable("realmlord.militia.join.tip"))).build();
        militia.active = r.militia && !r.sailor || !r.militia && r.office < 0 && v.getInt("militiaCount") < v.getInt("militiaCap");
        addRenderableWidget(militia);
        if (v.getInt("navyCap") > 0 || r.sailor) {
            Button sailor = Button.builder(r.sailor ? Component.translatable("realmlord.navy.leave")
                            : Component.translatable("realmlord.navy.join_cost", Ui.money(com.realmlord.village.Navy.ENLIST_COST)),
                    b -> send(ResidentActionPacket.Action.SAILOR, r.sailor ? 0 : 1)).bounds(x + W - 130, y + 72, 120, 16)
                    .tooltip(Tooltip.create(Component.translatable("realmlord.navy.join.tip", v.getInt("navyCount"), v.getInt("navyCap")))).build();
            sailor.active = r.sailor || !r.militia && r.office < 0 && v.getInt("navyCount") < v.getInt("navyCap");
            addRenderableWidget(sailor);
        }
        if (r.militia && r.equipTier < com.realmlord.village.Village.KIT_COST.length) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.militia.equip", Component.translatable("realmlord.militia.kit." + (r.equipTier + 1)),
                            Ui.money(com.realmlord.village.Village.KIT_COST[r.equipTier])), b -> send(ResidentActionPacket.Action.EQUIP, 0))
                    .bounds(x + W - 130, y + 112, 120, 16).tooltip(Tooltip.create(Component.translatable("realmlord.militia.equip.tip"))).build());
        }
        ListTag offices = v.getList("offices", Tag.TAG_COMPOUND);
        for (int i = 0; i < offices.size() && i < com.realmlord.village.Office.values().length; i++) {
            CompoundTag o = offices.getCompound(i);
            final int idx = i;
            boolean mine = r.office == i;
            Button b = Button.builder(Component.translatable(mine ? "realmlord.office.dismiss" : "realmlord.office.appoint"),
                    btn -> send(ResidentActionPacket.Action.OFFICE, mine ? -1 : idx)).bounds(x + W - 70, y + 134 + i * 15, 60, 13).build();
            b.active = mine || o.getBoolean("open");
            if (!o.getBoolean("open")) b.setTooltip(Tooltip.create(Component.translatable("realmlord.office.locked", com.realmlord.village.Office.values()[i].grade)));
            else b.setTooltip(Tooltip.create(Component.translatable(com.realmlord.village.Office.values()[i].key() + ".desc")));
            addRenderableWidget(b);
        }
    }

    private void initWork(int x, int y) {
        CompoundTag v = v();
        Resident r = res();
        ListTag errs = v.getList("jobErrors", Tag.TAG_STRING);
        int[] jobs = v.getIntArray("jobs"), places = v.getIntArray("places");
        for (int i = 0; i < JOBS.length; i++) {
            Job j = JOBS[i];
            int c = jobs.length > j.ordinal() ? jobs[j.ordinal()] : 0;
            int p = places.length > j.ordinal() ? places[j.ordinal()] : 0;
            Component label = Component.translatable(j.key()).append(p >= 9999 || j == Job.IDLE ? Component.empty() : Component.literal(" " + c + "/" + p));
            Button b = Button.builder(label, btn -> send(ResidentActionPacket.Action.SET_JOB, j.ordinal())).bounds(x + 10, y + 56 + i * 14, 140, 13).build();
            String err = errs.size() > j.ordinal() ? errs.getString(j.ordinal()) : "";
            b.active = r.job != j && err.isEmpty();
            if (!err.isEmpty()) b.setTooltip(Tooltip.create(Component.translatable(err)));
            addRenderableWidget(b);
        }
        ListTag staff = v.getList("staff", Tag.TAG_COMPOUND);
        for (int i = 0; i < staff.size() && i < 6; i++) {
            CompoundTag st = staff.getCompound(i);
            BuildingType bt = BuildingType.byOrdinal(st.getInt("type"));
            final int id = st.getInt("id");
            Button b = Button.builder(Component.translatable(bt.key()), btn -> send(ResidentActionPacket.Action.STAFF, id)).bounds(x + 166, y + 56 + i * 18, 144, 16).build();
            boolean mine = r.job == Job.STAFF && r.workplace == id;
            b.active = !mine && !st.contains("by");
            if (st.contains("by") && !mine) b.setTooltip(Tooltip.create(Component.translatable("realmlord.resident.staffed_by", st.getString("by"))));
            addRenderableWidget(b);
        }
    }

    private static final int TRADE_ROWS = 7;

    private void initTrade(int x, int y) {
        ListTag trades = v().getList("trades", Tag.TAG_COMPOUND);
        scroll = Math.max(0, Math.min(scroll, trades.size() - TRADE_ROWS));
        for (int i = 0; i < TRADE_ROWS && i + scroll < trades.size(); i++) {
            CompoundTag t = trades.getCompound(i + scroll);
            final int idx = i + scroll;
            boolean sell = t.getBoolean("sell");
            Button b = Button.builder(Component.translatable(sell ? "realmlord.trade.buy" : "realmlord.trade.sell"), btn -> send(ResidentActionPacket.Action.TRADE, idx))
                    .bounds(x + W - 66, y + 54 + i * 20, 56, 16).build();
            b.active = t.getBoolean("open");
            if (!b.active) b.setTooltip(Tooltip.create(Component.translatable("realmlord.trade.no_workshop")));
            addRenderableWidget(b);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        CompoundTag v = v();
        if (v == null) return;
        Resident r = res();
        Ui.centered(g, font, Component.literal(r.name), x + W / 2, y + 7, r.grade >= 0 ? TokenGrade.byId(r.grade).color : Ui.GOLD);
        switch (tab) {
            case CARD -> renderCard(g, v, r, x, y);
            case WORK -> renderWork(g, v, r, x, y);
            case DUTY -> renderDuty(g, v, r, x, y);
            default -> renderTrade(g, v, r, x, y);
        }
        super.render(g, mx, my, pt);
    }

    private void renderCard(GuiGraphics g, CompoundTag v, Resident r, int x, int y) {
        int lx = x + 12, ly = y + 44;
        Ui.inset(g, lx - 4, ly - 4, W - 16, H - 50);
        Component gradeName = r.grade >= 0 ? Component.translatable(TokenGrade.byId(r.grade).key()) : Component.translatable("realmlord.grade.common");
        int yy = ly;
        yy = line(g, lx, yy, Component.translatable("realmlord.resident.name", r.name, gradeName), r.grade >= 0 ? TokenGrade.byId(r.grade).color : Ui.INK);
        yy = line(g, lx, yy, Component.translatable("realmlord.resident.identity", v.getString("village")), Ui.INK);
        Component occ = r.occupation.special
                ? Component.translatable("realmlord.resident.occupation_ranked", Component.translatable("realmlord.work.tier." + r.rank), Component.translatable(r.occupation.key()))
                : Component.translatable(r.occupation.key());
        yy = line(g, lx, yy, Component.translatable("realmlord.resident.occupation", occ), Ui.INK);
        yy = line(g, lx, yy, Component.translatable("realmlord.resident.loyalty", r.loyalty), Ui.INK);
        if (r.isFigure()) yy = Ui.wrap(g, font, Component.translatable("realmlord.figure." + r.figure + ".desc"), lx, yy, W - 28, Ui.GOLD) + 1;
        if (r.office >= 0) yy = line(g, lx, yy, Component.translatable("realmlord.office.holds", Component.translatable(com.realmlord.village.Office.values()[r.office].key())), Ui.JADE);
        if (r.follows) yy = line(g, lx, yy, Component.translatable("realmlord.companion.following"), Ui.JADE);
        yy += 3;
        String[] stats = {"command", "force", "intelligence", "politics"};
        int[] vals = {r.command, r.force, r.intelligence, r.politics};
        for (int i = 0; i < 4; i++) {
            int sx = lx + (i % 2) * 140, sy = yy + (i / 2) * 11;
            g.drawString(font, Component.translatable("realmlord.stat." + stats[i]), sx, sy, Ui.INK, false);
            String s = String.valueOf(vals[i]);
            g.drawString(font, s, sx + 110 - font.width(s), sy, Ui.GOLD, false);
        }
        yy += 26;
        if (r.specialty != null) {
            yy = line(g, lx, yy, Component.translatable("realmlord.resident.specialty", Component.translatable(r.specialty.key()),
                    Math.round(r.specialtyBonus() * 100)), Ui.JADE);
            yy = Ui.wrap(g, font, Component.translatable(r.specialty.key() + ".desc"), lx, yy, W - 28, Ui.MUTED) + 2;
        }
        Component work = v.contains("workType") ? Component.translatable(BuildingType.byOrdinal(v.getInt("workType")).key()) : Component.translatable(r.job.key());
        yy = line(g, lx, yy, Component.translatable("realmlord.resident.job", work), Ui.INK);
        if (r.sailor) yy = line(g, lx, yy, Component.translatable("realmlord.resident.sailor"), Ui.RED);
        else if (r.militia) yy = line(g, lx, yy, Component.translatable("realmlord.resident.militia"), Ui.RED);
        if (!r.degree.isEmpty()) yy = line(g, lx, yy, Component.translatable("realmlord.resident.degree", Component.translatable("realmlord.degree." + r.degree)), Ui.GOLD);
        String act = v.getString("activity");
        if (!act.isEmpty()) yy = line(g, lx, yy, Component.translatable("realmlord.resident.doing", Component.translatable(act)), Ui.JADE);
        yy = Ui.wrap(g, font, Component.translatable("realmlord.resident.evaluation", Component.translatable(r.occupation.key() + ".eval")), lx, yy + 4, W - 28, Ui.GREY);
        // What they carry.
        int py = y + H - 30;
        g.drawString(font, Component.translatable("realmlord.resident.pack_label"), lx, py + 4, Ui.MUTED, false);
        int ix = lx + 40;
        boolean empty = true;
        for (int i = 0; i < r.inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack st = r.inv.getItem(i);
            if (st.isEmpty()) continue;
            empty = false;
            g.renderItem(st, ix, py);
            g.renderItemDecorations(font, st, ix, py);
            ix += 18;
        }
        if (empty) g.drawString(font, Component.translatable("realmlord.resident.pack_empty"), lx + 40, py + 4, Ui.GREY, false);
    }

    private void renderWork(GuiGraphics g, CompoundTag v, Resident r, int x, int y) {
        g.drawString(font, Component.translatable("realmlord.resident.jobs"), x + 10, y + 43, Ui.GOLD, false);
        g.drawString(font, Component.translatable("realmlord.resident.workplaces"), x + 166, y + 43, Ui.GOLD, false);
        if (v.getList("staff", Tag.TAG_COMPOUND).isEmpty()) {
            Ui.wrap(g, font, Component.translatable("realmlord.resident.no_workplace", Component.translatable(r.occupation.key())), x + 166, y + 58, W - 176, Ui.MUTED);
        }
        Ui.wrap(g, font, Component.translatable("realmlord.resident.skill_hint", String.format("%.0f", (r.skillAt(r.job) - 1) * 100)), x + 10, y + H - 26, W - 20, Ui.GREY);
    }

    private void renderDuty(GuiGraphics g, CompoundTag v, Resident r, int x, int y) {
        g.drawString(font, Component.translatable("realmlord.companion.title"), x + 10, y + 43, Ui.GOLD, false);
        Ui.wrap(g, font, Component.translatable(v.getBoolean("wounded") ? "realmlord.companion.wounded_now" : r.isTalent() ? "realmlord.companion.hint" : "realmlord.companion.err.talent",
                v.getInt("companions"), com.realmlord.village.Village.MAX_COMPANIONS), x + 10, y + 55, W - 150, Ui.MUTED);
        g.drawString(font, Component.translatable("realmlord.militia.title"), x + 10, y + 83, Ui.GOLD, false);
        Ui.wrap(g, font, v.getInt("militiaCap") == 0 ? Component.translatable("realmlord.militia.err.no_camp")
                : Component.translatable("realmlord.militia.count", v.getInt("militiaCount"), v.getInt("militiaCap")), x + 10, y + 95, W - 150, Ui.MUTED);
        g.drawString(font, Component.translatable("realmlord.office.title"), x + 10, y + 122, Ui.GOLD, false);
        ListTag offices = v.getList("offices", Tag.TAG_COMPOUND);
        for (int i = 0; i < offices.size() && i < com.realmlord.village.Office.values().length; i++) {
            CompoundTag o = offices.getCompound(i);
            com.realmlord.village.Office off = com.realmlord.village.Office.values()[i];
            int ry = y + 137 + i * 15;
            g.drawString(font, Component.translatable(off.key()), x + 10, ry, o.getBoolean("open") ? Ui.INK : Ui.GREY, false);
            if (o.contains("by")) g.drawString(font, o.getString("by"), x + 120, ry, r.office == i ? Ui.GOLD : Ui.JADE, false);
            else if (o.getBoolean("open")) g.drawString(font, Component.translatable("realmlord.office.vacant"), x + 120, ry, Ui.GREY, false);
        }
    }

    private void renderTrade(GuiGraphics g, CompoundTag v, Resident r, int x, int y) {
        Component coins = Component.translatable("realmlord.market.your_coins", Ui.money(v.getLong("coins")));
        g.drawString(font, coins, x + 10, y + 42, Ui.INK, false);
        ListTag trades = v.getList("trades", Tag.TAG_COMPOUND);
        if (trades.isEmpty()) g.drawString(font, Component.translatable("realmlord.trade.none"), x + 10, y + 60, Ui.MUTED, false);
        for (int i = 0; i < TRADE_ROWS && i + scroll < trades.size(); i++) {
            CompoundTag t = trades.getCompound(i + scroll);
            ItemStack stack = ItemStack.of(t.getCompound("item"));
            int ry = y + 54 + i * 20;
            g.renderItem(stack, x + 10, ry);
            g.renderItemDecorations(font, stack, x + 10, ry);
            boolean sell = t.getBoolean("sell");
            g.drawString(font, stack.getHoverName(), x + 32, ry + 4, t.getBoolean("open") ? Ui.INK : Ui.GREY, false);
            Component price = Component.translatable(sell ? "realmlord.trade.costs" : "realmlord.trade.pays", Ui.money(t.getLong("price")));
            g.drawString(font, price, x + W - 72 - font.width(price), ry + 4, sell ? Ui.GOLD : Ui.JADE, false);
        }
    }

    private int line(GuiGraphics g, int x, int y, Component c, int color) {
        g.drawString(font, c, x, y, color, false);
        return y + 11;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
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
