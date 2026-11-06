package com.realmlord.territory;

import com.realmlord.Notify;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.registry.ModBlocks;
import com.realmlord.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

/** Turning a Village Creation Token into a village: checks, the Lord's Manor and the rewards. */
public final class Founding {
    private Founding() {}

    /** Minimum gap between two territories' borders, in blocks. */
    public static final int BORDER_GAP = 32;
    public static final int DEFAULT_POPULATION = 10;

    /** Merit for the n-th village founded in the world: 1100 for the first, 100 less each, at least 100. */
    public static int foundingMerit(int order) {
        return Math.max(100, 1100 - 100 * (order - 1));
    }

    /**
     * Founds a village for this player at the given spot. Returns the new territory, or null
     * after telling the player why it could not be done.
     */
    public static Territory found(ServerPlayer player, BlockPos target, TokenGrade grade, String customName) {
        Profile prof = Profiles.get(player);
        ServerLevel level = player.serverLevel();
        TerritoryData data = TerritoryData.get(player.server);
        if (!prof.isLord()) {
            Notify.player(player, "realmlord.found.not_lord");
            return null;
        }
        if (prof.getTerritoryId() >= 0 && data.byId(prof.getTerritoryId()) != null) {
            Notify.player(player, "realmlord.found.already");
            return null;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            Notify.player(player, "realmlord.found.overworld");
            return null;
        }
        int radius = TerritoryLevel.VILLAGE_1.radius;
        double gap = data.gapToNearest(level.dimension(), target, radius);
        if (gap < BORDER_GAP) {
            Notify.player(player, "realmlord.found.too_close", (int) Math.ceil(BORDER_GAP - gap));
            return null;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos center = new BlockPos(target.getX(), y, target.getZ());
        Direction front = Direction.getNearest(player.getX() - center.getX(), 0, player.getZ() - center.getZ());
        if (front.getAxis() == Direction.Axis.Y) front = Direction.SOUTH;
        buildManor(level, center, front);

        Territory t = new Territory();
        t.owner = player.getUUID();
        t.ownerName = player.getGameProfile().getName();
        t.name = customName != null && !customName.isBlank() ? customName : Component.translatable("realmlord.territory.default_name", t.ownerName).getString();
        t.grade = grade;
        t.level = TerritoryLevel.VILLAGE_1;
        t.center = center;
        t.dimension = level.dimension();
        t.population = prof.quest().captives > 0 ? prof.quest().captives * 5 : DEFAULT_POPULATION;
        t.foundedTick = level.getGameTime();
        data.add(t);
        prof.setTerritoryId(t.id);
        bringSettlers(level, player, center, prof.quest().captives);
        com.realmlord.village.Village.settle(t, level.getRandom(), prof.quest().captives);
        t.lastDay = com.realmlord.village.GaiaCalendar.day(level.getServer().overworld().getGameTime());

        int merit = foundingMerit(t.foundedOrder);
        Component ordinal = Component.translatable("realmlord.ordinal", t.foundedOrder);
        if (t.foundedOrder <= 10) {
            Notify.world(player.server, Component.translatable("realmlord.found.world", player.getDisplayName(), ordinal,
                    Component.literal(t.name), Component.translatable(grade.key()), merit));
        } else {
            Notify.player(player, Component.translatable("realmlord.found.private", Component.literal(t.name), Component.translatable(grade.key())));
        }
        Profiles.grantMerit(player, merit, Component.translatable("realmlord.reason.founding"));
        level.playSound(null, center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        Profiles.sync(player);
        return t;
    }

    /**
     * The settlers freed from the bandits move into the new village: any still waiting at the
     * camp leave it, and join the village's records.
     */
    public static void bringSettlers(ServerLevel level, ServerPlayer player, BlockPos center, int captives) {
        java.util.UUID owner = player.getUUID();
        for (SettlerEntity s : level.getEntities(ModEntities.SETTLER.get(), e -> owner.equals(e.getOwner()) && e.getHome() == null)) {
            s.discard();
        }
        // The village's residents now appear from its records (see Residents.sync).
    }

    /**
     * A small Lord's Manor: a levelled yard, a timber hall with red pillars and a tiered dark
     * roof, and the manor seal at its heart.
     */
    public static void buildManor(ServerLevel level, BlockPos c, Direction front) {
        int x0 = c.getX(), y0 = c.getY(), z0 = c.getZ();
        BlockState air = Blocks.AIR.defaultBlockState();
        // Level the yard: clear above, fill below.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                for (int y = y0; y <= y0 + 12; y++) set(level, x0 + dx, y, z0 + dz, air);
                for (int y = y0 - 1; y >= y0 - 8; y--) {
                    BlockPos p = new BlockPos(x0 + dx, y, z0 + dz);
                    BlockState s = level.getBlockState(p);
                    if (y == y0 - 1) {
                        level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        break;
                    }
                }
            }
        }
        // Stone-paved platform and floor.
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++)
                set(level, x0 + dx, y0 - 1, z0 + dz, Math.abs(dx) == 5 || Math.abs(dz) == 5
                        ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
        // Walls with red pillars at the corners and windows on each side.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                if (!edge) continue;
                boolean corner = Math.abs(dx) == 4 && Math.abs(dz) == 4;
                for (int y = 0; y <= 3; y++) {
                    BlockState s;
                    if (corner) s = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
                    else if ((y == 1 || y == 2) && (dx == 0 || dz == 0 || Math.abs(dx) == 2 || Math.abs(dz) == 2)) s = Blocks.GLASS_PANE.defaultBlockState();
                    else if (y == 0 || y == 3) s = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    else s = Blocks.WHITE_TERRACOTTA.defaultBlockState();
                    set(level, x0 + dx, y0 + y, z0 + dz, s);
                }
            }
        }
        // Door on the front side.
        BlockPos door = c.relative(front, 4);
        set(level, door.getX(), y0 + 2, door.getZ(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, front.getOpposite())
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(door, lower, Block.UPDATE_CLIENTS);
        level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        // Red carpet from the door to the seal.
        for (int i = 1; i <= 3; i++) {
            BlockPos p = c.relative(front, i);
            set(level, p.getX(), y0, p.getZ(), Blocks.RED_CARPET.defaultBlockState());
        }
        // Tiered roof with wide eaves.
        BlockState slab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState tile = Blocks.DEEPSLATE_TILES.defaultBlockState();
        int[][] tiers = {{6, 5}, {5, 4}, {4, 3}, {2, 2}};
        for (int i = 0; i < tiers.length; i++) {
            int outer = tiers[i][0], inner = tiers[i][1];
            int y = y0 + 4 + i;
            for (int dx = -outer; dx <= outer; dx++)
                for (int dz = -outer; dz <= outer; dz++) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dz));
                    if (ring <= inner) set(level, x0 + dx, y, z0 + dz, tile);
                    else set(level, x0 + dx, y, z0 + dz, slab);
                }
        }
        set(level, x0, y0 + 8, z0, Blocks.GOLD_BLOCK.defaultBlockState());
        // Lanterns hanging inside, a pair outside the door.
        BlockState hanging = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int[] l : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) set(level, x0 + l[0], y0 + 3, z0 + l[1], hanging);
        Direction side = front.getClockWise();
        for (int s = -1; s <= 1; s += 2) {
            BlockPos post = c.relative(front, 5).relative(side, 2 * s);
            set(level, post.getX(), y0, post.getZ(), Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState());
            set(level, post.getX(), y0 + 1, post.getZ(), Blocks.LANTERN.defaultBlockState());
        }
        furnish(level, c, front);
        // The manor seal.
        level.setBlock(c, ModBlocks.LORDS_MANOR.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Straw beds in the manor hall for the first ten residents, as in the novel's early village
     * where the refugees sleep in the Lord's Manor: seven along the back wall, two on each side.
     */
    public static void furnish(ServerLevel level, BlockPos c, Direction front) {
        Direction back = front.getOpposite(), side = front.getClockWise();
        java.util.List<BlockPos[]> beds = new java.util.ArrayList<>();
        for (int i = -3; i <= 3; i++) beds.add(new BlockPos[]{c.relative(back, 2).relative(side, i), c.relative(back, 3).relative(side, i)});
        for (int s = -1; s <= 1; s += 2)
            for (int f = 0; f <= 1; f++) {
                BlockPos foot = c.relative(front, f).relative(side, 2 * s);
                beds.add(new BlockPos[]{foot, foot.relative(side, s)});
            }
        for (BlockPos[] b : beds) {
            if (!level.getBlockState(b[0]).isAir() || !level.getBlockState(b[1]).isAir()) continue;
            Direction head = Direction.fromDelta(b[1].getX() - b[0].getX(), 0, b[1].getZ() - b[0].getZ());
            if (head == null) continue;
            BlockState foot = Blocks.YELLOW_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, head)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
            level.setBlock(b[0], foot, Block.UPDATE_CLIENTS);
            level.setBlock(b[1], foot.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), Block.UPDATE_CLIENTS);
        }
    }

    /** Beds for a manor built before there were any: finds its door to know which way it faces. */
    public static void furnishOld(ServerLevel level, BlockPos c) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(c.relative(d, 4)).getBlock() instanceof DoorBlock) {
                furnish(level, c, d);
                return;
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) {
        level.setBlock(new BlockPos(x, y, z), s, Block.UPDATE_CLIENTS);
    }
}
