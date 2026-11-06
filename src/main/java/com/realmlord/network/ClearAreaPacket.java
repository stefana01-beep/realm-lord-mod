package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: clear and level the ground between two corners, at the usual height plus an offset. */
public class ClearAreaPacket {
    private final int x0, z0, x1, z1, offset;

    public ClearAreaPacket(int x0, int z0, int x1, int z1, int offset) {
        this.x0 = x0;
        this.z0 = z0;
        this.x1 = x1;
        this.z1 = z1;
        this.offset = offset;
    }

    public ClearAreaPacket(FriendlyByteBuf buf) {
        x0 = buf.readInt();
        z0 = buf.readInt();
        x1 = buf.readInt();
        z1 = buf.readInt();
        offset = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(x0);
        buf.writeInt(z0);
        buf.writeInt(x1);
        buf.writeInt(z1);
        buf.writeVarInt(offset);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return;
        String err = Village.startClearing(p, t, x0, z0, x1, z1, offset);
        if (err != null) Notify.player(p, err);
        data.touch();
        ModNetwork.sync(p);
        TerritoryMapPacket.send(p, t, false);
    }
}
