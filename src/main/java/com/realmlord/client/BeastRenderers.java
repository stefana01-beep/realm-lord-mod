package com.realmlord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.realmlord.RealmLord;
import com.realmlord.entity.beast.Sized;
import com.realmlord.registry.ModEntities;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.client.renderer.entity.OcelotRenderer;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * The beasts are drawn on Minecraft's animal bodies, each with its own coat, and grown to its
 * size: a level 30 bear stands a good deal taller than a level 10 one.
 */
public final class BeastRenderers {
    private BeastRenderers() {}

    private static ResourceLocation tex(String name) {
        return RealmLord.id("textures/entity/" + name + ".png");
    }

    private static void scale(Entity e, PoseStack pose) {
        if (e instanceof Sized s) {
            float k = s.getSize() * s.baseScale();
            pose.scale(k, k, k);
        }
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        boar(event, ModEntities.WILD_BOAR.get(), tex("wild_boar"));
        boar(event, ModEntities.BOAR_KING.get(), tex("boar_king"));
        wolf(event, ModEntities.GREY_WOLF.get(), tex("grey_wolf"));
        wolf(event, ModEntities.WOLF_KING.get(), tex("wolf_king"));
        cat(event, ModEntities.MOUNTAIN_TIGER.get(), tex("tiger"));
        cat(event, ModEntities.LEOPARD.get(), tex("leopard"));
        cat(event, ModEntities.TIGER_KING.get(), tex("white_tiger"));
        bear(event, ModEntities.SNOW_BEAR.get(), tex("snow_bear"));
        bear(event, ModEntities.BLACK_BEAR.get(), tex("black_bear"));
        event.registerEntityRenderer(ModEntities.WILD_OX.get(), ctx -> new CowRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Cow e) {
                return tex("wild_ox");
            }

            @Override
            protected void scale(Cow e, PoseStack pose, float pt) {
                BeastRenderers.scale(e, pose);
            }
        });
    }

    private static void boar(EntityRenderersEvent.RegisterRenderers event, net.minecraft.world.entity.EntityType<? extends Hoglin> type, ResourceLocation t) {
        event.registerEntityRenderer(type, ctx -> new HoglinRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Hoglin e) {
                return t;
            }

            @Override
            protected void scale(Hoglin e, PoseStack pose, float pt) {
                BeastRenderers.scale(e, pose);
            }
        });
    }

    private static void wolf(EntityRenderersEvent.RegisterRenderers event, net.minecraft.world.entity.EntityType<? extends Wolf> type, ResourceLocation t) {
        event.registerEntityRenderer(type, ctx -> new WolfRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Wolf e) {
                return t;
            }

            @Override
            protected void scale(Wolf e, PoseStack pose, float pt) {
                BeastRenderers.scale(e, pose);
            }
        });
    }

    private static void cat(EntityRenderersEvent.RegisterRenderers event, net.minecraft.world.entity.EntityType<? extends Ocelot> type, ResourceLocation t) {
        event.registerEntityRenderer(type, ctx -> new OcelotRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Ocelot e) {
                return t;
            }

            @Override
            protected void scale(Ocelot e, PoseStack pose, float pt) {
                BeastRenderers.scale(e, pose);
            }
        });
    }

    private static void bear(EntityRenderersEvent.RegisterRenderers event, net.minecraft.world.entity.EntityType<? extends PolarBear> type, ResourceLocation t) {
        event.registerEntityRenderer(type, ctx -> new PolarBearRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(PolarBear e) {
                return t;
            }

            @Override
            protected void scale(PolarBear e, PoseStack pose, float pt) {
                BeastRenderers.scale(e, pose);
            }
        });
    }
}
