package com.realmlord.village;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Levelling the village grounds: once a village is founded, its builders take the whole site
 * down (or build it up) to the level of the Lord's Manor, out to ten blocks past where the
 * Grade 3 wall will stand, whether or not that is yet inside the border. Hills and trees are cut
 * away, hollows, ponds and ravines are filled with the land's own earth (dirt under grass, or
 * sandstone under sand in the desert), and the top is laid with that earth's surface.
 *
 * <p>The work goes outwards ring by ring from the manor. What people have built is left
 * standing: buildings, paths, the manor yard, an already-raised wall, and any column holding
 * blocks that do not occur in nature.
 */
public final class Grounds {
    private Grounds() {}

    /** How far past the Grade 3 wall the ground is levelled. */
    public static final int MARGIN = 10;
    /** How many columns ahead of the finished ring a builder may look for work. */
    private static final int LOOKAHEAD = 4096;

    public static int radius() {
        return TerritoryLevel.villageWall(3) + MARGIN;
    }

    // ------------------------------------------------------------------ the columns, ring by ring

    private static final Map<Integer, int[]> RINGS = new ConcurrentHashMap<>();

    /** Column offsets (dx, dz pairs) within radius r, nearest the centre first. */
    static int[] ring(int r) {
        return RINGS.computeIfAbsent(r, rr -> {
            java.util.List<int[]> cols = new java.util.ArrayList<>();
            for (int x = -rr; x <= rr; x++)
                for (int z = -rr; z <= rr; z++) if (x * x + z * z <= rr * rr) cols.add(new int[]{x, z});
            cols.sort((a, b) -> {
                int d = Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]);
                return d != 0 ? d : Double.compare(Math.atan2(a[1], a[0]), Math.atan2(b[1], b[0]));
            });
            int[] out = new int[cols.size() * 2];
            for (int i = 0; i < cols.size(); i++) {
                out[i * 2] = cols.get(i)[0];
                out[i * 2 + 1] = cols.get(i)[1];
            }
            return out;
        });
    }

    public static int columns(Building b) {
        return ring(b.clearHeight).length / 2;
    }

    // ------------------------------------------------------------------ the job

    /** The village's levelling job, if one is under way. */
    public static Building of(Territory t) {
        for (Building b : t.buildings) if (b.type == BuildingType.GROUNDS && !b.complete) return b;
        return null;
    }

    public static boolean active(Territory t) {
        return of(t) != null;
    }

    /** True if a column lies on the grounds of a levelling under way. */
    public static boolean covers(Territory t, int x, int z) {
        Building b = of(t);
        if (b == null) return false;
        long dx = x - b.origin.getX(), dz = z - b.origin.getZ();
        return dx * dx + dz * dz <= (long) b.clearHeight * b.clearHeight;
    }

    /** Sets the builders to levelling the grounds; returns the job, or null if one is already under way. */
    public static Building start(ServerLevel level, Territory t) {
        if (active(t)) return null;
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = BuildingType.GROUNDS;
        b.origin = t.center;
        b.clearHeight = radius();
        b.stage = Building.CLEAR;
        b.workNeeded = columns(b);
        b.progress = 0;
        b.groundsCursor = 0;
        b.lastWork = level.getGameTime();
        t.buildings.add(b);
        return b;
    }

    public static boolean done(Building b) {
        return b.groundsCursor >= columns(b);
    }

    /** A job meaning "this column can't be judged yet" (its chunk is not loaded). */
    private static final Sites.Op WAIT = new Sites.Op(Sites.Kind.CLEAR, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), -2);

    /** The next block job for a builder: a column a little ahead of the finished ring, so a crew spreads out. */
    public static Sites.Op next(ServerLevel level, Territory t, Building b) {
        int n = columns(b);
        if (b.groundsCursor >= n) return finished(b);
        Set<Long> kept = kept(t, b);
        int skip = level.random.nextInt(Math.min(64, n - b.groundsCursor));
        Sites.Op op = scan(level, t, b, kept, b.groundsCursor + skip, false);
        if (op == null && skip > 0) op = scan(level, t, b, kept, b.groundsCursor, true);
        return op;
    }

    /**
     * The village working its grounds on its own: up to budget blocks cut or laid, ring by ring.
     * Unlike the builders' hand work it makes no din, so a large site does not deafen the Lord.
     */
    public static void sweep(ServerLevel level, Territory t, Building b, int budget) {
        if (b.groundsCursor >= columns(b)) {
            finished(b);
            if (t.buildings.contains(b)) Village.finish(level, t, b);
            return;
        }
        Set<Long> kept = kept(t, b);
        while (budget-- > 0) {
            Sites.Op op = scan(level, t, b, kept, b.groundsCursor, true);
            if (op == null) break;
            apply(level, t, op);
        }
        b.progress = Math.min(b.workNeeded, b.groundsCursor);
        b.lastWork = level.getGameTime();
        if (b.groundsCursor >= columns(b)) {
            finished(b);
            if (t.buildings.contains(b)) Village.finish(level, t, b);
        }
    }

    private static Sites.Op finished(Building b) {
        b.progress = b.workNeeded;
        b.stage = Building.BUILD;
        return null;
    }

    /**
     * Looks for work from column `from` on. Finished columns at the head of the line move the
     * cursor on (when `advance`); a column not yet loaded holds it back but is passed over.
     */
    private static Sites.Op scan(ServerLevel level, Territory t, Building b, Set<Long> kept, int from, boolean advance) {
        int[] ring = ring(b.clearHeight);
        int n = ring.length / 2;
        boolean head = advance && from == b.groundsCursor;
        int cx = b.origin.getX(), cz = b.origin.getZ();
        for (int i = from, end = Math.min(n, from + LOOKAHEAD); i < end; i++) {
            int x = cx + ring[i * 2], z = cz + ring[i * 2 + 1];
            Sites.Op op = kept.contains(Village.colKeyOf(x, z)) ? null : column(level, b.origin.getY(), x, z);
            if (op == WAIT) {
                head = false;
                continue;
            }
            if (op != null) return op;
            if (head) b.groundsCursor = i + 1;
        }
        return null;
    }

    /** The next thing to do in one column, null if it is done, or WAIT if it can't be seen. */
    static Sites.Op column(ServerLevel level, int y0, int x, int z) {
        if (!level.isLoaded(new BlockPos(x, y0, z))) return WAIT;
        // Cut: everything at or above the manor floor, from the top down.
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        for (int y = top; y >= y0; y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            // Somebody's handiwork stands here: the column is left as it is.
            if (!natural(s)) return null;
            return new Sites.Op(Sites.Kind.CLEAR, p, s, -1);
        }
        Earth earth = earth(level, x, y0 - 1, z);
        // Fill: from the lowest gap up to the floor, with the land's own earth. Anything below the
        // floor that is not firm ground (water, air, a buried trunk, leaves, flowers, snow) is
        // replaced, so the floor is earth all the way down to rock.
        BlockPos floor = new BlockPos(x, y0 - 1, z);
        BlockState f = level.getBlockState(floor);
        int bottom = Math.max(level.getMinBuildHeight() + 1, y0 - 1 - MAX_FILL);
        if (!firm(f)) {
            int y = y0 - 1;
            while (y > bottom && !firm(level.getBlockState(new BlockPos(x, y - 1, z)))) y--;
            return new Sites.Op(Sites.Kind.SURFACE, new BlockPos(x, y, z), y == y0 - 1 ? earth.top : earth.under, -1);
        }
        // Sand needs something under it, or it falls into the hollow below.
        if (earth.top.getBlock() instanceof FallingBlock && !firm(level.getBlockState(floor.below())) && f.getBlock() != earth.top.getBlock()) {
            int y = y0 - 2;
            while (y > bottom && !firm(level.getBlockState(new BlockPos(x, y - 1, z)))) y--;
            return new Sites.Op(Sites.Kind.SURFACE, new BlockPos(x, y, z), earth.under, -1);
        }
        // Surface: bare rock or earth laid open is given its top (grass, or sand).
        if (f.getBlock() != earth.top.getBlock() && resurface(f)) return new Sites.Op(Sites.Kind.SURFACE, floor, earth.top, -1);
        return null;
    }

    /** How far below the floor a hollow is filled (deeper caves are left, roofed over). */
    private static final int MAX_FILL = 64;

    /** Ground that can be built on: solid, and not something that grew there (a buried log, leaves, a mushroom). */
    static boolean firm(BlockState s) {
        return Sites.solid(s) && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES) && !Village.growth(s)
                && !(s.getBlock() instanceof net.minecraft.world.level.block.BushBlock) && !s.is(BlockTags.FLOWERS) && !s.is(BlockTags.SAPLINGS)
                && !s.is(Blocks.BROWN_MUSHROOM_BLOCK) && !s.is(Blocks.RED_MUSHROOM_BLOCK) && !s.is(Blocks.MUSHROOM_STEM);
    }

    /** Does a job without the noise of hand work. */
    private static void apply(ServerLevel level, Territory t, Sites.Op op) {
        BlockState now = level.getBlockState(op.pos());
        switch (op.kind()) {
            case CLEAR -> {
                if (now.isAir()) return;
                if (now.getFluidState().isEmpty()) Sites.keepDrops(level, t, null, op.pos(), now);
                level.setBlock(op.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            case FILL -> {
                if (Sites.solid(now)) return;
                if (Goods.EARTH.stock(t) > 0) Goods.EARTH.add(t, -1);
                level.setBlock(op.pos(), op.state(), Block.UPDATE_CLIENTS);
            }
            case SURFACE -> {
                if (now.getBlock() == op.state().getBlock()) return;
                if (!now.isAir() && now.getFluidState().isEmpty()) Sites.keepDrops(level, t, null, op.pos(), now);
                level.setBlock(op.pos(), op.state(), Block.UPDATE_CLIENTS);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ what is left alone

    /**
     * Columns the levelling passes over: every building or site with a block around it, the
     * paths, and a wall already rising. (The manor hall itself is spared as somebody's work; its
     * yard is levelled with the rest.)
     */
    static Set<Long> kept(Territory t, Building job) {
        Set<Long> out = new HashSet<>();
        int cx = t.center.getX(), cz = t.center.getZ();
        for (Building b : t.buildings) {
            if (b == job || b.demolishing && b.placed <= 0) continue;
            if (b.type.placed()) {
                for (int x = -1; x <= b.sizeX(); x++)
                    for (int z = -1; z <= b.sizeZ(); z++) out.add(Village.colKeyOf(b.origin.getX() + x, b.origin.getZ() + z));
            } else if (b.type == BuildingType.PATH && b.pathCols != null) {
                for (int i = 0; i + 3 < b.pathCols.length; i += 4) out.add(Village.colKeyOf(b.pathCols[i], b.pathCols[i + 2]));
            } else if (b.type.style == BuildingType.Style.FENCE && (b.complete || b.placed > 0)) {
                int fr = b.fenceRadius > 0 ? b.fenceRadius : t.level.wall();
                for (int x = -fr - 3; x <= fr + 3; x++)
                    for (int z = -fr - 3; z <= fr + 3; z++) {
                        double d = Math.sqrt((double) x * x + (double) z * z);
                        if (d >= fr - 3 && d <= fr + 2) out.add(Village.colKeyOf(cx + x, cz + z));
                    }
            }
        }
        return out;
    }

    /** Blocks the land is made of, or grows: these may be cut away. Anything else is somebody's work. */
    static boolean natural(BlockState s) {
        if (!s.getFluidState().isEmpty() || s.canBeReplaced() || Village.growth(s)) return true;
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.TERRACOTTA)
                || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE) || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CAVE_VINES) || s.is(BlockTags.CORAL_BLOCKS) || s.is(BlockTags.CORALS)
                || s.is(net.minecraftforge.common.Tags.Blocks.ORES) || s.is(net.minecraftforge.common.Tags.Blocks.GRAVEL)
                || s.is(net.minecraftforge.common.Tags.Blocks.SANDSTONE) || s.is(Blocks.CLAY) || s.is(Blocks.CALCITE)
                || s.is(Blocks.DRIPSTONE_BLOCK) || s.is(Blocks.POINTED_DRIPSTONE) || s.is(Blocks.AMETHYST_BLOCK) || s.is(Blocks.BUDDING_AMETHYST)
                || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.COCOA) || s.is(Blocks.BEE_NEST)
                || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.OBSIDIAN) || s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.KELP)
                || s.is(Blocks.KELP_PLANT) || s.is(Blocks.SEA_PICKLE) || s.is(Blocks.BIG_DRIPLEAF) || s.is(Blocks.BIG_DRIPLEAF_STEM)
                || s.is(Blocks.SMALL_DRIPLEAF) || s.is(Blocks.SPORE_BLOSSOM) || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA)
                || s.is(Blocks.GLOW_LICHEN) || s.is(Blocks.HANGING_ROOTS) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.SCULK);
    }

    /** Ground laid open that should be given its proper top. */
    private static boolean resurface(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.TERRACOTTA)
                || s.is(net.minecraftforge.common.Tags.Blocks.GRAVEL) || s.is(net.minecraftforge.common.Tags.Blocks.SANDSTONE)
                || s.is(net.minecraftforge.common.Tags.Blocks.ORES) || s.is(Blocks.CLAY) || s.is(Blocks.CALCITE) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.DRIPSTONE_BLOCK) || s.is(BlockTags.ICE);
    }

    /** The land's own earth: its top, and what lies beneath. */
    public record Earth(BlockState top, BlockState under) {}

    public static final Earth GRASS = new Earth(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
    public static final Earth SAND = new Earth(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState());
    public static final Earth RED_SAND = new Earth(Blocks.RED_SAND.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState());
    public static final Earth MYCELIUM = new Earth(Blocks.MYCELIUM.defaultBlockState(), Blocks.DIRT.defaultBlockState());

    public static Earth earth(ServerLevel level, int x, int y, int z) {
        net.minecraft.core.Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        if (biome.is(BiomeTags.IS_BADLANDS)) return RED_SAND;
        if (biome.is(net.minecraftforge.common.Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_DEEP_OCEAN)) return SAND;
        if (biome.is(net.minecraftforge.common.Tags.Biomes.IS_MUSHROOM)) return MYCELIUM;
        return GRASS;
    }
}
