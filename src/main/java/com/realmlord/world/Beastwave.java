package com.realmlord.world;

import com.realmlord.Notify;
import com.realmlord.entity.beast.Beasts;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The beastwave, the novel's great world quest: on every other lunar new year, at nightfall,
 * the beasts of the wilds come against every village at once, in three waves through the night.
 * Warnings go out three days and one day before. At dawn the best defenders are rewarded in gold.
 */
public final class Beastwave {
    private Beastwave() {}

    public static final int WAVES = 3;
    /** Ticks between waves. */
    public static final int WAVE_GAP = 1800;
    /** Gold coins for the first, second and third defenders; one for anyone else with five kills. */
    public static final int[] PRIZES = {10, 6, 4};

    private record Front(int territory, int wavesLeft, long nextTick) {}

    private static final Map<Integer, Front> FRONTS = new HashMap<>();

    /** A beastwave falls on the new year of every odd year from the third. */
    public static boolean waveYear(int year) {
        return year >= 3 && year % 2 == 1;
    }

    public static long newYearDay(int year) {
        return (year - 1) * 12L * GaiaCalendar.daysPerMonth();
    }

    /** The year of the next beastwave on or after today. */
    public static int nextWaveYear(long today) {
        int y = GaiaCalendar.year(today);
        while (!waveYear(y) || newYearDay(y) < today) y++;
        return y;
    }

    /** Every second on the server. */
    public static void tick(MinecraftServer server) {
        WorldState w = WorldState.get(server);
        long today = GaiaCalendar.today(server);
        long time = server.overworld().getDayTime() % 24000L;
        if (w.waveDay >= 0) {
            run(server, w);
            // Dawn after the night of the wave.
            if (today > w.waveDay && time < 12000) end(server, w);
            return;
        }
        int y = nextWaveYear(today);
        long left = newYearDay(y) - today;
        int key = y * 10 + (int) Math.min(9, left);
        if ((left == 3 || left == 1 || left == 0) && w.warnedYear != key) {
            w.warnedYear = key;
            w.touch();
            Notify.world(server, left == 0 ? Component.translatable("realmlord.beastwave.tonight")
                    : Component.translatable("realmlord.beastwave.warning", left));
        }
        if (left == 0 && w.lastWaveYear != y && time >= 12500) start(server, w, today, y);
    }

    /** Night falls on new year's day: the beasts come. */
    public static void start(MinecraftServer server, WorldState w, long today, int year) {
        w.waveDay = today;
        w.lastWaveYear = year;
        w.waveKills.clear();
        w.waveNames.clear();
        w.touch();
        FRONTS.clear();
        long now = server.overworld().getGameTime();
        for (Territory t : TerritoryData.get(server).all()) FRONTS.put(t.id, new Front(t.id, WAVES, now));
        Notify.world(server, Component.translatable("realmlord.beastwave.begins"));
    }

    private static void run(MinecraftServer server, WorldState w) {
        TerritoryData data = TerritoryData.get(server);
        for (Front f : new ArrayList<>(FRONTS.values())) {
            Territory t = data.byId(f.territory());
            ServerLevel level = t == null ? null : server.getLevel(t.dimension);
            if (t == null || level == null) {
                FRONTS.remove(f.territory());
                continue;
            }
            if (f.wavesLeft() <= 0 || level.getGameTime() < f.nextTick()) continue;
            int wave = WAVES - f.wavesLeft() + 1;
            if (level.isLoaded(t.center)) {
                int n = spawnWave(level, t, wave);
                Village.tell(level, t, Component.translatable("realmlord.beastwave.wave", wave, WAVES, n, t.name));
            } else {
                offScreen(level, t, wave);
            }
            FRONTS.put(f.territory(), new Front(f.territory(), f.wavesLeft() - 1, level.getGameTime() + WAVE_GAP));
        }
    }

    /** How many beasts a wave sends against a village: more against bigger villages, a king with the last. */
    public static int waveSize(Territory t, int wave) {
        int base = 5 + t.residents.size() / 12 + t.level.ordinal() * 3;
        return Math.min(40, base + (wave - 1) * 3);
    }

