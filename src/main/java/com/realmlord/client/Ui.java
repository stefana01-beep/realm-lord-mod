package com.realmlord.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Drawing helpers for the mod's lacquer-and-gold panels. */
public final class Ui {
    private Ui() {}

    public static final int INK = 0xFFF3E7C9;       // body text on dark lacquer
    public static final int MUTED = 0xFFB5A685;
    public static final int GOLD = 0xFFE8C15A;
    public static final int RED = 0xFFC8423B;
    public static final int JADE = 0xFF6FCF97;
    public static final int GREY = 0xFF7D756A;

    /**
     * Makes a panel of w x h fit the window: when the window is too small for it at the chosen
     * GUI scale (out of full screen, say), the GUI scale is lowered for as long as the panel is
     * open, so nothing is cut off. Returns true if the screen was laid out again at the new scale,
     * in which case the caller's init should stop.
     */
    /** Text cut to a width, with an ellipsis if it had to be. */
    public static net.minecraft.util.FormattedCharSequence fit(net.minecraft.client.gui.Font font, Component c, int w) {
        if (font.width(c) <= w) return c.getVisualOrderText();
        net.minecraft.network.chat.FormattedText cut = font.substrByWidth(c, w - font.width("\u2026"));
        return net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(cut,
                net.minecraft.network.chat.FormattedText.of("\u2026")));
    }

    public static boolean fit(net.minecraft.client.gui.screens.Screen s, int w, int h) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var win = mc.getWindow();
        if (s.width >= w + 8 && s.height >= h + 8) return false;
        int scale = (int) win.getGuiScale(), best = scale;
        while (best > 1 && (win.getWidth() / best < w + 8 || win.getHeight() / best < h + 8)) best--;
        if (best == scale) return false;
        win.setGuiScale(best);
        s.init(mc, win.getGuiScaledWidth(), win.getGuiScaledHeight());
        return true;
    }

    /** Puts the GUI scale back to the player's setting when a fitted panel closes. */
    public static void restoreScale() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var win = mc.getWindow();
        int want = win.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode());
        if ((int) win.getGuiScale() != want) win.setGuiScale(want);
    }

    /** Dark lacquered panel with a thin gold rim. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xF0120C08);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF8C6A2A);
        g.fill(x, y, x + w, y + h, 0xF02A1B14);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, 0x40E8C15A);
    }

    /** A recessed inner box. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xC0140D09);
        g.fill(x, y, x + w, y + 1, 0x60000000);
        g.fill(x, y + h - 1, x + w, y + h, 0x30E8C15A);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
        g.fill(x, y, x + w, y + h, 0xFF3A2C22);
        g.fill(x, y, x + (int) Math.round(w * frac), y + h, color);
    }

    /** Word-wrapped text; returns the y just below the last line. */
    public static int wrap(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    public static void centered(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    public static Component money(long copper) {
        return com.realmlord.village.Coins.format(copper);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
