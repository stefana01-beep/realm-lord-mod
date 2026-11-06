package com.realmlord.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.realmlord.RealmLord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The land you have seen, drawn like a vanilla map, for the world map's close-up view. Every
 * chunk the game loads around you is drawn into 256-block tiles; tiles near you are redrawn now
 * and then so that what you build shows up. Kept for the session only.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExploredMap {
    private ExploredMap() {}

    public static final int TILE = 256;

    public static final class Tile {
        public final ResourceLocation id;
        final DynamicTexture texture;
        boolean dirty;

        Tile(ResourceLocation id) {
            this.id = id;
            NativeImage img = new NativeImage(TILE, TILE, true);
            img.fillRect(0, 0, TILE, TILE, 0);
            this.texture = new DynamicTexture(img);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }

        /** Uploads changes before the tile is drawn. */
        public void prepare() {
            if (dirty) {
                texture.upload();
                dirty = false;
            }
        }
    }

    /** Tiles by dimension, then by tile x/z packed into a long. */
    private static final Map<String, Map<Long, Tile>> TILES = new HashMap<>();
    private static final ArrayDeque<ChunkPos> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static int counter;

    private static long key(int tx, int tz) {
        return ((long) tx << 32) ^ (tz & 0xffffffffL);
    }

    public static Tile tile(String dim, int tx, int tz) {
        Map<Long, Tile> m = TILES.get(dim);
        return m == null ? null : m.get(key(tx, tz));
    }

    private static Tile tileFor(String dim, int tx, int tz) {
        return TILES.computeIfAbsent(dim, d -> new HashMap<>()).computeIfAbsent(key(tx, tz), k -> {
            String safe = (dim + "_" + tx + "_" + tz).replaceAll("[^a-z0-9_.-]", "_");
            return new Tile(RealmLord.id("dynamic/explored/" + safe.replace('-', 'm')));
        });
    }

    private static void enqueue(ChunkPos p) {
        if (QUEUED.add(p.toLong())) QUEUE.add(p);
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) enqueue(event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        // Every few seconds, the chunks right around you again.
        if (++counter % 100 == 0) {
            ChunkPos c = mc.player.chunkPosition();
            for (int dx = -3; dx <= 3; dx++)
                for (int dz = -3; dz <= 3; dz++) enqueue(new ChunkPos(c.x + dx, c.z + dz));
        }
        for (int n = 0; n < 24 && !QUEUE.isEmpty(); n++) {
            ChunkPos p = QUEUE.poll();
            QUEUED.remove(p.toLong());
            draw(level, p);
        }
    }

    private static void draw(ClientLevel level, ChunkPos p) {
        LevelChunk chunk = level.getChunkSource().getChunk(p.x, p.z, false);
        if (chunk == null || chunk.isEmpty()) return;
        String dim = level.dimension().location().toString();
        int bx = p.getMinBlockX(), bz = p.getMinBlockZ();
        Tile t = tileFor(dim, Math.floorDiv(bx, TILE), Math.floorDiv(bz, TILE));
        NativeImage img = t.texture.getPixels();
        if (img == null) return;
        int ox = Math.floorMod(bx, TILE), oz = Math.floorMod(bz, TILE);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            int prevY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, 0);
            for (int z = 0; z < 16; z++) {
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                pos.set(bx + x, y, bz + z);
                BlockState s = chunk.getBlockState(pos);
                int depth = 0;
                while (s.isAir() && pos.getY() > level.getMinBuildHeight()) {
                    pos.move(0, -1, 0);
                    s = chunk.getBlockState(pos);
                }
                MapColor col = s.getMapColor(level, pos);
                MapColor.Brightness br;
                if (!s.getFluidState().isEmpty()) {
                    // Water: darker where deep.
                    BlockPos.MutableBlockPos q = pos.mutable();
                    while (depth < 12 && !chunk.getBlockState(q).getFluidState().isEmpty()) {
                        q.move(0, -1, 0);
                        depth++;
                    }
                    br = depth > 6 ? MapColor.Brightness.LOW : depth > 2 ? MapColor.Brightness.NORMAL : MapColor.Brightness.HIGH;
                } else {
                    br = y > prevY ? MapColor.Brightness.HIGH : y < prevY ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                }
                prevY = y;
                int argb = col == MapColor.NONE ? 0 : col.calculateRGBColor(br);
                img.setPixelRGBA(ox + x, oz + z, argb);
            }
        }
        t.dirty = true;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Map<Long, Tile> m : TILES.values())
            for (Tile t : m.values()) Minecraft.getInstance().getTextureManager().release(t.id);
        TILES.clear();
        QUEUE.clear();
        QUEUED.clear();
        WorldMapData.clear();
    }
}
