package com.realmlord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.realmlord.ship.ShipKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/**
 * Draws a ship: the boat's hull at the ship's size, painted with a wood, and what stands on its
 * deck (a mast and sail, a rawhide-covered deckhouse, or the turreted ship's tower).
 */
public class ShipRenderer<T extends Boat> extends EntityRenderer<T> {
    private final ShipKind kind;
    private final ListModel<Boat> model;
    private final ResourceLocation texture;

    public ShipRenderer(EntityRendererProvider.Context ctx, ShipKind kind) {
        super(ctx);
        this.kind = kind;
        this.shadowRadius = 0.8F * kind.scale;
        Boat.Type wood = Boat.Type.byName(kind.wood);
        this.model = kind.cargo ? new ChestBoatModel(ctx.bakeLayer(ModelLayers.createChestBoatModelName(wood)))
                : new BoatModel(ctx.bakeLayer(ModelLayers.createBoatModelName(wood)));
        this.texture = new ResourceLocation("minecraft", (kind.cargo ? "textures/entity/chest_boat/" : "textures/entity/boat/") + kind.wood + ".png");
    }

    @Override
    public void render(T boat, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        float s = kind.scale;
        pose.translate(0.0F, 0.375F * s, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurt = boat.getHurtTime() - pt;
        float dmg = Math.max(0.0F, boat.getDamage() - pt);
        if (hurt > 0.0F) pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * dmg / 10.0F * boat.getHurtDir()));
        float bubble = boat.getBubbleAngle(pt);
        if (!Mth.equal(bubble, 0.0F)) pose.mulPose(new Quaternionf().setAngleAxis(bubble * ((float) Math.PI / 180F), 1.0F, 0.0F, 1.0F));
        // What stands on deck, drawn in the yawed frame before the hull's own flip.
        deck(pose, buf, light, s);
        pose.scale(-s, -s, s);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        model.setupAnim(boat, pt, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vc = buf.getBuffer(model.renderType(texture));
        model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        if (!boat.isUnderWater() && model instanceof WaterPatchModel wp)
            wp.waterPatch().render(pose, buf.getBuffer(RenderType.waterMask()), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(boat, yaw, pt, pose, buf, light);
    }

    private void block(PoseStack pose, MultiBufferSource buf, int light, BlockState state, float x, float y, float z, float sx, float sy, float sz) {
        pose.pushPose();
        pose.translate(x - sx / 2, y, z - sz / 2);
        pose.scale(sx, sy, sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buf, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void deck(PoseStack pose, MultiBufferSource buf, int light, float s) {
        float deck = 0.05F * s;
        switch (kind) {
            case FISHING_BOAT -> block(pose, buf, light, Blocks.BARREL.defaultBlockState(), 0, deck - 0.2F, 0.45F, 0.3F, 0.3F, 0.3F);
            case CIMA_BOAT -> {
                block(pose, buf, light, Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState(), 0, deck - 0.2F, 0, 0.08F, 1.6F, 0.08F);
                block(pose, buf, light, Blocks.RED_WOOL.defaultBlockState(), 0, deck + 0.3F, 0, 0.9F, 1.0F, 0.04F);
            }
            case MERCHANT_SHIP -> {
                block(pose, buf, light, Blocks.SPRUCE_LOG.defaultBlockState(), 0, deck - 0.3F, -0.2F, 0.1F, 2.6F, 0.1F);
                block(pose, buf, light, Blocks.WHITE_WOOL.defaultBlockState(), 0, deck + 0.6F, -0.2F, 1.4F, 1.6F, 0.05F);
                block(pose, buf, light, Blocks.BARREL.defaultBlockState(), 0.35F, deck - 0.25F, 0.5F, 0.35F, 0.35F, 0.35F);
            }
            case MENGCHONG -> {
                block(pose, buf, light, Blocks.BROWN_TERRACOTTA.defaultBlockState(), 0, deck - 0.2F, 0, 1.1F, 0.55F, 1.1F);
                block(pose, buf, light, Blocks.IRON_BARS.defaultBlockState(), 0, deck + 0.35F, 0, 1.1F, 0.25F, 1.1F);
            }
            case TURRETED_SHIP -> {
                block(pose, buf, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0, deck - 0.3F, 0, 1.8F, 0.9F, 1.8F);
                block(pose, buf, light, Blocks.RED_TERRACOTTA.defaultBlockState(), 0, deck + 0.6F, 0, 1.4F, 0.8F, 1.4F);
                block(pose, buf, light, Blocks.DEEPSLATE_TILES.defaultBlockState(), 0, deck + 1.4F, 0, 1.7F, 0.2F, 1.7F);
                block(pose, buf, light, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 0, deck + 1.6F, 0, 1.0F, 0.6F, 1.0F);
                block(pose, buf, light, Blocks.DEEPSLATE_TILES.defaultBlockState(), 0, deck + 2.2F, 0, 1.3F, 0.15F, 1.3F);
                block(pose, buf, light, Blocks.RED_WOOL.defaultBlockState(), 0, deck + 2.35F, 0, 0.06F, 0.9F, 0.5F);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T boat) {
        return texture;
    }
}
