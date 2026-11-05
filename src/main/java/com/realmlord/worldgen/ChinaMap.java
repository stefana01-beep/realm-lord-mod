package com.realmlord.worldgen;

import com.realmlord.RealmLord;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * The China world's map: continentalness, erosion, temperature, humidity, mountain and river
 * strength, one pixel per 32 blocks, centred on Luoyang at block 0,0 (about 280 m per block).
 * Made by tools/gen_china.py.
 */
public final class ChinaMap {
    private ChinaMap() {}

    public static final int CONT = 0, ERO = 1, TEMP = 2, VEG = 3, MOUNTAIN = 4, RIVER = 5, LIFT = 6;
    /** Pixels over which the map fades into ordinary terrain at its edges. */
    private static final double EDGE = 40;

    private static int w, h, x0, z0, px, channels;
    private static byte[][] data;
    private static boolean loaded, failed;

    /** The novel's nine Imperial Capitals, at their real places on the map. */
    public static final String[] CAPITALS = {"Jingdu", "Xianyang", "Chengdu", "Dali", "Luoyang", "Changan", "Xiangyang", "Jianye", "Quanzhou"};
    public static final int[][] CAPITAL_POS = {{1286, -2099}, {-1218, 115}, {-2729, 1574}, {-3980, 3582}, {0, 0}, {-1143, 143}, {-101, 1026}, {2068, 1018}, {2000, 3860}};

    private static synchronized void load() {
        if (loaded || failed) return;
        try (InputStream raw = ChinaMap.class.getResourceAsStream("/realmlord/china/map.bin.gz")) {
            if (raw == null) throw new IOException("map missing");
            DataInputStream in = new DataInputStream(new GZIPInputStream(raw));
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'R' || magic[1] != 'L' || magic[2] != 'C' || magic[3] != 'N') throw new IOException("bad map");
            in.readInt();
            w = in.readInt();
            h = in.readInt();
            x0 = in.readInt();
            z0 = in.readInt();
            px = in.readInt();
            channels = in.readInt();
            byte[][] d = new byte[channels][w * h];
            for (int c = 0; c < channels; c++) in.readFully(d[c]);
            data = d;
            loaded = true;
        } catch (IOException e) {
            failed = true;
            RealmLord.LOGGER.error("Could not load the China map; the China world will fall back to ordinary terrain", e);
        }
    }

    public static int width() {
        load();
        return w;
    }

    public static int height() {
        load();
        return h;
    }

    /** Block x of a map pixel's centre. */
    public static double blockX(double ix) {
        return x0 + (ix + 0.5) * px;
    }

    public static double blockZ(double iz) {
        return z0 + (iz + 0.5) * px;
    }

    /** A channel's raw value at a pixel. */
    public static double pixel(int channel, int ix, int iz) {
        load();
        return loaded ? at(channel, ix, iz) : 0;
    }

    /** True if a block position lies on the map. */
    public static boolean onMap(double bx, double bz) {
        load();
        return loaded && bx >= x0 && bz >= z0 && bx < x0 + (double) w * px && bz < z0 + (double) h * px;
    }

    public static boolean ready() {
        load();
        return loaded;
    }

    private static double at(int c, int x, int z) {
        x = Math.max(0, Math.min(w - 1, x));
        z = Math.max(0, Math.min(h - 1, z));
        return data[c][z * w + x] / 100.0;
    }

    /** A channel at a block position, smoothly interpolated between pixels. */
    public static double sample(int channel, double bx, double bz) {
        double fx = (bx - x0) / px - 0.5, fz = (bz - z0) / px - 0.5;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        double a = at(channel, ix, iz), b = at(channel, ix + 1, iz), c = at(channel, ix, iz + 1), d = at(channel, ix + 1, iz + 1);
        return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz;
    }

    /** 1 well inside the map, falling to 0 at and beyond its edges. */
    public static double inside(double bx, double bz) {
        double fx = (bx - x0) / px, fz = (bz - z0) / px;
        double edge = Math.min(Math.min(fx, w - fx), Math.min(fz, h - fz));
        return Math.max(0, Math.min(1, edge / EDGE));
    }

    /**
     * A climate value for a channel at a block, with vanilla's noise as detail. Mountains raise
     * peaks, great rivers cut valleys, and beyond the map the world is ordinary terrain.
     */
    public static double value(String channel, double bx, double bz, double detail, double amount) {
        if (!ready()) return detail;
        double in = inside(bx, bz);
        if (in <= 0) return detail;
        if (channel.equals("lift")) {
            // Raises the land by up to 200 blocks: terrain offset is 1/128 per block.
            return channels > LIFT ? in * sample(LIFT, bx, bz) * 200.0 / 128.0 : 0.0;
        }
        double river = sample(RIVER, bx, bz);
        double v;
        switch (channel) {
            case "continents" -> {
                v = sample(CONT, bx, bz) + detail * amount;
                if (river > 0) v = v * (1 - river) + -0.16 * river;
            }
            case "erosion" -> {
                v = sample(ERO, bx, bz) + detail * amount;
                if (river > 0) v = v * (1 - river) + 0.55 * river;
            }
            case "ridges" -> {
                double m = sample(MOUNTAIN, bx, bz);
                v = detail * (1 - m) + Math.copySign(0.67, detail) * m;
                v *= 1 - river;
            }
            case "temperature" -> v = sample(TEMP, bx, bz) + detail * amount;
            case "vegetation" -> v = sample(VEG, bx, bz) + detail * amount;
            default -> v = detail;
        }
        return detail + (v - detail) * in;
    }
}
