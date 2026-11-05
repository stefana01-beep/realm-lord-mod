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
        CHANNEL.messageBuilder(ManorViewPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManorViewPacket::encode)
                .decoder(ManorViewPacket::new)
                .consumerMainThread(ManorViewPacket::handle)
                .add();
        CHANNEL.messageBuilder(ManorActionPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ManorActionPacket::encode)
                .decoder(ManorActionPacket::new)
                .consumerMainThread(ManorActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(PlaceBuildingPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlaceBuildingPacket::encode)
                .decoder(PlaceBuildingPacket::new)
                .consumerMainThread(PlaceBuildingPacket::handle)
                .add();
        CHANNEL.messageBuilder(ResidentViewPacket.class, 7, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResidentViewPacket::encode)
                .decoder(ResidentViewPacket::new)
                .consumerMainThread(ResidentViewPacket::handle)
                .add();
        CHANNEL.messageBuilder(TerritoryMapPacket.class, 9, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerritoryMapPacket::encode)
                .decoder(TerritoryMapPacket::new)
                .consumerMainThread(TerritoryMapPacket::handle)
                .add();
        CHANNEL.messageBuilder(ResidentActionPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResidentActionPacket::encode)
                .decoder(ResidentActionPacket::new)
                .consumerMainThread(ResidentActionPacket::handle)
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
