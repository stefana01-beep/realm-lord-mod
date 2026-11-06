package com.realmlord.village;

import com.realmlord.entity.SettlerEntity;
import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The work on a building site, one block at a time. A new site is first cleared (trees, plants
 * and high ground above the footprint), then shaped (holes and slopes beneath it filled), then
 * built from its design. A building being demolished comes down top first, its materials
 * carried back to the stores. Builders do this by hand; when none is there to do it, the
 * village does it on its own so that work goes on while the Lord is away.
 */
public final class Sites {
    private Sites() {}

    /** Clearing reaches this high above the footprint; shaping fills this deep below it. */
    public static final int HEIGHT = 16, DEPTH = 12;
    /** A site left this long with builders nearby but nothing done is taken over by the village. */
    public static final long STALL_TICKS = 600;
    /** Share of a building's wood and stone that taking it down returns. */
    public static final double SALVAGE = 0.75;

    public enum Kind { CLEAR, FILL, PLACE, REMOVE, REPAIR }

    public record Op(Kind kind, BlockPos pos, BlockState state, int index) {}

    /** The site builders work on now: the oldest one being built or taken down. */
    public static Building active(Territory t) {
        for (Building b : t.buildings) if (!b.complete) return b;
        return null;
    }

    public static boolean loaded(ServerLevel level, Building b) {
        if (!b.footprint()) return level.isLoaded(b.origin);
        return level.isLoaded(b.origin) && level.isLoaded(b.origin.offset(b.sizeX() - 1, 0, b.sizeZ() - 1))
                && level.isLoaded(b.center());
    }

    // ------------------------------------------------------------------ finding the next job

    /** The next block job on a site, or null if there is nothing to do yet. */
    public static Op next(ServerLevel level, Territory t, Building b) {
        if (!loaded(level, b)) return null;
        if (b.demolishing) return nextRemove(level, t, b);
        if (b.stage == Building.CLEAR) {
            Op o = b.footprint() ? nextClear(level, b) : null;
            if (o != null) return o;
            b.stage = Building.SHAPE;
        }
        if (b.stage == Building.SHAPE) {
            Op o = b.footprint() ? nextFill(level, b) : null;
            if (o != null) return o;
            b.stage = Building.BUILD;
        }
        return nextPlace(level, t, b);
    }

    /** Anything standing on the footprint, highest first. */
    private static Op nextClear(ServerLevel level, Building b) {
        int y0 = b.origin.getY();
        // Working down layer by layer: start from the layer reached, not the top, every time.
        for (int y = Math.min(b.clearTop, y0 + height(b) - 1); y >= y0; y--) {
            for (int x = 0; x < b.sizeX(); x++)
                for (int z = 0; z < b.sizeZ(); z++) {
                    BlockPos p = b.origin.offset(x, y - y0, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir()) {
                        b.clearTop = y;
                        return new Op(Kind.CLEAR, p, s, -1);
                    }
                }
        }
        b.clearTop = Integer.MAX_VALUE;
        return null;
    }

    /** How high above its floor a site is cleared. */
    public static int height(Building b) {
        return b.type == BuildingType.CLEARING && b.clearHeight > 0 ? b.clearHeight : HEIGHT;
    }

