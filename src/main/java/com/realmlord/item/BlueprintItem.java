package com.realmlord.item;

import com.realmlord.loot.Blueprints;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * A building blueprint. Many of the novel's buildings cannot be raised without one. Right-click
 * it to teach it to your village: the plan is kept for good, and the blueprint is used up.
 */
public class BlueprintItem extends Item {
    public BlueprintItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) return InteractionResultHolder.success(stack);
        String b = Blueprints.buildingOf(stack);
        String err = com.realmlord.village.Village.learnBlueprint(sp, b);
        if (err != null) {
            com.realmlord.Notify.player(sp, Component.translatable(err, Component.translatable("realmlord.building." + b)));
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        com.realmlord.Notify.player(sp, Component.translatable("realmlord.blueprint.learn.done", Component.translatable("realmlord.building." + b)));
        sp.playNotifySound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        com.realmlord.network.ModNetwork.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String b = Blueprints.buildingOf(stack);
        if (b.isEmpty()) return super.getName(stack);
        return Component.translatable("item.realmlord.blueprint.named", Component.translatable("realmlord.building." + b));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String b = Blueprints.buildingOf(stack);
        int grade = Blueprints.gradeOf(b);
        if (grade > 0) tooltip.add(Component.translatable("realmlord.blueprint.tooltip.grade", grade).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.blueprint.tooltip.learn").withStyle(ChatFormatting.GOLD));
    }
}
