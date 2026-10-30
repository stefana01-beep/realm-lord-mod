package com.realmlord;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** "System Notification" messages, the way the game in the novel speaks to its players. */
public final class Notify {
    private Notify() {}

    public static MutableComponent prefix() {
        return Component.translatable("realmlord.system.prefix").withStyle(ChatFormatting.GOLD);
    }

    /** A private system message to one player. */
    public static void player(ServerPlayer p, Component body) {
        p.sendSystemMessage(prefix().append(" ").append(body.copy().withStyle(ChatFormatting.YELLOW)));
    }

    public static void player(ServerPlayer p, String key, Object... args) {
        player(p, Component.translatable(key, args));
    }

    /** A world announcement every player sees. */
    public static void world(MinecraftServer server, Component body) {
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("realmlord.system.world").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .append(" ").append(body.copy().withStyle(ChatFormatting.YELLOW)), false);
    }
}
