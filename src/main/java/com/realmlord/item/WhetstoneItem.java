package com.realmlord.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A whetstone: right-click to hone the blade or mend the armour held in the other hand (a quarter of its wear). */
public class WhetstoneItem extends Item {
    public WhetstoneItem(Properties p) {
        super(p);
    }

    /** Mends a quarter of an item's full durability; false if there is nothing to mend. */
    public static boolean hone(ItemStack target) {
        if (target.isEmpty() || !target.isDamageableItem() || target.getDamageValue() <= 0) return false;
        target.setDamageValue(Math.max(0, target.getDamageValue() - Math.max(1, target.getMaxDamage() / 4)));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        ItemStack target = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!hone(target)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("realmlord.whetstone.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stone);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
        if (!player.getAbilities().instabuild) stone.shrink(1);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stone, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.whetstone.tip").withStyle(ChatFormatting.GRAY));
    }
}
