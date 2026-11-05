package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.territory.SpawnPicker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: begin at this place. */
public class SpawnChoicePacket {
    private final int x, z;

    public SpawnChoicePacket(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public SpawnChoicePacket(FriendlyByteBuf buf) {
        x = buf.readVarInt();
        z = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        String err = SpawnPicker.choose(p, x, z, p.hasPermissions(2));
        if (err != null) {
            Notify.player(p, err);
            SpawnPicker.offer(p);
        }
    }
}
