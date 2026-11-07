package com.realmlord.item;

import com.realmlord.loot.Manuals;
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

/** A forging or alchemy manual: right-click to teach it to the village's masters. */
public class ManualItem extends Item {
    public ManualItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        Manuals.Plan plan = Manuals.planOf(stack);
        if (plan == null) return InteractionResultHolder.fail(stack);
        String err = com.realmlord.village.Village.learnPlan(sp, plan);
        if (err != null) {
            com.realmlord.Notify.player(sp, Component.translatable(err, Component.translatable(plan.key())));
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        com.realmlord.Notify.player(sp, Component.translatable("realmlord.plan.learned", Component.translatable(plan.key())));
        sp.playNotifySound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        com.realmlord.network.ModNetwork.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Manuals.Plan p = Manuals.planOf(stack);
        return p == null ? super.getName(stack) : Component.translatable("item.realmlord.manual.named", Component.translatable(p.key()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Manuals.Plan p = Manuals.planOf(stack);
        if (p != null) tooltip.add(Component.translatable(p.key() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.plan.tooltip").withStyle(ChatFormatting.GOLD));
    }
}
