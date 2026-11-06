package com.realmlord.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/** One building in a territory: where it stands, how far construction has got, and farm state. */
public class Building {
    public int id;
    public BuildingType type;
    /** Lowest corner of the footprint, at ground level. */
    public BlockPos origin = BlockPos.ZERO;
    /** 0-3 quarter turns. */
    public int rotation;
    /** Construction progress in builder-ticks, out of {@link #workNeeded}. */
    public long progress;
    public long workNeeded;
    public boolean complete;
    /** How many blocks of the design have been placed so far. */
    public int placed;
    /** Granted free by a village upgrade. */
    public boolean free;

    /** Site stages: clearing the ground, shaping it level, then building. */
    public static final int CLEAR = 0, SHAPE = 1, BUILD = 2;
    public int stage = CLEAR;
    /** Being taken down block by block, its materials carried back to the stores. */
    public boolean demolishing;
    /** Wood and stone the demolition will return in all, over {@link #demolishFrom} blocks. */
    public int salvageWood, salvageStone, demolishFrom;
    /** Game time a worker last did something here; a site left alone gets done by the village. */
    public long lastWork;
    /** Builders the Lord set for this site, or -1 to share the free builders with the other sites. */
    public int crew = -1;

    // The Simple Fence: its ring, the ground height of each post and where the gates are.
    public int fenceRadius;
    public int[] fenceYs;
    /** A path: its points (x, z pairs), surface, width, and the columns it covers (x, y, z, earth beneath). */
    public int[] pathPoints, pathCols;
    public int pathSurface, pathWidth = 1;
    /** A clearing: how high above its level the builders cut away; the layer they have reached (not saved). */
    public int clearHeight;
    /**
     * A clearing's columns, {x, z, cut, fill}: everything from cut up is taken away, and gaps
     * below fill are filled (NO_FILL for none). The ground at its edges is cut back and banked
     * up in a slope, and finished buildings, paths, the fence and the manor yard are left alone.
     */
    public int[] clearCols;
    /** Which design the fence was built to: 0 the old post-and-rail, 2 the plank palisade, 3 with a wall-walk. */
    public int fenceStyle;
    /** The towers the fence's design last allowed for (not saved). */
    public transient int towerSig = Integer.MIN_VALUE;
    /** A refit the Lord need not hear about when it is done. */
    public transient boolean quietRefit;
    public static final int NO_FILL = Integer.MIN_VALUE, NO_CUT = Integer.MAX_VALUE;
    public transient int clearTop = Integer.MAX_VALUE;
    /** Levelling the grounds: how many columns, counted from the manor outwards, are done. */
    public int groundsCursor;

    /** True if the site works a rectangle of ground: a building, or a clearing. */
    public boolean footprint() {
        return type.placed() || type == BuildingType.CLEARING;
    }
    /** Earth packed under each post where the ground falls away. */
    public int[] fenceFill;
    /** Before a building is finished, its whole design is checked against the world (transient). */
    public transient boolean verified;
    public transient int verifyCursor;
    /** Gate bearings in degrees, 0 = east, 90 = south. */
    public int[] gates = new int[0];

    // Farmland
    public boolean sown;
    public long sowDay;
    public float sowYield = 1.0F;

    /**
     * The footprint this building was laid out with (before turning). Fixed when it is placed, so
     * a later change to a building type's size never moves the walls of ones already standing.
     */
    public int w, d;

    public int width() {
        return w > 0 ? w : type.width;
    }

    public int depth() {
        return d > 0 ? d : type.depth;
    }

    public int sizeX() {
        return rotation % 2 == 0 ? width() : depth();
    }

    public int sizeZ() {
        return rotation % 2 == 0 ? depth() : width();
    }

    public BlockPos center() {
        return origin.offset(sizeX() / 2, 0, sizeZ() / 2);
    }

    public boolean overlaps(int x0, int z0, int sx, int sz, int gap) {
        return x0 - gap < origin.getX() + sizeX() && origin.getX() < x0 + sx + gap
                && z0 - gap < origin.getZ() + sizeZ() && origin.getZ() < z0 + sz + gap;
    }

    /** True if a bearing (degrees) falls in one of the fence's gateways, three posts wide. */
    public boolean isGate(double degrees) {
        double halfWidth = Math.toDegrees(1.6 / Math.max(4, fenceRadius));
        for (int g : gates) {
            double diff = Math.abs(((degrees - g) % 360 + 540) % 360 - 180);
            if (diff <= halfWidth) return true;
        }
        return false;
    }

