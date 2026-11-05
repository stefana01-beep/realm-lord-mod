package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: lay out the Simple Fence with these gates, or move the gates of the one standing. */
public class FenceGatesPacket {
    private final int[] gates;

    public FenceGatesPacket(int[] gates) {
        this.gates = gates;
    }

    public FenceGatesPacket(FriendlyByteBuf buf) {
        int n = Math.min(32, buf.readVarInt());
        gates = new int[n];
        for (int i = 0; i < n; i++) gates[i] = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(gates.length);
        for (int g : gates) buf.writeVarInt(g);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return;
        boolean exists = false;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) exists = true;
        if (exists) {
            Village.setGates(p.serverLevel(), t, gates);
            Notify.player(p, Component.translatable("realmlord.fence.gates_set", Village.cleanGates(gates).length));
        } else {
            String err = Village.start(p, t, BuildingType.SIMPLE_FENCE, t.center, 0, gates);
            if (err != null) Notify.player(p, err);
        }
        data.touch();
        ModNetwork.sync(p);
        TerritoryMapPacket.send(p, t, false);
    }
}