    private static boolean solid(BlockState s) {
        return !s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    /** The lowest gap beneath the footprint's floor. */
    private static Op nextFill(ServerLevel level, Building b) {
        int y0 = b.origin.getY();
        for (int x = 0; x < b.sizeX(); x++)
            for (int z = 0; z < b.sizeZ(); z++) {
                BlockPos top = b.origin.offset(x, -1, z);
                if (solid(level.getBlockState(top))) continue;
                int y = y0 - 2;
                while (y > y0 - 1 - DEPTH && !solid(level.getBlockState(new BlockPos(top.getX(), y, top.getZ())))) y--;
                BlockPos p = new BlockPos(top.getX(), y + 1, top.getZ());
                return new Op(Kind.FILL, p, p.getY() == y0 - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState(), -1);
            }
        return null;
    }

    /** How many design blocks the work done so far allows to be placed. */
    public static int target(Building b, int size) {
        if (b.progress >= b.workNeeded) return size;
        return (int) Math.floor(size * b.fraction());
    }

    private static Op nextPlace(ServerLevel level, Territory t, Building b) {
        List<BuildingDesigns.Placement> design = BuildingDesigns.design(level, t, b);
        if (design == null) return null;
        int target = target(b, design.size());
        while (b.placed < target) {
            BuildingDesigns.Placement p = design.get(b.placed);
            // Never skip a block whose ground is not loaded: wait for it instead, so no gaps are left.
            if (!level.isLoaded(p.pos())) return null;
            if (level.getBlockState(p.pos()) == p.state()) {
                b.placed++;
                continue;
            }
            return new Op(Kind.PLACE, p.pos(), p.state(), b.placed);
        }
        if (b.placed >= design.size() && b.progress >= b.workNeeded) return nextRepair(level, b, design);
        return null;
    }

    /**
     * The last look over a finished site: anything missing or knocked out of place (a block
     * that could not go in, something that grew or fell into the way) is put right before the
     * building counts as done.
     */
    private static Op nextRepair(ServerLevel level, Building b, List<BuildingDesigns.Placement> design) {
        for (int i = Math.max(0, b.verifyCursor); i < design.size(); i++) {
            BuildingDesigns.Placement p = design.get(i);
            if (!level.isLoaded(p.pos())) {
                b.verifyCursor = i;
                return null;
            }
            BlockState now = level.getBlockState(p.pos());
            boolean same = p.state().isAir() ? now.isAir() : now.getBlock() == p.state().getBlock();
            if (!same) {
                b.verifyCursor = i;
                return new Op(Kind.REPAIR, p.pos(), p.state(), i);
            }
        }
        b.verified = true;
        return null;
    }

    private static Op nextRemove(ServerLevel level, Territory t, Building b) {
        List<BuildingDesigns.Placement> design = BuildingDesigns.design(level, t, b);
        if (design == null) return null;
        b.placed = Math.min(b.placed, design.size());
        while (b.placed > 0) {
            BuildingDesigns.Placement p = design.get(b.placed - 1);
            BlockState now = level.getBlockState(p.pos());
            if (!p.state().isAir() && !now.isAir() && now.getBlock() == p.state().getBlock()) {
                return new Op(Kind.REMOVE, p.pos(), now, b.placed - 1);
            }
            // Already gone (or never there): its share of the salvage still comes back.
            b.placed--;
            salvage(t, b, null);
        }
        demolished(t, b);
        return null;
    }

    // ------------------------------------------------------------------ doing it

    /**
     * Does one block job. Drops worth keeping go into the worker's pack (or straight to the
     * stores when the village works on its own). Returns false if it could not be done now.
     */
    public static boolean apply(ServerLevel level, Territory t, Building b, Op op, SettlerEntity worker) {
        Resident r = worker == null ? null : worker.resident();
        switch (op.kind()) {
            case CLEAR -> {
                BlockState s = level.getBlockState(op.pos());
                if (s.isAir()) return true;
                if (isNaturalLog(s)) {
                    for (BlockPos log : tree(level, op.pos())) take(level, t, r, log, worker);
                } else {
                    take(level, t, r, op.pos(), worker);
                }
            }
            case FILL -> {
                if (solid(level.getBlockState(op.pos()))) return true;
                // Earth dug from other sites goes back into the ground first.
                if (Goods.EARTH.stock(t) > 0) Goods.EARTH.add(t, -1);
                level.setBlock(op.pos(), op.state(), Block.UPDATE_ALL);
                sound(level, op.pos(), op.state(), false);
            }
            case PLACE, REPAIR -> {
                if (op.kind() == Kind.PLACE && op.index() != b.placed) return true;
                BlockState want = op.state();
                if (worker != null && !want.getCollisionShape(level, op.pos()).isEmpty() && occupied(level, op.pos())) return false;
                BlockState was = level.getBlockState(op.pos());
                if (isNaturalLog(was)) {
                    // A tree in the way comes down whole, not just the block that is needed.
                    for (BlockPos log : tree(level, op.pos())) take(level, t, r, log, worker);
                } else if (!was.isAir() && was.getBlock() != want.getBlock()) {
                    keepDrops(level, t, r, op.pos(), was);
                }
                level.setBlock(op.pos(), want, Block.UPDATE_ALL);
                if (!want.isAir()) sound(level, op.pos(), want, false);
                if (op.kind() == Kind.PLACE) b.placed++;
                else b.verifyCursor = op.index() + 1;
            }
            case REMOVE -> {
                if (op.index() != b.placed - 1) return true;
                emptyContainer(level, op.pos());
                level.destroyBlock(op.pos(), false, worker);
                b.placed--;
                salvage(t, b, r);
                if (b.placed <= 0) demolished(t, b);
            }
        }
        b.lastWork = level.getGameTime();
        return true;
    }

    /**
     * The village working a site on its own: up to budget block jobs, straight from and to the
     * stores. Clearing and shaping go in one sweep; building keeps to the work done so far.
     */
    public static void work(ServerLevel level, Territory t, Building b, int budget) {
        if (!loaded(level, b)) return;
        if (!b.demolishing && b.stage == Building.CLEAR && b.footprint()) {
            int y0 = b.origin.getY();
            for (int y = Math.min(b.clearTop, y0 + height(b) - 1); y >= y0 && budget > 0; y--)
                for (int x = 0; x < b.sizeX() && budget > 0; x++)
                    for (int z = 0; z < b.sizeZ() && budget > 0; z++) {
                        BlockPos p = b.origin.offset(x, y - y0, z);
                        BlockState s = level.getBlockState(p);
                        if (s.isAir()) continue;
                        if (isNaturalLog(s)) for (BlockPos log : tree(level, p)) take(level, t, null, log, null);
                        else take(level, t, null, p, null);
                        budget--;
                        b.clearTop = y;
                    }
            if (budget <= 0) return;
            b.clearTop = Integer.MAX_VALUE;
            b.stage = Building.SHAPE;
        }
        for (int i = 0; i < budget; i++) {
            Op op = next(level, t, b);
            if (op == null || !apply(level, t, b, op, null)) return;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Breaks a block, keeping what the stores would take. */
    private static void take(ServerLevel level, Territory t, Resident r, BlockPos p, Entity breaker) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof LiquidBlock) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        keepDrops(level, t, r, p, s);
        emptyContainer(level, p);
        level.destroyBlock(p, false, breaker);
    }

    private static void keepDrops(ServerLevel level, Territory t, Resident r, BlockPos p, BlockState s) {
        BlockEntity be = level.getBlockEntity(p);
        for (ItemStack drop : Block.getDrops(s, level, p, be)) toStores(t, drop);
    }

    /**
     * What clearing a site turns up goes straight to the village's stores: timber, stone, ore,
     * sand, clay and earth as stock, saplings to the storehouse for replanting. The rest is left.
     */
    public static void toStores(Territory t, ItemStack drop) {
        Goods.Unit u = Goods.of(drop);
        if (u != null) {
            u.good().add(t, (long) u.units() * drop.getCount());
        } else if (drop.is(net.minecraft.tags.ItemTags.SAPLINGS)) {
            t.storage.addItem(drop);
        }
    }

    /** Into the worker's pack; what does not fit, or with no worker, straight into the stores. */
    public static void store(Territory t, Resident r, ItemStack s) {
        ItemStack left = r == null ? s : Haul.give(r, s);
        if (left.isEmpty()) return;
        int[] v = Haul.value(left);
        if (v == null) return;
        long units = (long) v[1] * left.getCount();
        switch (v[0]) {
            case Haul.WOOD -> t.wood += units;
            case Haul.STONE -> t.stone += units;
            case Haul.ORE -> t.ironOre += units;
            default -> t.food += units;
        }
    }

    private static void emptyContainer(ServerLevel level, BlockPos p) {
        if (level.getBlockEntity(p) instanceof Container c) Containers.dropContents(level, p, c);
    }

    private static void sound(ServerLevel level, BlockPos p, BlockState s, boolean breaking) {
        SoundType st = s.getSoundType();
        level.playSound(null, p, breaking ? st.getBreakSound() : st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume() + 1.0F) / 2.0F, st.getPitch() * 0.8F);
    }

