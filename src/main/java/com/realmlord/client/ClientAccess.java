package com.realmlord.client;

import com.realmlord.network.OpenScreenPacket;
import net.minecraft.client.Minecraft;

/** Client-only entry points, reached from packets through DistExecutor. */
public final class ClientAccess {
    private ClientAccess() {}

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
