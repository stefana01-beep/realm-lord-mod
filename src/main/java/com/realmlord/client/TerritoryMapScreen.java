package com.realmlord.client;

import com.realmlord.network.ManorActionPacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.PlaceBuildingPacket;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Paths;
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
    /** Laying out the Simple Fence or moving its gates: bearings in degrees. */
    private boolean fenceMode;
    private final java.util.List<Integer> gates = new java.util.ArrayList<>();
    /** A building picked on the map, and whether its demolition awaits a second click. */
    private int selectedId = -1;
    private boolean confirmDemolish;
    /** Laying out a path: its points (x, z), surface and width. */
    private boolean pathMode;
    private final java.util.List<int[]> pathPts = new java.util.ArrayList<>();
    private int pathSurface = 1, pathWidth = 2;
    /** Marking out ground to clear and level: two corners, and the level relative to the usual ground. */
    private boolean clearMode;
    private int[] corner1, corner2;
    private int clearOffset;
    private BlockPos hoverAt;

    public TerritoryMapScreen(BuildingType placing) {
        super(Component.translatable("realmlord.map.title"));
        if (placing == BuildingType.SIMPLE_FENCE) {
            fenceMode = true;
            loadGates();
        } else {
            this.placing = placing;
        }
    }

    /** The standing fence, or null. */
    private static CompoundTag fence() {
        CompoundTag v = ClientVillageData.manor;
        if (v == null) return null;
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            if (b.getInt("type") == BuildingType.SIMPLE_FENCE.ordinal() && !b.getBoolean("demolish")) return b;
        }
        return null;
    }

    private void loadGates() {
        gates.clear();
        CompoundTag f = fence();
        int[] g = f != null ? f.getIntArray("gates") : new int[]{0, 90, 180, 270};
        for (int d : g) gates.add(d);
    }

    private int fenceRadius() {
        CompoundTag f = fence();
        CompoundTag v = ClientVillageData.manor;
        if (f != null && f.getInt("fr") > 0) return f.getInt("fr");
        return v == null ? 0 : v.getInt("radius") - 2;
    }

    private CompoundTag selected() {
        CompoundTag v = ClientVillageData.manor;
        if (v == null || selectedId < 0) return null;
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) if (bs.getCompound(i).getInt("id") == selectedId) return bs.getCompound(i);
        return null;
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
        if (fenceMode) {
            boolean exists = fence() != null;
            addRenderableWidget(Button.builder(Component.translatable(exists ? "realmlord.map.fence.set" : "realmlord.map.fence.lay"), b -> {
                ModNetwork.toServer(new com.realmlord.network.FenceGatesPacket(gates.stream().mapToInt(Integer::intValue).toArray()));
                fenceMode = false;
                refreshIn = 20;
                init();
            }).bounds(bx, by - 54, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.fence.four"), b -> {
                gates.clear();
                gates.addAll(java.util.List.of(0, 90, 180, 270));
            }).bounds(bx, by - 36, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.cancel"), b -> {
                fenceMode = false;
                init();
            }).bounds(bx, by - 18, bw, 16).build());
        } else if (clearMode) {
            int half = (bw - 4) / 2;
            addRenderableWidget(Button.builder(Component.translatable("realmlord.clear.lower"), b -> {
                clearOffset = Math.max(-8, clearOffset - 1);
            }).bounds(bx, by - 72, half, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.clear.raise"), b -> {
                clearOffset = Math.min(8, clearOffset + 1);
            }).bounds(bx + half + 4, by - 72, half, 16).build());
            Button go = Button.builder(Component.translatable("realmlord.clear.go"), b -> {
                ModNetwork.toServer(new com.realmlord.network.ClearAreaPacket(corner1[0], corner1[1], corner2[0], corner2[1], clearOffset));
                clearMode = false;
                corner1 = corner2 = null;
                refreshIn = 20;
                init();
            }).bounds(bx, by - 54, bw, 16).build();
            go.active = corner1 != null && corner2 != null && clearSize()[0] <= Village.MAX_CLEARING && clearSize()[1] <= Village.MAX_CLEARING;
            addRenderableWidget(go);
            addRenderableWidget(Button.builder(Component.translatable("realmlord.clear.again"), b -> {
                corner1 = corner2 = null;
                init();
            }).bounds(bx, by - 36, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.cancel"), b -> {
                clearMode = false;
                corner1 = corner2 = null;
                init();
            }).bounds(bx, by - 18, bw, 16).build());
        } else if (pathMode) {
            Paths.Surface s = Paths.Surface.byOrdinal(pathSurface);
            addRenderableWidget(Button.builder(Component.translatable("realmlord.path.surface", Component.translatable(s.key())), b -> {
                pathSurface = (pathSurface + 1) % Paths.Surface.values().length;
                init();
            }).bounds(bx, by - 90, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.path.width", pathWidth), b -> {
                pathWidth = pathWidth % Paths.MAX_WIDTH + 1;
                init();
            }).bounds(bx, by - 72, bw, 16).build());
            Button lay = Button.builder(Component.translatable("realmlord.path.lay"), b -> {
                int[] pts = new int[pathPts.size() * 2];
                for (int i = 0; i < pathPts.size(); i++) {
                    pts[i * 2] = pathPts.get(i)[0];
                    pts[i * 2 + 1] = pathPts.get(i)[1];
                }
                ModNetwork.toServer(new com.realmlord.network.PlacePathPacket(pathSurface, pathWidth, pts));
                pathMode = false;
                pathPts.clear();
                refreshIn = 20;
                init();
            }).bounds(bx, by - 54, bw, 16).build();
            lay.active = pathPts.size() >= 2;
            addRenderableWidget(lay);
            addRenderableWidget(Button.builder(Component.translatable("realmlord.path.undo"), b -> {
                if (!pathPts.isEmpty()) pathPts.remove(pathPts.size() - 1);
                init();
            }).bounds(bx, by - 36, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.map.cancel"), b -> {
                pathMode = false;
                pathPts.clear();
                init();
            }).bounds(bx, by - 18, bw, 16).build());
        } else if (placing != null) {
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
            CompoundTag f = fence();
            if (f != null) {
                addRenderableWidget(Button.builder(Component.translatable("realmlord.map.fence.edit"), b -> {
                    fenceMode = true;
                    selectedId = -1;
                    loadGates();
                    init();
                }).bounds(bx, by - 36, bw, 16).build());
            }
            addRenderableWidget(Button.builder(Component.translatable("realmlord.path.new"), b -> {
                pathMode = true;
                selectedId = -1;
                pathPts.clear();
                init();
            }).bounds(bx, by - 72, bw, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("realmlord.clear.new"), b -> {
                clearMode = true;
                selectedId = -1;
                corner1 = corner2 = null;
                clearOffset = 0;
                init();
            }).bounds(bx, by - 90, bw, 16).build());
            CompoundTag sel = selected();
            boolean isPath = sel != null && (sel.getInt("type") == BuildingType.PATH.ordinal() || sel.getInt("type") == BuildingType.CLEARING.ordinal());
            if (sel != null && (sel.getBoolean("complete") || isPath) && !sel.getBoolean("demolish")) {
                Button d = Button.builder(Component.translatable(confirmDemolish ? "realmlord.map.demolish.confirm" : "realmlord.map.demolish"), b -> {
                    if (!confirmDemolish) {
                        confirmDemolish = true;
                    } else {
                        ModNetwork.toServer(new ManorActionPacket(sel.getBoolean("complete") ? ManorActionPacket.Action.DEMOLISH : ManorActionPacket.Action.CANCEL_SITE, selectedId, 0));
                        selectedId = -1;
                        confirmDemolish = false;
                        refreshIn = 20;
                    }
                    init();
                }).bounds(bx, by - 54, bw, 16).build();
                d.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("realmlord.map.demolish.tip")));
                addRenderableWidget(d);
            }
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
        if (fenceMode && at != null && button == 0) {
            CompoundTag v = ClientVillageData.manor;
            if (v != null) {
                int cx = v.getInt("cx"), cz = v.getInt("cz");
                double dist = Math.sqrt(Math.pow(at.getX() - cx, 2) + Math.pow(at.getZ() - cz, 2));
                double slack = Math.max(4, ClientMap.step * 4 / scale());
                if (Math.abs(dist - fenceRadius()) <= slack) {
                    int deg = (int) Math.round(com.realmlord.village.BuildingDesigns.bearing(new BlockPos(cx, 0, cz), at.getX(), at.getZ())) % 360;
                    Integer near = null;
                    for (int g : gates) if (angleDiff(g, deg) <= Math.max(6, Math.toDegrees(5.0 / Math.max(4, fenceRadius())))) near = g;
                    if (near != null) gates.remove(near);
                    else if (gates.size() < 16) gates.add(deg);
                    return true;
                }
            }
        }
        if (clearMode && at != null) {
            if (button == 0) {
                if (corner1 == null || corner2 != null) {
                    corner1 = new int[]{at.getX(), at.getZ()};
                    corner2 = null;
                } else {
                    corner2 = new int[]{at.getX(), at.getZ()};
                }
            } else if (button == 1) {
                corner1 = corner2 = null;
            }
            init();
            return true;
        }
        if (pathMode && at != null) {
            if (button == 0 && pathPts.size() < Paths.MAX_POINTS) pathPts.add(new int[]{at.getX(), at.getZ()});
            else if (button == 1 && !pathPts.isEmpty()) pathPts.remove(pathPts.size() - 1);
            init();
            return true;
        }
        if (!fenceMode && !pathMode && !clearMode && placing == null && at != null && button == 0) {
            CompoundTag v = ClientVillageData.manor;
            int was = selectedId;
            selectedId = -1;
            if (v != null) {
                ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
                for (int i = 0; i < bs.size(); i++) {
                    CompoundTag b = bs.getCompound(i);
                    BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
                    if (bt == null || !(bt.placed() || bt == BuildingType.CLEARING)) continue;
                    int bx = b.getInt("x"), bz = b.getInt("z");
                    if (at.getX() >= bx && at.getX() < bx + b.getInt("sx") && at.getZ() >= bz && at.getZ() < bz + b.getInt("sz")) selectedId = b.getInt("id");
                }
                if (selectedId < 0) {
                    CompoundTag p = pathNear(v, at.getX(), at.getZ());
                    if (p != null) selectedId = p.getInt("id");
                }
            }
            if (selectedId != was) {
                confirmDemolish = false;
                init();
            }
        }
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
        hoverAt = worldAt(mx, my);
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
        if (hoverAt != null) {
            Component co = Component.translatable("realmlord.map.coords", hoverAt.getX(), hoverAt.getZ());
            g.drawString(font, co, x + W - 10 - font.width(co), y + 6, Ui.MUTED, false);
        }
        super.render(g, mx, my, pt);
    }

    private static double angleDiff(double a, double b) {
        return Math.abs(((a - b) % 360 + 540) % 360 - 180);
    }

    /** The fence ring, posts and gates (gold) as they are or as they are being laid out. */
    private void fenceRing(GuiGraphics g, CompoundTag v) {
        CompoundTag f = fence();
        if (f == null && !fenceMode) return;
        int cx = v.getInt("cx"), cz = v.getInt("cz"), r = fenceRadius();
        java.util.List<Integer> shown = fenceMode ? gates : java.util.Arrays.stream(f.getIntArray("gates")).boxed().toList();
        double half = Math.toDegrees(1.6 / Math.max(4, r));
        int steps = 720;
        for (int i = 0; i < steps; i++) {
            double deg = 360.0 * i / steps;
            boolean gate = false;
            for (int gd : shown) if (angleDiff(deg, gd) <= half + 0.5) gate = true;
            double a = Math.toRadians(deg);
            int px = screenX(cx + Math.cos(a) * r), py = screenY(cz + Math.sin(a) * r);
            int col = gate ? 0xFFFFD84D : (f != null && f.getBoolean("complete") ? 0xFF9C6B30 : 0xFFC9A26A);
            int sz = gate ? 2 : 1;
            g.fill(px - sz / 2, py - sz / 2, px - sz / 2 + sz, py - sz / 2 + sz, col);
        }
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
        // Paths
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            if (b.getInt("type") != BuildingType.PATH.ordinal() || !b.contains("pts")) continue;
            int col = surfaceColor(b.getInt("mat"));
            if (!b.getBoolean("complete")) col = (col & 0x00FFFFFF) | 0x90000000;
            if (b.getInt("id") == selectedId) col = 0xFFFFFFFF;
            polyline(g, b.getIntArray("pts"), b.getInt("w"), col);
        }
        if (pathMode && !pathPts.isEmpty()) {
            int[] pts = new int[pathPts.size() * 2];
            for (int i = 0; i < pathPts.size(); i++) {
                pts[i * 2] = pathPts.get(i)[0];
                pts[i * 2 + 1] = pathPts.get(i)[1];
            }
            polyline(g, pts, pathWidth, 0xC0FFE07A);
            for (int[] p : pathPts) {
                int px = screenX(p[0] + 0.5), py = screenY(p[1] + 0.5);
                g.fill(px - 2, py - 2, px + 2, py + 2, 0xFFFFD84D);
            }
        }
        // Clearings, and the one being marked out
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            if (b.getInt("type") != BuildingType.CLEARING.ordinal()) continue;
            rect(g, b.getInt("x"), b.getInt("z"), b.getInt("sx"), b.getInt("sz"), 0x4040C0E0, b.getInt("id") == selectedId ? 0xFFFFFFFF : 0xFF40C0E0);
        }
        if (clearMode && corner1 != null) {
            int[] c2 = corner2;
            if (c2 == null && hoverAt != null) c2 = new int[]{hoverAt.getX(), hoverAt.getZ()};
            if (c2 == null) c2 = corner1;
            int ax = Math.min(corner1[0], c2[0]), az = Math.min(corner1[1], c2[1]);
            int sx = Math.abs(c2[0] - corner1[0]) + 1, sz = Math.abs(c2[1] - corner1[1]) + 1;
            boolean ok = sx <= Village.MAX_CLEARING && sz <= Village.MAX_CLEARING;
            rect(g, ax, az, sx, sz, ok ? 0x5040C0E0 : 0x60FF4040, ok ? 0xFF7FE6FF : 0xFFFF4040);
        }
        // Buildings
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
            if (bt == null || !bt.placed()) continue;
            boolean done = b.getBoolean("complete"), down = b.getBoolean("demolish");
            int fillCol = down ? 0x60C8423B : done ? 0x80E8C15A : 0x50E89B3A, edge = down ? 0xFFC8423B : done ? 0xFFE8C15A : 0xFFE89B3A;
            if (b.getInt("id") == selectedId) edge = 0xFFFFFFFF;
            rect(g, b.getInt("x"), b.getInt("z"), b.getInt("sx"), b.getInt("sz"), fillCol, edge);
        }
        fenceRing(g, v);
    }

    private static int surfaceColor(int mat) {
        return switch (Paths.Surface.byOrdinal(mat)) {
            case DIRT -> 0xFFA07A4A;
            case GRAVEL -> 0xFF8E8A86;
            case PLANKS -> 0xFF7A5A34;
            case COBBLE -> 0xFF6E6E6E;
            case STONE -> 0xFFB0B0B0;
        };
    }

    /** A path drawn as a thick line through its points. */
    private void polyline(GuiGraphics g, int[] pts, int w, int col) {
        int r = Math.max(1, (int) Math.round(w * scale() / Math.max(1, ClientMap.step) / 2.0));
        for (int s = 0; s + 3 < pts.length; s += 2) {
            double ax = screenX(pts[s] + 0.5), ay = screenY(pts[s + 1] + 0.5), bx = screenX(pts[s + 2] + 0.5), by = screenY(pts[s + 3] + 0.5);
            int n = (int) Math.max(1, Math.ceil(Math.hypot(bx - ax, by - ay)));
            for (int i = 0; i <= n; i++) {
                int px = (int) Math.round(ax + (bx - ax) * i / n), py = (int) Math.round(ay + (by - ay) * i / n);
                g.fill(px - r / 2, py - r / 2, px - r / 2 + r, py - r / 2 + r, col);
            }
        }
    }

    /** The path within a few blocks of a point, or null. */
    private static CompoundTag pathNear(CompoundTag v, int x, int z) {
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        double reach = Math.max(3, ClientMap.step * 2);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            if (b.getInt("type") != BuildingType.PATH.ordinal() || !b.contains("pts")) continue;
            int[] p = b.getIntArray("pts");
            for (int s = 0; s + 3 < p.length; s += 2) {
                if (segDist(x, z, p[s], p[s + 1], p[s + 2], p[s + 3]) <= reach + b.getInt("w") / 2.0) return b;
            }
        }
        return null;
    }

    private static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az, l = dx * dx + dz * dz;
        double t = l == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }

    private int[] clearSize() {
        return new int[]{Math.abs(corner2[0] - corner1[0]) + 1, Math.abs(corner2[1] - corner1[1]) + 1};
    }

    /** Rough size of the path being drawn, in blocks. */
    private int pathEstimate() {
        double len = 0;
        for (int i = 1; i < pathPts.size(); i++)
            len += Math.hypot(pathPts.get(i)[0] - pathPts.get(i - 1)[0], pathPts.get(i)[1] - pathPts.get(i - 1)[1]);
        return pathPts.size() < 2 ? 0 : (int) Math.ceil((len + 1) * pathWidth);
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
            BuildingType ht = BuildingType.byOrdinal(b.getInt("type"));
            if (ht == null || !(ht.placed() || ht == BuildingType.CLEARING)) continue;
            int bx = b.getInt("x"), bz = b.getInt("z");
            if (at.getX() >= bx && at.getX() < bx + b.getInt("sx") && at.getZ() >= bz && at.getZ() < bz + b.getInt("sz")) {
                BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
                Component name = Component.translatable(bt.key());
                if (b.getBoolean("demolish")) return Component.translatable("realmlord.map.demolishing", name, Math.round((1 - b.getDouble("fraction")) * 100));
                if (b.getBoolean("complete")) {
                    return b.contains("staff") ? Component.translatable("realmlord.map.staffed", name, b.getString("staff")) : name;
                }
                return Component.translatable("realmlord.map.building", name, Math.round(b.getDouble("fraction") * 100));
            }
        }
        if (!pathMode) {
            CompoundTag p = pathNear(v, at.getX(), at.getZ());
            if (p != null) {
                Component name = Component.translatable("realmlord.path.named", Component.translatable(Paths.Surface.byOrdinal(p.getInt("mat")).key()), p.getInt("w"));
                return p.getBoolean("complete") ? name : Component.translatable("realmlord.map.building", name, Math.round(p.getDouble("fraction") * 100));
            }
        }
        return null;
    }

    private void side(GuiGraphics g, CompoundTag v, int sx, int sy, int mx, int my) {
        int w = W - MAP - 32;
        if (fenceMode) {
            g.drawString(font, Component.translatable("realmlord.building.simple_fence"), sx, sy, Ui.GOLD, false);
            g.drawString(font, Component.translatable("realmlord.map.fence.gates", gates.size()), sx, sy + 11, Ui.INK, false);
            Ui.wrap(g, font, Component.translatable("realmlord.map.fence.hint"), sx, sy + 25, w, Ui.MUTED);
        } else if (clearMode) {
            g.drawString(font, Component.translatable("realmlord.clear.title"), sx, sy, Ui.GOLD, false);
            if (corner1 != null && corner2 != null) {
                int[] s2 = clearSize();
                g.drawString(font, Component.translatable("realmlord.clear.size", s2[0], s2[1], Village.MAX_CLEARING),
                        sx, sy + 11, s2[0] > Village.MAX_CLEARING || s2[1] > Village.MAX_CLEARING ? Ui.RED : Ui.INK, false);
            } else {
                g.drawString(font, Component.translatable(corner1 == null ? "realmlord.clear.corner1" : "realmlord.clear.corner2"), sx, sy + 11, Ui.INK, false);
            }
            g.drawString(font, clearOffset == 0 ? Component.translatable("realmlord.clear.level0")
                    : Component.translatable(clearOffset > 0 ? "realmlord.clear.level_up" : "realmlord.clear.level_down", Math.abs(clearOffset)), sx, sy + 22, Ui.INK, false);
            Ui.wrap(g, font, Component.translatable("realmlord.clear.hint"), sx, sy + 36, w, Ui.MUTED);
            return;
        } else if (pathMode) {
            Paths.Surface s = Paths.Surface.byOrdinal(pathSurface);
            int n = pathEstimate();
            long[] c = Paths.cost(s, n);
            g.drawString(font, Component.translatable("realmlord.path.title"), sx, sy, Ui.GOLD, false);
            g.drawString(font, Component.translatable("realmlord.path.points", pathPts.size(), Paths.MAX_POINTS), sx, sy + 11, Ui.INK, false);
            g.drawString(font, Component.translatable("realmlord.path.estimate", n), sx, sy + 22, Ui.INK, false);
            g.drawString(font, c[0] == 0 && c[1] == 0 ? Component.translatable("realmlord.path.free")
                    : Component.translatable("realmlord.path.cost", c[0], c[1]), sx, sy + 33, Ui.INK, false);
            Ui.wrap(g, font, Component.translatable("realmlord.path.hint"), sx, sy + 47, w, Ui.MUTED);
            return;
        } else if (placing != null) {
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
            CompoundTag sel = selected();
            if (sel != null) {
                BuildingType bt = BuildingType.byOrdinal(sel.getInt("type"));
                boolean path = bt == BuildingType.PATH;
                if (bt == BuildingType.CLEARING) {
                    g.drawString(font, Component.translatable(bt.key()), sx, sy + 74, Ui.GOLD, false);
                    g.drawString(font, Component.translatable("realmlord.map.state.site", Math.round(sel.getDouble("fraction") * 100)), sx, sy + 85, Ui.INK, false);
                    Ui.wrap(g, font, Component.translatable("realmlord.clear.cancel_hint", sel.getInt("sx"), sel.getInt("sz")), sx, sy + 97, w, Ui.MUTED);
                    return;
                }
                g.drawString(font, path ? Component.translatable("realmlord.path.named", Component.translatable(Paths.Surface.byOrdinal(sel.getInt("mat")).key()), sel.getInt("w"))
                        : Component.translatable(bt.key()), sx, sy + 74, Ui.GOLD, false);
                Component state = sel.getBoolean("demolish") ? Component.translatable("realmlord.map.state.demolishing")
                        : sel.getBoolean("complete") ? Component.translatable("realmlord.map.state.built")
                        : Component.translatable("realmlord.map.state.site", Math.round(sel.getDouble("fraction") * 100));
                g.drawString(font, state, sx, sy + 85, Ui.INK, false);
                if (path) Ui.wrap(g, font, Component.translatable("realmlord.path.remove_hint", sel.getInt("blocks")), sx, sy + 97, w, Ui.MUTED);
                else if (sel.getBoolean("complete")) Ui.wrap(g, font, Component.translatable("realmlord.map.salvage", bt.wood * 3 / 4, bt.stone * 3 / 4), sx, sy + 97, w, Ui.MUTED);
            } else {
                Ui.wrap(g, font, Component.translatable("realmlord.map.view_hint"), sx, sy + 74, w, Ui.MUTED);
            }
        }
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
