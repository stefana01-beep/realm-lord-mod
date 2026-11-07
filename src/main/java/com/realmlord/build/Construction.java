package com.realmlord.build;

import com.realmlord.RealmLord;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.work.TechManuals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Raising a building from a plan, a few dozen blocks a tick, paid for from the builder's pack
 * (nothing in creative). First the ground is cleared where the plan has air, then solid blocks
 * go up, then everything that hangs on them (doors, ladders, lanterns, stairs). An architect's
 * hand saves some materials. What can't be paid for is left out and listed at the end.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Construction {
    private Construction() {}

    public static final int PER_TICK = 24;

    static final class Job {
        final ServerLevel level;
        final UUID player;
        final BlockPos origin;
        final Design design;
        final Component name;
        final boolean free;
        int cursor, placed;
        final Map<Item, Integer> missing = new LinkedHashMap<>();

        Job(ServerLevel level, UUID player, BlockPos origin, Design design, Component name, boolean free) {
            this.level = level;
            this.player = player;
            this.origin = origin;
            this.design = design;
            this.name = name;
            this.free = free;
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    /** What a block costs to place: an item stack, EMPTY for nothing, or null if it can't be built from items. */
    public static ItemStack cost(BlockState s) {
        if (s.isAir()) return ItemStack.EMPTY;
        Block b = s.getBlock();
        if (b instanceof LiquidBlock) {
            return s.getFluidState().is(Fluids.WATER) && s.getFluidState().isSource() ? new ItemStack(Items.WATER_BUCKET) : null;
        }
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return ItemStack.EMPTY;
        if (s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) return ItemStack.EMPTY;
        if (b == Blocks.FARMLAND || b == Blocks.DIRT_PATH) return new ItemStack(Items.DIRT);
        if (b instanceof AbstractCauldronBlock) return new ItemStack(Items.CAULDRON);
        if (b instanceof FlowerPotBlock) return new ItemStack(Items.FLOWER_POT);
        if (s.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 0) return null;
        Item i = b.asItem();
        if (i == Items.AIR) return null;
        int n = b instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1;
        return new ItemStack(i, n);
    }

    /** What a whole design needs. */
    public static Map<Item, Integer> materials(Design d) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        for (int c : d.cells) {
            if (c <= 0) continue;
            ItemStack s = cost(d.palette[c]);
            if (s != null && !s.isEmpty()) out.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        return out;
    }

    /** The turn that makes a design's front face a player looking this way. */
    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Where a turned design's lowest corner goes, built in front of a player at the clicked block. */
    public static BlockPos originFor(Design turned, BlockPos clicked, Direction facing) {
        int y = clicked.getY() + 1 + turned.yOff;
        return switch (facing) {
            case NORTH -> new BlockPos(clicked.getX() - turned.w / 2, y, clicked.getZ() - turned.d + 1);
            case EAST -> new BlockPos(clicked.getX(), y, clicked.getZ() - turned.d / 2);
            case WEST -> new BlockPos(clicked.getX() - turned.w + 1, y, clicked.getZ() - turned.d / 2);
            default -> new BlockPos(clicked.getX() - turned.w / 2, y, clicked.getZ());
        };
    }

    public static void start(ServerPlayer p, Design turned, BlockPos origin, Component name) {
        JOBS.add(new Job(p.serverLevel(), p.getUUID(), origin, turned, name, p.getAbilities().instabuild));
        p.displayClientMessage(Component.translatable("realmlord.build.started", name, turned.blocks()).withStyle(ChatFormatting.GOLD), false);
    }

    public static boolean busy(UUID player) {
        for (Job j : JOBS) if (j.player.equals(player)) return true;
        return false;
    }

    private static int count(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static void take(ServerPlayer p, Item item, int n) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) continue;
            int k = Math.min(n, s.getCount());
            s.shrink(k);
            n -= k;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            ServerPlayer p = j.level.getServer().getPlayerList().getPlayer(j.player);
            if (p == null) {
                it.remove();
                continue;
            }
            if (work(j, p)) {
                it.remove();
                finish(j, p);
            }
        }
    }

    /** Builds a design at once (for tests and commands); returns the blocks placed. */
    public static int buildNow(ServerPlayer p, Design turned, BlockPos origin) {
        Job j = new Job(p.serverLevel(), p.getUUID(), origin, turned, Component.empty(), p.getAbilities().instabuild);
        while (!work(j, p)) { }
        return j.placed;
    }

    /** Builds some of the job; true when it is done. */
    static boolean work(Job j, ServerPlayer p) {
        Design d = j.design;
        int n = d.cells.length, done = 0;
        int tier = TechManuals.tradeTier(p, WorkOccupation.ARCHITECT);
        float saving = (tier >= 0 ? 0.05F * (tier + 1) : 0) + (TechManuals.knows(p, "yingzao_fashi") ? 0.2F : 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (done < PER_TICK && j.cursor < 3 * n) {
            int pass = j.cursor / n, i = j.cursor % n;
            j.cursor++;
            int c = d.cells[i];
            if (c < 0) continue;
            int x = i % d.w, z = (i / d.w) % d.d, y = i / (d.w * d.d);
            pos.set(j.origin.getX() + x, j.origin.getY() + y, j.origin.getZ() + z);
            if (!j.level.isLoaded(pos)) continue;
            BlockState cur = j.level.getBlockState(pos);
            if (pass == 0) {
                // Clear the ground where the plan has air (dropping what was there).
                if (c == 0 && !cur.isAir() && j.level.getBlockEntity(pos) == null) {
                    float hard = cur.getDestroySpeed(j.level, pos);
                    if (hard >= 0 && hard <= 3.0F) {
                        j.level.destroyBlock(pos, true);
                        done++;
                    }
                }
                continue;
            }
            if (c == 0) continue;
            BlockState target = d.palette[c];
            boolean solid = target.isSolidRender(j.level, pos);
            if (pass == 1 != solid) continue;
            if (cur == target) continue;
            ItemStack cost = cost(target);
            if (cost == null) continue;
            boolean pay = !j.free && !cost.isEmpty() && !(saving > 0 && p.getRandom().nextFloat() < saving);
            if (pay) {
                if (count(p, cost.getItem()) < cost.getCount()) {
                    j.missing.merge(cost.getItem(), cost.getCount(), Integer::sum);
                    continue;
                }
                take(p, cost.getItem(), cost.getCount());
                if (cost.is(Items.WATER_BUCKET) && !p.getInventory().add(new ItemStack(Items.BUCKET))) p.drop(new ItemStack(Items.BUCKET), false);
            }
            if (!cur.isAir() && j.level.getBlockEntity(pos) == null && !cur.canBeReplaced()) j.level.destroyBlock(pos, true);
            j.level.setBlock(pos, target, Block.UPDATE_ALL);
            j.placed++;
            done++;
        }
        return j.cursor >= 3 * n;
    }

    private static void finish(Job j, ServerPlayer p) {
        p.displayClientMessage(Component.translatable("realmlord.build.done", j.name, j.placed).withStyle(ChatFormatting.GREEN), false);
        if (!j.missing.isEmpty()) p.displayClientMessage(Component.translatable("realmlord.build.missing", list(j.missing, 8)).withStyle(ChatFormatting.RED), false);
        if (TechManuals.tradeTier(p, WorkOccupation.ARCHITECT) >= 0) com.realmlord.profile.Profiles.grantWorkXp(p, Math.max(2, j.placed / 25));
    }

    /** "12 Spruce Planks, 4 Glass Pane, ..." */
    public static MutableComponent list(Map<Item, Integer> m, int max) {
        MutableComponent c = Component.empty();
        int i = 0;
        for (var e : m.entrySet()) {
            if (i == max) {
                c.append(Component.translatable("realmlord.build.more", m.size() - max));
                break;
            }
            if (i++ > 0) c.append(", ");
            c.append(e.getValue() + " ").append(e.getKey().getDescription());
        }
        return c;
    }
}
