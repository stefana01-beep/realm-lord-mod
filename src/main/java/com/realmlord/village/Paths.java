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

    public static final int MAX_POINTS = 32, MAX_WIDTH = 7;
    /** How far from the manor a road may run. */
    public static final int MAX_ROAD = 2048;
    /** Every territory, for keeping roads out of other Lords' lands (set by the server). */
    public static java.util.function.Supplier<java.util.Collection<Territory>> ALL = java.util.List::of;
    /** Largest paved area, a side. */
    public static final int MAX_AREA = 48;

    /**
     * A paved area (a square, a market place, a yard) between two corners: every block of it laid
     * with the surface, the ground evened out the way a path's is.
     */
    public static List<int[]> areaColumns(ServerLevel level, Territory t, int x0, int z0, int x1, int z1) {
        List<int[]> out = new ArrayList<>();
        int ax = Math.min(x0, x1), az = Math.min(z0, z1), bx = Math.max(x0, x1), bz = Math.max(z0, z1);
        if (bx - ax + 1 > MAX_AREA || bz - az + 1 > MAX_AREA) return out;
        for (int x = ax; x <= bx; x++)
            for (int z = az; z <= bz; z++) {
                if (!allowed(t, x, z)) continue;
                int y = height(level, x, z);
                if (level.isLoaded(new BlockPos(x, 64, z)) && fenced(level, x, y, z)) continue;
                out.add(new int[]{x, y, z, 0});
            }
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
            c[1] = y;
            c[3] = needsEarth(level, c[0], y, c[2]) ? EARTH : 0;
        }
        return out;
    }
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
                    int y = height(level, x, z);
                    if (level.isLoaded(new BlockPos(x, 64, z)) && fenced(level, x, y, z)) continue;
                    out.add(new int[]{x, y, z, 0});
                }
            }
        }
        // Even out the way: each block takes the middle height of the path around it, so a
        // ditch is bridged and a hump cut down rather than followed.
        java.util.Map<Long, Integer> raw = new java.util.HashMap<>();
        for (int[] c : out) raw.put(key(c[0], c[2]), c[1]);
        for (int[] c : out) {
            int depth = waterDepth(level, c[0], raw.get(key(c[0], c[2])), c[2]);
            if (depth >= 2) {
                // Over a river or a lake: a bridge, its deck just above the water.
                c[1] = raw.get(key(c[0], c[2]));
                c[3] = BRIDGE | (Math.min(255, depth) << 5);
                continue;
            }
            List<Integer> hs = new ArrayList<>();
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    Integer h = raw.get(key(c[0] + dx, c[2] + dz));
                    if (h != null) hs.add(h);
                }
            java.util.Collections.sort(hs);
            int y = hs.get(hs.size() / 2);
            c[1] = y;
            c[3] = needsEarth(level, c[0], y, c[2]) ? EARTH : 0;
        }
        return out;
    }

    /** A bridge column, and how deep the water under it is (bits 5 and up). */
    public static final int BRIDGE = 16;

    public static int bridgeDepth(int flags) {
        return (flags >> 5) & 255;
    }

    /** The first free block above the ground: from the world where it is loaded, else from the land's shape as generated. */
    public static int height(ServerLevel level, int x, int z) {
        if (level.isLoaded(new BlockPos(x, 64, z))) return Village.groundAt(level, x, z);
        return level.getChunkSource().getGenerator().getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                level, level.getChunkSource().randomState());
    }

    /** Water below a column's first free block (0 on dry land). */
    public static int waterDepth(ServerLevel level, int x, int y, int z) {
        if (level.isLoaded(new BlockPos(x, 64, z))) {
            int d = 0;
            while (d < 64 && level.getFluidState(new BlockPos(x, y - 1 - d, z)).is(net.minecraft.tags.FluidTags.WATER)) d++;
            return d;
        }
        var gen = level.getChunkSource().getGenerator();
        int floor = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
        return Math.max(0, y - floor);
    }

    private static boolean needsEarth(ServerLevel level, int x, int y, int z) {
        if (!level.isLoaded(new BlockPos(x, 64, z))) return false;
        BlockState below = level.getBlockState(new BlockPos(x, y - 2, z));
        return below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced();
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
                int y = height(level, c[0], c[1]);
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
                    if (level.isLoaded(new BlockPos(x, 64, z)) && fenced(level, x, y, z)) continue;
                    int depth = waterDepth(level, x, y, z);
                    if (depth >= 2) {
                        out.add(new int[]{x, y, z, BRIDGE | (Math.min(255, depth) << 5)});
                        continue;
                    }
                    out.add(new int[]{x, y, z, (needsEarth(level, x, y, z) ? EARTH : 0) | (up << 1)});
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

    public static boolean allowed(Territory t, int x, int z) {
        long dx = x - t.center.getX(), dz = z - t.center.getZ();
        // Within the village's grounds (past the wall, even past the border), and beyond as a road
        // into the wilds, so long as it keeps out of other Lords' lands.
        if (dx * dx + dz * dz > (long) MAX_ROAD * MAX_ROAD) return false;
        if (dx * dx + dz * dz > (long) Village.reach(t) * Village.reach(t)) {
            for (Territory o : ALL.get()) if (o != t && o.dimension.equals(t.dimension) && o.distanceTo(new BlockPos(x, 64, z)) <= o.radius()) return false;
            return true;
        }
        if (Math.abs(dx) <= 5 && Math.abs(dz) <= 5) return false;
        for (Building b : t.buildings) if (b.footprint() && b.overlaps(x, z, 1, 1, 0)) return false;
        return !onWall(t, x, z);
    }

    /** True if a column belongs to a building's footprint or to a wall (outside its gates). */
    public static boolean guarded(Territory t, Building path, int x, int z) {
        for (Building o : t.buildings) {
            if (o == path || o.demolishing || !o.type.placed()) continue;
            if (o.overlaps(x, z, 1, 1, 0)) return true;
        }
        return onWall(t, x, z);
    }

    /**
     * True if a column is part of a wall (its rails and posts, the walkway behind, and a city
     * wall's moat in front) other than at a gate: a path goes through the gates, never through the
     * wall itself, so the builders never cut a breach they then leave open.
     */
    public static boolean onWall(Territory t, int x, int z) {
        double dist = Math.hypot(x + 0.5 - t.center.getX(), z + 0.5 - t.center.getZ());
        for (Building f : t.buildings) {
            if (f.type.style != BuildingType.Style.FENCE || f.demolishing) continue;
            int r = f.fenceRadius > 0 ? f.fenceRadius : t.level.wall();
            int outer = f.type == BuildingType.CITY_WALL ? 5 : 1;
            if (dist < r - 3 || dist > r + outer) continue;
            if (!f.isGate(BuildingDesigns.bearing(t.center, x, z))) return true;
        }
        return false;
    }

    /** Lays out a path: checks it, takes its cost from the stores and opens it as a site. Returns an error key or null. */
    public static String start(ServerPlayer p, Territory t, Surface surface, int width, int[] pts) {
        return start(p, t, surface, width, pts, false);
    }

    public static String start(ServerPlayer p, Territory t, Surface surface, int width, int[] pts, boolean stairs) {
        return start(p, t, surface, width, pts, stairs, false);
    }

    /** As above; area: the first two points are opposite corners of a paved area instead. */
    public static String start(ServerPlayer p, Territory t, Surface surface, int width, int[] pts, boolean stairs, boolean area) {
        return start(p.serverLevel(), t, surface, width, pts, stairs, area);
    }

    public static String start(ServerLevel level, Territory t, Surface surface, int width, int[] pts, boolean stairs, boolean area) {
        if (pts.length < 4 || pts.length % 2 != 0 || pts.length > MAX_POINTS * 2) return "realmlord.path.err.points";
        width = Math.max(1, Math.min(MAX_WIDTH, width));
        if (!level.dimension().equals(t.dimension)) return "realmlord.build.err.dimension";
        if (area && (Math.abs(pts[2] - pts[0]) + 1 > MAX_AREA || Math.abs(pts[3] - pts[1]) + 1 > MAX_AREA)) return "realmlord.path.err.area";
        List<int[]> cols = area ? areaColumns(level, t, pts[0], pts[1], pts[2], pts[3]) : columns(level, t, pts, width, stairs);
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
        b.pathArea = area;
        int[] flat = new int[cols.size() * 4];
        for (int i = 0; i < cols.size(); i++) System.arraycopy(cols.get(i), 0, flat, i * 4, 4);
        b.pathCols = flat;
        t.buildings.add(b);
        return null;
    }

    /** The blocks of a path: earth beneath where needed, the surface, and the plants on it cleared. */
    public static List<BuildingDesigns.Placement> design(Territory t, Building b) {
        List<BuildingDesigns.Placement> out = new ArrayList<>();
        if (b.pathCols == null) return out;
        Surface surface = Surface.byOrdinal(b.pathSurface);
        BlockState top = surface.block.defaultBlockState();
        for (int i = 0; i + 3 < b.pathCols.length; i += 4) {
            int x = b.pathCols[i], y = b.pathCols[i + 1], z = b.pathCols[i + 2], flags = b.pathCols[i + 3];
            // A way laid out before a building or a wall went up never cuts through it.
            if (t != null && guarded(t, b, x, z)) continue;
            if ((flags & EARTH) != 0) out.add(new BuildingDesigns.Placement(new BlockPos(x, y - 2, z), Blocks.DIRT.defaultBlockState()));
            out.add(new BuildingDesigns.Placement(new BlockPos(x, y - 1, z), top));
            if ((flags & BRIDGE) != 0) {
                bridge(out, b, surface, x, y, z, flags);
                continue;
            }
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

    /**
     * A bridge column: a deck one block above the water, a rail along each open side, and a
     * pier down to the bed under every fourth block of the rails. Planks and dirt or gravel
     * ways get a timber bridge; cobble and stone ways a stone one.
     */
    private static void bridge(List<BuildingDesigns.Placement> out, Building b, Surface surface, int x, int y, int z, int flags) {
        boolean stone = surface == Surface.COBBLE || surface == Surface.STONE;
        BlockState deck = (surface == Surface.STONE ? Blocks.STONE_BRICKS : stone ? Blocks.COBBLESTONE : Blocks.SPRUCE_PLANKS).defaultBlockState();
        BlockState pier = (stone ? Blocks.STONE_BRICKS : Blocks.STRIPPED_SPRUCE_LOG).defaultBlockState();
        java.util.Map<Long, Integer> cols = colMap(b);
        boolean wide = b.pathArea || b.pathWidth >= 3;
        boolean edge = !cols.containsKey(key(x + 1, z)) || !cols.containsKey(key(x - 1, z)) || !cols.containsKey(key(x, z + 1)) || !cols.containsKey(key(x, z - 1));
        // Rails along the sides of a bridge three or more wide; a narrow one is left open to walk.
        boolean rail = wide && edge;
        if (edge && Math.floorMod(x + z, 4) == 0) {
            for (int d = 1; d <= bridgeDepth(flags); d++) out.add(new BuildingDesigns.Placement(new BlockPos(x, y - d, z), pier));
        }
        out.add(new BuildingDesigns.Placement(new BlockPos(x, y, z), deck));
        out.add(new BuildingDesigns.Placement(new BlockPos(x, y + 1, z), rail ? rail(surface) : Blocks.AIR.defaultBlockState()));
        out.add(new BuildingDesigns.Placement(new BlockPos(x, y + 2, z), Blocks.AIR.defaultBlockState()));
    }

    private static BlockState rail(Surface surface) {
        boolean stone = surface == Surface.COBBLE || surface == Surface.STONE;
        return (surface == Surface.STONE ? Blocks.STONE_BRICK_WALL : stone ? Blocks.COBBLESTONE_WALL : Blocks.SPRUCE_FENCE).defaultBlockState();
    }

    private static final java.util.Map<int[], java.util.Map<Long, Integer>> COLS = new java.util.WeakHashMap<>();

    private static java.util.Map<Long, Integer> colMap(Building b) {
        return COLS.computeIfAbsent(b.pathCols, k -> {
            java.util.Map<Long, Integer> m = new java.util.HashMap<>();
            for (int i = 0; i + 3 < k.length; i += 4) m.put(key(k[i], k[i + 2]), k[i + 3]);
            return m;
        });
    }

    public static int blocks(Building b) {
        return b.pathCols == null ? 0 : b.pathCols.length / 4;
    }
}
