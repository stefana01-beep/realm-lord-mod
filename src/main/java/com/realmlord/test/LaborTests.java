package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.SpawnPicker;
import com.realmlord.territory.Territory;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Haul;
import com.realmlord.village.Job;
import com.realmlord.village.Resident;
import com.realmlord.village.Residents;
import com.realmlord.village.Sites;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** In-game tests for working residents, their packs and hauls, and choosing where to begin. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class LaborTests {

    @GameTest(template = "empty", timeoutTicks = 200, batch = "labor_haul")
    public static void haulsCountTowardsTheDay(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 4400);
        ServerLevel level = helper.getLevel();
        Resident r = t.residents.get(0);
        // A pack holds a haul until it is handed in.
        r.inv.addItem(new ItemStack(Items.OAK_LOG, 3));
        r.inv.addItem(new ItemStack(Items.COBBLESTONE, 5));
        r.inv.addItem(new ItemStack(Items.BREAD, 2));
        long wood = t.wood, stone = t.stone;
        Haul.deposit(t, r);
        helper.assertTrue(t.wood - wood == 6 && t.stone - stone == 5, "haul not handed in: " + (t.wood - wood) + " wood, " + (t.stone - stone) + " stone");
        helper.assertTrue(r.inv.countItem(Items.BREAD) == 2 && r.inv.countItem(Items.OAK_LOG) == 0, "the pack should keep only what the stores do not take");
        helper.assertTrue(r.gathered[Haul.WOOD] == 6, "haul not counted towards the day");
        // Packs survive saving.
        Resident copy = Resident.load(r.save());
        helper.assertTrue(copy.inv.countItem(Items.BREAD) == 2, "pack lost on saving");
        // Wood carried in by hand counts towards a logger's day: no more comes in for them.
        VillageTests.built(t, BuildingType.LOGGING_FIELD);
        r.entity = null;
        r.militia = false;
        helper.assertTrue(Village.assign(t, r, Job.LOGGING, -1), "could not make a logger");
        for (Resident o : t.residents) if (o != r && o.job == Job.LOGGING) o.job = Job.IDLE;
        double quota = Village.quota(t, r)[0];
        r.gathered[Haul.WOOD] = (long) Math.ceil(quota) + 5;
        long day = GaiaCalendar.day(level.getServer().overworld().getGameTime());
        wood = t.wood;
        Village.day(level, t, day);
        helper.assertTrue(t.wood == wood, "a logger who already brought in the day's wood was paid again: +" + (t.wood - wood));
        helper.assertTrue(r.gathered[Haul.WOOD] == 0, "the day's count should start again");
        wood = t.wood;
        Village.day(level, t, day + 1);
        helper.assertTrue(t.wood - wood == Math.round(quota), "a logger who carried nothing should still bring the day's quota: +" + (t.wood - wood));
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 1200, batch = "labor_walk")
    public static void residentsGoOutToWork(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 4800);
        ServerLevel level = helper.getLevel();
        day(level, 1000);
        // Firm, dry ground from the manor to the site, whatever the test world's terrain.
        int gy = t.center.getY() - 1;
        for (int x = -8; x <= 34; x++)
            for (int z = -10; z <= 10; z++) {
                level.setBlock(new BlockPos(t.center.getX() + x, gy, t.center.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 12; y++) level.setBlock(new BlockPos(t.center.getX() + x, gy + y, t.center.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        // A site 24 blocks out, where every builder should go.
        Building site = new Building();
        site.id = t.nextBuildingId++;
        site.type = BuildingType.BATHROOM;
        site.origin = t.center.offset(24, 0, -3);
        site.stage = Building.BUILD;
        site.workNeeded = Village.workNeeded(site.type);
        site.lastWork = level.getGameTime();
        t.buildings.add(site);
        Village.shift(t, Job.CONSTRUCTION, 3);
        for (int i = 0; i < 6; i++) Residents.sync(level, t);
        SettlerEntity builder = null, idle = null;
        for (Resident r : t.residents) {
            if (r.entity == null) continue;
            var e = level.getEntity(r.entity);
            if (!(e instanceof SettlerEntity s)) continue;
            s.routine(t, r);
            if (r.job == Job.CONSTRUCTION && builder == null) builder = s;
            if (r.job == Job.IDLE && idle == null) idle = s;
        }
        helper.assertTrue(builder != null && idle != null, "residents were not shown");
        helper.assertTrue(builder.getActivity() == SettlerEntity.Activity.BUILD, "builder not building: " + builder.getActivity());
        helper.assertTrue(builder.getRestrictCenter().equals(site.center()), "builder not sent to the site: " + builder.getRestrictCenter());
        helper.assertTrue(idle.getActivity() == SettlerEntity.Activity.IDLE, "idle resident doing " + idle.getActivity());
        helper.assertTrue(!idle.getRestrictCenter().equals(t.center), "idle residents should spread out, not crowd the manor");
        helper.assertTrue(!idle.resident().activity.isEmpty(), "card should say what they are doing");
        // By night they go home.
        day(level, 14000);
        idle.routine(t, idle.resident());
        helper.assertTrue(idle.getActivity() == SettlerEntity.Activity.HOME, "not home at night: " + idle.getActivity());
        helper.assertTrue(idle.getRestrictCenter().distSqr(t.center) <= 8 * 8, "home should be by the manor for the first ten");
        day(level, 1000);
        // The builder actually walks to the site and lays blocks there.
        final SettlerEntity b = builder;
        helper.succeedWhen(() -> {
            for (Resident r : t.residents) if (r.entity != null && level.getEntity(r.entity) instanceof SettlerEntity s) s.routine(t, r);
            double dx = b.getX() - (site.center().getX() + 0.5), dz = b.getZ() - (site.center().getZ() + 0.5);
            helper.assertTrue(dx * dx + dz * dz < 14 * 14, "builder still " + Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocks from the site");
            helper.assertTrue(site.placed > 0 || site.stage < Building.BUILD || Sites.next(level, t, site) == null, "builder at the site but nothing placed");
            com.realmlord.territory.TerritoryData.get(level.getServer()).remove(t.id);
            RealmTests.leave(p);
        });
    }

    private static void day(ServerLevel level, long time) {
        level.setDayTime(time);
        level.updateSkyBrightness();
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "labor_spawn")
    public static void playersChooseWhereToBegin(GameTestHelper helper) {
        ServerPlayer p = RealmTests.mockPlayer(helper);
        ServerLevel level = p.server.overworld();
        var options = SpawnPicker.options(level);
        helper.assertTrue(!options.isEmpty(), "no starting places offered");
        helper.assertTrue(!Profiles.get(p).isPlaced(), "a new player has not chosen yet");
        // The farthest place offered, whose ground has not been generated yet.
        var spawn = level.getSharedSpawnPos();
        SpawnPicker.Option o = options.stream().max(java.util.Comparator.comparingDouble(
                (SpawnPicker.Option k) -> Math.hypot(k.x() - spawn.getX(), k.z() - spawn.getZ()))).orElseThrow();
        helper.assertTrue(com.realmlord.worldgen.ChinaMap.sample(com.realmlord.worldgen.ChinaMap.CONT, -1091, 5077) > -0.05,
                "Shanhai in the Lianzhou Basin should be on land");
        String err = SpawnPicker.choose(p, o.x(), o.z(), false);
        helper.assertTrue(err == null, "could not begin at " + o.key() + ": " + err);
        helper.assertTrue(Profiles.get(p).isPlaced(), "choice not remembered");
        double dx = p.getX() - o.x(), dz = p.getZ() - o.z();
        helper.assertTrue(dx * dx + dz * dz < 70 * 70, "not taken there: " + p.blockPosition());
        var below = level.getBlockState(p.blockPosition().below());
        helper.assertTrue(below.getFluidState().isEmpty(), "arrived in water");
        helper.assertTrue("realmlord.spawn.err.placed".equals(SpawnPicker.choose(p, o.x(), o.z(), false)), "could choose twice");
        helper.assertTrue(p.getRespawnPosition() != null, "respawn point not set");
        RealmTests.leave(p);
        helper.succeed();
    }
}
