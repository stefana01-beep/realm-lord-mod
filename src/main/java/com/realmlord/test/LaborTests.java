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
