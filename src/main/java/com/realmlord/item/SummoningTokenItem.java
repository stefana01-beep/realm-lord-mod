package com.realmlord.item;

import com.realmlord.Notify;
import com.realmlord.registry.ModItems;
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
 * The Summoning Token from the Village Creation quest. In the novel it summons a random
 * historical figure of its rank (King, Emperor, Divine or Saint); a perfect quest earns an
 * Emperor-rank token. It becomes usable when historical figures arrive (Phase 3).
 */
public class SummoningTokenItem extends Item {
    public static final String TAG = "realmlord_rank";
    public static final String[] RANKS = {"king", "emperor", "divine", "saint"};

    public SummoningTokenItem(Properties props) {
        super(props);
    }

    public static ItemStack of(int rank) {
        ItemStack s = new ItemStack(ModItems.SUMMONING_TOKEN.get());
        s.getOrCreateTag().putInt(TAG, Math.max(0, Math.min(rank, RANKS.length - 1)));
        return s;
    }

    public static int rank(ItemStack s) {
        return s.hasTag() ? Math.max(0, Math.min(s.getTag().getInt(TAG), RANKS.length - 1)) : 0;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.realmlord.summoning_token.named", Component.translatable("realmlord.rank." + RANKS[rank(stack)]));
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
        tooltip.add(Component.translatable("realmlord.summoning.tooltip", Component.translatable("realmlord.rank." + RANKS[rank(stack)]))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.summoning.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
