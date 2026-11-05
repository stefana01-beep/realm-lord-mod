package com.realmlord.quest;

import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.loot.Blueprints;
import com.realmlord.loot.Gear;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.TokenGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

/**
 * The bandit camp of the Village Creation quest: tents around a campfire, a barred pen of
 * captive settlers, barrels of stolen supplies, a band of bandits with their chief, and the
 * chief's treasure buried under the fire.
 */
public final class BanditCamp {
    private BanditCamp() {}

    public static final String BANDIT_TAG = "realmlord_bandit";
    public static final String CHIEF_TAG = "realmlord_bandit_chief";
    public static final String CAPTIVE_TAG = "realmlord_captive";
    public static final String OWNER_KEY = "realmlord_owner";

    /** Offset of the captives' pen from the camp centre. */
    public static final int PEN_X = 7;

    public static boolean ownedBy(Entity e, String tag, UUID owner) {
        return e.getTags().contains(tag) && e.getPersistentData().hasUUID(OWNER_KEY)
                && owner.equals(e.getPersistentData().getUUID(OWNER_KEY));
    }

    public static UUID owner(Entity e) {
        return e.getPersistentData().hasUUID(OWNER_KEY) ? e.getPersistentData().getUUID(OWNER_KEY) : null;
    }

    public static void build(ServerLevel level, ServerPlayer player, VillageQuest q) {
        RandomSource r = level.getRandom();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, q.camp.getX(), q.camp.getZ());
        BlockPos c = new BlockPos(q.camp.getX(), y, q.camp.getZ());
        q.camp = c;
        flatten(level, c);

        // The chief's treasure, buried beneath the campfire.
        BlockPos cache = c.below();
        level.setBlock(cache, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(cache) instanceof ChestBlockEntity chest) fillTreasure(chest, r);
        level.setBlock(c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
        q.cache = cache;

        // Three tents.
        tent(level, c.offset(-6, 0, -5));
        tent(level, c.offset(-6, 0, 5));
        tent(level, c.offset(0, 0, -7));
        // Stolen supplies.
        barrel(level, c.offset(3, 0, 3), new ItemStack(Items.BREAD, 6 + r.nextInt(6)), new ItemStack(Items.WHEAT, 12 + r.nextInt(12)),
                new ItemStack(Items.POTATO, 4 + r.nextInt(6)));
        barrel(level, c.offset(4, 0, 3), new ItemStack(Items.ARROW, 8 + r.nextInt(12)), new ItemStack(Items.LEATHER, 2 + r.nextInt(4)),
                new ItemStack(Items.IRON_INGOT, 1 + r.nextInt(3)));
        level.setBlock(c.offset(-3, 0, 3), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

        // The captives' pen.
        BlockPos pen = c.offset(PEN_X, 0, 0);
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState roof = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int dy = 0; dy < 3; dy++) {
                    BlockPos p = pen.offset(dx, dy, dz);
                    level.setBlock(p, edge ? (Math.abs(dx) == 2 && Math.abs(dz) == 2 ? Blocks.SPRUCE_LOG.defaultBlockState() : bars)
                            : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.setBlock(pen.offset(dx, 3, dz), roof, Block.UPDATE_ALL);
            }
        int captives = 3 + r.nextInt(5);
        for (int i = 0; i < captives; i++) {
            SettlerEntity s = ModEntities.SETTLER.get().create(level);
            if (s == null) continue;
            s.moveTo(pen.getX() + 0.5 + r.nextInt(3) - 1, pen.getY(), pen.getZ() + 0.5 + r.nextInt(3) - 1, r.nextFloat() * 360F, 0F);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(pen), MobSpawnType.EVENT, null, null);
            s.makeCaptive(player.getUUID());
            mark(s, CAPTIVE_TAG, player.getUUID());
            level.addFreshEntity(s);
        }
        q.captives = captives;

        // The band: four swordsmen, two archers and their chief.
        BanditEntity.Role[] band = {BanditEntity.Role.SWORDSMAN, BanditEntity.Role.ARCHER, BanditEntity.Role.SWORDSMAN,
                BanditEntity.Role.SWORDSMAN, BanditEntity.Role.ARCHER, BanditEntity.Role.SWORDSMAN};
        for (int i = 0; i < band.length; i++) {
            double a = Math.PI * 2 * i / band.length;
            double radius = band[i] == BanditEntity.Role.ARCHER ? 7 : 4;
            spawnBandit(level, band[i], c.getX() + 0.5 + Math.cos(a) * radius, c.getZ() + 0.5 + Math.sin(a) * radius, player.getUUID());
        }
        spawnBandit(level, BanditEntity.Role.CHIEF, c.getX() - 2.5, c.getZ() + 0.5, player.getUUID());
        q.banditsLeft = band.length + 1;
        q.campBuilt = true;
    }

