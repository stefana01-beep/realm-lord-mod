package com.realmlord.item;

import com.realmlord.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * The novel's coins: 1 gold = 100 silver = 10,000 copper. Use a coin to break it into 100
 * of the coin below; sneak and use to change 100 coins into one of the coin above.
 */
public class CoinItem extends Item {
    public static final int RATE = 100;

    /** 0 copper, 1 silver, 2 gold. */
    public final int tier;

    public CoinItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    private static Item ofTier(int t) {
        return switch (t) {
            case 0 -> ModItems.COPPER_COIN.get();
            case 1 -> ModItems.SILVER_COIN.get();
            default -> ModItems.GOLD_COIN.get();
        };
    }

    /** Value of a stack of coins in copper. */
    public static long copperValue(ItemStack s) {
        if (!(s.getItem() instanceof CoinItem c)) return 0;
        long each = c.tier == 0 ? 1 : c.tier == 1 ? RATE : (long) RATE * RATE;
        return each * s.getCount();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        if (sp.isShiftKeyDown()) {
            if (tier >= 2) return InteractionResultHolder.pass(stack);
            if (sp.getInventory().countItem(this) < RATE) {
                sp.displayClientMessage(Component.translatable("realmlord.coin.need", RATE, getDescription()), true);
                return InteractionResultHolder.fail(stack);
            }
            int left = RATE;
            for (int i = 0; i < sp.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (s.is(this)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
            ResourcesBagItem.give(sp, new ItemStack(ofTier(tier + 1)), 1);
        } else {
            if (tier <= 0) return InteractionResultHolder.pass(stack);
            stack.shrink(1);
            ResourcesBagItem.give(sp, new ItemStack(ofTier(tier - 1)), RATE);
        }
        level.playSound(null, sp.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.8F, 1.6F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.coin.rate").withStyle(ChatFormatting.GRAY));
        if (tier > 0) tooltip.add(Component.translatable("realmlord.coin.break").withStyle(ChatFormatting.DARK_GRAY));
        if (tier < 2) tooltip.add(Component.translatable("realmlord.coin.combine").withStyle(ChatFormatting.DARK_GRAY));
    }
}
