package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.loot.Blueprints;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Title;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Coins;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Job;
import com.realmlord.village.Market;
import com.realmlord.village.Occupation;
import com.realmlord.village.Resident;
import com.realmlord.village.Trades;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** In-game tests for Phase 2: the village economy, construction, the market and residents. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class VillageTests {

    /** A Lord with a fresh, settled village far from the test grid, its chunks kept loaded. */
    private static Territory village(GameTestHelper helper, ServerPlayer p, int dx) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0)).offset(dx, 0, 0);
        for (int cx = -5; cx <= 5; cx++)
            for (int cz = -5; cz <= 5; cz++) level.setChunkForced((base.getX() >> 4) + cx, (base.getZ() >> 4) + cz, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        Territory t = new Territory();
        t.owner = p.getUUID();
        t.ownerName = p.getGameProfile().getName();
        t.name = "Test Village " + dx;
        t.grade = TokenGrade.BRONZE;
        t.center = new BlockPos(base.getX(), y, base.getZ());
        TerritoryData.get(level.getServer()).add(t);
        Profiles.get(p).setTerritoryId(t.id);
        Village.settle(t, level.getRandom(), 0);
        t.lastDay = GaiaCalendar.day(level.getServer().overworld().getGameTime());
        return t;
    }

    private static Building built(Territory t, BuildingType type) {
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = type;
        b.origin = t.center.offset(1000 + b.id * 20, 0, 1000);
        b.complete = true;
        b.workNeeded = 1;
        b.progress = 1;
        t.buildings.add(b);
        return b;
    }

    private static void done(GameTestHelper helper, ServerPlayer p, Territory t) {
        TerritoryData.get(helper.getLevel().getServer()).remove(t.id);
        RealmTests.leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_settle")
    public static void villageStartsWithTheNovelsRefugees(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 2000);
        helper.assertTrue(t.residents.size() == 10, "expected 10 refugees, got " + t.residents.size());
        long chiefs = t.residents.stream().filter(r -> r.occupation == Occupation.CIVIL_SERVANT && r.grade == 0).count();
        helper.assertTrue(chiefs == 1, "the village chief (Bronze civil servant) is missing");
        helper.assertTrue(t.residents.stream().filter(r -> r.occupation == Occupation.FRUIT_FARMER).count() == 2, "two fruit farmers expected");
        helper.assertTrue(t.residents.stream().anyMatch(r -> r.occupation == Occupation.CARPENTER), "no carpenter");
        helper.assertTrue(t.residents.stream().anyMatch(r -> r.occupation == Occupation.MASON), "no mason");
        helper.assertTrue(Village.housing(t) == Village.MANOR_HOUSING, "manor should house 10");
        helper.assertTrue(t.level.populationCap() == 200, "Grade 1 village should hold 200");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_economy")
    public static void jobsProduceAndPeopleEat(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 2400);
        ServerLevel level = helper.getLevel();
        built(t, BuildingType.LOGGING_FIELD);
        built(t, BuildingType.QUARRY);
        helper.assertTrue(Village.places(t, Job.LOGGING) == 10, "a logging field holds 10 workers");
        helper.assertTrue(Village.shift(t, Job.LOGGING, 4) == 4, "could not assign 4 loggers");
        helper.assertTrue(Village.shift(t, Job.QUARRYING, 2) == 2, "could not assign 2 quarrymen");
        helper.assertTrue(Village.shift(t, Job.MINING, 2) == 0, "assigned miners without a mining field");
        t.food = 100;
        long wood = t.wood, stone = t.stone, treasury = t.treasury;
        Village.day(level, t, t.lastDay + 1);
        helper.assertTrue(t.wood - wood >= 40, "4 loggers should cut at least 40 wood, cut " + (t.wood - wood));
        helper.assertTrue(t.stone - stone >= 10, "2 quarrymen should cut at least 10 stone, cut " + (t.stone - stone));
        helper.assertTrue(t.food == 100 - t.residents.size(), "each resident eats 1 food a day");
        helper.assertTrue(t.treasury > treasury, "no taxes collected");
        // No housing beyond the manor: nobody new can move in.
        helper.assertTrue(t.residents.size() == 10, "immigrants arrived with no housing: " + t.residents.size());
        built(t, BuildingType.SMALL_RESIDENTIAL_COURTYARD);
        t.food = 1000;
        Village.day(level, t, t.lastDay + 2);
        helper.assertTrue(t.residents.size() > 10 && t.residents.size() <= 30, "a courtyard should let immigrants in: " + t.residents.size());
        // Starvation lowers People's Thoughts.
        t.food = 0;
        int before = t.peopleThoughts;
        Village.day(level, t, t.lastDay + 3);
        helper.assertTrue(t.peopleThoughts < before, "hunger did not lower People's Thoughts");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_farm")
    public static void farmingFollowsTheSeasons(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 2800);
        ServerLevel level = helper.getLevel();
        Building f = built(t, BuildingType.FARMLAND);
        Village.shift(t, Job.FARMING, 5);
        long perMonth = GaiaCalendar.daysPerMonth();
        long april = 3 * perMonth; // day index of month 4
        helper.assertTrue(GaiaCalendar.month(april) == 4, "calendar month wrong: " + GaiaCalendar.month(april));
        helper.assertTrue(GaiaCalendar.sowingYield(4) == 1.0F && GaiaCalendar.sowingYield(1) < 0.8F, "sowing windows wrong");
        t.food = 100_000;
        Village.day(level, t, april);
        helper.assertTrue(f.sown && f.sowYield == 1.0F, "field not sown at Qingming");
        long food = t.food;
        Village.day(level, t, april + 3 * perMonth);
        long harvest = t.food - food + t.residents.size();
        helper.assertTrue(harvest >= 1500, "harvest too small: " + harvest);
        helper.assertTrue(!f.sown, "field not cleared after harvest");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "village_build")
    public static void buildingsArePlacedAndRise(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 3200);
        ServerLevel level = helper.getLevel();
        // Placement rules
        BlockPos onManor = t.center.offset(-2, 0, -2);
        helper.assertTrue("realmlord.build.err.manor".equals(Village.checkPlacement(level, t, BuildingType.DINING_HALL, onManor, 0)), "allowed building on the manor yard");
        BlockPos outside = t.center.offset(t.radius() + 5, 0, 0);
        helper.assertTrue("realmlord.build.err.border".equals(Village.checkPlacement(level, t, BuildingType.DINING_HALL, outside, 0)), "allowed building outside the border");
        // Cost: no blueprint, no resources
        BlockPos site = t.center.offset(14, 0, -3);
        helper.assertTrue(Village.start(p, t, BuildingType.DINING_HALL, site, 1) != null, "built without paying");
        t.wood = 100;
        t.stone = 100;
        p.getInventory().add(Blueprints.of("dining_hall"));
        String err = Village.start(p, t, BuildingType.DINING_HALL, site, 1);
        helper.assertTrue(err == null, "could not start the Dining Hall: " + err);
        helper.assertTrue(t.wood == 70 && t.stone == 80, "costs not taken: " + t.wood + "/" + t.stone);
        helper.assertTrue(p.getInventory().countItem(ModItems.BLUEPRINT.get()) == 0, "blueprint not used up");
        Building b = t.buildings.get(t.buildings.size() - 1);
        helper.assertTrue(Village.checkPlacement(level, t, BuildingType.BATHROOM, b.origin, 0) != null, "allowed a building on top of another");
        // Nothing happens without builders.
        Village.construct(level, t, 20);
        helper.assertTrue(b.progress == 0, "built with no builders");
        Village.shift(t, Job.CONSTRUCTION, 5);
        for (int i = 0; i < 400 && !b.complete; i++) Village.construct(level, t, (int) (b.workNeeded / 50) + 1);
        helper.assertTrue(b.complete, "Dining Hall never finished: " + b.placed + " blocks placed, progress " + b.progress + "/" + b.workNeeded);
        int solid = 0;
        for (int x = 0; x < b.sizeX(); x++)
            for (int z = 0; z < b.sizeZ(); z++)
                for (int y = 0; y < 4; y++) if (!level.getBlockState(b.origin.offset(x, y, z)).isAir()) solid++;
        helper.assertTrue(solid > 40, "the Dining Hall has hardly any blocks: " + solid);
        helper.assertTrue(Village.countComplete(t, BuildingType.DINING_HALL) == 1, "not counted as built");
        // A specialist is needed for the Blacksmith Workshop.
        t.wood = 1000;
        t.stone = 1000;
        p.getInventory().add(Blueprints.of("blacksmith_workshop"));
        t.residents.removeIf(r -> r.occupation == Occupation.BLACKSMITH);
        helper.assertTrue("realmlord.build.err.specialist".equals(Village.checkCost(p, t, BuildingType.BLACKSMITH_WORKSHOP, false)), "built a smithy with no blacksmith");
        // The fence rings the border.
        err = Village.start(p, t, BuildingType.SIMPLE_FENCE, t.center, 0);
        helper.assertTrue(err == null, "could not start the fence: " + err);
        Building fence = t.buildings.get(t.buildings.size() - 1);
        for (int i = 0; i < 400 && !fence.complete; i++) Village.construct(level, t, (int) (fence.workNeeded / 50) + 1);
        helper.assertTrue(fence.complete, "fence never finished");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_upgrade")
    public static void villageUpgradesWhenReady(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 3600);
        helper.assertTrue(!Village.upgradeMissing(t, Profiles.get(p)).isEmpty(), "a new village should not be ready to upgrade");
        for (BuildingType bt : BuildingType.values()) {
            if (bt.requiredFor == 1) for (int i = 0; i < bt.requiredCount; i++) built(t, bt);
        }
        helper.assertTrue(Village.countComplete(t, BuildingType.SMALL_RESIDENTIAL_COURTYARD) == 10, "ten courtyards required, as in the novel");
        while (t.residents.size() < 200) Village.add(t, Resident.random(helper.getLevel().getRandom(), Occupation.FARMER, 0));
        helper.assertTrue(!Village.upgradeMissing(t, Profiles.get(p)).isEmpty(), "upgraded without the title");
        Profiles.grantMerit(p, Title.BARON_2.merit, null);
        List<?> missing = Village.upgradeMissing(t, Profiles.get(p));
        helper.assertTrue(missing.isEmpty(), "still missing: " + missing);
        long merit = Profiles.get(p).getMerit();
        helper.assertTrue(Village.upgrade(p, t), "upgrade failed");
        helper.assertTrue(t.level == TerritoryLevel.VILLAGE_2, "not a Grade 2 village");
        helper.assertTrue(Profiles.get(p).getMerit() > merit, "no merit for upgrading");
        helper.assertTrue(t.freeChoices.size() == 3, "should offer three random buildings");
        BuildingType pick = t.freeChoices.get(0);
        helper.assertTrue(Village.chooseFree(t, pick) && t.freeBuilds.contains(pick), "free building not granted");
        helper.assertTrue(Village.checkCost(p, t, pick, true) == null, "free building still costs");
        helper.assertTrue(t.level.populationCap() == 500, "Grade 2 village should hold 500");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_trade")
    public static void marketAndResidentTrades(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 4000);
        p.getInventory().clearContent();
        Coins.give(p, 10_000);
        helper.assertTrue(Coins.count(p) == 10_000, "coin count wrong: " + Coins.count(p));
        long wood = t.wood;
        helper.assertTrue(Village.buy(p, t, Market.WOOD, 100), "could not buy 100 wood");
        helper.assertTrue(t.wood == wood + 100 && Coins.count(p) == 10_000 - 2_000, "wood is 20 copper a unit: coins left " + Coins.count(p));
        helper.assertTrue(Village.sell(p, t, Market.WOOD, 100), "could not sell 100 wood");
        helper.assertTrue(Coins.count(p) == 8_000 + 1_600, "selling should pay 80% (20% tax): " + Coins.count(p));
        helper.assertTrue(!Village.sell(p, t, Market.IRON_ORE, 1_000_000), "sold ore the village does not have");
        // Blueprints cost 2 gold for a Grade 1 building.
        Coins.give(p, 20_000);
        long before = Coins.count(p);
        helper.assertTrue(Village.buyBlueprint(p, BuildingType.DINING_HALL), "could not buy a blueprint");
        helper.assertTrue(before - Coins.count(p) == 20_000, "blueprint should cost 2 gold");
        // A blacksmith only sells tools from a workshop.
        Resident smith = Village.add(t, Resident.random(helper.getLevel().getRandom(), Occupation.BLACKSMITH, 0));
        int pick = -1;
        List<Trades.Offer> offers = Trades.of(Occupation.BLACKSMITH);
        for (int i = 0; i < offers.size(); i++) if (offers.get(i).needsWorkshop()) pick = i;
        helper.assertTrue("realmlord.trade.no_workshop".equals(Village.trade(p, t, smith, pick)), "sold tools with no forge");
        Building forge = built(t, BuildingType.BLACKSMITH_WORKSHOP);
        helper.assertTrue(Village.assign(t, smith, Job.STAFF, forge.id), "could not staff the forge");
        Coins.give(p, 10_000);
        helper.assertTrue(Village.trade(p, t, smith, pick) == null, "could not buy from a staffed blacksmith");
        helper.assertTrue(p.getInventory().countItem(offers.get(pick).item()) >= 1, "bought item missing");
        // A farmer cannot staff a forge.
        Resident farmer = t.residents.stream().filter(r -> r.occupation == Occupation.FARMER).findFirst().orElseThrow();
        helper.assertTrue(Village.canAssign(t, farmer, Job.STAFF, forge.id) != null, "a farmer staffed the forge");
        // Selling to a carpenter: 16 logs.
        p.getInventory().add(new ItemStack(Items.OAK_LOG, 16));
        long coins = Coins.count(p);
        int logs = -1;
        List<Trades.Offer> carp = Trades.of(Occupation.CARPENTER);
        for (int i = 0; i < carp.size(); i++) if (!carp.get(i).sell()) logs = i;
        Resident carpenter = t.residents.stream().filter(r -> r.occupation == Occupation.CARPENTER).findFirst().orElseThrow();
        helper.assertTrue(Village.trade(p, t, carpenter, logs) == null, "carpenter would not buy logs");
        helper.assertTrue(Coins.count(p) > coins && p.getInventory().countItem(Items.OAK_LOG) == 0, "log sale went wrong");
        done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "village_people")
    public static void residentsAppearInTheVillage(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = village(helper, p, 4400);
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 6; i++) com.realmlord.village.Residents.sync(level, t);
        long shown = t.residents.stream().filter(r -> r.entity != null && level.getEntity(r.entity) != null).count();
        helper.assertTrue(shown == 10, "expected all 10 residents walking about, saw " + shown);
        var e = level.getEntity(t.residents.get(0).entity);
        helper.assertTrue(e instanceof com.realmlord.entity.SettlerEntity s && s.getResidentId() == t.residents.get(0).id, "entity not linked to its record");
        helper.assertTrue(Blocks.AIR != null, "");
        done(helper, p, t);
    }
}
