package com.realmlord.client;

import com.realmlord.network.ManorActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.PlaceBuildingPacket;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Village;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/**
 * A top-down map of the territory: terrain, border, the Lord's Manor, every building and
 * site, and you. With a building chosen, click the map to place it (right-click turns it).
 */
public class TerritoryMapScreen extends Screen {
    private static final int W = 420, H = 262, MAP = 236;

    private BuildingType placing;
    private int rotation;
    private int refreshIn = -1;

    public TerritoryMapScreen(BuildingType placing) {
        super(Component.translatable("realmlord.map.title"));
        this.placing = placing;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private int mapX() {
        return left() + 10;
    }

    private int mapY() {
        return top() + 18;
    }

    private double scale() {
        return ClientMap.size > 0 ? MAP / (double) ClientMap.size : 1;
    }

    /** World block under a screen point, or null outside the map. */
    private BlockPos worldAt(double mx, double my) {
        if (!ClientMap.ready || mx < mapX() || my < mapY() || mx >= mapX() + MAP || my >= mapY() + MAP) return null;
        double px = (mx - mapX()) / scale(), pz = (my - mapY()) / scale();
        return new BlockPos(ClientMap.x0 + (int) Math.floor(px * ClientMap.step), 64, ClientMap.z0 + (int) Math.floor(pz * ClientMap.step));
    }

    private int screenX(double wx) {
        return mapX() + (int) Math.round((wx - ClientMap.x0) / ClientMap.step * scale());
    }

    private int screenY(double wz) {
        return mapY() + (int) Math.round((wz - ClientMap.z0) / ClientMap.step * scale());
    }

    private int sx() {
        return rotation % 2 == 0 ? placing.width : placing.depth;
    }

    private int sz() {
        return rotation % 2 == 0 ? placing.depth : placing.width;
    }

    @Override
    protected void init() {
        clearWidgets();
        if (!ClientMap.ready || refreshIn < 0) ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.MAP, 0, 0));
        int bx = left() + MAP + 22, by = top() + H - 24, bw = W - MAP - 32;
        if (placing != null) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.rotate"), b -> rotation = (rotation + 1) & 3)
                    .bounds(bx, by - 54, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.in_world"), b -> {
                ClientPlacement.start(placing);
                onClose();
            }).bounds(bx, by - 36, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.cancel"), b -> {
                placing = null;
                init();
            }).bounds(bx, by - 18, bw, 16).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.refresh"), b -> ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.MAP, 0, 0)))
                    .bounds(bx, by - 18, bw, 16).build());
        }
        if (ClientVillageData.manor != null) {
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.manor"), b -> minecraft.setScreen(new ManorScreen()))
                    .bounds(bx, by, bw, 16).build());
        }
    }

    @Override
    public void tick() {
        if (refreshIn > 0 && --refreshIn == 0) {
            refreshIn = -1;
            ModNetwork.toServer(new ManorActionPacket(ManorActionPacket.Action.MAP, 0, 0));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        BlockPos at = worldAt(mx, my);
        if (placing != null && at != null) {
            if (button == 1) {
                rotation = (rotation + 1) & 3;
                return true;
            }
            if (button == 0) {
                BlockPos o = at.offset(-sx() / 2, 0, -sz() / 2);
                ModNetwork.toServer(new PlaceBuildingPacket(placing.ordinal(), o, rotation));
                placing = null;
                refreshIn = 20;
                init();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        CompoundTag v = ClientVillageData.manor;
        Ui.centered(g, font, v == null ? title : Component.literal(v.getString("name")), x + W / 2, y + 6, Ui.GOLD);
        int ax = mapX(), ay = mapY();
        g.fill(ax - 1, ay - 1, ax + MAP + 1, ay + MAP + 1, 0xFF8C6A2A);
        if (!ClientMap.ready) {
            g.fill(ax, ay, ax + MAP, ay + MAP, 0xFF1C1612);
            Ui.centered(g, font, Component.translatable("realmlord.map.loading"), ax + MAP / 2, ay + MAP / 2, Ui.MUTED);
        } else {
            g.blit(ClientMap.ID, ax, ay, MAP, MAP, 0, 0, ClientMap.size, ClientMap.size, ClientMap.size, ClientMap.size);
            g.enableScissor(ax, ay, ax + MAP, ay + MAP);
            if (v != null) overlay(g, v);
            player(g);
            Component hover = placing != null ? placement(g, mx, my) : hover(v, mx, my);
            g.disableScissor();
            if (hover != null) g.renderTooltip(font, hover, mx, my);
        }
        side(g, v, x + MAP + 22, y + 22, mx, my);
        super.render(g, mx, my, pt);
    }

    private void overlay(GuiGraphics g, CompoundTag v) {
        int cx = v.getInt("cx"), cz = v.getInt("cz"), r = v.getInt("radius");
        // Border
        int steps = 360;
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int px = screenX(cx + Math.cos(a) * r), py = screenY(cz + Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, 0xFFE8C15A);
        }
        // Lord's Manor yard
        int m = Village.MANOR_YARD;
        rect(g, cx - m, cz - m, 2 * m, 2 * m, 0x60C8423B, 0xFFC8423B);
        // Buildings
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
            if (bt == null || !bt.placed()) continue;
            boolean done = b.getBoolean("complete");
            rect(g, b.getInt("x"), b.getInt("z"), b.getInt("sx"), b.getInt("sz"), done ? 0x80E8C15A : 0x50E89B3A, done ? 0xFFE8C15A : 0xFFE89B3A);
        }
    }

    private void rect(GuiGraphics g, double wx, double wz, double sx, double sz, int fillCol, int edge) {
        int x0 = screenX(wx), y0 = screenY(wz), x1 = Math.max(x0 + 1, screenX(wx + sx)), y1 = Math.max(y0 + 1, screenY(wz + sz));
        g.fill(x0, y0, x1, y1, fillCol);
        g.fill(x0, y0, x1, y0 + 1, edge);
        g.fill(x0, y1 - 1, x1, y1, edge);
        g.fill(x0, y0, x0 + 1, y1, edge);
        g.fill(x1 - 1, y0, x1, y1, edge);
    }

    private void player(GuiGraphics g) {
        if (minecraft.player == null) return;
        int px = screenX(minecraft.player.getX()), py = screenY(minecraft.player.getZ());
        g.fill(px - 2, py - 2, px + 2, py + 2, 0xFF000000);
        g.fill(px - 1, py - 1, px + 1, py + 1, 0xFFFFFFFF);
    }

    private Component placement(GuiGraphics g, int mx, int my) {
        BlockPos at = worldAt(mx, my);
        if (at == null) return null;
        BlockPos o = at.offset(-sx() / 2, 0, -sz() / 2);
        boolean ok = ClientPlacement.fits(placing, rotation, o);
        rect(g, o.getX(), o.getZ(), sx(), sz(), ok ? 0x8040FF60 : 0x80FF4040, ok ? 0xFF40FF60 : 0xFFFF4040);
        // Door side
        int x0 = screenX(o.getX()), y0 = screenY(o.getZ()), x1 = screenX(o.getX() + sx()), y1 = screenY(o.getZ() + sz());
        switch (rotation & 3) {
            case 1 -> g.fill(x1 - 2, y0, x1, y1, 0xFFFFD84D);
            case 2 -> g.fill(x0, y1 - 2, x1, y1, 0xFFFFD84D);
            case 3 -> g.fill(x0, y0, x0 + 2, y1, 0xFFFFD84D);
            default -> g.fill(x0, y0, x1, y0 + 2, 0xFFFFD84D);
        }
        return null;
    }

    private Component hover(CompoundTag v, int mx, int my) {
        BlockPos at = worldAt(mx, my);
        if (at == null || v == null) return null;
        int cx = v.getInt("cx"), cz = v.getInt("cz"), m = Village.MANOR_YARD;
        if (Math.abs(at.getX() - cx) <= m && Math.abs(at.getZ() - cz) <= m) return Component.translatable("block.realmlord.lords_manor");
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            int bx = b.getInt("x"), bz = b.getInt("z");
            if (at.getX() >= bx && at.getX() < bx + b.getInt("sx") && at.getZ() >= bz && at.getZ() < bz + b.getInt("sz")) {
                BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
                Component name = Component.translatable(bt.key());
                if (b.getBoolean("complete")) {
                    return b.contains("staff") ? Component.translatable("realmlord.map.staffed", name, b.getString("staff")) : name;
                }
                return Component.translatable("realmlord.map.building", name, Math.round(b.getDouble("fraction") * 100));
            }
        }
        return null;
    }

    private void side(GuiGraphics g, CompoundTag v, int sx, int sy, int mx, int my) {
        int w = W - MAP - 32;
        if (placing != null) {
            g.drawString(font, Component.translatable("realmlord.map.placing"), sx, sy, Ui.GOLD, false);
            g.drawString(font, Component.translatable(placing.key()), sx, sy + 11, Ui.INK, false);
            g.drawString(font, placing.width + " x " + placing.depth, sx, sy + 22, Ui.MUTED, false);
            Ui.wrap(g, font, Component.translatable("realmlord.map.place_hint"), sx, sy + 36, w, Ui.MUTED);
        } else {
            g.drawString(font, Component.translatable("realmlord.map.legend"), sx, sy, Ui.GOLD, false);
            legend(g, sx, sy + 13, 0xFFE8C15A, "realmlord.map.legend.border");
            legend(g, sx, sy + 24, 0xFFC8423B, "realmlord.map.legend.manor");
            legend(g, sx, sy + 35, 0xFFE8C15A, "realmlord.map.legend.built");
            legend(g, sx, sy + 46, 0xFFE89B3A, "realmlord.map.legend.site");
            legend(g, sx, sy + 57, 0xFFFFFFFF, "realmlord.map.legend.you");
            Ui.wrap(g, font, Component.translatable("realmlord.map.view_hint"), sx, sy + 74, w, Ui.MUTED);
        }
        BlockPos at = worldAt(mx, my);
        if (at != null) g.drawString(font, Component.translatable("realmlord.map.coords", at.getX(), at.getZ()), sx, top() + H - 96, Ui.MUTED, false);
    }

    private void legend(GuiGraphics g, int x, int y, int color, String key) {
        g.fill(x, y + 1, x + 7, y + 8, color);
        g.drawString(font, Component.translatable(key), x + 11, y, Ui.INK, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
