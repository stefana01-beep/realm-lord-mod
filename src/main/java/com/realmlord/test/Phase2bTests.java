package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.entity.beast.Beasts;
import com.realmlord.profile.CombatClass;
import com.realmlord.registry.ModEntities;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Figures;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Goods;
import com.realmlord.village.Job;
import com.realmlord.village.Office;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import com.realmlord.world.Beastwave;
import com.realmlord.world.Dens;
import com.realmlord.world.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

/** Phase 2b: summoned figures, companions, offices, the militia, hunting, beasts, dens, the beastwave and hidden buildings. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class Phase2bTests {

    @GameTest(template = "empty", timeoutTicks = 300, batch = "p2b_people")
    public static void figuresOfficesMilitiaAndHunters(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 9200);
        ServerLevel level = helper.getLevel();
        // The roster: at least 150 figures, each unique, of every rank, with sensible stats.
        var all = Figures.all();
        helper.assertTrue(all.size() >= 150, "the roster has only " + all.size() + " figures");
        Set<String> ids = new HashSet<>();
        int[] ranks = new int[4];
        for (var f : all) {
            helper.assertTrue(ids.add(f.id()), "two figures share the id " + f.id());
            ranks[f.rank()]++;
            for (int s : new int[]{f.command(), f.force(), f.intelligence(), f.politics()})
                helper.assertTrue(s >= 10 && s <= 100, f.id() + " has a stat of " + s);
        }
        for (int r = 0; r < 4; r++) helper.assertTrue(ranks[r] >= 10, "too few figures of rank " + r);
        // Summoning: a figure of the token's rank joins the village, once in all the world.
        int before = t.residents.size();
        Resident a = Figures.summon(p, t, 1);
        Resident b = Figures.summon(p, t, 1);
        helper.assertTrue(a != null && b != null && t.residents.size() == before + 2, "summoned figures did not join");
        helper.assertTrue(a.isFigure() && !a.figure.equals(b.figure) && Figures.byId(a.figure).rank() == 1 && a.grade == 3, "wrong figure summoned: " + a.figure);
        helper.assertTrue(WorldState.get(level.getServer()).summoned.contains(a.figure), "the summoned figure is not marked as taken");
        helper.assertTrue(Resident.load(a.save()).figure.equals(a.figure), "a figure lost who they are on saving");
        // Offices: open by grade, and they work.
        helper.assertTrue("realmlord.office.err.grade".equals(Office.appoint(t, Office.WORKS, a)), "the Works office opened at Grade 1");
        long tax0 = Village.dailyTax(t);
        int pol0 = Village.indexes(t)[0];
        Resident minister = a.politics >= b.politics ? a : b;
        helper.assertTrue(Office.appoint(t, Office.TREASURER, minister) == null && Office.holder(t, Office.TREASURER) == minister, "could not appoint a treasurer");
        helper.assertTrue(Village.dailyTax(t) > tax0, "a treasurer should raise the taxes");
        helper.assertTrue(Office.appoint(t, Office.STEWARD, minister == a ? b : a) == null && Village.indexes(t)[0] > pol0, "a steward should raise the political index");
        helper.assertTrue("realmlord.job.err.office".equals(Village.canAssign(t, minister, Job.LOGGING, -1)), "an office holder was sent logging");
        // The militia needs a camp; a camp holds ten, and they drill.
        Resident farmer = t.residents.get(1);
        helper.assertTrue("realmlord.militia.err.no_camp".equals(Village.enlist(t, farmer, true)), "a militia without a camp");
        VillageTests.built(t, BuildingType.MILITARY_CAMP);
        while (t.residents.size() < 30) Village.add(t, Resident.random(level.getRandom(), com.realmlord.village.Occupation.FARMER, 0));
        int enlisted = 0;
        for (Resident r : t.residents) if (r.office < 0 && Village.enlist(t, r, true) == null) enlisted++;
        helper.assertTrue(enlisted == 10 && Village.militiaCount(t) == 10, "a camp should hold ten militia, enlisted " + enlisted);
        int force = 0;
        for (Resident r : t.residents) if (r.militia) force += r.force;
        // Hunters bring meat and hides.
        Resident hunter = null;
        for (Resident r : t.residents) if (!r.militia && r.office < 0 && !r.isFigure()) hunter = r;
        helper.assertTrue(Village.assign(t, hunter, Job.HUNTING, -1), "could not send a hunter out");
        long meat = Goods.MEAT.stock(t), leather = Goods.LEATHER.stock(t);
        long day = GaiaCalendar.today(level.getServer());
        t.food = 100_000;
        for (int d = 1; d <= 10; d++) Village.day(level, t, day + d);
        int force2 = 0;
        for (Resident r : t.residents) if (r.militia) force2 += r.force;
        helper.assertTrue(force2 > force, "ten days of drill made the militia no stronger");
        helper.assertTrue(Goods.MEAT.stock(t) - meat >= 40 && Goods.LEATHER.stock(t) > leather, "the hunter brought too little: meat +" + (Goods.MEAT.stock(t) - meat));
        // Hidden buildings stay hidden until revealed.
        helper.assertTrue(!BuildingType.ofGrade(2).contains(BuildingType.MAZU_TEMPLE), "the Mazu Temple is listed openly");
        t.level = TerritoryLevel.VILLAGE_2;
        Village.revealHidden(level, t);
        if (!Village.coastal(level, t)) helper.assertTrue(!t.revealed.contains("mazu_temple"), "the Mazu Temple was revealed inland");
        t.revealed.add("mazu_temple");
        helper.assertTrue(Territory.load(t.save()).revealed.contains("mazu_temple"), "revealed buildings lost on saving");
        t.level = TerritoryLevel.VILLAGE_1;
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "p2b_companion")
    public static void companionsFollowAndRecover(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 9600);
        ServerLevel level = helper.getLevel();
        Resident hero = Figures.summon(p, t, 2);
        helper.assertTrue(hero != null, "no figure summoned");
        hero.follows = true;
        p.teleportTo(level, t.center.getX() + 4.5, t.center.getY() + 1, t.center.getZ() + 0.5, 0, 0);
        SettlerEntity s = ModEntities.SETTLER.get().create(level);
        s.moveTo(t.center.getX() + 2.5, t.center.getY() + 1, t.center.getZ() + 2.5, 0, 0);
        s.link(t, hero);
        hero.entity = s.getUUID();
        level.addFreshEntity(s);
        s.routine(t, hero);
        helper.assertTrue(s.getActivity() == SettlerEntity.Activity.FOLLOW && s.fighter(), "the figure should walk with the Lord");
        helper.assertTrue(s.getMaxHealth() > 30, "a Divine-rank figure should be hardier than a farmer: " + s.getMaxHealth());
        s.hurt(level.damageSources().generic(), 10_000);
        helper.assertTrue(s.isAlive() && !hero.follows && hero.woundedUntil > GaiaCalendar.today(level.getServer()), "a figure struck down should be wounded, not killed");
        s.discard();
        VillageTests.done(helper, p, t);
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "p2b_beasts")
    public static void beastsDensAndTheBeastwave(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 10000);
        ServerLevel level = helper.getLevel();
        int gy = t.center.getY() - 1, cx = t.center.getX(), cz = t.center.getZ();
        for (int x = -40; x <= 40; x++)
            for (int z = -40; z <= 40; z++) {
                level.setBlock(new BlockPos(cx + x, gy, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 8; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
            }
        // Each beast leaves what the novel's hunters prized.
        Object[][] drops = {{ModEntities.WILD_BOAR.get(), ModItems.BOAR_HIDE.get()}, {ModEntities.GREY_WOLF.get(), ModItems.WOLF_PELT.get()},
                {ModEntities.MOUNTAIN_TIGER.get(), ModItems.TIGER_PELT.get()}, {ModEntities.SNOW_BEAR.get(), ModItems.BEAR_PAW.get()}};
        int i = 0;
        for (Object[] d : drops) {
            BlockPos at = new BlockPos(cx + 20 + (i++) * 4, gy + 1, cz + 20);
            Mob m = ((EntityType<? extends Mob>) d[0]).create(level);
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            level.addFreshEntity(m);
            helper.assertTrue(m.isAlive(), m.getType() + " did not appear");
            m.hurt(level.damageSources().playerAttack(p), 10_000);
            Item want = (Item) d[1];
            boolean got = !level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3), e -> e.getItem().is(want)).isEmpty();
            helper.assertTrue(got, m.getType() + " dropped no " + want);
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(cx, gy, cz)).inflate(60))) e.discard();
        // The drops are useful: crafted, cooked or sold.
        var recipes = level.getRecipeManager();
        for (String r : new String[]{"tusk_spear", "boar_king_spear", "tiger_bone_wine", "leather_from_tiger_pelt", "braised_bear_paw_smelting"})
            helper.assertTrue(recipes.byKey(new ResourceLocation(RealmLord.MODID, r)).isPresent(), "missing recipe " + r);
        helper.assertTrue(Goods.of(new net.minecraft.world.item.ItemStack(ModItems.TIGER_PELT.get())).good() == Goods.LEATHER, "a tiger pelt should sell as leather");
        // A den: the Boar King and his sounder; slaying him clears it for some days.
        WorldState w = WorldState.get(level.getServer());
        WorldState.Den den = Dens.create(level, new BlockPos(cx - 25, gy + 1, cz - 25), Dens.BOAR);
        w.dens.add(den);
        Entity king = level.getEntity(den.king);
        int boars = level.getEntitiesOfClass(com.realmlord.entity.beast.WildBoarEntity.class, new AABB(den.pos).inflate(14),
                e -> e.getTags().contains(Beasts.DEN_TAG) && !(e instanceof com.realmlord.entity.beast.BoarKingEntity)).size();
        helper.assertTrue(king instanceof com.realmlord.entity.beast.BoarKingEntity && boars >= 4, "the den is not guarded: king " + king + ", boars " + boars);
        king.hurt(level.damageSources().playerAttack(p), 10_000);
        helper.assertTrue(den.cleared && den.respawnDay > GaiaCalendar.today(level.getServer()), "slaying the king did not clear the den");
        helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, new AABB(king.blockPosition()).inflate(6), e -> e.getItem().is(ModItems.BOAR_KING_TUSK.get())).isEmpty(),
                "the Boar King left no tusk: king at " + king.position() + ", den at " + den.pos + ", items near: "
                        + level.getEntitiesOfClass(ItemEntity.class, new AABB(king.blockPosition()).inflate(8)).stream().map(e -> e.getItem().toString()).toList());
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(den.pos).inflate(20), e -> e.getTags().contains(Beasts.DEN_TAG) || e instanceof ItemEntity)) e.discard();
        w.dens.remove(den);
        // The beastwave: on every other new year from the third year, and it sends beasts at the manor.
        helper.assertTrue(!Beastwave.waveYear(2) && Beastwave.waveYear(3) && !Beastwave.waveYear(4) && Beastwave.waveYear(5), "wrong beastwave years");
        helper.assertTrue(Beastwave.nextWaveYear(0) == 3 && Beastwave.newYearDay(3) == 24L * GaiaCalendar.daysPerMonth(), "wrong first beastwave day");
        // The ring where the beasts gather lies beyond the forced area: load it, and give it a moment.
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) level.setChunkForced((cx >> 4) + x, (cz >> 4) + z, true);
        helper.runAfterDelay(40, () -> {
            // Dry ground all round the ring.
            for (int x = -100; x <= 100; x++)
                for (int z = -100; z <= 100; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d < t.radius() + 12 || d > t.radius() + 32) continue;
                    level.setBlock(new BlockPos(cx + x, gy, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 6; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
                }
            w.waveDay = GaiaCalendar.today(level.getServer());
            int n = Beastwave.spawnWave(level, t, 1);
            var wave = level.getEntitiesOfClass(Mob.class, new AABB(t.center).inflate(t.radius() + 40), e -> e.getTags().contains(Beasts.WAVE_TAG));
            helper.assertTrue(n > 0 && wave.size() == n, "the wave did not come: " + n + " / " + wave.size());
            helper.assertTrue(t.center.equals(Beasts.raid(wave.get(0))), "the beasts are not driving on the manor");
            for (int k = 0; k < Math.min(3, wave.size()); k++) wave.get(k).hurt(level.damageSources().playerAttack(p), 10_000);
            helper.assertTrue(w.waveKills.getOrDefault(p.getUUID(), 0) >= 1, "the defender's kills were not counted");
            long coins = com.realmlord.village.Coins.count(p);
            Beastwave.end(level.getServer(), w);
            helper.assertTrue(com.realmlord.village.Coins.count(p) > coins, "the best defender was not rewarded");
            helper.assertTrue(level.getEntitiesOfClass(Mob.class, new AABB(t.center).inflate(t.radius() + 40), e -> e.getTags().contains(Beasts.WAVE_TAG)).isEmpty(),
                    "beasts remained after the wave");
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(cx, gy, cz)).inflate(120))) e.discard();
            for (int x = -8; x <= 8; x++)
                for (int z = -8; z <= 8; z++) if (Math.abs(x) > 5 || Math.abs(z) > 5) level.setChunkForced((cx >> 4) + x, (cz >> 4) + z, false);
            VillageTests.done(helper, p, t);
        });
    }
}
