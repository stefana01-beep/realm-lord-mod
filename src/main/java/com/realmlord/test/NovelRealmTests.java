package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.block.SiegeEngineBlockEntity;
import com.realmlord.entity.BanditEntity;
import com.realmlord.item.SummoningTokenItem;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Profiles;
import com.realmlord.registry.ModBlocks;
import com.realmlord.registry.ModCrafts;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Exams;
import com.realmlord.village.FigureArts;
import com.realmlord.village.Figures;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Goods;
import com.realmlord.village.Job;
import com.realmlord.village.Navy;
import com.realmlord.village.Occupation;
import com.realmlord.village.Resident;
import com.realmlord.village.Temples;
import com.realmlord.village.Trade;
import com.realmlord.village.Village;
import com.realmlord.village.Workshops;
import com.realmlord.world.Raids;
import com.realmlord.world.WaterForts;
import com.realmlord.world.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The novel's realm: salt fields and mines, the dock, shipyard and navy, temples and their
 * priests, the academy and the imperial examination, chambers of commerce and caravans, the
 * summoned figures' gifts, water bandits, barbarian raids and the siege engines.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class NovelRealmTests {

    private static int byJob(Territory t, Job job, int n) {
        int moved = 0;
        for (Resident r : t.residents) {
            if (moved >= n) break;
            if (r.militia || r.office >= 0 || Village.isStaffing(t, r) || r.job == job) continue;
            r.job = job;
            r.workplace = -1;
            moved++;
        }
        return moved;
    }

    private static Resident hire(Territory t, Occupation occ, Building b) {
        Resident r = Resident.random(net.minecraft.util.RandomSource.create(7), occ, 0);
        r.rank = 1;
        Village.add(t, r);
        if (b != null) Village.assign(t, r, Job.STAFF, b.id);
        return r;
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "realm_economy")
    public static void saltShipsNavyAndCaravans(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 17600);
        ServerLevel level = helper.getLevel();
        long day = t.lastDay + 1;
        // Salt field: salt makers rake out salt.
        VillageTests.built(t, BuildingType.SALT_FIELD);
        helper.assertTrue(Village.places(t, Job.SALT_MAKING) > 0, "a salt field should give salt-making places");
        helper.assertTrue(byJob(t, Job.SALT_MAKING, 2) == 2, "could not set two salt makers");
        t.food = 5000;
        long salt = Goods.SALT.stock(t);
        Village.day(level, t, day++);
        helper.assertTrue(Goods.SALT.stock(t) - salt >= 20, "two salt makers should make at least 20 salt, made " + (Goods.SALT.stock(t) - salt));
        // Dock and shipyard: a shipbuilder lays down fishing boats; each boat gives two fishing places.
        VillageTests.built(t, BuildingType.DOCK);
        Building yard = VillageTests.built(t, BuildingType.SHIPYARD);
        hire(t, Occupation.SHIPBUILDER, yard);
        helper.assertTrue(Village.staffOf(t, yard) != null, "the shipbuilder is not running the shipyard");
        t.wood = 1000;
        Village.day(level, t, day++);
        helper.assertTrue(Navy.ships(t, ModCrafts.FISHING_BOAT.get()) == 1, "the shipyard should build a fishing boat a day");
        helper.assertTrue(Navy.fishingPlaces(t) == 2, "a fishing boat at the dock carries two fishermen");
        // A warship needs its manual; slow ships carry their work over the days.
        yard.mode = 3;
        helper.assertTrue(!Workshops.mode(yard).known(t), "Mengchong warships were known without the manual");
        yard.mode = 2;
        t.storage.addItem(new net.minecraft.world.item.ItemStack(ModCrafts.PINE_TAR.get(), 8));
        for (int i = 0; i < 5; i++) Village.day(level, t, day++);
        helper.assertTrue(Navy.ships(t, ModCrafts.MERCHANT_SHIP.get()) >= 1, "five days at the slipway should launch a merchant ship");
        // The navy: sailors enlist at the dock up to its cap.
        helper.assertTrue(Navy.cap(t) == 30, "a dock and a shipyard should hold 30 sailors, held " + Navy.cap(t));
        Resident sailor = null;
        for (Resident r : t.residents) if (sailor == null && !r.militia && r.office < 0 && !r.isFigure() && !Village.isStaffing(t, r)) sailor = r;
        int militia = Village.militiaCount(t);
        helper.assertTrue(sailor != null && !sailor.militia, "no free resident to enlist");
        helper.assertTrue(Navy.enlist(t, sailor, true) == null && sailor.sailor && sailor.militia, "could not enlist a sailor");
        helper.assertTrue(Village.militiaCount(t) == militia && Navy.count(t) == 1, "a sailor should not count against the militia");
        // A Chamber of Commerce sends a caravan with the village's goods, and it comes home richer.
        Building chamber = VillageTests.built(t, BuildingType.CHAMBER_OF_COMMERCE);
        hire(t, Occupation.MERCHANT, chamber);
        Goods.WINE.add(t, 400);
        long treasury = t.treasury;
        Trade.day(level, t, day, level.getRandom());
        CompoundTag c = Trade.caravanOf(t, chamber.id);
        helper.assertTrue(c != null && c.getLong("value") > 0, "no caravan left the chamber");
        helper.assertTrue(Goods.WINE.stock(t) < 400, "the caravan carried no wine");
        c.putDouble("risk", 0);
        Trade.day(level, t, c.getLong("back"), level.getRandom());
        helper.assertTrue(t.treasury > treasury, "the caravan brought nothing home");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "realm_culture")
    public static void templesExamsAndFigures(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.STRATEGIST);
        Territory t = VillageTests.village(helper, p, 18000);
        ServerLevel level = helper.getLevel();
        // A temple with no priest sends for one, paid from the treasury.
        Building temple = VillageTests.built(t, BuildingType.VILLAGE_TEMPLE);
        VillageTests.built(t, BuildingType.SMALL_RESIDENTIAL_COURTYARD);
        t.treasury = Temples.INVITE_COST + 5;
        Temples.day(level, t, t.lastDay + 1, level.getRandom());
        Resident priest = Village.staffOf(t, temple);
        helper.assertTrue(priest != null && priest.occupation == Occupation.PRIEST, "no Taoist priest came to keep the temple");
        helper.assertTrue(t.treasury == 5, "the priest's travelling money was not paid");
        temple.mode = 0;
        helper.assertTrue(Temples.morale(t) >= 3, "a priest giving blessings should raise People's Thoughts");
        temple.mode = 2;
        helper.assertTrue(Temples.cropBonus(t) > 1.05, "a priest praying for rain should raise the harvest");
        // The imperial examination, in the eighth month, at an academy.
        VillageTests.built(t, BuildingType.ACADEMY);
        for (int i = 0; i < 20; i++) {
            Resident s = Village.add(t, Resident.random(level.getRandom(), Occupation.SCHOLAR, 0));
            s.intelligence = 95;
        }
        long exam = (GaiaCalendar.year(t.lastDay) * 12L + Exams.EXAM_MONTH - 1) * GaiaCalendar.daysPerMonth();
        helper.assertTrue(GaiaCalendar.month(exam) == Exams.EXAM_MONTH, "exam day is not in the eighth month");
        Exams.day(level, t, exam, level.getRandom());
        helper.assertTrue(Exams.holders(t, 1) >= 5, "twenty bright scholars should give at least five Xiucai, gave " + Exams.holders(t, 1));
        int xiucai = Exams.holders(t, 1);
        Exams.day(level, t, exam, level.getRandom());
        helper.assertTrue(Exams.holders(t, 1) == xiucai, "the examination was held twice in one year");
        // A summoned figure lends their gift and, loyal enough, teaches their art.
        Figures.Figure sunWu = Figures.byId("sun_wu");
        helper.assertTrue(sunWu != null, "Sun Wu is missing from the figures");
        int manuals = p.getInventory().countItem(ModCrafts.SECRET_MANUAL.get());
        Resident sun = Village.add(t, Figures.resident(sunWu, level.getRandom()));
        sun.loyalty = 95;
        helper.assertTrue(FigureArts.count(t, "strategist") == 1, "Sun Wu should count as a strategist");
        helper.assertTrue(FigureArts.signature(sun) != null && FigureArts.signature(sun).id().equals("art_of_war"), "Sun Wu should teach the Art of War");
        helper.assertTrue(FigureArts.teach(p, t, sun) == null, "Sun Wu would not teach his art");
        helper.assertTrue(p.getInventory().countItem(ModCrafts.SECRET_MANUAL.get()) == manuals + 1, "the Art of War manual was not given");
        helper.assertTrue(FigureArts.teach(p, t, sun) != null, "a figure taught their art twice");
        // A title promotion brings a Summoning Token.
        int tokens = p.getInventory().countItem(com.realmlord.registry.ModItems.SUMMONING_TOKEN.get());
        Profiles.grantMerit(p, 900, null);
        helper.assertTrue(p.getInventory().countItem(com.realmlord.registry.ModItems.SUMMONING_TOKEN.get()) == tokens + 1,
                "becoming a Viscount should bring a Summoning Token");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "realm_raids")
    public static void barbariansAndWaterBandits(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 18400);
        ServerLevel level = helper.getLevel();
        long day = t.lastDay + 1;
        // A barbarian band rides in from the north and withdraws at dawn.
        int reach = (t.radius() + 40) >> 4;
        for (int cx = -reach; cx <= reach; cx++)
            for (int cz = -reach; cz <= 1; cz++) level.setChunkForced((t.center.getX() >> 4) + cx, (t.center.getZ() >> 4) + cz, true);
        int n = Raids.spawn(level, t, day);
        helper.assertTrue(n >= 3, "the barbarian band was too small: " + n);
        int seen = 0, chiefs = 0;
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (!(e instanceof BanditEntity b) || !b.getTags().contains(Raids.TAG) || b.getPersistentData().getInt(Raids.TERRITORY) != t.id) continue;
            seen++;
            helper.assertTrue(b.getFaction() == BanditEntity.Faction.BARBARIAN, "a raider was not a barbarian");
            if (b.getRole() == BanditEntity.Role.CHIEF) chiefs++;
            helper.assertTrue(b.getZ() <= t.center.getZ() + 1, "barbarians should come from the north");
        }
        helper.assertTrue(seen >= n - 1 && seen > 0 && chiefs == 1, "expected " + n + " barbarians with one chieftain, saw " + seen + " with " + chiefs);
        Raids.withdraw(level, t);
        helper.assertTrue(level.getEntitiesOfClass(BanditEntity.class, new net.minecraft.world.phys.AABB(t.center).inflate(t.radius() + 60),
                b -> b.getTags().contains(Raids.TAG)).isEmpty(), "the raiders did not withdraw");
        // A water bandits' fort: while it stands the village suffers; killing the chief breaks it.
        WorldState w = WorldState.get(level.getServer());
        CompoundTag fort = new CompoundTag();
        fort.putLong("pos", t.center.offset(80, 0, 0).asLong());
        fort.putInt("id", 424242);
        ListTag forts = WaterForts.forts(w);
        forts.add(fort);
        w.ext.put("forts", forts);
        WaterForts.day(level, t, day, level.getRandom());
        helper.assertTrue(t.ext.getBoolean("waterBandits"), "a fort nearby should harry the village");
        BanditEntity chief = ModEntities.BANDIT.get().create(level);
        chief.setFaction(BanditEntity.Faction.WATER);
        chief.configure(BanditEntity.Role.CHIEF, 2);
        chief.getPersistentData().putInt("realmlord_fort", 424242);
        WaterForts.chiefSlain(level, chief, p);
        helper.assertTrue(!t.ext.getBoolean("waterBandits"), "breaking the fort did not free the village");
        helper.assertTrue(WaterForts.near(w, t.center, WaterForts.RANGE) == null, "the fort still stands");
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "realm_siege")
    public static void siegeEnginesShoot(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        ServerLevel level = helper.getLevel();
        // A territory around the test area, so the crossbow shoots on its own.
        Territory t = new Territory();
        t.owner = p.getUUID();
        t.ownerName = "test";
        t.name = "Siege Test";
        t.center = helper.absolutePos(new BlockPos(1, 1, 1));
        com.realmlord.territory.TerritoryData.get(level.getServer()).add(t);
        BlockPos at = new BlockPos(2, 1, 0);
        helper.setBlock(at, ModBlocks.BED_CROSSBOW.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        SiegeEngineBlockEntity be = (SiegeEngineBlockEntity) helper.getBlockEntity(at);
        helper.assertTrue(be != null, "the bed crossbow has no block entity");
        helper.assertTrue(be.load(20) == 16 && be.ammo() == 16, "a bed crossbow holds sixteen bolts");
        Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        z.setNoAi(true);
        float hp = z.getHealth();
        helper.runAfterDelay(30, () -> {
            String why = be.ammo() < 16 ? null : be.autoFire();
            helper.assertTrue(why == null && be.ammo() < 16, "the bed crossbow never fired: " + why);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!z.isAlive() || z.getHealth() < hp, "the bolt missed the zombie");
            // The trebuchet's aim lands where it is told to.
            Vec3 v = com.realmlord.entity.StoneShotEntity.aim(new Vec3(0, 65, 0), new Vec3(30, 64, 0));
            helper.assertTrue(v.x > 0 && v.y > 0 && Math.abs(v.z) < 1e-6, "the trebuchet aimed the wrong way: " + v);
            com.realmlord.territory.TerritoryData.get(level.getServer()).remove(t.id);
            RealmTests.leave(p);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "realm_tokens")
    public static void tokensAndArts(GameTestHelper helper) {
        helper.assertTrue(com.realmlord.martial.Arts.byId("hell_slaughterer") != null, "the Hell Slaughterer True Manual is missing");
        helper.assertTrue(com.realmlord.martial.Arts.byId("hell_slaughterer").innate()[3] < 0, "the Hell Slaughterer should cost Charm");
        helper.assertTrue(SummoningTokenItem.rank(SummoningTokenItem.of(0)) == 0, "a King token is not King rank");
        helper.assertTrue(com.realmlord.loot.Manuals.byId("warships") != null && com.realmlord.loot.Manuals.byId("siege_engines") != null,
                "the warship and siege engine manuals are missing");
        helper.succeed();
    }
}
