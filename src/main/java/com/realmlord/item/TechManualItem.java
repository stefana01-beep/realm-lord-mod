package com.realmlord.item;

import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.work.TechData.Tech;
import com.realmlord.work.TechManuals;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A technical manual: "After use, gain knowledge of crafting ...". Right-click to learn it. */
public class TechManualItem extends Item {
    public TechManualItem(Properties p) {
        super(p);
    }

    /** Teaches a player a manual; returns null, or the message key of why not. */
    public static String learn(ServerPlayer sp, Tech t) {
        Profile prof = Profiles.get(sp);
        String err = TechManuals.cannotLearn(prof, t);
        if (err != null) return err;
        prof.learnTech(t.id());
        Profiles.applyAttributes(sp);
        Profiles.sync(sp);
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Tech t = TechManuals.of(stack);
        if (t == null) return InteractionResultHolder.fail(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        String err = learn(sp, t);
        if (err != null) {
            sp.displayClientMessage(Component.translatable(err, err.equals("realmlord.tech.need_trade")
                    ? new Object[]{Component.translatable(t.trade().key()), Component.translatable("realmlord.tech.tier." + t.tier())}
                    : new Object[]{Component.translatable(t.key())}).withStyle(ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        sp.displayClientMessage(Component.translatable("realmlord.tech.learned", Component.translatable(t.key())).withStyle(ChatFormatting.GOLD), false);
        sp.playNotifySound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Tech t = TechManuals.of(stack);
        return t == null ? super.getName(stack) : Component.translatable("item.realmlord.tech_manual.named", Component.translatable(t.key()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Tech t = TechManuals.of(stack);
        if (t == null) return;
        tooltip.add(Component.translatable(TechManuals.open(t) ? "realmlord.tech.tooltip.anyone" : "realmlord.tech.tooltip.trade",
                Component.translatable(t.trade().key()), Component.translatable("realmlord.tech.tier." + t.tier())).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(t.key() + ".desc").withStyle(ChatFormatting.GRAY));
        if (t.outputs().length > 0) {
            MutableComponent list = Component.empty();
            for (int i = 0; i < t.outputs().length; i++) {
                if (i > 0) list.append(", ");
                Item it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(t.outputs()[i]));
                list.append(it == null ? Component.literal(t.outputs()[i]) : it.getDescription());
            }
            tooltip.add(Component.translatable("realmlord.tech.tooltip.teaches", list).withStyle(ChatFormatting.AQUA));
        }
        if (t.passive()) tooltip.add(Component.translatable("realmlord.tech.tooltip.passive").withStyle(ChatFormatting.AQUA));
        if (t.novel()) tooltip.add(Component.translatable("realmlord.tech.tooltip.novel").withStyle(ChatFormatting.DARK_GRAY));
        if (TechManuals.CLIENT_KNOWN.contains(t.id())) tooltip.add(Component.translatable("realmlord.tech.tooltip.known").withStyle(ChatFormatting.GREEN));
        else tooltip.add(Component.translatable("realmlord.tech.tooltip.use").withStyle(ChatFormatting.GOLD));
    }
}
