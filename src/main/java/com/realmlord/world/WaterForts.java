package com.realmlord.world;

import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.entity.BanditEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Goods;
import com.realmlord.village.Navy;
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
import net.minecraft.world.level.block.Blocks;

/**
 * Water bandits. The novel's rivers and coasts had their pirates and water bandits, who preyed on
 * fishermen and merchant ships until a territory built a navy to clear them out.
 *
 * <p>A village by the water (with a dock, or on the coast) may find that water bandits have built
 * a fort on stilts out on the water within a few hundred blocks: a palisade, a hut with their loot
 * chest and a watchtower, held by a band and their chief. While it stands, the village's fishing
 * brings in 40% less, its Security falls, its merchant ships are robbed far more often, and now and
 * then the bandits raid the docks for fish and goods (unless the navy, ten sailors with a Mengchong
 * warship, beats them off). Kill the chief and the fort is broken: the Lord gains merit, the chief
 * may carry the warship manual or a Summoning Token, and the chest holds their plunder. A broken
 * fort may be taken over again by new bandits after a month or two.
 */
public final class WaterForts {
    private WaterForts() {}

    public static final String TAG = "realmlord_water_fort";
    public static final int RANGE = 256;
    public static final long CHIEF_MERIT = 120;

    public static ListTag forts(WorldState w) {
        return w.ext.getList("forts", Tag.TAG_COMPOUND);
    }

    private static void save(WorldState w, ListTag l) {
        w.ext.put("forts", l);
        w.touch();
    }

    /** The nearest standing fort within range of a point, or null. */
    public static CompoundTag near(WorldState w, BlockPos at, int range) {
        CompoundTag best = null;
        double bd = (double) range * range;
        for (Tag tg : forts(w)) {
            CompoundTag f = (CompoundTag) tg;
            if (f.getBoolean("cleared")) continue;
            double d = BlockPos.of(f.getLong("pos")).distSqr(at);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best;
    }

    public static boolean byWater(ServerLevel level, Territory t) {
        return Navy.hasDock(t) || Village.coastal(level, t);
    }

    /** Each village's day: a fort may be founded nearby, and one standing nearby harries the village. */
    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        if (t.dimension != level.dimension()) return;
        WorldState w = WorldState.get(level.getServer());
        CompoundTag fort = near(w, t.center, RANGE);
        boolean was = t.ext.getBoolean("waterBandits");
        if (fort == null && GaiaCalendar.year(day) >= 1 && t.residents.size() >= 20 && rnd.nextInt(12) == 0 && level.isLoaded(t.center)
                && byWater(level, t)) {
            fort = found(level, t, rnd);
            if (fort != null) Village.tell(level, t, Component.translatable("realmlord.water_fort.founded", t.name,
                    BlockPos.of(fort.getLong("pos")).getX(), BlockPos.of(fort.getLong("pos")).getZ()));
        }
        boolean now = fort != null;
        if (now != was) t.ext.putBoolean("waterBandits", now);
        if (!now || rnd.nextInt(6) != 0) return;
        // A raid on the docks.
        boolean navy = Navy.count(t) >= 10 && Navy.ships(t, com.realmlord.registry.ModCrafts.MENGCHONG_WARSHIP.get()) > 0;
        if (navy) {
            Village.tell(level, t, Component.translatable("realmlord.water_fort.repelled", t.name));
            for (com.realmlord.village.Resident r : t.residents) if (r.sailor) r.gainSoldierXp(40);
            return;
        }
        long fish = Math.min(Goods.FISH.stock(t), 20 + rnd.nextInt(40));
        Goods.FISH.add(t, -fish);
        long salt = Math.min(Goods.SALT.stock(t), rnd.nextInt(30));
        Goods.SALT.add(t, -salt);
        if (fish + salt > 0) Village.tell(level, t, Component.translatable("realmlord.water_fort.raided", t.name, fish, salt));
    }

