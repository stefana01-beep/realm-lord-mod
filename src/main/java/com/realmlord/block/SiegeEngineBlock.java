package com.realmlord.block;

import com.realmlord.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The siege engines of the novel's walls: the bed crossbow (a great crossbow on a frame, whose
 * bolts went through a horse and rider) and the counterweight trebuchet. Placed facing out from
 * the wall, loaded with their ammunition, they shoot at raiders and beasts in front of them on
 * their own inside a territory; crouch and right-click to shoot where you look.
 */
public class SiegeEngineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Kind { BED_CROSSBOW, TREBUCHET }

    public final Kind kind;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public SiegeEngineBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiegeEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlocks.SIEGE_ENGINE.get()) return null;
        return (l, p, s, be) -> ((SiegeEngineBlockEntity) be).serverTick();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SiegeEngineBlockEntity be) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (be.ammoItem() == held.getItem()) {
            int took = be.load(held.getCount());
            if (!player.getAbilities().instabuild) held.shrink(took);
            sp.displayClientMessage(Component.translatable("realmlord.siege.loaded", be.ammo(), be.capacity()), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            String err = be.fireWhereLooking(sp);
            if (err != null) sp.displayClientMessage(Component.translatable(err), true);
            return InteractionResult.CONSUME;
        }
        sp.displayClientMessage(Component.translatable("realmlord.siege.status", Component.translatable(getDescriptionId()), be.ammo(), be.capacity(),
                Component.translatable(be.ammoItem().getDescriptionId())), true);
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof SiegeEngineBlockEntity be && be.ammo() > 0) {
            Block.popResource(level, pos, new ItemStack(be.ammoItem(), be.ammo()));
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.realmlord." + (kind == Kind.BED_CROSSBOW ? "bed_crossbow" : "trebuchet") + ".desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