    private static boolean occupied(ServerLevel level, BlockPos p) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(p), e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }

    /** The salvage for the block just taken down, as planks and cobblestone. */
    private static void salvage(Territory t, Building b, Resident r) {
        int n = Math.max(1, b.demolishFrom);
        int done = b.demolishFrom - b.placed;
        int wood = share(b.salvageWood, done, n), stone = share(b.salvageStone, done, n);
        if (wood > 0) store(t, r, new ItemStack(Items.OAK_PLANKS, wood));
        if (stone > 0) store(t, r, new ItemStack(Items.COBBLESTONE, stone));
    }

    private static int share(int total, int done, int n) {
        if (done <= 0 || done > n) return 0;
        return (int) ((long) total * done / n - (long) total * (done - 1) / n);
    }

    /** The last block is down: the building is gone from the village. */
    private static void demolished(Territory t, Building b) {
        if (!t.buildings.remove(b)) return;
        BuildingDesigns.forget(b);
        for (Resident r : t.residents) if (r.workplace == b.id) {
            if (r.job != Job.CONSTRUCTION) r.job = Job.IDLE;
            r.workplace = -1;
        }
    }

    // ------------------------------------------------------------------ trees

    /** Any unstripped log, upright or a branch. */
    public static boolean isTreeLog(BlockState s) {
        if (!s.is(BlockTags.LOGS)) return false;
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return key != null && !key.getPath().contains("stripped") && !key.getPath().contains("wood");
    }

    /** An upright log that could be a tree trunk (not a stripped post or a log lying in a pile). */
    public static boolean isNaturalLog(BlockState s) {
        return isTreeLog(s) && (!s.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                || s.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y);
    }

    /** The logs of the tree a log belongs to, top first (at most 64). */
    public static List<BlockPos> tree(ServerLevel level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && out.size() < 64) {
            BlockPos p = queue.poll();
            out.add(p);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = p.offset(dx, dy, dz);
                        if (seen.add(q) && Math.abs(q.getX() - start.getX()) <= 6 && Math.abs(q.getZ() - start.getZ()) <= 6
                                && level.isLoaded(q) && isTreeLog(level.getBlockState(q))) queue.add(q);
                    }
        }
        out.sort((a, c) -> Integer.compare(c.getY(), a.getY()));
        return out;
    }

    /** True if leaves grow around a log, i.e. it belongs to a living tree. */
    public static boolean hasLeaves(ServerLevel level, BlockPos top) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = 0; dy <= 2; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    if (level.getBlockState(top.offset(dx, dy, dz)).is(BlockTags.LEAVES)) return true;
        return false;
    }
}
