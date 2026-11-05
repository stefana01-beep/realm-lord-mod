package com.realmlord.item;

import com.realmlord.Notify;
import com.realmlord.network.ModNetwork;
import com.realmlord.profile.Profiles;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
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
 * The Resources Bag from the Village Creation quest, as in the novel: the four basic
 * resources (food, wood, stone and iron ore) for a new territory. A Basic bag holds 1,000
 * food and 100 of each of the rest; an Intermediate bag five times as much. Using it empties
 * it into your territory's stores.
 */
public class ResourcesBagItem extends Item {
    public static final String TAG = "realmlord_bag_tier";
    public static final String[] TIERS = {"basic", "intermediate", "advanced"};

    public ResourcesBagItem(Properties props) {
        super(props);
    }

    public static ItemStack of(int tier) {
        ItemStack s = new ItemStack(ModItems.RESOURCES_BAG.get());
        s.getOrCreateTag().putInt(TAG, Math.max(0, Math.min(tier, TIERS.length - 1)));
        return s;
    }

    public static int tier(ItemStack s) {
        return s.hasTag() ? Math.max(0, Math.min(s.getTag().getInt(TAG), TIERS.length - 1)) : 0;
    }

    /** Basic x1, Intermediate x5, Advanced x25. */
    public static int multiplier(int tier) {
        return tier <= 0 ? 1 : tier == 1 ? 5 : 25;
    }

    public static long food(int tier) {
        return 1000L * multiplier(tier);
    }

    public static long other(int tier) {
        return 100L * multiplier(tier);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        int id = Profiles.get(sp).getTerritoryId();
        TerritoryData data = TerritoryData.get(sp.server);
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null) {
            Notify.player(sp, "realmlord.bag.no_territory");
            return InteractionResultHolder.fail(stack);
        }
        int tier = tier(stack);
        t.food += food(tier);
        t.wood += other(tier);
        t.stone += other(tier);
        t.ironOre += other(tier);
        data.touch();
        Notify.player(sp, Component.translatable("realmlord.bag.opened", Component.translatable("realmlord.bag.tier." + TIERS[tier]),
                food(tier), other(tier), other(tier), other(tier), t.name));
        level.playSound(null, sp.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        Profiles.get(sp).markDirty();
        ModNetwork.sync(sp);
        return InteractionResultHolder.consume(stack);
    }

    /** Puts items in a player's inventory in proper stacks, dropping what does not fit. */
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
        return Component.translatable("item.realmlord.resources_bag.named", Component.translatable("realmlord.bag.tier." + TIERS[tier(stack)]));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int t = tier(stack);
        tooltip.add(Component.translatable("realmlord.bag.tooltip", food(t), other(t), other(t), other(t)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.bag.tooltip.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
