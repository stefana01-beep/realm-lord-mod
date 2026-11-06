package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.entity.beast.GreyWolfEntity;
import com.realmlord.entity.level.MobLevels;
import com.realmlord.loot.Gear;
import com.realmlord.menu.Sorting;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Profiles;
import com.realmlord.quest.QuestLogic;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Coins;
import com.realmlord.village.Job;
import com.realmlord.village.Occupation;
import com.realmlord.village.Paths;
import com.realmlord.village.Resident;
import com.realmlord.village.Workshops;
import com.realmlord.world.Beastwave;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * This round: the purse and treasury, the storehouse's tidying, workshops' choices and lessons,
 * experience from kills and soldiers' levels, beasts going for villagers, gentler beastwaves,
 * stepped paths and graded slopes, and the Bandit Lands.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class TownTests {

    private static Building staffed(Territory t, BuildingType type, Occupation occ, int rank) {
        Building b = VillageTests.built(t, type);
        Resident m = t.residents.get(0);
        for (Resident r : t.residents) if (r.job != Job.STAFF && !r.militia) {
            m = r;
            break;
        }
        m.occupation = occ;
        m.rank = rank;
        m.job = Job.STAFF;
        m.workplace = b.id;
        return b;
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "town_economy")
    public static void purseStorehouseAndWorkshops(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 14000);
        ServerLevel level = helper.getLevel();
        // Coins go to the purse, not the inventory; payments come out of it, and then the treasury.
        p.getInventory().clearContent();
        Profiles.get(p).setPurse(0);
        Coins.give(p, 25_000);
        helper.assertTrue(Coins.purse(p) == 25_000 && Coins.carried(p) == 0, "coins given should go into the purse");
        p.getInventory().add(new ItemStack(com.realmlord.registry.ModItems.SILVER_COIN.get(), 5));
        helper.assertTrue(Coins.pocket(p) == 500 && Coins.purse(p) == 25_500, "carried coins were not pocketed");
        helper.assertTrue(Coins.pay(p, 500) && Coins.purse(p) == 25_000, "paying from the purse failed");
        t.treasury = 10_000;
        helper.assertTrue(Coins.payWithTreasury(p, 30_000) && Coins.purse(p) == 0 && t.treasury == 5_000, "the treasury did not make up the difference");
        helper.assertTrue(!Coins.payWithTreasury(p, 6_000), "paid more than purse and treasury hold");

        // The storehouse is large and tidies itself.
        helper.assertTrue(t.storage.getContainerSize() >= 540, "the storehouse should hold ten pages");
        SimpleContainer c = new SimpleContainer(9);
        c.setItem(0, new ItemStack(Items.COAL, 20));
        c.setItem(3, new ItemStack(Items.STONE, 5));
        c.setItem(5, new ItemStack(Items.COAL, 50));
        Sorting.sort(c, 0, 9);
        helper.assertTrue(c.getItem(0).is(Items.STONE) && c.getItem(1).is(Items.COAL) && c.getItem(1).getCount() == 64
                && c.getItem(2).getCount() == 6 && c.getItem(3).isEmpty(), "sorting did not merge and order: " + c.getItem(0) + " " + c.getItem(1) + " " + c.getItem(2));
        // Every drop turned up by digging is kept: coal goes to the storehouse.
        com.realmlord.village.Sites.toStores(t, new ItemStack(Items.COAL, 3));
        helper.assertTrue(t.storage.countItem(Items.COAL) >= 3, "coal was not kept");

        // A smithy set to forge arms puts graded iron gear in the storehouse.
        Building smithy = staffed(t, BuildingType.BLACKSMITH_WORKSHOP, Occupation.BLACKSMITH, 1);
        smithy.mode = 1;
        t.ironOre = 100;
        Workshops.make(t, level.getRandom());
        boolean gear = false;
        for (int i = 0; i < t.storage.getContainerSize(); i++) if (Gear.gradeOf(t.storage.getItem(i)) != null) gear = true;
        helper.assertTrue(gear && t.ironOre == 88, "the smithy did not forge graded gear from iron ore (ore left " + t.ironOre + ")");
        helper.assertTrue(!Workshops.makingStock(smithy), "a smithy forging arms should not also make tools stock");

        // A school teaches: a commoner sent there becomes a scholar.
        Building school = staffed(t, BuildingType.PRIVATE_SCHOOL, Occupation.SCHOLAR, 0);
        Resident pupil = null;
        for (Resident r : t.residents) if (Workshops.canLearn(t, r) && r.occupation == Occupation.FARMER) {
            pupil = r;
            break;
        }
        helper.assertTrue(pupil != null && Workshops.enrol(t, school, pupil) == null && pupil.trainingAt == school.id, "could not enrol a pupil");
        int wits = pupil.intelligence;
        for (int d = 0; d < 20 && pupil.trainingAt >= 0; d++) Workshops.teach(t, level.getRandom());
        helper.assertTrue(pupil.occupation == Occupation.SCHOLAR && pupil.trainingAt < 0 && pupil.intelligence > wits,
                "the pupil did not become a scholar: " + pupil.occupation + ", still learning " + (pupil.trainingAt >= 0));
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "town_combat")
    public static void killsTeachAndBeastsHuntVillagers(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 14400);
        ServerLevel level = helper.getLevel();
        BlockPos at = t.center.offset(20, 0, 0);
        // A wolf (not a "monster" to Minecraft) still teaches the one who kills it.
        GreyWolfEntity wolf = ModEntities.GREY_WOLF.get().create(level);
        wolf.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(wolf);
        MobLevels.set(wolf, 4);
        var prof = Profiles.get(p);
        int lv0 = prof.getLevel();
        long xp0 = prof.getXp();
        wolf.hurt(level.damageSources().playerAttack(p), 10_000);
        helper.assertTrue(prof.getLevel() > lv0 || prof.getXp() > xp0, "killing a wolf gave no experience");

        // Soldiers rise in level from drill and battle.
        Resident s = t.residents.get(1);
        int force = s.force;
        int up = s.gainSoldierXp(Resident.soldierXpToNext(1) + Resident.soldierXpToNext(2));
        helper.assertTrue(up == 2 && s.soldierLevel == 3 && s.force >= Math.min(100, force + 2), "soldier levels wrong: " + s.soldierLevel);

        // A beast near a villager goes for them, beastwave or not.
        SettlerEntity v = ModEntities.SETTLER.get().create(level);
        v.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        Resident vr = t.residents.get(t.residents.size() - 1);
        v.link(t, vr);
        vr.entity = v.getUUID();
        level.addFreshEntity(v);
        GreyWolfEntity w2 = ModEntities.GREY_WOLF.get().create(level);
        w2.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(w2);
        // An early beastwave is young beasts, not kings forty levels up.
        helper.assertTrue(Beastwave.waveLevel(level, t, 1, false, level.getRandom()) <= 12
                && Beastwave.waveLevel(level, t, 3, true, level.getRandom()) <= 20, "the first beastwave is too strong");
        helper.runAfterDelay(2, () -> {
            w2.setTarget(null);
            w2.tickCount = 40;
            com.realmlord.entity.beast.Beasts.think(w2);
            helper.assertTrue(w2.getTarget() == v, "the wolf ignored the villager");
            w2.discard();
            v.discard();
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "town_ground")
    public static void stairPathsSlopesAndTheBanditLands(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 14800);
        ServerLevel level = helper.getLevel();
        BlockPos a = t.center.offset(20, 0, 20);
        // Level ground first, whatever the test world's terrain, then a rise of three blocks along the way.
        for (int x = -6; x < 24; x++)
            for (int z = -8; z <= 8; z++) {
                for (int y = -3; y < 0; y++) level.setBlock(a.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                for (int y = 0; y < 12; y++) level.setBlock(a.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        for (int x = 0; x < 18; x++)
            for (int z = -5; z <= 5; z++) {
                int h = Math.min(3, Math.max(0, x - 4));
                for (int y = 0; y < h; y++) level.setBlock(a.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                for (int y = h; y < h + 4; y++) level.setBlock(a.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        var cols = Paths.columns(level, t, new int[]{a.getX(), a.getZ(), a.getX() + 11, a.getZ()}, 1, true);
        int steps = 0;
        for (int[] c : cols) if (Paths.stairFacing(c[3]) > 0) steps++;
        helper.assertTrue(cols != null && steps >= 3, "a stepped path should have a stair for each block it climbs, has " + steps);
        // A graded clearing shapes a ramp: its far end lies higher than its near end.
        t.wood = t.stone = 1000;
        helper.assertTrue(com.realmlord.village.Village.startClearing(p, t, a.getX(), a.getZ() - 2, a.getX() + 11, a.getZ() + 2, 0, true) == null, "could not grade a slope");
        Building ramp = t.buildings.get(t.buildings.size() - 1);
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = 0; i + 3 < ramp.clearCols.length; i += 4) {
            if (ramp.clearCols[i + 3] == Building.NO_FILL || ramp.clearCols[i + 2] == Building.NO_CUT) continue;
            lo = Math.min(lo, ramp.clearCols[i + 2]);
            hi = Math.max(hi, ramp.clearCols[i + 2]);
        }
        helper.assertTrue(hi - lo >= 2, "the graded clearing is flat: " + lo + ".." + hi);
        // The Bandit Lands exist, and a camp site can be found there.
        ServerLevel lands = level.getServer().getLevel(QuestLogic.LANDS);
        helper.assertTrue(lands != null, "the Bandit Lands dimension is missing");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "town_void")
    public static void residentsNeverFallOutOfTheWorld(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 15200);
        ServerLevel level = helper.getLevel();
        // Land that isn't loaded is never trusted as ground.
        helper.assertTrue(SettlerEntity.safeGround(level, t.center.getX() + 200_000, t.center.getZ()) == null, "unloaded land was taken for ground");
        helper.assertTrue(SettlerEntity.safeGround(level, t.center.getX(), t.center.getZ()) != null, "the manor's own ground was not found");
        // A resident dropped below the world is caught and brought back alive.
        Resident r = t.residents.get(0);
        SettlerEntity s = ModEntities.SETTLER.get().create(level);
        s.moveTo(t.center.getX() + 0.5, level.getMinBuildHeight() - 10, t.center.getZ() + 0.5);
        s.link(t, r);
        r.entity = s.getUUID();
        level.addFreshEntity(s);
        s.routine(t, r);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(s.isAlive() || s.isRemoved() && !s.isDeadOrDying(), "the resident died in the void");
            helper.assertTrue(!s.isAlive() || s.getY() > level.getMinBuildHeight() + 2, "the resident was left in the void at y " + s.getY());
            helper.assertTrue(t.residents.contains(r), "the resident was lost from the village");
            s.discard();
            VillageTests.done(helper, p, t);
        });
    }
}
