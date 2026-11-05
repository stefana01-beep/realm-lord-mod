package com.realmlord.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.realmlord.RealmLord;
import com.realmlord.network.TerritoryMapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** The territory map as a texture, rebuilt whenever the server sends a new picture. */
public final class ClientMap {
    private ClientMap() {}

    public static final ResourceLocation ID = RealmLord.id("dynamic/territory_map");
    public static int x0, z0, step = 1, size;
    public static boolean ready;
    private static DynamicTexture texture;

    public static void set(TerritoryMapPacket m) {
        x0 = m.x0;
        z0 = m.z0;
        step = Math.max(1, m.step);
        size = m.size;
        NativeImage img = new NativeImage(size, size, false);
        for (int z = 0; z < size; z++)
            for (int x = 0; x < size; x++) {
                int i = z * size + x;
                int rgb = i < m.rgb.length ? m.rgb[i] : 0;
                if (rgb == 0) rgb = 0xFF1C1612 | (((x + z) % 6 == 0) ? 0x080808 : 0);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                img.setPixelRGBA(x, z, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        texture = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(ID, texture);
        ready = true;
    }
}
