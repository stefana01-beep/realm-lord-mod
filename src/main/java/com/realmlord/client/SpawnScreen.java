package com.realmlord.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realmlord.RealmLord;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.SpawnChoicePacket;
import com.realmlord.network.SpawnOptionsPacket;
import com.realmlord.territory.SpawnPicker;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Where to begin. On the China world the map fills the window: the novel's provinces, the
 * great rivers and ranges, and the recommended places marked in red. Scroll to zoom, drag to
 * move about, click a marker or anywhere on land to choose. Elsewhere, the kinds of country
 * found around the world spawn are listed with how far away they are.
 */
public class SpawnScreen extends Screen {
    private static final ResourceLocation MAP = RealmLord.id("textures/gui/china_map.png");
    /** Image pixels per map pixel (see tools/gen_china_atlas.py). */
    private static final int IMAGE_SCALE = 1;

    private record Label(String name, int x, int z, String kind) {}

    private static List<Label> labels;

    private final SpawnOptionsPacket data;
    private int selected = 0;
    private Integer customX, customZ;

    // The view: zoom (1 = whole map) and the image pixel at the centre of the map area.
    private double zoom = 1, viewU, viewV;
    private boolean viewSet;
    private double pressX, pressY;
    private boolean dragging, pressedOnMap;

    private int W, H;

    public SpawnScreen(SpawnOptionsPacket data) {
        super(Component.translatable("realmlord.spawn.title"));
        this.data = data;
    }

    // ------------------------------------------------------------------ layout

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private int listW() {
        return data.china ? Mth.clamp(W / 5, 96, 150) : Mth.clamp(W * 2 / 5, 110, 180);
    }

    /** Room under the map for the chosen place's description. */
    private static final int BELOW = 40;

    private int mapX() {
        return left() + 6;
    }

    private int mapY() {
        return top() + 18;
    }

    private int mapW() {
        return W - listW() - 18;
    }

    private int mapH() {
        return H - 18 - BELOW;
    }

    private int rowH() {
        int room = H - 18 - 30;
        return Mth.clamp(room / Math.max(1, data.options.size()), 12, 20);
    }

    private static int imageW() {
        return ChinaMap.width() * IMAGE_SCALE;
    }

    private static int imageH() {
        return ChinaMap.height() * IMAGE_SCALE;
    }

    /** Image pixels per screen pixel at the current zoom (the whole map fits at zoom 1). */
    private double texelsPerPixel() {
        double fit = Math.max(imageW() / (double) mapW(), imageH() / (double) mapH());
        return fit / zoom;
    }

    private void clampView() {
        double tpp = texelsPerPixel();
        double halfW = mapW() * tpp / 2, halfH = mapH() * tpp / 2;
        viewU = halfW * 2 >= imageW() ? imageW() / 2.0 : Mth.clamp(viewU, halfW, imageW() - halfW);
        viewV = halfH * 2 >= imageH() ? imageH() / 2.0 : Mth.clamp(viewV, halfH, imageH() - halfH);
    }

    // block <-> image pixel <-> screen
    private static double imageU(double bx) {
        return (bx - ChinaMap.originX()) / ChinaMap.pixelSize() * IMAGE_SCALE;
    }

    private static double imageV(double bz) {
        return (bz - ChinaMap.originZ()) / ChinaMap.pixelSize() * IMAGE_SCALE;
    }

    private double screenX(double bx) {
        return mapX() + mapW() / 2.0 + (imageU(bx) - viewU) / texelsPerPixel();
    }

    private double screenY(double bz) {
        return mapY() + mapH() / 2.0 + (imageV(bz) - viewV) / texelsPerPixel();
    }

    private double blockX(double sx) {
        double u = viewU + (sx - mapX() - mapW() / 2.0) * texelsPerPixel();
        return ChinaMap.originX() + u / IMAGE_SCALE * ChinaMap.pixelSize();
    }

    private double blockZ(double sy) {
        double v = viewV + (sy - mapY() - mapH() / 2.0) * texelsPerPixel();
        return ChinaMap.originZ() + v / IMAGE_SCALE * ChinaMap.pixelSize();
    }

    private boolean overMap(double mx, double my) {
        return data.china && ChinaMap.ready() && Ui.inside(mx, my, mapX(), mapY(), mapW(), mapH());
    }

    // ------------------------------------------------------------------ setup

