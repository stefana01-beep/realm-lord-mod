package com.realmlord.network;

import com.realmlord.territory.SpawnPicker;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server to client: what the world map needs to know: whether this is the China world, and every territory. */
public class WorldInfoPacket {
    public record Place(String name, String owner, String dim, int x, int z, int radius, boolean mine) {}

    public final boolean china;
    public final List<Place> territories;

    public WorldInfoPacket(boolean china, List<Place> territories) {
        this.china = china;
        this.territories = territories;
    }

    public WorldInfoPacket(FriendlyByteBuf buf) {
        china = buf.readBoolean();
        int n = Math.min(4096, buf.readVarInt());
        territories = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            territories.add(new Place(buf.readUtf(64), buf.readUtf(64), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(china);
        buf.writeVarInt(territories.size());
        for (Place t : territories) {
            buf.writeUtf(t.name(), 64);
            buf.writeUtf(t.owner(), 64);
            buf.writeUtf(t.dim(), 128);
            buf.writeVarInt(t.x());
            buf.writeVarInt(t.z());
            buf.writeVarInt(t.radius());
            buf.writeBoolean(t.mine());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.WorldMapData.accept(this));
    }

    public static void send(ServerPlayer p) {
        List<Place> list = new ArrayList<>();
        for (Territory t : TerritoryData.get(p.server).all()) {
            String name = t.name.length() > 60 ? t.name.substring(0, 60) : t.name;
            String owner = t.ownerName.length() > 60 ? t.ownerName.substring(0, 60) : t.ownerName;
            list.add(new Place(name, owner, t.dimension.location().toString(), t.center.getX(), t.center.getZ(), t.radius(),
                    p.getUUID().equals(t.owner)));
        }
        ModNetwork.send(p, new WorldInfoPacket(SpawnPicker.isChina(p.server.overworld()), list));
    }

    /** Client to server: please send the map's information. */
    public static class Request {
        public Request() {}

        public Request(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) send(p);
        }
    }
}
