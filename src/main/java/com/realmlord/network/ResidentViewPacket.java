package com.realmlord.network;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Resident;
import com.realmlord.village.Views;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: a resident's card, work options and trades. */
public class ResidentViewPacket {
    private final CompoundTag view;
    private final boolean open;

    public ResidentViewPacket(CompoundTag view, boolean open) {
        this.view = view;
        this.open = open;
    }

    public ResidentViewPacket(FriendlyByteBuf buf) {
        view = buf.readNbt();
        open = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(view);
        buf.writeBoolean(open);
    }

    public static void open(ServerPlayer p, int territoryId, int residentId) {
        send(p, territoryId, residentId, true);
    }

    public static void send(ServerPlayer p, int territoryId, int residentId, boolean open) {
        Territory t = TerritoryData.get(p.server).byId(territoryId);
        Resident r = t == null ? null : t.resident(residentId);
        if (r == null) return;
        ModNetwork.send(p, new ResidentViewPacket(Views.resident(p, t, r), open));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.resident(view, open));
    }
}
