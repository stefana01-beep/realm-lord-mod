package com.realmlord.item;

import com.realmlord.build.Construction;
import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.work.TechManuals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A building plan: a ready-made design taught by a structure manual, or one an architect drew.
 * Right-click the ground to see where it will stand and what it needs; crouch and right-click
 * the same spot to build it, in front of you, its entrance facing you.
 */
public class PlanItem extends Item {
    public static final String BUILTIN = "builtin", DRAWN = "design", TECH = "tech", SIZE = "size", AUTHOR = "author";
    private static final String PENDING = "realmlord_plan_pending";

    public PlanItem(Properties p) {
        super(p);
    }

    public static ItemStack builtin(String id, String tech) {
        ItemStack s = new ItemStack(com.realmlord.registry.ModCrafts.BUILDING_PLAN.get());
        s.getOrCreateTag().putString(BUILTIN, id);
        s.getOrCreateTag().putString(TECH, tech);
        Design d = Designs.builtin(id);
        if (d != null) s.getOrCreateTag().putIntArray(SIZE, new int[]{d.w, d.h, d.d});
        return s;
    }

    public static ItemStack drawn(UUID id, Design d, String author) {
        ItemStack s = new ItemStack(com.realmlord.registry.ModCrafts.BUILDING_PLAN.get());
        s.getOrCreateTag().putString(DRAWN, id.toString());
        s.getOrCreateTag().putIntArray(SIZE, new int[]{d.w, d.h, d.d});
        s.getOrCreateTag().putString(AUTHOR, author);
        return s;
    }

    /** The design a plan stands for, or null. */
    public static Design design(ItemStack s, ServerLevel level) {
        if (!s.hasTag()) return null;
        if (s.getTag().contains(BUILTIN)) return Designs.builtin(s.getTag().getString(BUILTIN));
        if (s.getTag().contains(DRAWN)) {
            try {
                return Designs.get(level.getServer()).drawn(UUID.fromString(s.getTag().getString(DRAWN)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        Design design = design(stack, level);
        if (design == null) {
            p.displayClientMessage(Component.translatable("realmlord.build.no_design").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        String tech = stack.getTag().getString(TECH);
        if (!tech.isEmpty() && !TechManuals.knows(p, tech)) {
            p.displayClientMessage(Component.translatable("realmlord.build.need_manual", Component.translatable("realmlord.tech." + tech)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }
        if (Construction.busy(p.getUUID())) {
            p.displayClientMessage(Component.translatable("realmlord.build.busy").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos clicked = ctx.getClickedPos();
        Direction facing = ctx.getHorizontalDirection();
        Design turned = design.rotated(Construction.rotationFor(facing));
        BlockPos origin = Construction.originFor(turned, clicked, facing);
        var data = p.getPersistentData();
        long now = level.getGameTime();
        boolean confirm = p.isShiftKeyDown() && data.getLong(PENDING + "_pos") == clicked.asLong()
                && data.getInt(PENDING + "_facing") == facing.get2DDataValue() && now - data.getLong(PENDING + "_time") < 400;
        if (confirm) {
            data.remove(PENDING + "_pos");
            Construction.start(p, turned, origin, stack.getHoverName());
            return InteractionResult.CONSUME;
        }
        data.putLong(PENDING + "_pos", clicked.asLong());
        data.putInt(PENDING + "_facing", facing.get2DDataValue());
        data.putLong(PENDING + "_time", now);
        outline(level, p, origin, turned.w, turned.h, turned.d);
        Map<Item, Integer> need = Construction.materials(turned);
        Map<Item, Integer> lacking = new java.util.LinkedHashMap<>();
        for (var e : need.entrySet()) {
            int have = p.getInventory().countItem(e.getKey());
            if (have < e.getValue()) lacking.put(e.getKey(), e.getValue() - have);
        }
        p.displayClientMessage(Component.translatable("realmlord.build.preview", stack.getHoverName(), turned.w, turned.h, turned.d, turned.blocks())
                .withStyle(ChatFormatting.GOLD), false);
        p.displayClientMessage(Component.translatable("realmlord.build.needs", Construction.list(need, 10)).withStyle(ChatFormatting.GRAY), false);
        if (!p.getAbilities().instabuild && !lacking.isEmpty())
            p.displayClientMessage(Component.translatable("realmlord.build.lacking", Construction.list(lacking, 8)).withStyle(ChatFormatting.RED), false);
        p.displayClientMessage(Component.translatable("realmlord.build.confirm").withStyle(ChatFormatting.YELLOW), false);
        return InteractionResult.CONSUME;
    }

    /** Sparks along the edges of the box a building will fill. */
    public static void outline(ServerLevel level, ServerPlayer p, BlockPos o, int w, int h, int d) {
        int[][] edges = {{0, 0, 0}, {w, 0, 0}, {0, 0, d}, {w, 0, d}};
        for (int[] c : edges)
            for (int y = 0; y <= h; y += Math.max(1, h / 6))
                level.sendParticles(p, ParticleTypes.END_ROD, true, o.getX() + c[0], o.getY() + y, o.getZ() + c[2], 1, 0, 0, 0, 0);
        for (int x = 0; x <= w; x++)
            for (int z : new int[]{0, d}) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, o.getX() + x, o.getY(), o.getZ() + z, 1, 0, 0, 0, 0);
                level.sendParticles(p, ParticleTypes.END_ROD, true, o.getX() + x, o.getY() + h, o.getZ() + z, 1, 0, 0, 0, 0);
            }
        for (int z = 0; z <= d; z++)
            for (int x : new int[]{0, w}) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, o.getX() + x, o.getY(), o.getZ() + z, 1, 0, 0, 0, 0);
                level.sendParticles(p, ParticleTypes.END_ROD, true, o.getX() + x, o.getY() + h, o.getZ() + z, 1, 0, 0, 0, 0);
            }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(BUILTIN))
            return Component.translatable("item.realmlord.building_plan.named", Component.translatable("realmlord.design." + stack.getTag().getString(BUILTIN)));
        if (stack.hasTag() && stack.getTag().contains(DRAWN)) {
            int[] s = stack.getTag().getIntArray(SIZE);
            return Component.translatable("item.realmlord.building_plan.drawn", s.length == 3 ? s[0] + "×" + s[1] + "×" + s[2] : "?");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.hasTag()) return;
        var t = stack.getTag();
        int[] s = t.getIntArray(SIZE);
        if (s.length == 3) tooltip.add(Component.translatable("realmlord.build.tooltip.size", s[0], s[1], s[2]).withStyle(ChatFormatting.GRAY));
        if (t.contains(BUILTIN)) {
            tooltip.add(Component.translatable("realmlord.design." + t.getString(BUILTIN) + ".desc").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("realmlord.build.tooltip.manual", Component.translatable("realmlord.tech." + t.getString(TECH))).withStyle(ChatFormatting.YELLOW));
        }
        if (t.contains(AUTHOR)) tooltip.add(Component.translatable("realmlord.build.tooltip.author", t.getString(AUTHOR)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("realmlord.build.tooltip.use").withStyle(ChatFormatting.GOLD));
    }
}
