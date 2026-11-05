package com.realmlord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/** Draws human mobs exactly as players are drawn, with their own skins. */
public class HumanRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanModel<T>> {
    private final Function<T, ResourceLocation> skin;

    public HumanRenderer(EntityRendererProvider.Context ctx, Function<T, ResourceLocation> skin) {
        super(ctx, new HumanModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.skin = skin;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return skin.apply(entity);
    }

    /** Players are drawn at 15/16 scale. */
    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    private static ResourceLocation tex(String name) {
        return RealmLord.id("textures/entity/" + name + ".png");
    }

    public static ResourceLocation banditSkin(BanditEntity e) {
        return switch (e.getRole()) {
            case CHIEF -> tex("bandit_chief");
            case ARCHER -> tex("bandit_archer_" + Math.floorMod(e.getSkin(), BanditEntity.ARCHER_SKINS));
            default -> tex("bandit_" + Math.floorMod(e.getSkin(), BanditEntity.SKINS));
        };
    }

    public static ResourceLocation settlerSkin(SettlerEntity e) {
        return tex("settler_" + Math.floorMod(e.getSkin(), SettlerEntity.SKINS));
    }
}
