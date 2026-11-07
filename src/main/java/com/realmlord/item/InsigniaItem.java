package com.realmlord.item;

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
 * A bandit's insignia, taken from the fallen. Right-click it in your own territory to post the
 * kills at the manor: reputation for each, a small bounty from the county, and the villagers
 * sleep easier (Security rises one for every five).
 */
public class InsigniaItem extends Item {
    public static final long BOUNTY = 50, REPUTATION = 2;

    public InsigniaItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        int n = stack.getCount();
        String err = com.realmlord.village.Village.turnInInsignia(sp, n);
        if (err != null) {
            sp.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(n);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.insignia.tip").withStyle(ChatFormatting.GRAY));
    }
}
