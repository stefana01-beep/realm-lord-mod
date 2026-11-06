package com.realmlord.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Three Flower Wine, Shanhai's famous brew in the novel: a warming drink that lends strength for
 * a while. Too much makes the head spin.
 */
public class WineItem extends Item {
    public WineItem(Properties p) {
        super(p);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity who) {
        if (!level.isClientSide) {
            who.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 0));
            who.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 8 * 20, 0));
            MobEffectInstance had = who.getEffect(MobEffects.DAMAGE_BOOST);
            if (had != null && had.getDuration() > 90 * 20 || who.getRandom().nextInt(4) == 0) {
                who.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 8 * 20, 0));
            }
        }
        if (who instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) return bottle;
            if (!p.getInventory().add(bottle)) p.drop(bottle, false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player p, net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, p, hand);
    }
}