    @Override
    protected void init() {
        clearWidgets();
        W = width - 8;
        H = height - 8;
        if (data.china) {
            loadLabels();
            // Smooth when the whole map is shrunk to fit.
            minecraft.getTextureManager().getTexture(MAP).setFilter(true, false);
            if (!viewSet) {
                viewU = imageW() / 2.0;
                viewV = imageH() / 2.0;
                viewSet = true;
            }
            clampView();
        }
        int lw = listW();
        int lx = data.china ? left() + W - lw - 6 : left() + 6;
        int rh = rowH();
        for (int i = 0; i < data.options.size(); i++) {
            final int k = i;
            SpawnPicker.Option o = data.options.get(i);
            Component name = Component.translatable(o.key());
            Button b = Button.builder(Component.literal(font.plainSubstrByWidth(name.getString(), lw - 8)), btn -> pick(k))
                    .bounds(lx, top() + 18 + i * rh, lw, rh - 2).build();
            b.setTooltip(Tooltip.create(Component.translatable(o.key() + ".desc")));
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.spawn.begin"), b -> begin())
                .bounds(left() + W - lw - 6, top() + H - 24, lw, 18).build());
    }

    private void pick(int k) {
        selected = k;
        customX = customZ = null;
        if (data.china) {
            // Bring the place into view.
            SpawnPicker.Option o = data.options.get(k);
            zoom = Math.max(zoom, 2.5);
            viewU = imageU(o.x());
            viewV = imageV(o.z());
            clampView();
        }
    }