    public double fraction() {
        return complete ? 1.0 : workNeeded <= 0 ? 0.0 : Math.min(1.0, progress / (double) workNeeded);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("type", type.id());
        t.put("origin", NbtUtils.writeBlockPos(origin));
        t.putInt("rot", rotation);
        t.putLong("progress", progress);
        t.putLong("need", workNeeded);
        t.putBoolean("complete", complete);
        t.putInt("placed", placed);
        t.putBoolean("free", free);
        t.putBoolean("sown", sown);
        t.putLong("sowDay", sowDay);
        t.putFloat("sowYield", sowYield);
        t.putInt("stage", stage);
        t.putBoolean("demolish", demolishing);
        t.putIntArray("salvage", new int[]{salvageWood, salvageStone, demolishFrom});
        t.putLong("lastWork", lastWork);
        t.putInt("crew", crew);
        t.putInt("w", width());
        t.putInt("d", depth());
        t.putInt("fenceR", fenceRadius);
        if (fenceYs != null) t.putIntArray("fenceYs", fenceYs);
        if (clearHeight > 0) t.putInt("clearHeight", clearHeight);
        if (groundsCursor > 0) t.putInt("groundsCursor", groundsCursor);
        if (clearCols != null) t.putIntArray("clearCols", clearCols);
        if (fenceStyle > 0) t.putInt("fenceStyle", fenceStyle);
        if (pathCols != null) {
            t.putIntArray("pathPts", pathPoints);
            t.putIntArray("pathCols", pathCols);
            t.putInt("pathSurface", pathSurface);
            t.putInt("pathWidth", pathWidth);
        }
        if (fenceFill != null) t.putIntArray("fenceFill", fenceFill);
        t.putIntArray("gates", gates);
        return t;
    }

    public static Building load(CompoundTag t) {
        BuildingType type = BuildingType.byId(t.getString("type"));
        if (type == null) return null;
        Building b = new Building();
        b.id = t.getInt("id");
        b.type = type;
        b.origin = NbtUtils.readBlockPos(t.getCompound("origin"));
        b.rotation = t.getInt("rot");
        b.progress = t.getLong("progress");
        b.workNeeded = t.getLong("need");
        b.complete = t.getBoolean("complete");
        b.placed = t.getInt("placed");
        b.free = t.getBoolean("free");
        b.sown = t.getBoolean("sown");
        b.sowDay = t.getLong("sowDay");
        b.sowYield = t.contains("sowYield") ? t.getFloat("sowYield") : 1.0F;
        // Sites from before the clearing stages were already levelled.
        b.stage = t.contains("stage") ? t.getInt("stage") : BUILD;
        b.demolishing = t.getBoolean("demolish");
        int[] sv = t.getIntArray("salvage");
        if (sv.length == 3) {
            b.salvageWood = sv[0];
            b.salvageStone = sv[1];
            b.demolishFrom = sv[2];
        }
        b.lastWork = t.getLong("lastWork");
        b.crew = t.contains("crew") ? t.getInt("crew") : -1;
        if (t.contains("w")) {
            b.w = t.getInt("w");
            b.d = t.getInt("d");
        } else if (b.type == BuildingType.SMALL_RESIDENTIAL_COURTYARD) {
            // Courtyards from before they were enlarged keep their 9 x 9 walls.
            b.w = 9;
            b.d = 9;
        }
        b.fenceRadius = t.getInt("fenceR");
        b.fenceYs = t.contains("fenceYs") ? t.getIntArray("fenceYs") : null;
        b.clearHeight = t.getInt("clearHeight");
        b.groundsCursor = t.getInt("groundsCursor");
        b.clearCols = t.contains("clearCols") ? t.getIntArray("clearCols") : null;
        b.fenceStyle = t.getInt("fenceStyle");
        if (t.contains("pathCols")) {
            b.pathPoints = t.getIntArray("pathPts");
            b.pathCols = t.getIntArray("pathCols");
            b.pathSurface = t.getInt("pathSurface");
            b.pathWidth = Math.max(1, t.getInt("pathWidth"));
        }
        b.fenceFill = t.contains("fenceFill") ? t.getIntArray("fenceFill") : null;
        b.gates = t.contains("gates") ? t.getIntArray("gates") : new int[]{0, 90, 180, 270};
        return b;
    }
}
