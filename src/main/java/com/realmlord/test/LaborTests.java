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
        helper.assertTrue(com.realmlord.worldgen.ChinaMap.sample(com.realmlord.worldgen.ChinaMap.CONT,
                com.realmlord.worldgen.ChinaMap.blockOf(109.1, 21.85)[0], com.realmlord.worldgen.ChinaMap.blockOf(109.1, 21.85)[1]) > -0.05,
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

    @GameTest(template = "empty", timeoutTicks = 300, batch = "labor_crews")
    public static void crewsMarshAndTheBanditCamp(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 5200);
        ServerLevel level = helper.getLevel();
        // Two sites share the builders; a crew set by hand takes its number first.
        Building a = site(t, BuildingType.BATHROOM, 300), b = site(t, BuildingType.DINING_HALL, 340);
        helper.assertTrue(Village.shift(t, Job.CONSTRUCTION, 6) == 6, "could not take on 6 builders");
        Village.allocateBuilders(t);
        helper.assertTrue(Village.buildersAt(t, a) == 3 && Village.buildersAt(t, b) == 3, "builders not shared: " + Village.buildersAt(t, a) + "/" + Village.buildersAt(t, b));
        a.crew = 5;
        Village.allocateBuilders(t);
        helper.assertTrue(Village.buildersAt(t, a) == 5 && Village.buildersAt(t, b) == 1, "crew not honoured: " + Village.buildersAt(t, a) + "/" + Village.buildersAt(t, b));
        Village.construct(level, t, 20);
        helper.assertTrue(a.progress > 0 && b.progress > 0, "both sites should rise at once");
        helper.assertTrue(a.progress * 2 >= b.progress * 5, "five builders should work far faster than one: " + a.progress + " vs " + b.progress);
        // Fields take any number of workers.
        VillageTests.built(t, BuildingType.LOGGING_FIELD);
        while (t.residents.size() < 40) Village.add(t, Resident.random(level.getRandom(), com.realmlord.village.Occupation.FARMER, 0));
        helper.assertTrue(Village.shift(t, Job.LOGGING, 25) == 25, "a logging field should take 25 loggers");

        // Building on marsh: shallow water is filled in, deep water is not.
        int gy = t.center.getY() - 1;
        BlockPos pad = t.center.offset(16, 0, 16);
        for (int x = -2; x < 22; x++)
            for (int z = -2; z < 22; z++) {
                for (int y = -6; y <= 0; y++) level.setBlock(new BlockPos(pad.getX() + x, gy + y, pad.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 20; y++) level.setBlock(new BlockPos(pad.getX() + x, gy + y, pad.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int x = 2; x < 6; x++)
            for (int z = 2; z < 6; z++)
                for (int y = -1; y <= 0; y++) level.setBlock(new BlockPos(pad.getX() + x, gy + y, pad.getZ() + z), Blocks.WATER.defaultBlockState(), 2);
        BlockPos at = new BlockPos(pad.getX(), gy + 1, pad.getZ());
        String err = Village.checkPlacement(level, t, BuildingType.BATHROOM, at, 0);
        helper.assertTrue(err == null, "a shallow pond should be buildable: " + err);
        for (int x = 2; x < 6; x++)
            for (int z = 2; z < 6; z++)
                for (int y = -5; y <= 0; y++) level.setBlock(new BlockPos(pad.getX() + x, gy + y, pad.getZ() + z), Blocks.WATER.defaultBlockState(), 2);
        helper.assertTrue("realmlord.build.err.water".equals(Village.checkPlacement(level, t, BuildingType.BATHROOM, at, 0)), "deep water should not be buildable");

        // The bandit camp: salvaging it returns its materials and leaves nothing behind.
        com.realmlord.quest.VillageQuest q = Profiles.get(p).quest();
        BlockPos camp = t.center.offset(-60, 0, -60);
        for (int x = -14; x <= 14; x++)
            for (int z = -14; z <= 14; z++) {
                level.setBlock(new BlockPos(camp.getX() + x, gy, camp.getZ() + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 12; y++) level.setBlock(new BlockPos(camp.getX() + x, gy + y, camp.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        q.camp = new BlockPos(camp.getX(), gy + 1, camp.getZ());
        com.realmlord.quest.BanditCamp.build(level, p, q);
        q.stage = com.realmlord.quest.VillageQuest.Stage.CLEARED;
        int before = p.getInventory().countItem(Items.BROWN_WOOL);
        helper.assertTrue(com.realmlord.quest.QuestLogic.decideCamp(p, true), "could not salvage the camp");
        helper.assertTrue(p.getInventory().countItem(Items.BROWN_WOOL) - before >= 27, "tent cloth not recovered");
        helper.assertTrue(p.getInventory().countItem(Items.CAMPFIRE) >= 1 && p.getInventory().countItem(Items.IRON_BARS) >= 8, "campfire or pen bars not recovered");
        int left = 0;
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++)
                for (int y = 0; y <= 4; y++) {
                    var s2 = level.getBlockState(q.camp.offset(x, y, z));
                    if (s2.is(Blocks.SPRUCE_FENCE) || s2.is(Blocks.BROWN_WOOL) || s2.is(Blocks.IRON_BARS) || s2.is(Blocks.CAMPFIRE)) left++;
                }
        helper.assertTrue(left == 0, "camp blocks left standing: " + left);
        helper.assertTrue(!com.realmlord.quest.QuestLogic.decideCamp(p, false), "could decide twice");
        // Tidy up the band.
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(q.camp).inflate(40),
                e -> e.getTags().contains(com.realmlord.quest.BanditCamp.BANDIT_TAG) || e.getTags().contains(com.realmlord.quest.BanditCamp.CAPTIVE_TAG))) e.discard();
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "labor_market")
    public static void bedsMarketStorehouseAndTrees(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 5600);
        ServerLevel level = helper.getLevel();
        // A residential courtyard sleeps twenty on straw beds.
        Building yard = new Building();
        yard.id = t.nextBuildingId++;
        yard.type = BuildingType.SMALL_RESIDENTIAL_COURTYARD;
        yard.origin = t.center.offset(40, 0, 40);
        var design = com.realmlord.village.BuildingDesigns.design(level, t, yard);
        long beds = design.stream().filter(pl -> pl.state().getBlock() == Blocks.YELLOW_BED
                && pl.state().getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD).count();
        helper.assertTrue(beds >= 20, "the courtyard should hold 20 beds, has " + beds);
        // The manor hall has ten.
        int gy = t.center.getY() - 1;
        for (int x = -9; x <= 9; x++)
            for (int z = -9; z <= 9; z++) {
                level.setBlock(new BlockPos(t.center.getX() + x, gy, t.center.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 14; y++) level.setBlock(new BlockPos(t.center.getX() + x, gy + y, t.center.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        com.realmlord.territory.Founding.buildManor(level, t.center, net.minecraft.core.Direction.NORTH);
        int manorBeds = Residents.beds(level, t).size();
        helper.assertTrue(manorBeds >= 10, "the manor should sleep ten, has " + manorBeds + " beds");
        helper.assertTrue(Residents.bedOf(level, t, t.residents.get(0)) != null, "the first resident should have a bed");

        // Workshops make goods: the Mill grinds grain, the Mulberry Shed yields silk.
        VillageTests.built(t, BuildingType.MILL);
        VillageTests.built(t, BuildingType.MULBERRY_SHED);
        t.food = 1000;
        com.realmlord.village.Goods.produce(t);
        com.realmlord.village.Goods.produce(t);
        helper.assertTrue(com.realmlord.village.Goods.FLOUR.stock(t) == 200 && t.food == 800, "mill: flour " + com.realmlord.village.Goods.FLOUR.stock(t) + ", grain " + t.food);
        helper.assertTrue(com.realmlord.village.Goods.FINE_SILK.stock(t) == 1, "two days of the shed should give a roll of silk");
        // The market buys and sells them at the novel's prices, less 20% on sales.
        com.realmlord.village.Coins.give(p, 10_000);
        long before = com.realmlord.village.Coins.count(p);
        helper.assertTrue(Village.sellGood(p, t, com.realmlord.village.Goods.FINE_SILK, 1), "could not sell silk");
        helper.assertTrue(com.realmlord.village.Coins.count(p) - before == 4000, "a roll of fine silk sells for 50 silver less tax");
        helper.assertTrue(Village.buyGood(p, t, com.realmlord.village.Goods.SALT, 100), "could not buy salt");
        helper.assertTrue(com.realmlord.village.Goods.SALT.stock(t) == 100, "salt not stored");
        // Handing in from the pack, and the storehouse keeps the rest.
        p.getInventory().add(new ItemStack(Items.OAK_LOG, 10));
        p.getInventory().add(new ItemStack(Items.BEEF, 5));
        long wood = t.wood;
        com.realmlord.village.Goods.handIn(t, p.getInventory());
        helper.assertTrue(t.wood - wood == 20 && com.realmlord.village.Goods.MEAT.stock(t) == 5, "hand-in: wood +" + (t.wood - wood));
        t.storage.setItem(0, new ItemStack(Items.SADDLE));
        Territory copy = Territory.load(t.save());
        helper.assertTrue(copy.storage.getItem(0).is(Items.SADDLE) && copy.goods[com.realmlord.village.Goods.SALT.ordinal()] == 100, "storehouse or goods lost on saving");
        // Graded gear sells on the Special Items platform.
        p.getInventory().add(com.realmlord.loot.Gear.make(Items.IRON_SWORD, com.realmlord.territory.TokenGrade.BRONZE, level.getRandom()));
        helper.assertTrue(Village.sellSpecial(p) == 1600, "a Bronze-grade weapon sells for 20 silver less tax");

        // Mangroves are trees, not a cliff: the ground under them is what counts.
        BlockPos pad = t.center.offset(30, 0, -30);
        for (int x = -2; x < 22; x++)
            for (int z = -2; z < 22; z++) {
                level.setBlock(new BlockPos(pad.getX() + x, gy, pad.getZ() + z), Blocks.MUD.defaultBlockState(), 2);
                for (int y = 1; y < 24; y++) level.setBlock(new BlockPos(pad.getX() + x, gy + y, pad.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int[] tr : new int[][]{{2, 2}, {4, 2}, {2, 4}}) {
            for (int y = 1; y <= 3; y++) level.setBlock(new BlockPos(pad.getX() + tr[0] + 1, gy + y, pad.getZ() + tr[1]), Blocks.MANGROVE_ROOTS.defaultBlockState(), 2);
            for (int y = 1; y <= 14; y++) level.setBlock(new BlockPos(pad.getX() + tr[0], gy + y, pad.getZ() + tr[1]), Blocks.MANGROVE_LOG.defaultBlockState(), 2);
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    level.setBlock(new BlockPos(pad.getX() + tr[0] + dx, gy + 15, pad.getZ() + tr[1] + dz), Blocks.MANGROVE_LEAVES.defaultBlockState(), 2);
        }
        String err = Village.checkPlacement(level, t, BuildingType.BATHROOM, new BlockPos(pad.getX(), gy + 1, pad.getZ()), 0);
        helper.assertTrue(err == null, "ground under mangroves should be buildable: " + err);
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "labor_paths")
    public static void pathsSpoilsAndNewcomers(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 6000);
        ServerLevel level = helper.getLevel();
        int gy = t.center.getY() - 1, cx = t.center.getX(), cz = t.center.getZ();
        for (int x = 6; x <= 32; x++)
            for (int z = -4; z <= 4; z++) {
                for (int y = -3; y <= 0; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.STONE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(cx + x, gy, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 8; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
            }
        // A ditch the path must cross.
        level.setBlock(new BlockPos(cx + 20, gy, cz), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(new BlockPos(cx + 20, gy - 1, cz), Blocks.AIR.defaultBlockState(), 2);

        // Paths: priced by surface and width, laid by the builders.
        t.wood = 0;
        t.stone = 1000;
        helper.assertTrue("realmlord.path.err.cost".equals(com.realmlord.village.Paths.start(p, t, com.realmlord.village.Paths.Surface.PLANKS, 2, new int[]{cx + 8, cz, cx + 28, cz})),
                "a plank path should need wood");
        String err = com.realmlord.village.Paths.start(p, t, com.realmlord.village.Paths.Surface.COBBLE, 2, new int[]{cx + 8, cz, cx + 28, cz});
        helper.assertTrue(err == null, "could not lay a path: " + err);
        Building path = t.buildings.get(t.buildings.size() - 1);
        int blocks = com.realmlord.village.Paths.blocks(path);
        helper.assertTrue(blocks >= 40 && blocks <= 46, "a 2-wide path 21 blocks long should cover about 42 blocks, covers " + blocks);
        helper.assertTrue(1000 - t.stone == blocks, "cobblestone costs a stone a block: paid " + (1000 - t.stone));
        path.progress = path.workNeeded;
        for (int i = 0; i < 60 && !path.verified; i++) Sites.work(level, t, path, 2000);
        helper.assertTrue(level.getBlockState(new BlockPos(cx + 12, gy, cz)).is(Blocks.COBBLESTONE), "path not laid");
        helper.assertTrue(level.getBlockState(new BlockPos(cx + 20, gy, cz)).is(Blocks.COBBLESTONE)
                && !level.getBlockState(new BlockPos(cx + 20, gy - 1, cz)).isAir(), "the ditch should be filled beneath the path: "
                + level.getBlockState(new BlockPos(cx + 20, gy, cz)) + " over " + level.getBlockState(new BlockPos(cx + 20, gy - 1, cz))
                + ", column " + java.util.Arrays.toString(column(path, cx + 20, cz)) + ", placed " + path.placed + "/" + com.realmlord.village.Paths.blocks(path) * 3 + ", stage " + path.stage);
        Territory copy = Territory.load(t.save());
        helper.assertTrue(copy.building(path.id) != null && com.realmlord.village.Paths.blocks(copy.building(path.id)) == blocks, "path lost on saving");
        Village.demolish(level, t, path);
        helper.assertTrue(t.building(path.id) == null && level.getBlockState(new BlockPos(cx + 12, gy, cz)).is(Blocks.GRASS_BLOCK), "a demolished path should go back to grass");

        // Freed settlers wait for beds, and move in when there are some.
        helper.assertTrue(Village.openBeds(t) == 0, "the manor's ten beds should be full");
        t.waitingSettlers = 3;
        helper.assertTrue(Village.admitWaiting(level, t) == 0 && t.residents.size() == 10, "settlers moved in with no beds");
        VillageTests.built(t, BuildingType.SMALL_RESIDENTIAL_COURTYARD);
        helper.assertTrue(Village.admitWaiting(level, t) == 3 && t.waitingSettlers == 0 && t.residents.size() == 13, "waiting settlers did not move in");
        // Wanderers settle where there is a bed, and pass on where there is none.
        BlockPos near = new BlockPos(cx + 12, gy + 1, cz + 2);
        com.realmlord.village.Wanderers.spawn(level, near, 1);
        SettlerEntity w = level.getEntitiesOfClass(SettlerEntity.class, new net.minecraft.world.phys.AABB(near).inflate(3),
                e -> e.getTags().contains(com.realmlord.village.Wanderers.TAG)).get(0);
        com.realmlord.village.Wanderers.check(level, w);
        helper.assertTrue(w.isRemoved() && t.residents.size() == 14, "a wanderer did not settle in a village with beds");
        while (Village.openBeds(t) > 0) Village.add(t, Resident.random(level.getRandom(), com.realmlord.village.Occupation.FARMER, 0));
        com.realmlord.village.Wanderers.spawn(level, near, 1);
        SettlerEntity w2 = level.getEntitiesOfClass(SettlerEntity.class, new net.minecraft.world.phys.AABB(near).inflate(3),
                e -> e.getTags().contains(com.realmlord.village.Wanderers.TAG)).get(0);
        int full = t.residents.size();
        com.realmlord.village.Wanderers.check(level, w2);
        helper.assertTrue(!w2.isRemoved() && t.residents.size() == full, "a wanderer settled without a bed");
        w2.getPersistentData().putLong("realmlord_born", level.getGameTime() - com.realmlord.village.Wanderers.LIFETIME - 10);
        com.realmlord.village.Wanderers.check(level, w2);
        helper.assertTrue(w2.isRemoved(), "a wanderer should move on after two days");

        // The bandits' spoils: blueprints and gold now, supplies for the stores later.
        com.realmlord.quest.VillageQuest q = Profiles.get(p).quest();
        long coins = com.realmlord.village.Coins.count(p);
        com.realmlord.quest.QuestLogic.spoils(p, q);
        helper.assertTrue(p.getInventory().countItem(com.realmlord.registry.ModItems.BLUEPRINT.get()) >= 2, "no blueprints in the spoils");
        helper.assertTrue(com.realmlord.village.Coins.count(p) - coins == 50_000, "the chief's five gold coins are missing");
        long food = t.food;
        helper.assertTrue(com.realmlord.quest.QuestLogic.deliverSpoils(q, t) && t.food - food == 400 && !q.hasSpoils(), "spoils not delivered to the stores");

        // What clearing turns up goes straight to the stores.
        long sand0 = com.realmlord.village.Goods.SAND.stock(t), clay0 = com.realmlord.village.Goods.CLAY.stock(t), earth0 = com.realmlord.village.Goods.EARTH.stock(t);
        helper.assertTrue(earth0 > 0, "the turf dug up for the path should have gone to the stores as earth");
        Sites.toStores(t, new ItemStack(Items.SAND, 3));
        Sites.toStores(t, new ItemStack(Items.CLAY_BALL, 4));
        Sites.toStores(t, new ItemStack(Items.DIRT, 5));
        Sites.toStores(t, new ItemStack(Items.OAK_SAPLING, 2));
        helper.assertTrue(com.realmlord.village.Goods.SAND.stock(t) - sand0 == 3 && com.realmlord.village.Goods.CLAY.stock(t) - clay0 == 4
                && com.realmlord.village.Goods.EARTH.stock(t) - earth0 == 5, "sand, clay or earth not kept");
        helper.assertTrue(t.storage.countItem(Items.OAK_SAPLING) == 2, "saplings not kept in the storehouse");
        // Village names are tidied.
        helper.assertTrue("Red Cliff".equals(com.realmlord.network.FoundVillagePacket.clean("  Red Cliff ")) && com.realmlord.network.FoundVillagePacket.clean("   ") == null,
                "village name not cleaned");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "labor_clearing")
    public static void clearingAndTheLordsResidence(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 6400);
        ServerLevel level = helper.getLevel();
        int gy = t.center.getY() - 1, cx = t.center.getX(), cz = t.center.getZ();
        // Flat stone with a rocky hump and a hole in it.
        for (int x = -10; x <= 40; x++)
            for (int z = -10; z <= 10; z++) {
                for (int y = -4; y <= 0; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 16; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int x = 24; x <= 27; x++)
            for (int z = -2; z <= 2; z++)
                for (int y = 1; y <= 3; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.STONE.defaultBlockState(), 2);
        for (int x = 31; x <= 32; x++)
            for (int y = -1; y <= 0; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz), Blocks.AIR.defaultBlockState(), 2);

        helper.assertTrue("realmlord.clear.err.nothing".equals(Village.startClearing(p, t, cx - 3, cz - 3, cx + 3, cz + 3, 0)), "the manor yard should not be cleared");
        // A finished building inside the area is left alone; a rock just outside it is cut back in a slope.
        Building hut = new Building();
        hut.id = t.nextBuildingId++;
        hut.type = BuildingType.BATHROOM;
        hut.origin = new BlockPos(cx + 33, gy + 1, cz - 6);
        hut.complete = true;
        hut.workNeeded = hut.progress = 1;
        t.buildings.add(hut);
        level.setBlock(new BlockPos(cx + 34, gy + 1, cz - 5), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        for (int y = 1; y <= 6; y++) level.setBlock(new BlockPos(cx + 37, gy + y, cz), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue("realmlord.clear.err.big".equals(Village.startClearing(p, t, cx + 10, cz - 30, cx + 10 + Village.MAX_CLEARING, cz + 30, 0)), "too large an area was allowed");
        String err = Village.startClearing(p, t, cx + 20, cz - 6, cx + 35, cz + 6, 0);
        helper.assertTrue(err == null, "could not order a clearing: " + err);
        Building c = t.buildings.get(t.buildings.size() - 1);
        helper.assertTrue(c.type == BuildingType.CLEARING && c.origin.getY() == gy + 1 && c.sizeX() == 16 && c.sizeZ() == 13,
                "clearing laid out wrongly at y " + c.origin.getY() + ", " + c.sizeX() + " x " + c.sizeZ());
        helper.assertTrue(Village.checkPlacement(level, t, BuildingType.BATHROOM, new BlockPos(cx + 22, gy + 1, cz - 2), 0) != null, "a building was allowed on ground being cleared");
        long stone = t.stone;
        c.progress = c.workNeeded;
        for (int i = 0; i < 20 && c.stage != Building.BUILD; i++) Sites.work(level, t, c, 2000);
        helper.assertTrue(level.getBlockState(new BlockPos(cx + 25, gy + 1, cz)).isAir() && level.getBlockState(new BlockPos(cx + 26, gy + 3, cz + 1)).isAir(), "the hump was not cut away");
        helper.assertTrue(!level.getBlockState(new BlockPos(cx + 31, gy, cz)).isAir() && !level.getBlockState(new BlockPos(cx + 32, gy - 1, cz)).isAir(), "the hole was not filled");
        helper.assertTrue(t.stone - stone >= 40, "the rock cut away should go to the stores: +" + (t.stone - stone));
        helper.assertTrue(level.getBlockState(new BlockPos(cx + 34, gy + 1, cz - 5)).is(Blocks.GOLD_BLOCK), "a finished building was cleared away");
        helper.assertTrue(level.getBlockState(new BlockPos(cx + 37, gy + 5, cz)).isAir() && level.getBlockState(new BlockPos(cx + 37, gy + 2, cz)).is(Blocks.STONE),
                "the edge should be cut back in a slope, not a cliff");
        helper.assertTrue(Village.shift(t, Job.CONSTRUCTION, 2) > 0 || Village.count(t, Job.CONSTRUCTION) > 0, "no builders");
        Village.construct(level, t, 20);
        Village.construct(level, t, 20);
        helper.assertTrue(t.building(c.id) == null, "a finished clearing should be done with");

        t.buildings.remove(hut);
        // The larger courtyards sleep fifty and a hundred.
        for (BuildingType type : new BuildingType[]{BuildingType.MEDIUM_RESIDENTIAL_COURTYARD, BuildingType.LARGE_RESIDENTIAL_COURTYARD}) {
            Building yard = new Building();
            yard.id = 9000 + type.ordinal();
            yard.type = type;
            yard.origin = t.center.offset(200, 0, 200);
            long beds = com.realmlord.village.BuildingDesigns.design(level, t, yard).stream().filter(pl -> pl.state().getBlock() == Blocks.YELLOW_BED
                    && pl.state().getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD).count();
            helper.assertTrue(beds >= type.housing(), type.id() + " should hold " + type.housing() + " beds, has " + beds);
        }
        // The Lord's residence: rebuilt at Grade 2, with nothing lost and no residents' beds.
        for (int x = -9; x <= 9; x++)
            for (int z = -9; z <= 9; z++) {
                level.setBlock(new BlockPos(cx + x, gy, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 18; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
            }
        com.realmlord.territory.Founding.buildManor(level, t.center, net.minecraft.core.Direction.NORTH);
        BlockPos box = t.center.offset(2, 0, 2);
        level.setBlock(box, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(box) instanceof net.minecraft.world.Container ct) ct.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(Village.housing(t) == Village.MANOR_HOUSING, "the Grade 1 manor should house ten");
        t.level = com.realmlord.territory.TerritoryLevel.VILLAGE_2;
        helper.assertTrue(com.realmlord.territory.Residence.upgrade(level, t) && t.manorTier == 2, "the manor was not rebuilt");
        helper.assertTrue(!com.realmlord.territory.Residence.upgrade(level, t), "rebuilt twice");
        helper.assertTrue(t.storage.countItem(Items.DIAMOND) == 3, "what was kept in the old manor was lost");
        helper.assertTrue(Village.housing(t) == 0 && Residents.beds(level, t).isEmpty(), "residents should no longer sleep in the manor");
        helper.assertTrue(level.getBlockState(t.center).is(com.realmlord.registry.ModBlocks.LORDS_MANOR.get()), "the manor seal is gone");
        // Facing north: u is east (+x), v is north (-z).
        helper.assertTrue(level.getBlockState(t.center.offset(-4, 5, 5)).is(Blocks.RED_BED), "no bed for the Lord upstairs");
        helper.assertTrue(level.getBlockState(t.center.offset(-5, 0, 0)).is(Blocks.CHEST), "no storeroom chests");
        helper.assertTrue(level.getBlockState(t.center.offset(4, 0, -4)).is(Blocks.SPRUCE_STAIRS), "no stairs up");
        helper.assertTrue(level.getBlockState(t.center.offset(0, 0, -6)).is(Blocks.DARK_OAK_DOOR), "the door should still face north");
        t.level = com.realmlord.territory.TerritoryLevel.VILLAGE_3;
        helper.assertTrue(com.realmlord.territory.Residence.upgrade(level, t) && t.manorTier == 3, "the Grade 3 residence was not built");
        helper.assertTrue(level.getBlockState(t.center.offset(3, 0, 2)).is(Blocks.ENCHANTING_TABLE), "no enchanting table in the Grade 3 study");
        Territory copy = Territory.load(t.save());
        helper.assertTrue(copy.manorTier == 3 && copy.manorFront == net.minecraft.core.Direction.NORTH.get2DDataValue(), "manor state lost on saving");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "labor_safety")
    public static void safetyProtectionAndRecords(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        ServerPlayer stranger = RealmTests.mockPlayer(helper);
        Territory t = VillageTests.village(helper, p, 7200);
        ServerLevel level = helper.getLevel();
        int gy = t.center.getY() - 1, cx = t.center.getX(), cz = t.center.getZ();

        // The upgrade asks for beds, not small courtyards: four medium ones will do.
        for (int i = 0; i < 4; i++) VillageTests.built(t, BuildingType.MEDIUM_RESIDENTIAL_COURTYARD);
        helper.assertTrue(Village.courtyardBeds(t) == 200, "four medium courtyards should hold 200");
        boolean needsHousing = Village.upgradeMissing(t, Profiles.get(p)).stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && (tc.getKey().equals("realmlord.upgrade.need_housing") || tc.getKey().equals("realmlord.upgrade.need_building") && String.valueOf(java.util.Arrays.toString(tc.getArgs())).contains("small_residential")));
        helper.assertTrue(!needsHousing, "200 beds in medium courtyards should meet the housing requirement");

        // A finished fence keeps monsters out.
        Building fence = new Building();
        fence.id = t.nextBuildingId++;
        fence.type = BuildingType.SIMPLE_FENCE;
        fence.origin = t.center;
        fence.fenceRadius = 30;
        fence.gates = new int[]{0};
        fence.fenceStyle = 2;
        fence.complete = true;
        fence.workNeeded = fence.progress = 1;
        t.buildings.add(fence);
        helper.assertTrue(com.realmlord.territory.TerritoryEvents.fenced(level, t.center.offset(10, 0, 5)), "inside the fence should be safe");
        helper.assertTrue(!com.realmlord.territory.TerritoryEvents.fenced(level, t.center.offset(60, 0, 0)), "outside the fence is not");

        // Towers stand in the wall; nothing else may, and nothing may block a gate.
        for (int x = -40; x <= 40; x++)
            for (int z = -40; z <= 40; z++) {
                level.setBlock(new BlockPos(cx + x, gy, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 6; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
            }
        helper.assertTrue("realmlord.build.err.gate".equals(Village.checkPlacement(level, t, BuildingType.ARROW_TOWER, new BlockPos(cx + 29, gy + 1, cz - 1), 0)), "a tower was allowed in a gateway");
        helper.assertTrue("realmlord.build.err.on_fence".equals(Village.checkPlacement(level, t, BuildingType.BATHROOM, new BlockPos(cx - 2, gy + 1, cz + 27), 0)), "a hut was allowed on the fence");
        BlockPos towerAt = new BlockPos(cx - 1, gy + 1, cz + 29);
        String err = Village.checkPlacement(level, t, BuildingType.ARROW_TOWER, towerAt, 0);
        helper.assertTrue(err == null, "a tower should stand on the fence: " + err);
        Building tower = new Building();
        tower.id = t.nextBuildingId++;
        tower.type = BuildingType.ARROW_TOWER;
        tower.origin = towerAt;
        t.buildings.add(tower);
        com.realmlord.village.BuildingDesigns.forget(fence);
        var design = com.realmlord.village.BuildingDesigns.design(level, t, fence);
        helper.assertTrue(design != null && design.stream().noneMatch(pl -> pl.pos().getX() >= towerAt.getX() && pl.pos().getX() < towerAt.getX() + 3
                && pl.pos().getZ() >= towerAt.getZ() && pl.pos().getZ() < towerAt.getZ() + 3), "the wall should make room for the tower");
        t.buildings.remove(tower);
        t.buildings.remove(fence);

        // Only the Lord and those trusted may build; the manor's chests are the Lord's alone.
        BlockPos field = t.center.offset(20, 0, 0), manorChest = t.center.offset(2, 0, 2);
        if (!stranger.hasPermissions(2)) {
            helper.assertTrue(!com.realmlord.territory.TerritoryEvents.allowed(stranger, field, false), "a stranger could build in the territory");
            t.trusted.add(stranger.getUUID());
            helper.assertTrue(com.realmlord.territory.TerritoryEvents.allowed(stranger, field, false), "a trusted player could not build");
            helper.assertTrue(!com.realmlord.territory.TerritoryEvents.allowed(stranger, manorChest, true), "a trusted player opened the Lord's chests");
            helper.assertTrue(com.realmlord.territory.TerritoryEvents.allowed(p, manorChest, true), "the Lord could not open his own chests");
            Territory copy = Territory.load(t.save());
            helper.assertTrue(copy.trusts(stranger.getUUID()), "trust lost on saving");
        }

        // A resident who dies is gone, and the log says so.
        Resident r = t.residents.get(t.residents.size() - 1);
        SettlerEntity s = com.realmlord.registry.ModEntities.SETTLER.get().create(level);
        s.moveTo(cx + 12.5, gy + 1, cz + 12.5, 0, 0);
        s.link(t, r);
        r.entity = s.getUUID();
        level.addFreshEntity(s);
        int before = t.residents.size();
        s.kill();
        helper.assertTrue(t.resident(r.id) == null && t.residents.size() == before - 1, "a dead resident lived on in the records");
        helper.assertTrue(!t.log.isEmpty() && t.log.get(0).json().contains("realmlord.village.died"), "the death was not written in the log");

        // The ledger, and flour, meat and fish eaten when the grain runs out.
        t.food = 0;
        com.realmlord.village.Goods.MEAT.add(t, 500);
        long day = GaiaCalendar.day(level.getServer().overworld().getGameTime()) + 1;
        long need = Village.foodUse(t);
        Village.day(level, t, day);
        helper.assertTrue(t.ledger[Village.LEDGER_EATEN] == need && t.ledger[Village.LEDGER_OTHER_FOOD] == need && t.hungryDays == 0,
                "the village should have eaten meat: eaten " + t.ledger[Village.LEDGER_EATEN] + ", other " + t.ledger[Village.LEDGER_OTHER_FOOD] + " of " + need);
        helper.assertTrue(t.ledger[Village.LEDGER_DAY] == day && Territory.load(t.save()).ledger[Village.LEDGER_EATEN] == need, "ledger not kept");
        helper.assertTrue(com.realmlord.RealmConfig.visibleResidents() >= 48, "more residents should be shown");
        RealmTests.leave(stranger);
        VillageTests.done(helper, p, t);
    }

    private static int[] column(Building path, int x, int z) {
        for (int i = 0; i + 3 < path.pathCols.length; i += 4)
            if (path.pathCols[i] == x && path.pathCols[i + 2] == z) return java.util.Arrays.copyOfRange(path.pathCols, i, i + 4);
        return null;
    }

    private static Building site(Territory t, BuildingType type, int dx) {
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = type;
        b.origin = t.center.offset(dx, 0, 1000);
        b.stage = Building.BUILD;
        b.workNeeded = Village.workNeeded(type);
        t.buildings.add(b);
        return b;
    }
}