    private static void loadLabels() {
        if (labels != null) return;
        labels = new ArrayList<>();
        try (var in = SpawnScreen.class.getResourceAsStream("/realmlord/china/labels.json")) {
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

    private void begin() {
        int x, z;
        if (customX != null) {
            x = customX;
            z = customZ;
        } else {
            SpawnPicker.Option o = data.options.get(Math.min(selected, data.options.size() - 1));
            x = o.x();
            z = o.z();
        }
        ModNetwork.toServer(new SpawnChoicePacket(x, z));
        onClose();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!overMap(mx, my)) return super.mouseScrolled(mx, my, delta);
        // Zoom about the point under the cursor.
        double bx = blockX(mx), bz = blockZ(my);
        zoom = Mth.clamp(zoom * (delta > 0 ? 1.25 : 0.8), 1, 8);
        viewU += imageU(bx) - (viewU + (mx - mapX() - mapW() / 2.0) * texelsPerPixel());
        viewV += imageV(bz) - (viewV + (my - mapY() - mapH() / 2.0) * texelsPerPixel());
        clampView();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (overMap(mx, my) && (button == 0 || button == 1)) {
            pressX = mx;
            pressY = my;
            dragging = false;
            pressedOnMap = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressedOnMap) {
            if (Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) dragging = true;
            if (dragging) {
                viewU -= dx * texelsPerPixel();
                viewV -= dy * texelsPerPixel();
                clampView();
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressedOnMap) {
            pressedOnMap = false;
            if (!dragging && button == 0 && overMap(mx, my)) choose(mx, my);
            dragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void choose(double mx, double my) {
        for (int i = 0; i < data.options.size(); i++) {
            SpawnPicker.Option o = data.options.get(i);
            if (Math.abs(screenX(o.x()) - mx) <= 5 && Math.abs(screenY(o.z()) - my) <= 5) {
                selected = i;
                customX = customZ = null;
                return;
            }
        }
        int bx = (int) Math.round(blockX(mx)), bz = (int) Math.round(blockZ(my));
        if (!ChinaMap.onMap(bx, bz)) return;
        customX = bx;
        customZ = bz;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + (W - listW()) / 2, y + 5, Ui.GOLD);
        if (data.china && ChinaMap.ready()) drawMap(g, mx, my);
        drawDescription(g);
        super.render(g, mx, my, pt);
    }

    private void drawMap(GuiGraphics g, int mx, int my) {
        int ax = mapX(), ay = mapY(), aw = mapW(), ah = mapH();
        g.fill(ax - 1, ay - 1, ax + aw + 1, ay + ah + 1, 0xFF6B4A22);
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF7FA9C9);
        double tpp = texelsPerPixel();
        // The part of the image in view, and where it lands on screen (the whole map may not fill the area).
        double u0 = viewU - aw * tpp / 2, v0 = viewV - ah * tpp / 2;
        double cu0 = Math.max(0, u0), cv0 = Math.max(0, v0);
        double cu1 = Math.min(imageW(), u0 + aw * tpp), cv1 = Math.min(imageH(), v0 + ah * tpp);
        int sx0 = (int) Math.round(ax + (cu0 - u0) / tpp), sy0 = (int) Math.round(ay + (cv0 - v0) / tpp);
        int sx1 = (int) Math.round(ax + (cu1 - u0) / tpp), sy1 = (int) Math.round(ay + (cv1 - v0) / tpp);
        g.enableScissor(ax, ay, ax + aw, ay + ah);
        g.blit(MAP, sx0, sy0, sx1 - sx0, sy1 - sy0, (float) cu0, (float) cv0, (int) Math.round(cu1 - cu0), (int) Math.round(cv1 - cv0), imageW(), imageH());

        // Names: provinces always, smaller ones once zoomed in.
        float scale = (float) Mth.clamp(0.55 + zoom * 0.12, 0.6, 1.0);
        for (Label l : labels) {
            boolean small = !l.kind().equals("province");
            if (small && zoom < 1.6 && !l.kind().equals("sea")) continue;
            int color = switch (l.kind()) {
                case "province" -> 0xFF4A2C12;
                case "place" -> 0xFF9A1E1E;
                case "sea" -> 0xFF2D5A86;
                default -> 0xFF6E6A62;
            };
            label(g, l.name(), screenX(l.x()), screenY(l.z()), small ? scale * 0.85F : scale, color);
        }
        // Recommended places.
        for (int i = 0; i < data.options.size(); i++) {
            SpawnPicker.Option o = data.options.get(i);
            int px = (int) Math.round(screenX(o.x())), py = (int) Math.round(screenY(o.z()));
            boolean sel = customX == null && i == selected;
            int r = sel ? 4 : 3;
            g.fill(px - r - 1, py - r - 1, px + r + 1, py + r + 1, 0xFF2A1A0A);
            g.fill(px - r, py - r, px + r, py + r, sel ? 0xFFFFD84D : 0xFFC8423B);
            if (sel || zoom >= 2.5) {
                String name = Component.translatable(o.key()).getString();
                label(g, name, px, py - 9, scale * 0.9F, sel ? 0xFF7A1010 : 0xFF5A1A1A);
            }
        }
        if (customX != null) {
            int px = (int) Math.round(screenX(customX)), py = (int) Math.round(screenY(customZ));
            g.fill(px - 4, py - 4, px + 4, py + 4, 0xFF000000);
            g.fill(px - 3, py - 3, px + 3, py + 3, 0xFFFFFFFF);
        }
        g.disableScissor();
        if (overMap(mx, my)) {
            g.drawString(font, Component.translatable("realmlord.spawn.coords", (int) blockX(mx), (int) blockZ(my)), ax + 3, ay + ah - 11, 0xFF2A1A0A, false);
        }
    }

    private void label(GuiGraphics g, String text, double cx, double cy, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1);
        int w = font.width(text);
        g.drawString(font, text, -w / 2, -4, color, false);
        g.pose().popPose();
    }

    private void drawDescription(GuiGraphics g) {
        int x = left(), y = top();
        int dx = data.china ? x + 8 : x + listW() + 16;
        int dy = data.china ? mapY() + mapH() + 4 : y + 20;
        int dw = data.china ? mapW() : W - listW() - 24;
        if (customX != null) {
            g.drawString(font, Component.translatable("realmlord.spawn.custom", customX, customZ), dx, dy, Ui.GOLD, false);
            Ui.wrap(g, font, Component.translatable("realmlord.spawn.custom.desc"), dx, dy + 11, dw, Ui.INK);
        } else if (!data.options.isEmpty()) {
            SpawnPicker.Option o = data.options.get(Math.min(selected, data.options.size() - 1));
            g.drawString(font, Component.translatable(o.key()), dx, dy, Ui.GOLD, false);
            int ny = Ui.wrap(g, font, Component.translatable(o.key() + ".desc"), dx, dy + 11, dw, Ui.INK);
            if (!data.china) {
                int ddx = o.x() - data.spawnX, ddz = o.z() - data.spawnZ;
                int dist = (int) Math.round(Math.sqrt((double) ddx * ddx + (double) ddz * ddz));
                g.drawString(font, Component.translatable("realmlord.spawn.distance", dist, Component.translatable(direction(ddx, ddz)), o.x(), o.z()),
                        dx, ny + 6, Ui.MUTED, false);
            }
        }
        if (data.china) {
            int listEnd = top() + 18 + data.options.size() * rowH() + 4;
            int room = top() + H - 28 - listEnd;
            if (room >= 30) Ui.wrap(g, font, Component.translatable("realmlord.spawn.hint"), x + W - listW() - 6, listEnd, listW(), Ui.MUTED);
        }
    }

    private static String direction(int dx, int dz) {
        if (Math.abs(dx) < 16 && Math.abs(dz) < 16) return "realmlord.spawn.dir.here";
        String[] dirs = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        double a = Math.toDegrees(Math.atan2(dz, dx));
        int i = (int) Math.round(((a + 360) % 360) / 45) % 8;
        return "realmlord.spawn.dir." + dirs[i];
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
