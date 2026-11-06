package com.realmlord.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/** The Lord's Handbook: how to found, build and run a village, opened like a book. */
public class GuideItem extends Item {
    public static final int PAGES = 14;

    public GuideItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (level.isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.realmlord.client.ClientAccess::guide);
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
    }

    /** The handbook's pages as a written book, each page a translated text. */
    public static ItemStack book() {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = s.getOrCreateTag();
        tag.putString("title", "Lord's Handbook");
        tag.putString("author", "Realm Lord");
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        for (int i = 1; i <= PAGES; i++) pages.add(StringTag.valueOf("{\"translate\":\"realmlord.guide.page." + i + "\"}"));
        tag.put("pages", pages);
        return s;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.guide.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
