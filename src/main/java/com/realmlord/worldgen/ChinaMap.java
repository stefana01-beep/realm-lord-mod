package com.realmlord.worldgen;

import com.realmlord.RealmLord;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * The China world's map (format 2), made by tools/gen_china.py from real elevation (NOAA
 * ETOPO2), the Natural Earth coastline, rivers and lakes. Luoyang is block 0,0; one block is
 * 50 m; one map pixel covers 64 blocks. It holds the surface height and, for Minecraft's biome
 * choice, continentalness, erosion, temperature and humidity, plus how rugged the land is and
 * how far the nearest river is.
 */
public final class ChinaMap {
    private ChinaMap() {}

    public static final int CONT = 0, ERO = 1, TEMP = 2, VEG = 3, RELIEF = 4, RIVER_DIST = 5, RIVER_STR = 6;
    /** Pixels over which the map fades into ordinary terrain at its edges. */
    private static final double EDGE = 40;

    private static int w, h, x0, z0, px;
    private static double originLon, originLat, metersPerBlock, kmPerDegLon;
    private static short[] surf;
    private static byte[][] data;
    private static boolean loaded, failed;

    /** The novel's nine Imperial Capitals, at their real places: name, longitude, latitude. */
    public static final String[] CAPITALS = {"Jingdu", "Xianyang", "Chengdu", "Dali", "Luoyang", "Changan", "Xiangyang", "Jianye", "Quanzhou"};
    public static final double[][] CAPITAL_LONLAT = {{116.40, 39.90}, {108.71, 34.33}, {104.07, 30.66}, {100.23, 25.61}, {112.45, 34.62},
            {108.94, 34.26}, {112.14, 32.04}, {118.80, 32.06}, {118.59, 24.91}};

