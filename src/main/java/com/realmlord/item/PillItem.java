package com.realmlord.item;

import com.realmlord.profile.Innate;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The healer's and alchemist's work: golden wound powder and rejuvenation pills that heal, and
 * the special pills that, as in the novel, are the only way besides cultivation to raise an
 * innate attribute (each pill one point, up to {@link #INNATE_CAP}).
 */
public class PillItem extends Item {
    public enum Kind { WOUND_POWDER, HEALING, MARROW, ENLIGHTENMENT }

    public static final int INNATE_CAP = 30;

    private final Kind kind;

    public PillItem(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity who) {
        if (!level.isClientSide) {
            switch (kind) {
                case WOUND_POWDER -> {
                    who.heal(4.0F);
                    who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6 * 20, 0));
                }
                case HEALING -> {
                    who.heal(8.0F);
                    who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 12 * 20, 1));
                    who.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 0));
                }
                case MARROW, ENLIGHTENMENT -> {
                    if (who instanceof ServerPlayer sp && !raise(sp, kind == Kind.MARROW ? Innate.BODY : Innate.COMPREHENSION)) return stack;
                    who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 1));
                }
            }
        }
        if (!(who instanceof Player p) || !p.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    /** Raises one innate attribute by a point; false (and the pill kept) if it can rise no further. */
    public static boolean raise(ServerPlayer sp, Innate which) {
        Profile prof = Profiles.get(sp);
        int now = prof.innateBase(which);
        if (now >= INNATE_CAP) {
            com.realmlord.Notify.player(sp, Component.translatable("realmlord.pill.capped", Component.translatable(which.key()), INNATE_CAP));
            return false;
        }
        prof.setInnate(which, now + 1);
        Profiles.applyAttributes(sp);
        Profiles.sync(sp);
        com.realmlord.Notify.player(sp, Component.translatable("realmlord.pill.raised", Component.translatable(which.key()), now + 1));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return kind == Kind.WOUND_POWDER ? 16 : 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, p, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.pill." + kind.name().toLowerCase() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
