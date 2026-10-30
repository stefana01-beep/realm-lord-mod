package com.realmlord.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: open one of the mod's screens. */
public class OpenScreenPacket {
    public static final int ROLE = 0;
    public static final int CHARACTER = 1;
    public static final int TERRITORY = 2;
    public static final int QUEST = 3;

    private final int screen;

    public OpenScreenPacket(int screen) {
        this.screen = screen;
    }

    public OpenScreenPacket(FriendlyByteBuf buf) {
        this.screen = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(screen);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.open(screen));
    }
}
