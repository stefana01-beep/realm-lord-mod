package com.realmlord.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A siege ladder: set against the side of a wall, it runs straight up it (up to sixteen blocks,
 * while there is wall behind it), so a wall or a fort's palisade can be climbed.
 */
public class SiegeLadderItem extends CraftItem {
    public static final int HEIGHT = 16;

    public SiegeLadderItem(Properties props) {
        super(props, Kind.PLAIN);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face.getAxis().isVertical()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos start = ctx.getClickedPos().relative(face);
        // From the ground up: drop to the foot of the wall first.
        while (start.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(start.below()).canBeReplaced()
                && level.getBlockState(start.below().relative(face.getOpposite())).isFaceSturdy(level, start.below().relative(face.getOpposite()), face)) {
            start = start.below();
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, face);
        int n = 0;
        for (int i = 0; i < HEIGHT; i++) {
            BlockPos p = start.above(i);
            BlockPos wall = p.relative(face.getOpposite());
            if (!level.getBlockState(p).canBeReplaced() || !level.getBlockState(wall).isFaceSturdy(level, wall, face)) break;
            if (ctx.getPlayer() instanceof ServerPlayer sp && !com.realmlord.territory.TerritoryEvents.allowed(sp, p, false)) break;
            level.setBlock(p, ladder, 3);
            n++;
        }
        if (n == 0) {
            if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.translatable("realmlord.siege.ladder_no_wall"), true);
            return InteractionResult.FAIL;
        }
        level.playSound(null, start, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