    /** Looks for open water away from the village and builds a fort there, with its band. */
    public static CompoundTag found(ServerLevel level, Territory t, RandomSource rnd) {
        Design d = Designs.builtin("water_fort");
        if (d == null) return null;
        for (int tries = 0; tries < 40; tries++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            int dist = t.radius() + 48 + rnd.nextInt(120);
            int x = t.center.getX() + (int) (Math.cos(a) * dist), z = t.center.getZ() + (int) (Math.sin(a) * dist);
            BlockPos origin = openWater(level, x, z, d.w, d.d);
            if (origin == null) continue;
            if (TerritoryData.get(level.getServer()).at(level.dimension(), origin) != null) continue;
            place(level, d, origin);
            CompoundTag f = new CompoundTag();
            f.putLong("pos", origin.offset(d.w / 2, 0, d.d / 2).asLong());
            f.putInt("id", (int) (rnd.nextInt() & 0x7fffffff));
            f.putLong("day", GaiaCalendar.today(level.getServer()));
            stock(level, origin.offset(10, 3 + d.yOff + 1, 10), rnd);
            garrison(level, f, origin.offset(0, d.yOff + 4, 0), d, t, rnd);
            WorldState w = WorldState.get(level.getServer());
            ListTag l = forts(w);
            l.add(f);
            save(w, l);
            return f;
        }
        return null;
    }

