package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Paths and roads laid across the village from the territory map: a line of points, a width of
 * one to three blocks and a surface. Builders lay them like any other site, a block at a time,
 * with earth beneath where the ground falls away (so a gravel or plank way can cross a ditch or
 * a pond). They run around buildings, never through them.
 */
public final class Paths {
    private Paths() {}

    /** Surfaces: block, wood and stone per block (cost rounds up over the whole path). */
    public enum Surface {
        DIRT("dirt", Blocks.DIRT_PATH, 0, 0),
        GRAVEL("gravel", Blocks.GRAVEL, 0, 0.5),
        PLANKS("planks", Blocks.SPRUCE_PLANKS, 1, 0),
        COBBLE("cobble", Blocks.COBBLESTONE, 0, 1),
        STONE("stone", Blocks.STONE_BRICKS, 0, 2);

        public final String id;
        public final Block block;
        public final double wood, stone;

        Surface(String id, Block block, double wood, double stone) {
            this.id = id;
            this.block = block;
            this.wood = wood;
            this.stone = stone;
        }

        public String key() {
            return "realmlord.path.surface." + id;
        }

        public static Surface byOrdinal(int i) {
            return i >= 0 && i < values().length ? values()[i] : DIRT;
        }
    }

    public static final int MAX_POINTS = 32, MAX_WIDTH = 3;
    /** Work per block laid (five builders lay about two blocks a second). */
    public static final int WORK_PER_BLOCK = 50;

    public static long[] cost(Surface s, int blocks) {
        return new long[]{(long) Math.ceil(s.wood * blocks), (long) Math.ceil(s.stone * blocks)};
    }

    /**
     * The columns a path covers, {x, y, z, needs earth beneath (1/0)}, where y is the first free
     * block above the ground. Leaves out anything beyond the border, the manor hall, and every
     * building's footprint. Null if part of it is not loaded.
     */
    public static List<int[]> columns(ServerLevel level, Territory t, int[] pts, int width) {
        return columns(level, t, pts, width, false);
    }

    /** Bits of a column's fourth value: earth beneath, and a stair's facing (1-4 for N, E, S, W; 0 none). */
    public static final int EARTH = 1;

    public static int stairFacing(int flags) {
        return (flags >> 1) & 7;
    }

