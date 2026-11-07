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
        return draw(level, t, rangeRadius(t, 0));
    }

    /** How far the map reaches: the village and its grounds, the country around (512), or the region (1,536) for roads. */
    public static int rangeRadius(Territory t, int range) {
        return range >= 2 ? 1536 : range == 1 ? 512 : Math.max(t.radius(), Grounds.radius()) + 8;
    }

    public static Map draw(ServerLevel level, Territory t, int r) {
        int step = stepFor(r);
        int size = (2 * r) / step + 1;
        int x0 = t.center.getX() - r, z0 = t.center.getZ() - r;
        int[] rgb = new int[size * size];
        int[] heights = new int[size];
        TerrainCache cache = TerrainCache.get(level);
        for (int pz = 0; pz < size; pz++) {
            for (int px = 0; px < size; px++) {
                int x = x0 + px * step, z = z0 + pz * step;
                int i = pz * size + px;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    // Not loaded now: the ground as it was last seen (filled in below if never).
                    rgb[i] = cache.colour(x, z);
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
        sketch(level, x0, z0, step, size, rgb);
        return new Map(x0, z0, step, size, rgb);
    }

    /** Land never seen, sketched from the world generator in cells of a few pixels. */
    private static void sketch(ServerLevel level, int x0, int z0, int step, int size, int[] rgb) {
        int cell = 4;
        int cols = (size + cell - 1) / cell;
        int[] north = new int[cols];
        java.util.Arrays.fill(north, Integer.MIN_VALUE);
        int[] y = new int[1];
        for (int cz = 0; cz < size; cz += cell) {
            for (int cx = 0; cx < size; cx += cell) {
                boolean blank = false;
                for (int dz = 0; dz < cell && cz + dz < size && !blank; dz++)
                    for (int dx = 0; dx < cell && cx + dx < size; dx++)
                        if (rgb[(cz + dz) * size + cx + dx] == 0) { blank = true; break; }
                if (!blank) {
                    north[cx / cell] = Integer.MIN_VALUE;
                    continue;
                }
                int wx = x0 + (cx + cell / 2) * step, wz = z0 + (cz + cell / 2) * step;
                int c = TerrainCache.sketch(level, wx, wz, north[cx / cell], y);
                north[cx / cell] = y[0];
                for (int dz = 0; dz < cell && cz + dz < size; dz++)
                    for (int dx = 0; dx < cell && cx + dx < size; dx++) {
                        int i = (cz + dz) * size + cx + dx;
                        if (rgb[i] == 0) rgb[i] = c;
                    }
            }
        }
    }

    static int scale(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * f);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * f);
        int b = (int) Math.min(255, (rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }
}
