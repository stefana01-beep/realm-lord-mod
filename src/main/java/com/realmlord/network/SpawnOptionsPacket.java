package com.realmlord.network;

import com.realmlord.territory.SpawnPicker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server to client: the starting places to choose from, which opens the choice screen. */
public class SpawnOptionsPacket {
    public final boolean china;
    public final List<SpawnPicker.Option> options;
    public final int spawnX, spawnZ;

    public SpawnOptionsPacket(boolean china, List<SpawnPicker.Option> options, BlockPos spawn) {
        this.china = china;
        this.options = options;
        this.spawnX = spawn.getX();
        this.spawnZ = spawn.getZ();
    }

    public SpawnOptionsPacket(FriendlyByteBuf buf) {
        china = buf.readBoolean();
        spawnX = buf.readVarInt();
        spawnZ = buf.readVarInt();
        int n = Math.min(16, buf.readVarInt());
        options = new ArrayList<>();
        for (int i = 0; i < n; i++) options.add(new SpawnPicker.Option(buf.readUtf(128), buf.readVarInt(), buf.readVarInt()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(china);
        buf.writeVarInt(spawnX);
        buf.writeVarInt(spawnZ);
        buf.writeVarInt(options.size());
        for (SpawnPicker.Option o : options) {
            buf.writeUtf(o.key(), 128);
            buf.writeVarInt(o.x());
            buf.writeVarInt(o.z());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.realmlord.client.ClientAccess.spawn(this));
    }
}
