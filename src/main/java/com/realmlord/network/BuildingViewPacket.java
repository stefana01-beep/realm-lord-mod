package com.realmlord.network;

import com.realmlord.territory.Territory;
import com.realmlord.village.Building;
import com.realmlord.village.Views;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: a building's window (or, for no building, the list of buildings that have one). */
public class BuildingViewPacket {
    private final CompoundTag view;
    private final boolean open;

    public BuildingViewPacket(CompoundTag view, boolean open) {
        this.view = view;
        this.open = open;
    }

    public BuildingViewPacket(FriendlyByteBuf buf) {
        view = buf.readNbt();
        open = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(view);
        buf.writeBoolean(open);
    }

    public static void send(ServerPlayer p, Territory t, Building b, boolean open) {
        ModNetwork.send(p, new BuildingViewPacket(Views.building(p, t, b), open));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.building(view, open));
    }
}
