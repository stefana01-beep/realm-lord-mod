package com.realmlord.territory;

import com.mojang.datafixers.util.Pair;
import com.realmlord.Notify;
import com.realmlord.RealmLord;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.OpenScreenPacket;
import com.realmlord.network.SpawnOptionsPacket;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Choosing where to begin. New players pick a starting place before anything else: on the
 * China world, from the great regions of the map (or anywhere they click on it); on other
 * worlds, from the kinds of country found around the world spawn.
 */
public final class SpawnPicker {
    private SpawnPicker() {}

    public record Option(String key, int x, int z) {}

    /** The China world's recommended regions: key, longitude, latitude (a little off each city, on open land). */
    private static final Object[][] CHINA = {
            {"shanhai", 109.20, 21.845},
            {"central_plains", 112.60, 34.70},
            {"guanzhong", 108.80, 34.40},
            {"sichuan", 104.00, 30.80},
            {"jiangnan", 118.70, 31.90},
            {"jingxiang", 112.20, 32.10},
            {"youyan", 116.30, 39.80},
            {"fujian", 118.50, 25.00},
            {"yunnan", 100.25, 25.70},
    };

    /** Kinds of country to look for on ordinary worlds. */
    private static final Object[][] COUNTRY = {
            {"plains", Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW)},
            {"forest", Set.of(Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST)},
            {"river", Set.of(Biomes.RIVER)},
            {"coast", Set.of(Biomes.BEACH)},
            {"savanna", Set.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU)},
            {"taiga", Set.of(Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA)},
            {"hills", Set.of(Biomes.CHERRY_GROVE, Biomes.WINDSWEPT_HILLS)},
            {"jungle", Set.of(Biomes.SPARSE_JUNGLE, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE)},
    };

    private static final Map<ServerLevel, List<Option>> CACHE = new WeakHashMap<>();

    public static boolean isChina(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD && level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g
                && g.generatorSettings().is(RealmLord.id("china")) && ChinaMap.ready();
    }

    public static List<Option> options(ServerLevel level) {
        return CACHE.computeIfAbsent(level, l -> isChina(l) ? china() : country(l));
    }

    private static List<Option> china() {
        List<Option> out = new ArrayList<>();
        for (Object[] c : CHINA) {
            int[] b = ChinaMap.blockOf((double) c[1], (double) c[2]);
            out.add(new Option("realmlord.spawn.china." + c[0], b[0], b[1]));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Option> country(ServerLevel level) {
        List<Option> out = new ArrayList<>();
        BlockPos spawn = level.getSharedSpawnPos();
        out.add(new Option("realmlord.spawn.world_spawn", spawn.getX(), spawn.getZ()));
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        for (Object[] c : COUNTRY) {
            Set<ResourceKey<Biome>> want = (Set<ResourceKey<Biome>>) c[1];
            try {
                Pair<BlockPos, Holder<Biome>> found = source.findBiomeHorizontal(spawn.getX(), 64, spawn.getZ(), 3000, 48,
                        h -> want.stream().anyMatch(k -> h.is(k)), level.getRandom(), true, sampler);
                if (found != null) out.add(new Option("realmlord.spawn.biome." + c[0], found.getFirst().getX(), found.getFirst().getZ()));
            } catch (RuntimeException e) {
                RealmLord.LOGGER.warn("Could not look for {} near spawn", c[0], e);
            }
            if (out.size() >= 8) break;
        }
        return out;
    }

    /** Shows a player the starting places. */
    public static void offer(ServerPlayer p) {
        ServerLevel level = p.server.overworld();
        ModNetwork.send(p, new SpawnOptionsPacket(isChina(level), options(level), level.getSharedSpawnPos()));
    }

    /** Takes a player to their chosen place. Returns an error key, or null. */
    public static String choose(ServerPlayer p, int x, int z, boolean admin) {
        Profile prof = Profiles.get(p);
        if (prof.isPlaced() && !admin) return "realmlord.spawn.err.placed";
        ServerLevel level = p.server.overworld();
        boolean china = isChina(level);
        boolean listed = options(level).stream().anyMatch(o -> o.x() == x && o.z() == z);
        if (!listed) {
            if (!china) return "realmlord.spawn.err.far";
            if (!ChinaMap.onMap(x, z) || ChinaMap.inside(x, z) < 0.5) return "realmlord.spawn.err.far";
            if (ChinaMap.sample(ChinaMap.CONT, x, z) < -0.1) return "realmlord.spawn.err.sea";
        }
        if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 64, z))) return "realmlord.spawn.err.far";
        BlockPos at = land(level, x, z);
        if (at == null) return "realmlord.spawn.err.sea";
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.setRespawnPosition(level.dimension(), at, 0, true, false);
        prof.setPlaced(true);
        ModNetwork.sync(p);
        Notify.player(p, Component.translatable("realmlord.spawn.arrived", at.getX(), at.getZ()));
        // A Lord's quest clock starts once they arrive, not while they were choosing.
        if (prof.isLord() && prof.quest().stage == com.realmlord.quest.VillageQuest.Stage.SUPPLIES
                && prof.quest().logs == 0 && prof.quest().stone == 0 && prof.quest().food == 0) {
            prof.quest().startTick = p.server.overworld().getGameTime();
            prof.markDirty();
            ModNetwork.sync(p);
        }
        if (!prof.hasChosen()) ModNetwork.send(p, new OpenScreenPacket(OpenScreenPacket.ROLE));
        return null;
    }

    /** Dry, open ground near a point (searching outwards up to 64 blocks), or null. */
    public static BlockPos land(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 64; r += 4) {
            int steps = Math.max(1, r * 2);
            for (int i = 0; i < steps; i++) {
                double a = 2 * Math.PI * i / steps;
                int bx = x + (int) Math.round(Math.cos(a) * r), bz = z + (int) Math.round(Math.sin(a) * r);
                // Far places have not been generated yet: make the ground before looking at it.
                level.getChunk(bx >> 4, bz >> 4);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                BlockPos ground = new BlockPos(bx, y - 1, bz);
                var s = level.getBlockState(ground);
                if (!s.getFluidState().isEmpty() || s.is(BlockTags.LEAVES) || y <= level.getMinBuildHeight() + 1) continue;
                return ground.above();
            }
        }
        return null;
    }
}
