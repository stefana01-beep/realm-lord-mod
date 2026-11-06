package com.realmlord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.realmlord.RealmLord;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.PlaceBuildingPacket;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Village;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Placing a building: its outline follows the crosshair, green where it fits and red where it
 * does not. Right-click to place it, sneak and right-click to turn it, left-click to cancel.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientPlacement {
    private ClientPlacement() {}

    public static BuildingType active;
    public static int rotation;

    public static void start(BuildingType type) {
        active = type;
        rotation = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.translatable("realmlord.place.start", Component.translatable(type.key())), true);
    }

    public static void stop() {
        active = null;
    }

    public static int sizeX() {
        return rotation % 2 == 0 ? active.width : active.depth;
    }

    public static int sizeZ() {
        return rotation % 2 == 0 ? active.depth : active.width;
    }

    /** The footprint's lowest corner, centred on the block under the crosshair. */
    public static BlockPos origin() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() == HitResult.Type.MISS) return null;
        BlockPos ground = hit.getBlockPos().above();
        return ground.offset(-sizeX() / 2, 0, -sizeZ() / 2);
    }

    /** A quick check on the client; the server decides for certain. */
    public static boolean fits(BlockPos o) {
        return active != null && fits(active, rotation, o);
    }

    public static boolean fits(BuildingType type, int rot, BlockPos o) {
        CompoundTag v = ClientVillageData.manor;
        if (v == null || o == null) return false;
        int cx = v.getInt("cx"), cz = v.getInt("cz"), r = v.getInt("radius");
        int sx = rot % 2 == 0 ? type.width : type.depth, sz = rot % 2 == 0 ? type.depth : type.width;
        int[][] corners = {{0, 0}, {sx - 1, 0}, {0, sz - 1}, {sx - 1, sz - 1}};
        // Defences may stand out on a wall beyond the border.
        if (Village.defence(type) && v.contains("reach")) r = Math.max(r, v.getInt("reach"));
        if (Village.defence(type) && nearRoad(v, o.getX(), o.getZ())) r = Integer.MAX_VALUE / 4;
        for (int[] c : corners) {
            long dx = o.getX() + c[0] - cx, dz = o.getZ() + c[1] - cz;
            if (dx * dx + dz * dz > (long) r * r) return false;
        }
        int m = Village.MANOR_YARD;
        if (o.getX() - 1 < cx + m && cx - m < o.getX() + sx + 1 && o.getZ() - 1 < cz + m && cz - m < o.getZ() + sz + 1) return false;
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            BuildingType bt = BuildingType.byOrdinal(b.getInt("type"));
            if (bt == null || !bt.placed()) continue;
            int bx = b.getInt("x"), bz = b.getInt("z"), bsx = b.getInt("sx"), bsz = b.getInt("sz");
            int g = Village.defence(type) || Village.defence(bt) ? 1 : Village.BUILDING_GAP;
            if (o.getX() - g < bx + bsx && bx < o.getX() + sx + g && o.getZ() - g < bz + bsz && bz < o.getZ() + sz + g) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (active == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        BlockPos o = origin();
        if (o == null) return;
        boolean ok = fits(o);
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        AABB box = new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + sizeX(), o.getY() + 4, o.getZ() + sizeZ());
        float r = ok ? 0.35F : 1.0F, g = ok ? 1.0F : 0.3F, b = 0.35F;
        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, 1.0F);
        // Door side.
        double fx0, fz0, fx1, fz1;
        switch (rotation & 3) {
            case 1 -> { fx0 = box.maxX; fz0 = box.minZ; fx1 = box.maxX; fz1 = box.maxZ; }
            case 2 -> { fx0 = box.minX; fz0 = box.maxZ; fx1 = box.maxX; fz1 = box.maxZ; }
            case 3 -> { fx0 = box.minX; fz0 = box.minZ; fx1 = box.minX; fz1 = box.maxZ; }
            default -> { fx0 = box.minX; fz0 = box.minZ; fx1 = box.maxX; fz1 = box.minZ; }
        }
        LevelRenderer.renderLineBox(pose, lines, new AABB(Math.min(fx0, fx1) - 0.05, box.minY, Math.min(fz0, fz1) - 0.05,
                Math.max(fx0, fx1) + 0.05, box.minY + 0.1, Math.max(fz0, fz1) + 0.05), 1.0F, 0.85F, 0.3F, 1.0F);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        if (active == null) return;
        Minecraft mc = Minecraft.getInstance();
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack()) {
            stop();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("realmlord.place.cancelled"), true);
            return;
        }
        if (!event.isUseItem() || mc.player == null) return;
        if (mc.player.isShiftKeyDown()) {
            rotation = (rotation + 1) & 3;
            return;
        }
        BlockPos o = origin();
        if (o == null) return;
        ModNetwork.toServer(new PlaceBuildingPacket(active.ordinal(), o, rotation));
        stop();
    }

    /** Near one of the village's paths or roads (an outpost may stand by a road out in the wilds). */
    private static boolean nearRoad(CompoundTag v, int x, int z) {
        ListTag bs = v.getList("buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            if (b.getInt("type") != BuildingType.PATH.ordinal()) continue;
            int[] p = b.getIntArray("pts");
            for (int s = 0; s + 3 < p.length; s += 2) {
                double ax = p[s], az = p[s + 1], dx = p[s + 2] - ax, dz = p[s + 3] - az, l = dx * dx + dz * dz;
                double f = l == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / l));
                if (Math.hypot(x - (ax + f * dx), z - (az + f * dz)) <= Village.ROADSIDE) return true;
            }
        }
        return false;
    }
}
