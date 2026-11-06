package com.realmlord.worldgen;

import com.realmlord.RealmLord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Water for the China world's upland rivers and lakes. Minecraft only fills water up to the
 * sea's level, so a river running through the Sichuan Basin or across the Tibetan Plateau
 * would be a dry channel; this lays still water in the bed of every river and lake above y 63.
 */
public class RiverWaterFeature extends Feature<NoneFeatureConfiguration> {
    public RiverWaterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator() instanceof NoiseBasedChunkGenerator g) || !g.generatorSettings().is(RealmLord.id("china")) || !ChinaMap.ready()) return false;
        WorldGenLevel level = ctx.level();
        BlockPos o = ctx.origin();
        int cx = o.getX() & ~15, cz = o.getZ() & ~15;
        boolean any = false;
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++)
            for (int dz = 0; dz < 16; dz++) {
                int x = cx + dx, z = cz + dz;
                if (ChinaMap.inside(x, z) <= 0) continue;
                double d = ChinaMap.sample(ChinaMap.RIVER_DIST, x, z);
                if (d >= 1000) continue;
                double s = ChinaMap.sample(ChinaMap.RIVER_STR, x, z);
                if (d > ChinaMap.halfWidth(s) * 0.85) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top <= 64) continue;
                p.set(x, top - 1, z);
                BlockState bed = level.getBlockState(p);
                if (!bed.getFluidState().isEmpty() || bed.isAir()) continue;
                // The bed becomes water, with the banks on either side holding it in.
                level.setBlock(p, water, 2);
                p.move(0, -1, 0);
                if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                any = true;
            }
        return any;
    }
}
