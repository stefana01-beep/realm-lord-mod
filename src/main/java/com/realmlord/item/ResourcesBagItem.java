package com.realmlord.item;

import com.realmlord.Notify;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.TokenGrade;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Resources Bag the Village Creation quest rewards, sized by the token's grade: timber,
 * stone, grain and gold to start a village with.
 */
public class ResourcesBagItem extends Item {
    public static final String TAG = "realmlord_bag_grade";

    public ResourcesBagItem(Properties props) {
        super(props);
    }

    public static ItemStack of(TokenGrade g) {
        ItemStack s = new ItemStack(ModItems.RESOURCES_BAG.get());
        s.getOrCreateTag().putInt(TAG, g.ordinal());
        return s;
    }

    public static TokenGrade grade(ItemStack s) {
        return TokenGrade.byId(s.hasTag() ? s.getTag().getInt(TAG) : 0);
    }

    /** How many times the Bronze bag's contents a bag of this grade holds. */
    public static int multiplier(TokenGrade g) {
        return switch (g) {
            case BRONZE -> 1;
            case BLACK_IRON -> 2;
            case SILVER -> 3;
            case GOLD -> 5;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        int m = multiplier(grade(stack));
        give(sp, new ItemStack(Items.OAK_LOG), 32 * m);
        give(sp, new ItemStack(Items.COBBLESTONE), 32 * m);
        give(sp, new ItemStack(Items.WHEAT), 32 * m);
        give(sp, new ItemStack(Items.BREAD), 8 * m);
        give(sp, new ItemStack(ModItems.GOLD_COIN.get()), 20 * m);
        Notify.player(sp, Component.translatable("realmlord.bag.opened", Component.translatable(grade(stack).key())));
        level.playSound(null, sp.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    public static void give(ServerPlayer p, ItemStack template, int count) {
        while (count > 0) {
            ItemStack s = template.copy();
            int n = Math.min(count, s.getMaxStackSize());
            s.setCount(n);
            count -= n;
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.realmlord.resources_bag.named", Component.translatable(grade(stack).key()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int m = multiplier(grade(stack));
        tooltip.add(Component.translatable("realmlord.bag.tooltip", 32 * m, 32 * m, 32 * m, 8 * m, 20 * m).withStyle(ChatFormatting.GRAY));
    }
}
