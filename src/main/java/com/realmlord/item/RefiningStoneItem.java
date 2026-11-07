package com.realmlord.item;

import com.realmlord.loot.Gear;
import com.realmlord.territory.TokenGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A refining stone: right-click with a weapon or piece of armour in the other hand to raise it a
 * grade (ungraded gear becomes Bronze). The higher the grade, the likelier the refining fails; a
 * failed refining spends the stone but never harms the gear.
 */
public class RefiningStoneItem extends Item {
    public RefiningStoneItem(Properties p) {
        super(p);
    }

    public static boolean refinable(ItemStack s) {
        Item i = s.getItem();
        return !s.isEmpty() && (i instanceof ArmorItem || i instanceof TieredItem || i instanceof BowItem || i instanceof CrossbowItem)
                && Gear.gradeOf(s) != TokenGrade.GOLD;
    }

    /** The chance (out of 100) that refining to the next grade holds. */
    public static int chance(ItemStack s) {
        TokenGrade g = Gear.gradeOf(s);
        if (g == null) return 100;
        return switch (g) {
            case BRONZE -> 85;
            case BLACK_IRON -> 60;
            case SILVER -> 35;
            default -> 0;
        };
    }

    /** Refines the gear; returns the new stack, or null if the refining failed. */
    public static ItemStack refine(ItemStack s, RandomSource r) {
        if (r.nextInt(100) >= chance(s)) return null;
        TokenGrade g = Gear.gradeOf(s);
        TokenGrade next = g == null ? TokenGrade.BRONZE : TokenGrade.values()[g.ordinal() + 1];
        ItemStack out = Gear.make(s.getItem(), next, r);
        out.setDamageValue(s.getDamageValue());
        EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(s), out);
        return out;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stone = player.getItemInHand(hand);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gear = player.getItemInHand(other);
        if (!refinable(gear)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("realmlord.refine.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stone);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stone);
        ItemStack out = refine(gear, player.getRandom());
        if (out != null) {
            player.setItemInHand(other, out);
            level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("realmlord.refine.done", out.getHoverName()).withStyle(ChatFormatting.GOLD), false);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.4F);
            player.displayClientMessage(Component.translatable("realmlord.refine.failed").withStyle(ChatFormatting.RED), false);
        }
        if (!player.getAbilities().instabuild) stone.shrink(1);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stone);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.refine.tip").withStyle(ChatFormatting.GRAY));
    }
}