    /** Beasts gather in a ring just outside the border and drive on the manor. */
    public static int spawnWave(ServerLevel level, Territory t, int wave) {
        RandomSource r = level.getRandom();
        int n = waveSize(t, wave), made = 0;
        List<EntityType<? extends Mob>> kinds = List.of(ModEntities.WILD_BOAR.get(), ModEntities.WILD_BOAR.get(), ModEntities.GREY_WOLF.get(),
                ModEntities.GREY_WOLF.get(), ModEntities.MOUNTAIN_TIGER.get(), ModEntities.SNOW_BEAR.get());
        double a0 = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            EntityType<? extends Mob> kind = kinds.get(r.nextInt(kinds.size()));
            if (wave == WAVES && i == 0 && t.level.ordinal() >= TerritoryLevel.VILLAGE_2.ordinal()) kind = ModEntities.BOAR_KING.get();
            // Mostly from one side, but anywhere on dry land around the village will do.
            BlockPos at = null;
            for (int tries = 0; tries < 8 && at == null; tries++) {
                double a = tries < 3 ? a0 + (r.nextDouble() - 0.5) * 1.6 : r.nextDouble() * Math.PI * 2;
                int d = t.radius() + 16 + r.nextInt(12);
                int x = t.center.getX() + (int) (Math.cos(a) * d), z = t.center.getZ() + (int) (Math.sin(a) * d);
                if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
                int y = Village.groundAt(level, x, z);
                if (level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) at = new BlockPos(x, y, z);
            }
            if (at == null) continue;
            int x = at.getX(), y = at.getY(), z = at.getZ();
            Mob m = kind.create(level);
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360F, 0F);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null, null);
            Beasts.setRaid(m, t.center);
            m.getPersistentData().putInt("realmlord_wave_territory", t.id);
            level.addFreshEntity(m);
            made++;
        }
        return made;
    }

    /** A village nobody is near fights the wave unseen: a finished fence and a militia keep it safe. */
    public static void offScreen(ServerLevel level, Territory t, int wave) {
        boolean fence = com.realmlord.territory.TerritoryEvents.fenced(level, t.center);
        int militia = Village.militiaCount(t);
        int beasts = waveSize(t, wave);
        if (fence && militia * 2 >= beasts) return;
        double share = fence ? 0.03 : 0.08;
        long food = Math.round(t.food * share);
        t.food -= food;
        int lost = fence ? 0 : Math.min(3, Math.max(0, (beasts - militia * 2) / 8));
        for (int i = 0; i < lost && t.residents.size() > 5; i++) {
            Resident r = t.residents.get(level.getRandom().nextInt(t.residents.size()));
            if (r.isFigure()) continue;
            t.residents.remove(r);
        }
        Village.tell(level, t, Component.translatable("realmlord.beastwave.unseen", wave, t.name, food, lost));
    }

    /** A beast of the wave is killed: the player (or the Lord whose militia did it) is credited. */
    public static void killed(MinecraftServer server, Entity beast, Entity killer) {
        WorldState w = WorldState.get(server);
        if (w.waveDay < 0 || !beast.getTags().contains(Beasts.WAVE_TAG)) return;
        UUID who = null;
        String name = null;
        if (killer instanceof ServerPlayer p) {
            who = p.getUUID();
            name = p.getGameProfile().getName();
        } else if (killer instanceof com.realmlord.entity.SettlerEntity s && s.getTerritoryId() >= 0) {
            Territory t = TerritoryData.get(server).byId(s.getTerritoryId());
            if (t != null && t.owner != null) {
                who = t.owner;
                name = t.ownerName;
            }
        }
        if (who == null) return;
        w.waveKills.merge(who, 1, Integer::sum);
        w.waveNames.put(who, name);
        w.touch();
    }

    /** Dawn: the beasts that are left melt back into the wilds, and the defenders are rewarded. */
    public static void end(MinecraftServer server, WorldState w) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) if (e.getTags().contains(Beasts.WAVE_TAG)) e.discard();
        }
        List<Map.Entry<UUID, Integer>> rank = new ArrayList<>(w.waveKills.entrySet());
        rank.sort((a, b) -> b.getValue() - a.getValue());
        Notify.world(server, Component.translatable("realmlord.beastwave.over"));
        for (int i = 0; i < rank.size(); i++) {
            UUID u = rank.get(i).getKey();
            int kills = rank.get(i).getValue();
            int gold = i < PRIZES.length ? PRIZES[i] : kills >= 5 ? 1 : 0;
            String name = w.waveNames.getOrDefault(u, "?");
            if (i < PRIZES.length) Notify.world(server, Component.translatable("realmlord.beastwave.rank", i + 1, name, kills, gold));
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) {
                if (gold > 0) com.realmlord.village.Coins.give(p, gold * 10_000L);
                com.realmlord.profile.Profiles.grantMerit(p, kills * 5L, Component.translatable("realmlord.reason.beastwave"));
            }
        }
        w.waveDay = -1;
        w.touch();
        FRONTS.clear();
    }

    /** For operators: start a beastwave now, or end the one running. */
    public static void force(MinecraftServer server, boolean on) {
        WorldState w = WorldState.get(server);
        long today = GaiaCalendar.today(server);
        if (on) start(server, w, today, GaiaCalendar.year(today));
        else if (w.waveDay >= 0) end(server, w);
    }
}