    /**
     * As above. With stairs, the path keeps to the lie of the land instead of evening it out:
     * it climbs or drops a block at a time (cutting or banking any steeper bit), and each step up
     * is a stair facing the way it climbs.
     */
    public static List<int[]> columns(ServerLevel level, Territory t, int[] pts, int width, boolean stairs) {
        if (stairs) return stairColumns(level, t, pts, width);
        Set<Long> seen = new HashSet<>();
        List<int[]> out = new ArrayList<>();
        double half = (width - 1) / 2.0;
        for (int s = 0; s + 3 < pts.length; s += 2) {
            double ax = pts[s] + 0.5, az = pts[s + 1] + 0.5, bx = pts[s + 2] + 0.5, bz = pts[s + 3] + 0.5;
            double len = Math.hypot(bx - ax, bz - az);
            if (len < 0.01) continue;
            double dx = (bx - ax) / len, dz = (bz - az) / len, px = -dz, pz = dx;
            int n = (int) Math.ceil(len * 4);
            for (int i = 0; i <= n; i++) {
                double cx = ax + (bx - ax) * i / n, cz = az + (bz - az) * i / n;
                for (double o = -half; o <= half + 1e-6; o += 1) {
                    int x = (int) Math.floor(cx + px * o), z = (int) Math.floor(cz + pz * o);
                    if (!seen.add(key(x, z))) continue;
                    if (!allowed(t, x, z)) continue;
                    if (!level.isLoaded(new BlockPos(x, 64, z))) return null;
                    int y = Village.groundAt(level, x, z);
                    if (fenced(level, x, y, z)) continue;
                    out.add(new int[]{x, y, z, 0});
                }
            }
        }
        // Even out the way: each block takes the middle height of the path around it, so a
        // ditch is bridged and a hump cut down rather than followed.
        java.util.Map<Long, Integer> raw = new java.util.HashMap<>();
        for (int[] c : out) raw.put(key(c[0], c[2]), c[1]);
        for (int[] c : out) {
            List<Integer> hs = new ArrayList<>();
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    Integer h = raw.get(key(c[0] + dx, c[2] + dz));
                    if (h != null) hs.add(h);
                }
            java.util.Collections.sort(hs);
            int y = hs.get(hs.size() / 2);
            BlockState below = level.getBlockState(new BlockPos(c[0], y - 2, c[2]));
            c[1] = y;
            c[3] = below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced() ? 1 : 0;
        }
        return out;
    }

    private static final net.minecraft.core.Direction[] FACINGS = {null, net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST,
            net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};

    private static int facingCode(net.minecraft.core.Direction d) {
        for (int i = 1; i < FACINGS.length; i++) if (FACINGS[i] == d) return i;
        return 0;
    }

    private static List<int[]> stairColumns(ServerLevel level, Territory t, int[] pts, int width) {
        Set<Long> seen = new HashSet<>();
        List<int[]> out = new ArrayList<>();
        int half = (width - 1) / 2, extra = (width - 1) - half;
        Integer prevY = null;
        for (int s = 0; s + 3 < pts.length; s += 2) {
            double ax = pts[s] + 0.5, az = pts[s + 1] + 0.5, bx = pts[s + 2] + 0.5, bz = pts[s + 3] + 0.5;
            double len = Math.hypot(bx - ax, bz - az);
            if (len < 0.01) continue;
            net.minecraft.core.Direction dir = net.minecraft.core.Direction.getNearest(bx - ax, 0, bz - az);
            // The cells along the middle of the way, in order.
            List<int[]> mid = new ArrayList<>();
            int n = (int) Math.ceil(len * 4);
            for (int i = 0; i <= n; i++) {
                int x = (int) Math.floor(ax + (bx - ax) * i / n), z = (int) Math.floor(az + (bz - az) * i / n);
                if (mid.isEmpty() || mid.get(mid.size() - 1)[0] != x || mid.get(mid.size() - 1)[1] != z) mid.add(new int[]{x, z});
            }
            int[] ys = new int[mid.size()];
            for (int i = 0; i < mid.size(); i++) {
                int[] c = mid.get(i);
                if (!level.isLoaded(new BlockPos(c[0], 64, c[1]))) return null;
                int y = Village.groundAt(level, c[0], c[1]);
                // One block up or down at a time: steeper ground is cut back or banked up.
                if (prevY != null) y = Math.max(prevY - 1, Math.min(prevY + 1, y));
                ys[i] = y;
                prevY = y;
            }
            int px = -dir.getStepZ(), pz = dir.getStepX();
            for (int i = 0; i < mid.size(); i++) {
                int up = 0;
                if (i + 1 < mid.size() && ys[i + 1] == ys[i] + 1) up = facingCode(dir);
                else if (i > 0 && ys[i - 1] == ys[i] + 1) up = facingCode(dir.getOpposite());
                for (int o = -half; o <= extra; o++) {
                    int x = mid.get(i)[0] + px * o, z = mid.get(i)[1] + pz * o, y = ys[i];
                    if (!seen.add(key(x, z)) || !allowed(t, x, z)) continue;
                    if (!level.isLoaded(new BlockPos(x, 64, z))) return null;
                    if (fenced(level, x, y, z)) continue;
                    BlockState below = level.getBlockState(new BlockPos(x, y - 2, z));
                    int earth = below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced() ? EARTH : 0;
                    out.add(new int[]{x, y, z, earth | (up << 1)});
                }
            }
        }
        return out;
    }

    /** The stair block for a surface. */
    public static Block stairOf(Surface s) {
        return switch (s) {
            case PLANKS -> Blocks.SPRUCE_STAIRS;
            case STONE -> Blocks.STONE_BRICK_STAIRS;
            default -> Blocks.COBBLESTONE_STAIRS;
        };
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Part of the palisade (a post, rail or gate): paths stop short of it. */
    private static boolean fenced(ServerLevel level, int x, int y, int z) {
        for (int k = -4; k <= 1; k++) {
            Block b = level.getBlockState(new BlockPos(x, y + k, z)).getBlock();
            if (b instanceof net.minecraft.world.level.block.FenceBlock || b instanceof net.minecraft.world.level.block.FenceGateBlock) return true;
        }
        return false;
    }

    /** Takes a path up again: its surface goes back to grass, and three quarters of the cost of what was never laid returns. */
    public static void remove(ServerLevel level, Territory t, Building b) {
        Surface s = Surface.byOrdinal(b.pathSurface);
        int n = blocks(b);
        double laid = b.complete ? 1.0 : Math.min(1.0, b.placed / (double) Math.max(1, n * 2));
        long[] c = cost(s, n);
        t.wood += Math.round(c[0] * Sites.SALVAGE * (1 - laid));
        t.stone += Math.round(c[1] * Sites.SALVAGE * (1 - laid));
        if (b.pathCols != null) for (int i = 0; i + 3 < b.pathCols.length; i += 4) {
            BlockPos p = new BlockPos(b.pathCols[i], b.pathCols[i + 1] - 1, b.pathCols[i + 2]);
            if (level.isLoaded(p) && level.getBlockState(p).is(s.block)) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (com.realmlord.village.Resident r : t.residents) if (r.workplace == b.id) r.workplace = -1;
        t.buildings.remove(b);
        BuildingDesigns.forget(b);
    }

    private static boolean allowed(Territory t, int x, int z) {
        long dx = x - t.center.getX(), dz = z - t.center.getZ();
        // Out to the edge of the levelled grounds, past the wall and even past the border.
        long reach = Village.reach(t);
        if (dx * dx + dz * dz > reach * reach) return false;
        if (Math.abs(dx) <= 5 && Math.abs(dz) <= 5) return false;
        for (Building b : t.buildings) if (b.footprint() && b.overlaps(x, z, 1, 1, 0)) return false;
        return true;
    }

    /** Lays out a path: checks it, takes its cost from the stores and opens it as a site. Returns an error key or null. */
    public static String start(ServerPlayer p, Territory t, Surface surface, int width, int[] pts) {
        return start(p, t, surface, width, pts, false);
    }

    public static String start(ServerPlayer p, Territory t, Surface surface, int width, int[] pts, boolean stairs) {
        if (pts.length < 4 || pts.length % 2 != 0 || pts.length > MAX_POINTS * 2) return "realmlord.path.err.points";
        width = Math.max(1, Math.min(MAX_WIDTH, width));
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(t.dimension)) return "realmlord.build.err.dimension";
        List<int[]> cols = columns(level, t, pts, width, stairs);
        if (cols == null) return "realmlord.path.err.unloaded";
        if (cols.isEmpty()) return "realmlord.path.err.empty";
        long[] c = cost(surface, cols.size());
        if (t.wood < c[0] || t.stone < c[1]) return "realmlord.path.err.cost";
        t.wood -= c[0];
        t.stone -= c[1];
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = BuildingType.PATH;
        b.origin = t.center;
        b.stage = Building.BUILD;
        b.workNeeded = Math.max(1, (long) cols.size() * WORK_PER_BLOCK);
        b.lastWork = level.getGameTime();
        b.pathPoints = pts.clone();
        b.pathSurface = surface.ordinal();
        b.pathWidth = width;
        int[] flat = new int[cols.size() * 4];
        for (int i = 0; i < cols.size(); i++) System.arraycopy(cols.get(i), 0, flat, i * 4, 4);
        b.pathCols = flat;
        t.buildings.add(b);
        return null;
    }

    /** The blocks of a path: earth beneath where needed, the surface, and the plants on it cleared. */
    public static List<BuildingDesigns.Placement> design(Building b) {
        List<BuildingDesigns.Placement> out = new ArrayList<>();
        if (b.pathCols == null) return out;
        Surface surface = Surface.byOrdinal(b.pathSurface);
        BlockState top = surface.block.defaultBlockState();
        for (int i = 0; i + 3 < b.pathCols.length; i += 4) {
            int x = b.pathCols[i], y = b.pathCols[i + 1], z = b.pathCols[i + 2], flags = b.pathCols[i + 3];
            if ((flags & EARTH) != 0) out.add(new BuildingDesigns.Placement(new BlockPos(x, y - 2, z), Blocks.DIRT.defaultBlockState()));
            out.add(new BuildingDesigns.Placement(new BlockPos(x, y - 1, z), top));
            int f = stairFacing(flags);
            if (f > 0) {
                // A step up, then headroom over it.
                out.add(new BuildingDesigns.Placement(new BlockPos(x, y, z), stairOf(surface).defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, FACINGS[f])));
                out.add(new BuildingDesigns.Placement(new BlockPos(x, y + 1, z), Blocks.AIR.defaultBlockState()));
                out.add(new BuildingDesigns.Placement(new BlockPos(x, y + 2, z), Blocks.AIR.defaultBlockState()));
            } else {
                out.add(new BuildingDesigns.Placement(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState()));
                out.add(new BuildingDesigns.Placement(new BlockPos(x, y + 1, z), Blocks.AIR.defaultBlockState()));
            }
        }
        return out;
    }

    public static int blocks(Building b) {
        return b.pathCols == null ? 0 : b.pathCols.length / 4;
    }
}
