package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The look of every village building, generated from its style and footprint in the village's
 * Chinese style: stone plinths, red-lacquered posts, white walls and dark tiled roofs. A design
 * is a list of blocks, ordered bottom-up, placed one by one as construction advances.
 *
 * <p>Designs are drawn facing south (the door on the local z=0 side) and turned to the
 * building's rotation.
 */
public final class BuildingDesigns {
    private BuildingDesigns() {}

    public record Placement(BlockPos pos, BlockState state) {}

    private static final Map<Building, List<Placement>> CACHE = new WeakHashMap<>();

    /** The building's blocks in placing order, or null while part of it is not loaded. */
    public static List<Placement> design(ServerLevel level, Territory t, Building b) {
        List<Placement> d = CACHE.get(b);
        if (d == null) {
            d = make(level, t, b);
            if (d != null) CACHE.put(b, d);
        }
        return d;
    }

    /** Block count of the design, or -1 while it cannot be worked out. */
    public static int size(ServerLevel level, Territory t, Building b) {
        List<Placement> d = design(level, t, b);
        return d == null ? -1 : d.size();
    }

    // ------------------------------------------------------------------ canvas

    /** Collects blocks in local coordinates and turns them into world positions. */
    static final class Canvas {
        final Building b;
        final int w, d;
        final Rotation rot;
        final java.util.LinkedHashMap<BlockPos, BlockState> blocks = new java.util.LinkedHashMap<>();

        Canvas(Building b) {
            this.b = b;
            this.w = b.width();
            this.d = b.depth();
            this.rot = switch (b.rotation & 3) {
                case 1 -> Rotation.CLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                case 3 -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        void set(int x, int y, int z, BlockState s) {
            if (x < 0 || z < 0 || x >= w || z >= d) return;
            int wx, wz;
            switch (b.rotation & 3) {
                case 1 -> {
                    wx = d - 1 - z;
                    wz = x;
                }
                case 2 -> {
                    wx = w - 1 - x;
                    wz = d - 1 - z;
                }
                case 3 -> {
                    wx = z;
                    wz = w - 1 - x;
                }
                default -> {
                    wx = x;
                    wz = z;
                }
            }
            blocks.put(b.origin.offset(wx, y, wz), s.rotate(rot));
        }

        void set(int x, int y, int z, Block block) {
            set(x, y, z, block.defaultBlockState());
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
            fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
        }

        /** A two-block door facing out of the front (south, z=0 side). */
        void door(int x, int y, int z, Block door, Direction facing) {
            BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            set(x, y, z, lower);
            set(x, y + 1, z, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        /** A bed whose head points the given way. */
        void bed(int x, int y, int z, Block bed, Direction head) {
            BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT);
            set(x, y, z, foot);
            set(x + head.getStepX(), y, z + head.getStepZ(), foot.setValue(BedBlock.PART, BedPart.HEAD));
        }

        void facing(int x, int y, int z, Block block, Direction dir) {
            BlockState s = block.defaultBlockState();
            if (s.hasProperty(HorizontalDirectionalBlock.FACING)) s = s.setValue(HorizontalDirectionalBlock.FACING, dir);
            set(x, y, z, s);
        }

        List<Placement> build() {
            List<Placement> out = new ArrayList<>();
            blocks.forEach((p, s) -> out.add(new Placement(p, s)));
            // Bottom-up, so buildings rise from their foundations. Air first clears the way.
            out.sort(Comparator.comparingInt((Placement p) -> p.state().isAir() ? -1000 : p.pos().getY())
                    .thenComparingInt(p -> attachedLast(p.state()) ? 1 : 0));
            return out;
        }

        /** Doors, beds, lanterns, ladders and crops go after the block they rest on. */
        private static boolean attachedLast(BlockState s) {
            Block b = s.getBlock();
            return b instanceof DoorBlock || b instanceof BedBlock || b instanceof LanternBlock || b instanceof LadderBlock || b instanceof CropBlock;
        }
    }

    // ------------------------------------------------------------------ palette

    private static final BlockState PLINTH = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState POST = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
    private static final BlockState WALL = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    private static final BlockState BEAM = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState TILE = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState TILE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState WINDOW = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static List<Placement> make(ServerLevel level, Territory t, Building b) {
        if (b.type.style == BuildingType.Style.FENCE) return fence(level, t, b);
        if (b.type.style == BuildingType.Style.PATH) return Paths.design(b);
        if (b.type.style == BuildingType.Style.CLEARING) return new ArrayList<>();
        Canvas c = new Canvas(b);
        int w = c.w, d = c.d;
        switch (b.type.style) {
            case HALL -> {
                hall(c, w, d, 4, true);
                interior(c, b.type, w, d);
            }
            case HUT -> {
                hall(c, w, d, 3, false);
                interior(c, b.type, w, d);
            }
            case WORKSHOP -> {
                hall(c, w, d, 4, true);
                // Open front, so the work can be seen.
                for (int x = 1; x < w - 1; x++) for (int y = 0; y < 3; y++) c.set(x, y, 0, AIR);
                interior(c, b.type, w, d);
            }
            case SHOP -> {
                hall(c, w, d, 4, true);
                for (int x = 1; x < w - 1; x++) {
                    c.set(x, 1, 0, AIR);
                    c.set(x, 2, 0, AIR);
                    if (x != w / 2) c.set(x, 0, 1, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                }
                interior(c, b.type, w, d);
            }
            case COURTYARD -> courtyard(c, w, d);
            case LOGGING -> logging(c, w, d);
            case QUARRY -> quarry(c, w, d);
            case MINE -> mine(c, w, d);
            case FARM -> farm(c, w, d, b, level);
            case CAMP -> camp(c, w, d);
            case TEMPLE -> temple(c, w, d, b.type == BuildingType.TEMPLE);
            case DOJO -> dojo(c, w, d);
            case GRANARY -> granary(c, w, d);
            case TOWER -> tower(c, b.type == BuildingType.SENTRY_POST);
            case OUTPOST -> outpost(c, w, d);
            case SHED -> shed(c, w, d, b.type);
            default -> hall(c, w, d, 4, true);
        }
        if (b.type.style == BuildingType.Style.TOWER && onWall(t, b)) sealTower(c, t, b);
        return c.build();
    }

    /** True if a tower stands on the fence ring. */
    public static boolean onWall(Territory t, Building b) {
        BlockPos mid = b.center();
        for (Building f : t.buildings) {
            if (f.type != BuildingType.SIMPLE_FENCE || f.demolishing) continue;
            int fr = f.fenceRadius > 0 ? f.fenceRadius : t.level.wall();
            double dist = Math.hypot(mid.getX() - t.center.getX(), mid.getZ() - t.center.getZ());
            if (Math.abs(dist - fr) <= 2.5) return true;
        }
        return false;
    }

    /**
     * A tower standing in the wall must not be a way through it: below the wall-top its sides are
     * closed except the one facing into the village (or the next one, if that side holds the ladder).
     */
    private static void sealTower(Canvas c, Territory t, Building b) {
        BlockPos mid = b.center();
        BlockState solid = b.type == BuildingType.SENTRY_POST ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_LOG.defaultBlockState();
        List<BlockPos> sides = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) sides.add(mid.relative(d));
        sides.sort(Comparator.comparingDouble(p -> p.distSqr(new BlockPos(t.center.getX(), p.getY(), t.center.getZ()))));
        BlockPos open = null;
        for (BlockPos sd : sides) {
            BlockState now = c.blocks.get(sd);
            if (now == null || now.isAir()) {
                open = sd;
                break;
            }
        }
        for (BlockPos sd : sides) {
            if (sd.equals(open)) continue;
            for (int y = 0; y <= 2; y++) {
                BlockPos at = sd.above(y);
                BlockState now = c.blocks.get(at);
                if (now == null || now.isAir()) c.blocks.put(at, solid);
            }
        }
    }

    // ------------------------------------------------------------------ halls

    /** A timber-framed hall: stone plinth, red corner posts, white walls, windows and a tiered roof. */
    private static void hall(Canvas c, int w, int d, int wallH, boolean windows) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, PLINTH);
        c.fill(1, -1, 1, w - 2, -1, d - 2, FLOOR);
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (!edge) {
                    for (int y = 0; y < wallH; y++) c.set(x, y, z, AIR);
                    continue;
                }
                boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                for (int y = 0; y < wallH; y++) {
                    BlockState s;
                    if (corner) s = POST;
                    else if (y == wallH - 1) s = BEAM;
                    else if (windows && y >= 1 && y <= 2 && ((x % 2 == 0 && (z == 0 || z == d - 1)) || (z % 2 == 0 && (x == 0 || x == w - 1)))) s = WINDOW;
                    else s = WALL;
                    c.set(x, y, z, s);
                }
            }
        c.door(w / 2, 0, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        roof(c, w, d, wallH);
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        c.set(w / 2, wallH - 1, d / 2, lantern);
    }

