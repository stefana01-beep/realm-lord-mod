package com.realmlord.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

/**
 * The bandit camp of the Village Creation quest: tents around a campfire, a barred pen of
 * captive settlers, a band of bandits with their chief, and a cache buried under the fire.
 */
public final class BanditCamp {
    private BanditCamp() {}

    public static final String BANDIT_TAG = "realmlord_bandit";
    public static final String CHIEF_TAG = "realmlord_bandit_chief";
    public static final String CAPTIVE_TAG = "realmlord_captive";
    public static final String OWNER_KEY = "realmlord_owner";

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

        // Campfire with the cache buried beneath it.
        BlockPos cache = c.below();
        level.setBlock(cache, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(cache) instanceof ChestBlockEntity chest) {
            chest.setItem(4, new ItemStack(Items.GOLD_INGOT, 8));
            chest.setItem(10, new ItemStack(Items.EMERALD, 12));
            chest.setItem(13, new ItemStack(Items.IRON_SWORD));
            chest.setItem(16, new ItemStack(Items.BREAD, 16));
            chest.setItem(22, new ItemStack(Items.MAP));
        }
        level.setBlock(c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
        q.cache = cache;

        // Three tents.
        tent(level, c.offset(-6, 0, -5));
        tent(level, c.offset(-6, 0, 5));
        tent(level, c.offset(0, 0, -7));
        // Barrels and crates.
        level.setBlock(c.offset(3, 0, 3), Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(c.offset(4, 0, 3), Blocks.BARREL.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(c.offset(-3, 0, 3), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

        // The captives' pen.
        BlockPos pen = c.offset(7, 0, 0);
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
            Villager v = EntityType.VILLAGER.create(level);
            if (v == null) continue;
            v.moveTo(pen.getX() + 0.5 + r.nextInt(3) - 1, pen.getY(), pen.getZ() + 0.5 + r.nextInt(3) - 1, r.nextFloat() * 360F, 0F);
            v.setNoAi(true);
            v.setInvulnerable(true);
            v.setPersistenceRequired();
            v.setCustomName(Component.translatable("realmlord.entity.captive"));
            mark(v, CAPTIVE_TAG, player.getUUID());
            level.addFreshEntity(v);
        }
        q.captives = captives;

        // The bandits and their chief.
        int bandits = 6;
        for (int i = 0; i < bandits; i++) {
            EntityType<? extends Mob> type = i % 2 == 0 ? EntityType.VINDICATOR : EntityType.PILLAGER;
            double a = Math.PI * 2 * i / bandits;
            spawnBandit(level, type, c.getX() + Math.cos(a) * 4, c.getZ() + Math.sin(a) * 4, player.getUUID(), false);
        }
        spawnBandit(level, EntityType.VINDICATOR, c.getX() - 2.5, c.getZ() + 0.5, player.getUUID(), true);
        q.banditsLeft = bandits + 1;
        q.campBuilt = true;
    }

    private static void spawnBandit(ServerLevel level, EntityType<? extends Mob> type, double x, double z, UUID owner, boolean chief) {
        Mob m = type.create(level);
        if (m == null) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        m.moveTo(x, y, z, level.getRandom().nextFloat() * 360F, 0F);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null, null);
        m.setPersistenceRequired();
        if (m instanceof Raider raider) raider.setCanJoinRaid(false);
        if (chief) {
            var hp = m.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(60.0);
            m.setHealth(60.0F);
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            m.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            m.setDropChance(EquipmentSlot.HEAD, 0.0F);
            m.setCustomName(Component.translatable("realmlord.entity.bandit_chief"));
            m.addTag(CHIEF_TAG);
        } else {
            m.setCustomName(Component.translatable("realmlord.entity.bandit"));
        }
        mark(m, BANDIT_TAG, owner);
        level.addFreshEntity(m);
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
