package com.realmlord.item;

import com.realmlord.entity.FireOilEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A barrel of fire oil, as the novel's defenders poured on the beastwave: thrown, it bursts into flame. */
public class FireOilItem extends Item {
    public FireOilItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6F, 0.6F);
        if (!level.isClientSide) {
            FireOilEntity oil = new FireOilEntity(level, p);
            oil.setItem(stack);
            oil.shootFromRotation(p, p.getXRot(), p.getYRot(), -10.0F, 0.9F, 1.0F);
            level.addFreshEntity(oil);
        }
        p.getCooldowns().addCooldown(this, 20);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
