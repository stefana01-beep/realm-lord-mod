package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The town plan. In the novel Fan Zhongyan and the Construction Department do not simply raise
 * whatever the Lord points at: they draw up the town's plan (where the streets run, which
 * buildings go up first) and work through it. Here the head of Works (or the Steward, if there
 * is no head of Works) drafts a plan when the town needs one:
 *
 * <ol>
 * <li>main roads from the manor out through each gate of the wall, where none runs yet;</li>
 * <li>the buildings the next upgrade still needs, then housing if beds run short and farmland if
 * grain runs low, in that order;</li>
 * <li>after each building a lane from its door to the nearest road (or to the manor's yard), and
 * lanes for standing buildings no road reaches.</li>
 * </ol>
 *
 * <p>Once the Lord approves it, the officials carry the plan out step by step, whether or not the
 * Lord is there: one building at a time, each laid out on a good spot facing in towards the manor
 * (fields outside the wall), the roads and lanes as they come, waiting where wood or stone runs
 * short. Paths are routed around buildings and walls and through the gates.
 */
public final class Planning {
    private Planning() {}

    public static final int MAX_BUILDINGS = 6, MAX_LANES = 4;
    public static final int WAITING = 0, UNDERWAY = 1, DONE = 2, SKIPPED = 3;

    public static ListTag steps(Territory t) {
        return t.ext.getList("plan", Tag.TAG_COMPOUND);
    }

    public static boolean active(Territory t) {
        return !steps(t).isEmpty();
    }

    /** True if the planner may draw up a new plan (none running, and none drafted in five days). */
    static boolean due(ServerLevel level, Territory t) {
        return !active(t) && t.ext.getLong("planDay") + 5 <= GaiaCalendar.today(level.getServer());
    }

    // ------------------------------------------------------------------ drafting

    private static CompoundTag step(String kind) {
        CompoundTag s = new CompoundTag();
        s.putString("kind", kind);
        s.putInt("st", WAITING);
        s.putInt("bid", -1);
        return s;
    }

    /** The plan the planner would draw up now (perhaps empty). */
    public static ListTag draft(ServerLevel level, Territory t) {
        ListTag out = new ListTag();
        // Main roads out through the gates.
        Building wall = wall(t);
        if (wall != null && wall.complete) {
            for (int g : wall.gates) {
                if (reaches(t, wall, g)) continue;
                CompoundTag s = step("road");
                s.putInt("gate", g);
                out.add(s);
            }
        }
        // The buildings, in order of need.
        List<BuildingType> want = new ArrayList<>();
        for (BuildingType bt : BuildingType.values()) {
            if (bt.requiredFor != t.level.buildGrade() || bt.requiredFor == 0 || !bt.placed() || !possible(t, bt)) continue;
            int have = 0;
            for (Building b : t.buildings) if (b.type == bt && !b.demolishing) have++;
            for (int k = have; k < bt.requiredCount && want.size() < MAX_BUILDINGS; k++) want.add(bt);
        }
        if (want.size() < MAX_BUILDINGS && Village.openBeds(t) < Math.max(5, t.residents.size() / 20)) {
            BuildingType[] order = {BuildingType.RESIDENTIAL_QUARTER, BuildingType.LARGE_RESIDENTIAL_COURTYARD,
                    BuildingType.MEDIUM_RESIDENTIAL_COURTYARD, BuildingType.SMALL_RESIDENTIAL_COURTYARD};
            for (BuildingType h : order) if (possible(t, h)) {
                want.add(h);
                break;
            }
        }
        if (want.size() < MAX_BUILDINGS && t.food < Math.max(1, Village.foodUse(t)) * 20L && possible(t, BuildingType.FARMLAND)
                && !want.contains(BuildingType.FARMLAND)) want.add(BuildingType.FARMLAND);
        for (BuildingType bt : want) {
            CompoundTag s = step("build");
            s.putString("type", bt.name());
            out.add(s);
            if (bt.fieldJob() == null) {
                CompoundTag l = step("lane");
                l.putInt("of", out.size() - 1);
                out.add(l);
            }
        }
        // Lanes to standing buildings no road reaches.
        int lanes = 0;
        for (Building b : t.buildings) {
            if (lanes >= MAX_LANES) break;
            if (!b.complete || b.demolishing || !b.type.placed() || b.type.fieldJob() != null || b.type.waterside()) continue;
            int[] f = front(b);
            if (nearPath(t, f[0], f[1], 2) || manorYard(t, f[0], f[1], 2)) continue;
            CompoundTag l = step("lane");
            l.putInt("bid", -1);
            l.putInt("to", b.id);
            out.add(l);
            lanes++;
        }
        return out;
    }

    /** Grade, blueprint and specialist all allow it (wood and stone may come later). */
    private static boolean possible(Territory t, BuildingType bt) {
        return bt.grade <= t.level.buildGrade() && (!bt.blueprint || t.blueprints.contains(bt.id()))
                && (bt.specialist == null || Village.hasSpecialist(t, bt.specialist, bt.specialistRank));
    }

    static Building wall(Territory t) {
        Building best = null;
        for (Building b : t.buildings) {
            if (b.type.style != BuildingType.Style.FENCE || b.demolishing) continue;
            if (best == null || b.type == BuildingType.CITY_WALL) best = b;
        }
        return best;
    }

    private static int radius(Territory t, Building wall) {
        return wall.fenceRadius > 0 ? wall.fenceRadius : t.level.wall();
    }

    /** True if a road already runs through a gate. */
    private static boolean reaches(Territory t, Building wall, int gate) {
        double a = Math.toRadians(gate);
        int r = radius(t, wall);
        return nearPath(t, t.center.getX() + (int) Math.round(Math.cos(a) * r), t.center.getZ() + (int) Math.round(Math.sin(a) * r), 3);
    }

    private static boolean nearPath(Territory t, int x, int z, int within) {
        for (Building p : t.buildings) {
            if (p.type != BuildingType.PATH || p.pathCols == null || p.demolishing) continue;
            for (int i = 0; i + 3 < p.pathCols.length; i += 4) {
                if (Math.abs(p.pathCols[i] - x) <= within && Math.abs(p.pathCols[i + 2] - z) <= within) return true;
            }
        }
        return false;
    }

    private static boolean manorYard(Territory t, int x, int z, int within) {
        return Math.max(Math.abs(x - t.center.getX()), Math.abs(z - t.center.getZ())) <= 6 + within;
    }

    /** The block just outside a building's door (designs face their door out of the local z=0 side). */
    public static int[] front(Building b) {
        int w = b.width(), d = b.depth(), wx, wz;
        switch (b.rotation & 3) {
            case 1 -> {
                wx = d;
                wz = w / 2;
            }
            case 2 -> {
                wx = w - 1 - w / 2;
                wz = d;
            }
            case 3 -> {
                wx = -1;
                wz = w - 1 - w / 2;
            }
            default -> {
                wx = w / 2;
                wz = -1;
            }
        }
        return new int[]{b.origin.getX() + wx, b.origin.getZ() + wz};
    }

    // ------------------------------------------------------------------ adopting and carrying out

    public static String adopt(ServerLevel level, Territory t) {
        ListTag d = t.ext.getList("planDraft", Tag.TAG_COMPOUND);
        if (d.isEmpty()) return "realmlord.council.err.gone";
        t.ext.put("plan", d.copy());
        t.ext.remove("planDraft");
        return null;
    }

    public static void abandon(Territory t) {
        t.ext.remove("plan");
        t.ext.remove("planDraft");
    }

    /** Works the plan along: called every little while. */
    public static void tick(ServerLevel level, Territory t) {
        ListTag l = steps(t);
        if (l.isEmpty()) return;
        boolean buildingUnderway = false;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag s = l.getCompound(i);
            int st = s.getInt("st");
            if (st == DONE || st == SKIPPED) continue;
            String kind = s.getString("kind");
            if (st == UNDERWAY) {
                Building b = t.building(s.getInt("bid"));
                if (b == null || b.demolishing) s.putInt("st", SKIPPED);
                else if (b.complete) s.putInt("st", DONE);
                else if (kind.equals("build")) buildingUnderway = true;
                continue;
            }
            if (kind.equals("build")) {
                if (buildingUnderway) break;
                if (!startBuilding(level, t, s)) break;
                buildingUnderway = true;
            } else {
                // A lane waits for its building to be laid out; anything else waiting stops the plan here.
                if (!startPath(level, t, l, s)) {
                    if (s.getInt("st") == SKIPPED) continue;
                    break;
                }
            }
        }
        boolean finished = true;
        for (int i = 0; i < l.size(); i++) if (l.getCompound(i).getInt("st") < DONE) finished = false;
        if (finished) {
            t.ext.remove("plan");
            Village.tell(level, t, Component.translatable("realmlord.plan.finished", t.name));
            return;
        }
        t.ext.put("plan", l);
    }

    private static boolean startBuilding(ServerLevel level, Territory t, CompoundTag s) {
        BuildingType type;
        try {
            type = BuildingType.valueOf(s.getString("type"));
        } catch (IllegalArgumentException e) {
            s.putInt("st", SKIPPED);
            return true;
        }
        if (t.wood < type.wood || t.stone < type.stone) {
            s.putString("why", "realmlord.plan.why.resources");
            return false;
        }
        if (!possible(t, type)) {
            s.putInt("st", SKIPPED);
            s.putString("why", "realmlord.plan.why.cannot");
            return true;
        }
        int[] at = Council.site(level, t, type);
        if (at == null) {
            s.putString("why", "realmlord.plan.why.site");
            return false;
        }
        ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        String err = Village.start(level, lord, t, type, new BlockPos(at[0], t.center.getY(), at[1]), at[2], null, 0);
        if (err != null) {
            s.putString("why", err);
            return false;
        }
        s.putInt("bid", t.nextBuildingId - 1);
        s.putInt("st", UNDERWAY);
        s.remove("why");
        return true;
    }

    private static boolean startPath(ServerLevel level, Territory t, ListTag l, CompoundTag s) {
        int[] from, target = null;
        Set<Long> targets = new HashSet<>();
        int width;
        Paths.Surface[] surfaces;
        boolean road = s.getString("kind").equals("road");
        if (road) {
            Building wall = wall(t);
            if (wall == null) {
                s.putInt("st", SKIPPED);
                return false;
            }
            double a = Math.toRadians(s.getInt("gate"));
            int r = radius(t, wall) + (wall.type == BuildingType.CITY_WALL ? 8 : 4);
            from = new int[]{t.center.getX() + (int) Math.round(Math.cos(a) * 6.9), t.center.getZ() + (int) Math.round(Math.sin(a) * 6.9)};
            target = new int[]{t.center.getX() + (int) Math.round(Math.cos(a) * r), t.center.getZ() + (int) Math.round(Math.sin(a) * r)};
            targets.add(key(target[0], target[1]));
            width = 3;
            surfaces = new Paths.Surface[]{Paths.Surface.COBBLE, Paths.Surface.GRAVEL, Paths.Surface.DIRT};
        } else {
            int bid = s.contains("to") ? s.getInt("to") : -1;
            if (bid < 0) {
                CompoundTag of = l.getCompound(s.getInt("of"));
                if (of.getInt("st") == SKIPPED) {
                    s.putInt("st", SKIPPED);
                    return false;
                }
                if (of.getInt("st") == WAITING) return false;
                bid = of.getInt("bid");
            }
            Building b = t.building(bid);
            if (b == null || b.demolishing) {
                s.putInt("st", SKIPPED);
                return false;
            }
            from = front(b);
            if (nearPath(t, from[0], from[1], 1) || manorYard(t, from[0], from[1], 1)) {
                s.putInt("st", DONE);
                return true;
            }
            for (Building p : t.buildings) {
                if (p.type != BuildingType.PATH || p.pathCols == null || p.demolishing) continue;
                for (int i = 0; i + 3 < p.pathCols.length; i += 4) targets.add(key(p.pathCols[i], p.pathCols[i + 2]));
            }
            int cx = t.center.getX(), cz = t.center.getZ();
            for (int k = -6; k <= 6; k++) {
                targets.add(key(cx + k, cz - 6));
                targets.add(key(cx + k, cz + 6));
                targets.add(key(cx - 6, cz + k));
                targets.add(key(cx + 6, cz + k));
            }
            width = 2;
            surfaces = new Paths.Surface[]{Paths.Surface.GRAVEL, Paths.Surface.DIRT};
        }
        int reach = Math.max(Village.reach(t), target == null ? 0 : Math.max(Math.abs(target[0] - t.center.getX()), Math.abs(target[1] - t.center.getZ()))) + 6;
        int[] pts = route(t, from, targets, reach);
        if (pts == null) {
            s.putInt("st", SKIPPED);
            s.putString("why", "realmlord.plan.why.route");
            return false;
        }
        String err = null;
        for (Paths.Surface sf : surfaces) {
            // Keep stone for the buildings still to come: a paved way only out of plenty.
            if (sf.stone > 0 && t.stone < 300) continue;
            err = Paths.start(level, t, sf, width, pts, false, false);
            if (err == null) break;
        }
        if (err != null) {
            s.putString("why", err.equals("realmlord.path.err.cost") ? "realmlord.plan.why.resources" : err);
            if (!err.equals("realmlord.path.err.cost") && !err.equals("realmlord.path.err.unloaded")) s.putInt("st", SKIPPED);
            return false;
        }
        s.putInt("bid", t.nextBuildingId - 1);
        s.putInt("st", UNDERWAY);
        s.remove("why");
        return true;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static final int[] DX = {1, 0, -1, 0}, DZ = {0, 1, 0, -1};

    /**
     * The way from a block to the nearest target: around buildings and walls, through the gates,
     * keeping to straight runs (each turn costs as much as a few blocks). Returns its corners as
     * path points, or null if there is none (or it would need more corners than a path has).
     */
    public static int[] route(Territory t, int[] from, Set<Long> targets, int reach) {
        if (targets.isEmpty()) return null;
        int x0 = t.center.getX() - reach, z0 = t.center.getZ() - reach, n = 2 * reach + 1;
        if (from[0] < x0 || from[1] < z0 || from[0] >= x0 + n || from[1] >= z0 + n) return null;
        int cells = n * n;
        int[] dist = new int[cells * 4];
        int[] prev = new int[cells * 4];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        byte[] pass = new byte[cells];
        PriorityQueue<long[]> q = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        int start = (from[1] - z0) * n + (from[0] - x0);
        for (int d = 0; d < 4; d++) {
            dist[start * 4 + d] = 0;
            prev[start * 4 + d] = -1;
            q.add(new long[]{0, start * 4 + d});
        }
        int end = -1, expanded = 0;
        while (!q.isEmpty()) {
            long[] e = q.poll();
            int st = (int) e[1];
            if (e[0] > dist[st]) continue;
            int cell = st / 4, dir = st % 4;
            int cx = x0 + cell % n, cz = z0 + cell / n;
            if (targets.contains(key(cx, cz))) {
                end = st;
                break;
            }
            if (++expanded > 400_000) break;
            for (int nd = 0; nd < 4; nd++) {
                if (nd == (dir + 2) % 4 && cell != start) continue;
                int nx = cx + DX[nd], nz = cz + DZ[nd];
                if (nx < x0 || nz < z0 || nx >= x0 + n || nz >= z0 + n) continue;
                int nc = (nz - z0) * n + (nx - x0);
                if (pass[nc] == 0) pass[nc] = (byte) (targets.contains(key(nx, nz)) || Paths.allowed(t, nx, nz) ? 1 : 2);
                if (pass[nc] == 2) continue;
                int cost = (int) e[0] + 10 + (nd != dir && cell != start ? 30 : 0);
                int ns = nc * 4 + nd;
                if (cost < dist[ns]) {
                    dist[ns] = cost;
                    prev[ns] = st;
                    q.add(new long[]{cost, ns});
                }
            }
        }
        if (end < 0) return null;
        List<int[]> cellsOut = new ArrayList<>();
        for (int st = end; st >= 0; st = prev[st]) {
            int cell = st / 4;
            cellsOut.add(0, new int[]{x0 + cell % n, z0 + cell / n});
        }
        if (cellsOut.size() < 2) return null;
        List<int[]> corners = new ArrayList<>();
        corners.add(cellsOut.get(0));
        for (int i = 1; i + 1 < cellsOut.size(); i++) {
            int[] a = cellsOut.get(i - 1), b = cellsOut.get(i), c = cellsOut.get(i + 1);
            if (b[0] - a[0] != c[0] - b[0] || b[1] - a[1] != c[1] - b[1]) corners.add(b);
        }
        corners.add(cellsOut.get(cellsOut.size() - 1));
        if (corners.size() > Paths.MAX_POINTS) return null;
        int[] pts = new int[corners.size() * 2];
        for (int i = 0; i < corners.size(); i++) {
            pts[i * 2] = corners.get(i)[0];
            pts[i * 2 + 1] = corners.get(i)[1];
        }
        return pts;
    }

    // ------------------------------------------------------------------ telling

    private static final String[] BEARINGS = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};

    /** A step as the plan lists it. */
    public static Component text(Territory t, ListTag l, CompoundTag s) {
        switch (s.getString("kind")) {
            case "build" -> {
                BuildingType bt;
                try {
                    bt = BuildingType.valueOf(s.getString("type"));
                } catch (IllegalArgumentException e) {
                    return Component.literal("?");
                }
                return Component.translatable("realmlord.plan.step.build", Component.translatable(bt.key()));
            }
            case "road" -> {
                int b = Math.floorMod(Math.round(s.getInt("gate") / 45f), 8);
                return Component.translatable("realmlord.plan.step.road", Component.translatable("realmlord.plan.dir." + BEARINGS[b]));
            }
            default -> {
                Building b = s.contains("to") ? t.building(s.getInt("to")) : null;
                if (b == null && s.contains("of")) {
                    CompoundTag of = l.getCompound(s.getInt("of"));
                    try {
                        return Component.translatable("realmlord.plan.step.lane", Component.translatable(BuildingType.valueOf(of.getString("type")).key()));
                    } catch (IllegalArgumentException e) {
                        return Component.literal("?");
                    }
                }
                return Component.translatable("realmlord.plan.step.lane", b == null ? Component.literal("?") : Component.translatable(b.type.key()));
            }
        }
    }

    /** For the Council tab. */
    public static ListTag view(Territory t, ListTag l) {
        ListTag out = new ListTag();
        for (int i = 0; i < l.size(); i++) {
            CompoundTag s = l.getCompound(i);
            CompoundTag o = new CompoundTag();
            o.putString("text", Component.Serializer.toJson(text(t, l, s)));
            o.putInt("st", s.getInt("st"));
            if (s.contains("why")) o.putString("why", s.getString("why"));
            out.add(o);
        }
        return out;
    }

    /** "Build the Granary, then 5 more steps". */
    public static Component summary(Territory t, ListTag l) {
        return l.isEmpty() ? Component.empty() : text(t, l, l.getCompound(0));
    }
}
