package com.realmlord.block;

import com.realmlord.Notify;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.OpenScreenPacket;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The manor seal at the heart of a territory. Use it to open the territory panel. */
public class LordsManorBlock extends Block {
    public LordsManorBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            Territory t = TerritoryData.get(sl.getServer()).at(sl.dimension(), pos);
            if (t == null) {
                Notify.player(sp, "realmlord.manor.unclaimed");
            } else if (!sp.getUUID().equals(t.owner)) {
                Notify.player(sp, Component.translatable("realmlord.manor.visitor", t.name, t.ownerName));
            } else {
                ModNetwork.sync(sp);
                ModNetwork.send(sp, new OpenScreenPacket(OpenScreenPacket.TERRITORY));
            }
        }
        return InteractionResult.CONSUME;
    }
}
