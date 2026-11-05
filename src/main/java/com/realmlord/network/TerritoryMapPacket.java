package com.realmlord.network;

import com.realmlord.territory.Territory;
import com.realmlord.village.TerritoryMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the picture of a territory, and whether to open the map screen. */
public class TerritoryMapPacket {
    public final int x0, z0, step, size;
    public final int[] rgb;
    public final boolean open;

    public TerritoryMapPacket(TerritoryMap.Map m, boolean open) {
        this.x0 = m.x0();
        this.z0 = m.z0();
        this.step = m.step();
        this.size = m.size();
        this.rgb = m.rgb();
        this.open = open;
    }

    public TerritoryMapPacket(FriendlyByteBuf buf) {
        x0 = buf.readVarInt();
        z0 = buf.readVarInt();
        step = buf.readVarInt();
        size = buf.readVarInt();
        rgb = buf.readVarIntArray();
        open = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(x0);
        buf.writeVarInt(z0);
        buf.writeVarInt(step);
        buf.writeVarInt(size);
        buf.writeVarIntArray(rgb);
        buf.writeBoolean(open);
    }

    public static void send(ServerPlayer p, Territory t, boolean open) {
        ServerLevel level = p.server.getLevel(t.dimension);
        if (level == null) return;
        ManorViewPacket.send(p, t, false);
        ModNetwork.send(p, new TerritoryMapPacket(TerritoryMap.draw(level, t), open));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.map(this));
    }
}
