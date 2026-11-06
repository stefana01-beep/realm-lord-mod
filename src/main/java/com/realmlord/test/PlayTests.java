package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.entity.beast.Beasts;
import com.realmlord.entity.beast.WildBoarEntity;
import com.realmlord.entity.level.Horses;
import com.realmlord.entity.level.MobLevels;
import com.realmlord.loot.Gear;
import com.realmlord.profile.CombatClass;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TokenGrade;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Figures;
import com.realmlord.village.GaiaCalendar;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Play-throughs: the world is left to run, and what should happen in play is watched for:
 * the militia killing a boar, a companion keeping up with the Lord, a beastwave from warning to
 * dawn, finding a den. And the wilds' new rules: levels, graded horses, no vanilla monsters,
 * the ruins and their loot, graded gear's effects.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class PlayTests {

    private static void flat(ServerLevel level, BlockPos c, int r) {
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) {
                level.setBlock(c.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 0; y < 6; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "play_militia")
    public static void militiaKillsABoar(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 10400);
        ServerLevel level = helper.getLevel();
        BlockPos at = t.center.offset(14, 0, 0);
        flat(level, at, 10);
        VillageTests.built(t, BuildingType.MILITARY_CAMP);
        Resident guard = t.residents.get(2);
        guard.force = 90;
        helper.assertTrue(Village.enlist(t, guard, true) == null, "could not enlist");
        SettlerEntity s = ModEntities.SETTLER.get().create(level);
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.link(t, guard);
        guard.entity = s.getUUID();
        level.addFreshEntity(s);
        level.setDayTime(6000);
        s.routine(t, guard);
        WildBoarEntity boar = ModEntities.WILD_BOAR.get().create(level);
        boar.moveTo(at.getX() + 4.5, at.getY(), at.getZ() + 0.5, 0, 0);
        level.addFreshEntity(boar);
        MobLevels.set(boar, 1);
        helper.assertTrue(s.fighter(), "the militia should be on duty");
        helper.succeedWhen(() -> {
            s.routine(t, guard);
            helper.assertTrue(!boar.isAlive(), "the boar still lives (" + boar.getHealth() + " hp, guard at " + s.getHealth() + ")");
            s.discard();
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "play_companion")
    public static void companionKeepsUp(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 10800);
        ServerLevel level = helper.getLevel();
        BlockPos at = t.center.offset(10, 0, 10);
        flat(level, at, 20);
        Resident hero = Figures.summon(p, t, 0);
        hero.follows = true;
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        SettlerEntity s = ModEntities.SETTLER.get().create(level);
        s.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.link(t, hero);
        hero.entity = s.getUUID();
        level.addFreshEntity(s);
        s.routine(t, hero);
        // The Lord walks off fourteen blocks; then is suddenly far away.
        p.teleportTo(level, at.getX() + 14.5, at.getY(), at.getZ() + 0.5, 0, 0);
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(s.distanceTo(p) < 7, "the companion did not keep up: " + s.distanceTo(p));
            p.teleportTo(level, at.getX() - 15.5, at.getY(), at.getZ() + 15.5, 0, 0);
            p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 18.5, 0, 0);
        });
        // Far off, standing in tall grass: the companion still finds room beside the Lord.
        helper.runAfterDelay(170, () -> {
            BlockPos far = at.offset(-18, 0, -18);
            level.setBlock(far, Blocks.TALL_GRASS.defaultBlockState(), 2);
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++) if (x != 0 || z != 0) level.setBlock(far.offset(x, 0, z), Blocks.GRASS.defaultBlockState(), 2);
            p.teleportTo(level, far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0, 0);
        });
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(s.distanceTo(p) < 8, "the companion did not catch up from afar: " + s.distanceTo(p));
            s.discard();
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "play_beastwave")
    public static void beastwaveFromWarningToDawn(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 11600);
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        var world = server.overworld();
        var rule = world.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean cycle = rule.get();
        long time = world.getDayTime();
        rule.set(true, server);
        WorldState w = WorldState.get(server);
        int cx = t.center.getX(), cz = t.center.getZ(), gy = t.center.getY();
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) level.setChunkForced((cx >> 4) + x, (cz >> 4) + z, true);
        long day = Beastwave.newYearDay(3);
        // Three days before: the warning goes out.
        world.setDayTime((day - 3) * 24000L + 1000);
        w.warnedYear = 0;
        w.lastWaveYear = 0;
        w.waveDay = -1;
        Beastwave.tick(server);
        helper.assertTrue(w.warnedYear == 3 * 10 + 3, "no warning three days before: " + w.warnedYear);
        helper.runAfterDelay(40, () -> {
            for (int x = -100; x <= 100; x++)
                for (int z = -100; z <= 100; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d < t.radius() + 12 || d > t.radius() + 32) continue;
                    level.setBlock(new BlockPos(cx + x, gy - 1, cz + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 0; y < 5; y++) level.setBlock(new BlockPos(cx + x, gy + y, cz + z), Blocks.AIR.defaultBlockState(), 2);
                }
            // New year's evening: the wave begins and the first beasts come.
            world.setDayTime(day * 24000L + 12600);
            Beastwave.tick(server);
            helper.assertTrue(w.waveDay == day, "the beastwave did not begin at nightfall on new year's day");
            Beastwave.tick(server);
            int beasts = level.getEntitiesOfClass(Mob.class, new AABB(t.center).inflate(t.radius() + 40), e -> e != null && e.getTags().contains(Beasts.WAVE_TAG)).size();
            helper.assertTrue(beasts > 0, "no beasts came in the first wave");
            // Dawn: it ends, and the beasts are gone.
            world.setDayTime((day + 1) * 24000L + 100);
            Beastwave.tick(server);
            helper.assertTrue(w.waveDay < 0 && w.lastWaveYear == 3, "the beastwave did not end at dawn");
            helper.assertTrue(level.getEntitiesOfClass(Mob.class, new AABB(t.center).inflate(t.radius() + 40), e -> e != null && e.getTags().contains(Beasts.WAVE_TAG)).isEmpty(),
                    "beasts remained after dawn");
            rule.set(cycle, server);
            world.setDayTime(time);
            for (int x = -8; x <= 8; x++)
                for (int z = -8; z <= 8; z++) if (Math.abs(x) > 5 || Math.abs(z) > 5) level.setChunkForced((cx >> 4) + x, (cz >> 4) + z, false);
            VillageTests.done(helper, p, t);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "play_wilds")
    public static void levelsHorsesRuinsAndGear(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 12000);
        ServerLevel level = helper.getLevel();
        BlockPos at = t.center.offset(20, 0, 20);
        flat(level, at, 12);
        // Every beast and bandit carries a level, shown over its head, with health to match.
        for (EntityType<? extends Mob> type : java.util.List.<EntityType<? extends Mob>>of(ModEntities.WILD_BOAR.get(), ModEntities.GREY_WOLF.get(),
                ModEntities.MOUNTAIN_TIGER.get(), ModEntities.LEOPARD.get(), ModEntities.SNOW_BEAR.get(), ModEntities.BLACK_BEAR.get(),
                ModEntities.WILD_OX.get(), ModEntities.WOLF_KING.get(), ModEntities.TIGER_KING.get(), ModEntities.BANDIT.get())) {
            Mob m = type.create(level);
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWN_EGG, null, null);
            level.addFreshEntity(m);
            int lv = MobLevels.level(m);
            var range = MobLevels.ranges().get(type);
            helper.assertTrue(lv >= range.min() && lv <= range.max() + 16, type + " has level " + lv);
            helper.assertTrue(m.isCustomNameVisible() && m.getCustomName().getString().contains(String.valueOf(lv)), type + " shows no level");
            double base = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
            helper.assertTrue(m.getMaxHealth() >= base * (1 + (lv - 1) * 0.06) - 0.5, type + " health not scaled by level");
            m.discard();
        }
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(16), e -> MobLevels.leveled(e))) m.discard();
        WildBoarEntity small = ModEntities.WILD_BOAR.get().create(level), big = ModEntities.WILD_BOAR.get().create(level);
        small.moveTo(at.getX(), at.getY(), at.getZ());
        big.moveTo(at.getX() + 3, at.getY(), at.getZ());
        level.addFreshEntity(small);
        level.addFreshEntity(big);
        MobLevels.set(small, 1);
        MobLevels.set(big, 8);
        helper.assertTrue(big.getSize() > small.getSize() && big.getBbWidth() > small.getBbWidth(), "a higher level should make a bigger boar");
        small.discard();
        big.discard();
        // Horses come in grades, and foals take after their parents.
        int[] seen = new int[4];
        for (int i = 0; i < 40; i++) {
            Horse h = EntityType.HORSE.create(level);
            h.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            level.addFreshEntity(h);
            TokenGrade g = Horses.grade(h);
            helper.assertTrue(g != null && h.getCustomName() != null, "an ungraded horse");
            seen[g.ordinal()]++;
            h.discard();
        }
        helper.assertTrue(seen[0] > seen[3], "Gold-grade horses should be rare");
        Horse fast = EntityType.HORSE.create(level), slow = EntityType.HORSE.create(level);
        fast.moveTo(at.getX(), at.getY(), at.getZ());
        slow.moveTo(at.getX(), at.getY(), at.getZ());
        level.addFreshEntity(fast);
        level.addFreshEntity(slow);
        Horses.setGrade(fast, 3, level.getRandom());
        Horses.setGrade(slow, 0, level.getRandom());
        helper.assertTrue(fast.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                > slow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), "a Gold horse should be faster than a Bronze");
        helper.assertTrue(Horses.foal(3, 3, level.getRandom()) >= 2, "a foal of two Gold horses should be at least Silver");
        fast.discard();
        slow.discard();
        // Minecraft's monsters don't come to the overworld.
        Mob zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(at.getX(), at.getY(), at.getZ());
        var ev = new net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn(zombie, level, at.getX(), at.getY(), at.getZ(),
                level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null, null);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
        helper.assertTrue(ev.isSpawnCancelled(), "a zombie was allowed to spawn");
        zombie.discard();
        // The ruins: their templates load and their chests hold the mod's loot.
        var templates = level.getStructureManager();
        for (String s : new String[]{"ancient_shrine", "ruined_watchtower", "ancient_tomb"})
            helper.assertTrue(templates.get(new ResourceLocation(RealmLord.MODID, s)).isPresent(), "missing template " + s);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).create(LootContextParamSets.CHEST);
        boolean graded = false, coins = false, blueprint = false;
        for (int i = 0; i < 20; i++) {
            for (String tbl : new String[]{"ancient_tomb", "tomb_treasure", "watchtower", "ancient_shrine"}) {
                var table = level.getServer().getLootData().getLootTable(new ResourceLocation(RealmLord.MODID, "chests/" + tbl));
                for (ItemStack st : table.getRandomItems(params)) {
                    if (Gear.gradeOf(st) != null) graded = true;
                    if (st.is(com.realmlord.registry.ModItems.SILVER_COIN.get()) || st.is(com.realmlord.registry.ModItems.GOLD_COIN.get())) coins = true;
                    if (st.is(com.realmlord.registry.ModItems.BLUEPRINT.get()) && !com.realmlord.loot.Blueprints.buildingOf(st).isEmpty()) blueprint = true;
                }
            }
        }
        helper.assertTrue(graded && coins && blueprint, "ruin loot is missing graded gear " + graded + ", coins " + coins + " or blueprints " + blueprint);
        // Graded gear's effects: a Gold weapon always carries one, and a flaming blade burns.
        ItemStack gold = Gear.make(Items.IRON_SWORD, TokenGrade.GOLD, level.getRandom());
        helper.assertTrue(Gear.effectOf(gold) != null && Gear.effectOf(gold).weapon, "a Gold weapon without an effect");
        ItemStack flame = Gear.make(Items.IRON_SWORD, TokenGrade.BRONZE, level.getRandom());
        flame.getOrCreateTag().putInt(Gear.EFFECT, Gear.Effect.FLAME.ordinal());
        WildBoarEntity attacker = ModEntities.WILD_BOAR.get().create(level);
        attacker.moveTo(at.getX(), at.getY(), at.getZ());
        attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, flame);
        level.addFreshEntity(attacker);
        Cow cow = EntityType.COW.create(level);
        cow.moveTo(at.getX() + 1, at.getY(), at.getZ());
        level.addFreshEntity(cow);
        cow.hurt(level.damageSources().mobAttack(attacker), 1.0F);
        helper.assertTrue(cow.isOnFire(), "a flaming blade did not burn");
        attacker.discard();
        cow.discard();
        // A den nearby is found as the player comes near.
        WorldState w = WorldState.get(level.getServer());
        WorldState.Den den = new WorldState.Den();
        den.pos = at;
        den.cleared = true;
        den.respawnDay = Long.MAX_VALUE;
        w.dens.add(den);
        p.teleportTo(level, at.getX() + 10.5, at.getY(), at.getZ() + 0.5, 0, 0);
        Dens.check(p, false);
        helper.assertTrue(den.discoveredBy.contains(p.getUUID()), "the den was not found");
        w.dens.remove(den);
        VillageTests.done(helper, p, t);
    }
}
