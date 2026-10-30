package com.realmlord.item;

import com.realmlord.Notify;
import com.realmlord.profile.ProfileCapability;
import com.realmlord.quest.VillageQuest;
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

/** Dropped by the Bandit Chief. Reading it hints at the camp's hidden cache. */
public class BanditLedgerItem extends Item {
    public BanditLedgerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ProfileCapability.get(sp).ifPresent(p -> {
                VillageQuest q = p.quest();
                if (q.cache != null && !q.cacheFound) {
                    Notify.player(sp, Component.translatable("realmlord.ledger.read", q.cache.getX(), q.cache.getZ()));
                } else if (q.cacheFound) {
                    Notify.player(sp, Component.translatable("realmlord.ledger.done"));
                } else {
                    Notify.player(sp, Component.translatable("realmlord.ledger.cipher"));
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.ledger.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
