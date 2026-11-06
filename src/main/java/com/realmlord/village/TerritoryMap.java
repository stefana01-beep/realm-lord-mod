package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * A top-down picture of a territory, coloured like a vanilla map and shaded by height. Only
 * loaded ground is drawn; the rest is left blank as unexplored.
 */
public final class TerritoryMap {
    private TerritoryMap() {}

    /** Largest map side in pixels; bigger territories are sampled every few blocks. */
    public static final int MAX_SIZE = 256;

    public record Map(int x0, int z0, int step, int size, int[] rgb) {}

    public static int stepFor(int radius) {
        return Math.max(1, (int) Math.ceil((2 * radius + 1) / (double) MAX_SIZE));
    }

    public static Map draw(ServerLevel level, Territory t) {
        int r = Math.max(t.radius(), Grounds.radius()) + 8;
        int step = stepFor(r);
        int size = (2 * r) / step + 1;
        int x0 = t.center.getX() - r, z0 = t.center.getZ() - r;
        int[] rgb = new int[size * size];
        int[] heights = new int[size];
        for (int pz = 0; pz < size; pz++) {
            for (int px = 0; px < size; px++) {
                int x = x0 + px * step, z = z0 + pz * step;
                int i = pz * size + px;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    rgb[i] = 0;
                    heights[px] = Integer.MIN_VALUE;
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockPos pos = new BlockPos(x, y - 1, z);
                BlockState s = level.getBlockState(pos);
                MapColor mc = s.getMapColor(level, pos);
                int base = mc == MapColor.NONE ? 0x6E6E6E : mc.col;
                double shade;
                if (!s.getFluidState().isEmpty()) {
                    int depth = 0;
                    while (depth < 10 && !level.getBlockState(pos.below(depth + 1)).getFluidState().isEmpty()) depth++;
                    shade = 1.0 - depth * 0.04;
                } else {
                    int north = heights[px];
                    shade = north == Integer.MIN_VALUE || pz == 0 ? 0.92 : y > north ? 1.0 : y < north ? 0.78 : 0.88;
                }
                heights[px] = y;
                rgb[i] = 0xFF000000 | scale(base, shade);
            }
        }
        return new Map(x0, z0, step, size, rgb);
    }

    private static int scale(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * f);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * f);
        int b = (int) Math.min(255, (rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }
}
