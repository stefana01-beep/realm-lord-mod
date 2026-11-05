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
    private static final double KM_LON = 111.32 * Math.cos(Math.toRadians(35)), KM_LAT = 111.32, M = 280.0;

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

        // A coarse survey of the whole map for the build log (heights and biomes).
        StringBuilder row = new StringBuilder();
        for (int z = -8448; z <= 8192; z += 224) {
            row.setLength(0);
            for (int x = -14464; x <= 8960; x += 224) {
                int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
                Holder<Biome> b = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(h, 63) + 8), QuartPos.fromBlock(z), rs.sampler());
                String id = b.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                row.append(h).append(':').append(id).append(' ');
            }
            RealmLord.LOGGER.info("CHINAMAP {}", row.toString().trim());
        }
        // Tibetan Plateau (Lhasa area): high and cold.
        int tx = bx(91.1), tz = bz(31.5);
        double tTemp = router.temperature().compute(new DensityFunction.SinglePointContext(tx, 64, tz));
        int tibet = gen.getBaseHeight(tx, tz, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
        // East China Sea.
        int sea = gen.getBaseHeight(bx(125), bz(29), Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
        // North China Plain (between Jinan and Zhengzhou).
        int plain = gen.getBaseHeight(bx(115.5), bz(35.5), Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
        // Taklamakan Desert: hot and dry.
        int kx = bx(83), kz = bz(39);
        double kTemp = router.temperature().compute(new DensityFunction.SinglePointContext(kx, 64, kz));
        double kVeg = router.vegetation().compute(new DensityFunction.SinglePointContext(kx, 64, kz));
        RealmLord.LOGGER.info("CHINA TEST tibet={} (temp {}), sea={}, plain={}, taklamakan temp {} veg {}", tibet, tTemp, sea, plain, kTemp, kVeg);
        helper.assertTrue(tibet > 130, "the Tibetan Plateau should be high, is " + tibet);
        helper.assertTrue(tTemp < -0.3, "Tibet should be cold, temperature " + tTemp);
        helper.assertTrue(sea < 60, "the East China Sea should be sea, floor at " + sea);
        helper.assertTrue(plain >= 60 && plain < 110, "the North China Plain should be low land, is " + plain);
        helper.assertTrue(kTemp > 0.3 && kVeg < -0.5, "the Taklamakan should be hot and dry: " + kTemp + " / " + kVeg);

        helper.succeed();
    }
}
