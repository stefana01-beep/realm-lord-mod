package com.realmlord.world;

import com.realmlord.entity.BanditEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.FigureArts;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Resident;
import com.realmlord.village.Temples;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

/**
 * Barbarian raids. In the novel the barbarians of the northern steppe came south when the beast
 * tide had worn the border territories down, and again each autumn when the grain was in, and
 * their killing arts (the Hell Slaughterer True Manual among them) were as feared as their horses.
 *
 * <p>After every beastwave each village of the second level or above can expect a raid within the
 * week; and from the second year, every autumn (months 9 and 10) a village of forty people or more
 * may be raided once. Scouts give two days' warning. The band comes from the north, more and
 * stronger the bigger the village and the later the year: weaker if the temple wards the walls and
 * strategists saw them coming. They march on the manor and fight everyone they meet; at the next
 * dawn those still alive withdraw with what they can carry. The chieftain may carry the Hell
 * Slaughterer True Manual and, now and then, a Summoning Token.
 */
public final class Raids {
    private Raids() {}

    public static final String TAG = "realmlord_raid";
    public static final String TERRITORY = "realmlord_raid_territory";
    public static final long CHIEF_MERIT = 150;

    public static int bandSize(Territory t) {
        double n = 4 + t.residents.size() / 15.0 + t.level.ordinal() * 2;
        n *= Temples.warding(t) * Math.max(0.5, 1.0 - 0.1 * FigureArts.count(t, "strategist"));
        return (int) Math.max(3, Math.min(30, Math.round(n)));
    }

    public static int tier(Territory t, long day) {
        return Math.max(1, Math.min(5, 1 + t.level.ordinal() / 2 + Math.max(0, (GaiaCalendar.year(day) - 2) / 3)));
    }