    /** The corner of a w x d stretch of water at least two deep (its surface the origin's y - 1), or null. */
    public static BlockPos openWater(ServerLevel level, int x0, int z0, int w, int d) {
        if (!level.isLoaded(new BlockPos(x0, 64, z0)) || !level.isLoaded(new BlockPos(x0 + w, 64, z0 + d))) return null;
        int top = -1;
        for (int dx = 0; dx <= w; dx += w / 2) {
            for (int dz = 0; dz <= d; dz += d / 2) {
                int x = x0 + dx, z = z0 + dz;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getBlockState(p).is(Blocks.WATER) || !level.getBlockState(p.below()).is(Blocks.WATER)) return null;
                if (top >= 0 && top != y) return null;
                top = y;
            }
        }
        return top < 0 ? null : new BlockPos(x0, top + 1, z0);
    }

    /** Builds the fort: the design's y = 0 is the first block above the water. */
    public static void place(ServerLevel level, Design d, BlockPos origin) {
        for (int y = 0; y < d.h; y++)
            for (int z = 0; z < d.d; z++)
                for (int x = 0; x < d.w; x++) {
                    int v = d.at(x, y, z);
                    if (v < 0) continue;
                    BlockPos p = origin.offset(x, y + d.yOff, z);
                    if (v == 0) {
                        if (y + d.yOff >= 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        continue;
                    }
                    level.setBlock(p, d.palette[v], 2);
                }
    }

    private static void stock(ServerLevel level, BlockPos chest, RandomSource rnd) {
        for (BlockPos p : BlockPos.betweenClosed(chest.offset(-2, -1, -2), chest.offset(2, 1, 2))) {
            if (level.getBlockState(p).is(Blocks.CHEST) && level.getBlockEntity(p) instanceof Container cc) {
                fill(cc, rnd);
                return;
            }
        }
    }

    private static void fill(Container c, RandomSource rnd) {
        int slot = 0;
        c.setItem(slot++, new ItemStack(ModItems.SILVER_COIN.get(), 20 + rnd.nextInt(40)));
        c.setItem(slot++, new ItemStack(net.minecraft.world.item.Items.COD, 8 + rnd.nextInt(16)));
        c.setItem(slot++, new ItemStack(net.minecraft.world.item.Items.SALMON, 4 + rnd.nextInt(8)));
        if (rnd.nextInt(3) == 0) c.setItem(slot++, new ItemStack(ModItems.GOLD_COIN.get(), 1 + rnd.nextInt(3)));
        if (rnd.nextInt(3) == 0) c.setItem(slot++, new ItemStack(com.realmlord.registry.ModCrafts.PINE_TAR.get(), 2 + rnd.nextInt(4)));
        if (rnd.nextInt(4) == 0) c.setItem(slot++, com.realmlord.loot.Blueprints.random(rnd, false));
        if (rnd.nextInt(5) == 0) c.setItem(slot, com.realmlord.loot.Manuals.of(com.realmlord.loot.Manuals.WARSHIPS));
    }

    /** The band: six to ten water bandits on the deck and their chief, tougher for bigger villages. */
    private static void garrison(ServerLevel level, CompoundTag fort, BlockPos deck, Design d, Territory t, RandomSource rnd) {
        int n = 6 + rnd.nextInt(5);
        int tier = Math.max(1, Math.min(4, 1 + t.level.ordinal() / 2));
        for (int i = 0; i <= n; i++) {
            BanditEntity b = ModEntities.BANDIT.get().create(level);
            if (b == null) continue;
            int x = 1 + rnd.nextInt(5), z = 1 + rnd.nextInt(5);
            b.moveTo(deck.getX() + x + 0.5, deck.getY(), deck.getZ() + z + 0.5, rnd.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(deck), MobSpawnType.STRUCTURE, null, null);
            b.getPersistentData().remove("realmlord_wild");
            b.setFaction(BanditEntity.Faction.WATER);
            b.setSkin(rnd.nextInt(BanditEntity.WATER_SKINS));
            BanditEntity.Role role = i == 0 ? BanditEntity.Role.CHIEF : rnd.nextInt(10) < 4 ? BanditEntity.Role.ARCHER : BanditEntity.Role.SWORDSMAN;
            b.configure(role, i == 0 ? tier + 1 : tier);
            b.setFaction(BanditEntity.Faction.WATER);
            b.addTag(TAG);
            b.getPersistentData().putInt("realmlord_fort", fort.getInt("id"));
            b.setPersistenceRequired();
            if (i == 0) fort.putUUID("chief", b.getUUID());
            level.addFreshEntity(b);
        }
    }

    /** The chief is dead: the fort is broken. */
    public static void chiefSlain(ServerLevel level, BanditEntity chief, ServerPlayer by) {
        WorldState w = WorldState.get(level.getServer());
        ListTag l = forts(w);
        int id = chief.getPersistentData().getInt("realmlord_fort");
        for (Tag tg : l) {
            CompoundTag f = (CompoundTag) tg;
            if (f.getBoolean("cleared") || f.getInt("id") != id && !(f.hasUUID("chief") && f.getUUID("chief").equals(chief.getUUID()))) continue;
            f.putBoolean("cleared", true);
            f.putLong("clearedDay", GaiaCalendar.today(level.getServer()));
            if (by != null) {
                com.realmlord.profile.Profiles.grantMerit(by, CHIEF_MERIT, Component.translatable("realmlord.reason.water_fort"));
                com.realmlord.Notify.player(by, Component.translatable("realmlord.water_fort.cleared"));
            }
            BlockPos at = BlockPos.of(f.getLong("pos"));
            for (Territory t : TerritoryData.get(level.getServer()).all()) {
                if (t.ext.getBoolean("waterBandits") && t.center.distSqr(at) < (double) RANGE * RANGE && near(w, t.center, RANGE) == null) {
                    t.ext.putBoolean("waterBandits", false);
                    Village.tell(level, t, Component.translatable("realmlord.water_fort.freed", t.name));
                }
            }
        }
        // Broken forts are forgotten after two months, so the water may be taken again.
        long today = GaiaCalendar.today(level.getServer());
        for (int i = l.size() - 1; i >= 0; i--) {
            CompoundTag f = l.getCompound(i);
            if (f.getBoolean("cleared") && today - f.getLong("clearedDay") > 24L * GaiaCalendar.daysPerMonth()) l.remove(i);
        }
        save(w, l);
    }
}
