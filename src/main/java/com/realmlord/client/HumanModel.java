package com.realmlord.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The player model for human mobs, with arm poses for held items and drawn bows. */
public class HumanModel<T extends Mob> extends PlayerModel<T> {
    public HumanModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        ItemStack main = entity.getMainHandItem();
        if (main.is(Items.BOW) && entity.isAggressive()) rightArmPose = ArmPose.BOW_AND_ARROW;
        else if (!main.isEmpty()) rightArmPose = ArmPose.ITEM;
        else rightArmPose = ArmPose.EMPTY;
        leftArmPose = entity.getOffhandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }
}
