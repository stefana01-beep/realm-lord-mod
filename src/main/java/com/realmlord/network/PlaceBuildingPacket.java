package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: start a building where the Lord placed its outline. */
public class PlaceBuildingPacket {
    private final int type;
    private final BlockPos origin;
    private final int rotation;

    public PlaceBuildingPacket(int type, BlockPos origin, int rotation) {
        this.type = type;
        this.origin = origin;
        this.rotation = rotation;
    }

    public PlaceBuildingPacket(FriendlyByteBuf buf) {
        type = buf.readVarInt();
        origin = buf.readBlockPos();
        rotation = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeBlockPos(origin);
        buf.writeVarInt(rotation);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        BuildingType bt = BuildingType.byOrdinal(type);
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (bt == null || t == null || !p.getUUID().equals(t.owner)) return;
        if (p.distanceToSqr(origin.getX(), origin.getY(), origin.getZ()) > 64 * 64) return;
        String err = Village.start(p, t, bt, origin, rotation);
        if (err != null) Notify.player(p, err);
        data.touch();
        ManorViewPacket.send(p, t, false);
    }
}
