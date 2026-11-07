package com.realmlord.item;

import com.realmlord.build.Construction;
import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.work.TechManuals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Lu Ban's square: the architect's drafting tool. Right-click two corners of something you have
 * built; crouch and right-click (with paper in your pack) to draw it into a building plan you
 * can raise again anywhere. How big a plan you can draw grows with your rank as an architect.
 */
public class DraftingSquareItem extends Item {
    public static final int[] MAX_SIZE = {9, 15, 21, 31};

    public DraftingSquareItem(Properties p) {
        super(p);
    }

    private static int tier(Player p) {
        return TechManuals.tradeTier(p, WorkOccupation.ARCHITECT);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            draw(ctx.getLevel(), player, ctx.getItemInHand());
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        if (tier(p) < 0) {
            p.displayClientMessage(Component.translatable("realmlord.draft.architects_only").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        var tag = ctx.getItemInHand().getOrCreateTag();
        BlockPos pos = ctx.getClickedPos();
        if (!tag.contains("a") || tag.contains("b")) {
            tag.putLong("a", pos.asLong());
            tag.remove("b");
            p.displayClientMessage(Component.translatable("realmlord.draft.first", pos.getX(), pos.getY(), pos.getZ()), true);
        } else {
            tag.putLong("b", pos.asLong());
            BlockPos a = BlockPos.of(tag.getLong("a"));
            int w = Math.abs(a.getX() - pos.getX()) + 1, h = Math.abs(a.getY() - pos.getY()) + 1, d = Math.abs(a.getZ() - pos.getZ()) + 1;
            int max = MAX_SIZE[Math.min(3, tier(p))];
            p.displayClientMessage(Component.translatable("realmlord.draft.second", w, h, d, max)
                    .withStyle(w > max || h > max || d > max ? ChatFormatting.RED : ChatFormatting.GREEN), false);
            BlockPos lo = new BlockPos(Math.min(a.getX(), pos.getX()), Math.min(a.getY(), pos.getY()), Math.min(a.getZ(), pos.getZ()));
            PlanItem.outline(level, p, lo, w, h, d);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        draw(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Draws the marked box into a plan. */
    private static void draw(Level level, Player player, ItemStack stack) {
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return;
        int tier = tier(p);
        if (tier < 0) {
            p.displayClientMessage(Component.translatable("realmlord.draft.architects_only").withStyle(ChatFormatting.RED), true);
            return;
        }
        var tag = stack.getOrCreateTag();
        if (!tag.contains("a") || !tag.contains("b")) {
            p.displayClientMessage(Component.translatable("realmlord.draft.mark_first").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos a = BlockPos.of(tag.getLong("a")), b = BlockPos.of(tag.getLong("b"));
        int max = MAX_SIZE[Math.min(3, tier)];
        if (Math.abs(a.getX() - b.getX()) >= max || Math.abs(a.getY() - b.getY()) >= max || Math.abs(a.getZ() - b.getZ()) >= max) {
            p.displayClientMessage(Component.translatable("realmlord.draft.too_big", max).withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!p.getAbilities().instabuild) {
            if (p.getInventory().countItem(Items.PAPER) < 1) {
                p.displayClientMessage(Component.translatable("realmlord.draft.paper").withStyle(ChatFormatting.RED), true);
                return;
            }
            p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.PAPER), 1, p.inventoryMenu.getCraftSlots());
        }
        Design raw = Design.capture(level, a, b);
        // Stored as if drawn facing south, so it is raised the same way round as it was seen.
        Design d = raw.rotated(Design.inverse(Construction.rotationFor(p.getDirection())));
        var id = Designs.get(p.server).add(d);
        ItemStack plan = PlanItem.drawn(id, d, p.getGameProfile().getName());
        if (!p.getInventory().add(plan)) p.drop(plan, false);
        tag.remove("a");
        tag.remove("b");
        Profiles.grantWorkXp(p, 10 + d.blocks() / 20);
        p.displayClientMessage(Component.translatable("realmlord.draft.drawn", d.w, d.h, d.d, d.blocks()).withStyle(ChatFormatting.GOLD), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("realmlord.draft.tip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("realmlord.draft.sizes", MAX_SIZE[0], MAX_SIZE[1], MAX_SIZE[2], MAX_SIZE[3]).withStyle(ChatFormatting.YELLOW));
    }
}
