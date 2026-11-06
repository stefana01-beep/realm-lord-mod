package com.realmlord.network;

import com.realmlord.item.VillageTokenItem;
import com.realmlord.territory.Founding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: found the village here, under this name, with the token in hand. */
public class FoundVillagePacket {
    public static final int MAX_NAME = 32;
    private final BlockPos pos;
    private final String name;
    private final boolean offhand;

    public FoundVillagePacket(BlockPos pos, String name, boolean offhand) {
        this.pos = pos;
        this.name = name;
        this.offhand = offhand;
    }

    public FoundVillagePacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        name = buf.readUtf(64);
        offhand = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(name, 64);
        buf.writeBoolean(offhand);
    }

    /** A village name as typed: trimmed, without formatting codes, at most 32 characters. */
    public static String clean(String s) {
        if (s == null) return null;
        s = s.replace('§', ' ').strip();
        if (s.length() > MAX_NAME) s = s.substring(0, MAX_NAME).strip();
        return s.isEmpty() ? null : s;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        ItemStack stack = p.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof VillageTokenItem token)) return;
        if (p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 12 * 12) return;
        if (Founding.found(p, pos, token.grade, clean(name)) != null && !p.getAbilities().instabuild) stack.shrink(1);
    }
}
