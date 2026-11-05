package com.realmlord.item;

import com.realmlord.loot.Blueprints;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A building blueprint. Many of the novel's buildings cannot be raised without one. */
public class BlueprintItem extends Item {
    public BlueprintItem(Properties props) {
        super(props);
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
        tooltip.add(Component.translatable("realmlord.blueprint.tooltip.keep").withStyle(ChatFormatting.DARK_GRAY));
    }
}
