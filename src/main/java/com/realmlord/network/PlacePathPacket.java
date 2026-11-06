package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Paths;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: lay a path along these points (x, z pairs) with this surface and width. */
public class PlacePathPacket {
    private final int surface, width;
    private final int[] pts;

    public PlacePathPacket(int surface, int width, int[] pts) {
        this.surface = surface;
        this.width = width;
        this.pts = pts;
    }

    public PlacePathPacket(FriendlyByteBuf buf) {
        surface = buf.readVarInt();
        width = buf.readVarInt();
        int n = Math.min(Paths.MAX_POINTS * 2, buf.readVarInt());
        pts = new int[n];
        for (int i = 0; i < n; i++) pts[i] = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(surface);
        buf.writeVarInt(width);
        buf.writeVarInt(pts.length);
        for (int v : pts) buf.writeInt(v);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return;
        Paths.Surface s = Paths.Surface.byOrdinal(surface);
        String err = Paths.start(p, t, s, width, pts);
        if (err != null) Notify.player(p, err);
        else Notify.player(p, Component.translatable("realmlord.path.started", Component.translatable(s.key()), width));
        data.touch();
        ModNetwork.sync(p);
        TerritoryMapPacket.send(p, t, false);
    }
}
