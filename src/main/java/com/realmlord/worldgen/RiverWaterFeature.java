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
                double d = ChinaMap.riverDist(x, z);
                if (d >= 1000) continue;
                double s = ChinaMap.riverStr(x, z);
                if (d > ChinaMap.halfWidth(s) * 0.85) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top <= 64) continue;
                // Water as deep as the banks allow (up to four blocks): the lowest ground a few
                // blocks out on either side holds it in.
                int bank = Integer.MAX_VALUE;
                for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                    int bx = x + o[0], bz = z + o[1];
                    if (ChinaMap.riverDist(bx, bz) <= ChinaMap.halfWidth(s) * 0.85) continue;
                    bank = Math.min(bank, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz));
                }
                int surface = bank == Integer.MAX_VALUE ? top + 1 : Math.min(top + 3, bank - 1);
                surface = Math.max(top, surface);
                for (int y = top - 1; y < surface; y++) {
                    p.set(x, y, z);
                    BlockState here = level.getBlockState(p);
                    if (y == top - 1 && (!here.getFluidState().isEmpty() || here.isAir())) break;
                    if (y >= top && !here.isAir()) continue;
                    level.setBlock(p, water, 2);
                    any = true;
                }
                p.set(x, top - 2, z);
                if (level.getBlockState(p).isAir()) level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
            }
        return any;
    }
}