    /** Each territory's day: the raider attack that answers a county application (see Trials). */
    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        Trials.day(level, t, day, rnd);
    }

    /** The band gathers on the northern side, past the border, and marches on the manor. */
    public static int spawn(ServerLevel level, Territory t, long day) {
        return spawnBand(level, t, bandSize(t), tier(t, day), -Math.PI / 2, 1, TAG);
    }

    /**
     * A band of n barbarian raiders (the first {@code chiefs} of them chieftains) gathering past
     * the border around a bearing, then marching on the manor. Returns how many came.
     */
    public static int spawnBand(ServerLevel level, Territory t, int n, int tier, double bearing, int chiefs, String tag) {
        RandomSource r = level.getRandom();
        int made = 0;
        for (int i = 0; i < n; i++) {
            BlockPos at = null;
            for (int tries = 0; tries < 16 && at == null; tries++) {
                double a = bearing + (r.nextDouble() - 0.5) * (tries < 5 ? 1.2 : Math.PI);
                // Past the border if that ground is loaded, closer in if not.
                int d = tries < 8 ? t.radius() + 20 + r.nextInt(10) : tries < 12 ? t.radius() + r.nextInt(8) : t.radius() / 2 + r.nextInt(8);
                int x = t.center.getX() + (int) (Math.cos(a) * d), z = t.center.getZ() + (int) (Math.sin(a) * d);
                if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
                int y = Village.groundAt(level, x, z);
                if (level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty() || tries >= 14) at = new BlockPos(x, y, z);
            }
            if (at == null) continue;
            BanditEntity b = ModEntities.BANDIT.get().create(level);
            if (b == null) continue;
            b.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            b.getPersistentData().remove("realmlord_wild");
            b.setFaction(BanditEntity.Faction.BARBARIAN);
            b.setSkin(r.nextInt(BanditEntity.BARBARIAN_SKINS));
            b.configure(i < chiefs ? BanditEntity.Role.CHIEF : r.nextInt(10) < 3 ? BanditEntity.Role.ARCHER : BanditEntity.Role.SWORDSMAN, i < chiefs ? tier + 1 : tier);
            b.setFaction(BanditEntity.Faction.BARBARIAN);
            b.getPersistentData().putLong(BanditEntity.RAID_TARGET, t.center.asLong());
            b.getPersistentData().putInt(TERRITORY, t.id);
            b.addTag(TAG);
            if (!tag.equals(TAG)) b.addTag(tag);
            b.setPersistenceRequired();
            if (level.addFreshEntity(b)) made++;
        }
        return made;
    }

    /** Dawn after the raid: those left ride off with what they carry. */
    public static void withdraw(ServerLevel level, Territory t) {
        t.ext.remove("raidEnds");
        int left = 0;
        java.util.List<Entity> gone = new java.util.ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e != null && e.getTags().contains(TAG) && e.getPersistentData().getInt(TERRITORY) == t.id) gone.add(e);
        }
        for (Entity e : gone) {
            e.discard();
            left++;
        }
        if (left == 0) {
            Village.tell(level, t, Component.translatable("realmlord.raid.beaten", t.name));
            return;
        }
        long grain = Math.min(t.food, left * 60L);
        t.food -= grain;
        Village.tell(level, t, Component.translatable("realmlord.raid.withdrew", left, t.name, grain));
    }

    /** A raid nobody is near: the wall and the militia fight it unseen. */
    public static void offScreen(ServerLevel level, Territory t, long day) {
        boolean fence = com.realmlord.territory.TerritoryEvents.fenced(level, t.center);
        int militia = Village.militiaCount(t) + com.realmlord.village.Navy.count(t) / 2;
        int band = bandSize(t) * tier(t, day);
        if (fence && militia * 2 >= band) {
            Village.tell(level, t, Component.translatable("realmlord.raid.repelled", t.name));
            return;
        }
        double share = fence ? 0.05 : 0.15;
        long food = Math.round(t.food * share);
        t.food -= food;
        long coin = Math.round(t.treasury * share / 2);
        t.treasury -= coin;
        int lost = fence ? 0 : Math.min(4, Math.max(0, (band - militia * 2) / 10));
        for (int i = 0; i < lost && t.residents.size() > 5; i++) {
            Resident r = t.residents.get(level.getRandom().nextInt(t.residents.size()));
            if (r.isFigure()) continue;
            t.residents.remove(r);
        }
        Village.tell(level, t, Component.translatable("realmlord.raid.unseen", t.name, food, com.realmlord.village.Coins.format(coin), lost));
    }

    /** What a barbarian chieftain or a water bandit chief carries. */
    public static void chiefDrops(BanditEntity chief, RandomSource r, int looting) {
        if (chief.getFaction() == BanditEntity.Faction.BARBARIAN) {
            chief.spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get(), 1 + r.nextInt(2)));
            if (r.nextFloat() < 0.3F + 0.05F * looting) {
                var art = com.realmlord.martial.Arts.byId("hell_slaughterer");
                if (art != null) chief.spawnAtLocation(com.realmlord.martial.Arts.manual(art));
            }
            if (r.nextFloat() < 0.10F) chief.spawnAtLocation(com.realmlord.item.SummoningTokenItem.of(0));
            chief.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.LEATHER, 2 + r.nextInt(4)));
        } else if (chief.getFaction() == BanditEntity.Faction.MOUNTAIN) {
            chief.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.LEATHER, 4 + r.nextInt(6)));
            chief.spawnAtLocation(new ItemStack(ModItems.GOLD_COIN.get(), 1 + r.nextInt(3)));
            if (r.nextFloat() < 0.25F + 0.05F * looting) chief.spawnAtLocation(com.realmlord.martial.Arts.random(r, com.realmlord.martial.ArtGrade.GOLD));
            if (r.nextFloat() < 0.06F) chief.spawnAtLocation(com.realmlord.item.SummoningTokenItem.of(0));
        } else if (chief.getFaction() == BanditEntity.Faction.WATER) {
            if (r.nextFloat() < 0.2F + 0.05F * looting) chief.spawnAtLocation(com.realmlord.loot.Manuals.of(com.realmlord.loot.Manuals.WARSHIPS));
            if (r.nextFloat() < 0.08F) chief.spawnAtLocation(com.realmlord.item.SummoningTokenItem.of(0));
            chief.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.COD, 4 + r.nextInt(8)));
        }
    }

    /** A raider dies: a chieftain slain wins the Lord merit. */
    public static void killed(ServerLevel level, Entity dead, Entity killer) {
        if (!(dead instanceof BanditEntity b) || b.getRole() != BanditEntity.Role.CHIEF) return;
        ServerPlayer p = killer instanceof ServerPlayer sp ? sp : null;
        if (p == null && killer instanceof com.realmlord.entity.SettlerEntity s && s.getTerritoryId() >= 0) {
            Territory t = TerritoryData.get(level.getServer()).byId(s.getTerritoryId());
            if (t != null && t.owner != null) p = level.getServer().getPlayerList().getPlayer(t.owner);
        }
        if (b.getFaction() == BanditEntity.Faction.BARBARIAN) {
            if (p != null) com.realmlord.profile.Profiles.grantMerit(p, CHIEF_MERIT, Component.translatable("realmlord.reason.barbarian_chief"));
            if (b.getPersistentData().contains(TERRITORY)) {
                Territory t = TerritoryData.get(level.getServer()).byId(b.getPersistentData().getInt(TERRITORY));
                if (t != null) Village.tell(level, t, Component.translatable("realmlord.raid.chief_slain", t.name));
            }
        } else if (b.getFaction() == BanditEntity.Faction.WATER) {
            WaterForts.chiefSlain(level, b, p);
        } else if (b.getFaction() == BanditEntity.Faction.MOUNTAIN) {
            Tribes.chiefSlain(level, b, p);
        }
    }
}
