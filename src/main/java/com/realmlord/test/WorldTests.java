package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Checks the Earth Online: China world's terrain at known places. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class WorldTests {
    private static final double KM_LON = 111.32 * Math.cos(Math.toRadians(35)), KM_LAT = 111.32, M = 50.0;

    private static int bx(double lon) {
        return (int) Math.round((lon - 112.45) * KM_LON * 1000 / M);
    }

    private static int bz(double lat) {
        return (int) Math.round(-(lat - 34.62) * KM_LAT * 1000 / M);
    }

    @GameTest(template = "empty", timeoutTicks = 2000, batch = "china")
    public static void chinaTerrainFollowsGeography(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(ChinaMap.ready(), "the China map did not load");
        var reg = level.registryAccess();
        Holder<NoiseGeneratorSettings> settings = reg.registryOrThrow(Registries.NOISE_SETTINGS)
                .getHolderOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, RealmLord.id("china")));
        RandomState rs = RandomState.create(settings.value(), reg.lookupOrThrow(Registries.NOISE), 20190101L);
        NoiseBasedChunkGenerator gen = new NoiseBasedChunkGenerator(level.getChunkSource().getGenerator().getBiomeSource(), settings);
        var router = rs.router();
        // The China world is taller than the test world: y -64 to 576.
        var tall = net.minecraft.world.level.LevelHeightAccessor.create(-64, 640);

        // A coarse survey of the whole map for the build log (heights and biomes).
        StringBuilder row = new StringBuilder();
        for (int z = -47000; z <= 46000; z += 1792) {
            row.setLength(0);
            for (int x = -81000; x <= 50000; x += 1792) {
                int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, tall, rs);
                Holder<Biome> b = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(h, 63) + 8), QuartPos.fromBlock(z), rs.sampler());
                String id = b.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                row.append(h).append(':').append(id).append(' ');
            }
            RealmLord.LOGGER.info("CHINAMAP {}", row.toString().trim());
        }
        // Tibetan Plateau (Lhasa area): high and cold.
        int tx = bx(91.1), tz = bz(31.5);
        double tTemp = router.temperature().compute(new DensityFunction.SinglePointContext(tx, 64, tz));
        int tibet = gen.getBaseHeight(tx, tz, Heightmap.Types.OCEAN_FLOOR_WG, tall, rs);
        // East China Sea.
        int sea = gen.getBaseHeight(bx(125), bz(29), Heightmap.Types.OCEAN_FLOOR_WG, tall, rs);
        // North China Plain (between Jinan and Zhengzhou).
        int plain = gen.getBaseHeight(bx(115.5), bz(35.5), Heightmap.Types.OCEAN_FLOOR_WG, tall, rs);
        // Taklamakan Desert: hot and dry.
        int kx = bx(83), kz = bz(39);
        double kTemp = router.temperature().compute(new DensityFunction.SinglePointContext(kx, 64, kz));
        double kVeg = router.vegetation().compute(new DensityFunction.SinglePointContext(kx, 64, kz));
        // The Sichuan Basin (Chengdu, ~500 m) sits well above the plains and far below Tibet.
        int sichuan = gen.getBaseHeight(bx(104.07), bz(30.66), Heightmap.Types.OCEAN_FLOOR_WG, tall, rs);
        // The Yangtze at Wuhan: a river bed below the sea's level, so it holds water.
        int[] wuhan = ChinaMap.blockOf(114.3, 30.6);
        int yangtze = 999;
        for (int dx = -60; dx <= 60; dx += 4)
            for (int dz = -60; dz <= 60; dz += 4)
                yangtze = Math.min(yangtze, gen.getBaseHeight(wuhan[0] + dx, wuhan[1] + dz, Heightmap.Types.OCEAN_FLOOR_WG, tall, rs));
        RealmLord.LOGGER.info("CHINA TEST tibet={} (temp {}), sea={}, plain={}, sichuan={}, yangtze bed={}, taklamakan temp {} veg {}",
                tibet, tTemp, sea, plain, sichuan, yangtze, kTemp, kVeg);
        helper.assertTrue(tibet > 300, "the Tibetan Plateau should be high, is " + tibet);
        helper.assertTrue(sichuan > 85 && sichuan < 170, "the Sichuan Basin should be raised but low-lying, is " + sichuan);
        helper.assertTrue(yangtze < 63, "the Yangtze at Wuhan should be a river below the sea's level, bed at " + yangtze);
        helper.assertTrue(tTemp < -0.3, "Tibet should be cold, temperature " + tTemp);
        helper.assertTrue(sea < 60, "the East China Sea should be sea, floor at " + sea);
        helper.assertTrue(plain >= 60 && plain < 110, "the North China Plain should be low land, is " + plain);
        helper.assertTrue(kTemp > 0.3 && kVeg < -0.5, "the Taklamakan should be hot and dry: " + kTemp + " / " + kVeg);

        helper.succeed();
    }
}
