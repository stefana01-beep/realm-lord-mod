package com.realmlord.item;

import com.realmlord.territory.Founding;
import com.realmlord.territory.TokenGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Use on the ground to found a village there. Rename it in an anvil to name the village. */
public class VillageTokenItem extends Item {
    public final TokenGrade grade;

    public VillageTokenItem(TokenGrade grade, Properties props) {
        super(props);
        this.grade = grade;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        ItemStack stack = ctx.getItemInHand();
        String name = stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
        if (Founding.found(player, ctx.getClickedPos().above(), grade, name) != null) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return grade == TokenGrade.GOLD || grade == TokenGrade.SILVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.token.tooltip." + grade.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.token.tooltip.use").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("realmlord.token.tooltip.name").withStyle(ChatFormatting.DARK_GRAY));
    }
}
