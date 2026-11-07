package com.realmlord.world;

import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.entity.BanditEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Refugees;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The mountain barbarians. In the novel the hundred-thousand mountains around Shanhai were full of
 * mountain barbarian tribes, who raided the fields; Ouyang Shuo fought them, and the tribes that
 * submitted came down to live in his territory. A thousand of their young men became his elite
 * mountain barbarian soldiers, and Gushan County was set aside for them.
 *
 * <p>A town may find a mountain barbarian tribe settled in the wilds within a few hundred blocks: a
 * palisaded village of hide huts around a totem, held by warriors and a chieftain. While the tribe
 * is there it raids the town's fields now and then. Kill the chieftain and the tribe submits:
 * twenty to forty of its people join the town's refugee camp and move in as beds allow. They are
 * hardy hunters and farmers, and can be enlisted as Mountain Warriors, the army's elite infantry.
 */
public final class Tribes {
    private Tribes() {}

    public static final String TAG = "realmlord_tribe";
    public static final int RANGE = 320;
    public static final long CHIEF_MERIT = 150;

    public static ListTag tribes(WorldState w) {
        return w.ext.getList("tribes", Tag.TAG_COMPOUND);
    }

    public static CompoundTag near(WorldState w, BlockPos at, int range) {
        CompoundTag best = null;
        double bd = (double) range * range;
        for (Tag tg : tribes(w)) {
            CompoundTag f = (CompoundTag) tg;
            if (f.getBoolean("submitted")) continue;
            double d = BlockPos.of(f.getLong("pos")).distSqr(at);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1) || t.dimension != level.dimension()) return;
        WorldState w = WorldState.get(level.getServer());
        CompoundTag tribe = near(w, t.center, RANGE);
        if (tribe == null) {
            if (rnd.nextInt(16) == 0 && level.isLoaded(t.center)) {
                tribe = found(level, t, rnd);
                if (tribe != null) {
                    BlockPos at = BlockPos.of(tribe.getLong("pos"));
                    Village.tell(level, t, Component.translatable("realmlord.tribe.found", t.name, at.getX(), at.getZ()));
                }
            }
            return;
        }
        // Now and then they come down for the harvest.
        if (rnd.nextInt(8) == 0) {
            long food = Math.min(t.food, 50 + rnd.nextInt(150));
            if (Village.militiaCount(t) + com.realmlord.village.Army.count(t) >= 30) {
                Village.tell(level, t, Component.translatable("realmlord.tribe.repelled", t.name));
            } else if (food > 0) {
                t.food -= food;
                Village.tell(level, t, Component.translatable("realmlord.tribe.raided", t.name, food));
            }
        }
    }

    public static CompoundTag found(ServerLevel level, Territory t, RandomSource rnd) {
        Design d = Designs.builtin("barbarian_village");
        if (d == null) return null;
        for (int tries = 0; tries < 30; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            int dist = t.radius() + 60 + rnd.nextInt(160);
            int x = t.center.getX() + (int) (Math.cos(a) * dist), z = t.center.getZ() + (int) (Math.sin(a) * dist);
            if (!level.isLoaded(new BlockPos(x, 64, z)) || !level.isLoaded(new BlockPos(x + d.w, 64, z + d.d))) continue;
            BlockPos origin = dryGround(level, x, z, d.w, d.d);
            if (origin == null) continue;
            if (TerritoryData.get(level.getServer()).at(level.dimension(), origin) != null) continue;
            place(level, d, origin);
            CompoundTag f = new CompoundTag();
            f.putLong("pos", origin.offset(d.w / 2, 0, d.d / 2).asLong());
            f.putInt("id", rnd.nextInt() & 0x7fffffff);
            stock(level, origin, d, rnd);
            garrison(level, f, origin.offset(d.w / 2, 0, d.d / 2), t, rnd);
            WorldState w = WorldState.get(level.getServer());
            ListTag l = tribes(w);
            l.add(f);
            w.ext.put("tribes", l);
            w.touch();
            return f;
        }
        return null;
    }

    /** Dry land, no steeper than six blocks across the footprint: the corner at the median ground. */
    public static BlockPos dryGround(ServerLevel level, int x0, int z0, int w, int d) {
        int[] ys = new int[9];
        int k = 0;
        for (int dx = 0; dx <= w; dx += w / 2)
            for (int dz = 0; dz <= d; dz += d / 2) {
                int y = Village.groundAt(level, x0 + dx, z0 + dz);
                if (!level.getFluidState(new BlockPos(x0 + dx, y - 1, z0 + dz)).isEmpty()) return null;
                ys[k++] = y;
            }
        java.util.Arrays.sort(ys);
        if (ys[8] - ys[0] > 6) return null;
        return new BlockPos(x0, ys[4], z0);
    }

    /** Builds the village on a levelled pad: earth under it, air above it. */
    public static void place(ServerLevel level, Design d, BlockPos origin) {
        for (int z = 0; z < d.d; z++)
            for (int x = 0; x < d.w; x++) {
                int gx = origin.getX() + x, gz = origin.getZ() + z;
                int ground = Village.groundAt(level, gx, gz);
                for (int y = ground; y < origin.getY() - 1; y++) level.setBlock(new BlockPos(gx, y, gz), Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx, origin.getY() - 1, gz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = origin.getY(); y < Math.max(ground, origin.getY() + d.h); y++) level.setBlock(new BlockPos(gx, y, gz), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int y = 0; y < d.h; y++)
            for (int z = 0; z < d.d; z++)
                for (int x = 0; x < d.w; x++) {
                    int v = d.at(x, y, z);
                    if (v <= 0) continue;
                    level.setBlock(origin.offset(x, y + d.yOff, z), d.palette[v], 2);
                }
    }

    private static void stock(ServerLevel level, BlockPos origin, Design d, RandomSource rnd) {
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(d.w, d.h, d.d))) {
            if (level.getBlockState(p).is(Blocks.CHEST) && level.getBlockEntity(p) instanceof Container c) {
                int slot = 0;
                c.setItem(slot++, new ItemStack(Items.LEATHER, 6 + rnd.nextInt(10)));
                c.setItem(slot++, new ItemStack(ModItems.SILVER_COIN.get(), 10 + rnd.nextInt(30)));
                c.setItem(slot++, new ItemStack(com.realmlord.registry.ModItems.MEDICINAL_HERBS.get(), 4 + rnd.nextInt(8)));
                if (rnd.nextInt(3) == 0) c.setItem(slot++, com.realmlord.martial.Arts.random(rnd, com.realmlord.martial.ArtGrade.SILVER));
                if (rnd.nextInt(3) == 0) c.setItem(slot, new ItemStack(ModItems.GOLD_COIN.get(), 1 + rnd.nextInt(3)));
                return;
            }
        }
    }

    private static void garrison(ServerLevel level, CompoundTag tribe, BlockPos at, Territory t, RandomSource rnd) {
        int n = 8 + rnd.nextInt(6);
        int tier = Math.max(2, Math.min(5, 2 + (t.level.ordinal() - TerritoryLevel.TOWN_1.ordinal())));
        for (int i = 0; i <= n; i++) {
            BanditEntity b = ModEntities.BANDIT.get().create(level);
            if (b == null) continue;
            int x = at.getX() + rnd.nextInt(9) - 4, z = at.getZ() + rnd.nextInt(9) - 4;
            b.moveTo(x + 0.5, Village.groundAt(level, x, z), z + 0.5, rnd.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null, null);
            b.getPersistentData().remove("realmlord_wild");
            b.setFaction(BanditEntity.Faction.MOUNTAIN);
            b.setSkin(rnd.nextInt(BanditEntity.MOUNTAIN_SKINS));
            b.configure(i == 0 ? BanditEntity.Role.CHIEF : rnd.nextInt(10) < 3 ? BanditEntity.Role.ARCHER : BanditEntity.Role.SWORDSMAN, i == 0 ? tier + 1 : tier);
            b.setFaction(BanditEntity.Faction.MOUNTAIN);
            b.addTag(TAG);
            b.getPersistentData().putInt("realmlord_tribe", tribe.getInt("id"));
            b.setPersistenceRequired();
            level.addFreshEntity(b);
        }
    }

    /** The chieftain is dead: the tribe submits and comes down to the nearest town of the Lord who won. */
    public static void chiefSlain(ServerLevel level, BanditEntity chief, ServerPlayer by) {
        WorldState w = WorldState.get(level.getServer());
        ListTag l = tribes(w);
        int id = chief.getPersistentData().getInt("realmlord_tribe");
        for (Tag tg : l) {
            CompoundTag f = (CompoundTag) tg;
            if (f.getBoolean("submitted") || f.getInt("id") != id) continue;
            f.putBoolean("submitted", true);
            f.putLong("day", GaiaCalendar.today(level.getServer()));
            BlockPos at = BlockPos.of(f.getLong("pos"));
            Territory home = null;
            double best = Double.MAX_VALUE;
            for (Territory t : TerritoryData.get(level.getServer()).all()) {
                if (by != null && !by.getUUID().equals(t.owner)) continue;
                double d = t.center.distSqr(at);
                if (d < best) {
                    best = d;
                    home = t;
                }
            }
            int n = 20 + level.getRandom().nextInt(21);
            if (home != null) {
                Refugees.add(home, n, true);
                Village.tell(level, home, Component.translatable("realmlord.tribe.submitted", n, home.name));
            }
            if (by != null) com.realmlord.profile.Profiles.grantMerit(by, CHIEF_MERIT, Component.translatable("realmlord.reason.tribe"));
        }
        w.ext.put("tribes", l);
        w.touch();
    }
}
