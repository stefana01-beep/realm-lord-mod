package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.Affiliates;
import com.realmlord.village.Army;
import com.realmlord.village.Auction;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Festivals;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Job;
import com.realmlord.village.Occupation;
import com.realmlord.village.Office;
import com.realmlord.village.Recruits;
import com.realmlord.village.Refugees;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import com.realmlord.village.Visitors;
import com.realmlord.world.Trials;
import com.realmlord.world.WorldState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 3, the town: the upgrade conditions, the county trial, departments and the cabinet, the
 * standing army, the Recruitment Hall, refugees, festivals, visitors, affiliated villages, the
 * auction and the mountain barbarians.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class TownRealmTests {

    private static Resident staff(Territory t, Occupation occ, Building b, int rank) {
        Resident r = Village.add(t, Resident.random(RandomSource.create(5), occ, 0));
        r.rank = rank;
        if (b != null) Village.assign(t, r, Job.STAFF, b.id);
        return r;
    }

    private static String key(Component c) {
        return c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getKey() : "";
    }

    private static void grow(Territory t, int n) {
        RandomSource r = RandomSource.create(9);
        for (int i = 0; i < n; i++) Village.add(t, Resident.random(r, Occupation.FARMER, 0));
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "town_upgrade")
    public static void townConditionsAndTheCountyTrial(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 18800);
        ServerLevel level = helper.getLevel();
        // A Grade 3 village may now become a town: the novel's conditions, not "later".
        t.level = TerritoryLevel.VILLAGE_3;
        java.util.List<Component> missing = Village.upgradeMissing(t, com.realmlord.profile.Profiles.get(p));
        helper.assertTrue(missing.stream().noneMatch(c -> key(c).equals("realmlord.upgrade.town_later")), "town upgrades are still closed");
        helper.assertTrue(missing.stream().anyMatch(c -> key(c).equals("realmlord.upgrade.need_pop")
                && java.util.Arrays.asList(((net.minecraft.network.chat.contents.TranslatableContents) c.getContents()).getArgs()).contains(1000)),
                "a town should need 1,000 people");
        // Town buildings are in reach of a town and not of a village.
        helper.assertTrue(Village.checkCost(p, t, BuildingType.BANK, true) != null, "a village could build a bank");
        t.level = TerritoryLevel.TOWN_2;
        helper.assertTrue(Village.checkCost(p, t, BuildingType.BANK, true) == null, "a Grade 2 town could not build a bank");
        helper.assertTrue(Village.checkCost(p, t, BuildingType.INN, true) != null, "a Grade 2 town could build a Grade 3 town's inn");
        helper.assertTrue(Village.townDraw(t) == 3.0, "a Grade 2 town should draw three times the migrants");
        // The county trial: applying brings raiders in three days; they come in waves at three gates.
        t.level = TerritoryLevel.TOWN_3;
        helper.assertTrue(Trials.apply(p, t), "the application was refused");
        long today = GaiaCalendar.today(level.getServer());
        helper.assertTrue(t.ext.getLong("trialDay") == today + Trials.WARNING_DAYS, "the raiders should come in three days");
        helper.assertTrue(!Trials.apply(p, t), "applied twice");
        for (int cx = -12; cx <= 12; cx++)
            for (int cz = -12; cz <= 12; cz++) level.setChunkForced((t.center.getX() >> 4) + cx, (t.center.getZ() >> 4) + cz, true);
        Trials.day(level, t, today + Trials.WARNING_DAYS, level.getRandom());
        helper.assertTrue(t.ext.getLong("trialStart") > 0 && t.ext.getInt("trialWave") == 1, "the attack did not begin");
        java.util.List<BanditEntity> raiders = level.getEntitiesOfClass(BanditEntity.class, new AABB(t.center).inflate(t.radius() + 80),
                b -> b.getTags().contains(Trials.TAG));
        helper.assertTrue(raiders.size() >= 4, "the first wave was too small: " + raiders.size());
        // Five at the manor is a breach: the town is plundered and must wait to try again.
        for (int i = 0; i < Trials.BREACH && i < raiders.size(); i++) raiders.get(i).teleportTo(t.center.getX() + 0.5, t.center.getY(), t.center.getZ() + 0.5);
        t.treasury = 9_000;
        Trials.update(level, t);
        helper.assertTrue(t.ext.getLong("trialStart") == 0 && t.treasury == 6_000, "a breach should end the trial and plunder a third");
        helper.assertTrue(t.ext.getLong("trialRetry") > today, "a failed town should wait to apply again");
        helper.assertTrue(level.getEntitiesOfClass(BanditEntity.class, new AABB(t.center).inflate(t.radius() + 80), b -> b.getTags().contains(Trials.TAG)).isEmpty(),
                "the raiders stayed after the trial");
        // A won trial is remembered until the upgrade goes through.
        Trials.win(level, t, java.util.List.of());
        helper.assertTrue(t.ext.getBoolean("trialWon") && t.level == TerritoryLevel.TOWN_3, "a won trial with conditions unmet should wait for them");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "town_army")
    public static void departmentsAndTheArmy(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 19200);
        ServerLevel level = helper.getLevel();
        t.level = TerritoryLevel.TOWN_1;
        grow(t, 30);
        // The town's departments open with the Government Office; eight offices make a cabinet.
        helper.assertTrue(!Office.SECURITY.open(t), "the Security Division opened without a Government Office");
        VillageTests.built(t, BuildingType.GOVERNMENT_OFFICE);
        helper.assertTrue(Office.SECURITY.open(t) && !Office.PROCURATORATE.open(t), "the Grade 1 town departments should open, not the Grade 3");
        int[] before = Village.indexes(t);
        Office[] eight = {Office.STEWARD, Office.TREASURER, Office.WORKS, Office.AGRICULTURE, Office.MILITARY, Office.COMMERCE, Office.RITES, Office.SECURITY};
        int k = 0;
        for (Resident r : t.residents) {
            if (k >= eight.length) break;
            if (r.office >= 0 || r.isFigure()) continue;
            helper.assertTrue(Office.appoint(t, eight[k], r) == null, "could not appoint " + eight[k]);
            k++;
        }
        helper.assertTrue(Office.cabinet(t), "eight offices should make a cabinet");
        int[] after = Village.indexes(t);
        helper.assertTrue(after[2] >= before[2] + 5, "a cabinet should raise every index");
        // The standing army: a City Defense Camp holds fifty; crossbowmen need an arsenal.
        helper.assertTrue(Army.cap(t) == 0, "a town without a camp kept an army");
        VillageTests.built(t, BuildingType.CITY_DEFENSE_CAMP);
        helper.assertTrue(Army.cap(t) >= 50, "a City Defense Camp should hold fifty");
        int militia = Village.militiaCount(t);
        Resident a = null, b = null;
        for (Resident r : t.residents) {
            if (r.office >= 0 || r.isFigure() || r.militia) continue;
            if (a == null) a = r;
            else if (b == null) b = r;
        }
        helper.assertTrue(Army.enlist(t, a, Army.Unit.INFANTRY) == null && a.unit == Army.Unit.INFANTRY.ordinal() && a.militia, "could not enlist infantry");
        helper.assertTrue(Army.enlist(t, b, Army.Unit.CROSSBOWMAN) != null, "crossbowmen without an arsenal");
        VillageTests.built(t, BuildingType.ARSENAL);
        helper.assertTrue(Army.enlist(t, b, Army.Unit.CROSSBOWMAN) == null, "could not enlist a crossbowman");
        helper.assertTrue(Army.enlist(t, b, Army.Unit.MOUNTAIN_WARRIOR) != null || b.mountain, "a lowlander became a mountain warrior");
        helper.assertTrue(Army.count(t) == 2 && Village.militiaCount(t) == militia && Army.strength(t) > 0, "the army should count apart from the militia");
        helper.assertTrue(Army.rank(a).equals("soldier"), "a new soldier should be a Soldier");
        t.treasury = 1_000;
        Army.day(level, t);
        helper.assertTrue(t.treasury == 1_000 - Army.Unit.INFANTRY.pay - Army.Unit.CROSSBOWMAN.pay, "the soldiers were not paid");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "town_people")
    public static void recruitsRefugeesFestivalsVisitorsAndAffiliates(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 19600);
        ServerLevel level = helper.getLevel();
        t.level = TerritoryLevel.TOWN_1;
        for (int i = 0; i < 3; i++) VillageTests.built(t, BuildingType.RESIDENTIAL_QUARTER);
        grow(t, 90);
        long day = t.lastDay + 1;
        // The Recruitment Hall finds an alchemist for ten gold.
        Building hall = VillageTests.built(t, BuildingType.RECRUITMENT_HALL);
        staff(t, Occupation.CIVIL_SERVANT, hall, 1);
        hall.mode = 0;
        helper.assertTrue(Recruits.wanted(hall) == Occupation.ALCHEMIST, "the hall's first choice should be an alchemist");
        t.treasury = Recruits.COST;
        Recruits.day(level, t, day, level.getRandom());
        helper.assertTrue(t.treasury == 0 && t.residents.stream().anyMatch(r -> r.occupation == Occupation.ALCHEMIST && r.rank >= 2),
                "the Recruitment Hall brought no Advanced alchemist");
        Recruits.day(level, t, day, level.getRandom());
        // Refugees come every month and settle as beds allow.
        t.peopleThoughts = 80;
        int people = t.residents.size();
        Refugees.day(level, t, day, level.getRandom());
        helper.assertTrue(t.residents.size() > people, "no refugees settled");
        // A festival in its month: People's Thoughts up, the treasury pays.
        long festDay = (GaiaCalendar.year(day) * 12L) * GaiaCalendar.daysPerMonth();
        t.ext.remove("festivalKey");
        t.treasury = 100_000;
        Festivals.day(level, t, festDay, level.getRandom());
        helper.assertTrue(Festivals.morale(t) >= 5 && t.treasury != 100_000, "the Spring Festival was not held");
        // A visitor comes to a Lord with a name, and joins for a fee.
        t.ext.putLong("lordRep", 5_000);
        com.realmlord.profile.Profiles.get(p).addReputation(5_000);
        for (int m = 0; m < 40 && !Visitors.present(t); m++) Visitors.day(level, t, day + (long) m * GaiaCalendar.daysPerMonth(), level.getRandom());
        helper.assertTrue(Visitors.present(t), "no visitor came in forty months");
        t.treasury = 10_000_000;
        int before = t.residents.size();
        helper.assertTrue(Visitors.recruit(p, t) == null && t.residents.size() == before + 1, "the visitor did not join");
        // An affiliated village: fifty settlers go out, and it sends tax and grain home.
        t.wood = 1_000;
        t.stone = 1_000;
        int pop = t.residents.size();
        helper.assertTrue(Affiliates.found(p, t) == null, "could not found an affiliated village");
        helper.assertTrue(t.residents.size() == pop - Affiliates.SETTLERS && Affiliates.population(t) == Affiliates.SETTLERS, "the settlers did not move out");
        helper.assertTrue(Affiliates.found(p, t) != null, "a Grade 1 town founded a second affiliated village");
        long treasury = t.treasury;
        Affiliates.day(level, t, day, level.getRandom());
        helper.assertTrue(t.treasury > treasury && Affiliates.population(t) > Affiliates.SETTLERS, "the affiliated village neither grew nor paid");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "town_auction")
    public static void auctionAndMountainTribes(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 20000);
        ServerLevel level = helper.getLevel();
        t.level = TerritoryLevel.TOWN_3;
        long day = t.lastDay + 1;
        WorldState w = WorldState.get(level.getServer());
        w.ext.remove("auction");
        w.ext.remove("auctionDay");
        Auction.day(level, day);
        ListTag lots = Auction.lots(w);
        helper.assertTrue(lots.size() == Auction.LOTS, "the auction should hold five lots");
        helper.assertTrue(Auction.bid(p, t, 0) != null, "bid without an Auction House");
        VillageTests.built(t, BuildingType.AUCTION_HOUSE);
        long price = Auction.nextBid(lots.getCompound(0));
        com.realmlord.profile.Profiles.get(p).setPurse(0);
        p.getInventory().clearContent();
        t.treasury = price * 3;
        helper.assertTrue(Auction.bid(p, t, 0) == null, "the bid failed");
        helper.assertTrue(t.treasury == price * 2 && Auction.lots(w).getCompound(0).getString("bidder").equals(p.getUUID().toString()),
                "the bid was not paid and recorded");
        helper.assertTrue(Auction.bid(p, t, 0) != null, "outbid yourself");
        // A mountain tribe that submits sends its people to the refugee camp; they can be Mountain Warriors.
        helper.assertTrue(com.realmlord.build.Designs.builtin("barbarian_village") != null && com.realmlord.build.Designs.builtin("bandit_fortress") != null
                && com.realmlord.build.Designs.builtin("ruined_city") != null && com.realmlord.build.Designs.builtin("cemetery") != null, "a design is missing");
        Refugees.add(t, 5, true);
        helper.assertTrue(Refugees.waiting(t) == 5, "the tribe's people are not waiting");
        Resident m = Village.add(t, Refugees.mountaineer(level.getRandom()));
        VillageTests.built(t, BuildingType.CITY_DEFENSE_CAMP);
        helper.assertTrue(Army.enlist(t, m, Army.Unit.MOUNTAIN_WARRIOR) == null, "a mountain barbarian could not be a Mountain Warrior");
        VillageTests.done(helper, p, t);
    }
}
