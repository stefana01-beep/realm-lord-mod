package com.realmlord.client;

import com.realmlord.network.FoundVillagePacket;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.OpenNamingPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Naming the village as it is founded. */
public class NamingScreen extends Screen {
    private static final int W = 240, H = 104;
    private final OpenNamingPacket req;
    private EditBox box;

    public NamingScreen(OpenNamingPacket req) {
        super(Component.translatable("realmlord.naming.title"));
        this.req = req;
    }

    @Override
    protected void init() {
        if (Ui.fit(this, W, H)) return;
        int x = (width - W) / 2, y = (height - H) / 2;
        String was = box != null ? box.getValue() : !req.name.isEmpty() ? req.name
                : Component.translatable("realmlord.territory.default_name", minecraft.player == null ? "" : minecraft.player.getGameProfile().getName()).getString();
        box = new EditBox(font, x + 14, y + 40, W - 28, 18, Component.translatable("realmlord.naming.title"));
        box.setMaxLength(FoundVillagePacket.MAX_NAME);
        box.setValue(was);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(Component.translatable("realmlord.naming.found"), b -> found())
                .bounds(x + 14, y + H - 28, (W - 34) / 2, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("realmlord.map.cancel"), b -> onClose())
                .bounds(x + 20 + (W - 34) / 2, y + H - 28, (W - 34) / 2, 18).build());
    }

    private void found() {
        String name = FoundVillagePacket.clean(box.getValue());
        if (name == null) return;
        ModNetwork.toServer(new FoundVillagePacket(req.pos, name, req.offhand));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            found();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = (width - W) / 2, y = (height - H) / 2;
        Ui.panel(g, x, y, W, H);
        Ui.centered(g, font, title, x + W / 2, y + 8, Ui.GOLD);
        Ui.centered(g, font, Component.translatable("realmlord.naming.hint"), x + W / 2, y + 23, Ui.MUTED);
        super.render(g, mx, my, pt);
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
