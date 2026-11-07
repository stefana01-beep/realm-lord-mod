package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Territory;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingDesigns;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Council;
import com.realmlord.village.Job;
import com.realmlord.village.Office;
import com.realmlord.village.Paths;
import com.realmlord.village.Planning;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Paths keep off buildings and walls; the officials plan the town and run the workers. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class PlanningTests {

    private static Resident official(Territory t, Office o) {
        for (Resident r : t.residents) {
            if (r.office >= 0 || r.isFigure() || r.militia) continue;
            r.politics = 99;
            r.intelligence = 99;
            r.command = 99;
            Office.appoint(t, o, r);
            return r;
        }
        return null;
    }

    private static void level(ServerLevel level, Territory t, int x0, int x1, int z0, int z1) {
        int gy = t.center.getY();
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) {
                BlockPos c = t.center.offset(x, 0, z);
                level.setBlock(c.atY(gy - 1), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = gy; y < gy + 12; y++) level.setBlock(c.atY(y), Blocks.AIR.defaultBlockState(), 2);
                for (int y = gy - 4; y < gy - 1; y++) level.setBlock(c.atY(y), Blocks.DIRT.defaultBlockState(), 2);
            }
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "planning")
    public static void pathsKeepOffBuildingsAndWalls(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 21200);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            level(level, t, -50, 50, -12, 12);
            Building house = new Building();
            house.id = t.nextBuildingId++;
            house.type = BuildingType.SMALL_RESIDENTIAL_COURTYARD;
            house.rotation = 1;
            house.origin = t.center.offset(20, 0, -3);
            house.complete = true;
            house.workNeeded = house.progress = 1;
            Building wall = new Building();
            wall.id = t.nextBuildingId++;
            wall.type = BuildingType.SIMPLE_FENCE;
            wall.origin = t.center;
            wall.fenceRadius = 40;
            wall.gates = new int[]{0, 90, 180, 270};
            wall.complete = true;
            wall.workNeeded = wall.progress = 1;
            // A path laid out before the house and the wall went up, straight through both.
            Building path = new Building();
            path.id = t.nextBuildingId++;
            path.type = BuildingType.PATH;
            path.origin = t.center;
            path.pathSurface = Paths.Surface.GRAVEL.ordinal();
            int[] cols = new int[4 * 3 * 50];
            int n = 0;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = 8; dx < 58; dx++) {
                    cols[n++] = t.center.getX() + dx;
                    cols[n++] = t.center.getY();
                    cols[n++] = t.center.getZ() + dz + 6;
                    cols[n++] = 0;
                }
            path.pathCols = cols;
            t.buildings.add(path);
            t.buildings.add(house);
            t.buildings.add(wall);
            List<BuildingDesigns.Placement> d = Paths.design(t, path);
            boolean inHouse = false, onWall = false;
            for (BuildingDesigns.Placement pl : d) {
                if (house.overlaps(pl.pos().getX(), pl.pos().getZ(), 1, 1, 0)) inHouse = true;
                double dist = Math.hypot(pl.pos().getX() + 0.5 - t.center.getX(), pl.pos().getZ() + 0.5 - t.center.getZ());
                if (dist >= 38 && dist <= 41) onWall = true;
            }
            helper.assertTrue(!inHouse, "the old path still cuts through the house");
            helper.assertTrue(!onWall, "the old path still cuts through the wall away from a gate");
            helper.assertTrue(!Paths.onWall(t, t.center.getX() + 40, t.center.getZ()), "the east gate is closed to paths");
            helper.assertTrue(Paths.onWall(t, t.center.getX() + 39, t.center.getZ() + 12), "the wall is open to paths");
            // The door's front is just outside the house; the lane from it goes round to the manor's yard.
            t.buildings.remove(path);
            int[] f = Planning.front(house);
            helper.assertTrue(!house.overlaps(f[0], f[1], 1, 1, 0) && house.overlaps(f[0] - 1, f[1], 1, 1, 0), "the front is not at the east door");
            Set<Long> targets = new HashSet<>();
            for (int k = -6; k <= 6; k++) targets.add(((long) (t.center.getX() + k) << 32) ^ ((t.center.getZ() - 6) & 0xffffffffL));
            int[] pts = Planning.route(t, f, targets, 60);
            helper.assertTrue(pts != null, "no lane was found");
            List<int[]> lane = Paths.columns(level, t, pts, 2);
            helper.assertTrue(lane != null && !lane.isEmpty(), "the lane has no columns");
            for (int[] c : lane) helper.assertTrue(!house.overlaps(c[0], c[2], 1, 1, 0), "the lane runs through the house");
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "planning")
    public static void officialsPlanTheTownAndCarryItOut(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 21600);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(official(t, Office.WORKS) != null, "no one to appoint");
            t.blueprints.add(BuildingType.SMALL_RESIDENTIAL_COURTYARD.id());
            t.wood = 5_000;
            t.stone = 5_000;
            while (Village.openBeds(t) > 2) Village.add(t, Resident.random(level.getRandom(), com.realmlord.village.Occupation.FARMER, 0));
            level(level, t, -30, 30, 14, 50);
            String err = Council.requestPlan(p, t);
            helper.assertTrue(err == null, "no plan was drafted: " + err);
            CompoundTag s = null;
            for (int i = 0; i < Council.list(t).size(); i++) if (Council.list(t).getCompound(i).getString("key").equals("plan")) s = Council.list(t).getCompound(i);
            helper.assertTrue(s != null, "the plan was not put to the Lord");
            err = Council.approve(p, t, s.getInt("id"));
            helper.assertTrue(err == null && Planning.active(t), "the plan was not adopted: " + err);
            ListTag steps = Planning.steps(t);
            boolean housing = false, lane = false;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.getCompound(i).getString("type").contains("RESIDENTIAL")) housing = true;
                if (steps.getCompound(i).getString("kind").equals("lane")) lane = true;
            }
            helper.assertTrue(housing && lane, "the plan has no housing and lane");
            int before = t.buildings.size();
            Planning.tick(level, t);
            helper.assertTrue(t.buildings.size() > before, "the officials started nothing: " + Planning.steps(t));
            // The building done, its lane comes next.
            for (int i = 0; i < Planning.steps(t).size(); i++) {
                CompoundTag st = Planning.steps(t).getCompound(i);
                if (st.getString("kind").equals("build") && st.getInt("st") == Planning.UNDERWAY) {
                    Building b = t.building(st.getInt("bid"));
                    b.complete = true;
                }
            }
            Planning.tick(level, t);
            Planning.tick(level, t);
            boolean laneMoved = false;
            ListTag now = Planning.steps(t);
            for (int i = 0; i < now.size(); i++) {
                CompoundTag st = now.getCompound(i);
                if (st.getString("kind").equals("lane") && st.contains("of") && st.getInt("st") != Planning.WAITING) laneMoved = true;
            }
            helper.assertTrue(laneMoved || !Planning.active(t), "the lane was never laid: " + now);
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "planning")
    public static void officialsRunTheirWorkers(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 22000);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(official(t, Office.WORKS) != null, "no one to appoint");
            Building site = VillageTests.built(t, BuildingType.BARRACKS);
            site.complete = false;
            int idle = 0;
            for (Resident r : t.residents) {
                if (idle >= 6 || r.office >= 0 || r.isFigure() || r.militia || Village.isStaffing(t, r)) continue;
                r.job = Job.IDLE;
                idle++;
            }
            int builders = Village.count(t, Job.CONSTRUCTION);
            helper.assertTrue(Council.manages(t), "the officials do not run the workers by default");
            Council.manage(level, t, level.getRandom());
            helper.assertTrue(Village.count(t, Job.CONSTRUCTION) > builders, "the head of Works sent no one to the site");
            // Taken over by the Lord, they leave the workers alone.
            Council.setManages(t, false);
            int now = Village.count(t, Job.CONSTRUCTION);
            Council.day(level, t, t.lastDay + 1, level.getRandom());
            helper.assertTrue(Village.count(t, Job.CONSTRUCTION) == now || !Council.list(t).isEmpty() || Council.auto(t),
                    "the officials moved workers after the Lord took over");
            VillageTests.done(helper, p, t);
        });
    }
}
