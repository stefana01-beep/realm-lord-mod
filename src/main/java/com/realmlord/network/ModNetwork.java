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
        CHANNEL.messageBuilder(FenceGatesPacket.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FenceGatesPacket::encode)
                .decoder(FenceGatesPacket::new)
                .consumerMainThread(FenceGatesPacket::handle)
                .add();
        CHANNEL.messageBuilder(SpawnOptionsPacket.class, 11, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnOptionsPacket::encode)
                .decoder(SpawnOptionsPacket::new)
                .consumerMainThread(SpawnOptionsPacket::handle)
                .add();
        CHANNEL.messageBuilder(SpawnChoicePacket.class, 12, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpawnChoicePacket::encode)
                .decoder(SpawnChoicePacket::new)
                .consumerMainThread(SpawnChoicePacket::handle)
                .add();
        CHANNEL.messageBuilder(WorldInfoPacket.class, 13, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WorldInfoPacket::encode)
                .decoder(WorldInfoPacket::new)
                .consumerMainThread(WorldInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(WorldInfoPacket.Request.class, 14, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WorldInfoPacket.Request::encode)
                .decoder(WorldInfoPacket.Request::new)
                .consumerMainThread(WorldInfoPacket.Request::handle)
                .add();
        CHANNEL.messageBuilder(BanditGlowPacket.class, 15, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BanditGlowPacket::encode)
                .decoder(BanditGlowPacket::new)
                .consumerMainThread(BanditGlowPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenNamingPacket.class, 16, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNamingPacket::encode)
                .decoder(OpenNamingPacket::new)
                .consumerMainThread(OpenNamingPacket::handle)
                .add();
        CHANNEL.messageBuilder(FoundVillagePacket.class, 17, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FoundVillagePacket::encode)
                .decoder(FoundVillagePacket::new)
                .consumerMainThread(FoundVillagePacket::handle)
                .add();
        CHANNEL.messageBuilder(BuildingViewPacket.class, 21, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BuildingViewPacket::encode)
                .decoder(BuildingViewPacket::new)
                .consumerMainThread(BuildingViewPacket::handle)
                .add();
        CHANNEL.messageBuilder(BuildingActionPacket.class, 22, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildingActionPacket::encode)
                .decoder(BuildingActionPacket::new)
                .consumerMainThread(BuildingActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChestActionPacket.class, 20, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChestActionPacket::encode)
                .decoder(ChestActionPacket::new)
                .consumerMainThread(ChestActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ClearAreaPacket.class, 19, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClearAreaPacket::encode)
                .decoder(ClearAreaPacket::new)
                .consumerMainThread(ClearAreaPacket::handle)
                .add();
        CHANNEL.messageBuilder(PlacePathPacket.class, 18, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlacePathPacket::encode)
                .decoder(PlacePathPacket::new)
                .consumerMainThread(PlacePathPacket::handle)
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
