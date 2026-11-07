package com.realmlord.village;

import com.realmlord.RealmLord;
import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ground around every territory as it was last seen, kept with the world: each chunk near a
 * territory is drawn (like a vanilla map) when it loads and again when it unloads, so the build
 * map shows the land even where no one stands now. Land never loaded at all is sketched from the
 * world generator (its height, its water and its biome), a little dimmer.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerrainCache extends SavedData {
    private static final String NAME = "realmlord_terrain";
    /** How far from a territory's centre the ground is kept (the village and the country around). */
    public static final int KEEP = 640;

    private final Map<Long, int[]> chunks = new ConcurrentHashMap<>();
    private static final Map<String, Map<Long, Integer>> SKETCH = new HashMap<>();

    public static TerrainCache get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TerrainCache::load, TerrainCache::new, NAME);
    }

    public static TerrainCache load(CompoundTag tag) {
        TerrainCache c = new TerrainCache();
        for (String k : tag.getAllKeys()) {
            try {
                int[] a = tag.getIntArray(k);
                if (a.length == 256) c.chunks.put(Long.parseLong(k), a);
            } catch (NumberFormatException ignored) {}
        }
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<Long, int[]> e : chunks.entrySet()) tag.putIntArray(Long.toString(e.getKey()), e.getValue());
        return tag;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    /** The kept colour of a column (0xFFrrggbb), or 0 if its chunk was never seen. */
    public int colour(int x, int z) {
        int[] a = chunks.get(key(x >> 4, z >> 4));
        return a == null ? 0 : a[(z & 15) * 16 + (x & 15)];
    }

    static boolean near(ServerLevel level, int bx, int bz) {
        for (Territory t : Paths.ALL.get()) {
            if (!t.dimension.equals(level.dimension())) continue;
            long dx = bx + 8 - t.center.getX(), dz = bz + 8 - t.center.getZ();
            if (dx * dx + dz * dz <= (long) KEEP * KEEP) return true;
        }
        return false;
    }

    /** Draws a loaded chunk into the cache. */
    public static void record(ServerLevel level, LevelChunk chunk) {
        record(level, chunk, true);
    }

    /** As above; fresh false keeps a chunk already drawn (a chunk just loaded is as it was left). */
    public static void record(ServerLevel level, LevelChunk chunk, boolean fresh) {
        TerrainCache cache = get(level);
        long k = key(chunk.getPos().x, chunk.getPos().z);
        if (!fresh && cache.chunks.containsKey(k)) return;
        int bx = chunk.getPos().getMinBlockX(), bz = chunk.getPos().getMinBlockZ();
        if (!near(level, bx, bz)) return;
        int[] a = new int[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            int north = Integer.MIN_VALUE;
            for (int z = 0; z < 16; z++) {
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set(bx + x, y, bz + z);
                BlockState s = chunk.getBlockState(pos);
                while (s.isAir() && pos.getY() > level.getMinBuildHeight()) {
                    pos.move(0, -1, 0);
                    s = chunk.getBlockState(pos);
                }
                MapColor mc = s.getMapColor(chunk, pos);
                int base = mc == MapColor.NONE ? 0x6E6E6E : mc.col;
                double shade;
                if (!s.getFluidState().isEmpty()) {
                    int depth = 0;
                    BlockPos.MutableBlockPos q = pos.mutable();
                    while (depth < 10 && !chunk.getBlockState(q.move(0, -1, 0)).getFluidState().isEmpty()) depth++;
                    shade = 1.0 - depth * 0.04;
                } else {
                    shade = north == Integer.MIN_VALUE ? 0.88 : pos.getY() > north ? 1.0 : pos.getY() < north ? 0.78 : 0.88;
                }
                north = pos.getY();
                a[z * 16 + x] = 0xFF000000 | TerritoryMap.scale(base, shade);
            }
        }
        int[] old = cache.chunks.put(k, a);
        // Saved with the world only when the land has changed.
        if (old == null || !java.util.Arrays.equals(old, a)) cache.setDirty();
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel level && e.getChunk() instanceof LevelChunk c && level.getServer().isSameThread()) record(level, c, false);
    }

    /** A new world (or the same one opened again) starts its sketches afresh. */
    @SubscribeEvent
    public static void starting(net.minecraftforge.event.server.ServerStartingEvent e) {
        SKETCH.clear();
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel level && e.getChunk() instanceof LevelChunk c && level.getServer().isSameThread()) record(level, c);
    }

    /**
     * Land never seen, sketched from the generator: water where the surface stands over the
     * floor, sand on beaches and deserts, snow, stone on high ground, forest or grass, shaded by
     * slope and a little dimmer than land seen. Remembered for the session.
     */
    public static int sketch(ServerLevel level, int x, int z, int northY, int[] yOut) {
        Map<Long, Integer> memo = SKETCH.computeIfAbsent(level.dimension().location().toString(), d -> new HashMap<>());
        if (memo.size() > 400_000) memo.clear();
        long k = key(x, z);
        Integer kept = memo.get(k);
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        int floor, surface, base;
        if (kept != null) {
            yOut[0] = kept >>> 24 & 0xFF;
            yOut[0] = yOut[0] * 2 + level.getMinBuildHeight();
            base = kept & 0xFFFFFF;
            floor = yOut[0];
            surface = floor;
        } else {
            floor = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            surface = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
            Holder<Biome> biome = gen.getBiomeSource().getNoiseBiome(x >> 2, floor >> 2, z >> 2, rs.sampler());
            if (surface > floor + 1) base = MapColor.WATER.col;
            else if (biome.is(BiomeTags.IS_BEACH) || biome.is(net.minecraftforge.common.Tags.Biomes.IS_DESERT)) base = MapColor.SAND.col;
            else if (biome.value().coldEnoughToSnow(new BlockPos(x, floor, z))) base = MapColor.SNOW.col;
            else if (floor > 200) base = MapColor.STONE.col;
            else if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA)) base = MapColor.PLANT.col;
            else base = MapColor.GRASS.col;
            yOut[0] = floor;
            int packed = Math.max(0, Math.min(255, (floor - level.getMinBuildHeight()) / 2));
            memo.put(k, packed << 24 | base);
        }
        double shade;
        if (base == MapColor.WATER.col) shade = 0.8;
        else shade = northY == Integer.MIN_VALUE ? 0.88 : yOut[0] > northY + 1 ? 1.0 : yOut[0] < northY - 1 ? 0.76 : 0.88;
        return 0xFF000000 | TerritoryMap.scale(base, shade * 0.86);
    }
}
