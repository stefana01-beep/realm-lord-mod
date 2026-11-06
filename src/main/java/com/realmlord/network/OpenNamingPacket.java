package com.realmlord.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: ask the Lord to name the village about to be founded here. */
public class OpenNamingPacket {
    public final BlockPos pos;
    public final String name;
    public final boolean offhand;

    public OpenNamingPacket(BlockPos pos, String name, boolean offhand) {
        this.pos = pos;
        this.name = name;
        this.offhand = offhand;
    }

    public OpenNamingPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        name = buf.readUtf(64);
        offhand = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name, 64);
        buf.writeBoolean(offhand);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.naming(this));
    }
}
