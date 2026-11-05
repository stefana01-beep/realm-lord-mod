package com.realmlord.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.realmlord.RealmLord;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.SpawnChoicePacket;
import com.realmlord.network.SpawnOptionsPacket;
import com.realmlord.territory.SpawnPicker;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Where to begin. On the China world the whole map is shown with its recommended regions
 * marked; pick one from the list or click anywhere on land. Elsewhere, the kinds of country
 * found around the world spawn are listed with how far away they are.
 */
public class SpawnScreen extends Screen {
    private static final ResourceLocation MAP_ID = RealmLord.id("dynamic/china_map");
    private static DynamicTexture mapTexture;
    private static int mapW, mapH;

    private final SpawnOptionsPacket data;
    private int selected = 0;
    /** A place clicked on the map instead of a listed one. */
    private Integer customX, customZ;

    private static final int W = 440, H = 300;

    public SpawnScreen(SpawnOptionsPacket data) {
        super(Component.translatable("realmlord.spawn.title"));
        this.data = data;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    // The map panel keeps the map's shape.
    private int mapAreaW() {
        return 270;
    }

    private int mapAreaH() {
        return mapW == 0 ? 192 : (int) Math.round(mapAreaW() * mapH / (double) mapW);
    }

    private int mapX() {
        return left() + 10;
    }

    private int mapY() {
        return top() + 24;
    }

    @Override
    protected void init() {
        clearWidgets();
        if (data.china) buildMap();
        int lx = data.china ? left() + mapAreaW() + 20 : left() + 10;
        int lw = data.china ? W - mapAreaW() - 30 : 180;
        for (int i = 0; i < data.options.size(); i++) {
            final int k = i;
            SpawnPicker.Option o = data.options.get(i);
            addRenderableWidget(Button.builder(Component.translatable(o.key()), b -> {
                selected = k;
                customX = customZ = null;
            }).bounds(lx, top() + 24 + i * 19, lw, 17).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("realmlord.spawn.begin"), b -> begin())
                .bounds(left() + W - 130, top() + H - 26, 120, 18).build());
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

    /** Draws the China map once: sea, plains, forest, desert, mountains, snow and the great rivers. */
    private static void buildMap() {
        if (mapTexture != null || !ChinaMap.ready()) return;
        int w = ChinaMap.width(), h = ChinaMap.height();
        int step = 2;
        mapW = w / step;
        mapH = h / step;
        NativeImage img = new NativeImage(mapW, mapH, false);
        for (int y = 0; y < mapH; y++)
            for (int x = 0; x < mapW; x++) {
                int ix = x * step, iz = y * step;
                double cont = ChinaMap.pixel(ChinaMap.CONT, ix, iz);
                double temp = ChinaMap.pixel(ChinaMap.TEMP, ix, iz), veg = ChinaMap.pixel(ChinaMap.VEG, ix, iz);
                double mtn = ChinaMap.pixel(ChinaMap.MOUNTAIN, ix, iz), river = ChinaMap.pixel(ChinaMap.RIVER, ix, iz);
                double lift = ChinaMap.pixel(ChinaMap.LIFT, ix, iz);
                int rgb;
                if (cont < -0.12) {
                    double deep = Math.min(1, Math.max(0, (-cont - 0.12) * 1.6));
                    rgb = mix(0x4C7FB8, 0x23446E, deep);
                } else if (river > 0.45) {
                    rgb = 0x3F76B0;
                } else {
                    if (temp > 0.35 && veg < -0.35) rgb = 0xD8C28C;          // desert
                    else if (temp < -0.45) rgb = 0xE6ECEF;                    // snow
                    else if (veg > 0.35) rgb = 0x3F6E36;                      // forest
                    else if (veg > -0.1) rgb = 0x79A456;                      // green country
                    else rgb = 0xA7A86A;                                      // dry grassland
                    double high = Math.min(1, Math.max(0, Math.max(mtn, lift * 0.8)));
                    rgb = mix(rgb, temp < -0.2 ? 0xF4F6F8 : 0x8C7B66, high * 0.75);
                }
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                img.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        mapTexture = new DynamicTexture(img);
        net.minecraft.client.Minecraft.getInstance().getTextureManager().register(MAP_ID, mapTexture);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // Map pixel <-> block. The texture's pixel i covers map pixel i*2.
    private int screenX(double bx) {
        double ix = (bx - ChinaMap.blockX(0)) / (ChinaMap.blockX(1) - ChinaMap.blockX(0));
        return mapX() + (int) Math.round(ix / ChinaMap.width() * mapAreaW());
    }

    private int screenY(double bz) {
        double iz = (bz - ChinaMap.blockZ(0)) / (ChinaMap.blockZ(1) - ChinaMap.blockZ(0));
        return mapY() + (int) Math.round(iz / ChinaMap.height() * mapAreaH());
    }

    private double blockX(double sx) {
        double ix = (sx - mapX()) / mapAreaW() * ChinaMap.width();
        return ChinaMap.blockX(ix - 0.5);
    }

    private double blockZ(double sy) {
        double iz = (sy - mapY()) / mapAreaH() * ChinaMap.height();
        return ChinaMap.blockZ(iz - 0.5);
    }

    private boolean overMap(double mx, double my) {
        return data.china && mapTexture != null && Ui.inside(mx, my, mapX(), mapY(), mapAreaW(), mapAreaH());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overMap(mx, my)) {
            int bx = (int) Math.round(blockX(mx)), bz = (int) Math.round(blockZ(my));
            // Clicking near a recommended place picks it.
            for (int i = 0; i < data.options.size(); i++) {
                SpawnPicker.Option o = data.options.get(i);
                if (Math.abs(screenX(o.x()) - mx) <= 4 && Math.abs(screenY(o.z()) - my) <= 4) {
                    selected = i;
                    customX = customZ = null;
                    return true;
                }
            }
            customX = bx;
            customZ = bz;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = left(), y = top();
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + W / 2, y + 8, Ui.GOLD);
        if (data.china) {
            int ax = mapX(), ay = mapY(), aw = mapAreaW(), ah = mapAreaH();
            g.fill(ax - 1, ay - 1, ax + aw + 1, ay + ah + 1, 0xFF8C6A2A);
            if (mapTexture != null) {
                g.blit(MAP_ID, ax, ay, aw, ah, 0, 0, mapW, mapH, mapW, mapH);
                for (int i = 0; i < data.options.size(); i++) {
                    SpawnPicker.Option o = data.options.get(i);
                    int px = screenX(o.x()), py = screenY(o.z());
                    boolean sel = customX == null && i == selected;
                    g.fill(px - 3, py - 3, px + 3, py + 3, 0xFF000000);
                    g.fill(px - 2, py - 2, px + 2, py + 2, sel ? 0xFFFFD84D : 0xFFC8423B);
                }
                if (customX != null) {
                    int px = screenX(customX), py = screenY(customZ);
                    g.fill(px - 3, py - 3, px + 3, py + 3, 0xFF000000);
                    g.fill(px - 2, py - 2, px + 2, py + 2, 0xFFFFFFFF);
                }
                if (overMap(mx, my)) {
                    g.drawString(font, Component.translatable("realmlord.spawn.coords", (int) blockX(mx), (int) blockZ(my)), ax, y + 8, Ui.MUTED, false);
                }
            } else {
                Ui.centered(g, font, Component.translatable("realmlord.map.loading"), ax + aw / 2, ay + ah / 2, Ui.MUTED);
            }
        }
        // What is chosen, and why one might.
        int dx = data.china ? x + 10 : x + 200, dy = data.china ? mapY() + mapAreaH() + 8 : y + 26, dw = data.china ? W - 150 : W - 210;
        if (customX != null) {
            g.drawString(font, Component.translatable("realmlord.spawn.custom", customX, customZ), dx, dy, Ui.GOLD, false);
            Ui.wrap(g, font, Component.translatable("realmlord.spawn.custom.desc"), dx, dy + 12, dw, Ui.INK);
        } else if (!data.options.isEmpty()) {
            SpawnPicker.Option o = data.options.get(Math.min(selected, data.options.size() - 1));
            g.drawString(font, Component.translatable(o.key()), dx, dy, Ui.GOLD, false);
            int ny = Ui.wrap(g, font, Component.translatable(o.key() + ".desc"), dx, dy + 12, dw, Ui.INK);
            if (!data.china) {
                int ddx = o.x() - data.spawnX, ddz = o.z() - data.spawnZ;
                int dist = (int) Math.round(Math.sqrt((double) ddx * ddx + (double) ddz * ddz));
                g.drawString(font, Component.translatable("realmlord.spawn.distance", dist, Component.translatable(direction(ddx, ddz)), o.x(), o.z()),
                        dx, ny + 6, Ui.MUTED, false);
            }
        }
        if (data.china) Ui.wrap(g, font, Component.translatable("realmlord.spawn.hint"), x + 10, y + H - 22, W - 150, Ui.MUTED);
        super.render(g, mx, my, pt);
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
