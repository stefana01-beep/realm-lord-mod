package com.realmlord.item;

import com.realmlord.Notify;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Summoning Token from the Village Creation quest. In the novel it calls a talent to the
 * Lord's side; it becomes usable when historical figures arrive (Phase 3).
 */
public class SummoningTokenItem extends Item {
    public SummoningTokenItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Notify.player(sp, "realmlord.summoning.not_yet");
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.summoning.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.summoning.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
