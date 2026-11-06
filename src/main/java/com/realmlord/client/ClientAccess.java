package com.realmlord.client;

import com.realmlord.network.OpenScreenPacket;
import net.minecraft.client.Minecraft;

/** Client-only entry points, reached from packets through DistExecutor. */
public final class ClientAccess {
    private ClientAccess() {}

    public static void manor(net.minecraft.nbt.CompoundTag view, boolean open) {
        ClientVillageData.manor = view;
        Minecraft mc = Minecraft.getInstance();
        if (open) mc.setScreen(new ManorScreen());
        else if (mc.screen instanceof ManorScreen m) m.refresh();
    }

    public static void resident(net.minecraft.nbt.CompoundTag view, boolean open) {
        ClientVillageData.resident = view;
        Minecraft mc = Minecraft.getInstance();
        if (open) mc.setScreen(new ResidentScreen());
        else if (mc.screen instanceof ResidentScreen r) r.refresh();
    }

    public static void map(com.realmlord.network.TerritoryMapPacket m) {
        ClientMap.set(m);
        Minecraft mc = Minecraft.getInstance();
        if (m.open && !(mc.screen instanceof TerritoryMapScreen)) mc.setScreen(new TerritoryMapScreen(null));
    }

    public static void spawn(com.realmlord.network.SpawnOptionsPacket p) {
        Minecraft.getInstance().setScreen(new SpawnScreen(p));
    }

    public static void naming(com.realmlord.network.OpenNamingPacket p) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new NamingScreen(p));
    }

    public static void guide() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(com.realmlord.item.GuideItem.book())));
    }

    public static void open(int screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (screen) {
            case OpenScreenPacket.ROLE -> mc.setScreen(new RoleScreen());
            case OpenScreenPacket.TERRITORY -> mc.setScreen(new CharacterScreen(CharacterScreen.TAB_TERRITORY));
            case OpenScreenPacket.QUEST -> mc.setScreen(new CharacterScreen(CharacterScreen.TAB_QUEST));
            default -> mc.setScreen(new CharacterScreen(CharacterScreen.TAB_CHARACTER));
        }
    }
}
