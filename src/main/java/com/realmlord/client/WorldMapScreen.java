package com.realmlord.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realmlord.RealmLord;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.WorldInfoPacket;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The world map (M). On the China world it opens on the whole of China drawn as an atlas, with
 * the novel's provinces, the Imperial Capitals and every territory; scroll in and the land you
 * have seen appears block by block, like a vanilla map. On other worlds it shows the land you
 * have seen. Drag to move about; Space (or the button) brings you back to yourself.
 */
public class WorldMapScreen extends Screen {
    private static final ResourceLocation ATLAS = RealmLord.id("textures/gui/china_map.png");

    private record Label(String name, int x, int z, String kind) {}

    private static List<Label> labels;

    /** The view: the block at the centre and how many blocks one screen pixel covers. */
    private static double viewX = Double.NaN, viewZ, scale = 8;
    private boolean dragging;
    private double pressX, pressY;

    public WorldMapScreen() {
        super(Component.translatable("realmlord.worldmap.title"));
    }

    private int mapX() {
        return 4;
    }

    private int mapY() {
        return 18;
    }

    private int mapW() {
        return width - 8;
    }

    private int mapH() {
        return height - 40;
    }

    /** Fully zoomed out: the whole China map (or about 8,000 blocks elsewhere) fits. */
    private double maxScale() {
        if (WorldMapData.china && ChinaMap.ready()) {
            return Math.max(ChinaMap.width() * (double) ChinaMap.pixelSize() / mapW(), ChinaMap.height() * (double) ChinaMap.pixelSize() / mapH()) * 1.05;
        }
        return 8192.0 / Math.min(mapW(), mapH());
    }