    /**
     * The chief's hoard: gold and silver coins, building blueprints, a piece of graded equipment and
     * grain, the kind of spoils a Lord in the novel takes from a bandit lair.
     */
    public static void fillTreasure(ChestBlockEntity chest, RandomSource r) {
        chest.setItem(4, new ItemStack(ModItems.GOLD_COIN.get(), 2 + r.nextInt(3)));
        chest.setItem(10, Blueprints.random(r, false));
        chest.setItem(11, Blueprints.random(r, true));
        net.minecraft.world.item.Item[] weapons = {Items.IRON_SWORD, Items.IRON_AXE};
        chest.setItem(13, Gear.make(weapons[r.nextInt(weapons.length)], TokenGrade.SILVER, r));
        net.minecraft.world.item.Item[] armour = {Items.CHAINMAIL_CHESTPLATE, Items.IRON_HELMET, Items.IRON_BOOTS};
        chest.setItem(15, Gear.make(armour[r.nextInt(armour.length)], TokenGrade.BLACK_IRON, r));
        chest.setItem(16, new ItemStack(Items.WHEAT, 32));
        chest.setItem(22, new ItemStack(ModItems.SILVER_COIN.get(), 40 + r.nextInt(21)));
    }

    private static void barrel(ServerLevel level, BlockPos pos, ItemStack... items) {
        level.setBlock(pos, Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity b) {
            for (int i = 0; i < items.length; i++) b.setItem(i * 4 + 2, items[i]);
        }
    }

    private static void spawnBandit(ServerLevel level, BanditEntity.Role role, double x, double z, UUID owner) {
        BanditEntity m = ModEntities.BANDIT.get().create(level);
        if (m == null) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        m.moveTo(x, y, z, level.getRandom().nextFloat() * 360F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null, null);
        m.configure(role, 1);
        m.setPersistenceRequired();
        if (role == BanditEntity.Role.CHIEF) m.addTag(CHIEF_TAG);
        mark(m, BANDIT_TAG, owner);
        level.addFreshEntity(m);
    }

    /** Opens the side of the pen facing the camp. */
    public static void openPen(ServerLevel level, BlockPos camp) {
        BlockPos pen = camp.offset(PEN_X, 0, 0);
        for (int dz = -1; dz <= 1; dz++)
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = pen.offset(-2, dy, dz);
                if (level.getBlockState(p).is(Blocks.IRON_BARS)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
    }

    private static void mark(Entity e, String tag, UUID owner) {
        e.addTag(tag);
        e.getPersistentData().putUUID(OWNER_KEY, owner);
    }

    private static void flatten(ServerLevel level, BlockPos c) {
        for (int dx = -10; dx <= 10; dx++)
            for (int dz = -10; dz <= 10; dz++) {
                if (dx * dx + dz * dz > 110) continue;
                for (int y = 0; y <= 8; y++) level.setBlock(c.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(c.offset(dx, -1, dz), (dx + dz) % 3 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = -2; y >= -6; y--) {
                    BlockPos p = c.offset(dx, y, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    else break;
                }
            }
    }

    private static void tent(ServerLevel level, BlockPos base) {
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        BlockState cloth = Blocks.BROWN_WOOL.defaultBlockState();
        for (int dx = -1; dx <= 1; dx += 2)
            for (int dz = -1; dz <= 1; dz += 2) {
                level.setBlock(base.offset(dx, 0, dz), fence, Block.UPDATE_ALL);
                level.setBlock(base.offset(dx, 1, dz), fence, Block.UPDATE_ALL);
            }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) level.setBlock(base.offset(dx, 2, dz), cloth, Block.UPDATE_ALL);
        level.setBlock(base.offset(0, 3, 0), Blocks.BROWN_CARPET.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base, Blocks.BROWN_CARPET.defaultBlockState(), Block.UPDATE_ALL);
    }
}
