package com.realmlord.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the bandits of this player's quest camp, to outline for them alone. */
public class BanditGlowPacket {
    public final int[] ids;

    public BanditGlowPacket(int[] ids) {
        this.ids = ids;
    }

    public BanditGlowPacket(FriendlyByteBuf buf) {
        int n = Math.min(64, buf.readVarInt());
        ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ids.length);
        for (int id : ids) buf.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.QuestGlow.set(ids));
    }
}
