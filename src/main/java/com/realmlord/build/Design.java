package com.realmlord.build;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A building drawn block by block: w across, h high, d deep. Each cell is -1 (leave the world
 * as it is), 0 (clear to air) or an index into the palette. The front (z = 0) is the entrance,
 * which faces whoever builds it.
 */
public final class Design {
    public final int w, h, d, yOff;
    public final BlockState[] palette;
    public final int[] cells;

    public Design(int w, int h, int d, int yOff, BlockState[] palette, int[] cells) {
        this.w = w;
        this.h = h;
        this.d = d;
        this.yOff = yOff;
        this.palette = palette;
        this.cells = cells;
    }

    public int index(int x, int y, int z) {
        return (y * d + z) * w + x;
    }

    public int at(int x, int y, int z) {
        return cells[index(x, y, z)];
    }

    /** How many blocks it places. */
    public int blocks() {
        int n = 0;
        for (int c : cells) if (c > 0) n++;
        return n;
    }

    /** The same building turned (looking down, clockwise for CLOCKWISE_90). */
    public Design rotated(Rotation r) {
        if (r == Rotation.NONE) return this;
        boolean swap = r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
        int nw = swap ? d : w, nd = swap ? w : d;
        BlockState[] pal = new BlockState[palette.length];
        for (int i = 0; i < pal.length; i++) pal[i] = palette[i].rotate(r);
        int[] out = new int[cells.length];
        java.util.Arrays.fill(out, -1);
        for (int y = 0; y < h; y++)
            for (int z = 0; z < d; z++)
                for (int x = 0; x < w; x++) {
                    int nx, nz;
                    switch (r) {
                        case CLOCKWISE_90 -> { nx = d - 1 - z; nz = x; }
                        case CLOCKWISE_180 -> { nx = w - 1 - x; nz = d - 1 - z; }
                        default -> { nx = z; nz = w - 1 - x; }
                    }
                    out[(y * nd + nz) * nw + nx] = at(x, y, z);
                }
        return new Design(nw, h, nd, yOff, pal, out);
    }

    public static Rotation inverse(Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> r;
        };
    }

    /** Copies a box of the world (corners inclusive). Blocks that can't be built from items are left out. */
    public static Design capture(Level level, BlockPos a, BlockPos b) {
        int x0 = Math.min(a.getX(), b.getX()), y0 = Math.min(a.getY(), b.getY()), z0 = Math.min(a.getZ(), b.getZ());
        int w = Math.abs(a.getX() - b.getX()) + 1, h = Math.abs(a.getY() - b.getY()) + 1, d = Math.abs(a.getZ() - b.getZ()) + 1;
        java.util.List<BlockState> pal = new java.util.ArrayList<>();
        pal.add(Blocks.AIR.defaultBlockState());
        java.util.Map<BlockState, Integer> idx = new java.util.HashMap<>();
        idx.put(Blocks.AIR.defaultBlockState(), 0);
        int[] cells = new int[w * h * d];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = 0; y < h; y++)
            for (int z = 0; z < d; z++)
                for (int x = 0; x < w; x++) {
                    p.set(x0 + x, y0 + y, z0 + z);
                    BlockState s = level.getBlockState(p);
                    int i = (y * d + z) * w + x;
                    if (s.isAir()) { cells[i] = 0; continue; }
                    if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))
                        s = s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false);
                    if (Construction.cost(s) == null) { cells[i] = -1; continue; }
                    final BlockState st = s;
                    cells[i] = idx.computeIfAbsent(st, k -> { pal.add(k); return pal.size() - 1; });
                }
        return new Design(w, h, d, 0, pal.toArray(new BlockState[0]), cells);
    }

    // ------------------------------------------------------------------ saving

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("size", new int[]{w, h, d, yOff});
        ListTag pal = new ListTag();
        for (BlockState s : palette) pal.add(NbtUtils.writeBlockState(s));
        t.put("palette", pal);
        t.putIntArray("cells", cells);
        return t;
    }

    public static Design load(CompoundTag t) {
        int[] s = t.getIntArray("size");
        ListTag pal = t.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] p = new BlockState[pal.size()];
        for (int i = 0; i < p.length; i++) p[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), pal.getCompound(i));
        return new Design(s[0], s[1], s[2], s.length > 3 ? s[3] : 0, p, t.getIntArray("cells"));
    }

    /** Reads a design written by tools/gen_designs.py. */
    public static Design fromJson(JsonObject j) {
        JsonArray size = j.getAsJsonArray("size");
        JsonArray pal = j.getAsJsonArray("palette");
        BlockState[] p = new BlockState[pal.size()];
        for (int i = 0; i < p.length; i++) {
            try {
                p[i] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), pal.get(i).getAsString(), false).blockState();
            } catch (Exception e) {
                p[i] = Blocks.AIR.defaultBlockState();
            }
        }
        JsonArray c = j.getAsJsonArray("cells");
        int[] cells = new int[c.size()];
        for (int i = 0; i < cells.length; i++) cells[i] = c.get(i).getAsInt();
        return new Design(size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(),
                j.has("yOff") ? j.get("yOff").getAsInt() : 0, p, cells);
    }
}
