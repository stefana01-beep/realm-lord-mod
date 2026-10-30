package com.realmlord.network;

import com.realmlord.RealmLord;
import com.realmlord.profile.Profile;
import com.realmlord.profile.ProfileCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RealmLord.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(SyncProfilePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncProfilePacket::encode)
                .decoder(SyncProfilePacket::new)
                .consumerMainThread(SyncProfilePacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenScreenPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScreenPacket::encode)
                .decoder(OpenScreenPacket::new)
                .consumerMainThread(OpenScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChooseRolePacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChooseRolePacket::encode)
                .decoder(ChooseRolePacket::new)
                .consumerMainThread(ChooseRolePacket::handle)
                .add();
        CHANNEL.messageBuilder(ProfileActionPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileActionPacket::encode)
                .decoder(ProfileActionPacket::new)
                .consumerMainThread(ProfileActionPacket::handle)
                .add();
    }

    public static void toServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void send(ServerPlayer player, Object packet) {
        if (player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sync(ServerPlayer player) {
        ProfileCapability.get(player).ifPresent(p -> {
            send(player, new SyncProfilePacket(player, p));
            p.clearDirty();
        });
    }

    public static void syncIfDirty(ServerPlayer player) {
        ProfileCapability.get(player).filter(Profile::isDirty).ifPresent(p -> sync(player));
    }
}
