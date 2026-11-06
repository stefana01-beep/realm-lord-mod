package com.realmlord.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lord's Manor as the village grows. At Grade 1 it is the hall where the first refugees
 * sleep on straw. From Grade 2 it is rebuilt as the Lord's own two-storey residence: a hall with
 * the manor seal and the Lord's seat, a storeroom of chests and barrels, a study, and upstairs the
 * Lord's bedchamber and guest beds. Grade 3 and above add an armoury corner, an enchanting table
 * and brewing stand in the study, more storage and guest beds, a balcony, banners and a higher roof.
 * The residents no longer sleep there; they live in the residential courtyards.
 */
public final class Residence {
    private Residence() {}

    /** Which manor a level calls for: 1 the refugees' hall, 2 the residence, 3 the grand residence. */
    public static int tierFor(TerritoryLevel l) {
        return l == TerritoryLevel.VILLAGE_1 ? 1 : l == TerritoryLevel.VILLAGE_2 ? 2 : 3;
    }

    /** The way the manor's door faces: stored, or found from the door of an older manor. */
    public static Direction front(ServerLevel level, Territory t) {
        if (t.manorFront >= 0) return Direction.from2DDataValue(t.manorFront);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int n : new int[]{4, 6}) {
                if (level.getBlockState(t.center.relative(d, n)).getBlock() instanceof DoorBlock) return d;
            }
        }
        return Direction.SOUTH;
    }

    /** Rebuilds the manor if the territory's level calls for a grander one. Returns true if it did. */
    public static boolean upgrade(ServerLevel level, Territory t) {
        int tier = tierFor(t.level);
        if (t.manorTier >= tier) return false;
        BlockPos c = t.center;
        for (int dx = -9; dx <= 9; dx += 9)
            for (int dz = -9; dz <= 9; dz += 9) if (!level.isLoaded(c.offset(dx, 0, dz))) return false;
        Direction front = front(level, t);
        build(level, t, front, tier);
        t.manorTier = tier;
        t.manorFront = front.get2DDataValue();
        com.realmlord.village.Residents.forgetBeds(t);
        return true;
    }

    // ------------------------------------------------------------------ building

    private static final class Plan {
        final ServerLevel level;
        final BlockPos c;
        final Direction f, r, back;

        Plan(ServerLevel level, BlockPos c, Direction f) {
            this.level = level;
            this.c = c;
            this.f = f;
            this.r = f.getClockWise();
            this.back = f.getOpposite();
        }

        /** u to the right, v to the front, y up from the floor. */
        BlockPos at(int u, int y, int v) {
            return c.relative(r, u).relative(f, v).offset(0, y, 0);
        }

        void set(int u, int y, int v, BlockState s) {
            level.setBlock(at(u, y, v), s, Block.UPDATE_CLIENTS);
        }

        void bed(Block bed, int u, int y, int headV) {
            // Head towards the back, foot one block in front of it.
            BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, back);
            set(u, y, headV, s.setValue(BedBlock.PART, BedPart.HEAD));
            set(u, y, headV + 1, s.setValue(BedBlock.PART, BedPart.FOOT));
        }

        void chest(int u, int y, int v, Direction facing) {
            set(u, y, v, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        }
    }

    private static final BlockState POST = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
    private static final BlockState WALL = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    private static final BlockState BEAM = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState FLOOR = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState GLASS = Blocks.GLASS_PANE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState TILE = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState TILE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    private static final BlockState RAIL = Blocks.DARK_OAK_FENCE.defaultBlockState();
    private static final BlockState HANGING = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

    public static void build(ServerLevel level, Territory t, Direction front, int tier) {
        Plan p = new Plan(level, t.center, front);
        rescue(p, t);
        // Clear the old manor: everything within the walls, and above the yard's edge for the eaves.
        for (int u = -7; u <= 7; u++)
            for (int v = -7; v <= 7; v++) {
                boolean edge = Math.abs(u) == 7 || Math.abs(v) == 7;
                for (int y = edge ? 4 : 0; y <= 16; y++) p.set(u, y, v, AIR);
            }
        // Platform: stone-brick edge, plank floor.
        for (int u = -6; u <= 6; u++)
            for (int v = -6; v <= 6; v++)
                p.set(u, -1, v, Math.abs(u) == 6 || Math.abs(v) == 6 ? Blocks.STONE_BRICKS.defaultBlockState() : FLOOR);
        walls(p, tier);
        groundFloor(p, tier);
        upperFloor(p, tier);
        roof(p, tier);
        outside(p, tier);
        // The manor seal, at the heart of the hall as before.
        level.setBlock(t.center, com.realmlord.registry.ModBlocks.LORDS_MANOR.get().defaultBlockState(), Block.UPDATE_ALL);
        com.realmlord.village.Sites.connectAll(level, t.center.offset(-9, -1, -9), t.center.offset(9, 22, 9));
    }

    /** Anything kept in the old manor goes to the village storehouse, and anyone inside steps out of the door. */
    private static void rescue(Plan p, Territory t) {
        List<ItemStack> saved = new ArrayList<>();
        for (int u = -7; u <= 7; u++)
            for (int v = -7; v <= 7; v++)
                for (int y = -1; y <= 16; y++) {
                    BlockEntity be = p.level.getBlockEntity(p.at(u, y, v));
                    if (be instanceof Container ct) {
                        for (int i = 0; i < ct.getContainerSize(); i++) {
                            ItemStack s = ct.removeItemNoUpdate(i);
                            if (!s.isEmpty()) saved.add(s);
                        }
                    }
                }
        BlockPos out = p.at(0, 0, 9);
        int gy = com.realmlord.village.Village.groundAt(p.level, out.getX(), out.getZ());
        for (ItemStack s : saved) {
            ItemStack left = t.storage.addItem(s);
            if (!left.isEmpty()) p.level.addFreshEntity(new ItemEntity(p.level, out.getX() + 0.5, gy + 0.5, out.getZ() + 0.5, left));
        }
        BlockPos a = p.at(-7, -1, -7), b = p.at(7, 16, 7);
        AABB box = new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, b.getY() + 1, Math.max(a.getZ(), b.getZ()) + 1);
        for (Entity e : p.level.getEntitiesOfClass(Entity.class, box, e -> e instanceof net.minecraft.world.entity.LivingEntity)) {
            e.teleportTo(out.getX() + 0.5, gy, out.getZ() + 0.5);
        }
    }

    private static void walls(Plan p, int tier) {
        for (int u = -6; u <= 6; u++)
            for (int v = -6; v <= 6; v++) {
                int au = Math.abs(u), av = Math.abs(v);
                if (au != 6 && av != 6) continue;
                boolean pillar = (au == 6 && (av == 6 || av == 3)) || (av == 6 && au == 3);
                for (int y = 0; y <= 8; y++) {
                    BlockState s;
                    if (pillar) s = POST;
                    else if (y == 0 || y == 4 || y == 8) s = BEAM;
                    else if ((y == 1 || y == 2 || y == 5 || y == 6) && ((au == 6 && (av == 1 || av == 4)) || (av == 6 && (au == 1 || au == 4)))) s = GLASS;
                    else s = WALL;
                    p.set(u, y, v, s);
                }
            }
        // The front door.
        BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, p.back)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        p.set(0, 0, 6, lower);
        p.set(0, 1, 6, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        p.set(0, 2, 6, BEAM);
    }

    private static void groundFloor(Plan p, int tier) {
        // Partitions: storeroom on the left, study and stairs on the right, the hall between.
        for (int v = -5; v <= 5; v++)
            for (int y = 0; y <= 3; y++) {
                boolean doorway = v == -3 && y <= 1;
                p.set(-2, y, v, doorway ? AIR : FLOOR);
                p.set(2, y, v, doorway ? AIR : FLOOR);
            }
        // The hall: red carpet from the door to the seal, the Lord's seat behind it.
        for (int v = 1; v <= 5; v++) p.set(0, 0, v, Blocks.RED_CARPET.defaultBlockState());
        p.set(0, 0, -3, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, p.back));
        p.set(-1, 0, -3, Blocks.DARK_OAK_SLAB.defaultBlockState());
        p.set(1, 0, -3, Blocks.DARK_OAK_SLAB.defaultBlockState());
        p.set(0, 0, -5, BEAM);
        p.set(0, 1, -5, Blocks.GOLD_BLOCK.defaultBlockState());
        for (int u : new int[]{-1, 1}) {
            p.set(u, 0, -5, RAIL);
            p.set(u, 1, -5, Blocks.LANTERN.defaultBlockState());
        }
        p.set(0, 3, 3, HANGING);
        p.set(0, 3, -1, HANGING);
        // Storeroom: chests and barrels along the walls.
        for (int v = -5; v <= 5; v++) {
            if (v % 2 == 0) {
                p.chest(-5, 0, v, p.r);
                if (tier >= 3) p.chest(-5, 1, v, p.r);
            } else {
                p.set(-5, 0, v, Blocks.BARREL.defaultBlockState());
                p.set(-5, 1, v, Blocks.BARREL.defaultBlockState());
            }
        }
        p.chest(-4, 0, -5, p.f);
        p.chest(-3, 0, -5, p.f);
        if (tier >= 3) {
            p.chest(-4, 1, -5, p.f);
            p.chest(-3, 1, -5, p.f);
            // The armoury corner.
            p.set(-4, 0, 5, Blocks.ANVIL.defaultBlockState());
            p.set(-3, 0, 5, Blocks.SMITHING_TABLE.defaultBlockState());
            p.set(-3, 0, 4, Blocks.GRINDSTONE.defaultBlockState());
        } else {
            p.set(-3, 0, 5, Blocks.CRAFTING_TABLE.defaultBlockState());
        }
        p.set(-4, 3, 0, HANGING);
        p.set(-4, 3, -4, HANGING);
        // Study: bookshelves, a lectern and the map table.
        for (int v = -5; v <= -1; v++) for (int y = 0; y <= 2; y++) p.set(5, y, v, Blocks.BOOKSHELF.defaultBlockState());
        p.set(3, 0, -5, Blocks.LECTERN.defaultBlockState());
        p.set(4, 0, -5, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        if (tier >= 3) {
            p.set(3, 0, -2, Blocks.ENCHANTING_TABLE.defaultBlockState());
            p.set(3, 0, 5, Blocks.BREWING_STAND.defaultBlockState());
        }
        p.set(4, 3, -3, HANGING);
        // Stairs up along the study's middle, rising towards the back.
        for (int k = 0; k <= 4; k++) {
            p.set(4, k, 4 - k, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, p.back));
        }
    }

    private static void upperFloor(Plan p, int tier) {
        for (int u = -5; u <= 5; u++)
            for (int v = -5; v <= 5; v++) {
                if (u == 4 && v >= 0 && v <= 4) continue; // the stairwell (the top step is at v 0)
                p.set(u, 4, v, FLOOR);
            }
        for (int v = 1; v <= 4; v++) {
            p.set(3, 5, v, RAIL);
            p.set(5, 5, v, RAIL);
        }
        p.set(4, 5, 5, RAIL);
        // The Lord's bedchamber at the back: a wide red bed, wardrobes and a rug.
        p.bed(Blocks.RED_BED, -4, 5, -5);
        p.bed(Blocks.RED_BED, -3, 5, -5);
        p.chest(-5, 5, -5, p.r);
        p.chest(-5, 5, -4, p.r);
        for (int u = -5; u <= -2; u++) for (int v = -3; v <= -2; v++) p.set(u, 5, v, Blocks.RED_CARPET.defaultBlockState());
        p.set(-2, 5, -5, Blocks.POTTED_AZALEA.defaultBlockState());
        // Guest beds at the front.
        p.bed(Blocks.WHITE_BED, -5, 5, 1);
        p.bed(Blocks.WHITE_BED, -5, 5, 4);
        if (tier >= 3) {
            p.bed(Blocks.WHITE_BED, -3, 5, 1);
            p.bed(Blocks.WHITE_BED, -3, 5, 4);
        }
        // A reading corner on the right.
        for (int v = -5; v <= -3; v++) for (int y = 5; y <= 6; y++) p.set(5, y, v, Blocks.BOOKSHELF.defaultBlockState());
        p.set(3, 5, -5, Blocks.POTTED_BAMBOO.defaultBlockState());
        p.set(-3, 8, 0, HANGING);
        p.set(2, 8, -3, HANGING);
        p.set(0, 8, 3, HANGING);
        if (tier >= 3) {
            // A balcony over the door.
            BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, p.back)
                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            p.set(0, 5, 6, lower);
            p.set(0, 6, 6, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            BlockState deck = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            for (int u = -2; u <= 2; u++) {
                p.set(u, 4, 7, deck);
                p.set(u, 4, 8, deck);
                p.set(u, 5, 8, RAIL);
            }
            p.set(-2, 5, 7, RAIL);
            p.set(2, 5, 7, RAIL);
        }
    }

    private static void roof(Plan p, int tier) {
        int[][] tiers = tier >= 3
                ? new int[][]{{7, 6}, {6, 5}, {5, 4}, {4, 2}, {2, 1}}
                : new int[][]{{7, 6}, {6, 5}, {5, 3}, {3, 2}};
        for (int i = 0; i < tiers.length; i++) {
            int outer = tiers[i][0], inner = tiers[i][1], y = 9 + i;
            for (int u = -outer; u <= outer; u++)
                for (int v = -outer; v <= outer; v++) {
                    int ring = Math.max(Math.abs(u), Math.abs(v));
                    p.set(u, y, v, ring <= inner ? TILE : TILE_SLAB);
                }
        }
        p.set(0, 9 + tiers.length, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        if (tier >= 3) for (int u : new int[]{-7, 7}) for (int v : new int[]{-7, 7}) p.set(u, 8, v, HANGING);
    }

    private static void outside(Plan p, int tier) {
        for (int u : new int[]{-2, 2}) {
            p.set(u, 0, 7, POST);
            p.set(u, 1, 7, Blocks.LANTERN.defaultBlockState());
        }
        if (tier >= 3) {
            for (int u : new int[]{-3, 3}) p.set(u, 3, 7, Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, p.f));
            p.set(0, -1, 7, Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
    }
}