    private static synchronized void load() {
        if (loaded || failed) return;
        try (InputStream raw = ChinaMap.class.getResourceAsStream("/realmlord/china/map.bin.gz")) {
            if (raw == null) throw new IOException("map missing");
            DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw), 1 << 16));
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'R' || magic[1] != 'L' || magic[2] != 'C' || magic[3] != 'N') throw new IOException("bad map");
            int version = in.readInt();
            if (version != 2) throw new IOException("map format " + version + " not supported");
            w = in.readInt();
            h = in.readInt();
            x0 = in.readInt();
            z0 = in.readInt();
            px = in.readInt();
            originLon = in.readDouble();
            originLat = in.readDouble();
            metersPerBlock = in.readDouble();
            kmPerDegLon = in.readDouble();
            // Every plane is stored as differences along each row.
            byte[] buf = new byte[w * h * 2];
            in.readFully(buf);
            short[] s = new short[w * h];
            for (int r = 0; r < h; r++) {
                int acc = 0;
                for (int c = 0; c < w; c++) {
                    int i = r * w + c;
                    acc += (short) (((buf[2 * i] & 0xFF) << 8) | (buf[2 * i + 1] & 0xFF));
                    s[i] = (short) acc;
                }
            }
            int planes = in.readInt();
            byte[][] d = new byte[planes][w * h];
            for (int p = 0; p < planes; p++) {
                in.readFully(d[p]);
                for (int r = 0; r < h; r++) {
                    int acc = 0;
                    for (int c = 0; c < w; c++) {
                        int i = r * w + c;
                        acc += d[p][i];
                        d[p][i] = (byte) acc;
                    }
                }
            }
            surf = s;
            data = d;
            loaded = true;
        } catch (IOException e) {
            failed = true;
            RealmLord.LOGGER.error("Could not load the China map; the China world will fall back to ordinary terrain", e);
        }
    }

    public static boolean ready() {
        load();
        return loaded;
    }

    // ------------------------------------------------------------------ geometry

    /** Block position of the map's top-left corner, and blocks per map pixel. */
    public static int originX() {
        load();
        return x0;
    }

    public static int originZ() {
        load();
        return z0;
    }

    public static int pixelSize() {
        load();
        return px;
    }

    public static int width() {
        load();
        return w;
    }

    public static int height() {
        load();
        return h;
    }

    public static double metersPerBlock() {
        load();
        return metersPerBlock;
    }

    /** The block at a real longitude and latitude: {x, z}. */
    public static int[] blockOf(double lon, double lat) {
        load();
        return new int[]{(int) Math.round((lon - originLon) * kmPerDegLon * 1000 / metersPerBlock),
                (int) Math.round(-(lat - originLat) * 111.32 * 1000 / metersPerBlock)};
    }

    /** The longitude and latitude of a block: {lon, lat}. */
    public static double[] lonLatOf(double bx, double bz) {
        load();
        return new double[]{originLon + bx * metersPerBlock / 1000 / kmPerDegLon, originLat - bz * metersPerBlock / 1000 / 111.32};
    }

    /** True if a block position lies on the map. */
    public static boolean onMap(double bx, double bz) {
        load();
        return loaded && bx >= x0 && bz >= z0 && bx < x0 + (double) w * px && bz < z0 + (double) h * px;
    }

    /** 1 well inside the map, falling to 0 at and beyond its edges. */
    public static double inside(double bx, double bz) {
        double fx = (bx - x0) / px, fz = (bz - z0) / px;
        double edge = Math.min(Math.min(fx, w - fx), Math.min(fz, h - fz));
        return Math.max(0, Math.min(1, edge / EDGE));
    }

    // ------------------------------------------------------------------ sampling

    private static int idx(int ix, int iz) {
        ix = Math.max(0, Math.min(w - 1, ix));
        iz = Math.max(0, Math.min(h - 1, iz));
        return iz * w + ix;
    }

    private static double at(int channel, int ix, int iz) {
        byte b = data[channel][idx(ix, iz)];
        if (channel == RIVER_DIST) return (b & 0xFF) * 4.0;
        return b / 100.0;
    }

    private static double surfAt(int ix, int iz) {
        return surf[idx(ix, iz)] / 8.0;
    }

    /** A channel's raw value at a pixel. */
    public static double pixel(int channel, int ix, int iz) {
        load();
        return loaded ? at(channel, ix, iz) : 0;
    }

    /** A channel at a block, smoothly between pixels. */
    public static double sample(int channel, double bx, double bz) {
        load();
        if (!loaded) return 0;
        double fx = (bx - x0) / px - 0.5, fz = (bz - z0) / px - 0.5;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        double a = at(channel, ix, iz), b = at(channel, ix + 1, iz), c = at(channel, ix, iz + 1), d = at(channel, ix + 1, iz + 1);
        return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz;
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    /** The real ground height at a block, as a Minecraft y (smooth, before rivers and detail). */
    public static double surfaceY(double bx, double bz) {
        load();
        if (!loaded) return 64;
        double fx = (bx - x0) / px - 0.5, fz = (bz - z0) / px - 0.5;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        double[] row = new double[4];
        for (int j = -1; j <= 2; j++) {
            row[j + 1] = cubic(surfAt(ix - 1, iz + j), surfAt(ix, iz + j), surfAt(ix + 1, iz + j), surfAt(ix + 2, iz + j), tx);
        }
        return cubic(row[0], row[1], row[2], row[3], tz);
    }

    private static double smooth(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    /** How much a block lies in a river: 1 in the channel, falling to 0 across its banks. */
    public static double riverAt(double bx, double bz) {
        double d = sample(RIVER_DIST, bx, bz);
        if (d >= 1000) return 0;
        double s = sample(RIVER_STR, bx, bz);
        double half = halfWidth(s);
        return smooth(half + 8 + 16 * s, half, d);
    }

    /** Half the width of a river of a given size, in blocks (the Yangtze is about 50 across). */
    public static double halfWidth(double strength) {
        return 2 + 22 * strength;
    }

    /** The ground height with rivers cut in: lowland rivers down to the sea's level, upland ones a few blocks. */
    public static double groundY(double bx, double bz) {
        double y = surfaceY(bx, bz);
        double t = riverAt(bx, bz);
        if (t <= 0) return y;
        double s = sample(RIVER_STR, bx, bz);
        double bed;
        if (y <= 80) bed = 57;
        else if (y <= 100) bed = 57 + (y - 5 - 57) * (y - 80) / 20.0;
        else bed = y - (3 + 4 * s);
        return y + (Math.min(bed, y) - y) * t;
    }

    // ------------------------------------------------------------------ terrain inputs

    /**
     * A value for a terrain channel at a block. "offset" is the land's height for Minecraft's
     * shaper (a surface at y is offset y/128 - 1), with vanilla noise adding hills in rugged
     * country; the others steer the choice of biomes. Beyond the map the world is ordinary.
     */
    public static double value(String channel, double bx, double bz, double detail, double amount, double fallback) {
        if (!ready()) return fallback;
        double in = inside(bx, bz);
        if (in <= 0) return fallback;
        double v;
        switch (channel) {
            case "offset" -> {
                double y = groundY(bx, bz);
                double rugged = sample(RELIEF, bx, bz) * (1 - riverAt(bx, bz));
                y += rugged * 18 * detail;
                v = y / 128.0 - 1.0;
            }
            case "continents" -> {
                v = sample(CONT, bx, bz) + detail * amount;
                double t = riverAt(bx, bz);
                if (t > 0 && surfaceY(bx, bz) <= 100) v = v * (1 - t) + -0.16 * t;
            }
            case "erosion" -> {
                v = sample(ERO, bx, bz) + detail * amount;
                double t = riverAt(bx, bz);
                if (t > 0) v = v * (1 - t) + 0.55 * t;
            }
            case "ridges" -> {
                double m = sample(RELIEF, bx, bz);
                v = detail * (1 - m) + Math.copySign(0.67, detail) * m;
                v *= 1 - riverAt(bx, bz);
            }
            case "factor" -> {
                // A higher factor holds the surface to the real height; low relief means flat land.
                double rugged = sample(RELIEF, bx, bz);
                v = detail * (1 + 3.0 * (1 - rugged));
            }
            case "temperature" -> v = sample(TEMP, bx, bz) + detail * amount;
            case "vegetation" -> v = sample(VEG, bx, bz) + detail * amount;
            default -> v = fallback;
        }
        return fallback + (v - fallback) * in;
    }
}