    @Override
    protected void init() {
        ModNetwork.toServer(new WorldInfoPacket.Request());
        if (WorldMapData.china) loadLabels();
        if (Double.isNaN(viewX) && minecraft.player != null) centre();
        scale = Mth.clamp(scale, 0.25, maxScale());
        int y = height - 20;
        addRenderableWidget(Button.builder(Component.translatable("realmlord.worldmap.me"), b -> centre()).bounds(4, y, 90, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.worldmap.whole"), b -> {
            scale = maxScale();
            if (WorldMapData.china && ChinaMap.ready()) {
                viewX = ChinaMap.originX() + ChinaMap.width() * ChinaMap.pixelSize() / 2.0;
                viewZ = ChinaMap.originZ() + ChinaMap.height() * ChinaMap.pixelSize() / 2.0;
            }
        }).bounds(98, y, 90, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width - 64, y, 60, 16).build());
    }

    private void centre() {
        if (minecraft.player == null) return;
        viewX = minecraft.player.getX();
        viewZ = minecraft.player.getZ();
        scale = Math.min(scale, 4);
    }

    private static void loadLabels() {
        if (labels != null) return;
        labels = new ArrayList<>();
        try (var in = WorldMapScreen.class.getResourceAsStream("/realmlord/china/labels.json")) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.getAsJsonArray("labels")) {
                JsonObject l = e.getAsJsonObject();
                labels.add(new Label(l.get("name").getAsString(), l.get("x").getAsInt(), l.get("z").getAsInt(), l.get("kind").getAsString()));
            }
        } catch (Exception e) {
            RealmLord.LOGGER.warn("Could not read the China map labels", e);
        }
    }

    // ------------------------------------------------------------------ coordinates

    private double sx(double bx) {
        return mapX() + mapW() / 2.0 + (bx - viewX) / scale;
    }

    private double sy(double bz) {
        return mapY() + mapH() / 2.0 + (bz - viewZ) / scale;
    }

    private double bx(double sx) {
        return viewX + (sx - mapX() - mapW() / 2.0) * scale;
    }

    private double bz(double sy) {
        return viewZ + (sy - mapY() - mapH() / 2.0) * scale;
    }

    private boolean overMap(double mx, double my) {
        return Ui.inside(mx, my, mapX(), mapY(), mapW(), mapH());
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!overMap(mx, my)) return false;
        double bx = bx(mx), bz = bz(my);
        scale = Mth.clamp(scale * (delta > 0 ? 0.8 : 1.25), 0.25, maxScale());
        viewX = bx - (mx - mapX() - mapW() / 2.0) * scale;
        viewZ = bz - (my - mapY() - mapH() / 2.0) * scale;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (overMap(mx, my)) {
            dragging = true;
            pressX = mx;
            pressY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            viewX -= dx * scale;
            viewZ -= dy * scale;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KeyBindings.WORLD_MAP.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
            centre();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int ax = mapX(), ay = mapY(), aw = mapW(), ah = mapH();
        g.fill(ax - 1, ay - 1, ax + aw + 1, ay + ah + 1, 0xFF6B4A22);
        g.fill(ax, ay, ax + aw, ay + ah, WorldMapData.china ? 0xFF7FA9C9 : 0xFF1E1A14);
        g.enableScissor(ax, ay, ax + aw, ay + ah);
        boolean overworld = minecraft.level != null && minecraft.level.dimension() == Level.OVERWORLD;
        if (WorldMapData.china && overworld && ChinaMap.ready()) drawAtlas(g);
        drawExplored(g);
        if (WorldMapData.china && overworld) drawLabels(g);
        drawTerritories(g);
        drawPlayer(g);
        g.disableScissor();

        Ui.centered(g, font, title, width / 2, 5, Ui.GOLD);
        if (overMap(mx, my)) {
            int bx = (int) Math.floor(bx(mx)), bz = (int) Math.floor(bz(my));
            Component pos = Component.translatable("realmlord.worldmap.at", bx, bz);
            if (WorldMapData.china && ChinaMap.ready() && ChinaMap.onMap(bx, bz)) {
                double[] ll = ChinaMap.lonLatOf(bx, bz);
                pos = Component.translatable("realmlord.worldmap.at_real", bx, bz, String.format("%.2f", ll[1]), String.format("%.2f", ll[0]));
            }
            g.drawString(font, pos, 196, height - 16, Ui.INK, false);
        }
        String sc = scale >= 1 ? String.format("1 px = %.0f blocks", scale) : String.format("1 block = %.0f px", 1 / scale);
        g.drawString(font, sc, width - 70 - font.width(sc), height - 16, Ui.MUTED, false);
        super.render(g, mx, my, pt);
    }

    private void drawAtlas(GuiGraphics g) {
        int imgW = ChinaMap.width(), imgH = ChinaMap.height();
        double px = ChinaMap.pixelSize();
        // Image pixels in view.
        double u0 = (bx(mapX()) - ChinaMap.originX()) / px, v0 = (bz(mapY()) - ChinaMap.originZ()) / px;
        double u1 = (bx(mapX() + mapW()) - ChinaMap.originX()) / px, v1 = (bz(mapY() + mapH()) - ChinaMap.originZ()) / px;
        double cu0 = Mth.clamp(u0, 0, imgW), cv0 = Mth.clamp(v0, 0, imgH), cu1 = Mth.clamp(u1, 0, imgW), cv1 = Mth.clamp(v1, 0, imgH);
        if (cu1 <= cu0 || cv1 <= cv0) return;
        int x0 = (int) Math.floor(sx(ChinaMap.originX() + cu0 * px)), y0 = (int) Math.floor(sy(ChinaMap.originZ() + cv0 * px));
        int x1 = (int) Math.ceil(sx(ChinaMap.originX() + cu1 * px)), y1 = (int) Math.ceil(sy(ChinaMap.originZ() + cv1 * px));
        minecraft.getTextureManager().getTexture(ATLAS).setFilter(scale > px / 2, false);
        g.blit(ATLAS, x0, y0, x1 - x0, y1 - y0, (float) cu0, (float) cv0, Math.max(1, (int) Math.round(cu1 - cu0)), Math.max(1, (int) Math.round(cv1 - cv0)), imgW, imgH);
    }

    /** The land you have seen, once zoomed in far enough for it to matter. */
    private void drawExplored(GuiGraphics g) {
        if (minecraft.level == null || (WorldMapData.china && scale > 24)) return;
        String dim = minecraft.level.dimension().location().toString();
        int t = ExploredMap.TILE;
        int tx0 = Math.floorDiv((int) Math.floor(bx(mapX())), t), tz0 = Math.floorDiv((int) Math.floor(bz(mapY())), t);
        int tx1 = Math.floorDiv((int) Math.ceil(bx(mapX() + mapW())), t), tz1 = Math.floorDiv((int) Math.ceil(bz(mapY() + mapH())), t);
        if ((long) (tx1 - tx0 + 1) * (tz1 - tz0 + 1) > 2500) return;
        for (int tx = tx0; tx <= tx1; tx++)
            for (int tz = tz0; tz <= tz1; tz++) {
                ExploredMap.Tile tile = ExploredMap.tile(dim, tx, tz);
                if (tile == null) continue;
                tile.prepare();
                int x0 = (int) Math.round(sx(tx * t)), y0 = (int) Math.round(sy(tz * t));
                int x1 = (int) Math.round(sx((tx + 1) * t)), y1 = (int) Math.round(sy((tz + 1) * t));
                g.blit(tile.id, x0, y0, x1 - x0, y1 - y0, 0, 0, t, t, t, t);
            }
    }

    private void drawLabels(GuiGraphics g) {
        if (labels == null) return;
        float s = (float) Mth.clamp(1.2 - Math.log10(Math.max(1, scale)) * 0.22, 0.6, 1.2);
        for (Label l : labels) {
            boolean province = l.kind().equals("province");
            if (!province && scale > 200 && !l.kind().equals("sea")) continue;
            // The Lianzhou Basin's rivers, lakes and passes only show close up.
            if (l.kind().startsWith("local") && scale > 16) continue;
            if (province && scale < 6) continue;
            int color = switch (l.kind()) {
                case "province" -> 0xFF4A2C12;
                case "place" -> 0xFF9A1E1E;
                case "sea", "local_water" -> 0xFF2D5A86;
                case "local_land" -> 0xFF5A3A1A;
                default -> 0xFF6E6A62;
            };
            label(g, l.name(), sx(l.x()), sy(l.z()), province ? s : s * 0.85F, color);
        }
        // The Imperial Capitals.
        for (int i = 0; i < ChinaMap.CAPITALS.length; i++) {
            int[] b = ChinaMap.blockOf(ChinaMap.CAPITAL_LONLAT[i][0], ChinaMap.CAPITAL_LONLAT[i][1]);
            int x = (int) Math.round(sx(b[0])), y = (int) Math.round(sy(b[1]));
            g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF2A1A0A);
            g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFC8423B);
            if (scale < 150) label(g, ChinaMap.CAPITALS[i], x, y - 9, s * 0.85F, 0xFF7A1010);
        }
    }

    private void drawTerritories(GuiGraphics g) {
        if (minecraft.level == null) return;
        String dim = minecraft.level.dimension().location().toString();
        for (WorldInfoPacket.Place t : WorldMapData.territories) {
            if (!t.dim().equals(dim)) continue;
            double cx = sx(t.x()), cy = sy(t.z()), r = t.radius() / scale;
            int color = t.mine() ? 0xFF4FD16A : 0xFFE8C15A;
            if (r >= 3) {
                int steps = (int) Mth.clamp(r * 2, 24, 360);
                for (int i = 0; i < steps; i++) {
                    double a = Math.PI * 2 * i / steps;
                    int x = (int) Math.round(cx + Math.cos(a) * r), y = (int) Math.round(cy + Math.sin(a) * r);
                    g.fill(x, y, x + 1, y + 1, color);
                }
            }
            int x = (int) Math.round(cx), y = (int) Math.round(cy);
            g.fill(x - 2, y - 2, x + 2, y + 2, color);
            if (scale < 400 || t.mine()) label(g, t.name(), cx, cy + 9, 0.8F, t.mine() ? 0xFF1E6B2E : 0xFF5A4512);
        }
    }

    private void drawPlayer(GuiGraphics g) {
        if (minecraft.player == null) return;
        double x = sx(minecraft.player.getX()), y = sy(minecraft.player.getZ());
        float yaw = minecraft.player.getYRot();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(yaw + 180));
        // A small arrow pointing where you face.
        g.fill(-1, -5, 1, 4, 0xFF000000);
        g.fill(-3, -1, 3, 4, 0xFF000000);
        g.fill(0, -4, 1, 3, 0xFFFFFFFF);
        g.fill(-2, 0, 2, 3, 0xFFFFFFFF);
        g.pose().popPose();
    }

    private void label(GuiGraphics g, String text, double cx, double cy, float s, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, text, -font.width(text) / 2, -4, color, false);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