    /** Tiered roof with overhanging eaves, stepping in until it closes. */
    private static void roof(Canvas c, int w, int d, int y0) {
        int inset = -1;
        int y = y0;
        while (true) {
            int x0 = inset, x1 = w - 1 - inset, z0 = inset, z1 = d - 1 - inset;
            if (x0 > x1 || z0 > z1) break;
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
                    c.set(x, y, z, rim ? TILE_SLAB : TILE);
                }
            inset++;
            y++;
            if (x1 - x0 <= 1 || z1 - z0 <= 1) break;
        }
    }

    /** Furniture by building. */
    private static void interior(Canvas c, BuildingType type, int w, int d) {
        int back = d - 2;
        switch (type) {
            case DINING_HALL -> {
                for (int x = 2; x < w - 2; x += 3) {
                    for (int z = 2; z <= back - 1; z++) {
                        c.set(x, 0, z, Blocks.SPRUCE_FENCE);
                        c.set(x, 1, z, Blocks.SPRUCE_PRESSURE_PLATE);
                    }
                }
                c.facing(1, 0, back, Blocks.SMOKER, Direction.SOUTH);
                c.set(w - 2, 0, back, Blocks.BARREL);
            }
            case BATHROOM -> {
                Block cauldron = Blocks.WATER_CAULDRON;
                c.set(1, 0, back, cauldron.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
                c.set(w - 2, 0, back, cauldron.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            }
            case BLACKSMITH_WORKSHOP -> {
                c.facing(1, 0, back, Blocks.FURNACE, Direction.SOUTH);
                c.facing(2, 0, back, Blocks.BLAST_FURNACE, Direction.SOUTH);
                c.facing(w / 2, 0, d / 2, Blocks.ANVIL, Direction.EAST);
                c.set(w - 2, 0, back, Blocks.CHEST);
                c.set(w - 2, 0, back - 1, Blocks.LAVA_CAULDRON);
            }
            case WEAPONS_WORKSHOP -> {
                c.facing(1, 0, back, Blocks.BLAST_FURNACE, Direction.SOUTH);
                c.set(2, 0, back, Blocks.SMITHING_TABLE);
                c.facing(3, 0, back, Blocks.GRINDSTONE, Direction.SOUTH);
                c.facing(w / 2, 0, d / 2, Blocks.ANVIL, Direction.EAST);
                c.set(w - 2, 0, back, Blocks.CHEST);
            }
            case GROCERY_SHOP -> {
                for (int x = 1; x < w - 1; x++) c.set(x, 0, back, Blocks.BARREL);
                c.set(1, 1, back, Blocks.BARREL);
                c.set(w - 2, 1, back, Blocks.BARREL);
            }
            case TAILOR_WORKSHOP -> {
                c.facing(1, 0, back, Blocks.LOOM, Direction.SOUTH);
                c.facing(2, 0, back, Blocks.LOOM, Direction.SOUTH);
                c.set(w - 2, 0, back, Blocks.WHITE_WOOL);
                c.set(w - 2, 1, back, Blocks.RED_WOOL);
                c.set(w - 3, 0, back, Blocks.CHEST);
            }
            case POTTERY_WORKSHOP -> {
                c.facing(1, 0, back, Blocks.FURNACE, Direction.SOUTH);
                c.set(2, 0, back, Blocks.CLAY);
                c.set(w - 2, 0, back, Blocks.DECORATED_POT);
                c.set(w - 3, 0, back, Blocks.FLOWER_POT);
            }
            case WINE_WORKSHOP -> {
                for (int x = 1; x < w - 1; x += 2) c.set(x, 0, back, Blocks.BARREL);
                c.set(2, 0, back, Blocks.BREWING_STAND);
                c.set(w - 2, 0, 1, Blocks.CAULDRON);
            }
            case CLINIC -> {
                for (int x = 1; x < w - 2; x += 2) c.bed(x, 0, back - 1, Blocks.WHITE_BED, Direction.SOUTH);
                c.set(w - 2, 0, back, Blocks.BREWING_STAND);
                c.set(w - 2, 0, 1, Blocks.FLOWER_POT);
            }
            case PRIVATE_SCHOOL -> {
                for (int x = 1; x < w - 1; x++) c.set(x, 0, back, Blocks.BOOKSHELF);
                for (int x = 2; x < w - 2; x += 2)
                    for (int z = 2; z < back - 1; z += 2) c.facing(x, 0, z, Blocks.LECTERN, Direction.SOUTH);
            }
            case MILL -> {
                c.facing(w / 2, 0, d / 2, Blocks.GRINDSTONE, Direction.SOUTH);
                c.set(1, 0, back, Blocks.HAY_BLOCK);
                c.set(w - 2, 0, back, Blocks.COMPOSTER);
            }
            default -> {
                c.set(1, 0, back, Blocks.BARREL);
                c.set(w - 2, 0, back, Blocks.CHEST);
            }
        }
    }

    // ------------------------------------------------------------------ homes

    /** The first, smaller courtyard (9 x 9), kept for courtyards built with it. */
    private static void courtyardSmall(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (edge) {
                    c.set(x, -1, z, PLINTH);
                    c.set(x, 0, z, Blocks.STONE_BRICK_WALL);
                    c.set(x, 1, z, Blocks.STONE_BRICK_WALL);
                }
            }
        c.set(w / 2, 0, 0, AIR);
        c.set(w / 2, 1, 0, AIR);
        c.set(w / 2, -1, 0, Blocks.DIRT_PATH);
        // House across the back.
        int hz0 = d - 5;
        for (int x = 1; x < w - 1; x++)
            for (int z = hz0; z < d - 1; z++) {
                boolean edge = x == 1 || x == w - 2 || z == hz0 || z == d - 2;
                c.set(x, -1, z, FLOOR);
                for (int y = 0; y < 3; y++) c.set(x, y, z, edge ? (y == 2 ? BEAM : WALL) : AIR);
            }
        for (int x : new int[]{1, w - 2})
            for (int z : new int[]{hz0, d - 2})
                for (int y = 0; y < 3; y++) c.set(x, y, z, POST);
        c.door(w / 2, 0, hz0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        c.set(w / 2 - 2, 1, hz0, WINDOW);
        c.set(w / 2 + 2, 1, hz0, WINDOW);
        for (int x = 2; x < w - 2; x += 2) if (x != w / 2) c.bed(x, 0, d - 3, Blocks.RED_BED, Direction.SOUTH);
        for (int x = 0; x < w; x++)
            for (int z = hz0 - 1; z < d; z++) {
                boolean rim = x == 0 || x == w - 1 || z == hz0 - 1 || z == d - 1;
                c.set(x, 3, z, rim ? TILE_SLAB : TILE);
            }
        for (int x = 1; x < w - 1; x++) c.set(x, 4, hz0 + 1, TILE_SLAB);
        // Well and garden.
        c.set(2, 0, 2, Blocks.COBBLESTONE_WALL);
        c.set(2, -1, 2, Blocks.WATER);
        c.set(w - 3, 0, 2, Blocks.POPPY);
        c.set(w - 3, 0, 3, Blocks.OXEYE_DAISY);
    }

    /** Straw beds: plain, cheap bedding for ordinary folk (yellow beds look like straw). */
    static final Block STRAW_BED = Blocks.YELLOW_BED;

    /**
     * A walled residential courtyard for twenty people, the old siheyuan way: a hall across the
     * back and a wing down each side, all facing a yard with a well. Straw beds sleep 13 in the
     * hall and 5 in each wing.
     */
    private static void courtyard(Canvas c, int w, int d) {
        if (w < 17) {
            courtyardSmall(c, w, d);
            return;
        }
        if (w >= 21) {
            courtyardTall(c, w, d, w >= 27);
            return;
        }
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                if (x == 0 || z == 0 || x == w - 1 || z == d - 1) {
                    c.set(x, -1, z, PLINTH);
                    c.set(x, 0, z, Blocks.STONE_BRICK_WALL);
                    c.set(x, 1, z, Blocks.STONE_BRICK_WALL);
                }
            }
        // The gate.
        c.set(w / 2, 0, 0, AIR);
        c.set(w / 2, 1, 0, AIR);
        for (int z = 0; z <= d - 7; z++) c.set(w / 2, -1, z, Blocks.DIRT_PATH);
        // The hall across the back: beds side by side, heads to the back wall.
        int hz0 = d - 6, hz1 = d - 2;
        house(c, 1, hz0, w - 2, hz1);
        c.door(w / 2, 0, hz0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        c.set(w / 2 - 3, 1, hz0, WINDOW);
        c.set(w / 2 + 3, 1, hz0, WINDOW);
        for (int x = 2; x <= w - 3; x++) c.bed(x, 0, hz1 - 2, STRAW_BED, Direction.SOUTH);
        // The side wings: beds heads to the outer walls.
        int wz0 = 2, wz1 = d - 7;
        house(c, 1, wz0, 5, wz1);
        house(c, w - 6, wz0, w - 2, wz1);
        int mid = (wz0 + wz1) / 2;
        c.door(5, 0, mid, Blocks.SPRUCE_DOOR, Direction.EAST);
        c.door(w - 6, 0, mid, Blocks.SPRUCE_DOOR, Direction.WEST);
        for (int z = wz0 + 1; z <= wz1 - 1; z++) {
            c.bed(3, 0, z, STRAW_BED, Direction.WEST);
            c.bed(w - 4, 0, z, STRAW_BED, Direction.EAST);
        }
        // A water butt in the yard, a lantern by the gate.
        c.set(6, 0, wz0 + 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        c.set(w / 2 + 2, 0, 1, Blocks.LANTERN);
        c.set(w / 2 - 2, 0, 1, Blocks.POPPY);
    }

    /**
     * The larger courtyards, for a growing village: the hall across the back rises two storeys
     * (Medium, 50 straw beds), and in the Large courtyard the side wings do too (100). Ladders
     * lead to the upper floors.
     */
    private static void courtyardTall(Canvas c, int w, int d, boolean tallWings) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                if (x == 0 || z == 0 || x == w - 1 || z == d - 1) {
                    c.set(x, -1, z, PLINTH);
                    for (int y = 0; y <= 2; y++) c.set(x, y, z, y == 2 ? Blocks.STONE_BRICK_SLAB : Blocks.STONE_BRICK_WALL);
                }
            }
        // A gateway with red posts and a little roof.
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = 0; y <= 2; y++) c.set(w / 2 + dx, y, 0, AIR);
            c.set(w / 2 + dx, 3, 0, TILE_SLAB);
        }
        for (int dx : new int[]{-2, 2}) for (int y = 0; y <= 3; y++) c.set(w / 2 + dx, y, 0, POST);
        c.set(w / 2 - 2, 4, 0, Blocks.LANTERN);
        c.set(w / 2 + 2, 4, 0, Blocks.LANTERN);
        for (int z = 0; z <= d - 7; z++) c.set(w / 2, -1, z, Blocks.DIRT_PATH);
        // The hall across the back, two storeys.
        int hz0 = d - 6, hz1 = d - 2;
        house2(c, 1, hz0, w - 2, hz1);
        c.door(w / 2, 0, hz0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        for (int x = 3; x <= w - 3; x++) c.bed(x, 0, hz1 - 2, STRAW_BED, Direction.SOUTH);
        for (int x = 2; x <= w - 3; x++) c.bed(x, 4, hz1 - 2, STRAW_BED, Direction.SOUTH);
        ladder(c, 2, hz0 + 1, Direction.EAST);
        // The side wings.
        int wz0 = 2, wz1 = d - 7, mid = (wz0 + wz1) / 2;
        if (tallWings) {
            house2(c, 1, wz0, 5, wz1);
            house2(c, w - 6, wz0, w - 2, wz1);
        } else {
            house(c, 1, wz0, 5, wz1);
            house(c, w - 6, wz0, w - 2, wz1);
        }
        c.door(5, 0, mid, Blocks.SPRUCE_DOOR, Direction.EAST);
        c.door(w - 6, 0, mid, Blocks.SPRUCE_DOOR, Direction.WEST);
        for (int z = wz0 + 1; z <= wz1 - 1; z++) {
            c.bed(3, 0, z, STRAW_BED, Direction.WEST);
            c.bed(w - 4, 0, z, STRAW_BED, Direction.EAST);
            if (tallWings) {
                c.bed(3, 4, z, STRAW_BED, Direction.WEST);
                c.bed(w - 4, 4, z, STRAW_BED, Direction.EAST);
            }
        }
        if (tallWings) {
            ladder(c, 4, wz0 + 1, Direction.SOUTH);
            ladder(c, w - 5, wz0 + 1, Direction.SOUTH);
        }
        // A well and lanterns in the yard.
        c.set(w / 2 - 3, 0, 3, Blocks.COBBLESTONE_WALL);
        c.set(w / 2 - 3, -1, 3, Blocks.WATER);
        c.set(w / 2 + 3, 0, 3, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        c.set(w / 2 - 2, 0, d - 8, Blocks.LANTERN);
        c.set(w / 2 + 2, 0, d - 8, Blocks.LANTERN);
    }

    /** A ladder from the ground floor up through the floor above. */
    private static void ladder(Canvas c, int x, int z, Direction facing) {
        for (int y = 0; y <= 3; y++) c.facing(x, y, z, Blocks.LADDER, facing);
    }

    /** A two-storey timber house: as {@link #house} with a plank floor at y 3 and a second storey to y 6. */
    private static void house2(Canvas c, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                c.set(x, -1, z, FLOOR);
                for (int y = 0; y <= 6; y++) {
                    BlockState s;
                    if (!edge) s = y == 3 ? FLOOR : AIR;
                    else if (y == 2 || y == 3 || y == 6) s = BEAM;
                    else if ((y == 1 || y == 5) && (x - x0) % 4 == 2 && (z == z0 || z == z1)) s = WINDOW;
                    else s = WALL;
                    c.set(x, y, z, s);
                }
            }
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1})
                for (int y = 0; y <= 6; y++) c.set(x, y, z, POST);
        for (int x = x0 - 1; x <= x1 + 1; x++)
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean rim = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                c.set(x, 7, z, rim ? TILE_SLAB : TILE);
            }
        if (x1 - x0 >= z1 - z0) for (int x = x0; x <= x1; x++) c.set(x, 8, (z0 + z1) / 2, TILE_SLAB);
        else for (int z = z0; z <= z1; z++) c.set((x0 + x1) / 2, 8, z, TILE_SLAB);
    }

    /** A plain timber house: plank floor, white walls on a beam, posts at the corners, tiled roof with eaves. */
    private static void house(Canvas c, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                c.set(x, -1, z, FLOOR);
                for (int y = 0; y < 3; y++) c.set(x, y, z, edge ? (y == 2 ? BEAM : WALL) : AIR);
            }
        for (int x : new int[]{x0, x1})
            for (int z : new int[]{z0, z1})
                for (int y = 0; y < 3; y++) c.set(x, y, z, POST);
        for (int x = x0 - 1; x <= x1 + 1; x++)
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean rim = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                c.set(x, 3, z, rim ? TILE_SLAB : TILE);
            }
        if (x1 - x0 >= z1 - z0) for (int x = x0; x <= x1; x++) c.set(x, 4, (z0 + z1) / 2, TILE_SLAB);
        else for (int z = z0; z <= z1; z++) c.set((x0 + x1) / 2, 4, z, TILE_SLAB);
    }

    // ------------------------------------------------------------------ fields

    private static void fenceRing(Canvas c, int w, int d, Block fence, Block gate) {
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (edge) c.set(x, 0, z, fence);
            }
        c.set(w / 2, 0, 0, gate.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
    }

    private static void logging(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.COARSE_DIRT.defaultBlockState());
        fenceRing(c, w, d, Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE);
        for (int x = 2; x < w - 2; x += 3)
            for (int z = 5; z < d - 1; z += 3) {
                c.set(x, -1, z, Blocks.GRASS_BLOCK);
                c.set(x, 0, z, Blocks.OAK_SAPLING);
            }
        // Log piles and a chopping stump.
        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int x = 2; x <= 5; x++) {
            c.set(x, 0, 2, log);
            c.set(x, 0, 3, log);
            c.set(x, 1, 2, log);
        }
        c.set(w - 3, 0, 2, Blocks.OAK_LOG);
        c.set(w - 3, 1, 2, Blocks.SPRUCE_TRAPDOOR);
        c.set(w - 2, 0, 2, Blocks.CHEST);
    }

    private static void quarry(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.GRAVEL.defaultBlockState());
        for (int x = 2; x < w - 2; x++)
            for (int z = 2; z < d - 2; z++) {
                int depth = (x == 2 || z == 2 || x == w - 3 || z == d - 3) ? 2 : 3;
                for (int y = -1; y > -1 - depth; y--) c.set(x, y, z, AIR);
                c.set(x, -1 - depth, z, Blocks.STONE);
            }
        c.set(2, -1, 2, Blocks.STONE);
        c.set(1, 0, 1, Blocks.STONECUTTER);
        c.set(w - 2, 0, 1, Blocks.COBBLESTONE);
        c.set(w - 2, 1, 1, Blocks.COBBLESTONE_SLAB);
        c.set(1, 0, d - 2, Blocks.COBBLESTONE);
        c.set(w - 2, 0, d - 2, Blocks.CHEST);
        fenceRing(c, w, d, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE);
    }

    private static void mine(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.COBBLESTONE.defaultBlockState());
        int cx = w / 2, cz = d / 2;
        for (int x = cx - 1; x <= cx + 1; x++)
            for (int z = cz - 1; z <= cz + 1; z++) {
                for (int y = -1; y >= -6; y--) c.set(x, y, z, AIR);
                c.set(x, -7, z, Blocks.IRON_ORE);
            }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = -1; y >= -6; y--) {
            c.set(cx, y, cz - 2, Blocks.STONE);
            c.set(cx, y, cz - 1, ladder);
        }
        // Headframe over the shaft.
        for (int y = 0; y < 4; y++) {
            c.set(cx - 2, y, cz - 2, POST);
            c.set(cx + 2, y, cz - 2, POST);
            c.set(cx - 2, y, cz + 2, POST);
            c.set(cx + 2, y, cz + 2, POST);
        }
        c.fill(cx - 2, 4, cz - 2, cx + 2, 4, cz + 2, Blocks.SPRUCE_SLAB.defaultBlockState());
        c.set(cx, 3, cz, Blocks.CHAIN);
        c.set(1, 0, 1, Blocks.IRON_ORE);
        c.set(2, 0, 1, Blocks.RAW_IRON_BLOCK);
        c.set(w - 2, 0, 1, Blocks.CHEST);
    }

    private static void farm(Canvas c, int w, int d, Building b, ServerLevel level) {
        double growth = Village.growth(b, GaiaCalendar.today(level.getServer()));
        int age = (int) Math.round(growth * 7);
        for (int x = 1; x < w - 1; x++)
            for (int z = 1; z < d - 1; z++) {
                if (x == w / 2) {
                    c.set(x, -1, z, Blocks.WATER);
                    continue;
                }
                c.set(x, -1, z, Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
                c.set(x, 0, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, b.sown ? age : 0));
            }
        fenceRing(c, w, d, Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE);
        c.set(0, 1, 0, Blocks.TORCH);
        c.set(w - 1, 1, 0, Blocks.TORCH);
    }

    // ------------------------------------------------------------------ military and defence

    private static void camp(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (edge) {
                    c.set(x, 0, z, Blocks.SPRUCE_LOG);
                    c.set(x, 1, z, Blocks.SPRUCE_FENCE);
                }
            }
        c.set(w / 2, 0, 0, AIR);
        c.set(w / 2, 1, 0, AIR);
        // Tents.
        for (int tx : new int[]{2, w - 5}) {
            for (int x = tx; x < tx + 3; x++)
                for (int z = d - 4; z < d - 1; z++) c.set(x, 2, z, Blocks.RED_WOOL);
            c.set(tx, 0, d - 4, Blocks.SPRUCE_FENCE);
            c.set(tx, 1, d - 4, Blocks.SPRUCE_FENCE);
            c.set(tx + 2, 0, d - 4, Blocks.SPRUCE_FENCE);
            c.set(tx + 2, 1, d - 4, Blocks.SPRUCE_FENCE);
        }
        // Training targets and a weapon rack.
        c.set(w / 2, 0, d / 2, Blocks.HAY_BLOCK);
        c.set(w / 2, 1, d / 2, Blocks.TARGET);
        c.set(2, 0, 2, Blocks.CHEST);
        c.set(3, 0, 2, Blocks.FLETCHING_TABLE);
        c.set(w - 3, 0, 2, Blocks.SMITHING_TABLE);
        for (int y = 2; y < 6; y++) c.set(w - 2, y, 1, Blocks.SPRUCE_FENCE);
        c.set(w - 2, 6, 1, Blocks.RED_WOOL);
    }

    private static void tower(Canvas c, boolean stone) {
        BlockState wallBlock = stone ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        c.fill(0, -1, 0, 2, -1, 2, PLINTH);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 3; x++)
                for (int z = 0; z < 3; z++) {
                    boolean corner = (x == 0 || x == 2) && (z == 0 || z == 2);
                    if (corner) c.set(x, y, z, wallBlock);
                    else if (x == 1 && z == 1) c.set(x, y, z, AIR);
                    else if (x == 1 && z == 2) c.set(x, y, z, stone ? wallBlock : Blocks.SPRUCE_PLANKS.defaultBlockState());
                    else c.set(x, y, z, AIR);
                }
            c.set(1, y, 1, ladder);
        }
        c.fill(-1, 6, -1, 3, 6, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
        c.set(1, 6, 1, Blocks.SPRUCE_TRAPDOOR);
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++) if (x != 1 || z != 1) {
                boolean edge = x == 0 || z == 0 || x == 2 || z == 2;
                if (edge) c.set(x, 7, z, Blocks.SPRUCE_FENCE);
            }
        c.set(0, 8, 0, Blocks.TORCH);
        c.fill(0, 9, 0, 2, 9, 2, TILE_SLAB);
    }

    private static void outpost(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, PLINTH);
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < w; x++)
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    c.set(x, y, z, edge ? Blocks.COBBLESTONE.defaultBlockState() : AIR);
                }
        c.door(w / 2, 0, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        c.fill(0, 4, 0, w - 1, 4, d - 1, Blocks.STONE_BRICKS.defaultBlockState());
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (edge && (x + z) % 2 == 0) c.set(x, 5, z, Blocks.STONE_BRICK_WALL);
            }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = 0; y < 4; y++) c.set(w / 2, y, d - 2, ladder);
        c.set(w / 2, 4, d - 2, Blocks.SPRUCE_TRAPDOOR);
        c.set(1, 2, 1, Blocks.LANTERN);
    }

    // ------------------------------------------------------------------ worship and learning

    private static void temple(Canvas c, int w, int d, boolean grand) {
        // Raised terrace with steps.
        c.fill(0, -1, 0, w - 1, 0, d - 1, PLINTH);
        BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        for (int x = w / 2 - 1; x <= w / 2 + 1; x++) c.set(x, 0, 0, step);
        // Hall on the terrace.
        int x0 = 1, x1 = w - 2, z0 = 2, z1 = d - 2;
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 1; y <= 4; y++) {
                    BlockState s;
                    if (!edge) s = AIR;
                    else if ((x == x0 || x == x1) && (z == z0 || z == z1)) s = POST;
                    else if (z == z0 && y <= 3) s = (x % 2 == 0) ? POST : AIR;
                    else s = y == 4 ? BEAM : Blocks.RED_TERRACOTTA.defaultBlockState();
                    c.set(x, y, z, s);
                }
            }
        // Altar and offerings.
        c.set(w / 2, 1, z1 - 1, Blocks.CHISELED_STONE_BRICKS);
        c.set(w / 2, 2, z1 - 1, grand ? Blocks.GOLD_BLOCK : Blocks.DECORATED_POT);
        c.set(w / 2 - 1, 1, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        c.set(w / 2 + 1, 1, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        c.set(w / 2, 1, z0 + 2, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false));
        // Double roof.
        int y = 5;
        for (int tier = 0; tier < (grand ? 2 : 1); tier++) {
            for (int inset = -1; ; inset++) {
                int ax = x0 + inset + tier, bx = x1 - inset - tier, az = z0 + inset + tier, bz = z1 - inset - tier;
                if (ax > bx || az > bz) break;
                for (int x = ax; x <= bx; x++)
                    for (int z = az; z <= bz; z++) {
                        boolean rim = x == ax || x == bx || z == az || z == bz;
                        c.set(x, y, z, rim ? TILE_SLAB : TILE);
                    }
                y++;
                if (bx - ax <= 1 || bz - az <= 1) break;
            }
            y++;
        }
        BlockState hanging = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        c.set(x0, 4, z0 - 1, hanging);
        c.set(x1, 4, z0 - 1, hanging);
    }

    private static void dojo(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        // Covered hall at the back, open training floor in front.
        for (int x = 0; x < w; x++)
            for (int z = d - 4; z < d; z++) {
                boolean edge = x == 0 || x == w - 1 || z == d - 1;
                for (int y = 0; y < 4; y++) c.set(x, y, z, edge ? (y == 3 ? BEAM : WALL) : AIR);
            }
        for (int x = 0; x < w; x += w - 1) for (int y = 0; y < 4; y++) c.set(x, y, d - 4, POST);
        for (int x = -1; x <= w; x++)
            for (int z = d - 5; z <= d; z++) {
                boolean rim = x == -1 || x == w || z == d - 5 || z == d;
                c.set(x, 4, z, rim ? TILE_SLAB : TILE);
            }
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d - 4; z++) {
                boolean edge = x == 0 || x == w - 1 || z == 0;
                if (edge) c.set(x, 0, z, Blocks.BAMBOO_FENCE);
            }
        c.set(w / 2, 0, 0, AIR);
        // Practice posts.
        for (int x = 2; x < w - 2; x += 3) {
            c.set(x, 0, 3, Blocks.SPRUCE_LOG);
            c.set(x, 1, 3, Blocks.SPRUCE_LOG);
            c.set(x, 2, 3, Blocks.HAY_BLOCK);
        }
        c.set(1, 0, d - 2, Blocks.CHEST);
        c.set(w - 2, 0, d - 2, Blocks.TARGET);
    }

    private static void granary(Canvas c, int w, int d) {
        c.fill(0, -1, 0, w - 1, 0, d - 1, PLINTH);
        for (int x = 0; x < w; x++)
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                for (int y = 1; y <= 4; y++) {
                    BlockState s = !edge ? AIR : ((x == 0 || x == w - 1) && (z == 0 || z == d - 1)) ? POST
                            : y == 4 ? BEAM : Blocks.SPRUCE_PLANKS.defaultBlockState();
                    c.set(x, y, z, s);
                }
            }
        c.door(w / 2, 1, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
        for (int x = 1; x < w - 1; x++)
            for (int z = 2; z < d - 1; z++)
                if ((x + z) % 3 != 0) c.set(x, 1, z, Blocks.HAY_BLOCK);
        roof(c, w, d, 5);
    }

    private static void shed(Canvas c, int w, int d, BuildingType type) {
        c.fill(0, -1, 0, w - 1, -1, d - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int x = 0; x < w; x += 2)
            for (int z : new int[]{0, d - 1})
                for (int y = 0; y < 3; y++) c.set(x, y, z, POST);
        for (int x = -1; x <= w; x++)
            for (int z = -1; z <= d; z++) c.set(x, 3, z, x == -1 || x == w || z == -1 || z == d ? TILE_SLAB : TILE);
        if (type == BuildingType.STABLE) {
            for (int x = 1; x < w - 1; x++) c.set(x, 0, d - 2, Blocks.HAY_BLOCK);
            for (int x = 1; x < w - 1; x += 3)
                for (int z = 1; z < d - 2; z++) c.set(x, 0, z, Blocks.OAK_FENCE);
            c.set(w - 2, 0, 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        } else {
            // Mulberry rows for silkworms.
            for (int x = 1; x < w - 1; x += 2)
                for (int z = 1; z < d - 1; z++) {
                    c.set(x, -1, z, Blocks.GRASS_BLOCK);
                    c.set(x, 0, z, Blocks.AZALEA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
                }
        }
    }

    // ------------------------------------------------------------------ the fence

    /** The posts of a fence ring, in order around it: {x, z}. */
    public static List<int[]> fenceColumns(BlockPos center, int r) {
        List<int[]> out = new ArrayList<>();
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        int steps = (int) Math.ceil(2 * Math.PI * r * 1.5);
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int x = center.getX() + (int) Math.round(Math.cos(a) * r);
            int z = center.getZ() + (int) Math.round(Math.sin(a) * r);
            if (seen.add(((long) x << 32) ^ (z & 0xffffffffL))) out.add(new int[]{x, z});
        }
        return out;
    }

    /** Bearing of a point from a centre in degrees, 0 = east, 90 = south. */
    public static double bearing(BlockPos center, double x, double z) {
        double d = Math.toDegrees(Math.atan2(z - center.getZ(), x - center.getX()));
        return d < 0 ? d + 360 : d;
    }

    /**
     * The Simple Fence: a ring just inside the territory border. Each post is an oak plank set
     * into the ground, two fence blocks, and an oak plank on top, the planks joining into rails.
     * Gateways (three posts wide) have a gate below and the top rail as a lintel. The ground
     * height of every post is fixed when the fence is first laid out, so the design never shifts.
     */
    private static List<Placement> fence(ServerLevel level, Territory t, Building b) {
        int r = b.fenceRadius > 0 ? b.fenceRadius : t.level.wall();
        b.fenceRadius = r;
        List<int[]> cols = fenceColumns(t.center, r);
        int n = cols.size();
        if (b.fenceYs == null || b.fenceYs.length != n) {
            int[] raw = new int[n];
            for (int i = 0; i < n; i++) {
                int x = cols.get(i)[0], z = cols.get(i)[1];
                if (!level.isLoaded(new BlockPos(x, 64, z))) return null;
                raw[i] = groundAt(level, x, z);
            }
            // A post on a lone bump or in a lone dip follows its neighbours instead.
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                int[] win = new int[5];
                for (int k = -2; k <= 2; k++) win[k + 2] = raw[Math.floorMod(i + k, n)];
                java.util.Arrays.sort(win);
                ys[i] = win[2];
            }
            b.fenceYs = ys;
            b.fenceFill = null;
        }
        if (b.fenceFill == null || b.fenceFill.length != n) {
            // Where the ground falls away under a post, earth is packed beneath its footing.
            int[] fill = new int[n];
            for (int i = 0; i < n; i++) {
                int x = cols.get(i)[0], z = cols.get(i)[1], y = b.fenceYs[i];
                if (!level.isLoaded(new BlockPos(x, 64, z))) return null;
                int k = 0;
                while (k < 8) {
                    BlockState s = level.getBlockState(new BlockPos(x, y - 2 - k, z));
                    if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                    k++;
                }
                fill[i] = k;
            }
            b.fenceFill = fill;
        }
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState earth = Blocks.DIRT.defaultBlockState();
        BlockState post = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        BlockState trim = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState cap = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        boolean walkway = walkway(t);
        int[] rails = new int[n];
        boolean[] gate = new boolean[n];
        for (int i = 0; i < n; i++) {
            int y = b.fenceYs[i];
            int neighbour = Math.max(b.fenceYs[Math.floorMod(i - 1, n)], b.fenceYs[Math.floorMod(i + 1, n)]);
            // The top course runs no lower than one below either neighbour's, so no gaps open in the wall.
            rails[i] = Math.max(y, neighbour - 1) + 2;
            gate[i] = b.isGate(bearing(t.center, cols.get(i)[0], cols.get(i)[1]));
        }
        List<Placement> out = new ArrayList<>();
        java.util.Set<Long> towers = towerColumns(t);
        for (int i = 0; i < n; i++) {
            int x = cols.get(i)[0], z = cols.get(i)[1], y = b.fenceYs[i], rail = rails[i];
            // An arrow tower or sentry post set on the wall takes the wall's place there.
            if (towers.contains(((long) x << 32) ^ (z & 0xffffffffL))) continue;
            boolean prevGate = gate[Math.floorMod(i - 1, n)], nextGate = gate[Math.floorMod(i + 1, n)];
            for (int k = b.fenceFill[i]; k >= 1; k--) out.add(new Placement(new BlockPos(x, y - 1 - k, z), earth));
            out.add(new Placement(new BlockPos(x, y - 1, z), plank));
            if (gate[i]) {
                // The gateway: a wooden gate, room to pass beneath a beam, and a little tiled roof.
                double rad = Math.toRadians(bearing(t.center, x, z));
                Direction facing = Direction.getNearest(Math.cos(rad), 0, Math.sin(rad));
                out.add(new Placement(new BlockPos(x, y, z), Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing)));
                boolean middle = prevGate && nextGate && rail - y >= 3;
                for (int yy = y + 1; yy <= rail; yy++) if (!(middle && yy == rail)) out.add(new Placement(new BlockPos(x, yy, z), AIR));
                out.add(new Placement(new BlockPos(x, rail + 1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState()));
                out.add(new Placement(new BlockPos(x, rail + 2, z), TILE_SLAB));
                if (middle) out.add(new Placement(new BlockPos(x, rail, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)));
            } else if (prevGate || nextGate) {
                // Gate posts: tall timbers with a lantern on top.
                for (int yy = y; yy <= rail + 2; yy++) out.add(new Placement(new BlockPos(x, yy, z), post));
                out.add(new Placement(new BlockPos(x, rail + 3, z), Blocks.LANTERN.defaultBlockState()));
            } else if (i % 6 == 0) {
                // A post every six blocks, a lantern on every fourth.
                for (int yy = y; yy <= rail + 1; yy++) out.add(new Placement(new BlockPos(x, yy, z), post));
                if (i % 24 == 0) out.add(new Placement(new BlockPos(x, rail + 2, z), Blocks.LANTERN.defaultBlockState()));
            } else {
                for (int yy = y; yy < rail; yy++) out.add(new Placement(new BlockPos(x, yy, z), plank));
                out.add(new Placement(new BlockPos(x, rail, z), trim));
                // Towns: battlements along the top.
                out.add(new Placement(new BlockPos(x, rail + 1, z), walkway && i % 2 == 0 ? trim : cap));
            }
        }
        if (walkway) walk(level, t, b, cols, rails, gate, out);
        out.removeIf(pl -> towers.contains(((long) pl.pos().getX() << 32) ^ (pl.pos().getZ() & 0xffffffffL)));
        // One block to a place: the first claim wins (the wall before the walk, the walk before a ladder).
        java.util.Set<BlockPos> taken = new java.util.HashSet<>();
        out.removeIf(pl -> !taken.add(pl.pos()));
        return out;
    }

    /** Columns under towers (built or being built) that stand on or by the wall. */
    public static java.util.Set<Long> towerColumns(Territory t) {
        java.util.Set<Long> out = new java.util.HashSet<>();
        for (Building o : t.buildings) {
            if (o.type.style != BuildingType.Style.TOWER || o.demolishing && o.placed <= 0) continue;
            for (int x = 0; x < o.sizeX(); x++)
                for (int z = 0; z < o.sizeZ(); z++) {
                    int wx = o.origin.getX() + x, wz = o.origin.getZ() + z;
                    out.add(((long) wx << 32) ^ (wz & 0xffffffffL));
                }
        }
        return out;
    }

    /** A signature of the towers, so the fence knows when one is added or taken away. */
    public static int towerSignature(Territory t) {
        int sig = 1;
        for (Building o : t.buildings) if (o.type.style == BuildingType.Style.TOWER && !(o.demolishing && o.placed <= 0)) sig = sig * 31 + o.id;
        return sig;
    }

    /** From Town Grade 1 the palisade has a wall-walk behind it. */
    public static boolean walkway(Territory t) {
        return t.level.ordinal() >= com.realmlord.territory.TerritoryLevel.TOWN_1.ordinal();
    }

    /**
     * The wall-walk: a second, inner course of planks as high as the wall's top, so defenders
     * (and later arrow towers) stand behind the battlements. Ladders go up beside each gate.
     */
    private static void walk(ServerLevel level, Territory t, Building b, List<int[]> cols, int[] rails, boolean[] gate, List<Placement> out) {
        int r = b.fenceRadius, n = cols.size();
        double[] bearings = new double[n];
        for (int i = 0; i < n; i++) bearings[i] = bearing(t.center, cols.get(i)[0], cols.get(i)[1]);
        java.util.Set<Long> wall = new java.util.HashSet<>();
        for (int[] c : cols) wall.add(((long) c[0] << 32) ^ (c[1] & 0xffffffffL));
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState(), deck = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int[] c : fenceColumns(t.center, r - 1)) {
            int x = c[0], z = c[1];
            if (wall.contains(((long) x << 32) ^ (z & 0xffffffffL))) continue;
            double brg = bearing(t.center, x, z);
            int j = 0;
            double best = 999;
            for (int i = 0; i < n; i++) {
                double diff = Math.abs(((brg - bearings[i]) % 360 + 540) % 360 - 180);
                if (diff < best) {
                    best = diff;
                    j = i;
                }
            }
            boolean nearGate = false, besideGate = false;
            for (int k = -2; k <= 2; k++) if (gate[Math.floorMod(j + k, n)]) nearGate = true;
            for (int k = -4; k <= 4; k++) if (gate[Math.floorMod(j + k, n)]) besideGate = true;
            if (nearGate) continue;
            if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
            int y = Math.min(groundAt(level, x, z), rails[j]);
            for (int yy = y - 1; yy < rails[j]; yy++) out.add(new Placement(new BlockPos(x, yy, z), plank));
            out.add(new Placement(new BlockPos(x, rails[j], z), deck));
            if (besideGate) {
                // A ladder up on the inside.
                double rad = Math.toRadians(brg);
                int lx = x - (int) Math.round(Math.cos(rad)), lz = z - (int) Math.round(Math.sin(rad));
                Direction in = Direction.getNearest(-Math.cos(rad), 0, -Math.sin(rad));
                if (lx == x && lz == z) continue;
                int ly = groundAt(level, lx, lz);
                for (int yy = ly; yy <= rails[j]; yy++)
                    out.add(new Placement(new BlockPos(lx, yy, lz), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, in)));
            }
        }
    }

    /** Height of the first free block above the ground, ignoring leaves, plants and old fence. */
    private static int groundAt(ServerLevel level, int x, int z) {
        int y = Village.groundAt(level, x, z);
        for (int k = 0; k < 6; k++) {
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            Block bl = below.getBlock();
            boolean manMade = bl instanceof net.minecraft.world.level.block.FenceBlock || bl instanceof FenceGateBlock
                    || below.is(Blocks.OAK_PLANKS) || below.is(Blocks.SPRUCE_PLANKS) || below.is(Blocks.DARK_OAK_PLANKS)
                    || below.is(net.minecraft.tags.BlockTags.LOGS) || below.is(net.minecraft.tags.BlockTags.SLABS)
                    || bl instanceof LanternBlock || bl instanceof LadderBlock;
            if (!manMade) break;
            y--;
        }
        return y;
    }

    public static void forget(Building b) {
        CACHE.remove(b);
    }
}
