package com.realmlord.test;

import com.realmlord.RealmConfig;
import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.item.ResourcesBagItem;
import com.realmlord.loot.Blueprints;
import com.realmlord.loot.Gear;
import com.realmlord.network.ClientProfileData;
import com.realmlord.registry.ModEntities;
import com.realmlord.network.SyncProfilePacket;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.Profile;
import com.realmlord.profile.ProfileCapability;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Skill;
import com.realmlord.profile.Stat;
import com.realmlord.profile.Title;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.quest.BanditCamp;
import com.realmlord.quest.QuestLogic;
import com.realmlord.quest.VillageQuest;
import com.realmlord.registry.ModBlocks;
import com.realmlord.registry.ModItems;
import com.realmlord.skill.Skills;
import com.realmlord.territory.Founding;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game tests, run on CI with {@code ./gradlew runGameTestServer}. They drive the real game
 * code with a joined player and fail the build on any error.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class RealmTests {

    /** A real ServerPlayer joined through the player list over an in-memory channel. */
    static ServerPlayer mockPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "realm-test-" + (System.nanoTime() % 100000));
        ServerPlayer p = new ServerPlayer(server, helper.getLevel(), profile) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        server.getPlayerList().placeNewPlayer(connection, p);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 0));
        p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        return p;
    }

    static ServerPlayer lord(GameTestHelper helper, CombatClass c) {
        ServerPlayer p = mockPlayer(helper);
        QuestLogic.chooseRole(p, PlayMode.LORD, c, null);
        return p;
    }

    static void leave(ServerPlayer p) {
        p.getServer().getPlayerList().remove(p);
    }

    // ------------------------------------------------------------------ profile

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void choosingLordStartsTheQuest(GameTestHelper helper) {
        ServerPlayer p = lord(helper, CombatClass.GENERAL);
        Profile prof = Profiles.get(p);
        helper.assertTrue(prof.getMode() == PlayMode.LORD, "mode was not Lord");
        helper.assertTrue(prof.getCombatClass() == CombatClass.GENERAL, "sub-profession not set");
        helper.assertTrue(prof.quest().stage == VillageQuest.Stage.SUPPLIES, "Village Creation quest did not start");
        for (Stat s : Stat.values()) helper.assertTrue(prof.stat(s) == 5, s + " should start at 5");
        helper.assertTrue(prof.getPoints() == 2, "a new character should have 2 free points");
        for (var i : com.realmlord.profile.Innate.values()) {
            helper.assertTrue(prof.innate(i) >= 5 && prof.innate(i) <= 20, i + " out of range: " + prof.innate(i));
        }
        // Choosing twice is ignored.
        QuestLogic.chooseRole(p, PlayMode.ADVENTURER, CombatClass.HEALER, null);
        helper.assertTrue(prof.getMode() == PlayMode.LORD, "a second choice overwrote the first");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void titlesFollowMerit(GameTestHelper helper) {
        helper.assertTrue(Title.forMerit(99) == Title.NONE, "99 merit should give no title");
        helper.assertTrue(Title.forMerit(100) == Title.BARON_3, "100 merit should be Third Class Baron");
        helper.assertTrue(Title.forMerit(800) == Title.VISCOUNT_3, "800 merit should be Third Class Viscount");
        helper.assertTrue(Title.forMerit(204_800) == Title.MARQUIS_1, "204800 merit should be First Class Marquis");
        helper.assertTrue(Title.forMerit(1_000_000) == Title.DUKE, "a million merit should be Duke");
        helper.assertTrue(Title.forMerit(5_000_000) == Title.SON_OF_HEAVEN, "five million merit should be Son of Heaven");
        for (int i = 2; i <= Title.MARQUIS_1.ordinal(); i++) {
            Title t = Title.byId(i), prev = Title.byId(i - 1);
            helper.assertTrue(t.merit == prev.merit * 2, "merit should double each rank at " + t);
        }
        ServerPlayer p = lord(helper, CombatClass.STRATEGIST);
        Profiles.grantMerit(p, 400, null);
        helper.assertTrue(Profiles.get(p).getTitle() == Title.BARON_1, "400 merit did not make a First Class Baron");
        helper.assertTrue(p.getDisplayName().getString().length() > p.getGameProfile().getName().length() + 2,
                "title missing from display name: " + p.getDisplayName().getString());
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void novelLevellingAndAttributes(GameTestHelper helper) {
        // The novel's curve: EXP = 30(N^3 + 5N) - 80. Level 1 needs 100; 10,000 experience
        // from level 1 reaches level 6, as it did for the protagonist after his quest.
        helper.assertTrue(Profile.xpToNext(1) == 100 && Profile.xpToNext(2) == 460 && Profile.xpToNext(5) == 4420, "wrong experience curve");
        ServerPlayer p = mockPlayer(helper);
        QuestLogic.chooseRole(p, PlayMode.ADVENTURER, CombatClass.CHIVALROUS_EXPERT, null);
        Profile prof = Profiles.get(p);
        Profiles.grantXpRaw(p, 10_000);
        helper.assertTrue(prof.getLevel() == 6, "10,000 experience should reach level 6, got " + prof.getLevel());
        // 2 starting points + 2 per level; level 5 gave +1 to every attribute.
        helper.assertTrue(prof.getPoints() == 2 + 2 * 5, "expected 12 points, have " + prof.getPoints());
        for (Stat s : Stat.values()) helper.assertTrue(prof.stat(s) == 6, s + " should be 6 after level 5, is " + prof.stat(s));
        double dmg = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (int i = 0; i < 12; i++) prof.allocate(Stat.FORCE);
        Profiles.applyAttributes(p);
        helper.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > dmg, "Force did not raise damage");
        prof.setPoints(500);
        for (int i = 0; i < 500; i++) prof.allocate(Stat.FORCE);
        helper.assertTrue(prof.stat(Stat.FORCE) == Profile.MAX_STAT, "Force passed the cap of 100: " + prof.stat(Stat.FORCE));
        prof.setInnate(com.realmlord.profile.Innate.BODY, 5);
        Profiles.applyAttributes(p);
        float frail = p.getMaxHealth();
        prof.setInnate(com.realmlord.profile.Innate.BODY, 20);
        Profiles.applyAttributes(p);
        helper.assertTrue(p.getMaxHealth() > frail, "Body Structure did not raise health");
        long xp = prof.getXp();
        prof.deathPenalty();
        helper.assertTrue(prof.getXp() <= xp && prof.getLevel() == 6, "death penalty removed a level");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void oldCharactersAreMigrated(GameTestHelper helper) {
        Profile old = new Profile();
        old.chooseRole(PlayMode.LORD, CombatClass.GENERAL, null, helper.getLevel().getRandom());
        old.setLevel(11);
        old.setPoints(0);
        net.minecraft.nbt.CompoundTag tag = old.save();
        tag.putIntArray("base", new int[]{20, 15, 9, 18});
        tag.remove("ver");
        tag.remove("innate");
        Profile now = new Profile();
        now.load(tag);
        for (Stat s : Stat.values()) helper.assertTrue(now.baseStat(s) == 7, s + " should be 5 + 2 level bonuses, is " + now.baseStat(s));
        helper.assertTrue(now.getPoints() == 2 + 2 * 10, "points not refunded: " + now.getPoints());
        for (var i : com.realmlord.profile.Innate.values()) {
            helper.assertTrue(now.innate(i) >= 5 && now.innate(i) <= 20, i + " not rolled: " + now.innate(i));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void workOccupationsLevelUp(GameTestHelper helper) {
        ServerPlayer p = mockPlayer(helper);
        QuestLogic.chooseRole(p, PlayMode.ADVENTURER, null, WorkOccupation.ARCHITECT);
        Profile prof = Profiles.get(p);
        helper.assertTrue(!prof.hasCharacterLevel(), "work adventurers should have no character level");
        Profiles.grantXp(p, 10_000);
        helper.assertTrue(prof.getLevel() == 1, "work adventurer gained a character level");
        double reach = p.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        Profiles.grantWorkXp(p, 50_000);
        helper.assertTrue(prof.getWorkLevel() >= 7, "work level only " + prof.getWorkLevel());
        helper.assertTrue(WorkOccupation.tier(prof.getWorkLevel()) >= 2, "should be Advanced");
        helper.assertTrue(p.getAttributeValue(ForgeMod.BLOCK_REACH.get()) > reach, "Architect reach did not grow");
        helper.assertTrue(prof.deathPenalty() == 0, "work occupations must not lose experience on death");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everySkillFires(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();
        for (CombatClass c : CombatClass.values()) {
            ServerPlayer p = mockPlayer(helper);
            p.setInvulnerable(true);
            QuestLogic.chooseRole(p, PlayMode.ADVENTURER, c, null);
            Profiles.get(p).setLevel(30);
            for (Skill s : Skill.of(c)) {
                Husk husk = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 3));
                husk.setNoAi(true);
                husk.setTarget(p);
                p.lookAt(EntityAnchorArgument.Anchor.EYES, husk.getEyePosition());
                try {
                    if (!Skills.use(p, s)) failures.add(s.id() + " did not fire");
                    else if (Profiles.get(p).cooldownEnd(s) <= helper.getLevel().getGameTime()) failures.add(s.id() + " set no cooldown");
                } catch (Exception e) {
                    failures.add(s.id() + " threw " + e);
                }
                husk.discard();
                p.teleportTo(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 0)).getX() + 0.5,
                        helper.absolutePos(new BlockPos(2, 1, 0)).getY(), helper.absolutePos(new BlockPos(2, 1, 0)).getZ() + 0.5, 0F, 0F);
                p.setDeltaMovement(0, 0, 0);
            }
            leave(p);
        }
        helper.assertTrue(failures.isEmpty(), "Skill failures: " + failures);
        helper.succeed();
    }

    // ------------------------------------------------------------------ quest and founding

    @GameTest(template = "empty", timeoutTicks = 200, batch = "village_quest")
    public static void villageQuestEarnsTheGoldToken(GameTestHelper helper) {
        ServerPlayer p = lord(helper, CombatClass.GENERAL);
        Profile prof = Profiles.get(p);
        VillageQuest q = prof.quest();
        p.getInventory().add(new ItemStack(Items.OAK_LOG, 64));
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
        p.getInventory().add(new ItemStack(Items.BREAD, 32));
        QuestLogic.submitSupplies(p);
        helper.assertTrue(q.stage == VillageQuest.Stage.BANDITS, "supplies did not advance the quest");
        helper.assertTrue(q.camp != null, "no bandit camp chosen");

        // Build the camp right here instead of 200 blocks away.
        helper.assertTrue(q.inLands, "the camp should lie in the Bandit Lands");
        q.camp = helper.absolutePos(new BlockPos(8, 1, 8));
        q.inLands = false;
        ServerLevel level = helper.getLevel();
        BanditCamp.build(level, p, q);
        helper.assertTrue(q.campBuilt, "camp not built");
        int bandits = QuestLogic.countBandits(level, p, q);
        helper.assertTrue(bandits == 7, "expected 7 bandits, found " + bandits);
        helper.assertTrue(q.captives >= 3, "too few captives");
        helper.assertTrue(level.getBlockState(q.cache).is(Blocks.CHEST), "no cache chest under the campfire");

        for (Mob m : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(q.camp).inflate(40),
                e -> BanditCamp.ownedBy(e, BanditCamp.BANDIT_TAG, p.getUUID()))) {
            m.kill();
        }
        helper.assertTrue(q.chiefKilled, "chief's death was not recorded");
        p.teleportTo(level, q.camp.getX() + 0.5, q.camp.getY() + 1, q.camp.getZ() + 3.5, 0F, 0F);
        QuestLogic.tick(p);
        helper.assertTrue(q.stage == VillageQuest.Stage.CLEARED, "camp not cleared after every bandit died: " + q.stage);

        long freed = level.getEntitiesOfClass(SettlerEntity.class, new net.minecraft.world.phys.AABB(q.camp).inflate(30),
                s -> p.getUUID().equals(s.getOwner()) && !s.isCaptive()).size();
        helper.assertTrue(freed == q.captives, "expected " + q.captives + " freed settlers, found " + freed);

        QuestLogic.openedChest(p, q.cache);
        helper.assertTrue(q.cacheFound, "chief's treasure not recorded");
        TerritoryData data = TerritoryData.get(level.getServer());
        boolean goldBefore = data.goldTokenClaimed();
        QuestLogic.claim(p);
        helper.assertTrue(q.stage == VillageQuest.Stage.CLAIMED, "token not claimed");
        TokenGrade expect = goldBefore ? TokenGrade.SILVER : TokenGrade.GOLD;
        helper.assertTrue(q.grade == expect, "a same-day finish should earn " + expect + ", got " + q.grade);
        helper.assertTrue(p.getInventory().contains(new ItemStack(ModItems.token(expect))), "token not in inventory");
        helper.assertTrue(p.getInventory().countItem(ModItems.SUMMONING_TOKEN.get()) == 1, "no Summoning Token");
        int rank = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.SUMMONING_TOKEN.get())) rank = com.realmlord.item.SummoningTokenItem.rank(s);
        }
        helper.assertTrue(rank == 1, "a perfect run should give an Emperor-rank Summoning Token, got rank " + rank);
        helper.assertTrue(prof.getReputation() >= 250, "no reputation reward");
        helper.assertTrue(p.getInventory().countItem(ModItems.RESOURCES_BAG.get()) == 1, "no Resources Bag");
        helper.assertTrue(p.getInventory().countItem(ModItems.GOLD_COIN.get()) >= QuestLogic.bonus(expect)[2], "gold reward missing");
        helper.assertTrue(data.goldTokenClaimed(), "the world's Gold token should now be taken");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void runningOutOfTimeMakesAnAdventurer(GameTestHelper helper) {
        ServerPlayer p = lord(helper, CombatClass.BAILIFF);
        Profile prof = Profiles.get(p);
        prof.quest().startTick = p.server.overworld().getGameTime() - QuestLogic.failTicks() - 20;
        QuestLogic.tick(p);
        helper.assertTrue(prof.quest().stage == VillageQuest.Stage.FAILED, "quest did not fail");
        helper.assertTrue(prof.getMode() == PlayMode.ADVENTURER, "failed Lord was not made an Adventurer");
        helper.assertTrue(prof.getCombatClass() == CombatClass.BAILIFF, "occupation was lost");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "founding")
    public static void tokenFoundsAVillage(GameTestHelper helper) {
        helper.assertTrue(Founding.foundingMerit(1) == 1100 && Founding.foundingMerit(2) == 1000
                && Founding.foundingMerit(10) == 200 && Founding.foundingMerit(11) == 100 && Founding.foundingMerit(40) == 100,
                "founding merit does not follow 1100, 1000, ... 100");
        ServerPlayer p = lord(helper, CombatClass.GENERAL);
        Profile prof = Profiles.get(p);
        ServerLevel level = helper.getLevel();
        TerritoryData data = TerritoryData.get(level.getServer());
        int before = data.villagesFounded();
        BlockPos spot = helper.absolutePos(new BlockPos(1, 1, 1));
        Territory t = Founding.found(p, spot, TokenGrade.BLACK_IRON, "Testfield Village");
        helper.assertTrue(t != null, "founding failed");
        helper.assertTrue(t.foundedOrder == before + 1, "wrong founding order");
        helper.assertTrue(t.level == TerritoryLevel.VILLAGE_1 && t.grade == TokenGrade.BLACK_IRON, "wrong level or grade");
        helper.assertTrue(prof.getTerritoryId() == t.id, "profile not linked to the territory");
        helper.assertTrue(prof.getMerit() >= Founding.foundingMerit(t.foundedOrder), "founding merit not granted");
        helper.assertTrue(com.realmlord.village.Grounds.active(t), "founding did not set the builders to levelling the grounds");
        helper.assertTrue(level.getBlockState(t.center).is(ModBlocks.LORDS_MANOR.get()), "Lord's Manor not placed");
        helper.assertTrue(t.residents.size() >= 10, "the village should start with the novel's ten refugees, has " + t.residents.size());
        helper.assertTrue(data.at(Level.OVERWORLD, t.center) == t, "territory lookup failed");
        helper.assertTrue(Founding.found(p, spot.offset(500, 0, 0), TokenGrade.BRONZE, null) == null, "a Lord founded two villages");

        ServerPlayer adventurer = mockPlayer(helper);
        QuestLogic.chooseRole(adventurer, PlayMode.ADVENTURER, CombatClass.HEALER, null);
        helper.assertTrue(Founding.found(adventurer, spot.offset(600, 0, 0), TokenGrade.BRONZE, null) == null, "an Adventurer founded a village");

        ServerPlayer neighbour = lord(helper, CombatClass.STRATEGIST);
        helper.assertTrue(Founding.found(neighbour, spot.offset(20, 0, 0), TokenGrade.BRONZE, null) == null, "a village was founded inside another");
        helper.assertTrue(!RealmConfig.lordRestriction(), "the Lord restriction should be off by default");
        leave(p);
        leave(adventurer);
        leave(neighbour);
        helper.succeed();
    }


    // ------------------------------------------------------------------ grading, bandits, loot

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tokenGradeFollowsDaysTaken(GameTestHelper helper) {
        long day = QuestLogic.DAY;
        helper.assertTrue(QuestLogic.failTicks() == 10 * day, "the quest should allow 10 days");
        helper.assertTrue(QuestLogic.grade(0, false) == TokenGrade.GOLD, "instant finish should be Gold");
        helper.assertTrue(QuestLogic.grade(3 * day, false) == TokenGrade.GOLD, "3 days (7 left) should still be Gold");
        helper.assertTrue(QuestLogic.grade(2 * day, true) == TokenGrade.SILVER, "once Gold is taken, fast finishes get Silver");
        helper.assertTrue(QuestLogic.grade(3 * day + 1000, false) == TokenGrade.SILVER, "just over 3 days should be Silver");
        // The user's run: 6 days 6 hours left = 3 days 18 hours taken.
        helper.assertTrue(QuestLogic.grade(3 * day + 18000, false) == TokenGrade.SILVER, "3d18h should be Silver");
        helper.assertTrue(QuestLogic.grade(5 * day, false) == TokenGrade.SILVER, "5 days should be Silver");
        helper.assertTrue(QuestLogic.grade(6 * day, false) == TokenGrade.BLACK_IRON, "6 days should be Black Iron");
        helper.assertTrue(QuestLogic.grade(7 * day + 12000, false) == TokenGrade.BLACK_IRON, "7.5 days should be Black Iron");
        helper.assertTrue(QuestLogic.grade(9 * day, false) == TokenGrade.BRONZE, "9 days should be Bronze");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void questBanditsAreBeatableInIron(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BanditEntity sword = helper.spawn(ModEntities.BANDIT.get(), new BlockPos(1, 1, 1));
        sword.configure(BanditEntity.Role.SWORDSMAN, 1);
        BanditEntity archer = helper.spawn(ModEntities.BANDIT.get(), new BlockPos(2, 1, 1));
        archer.configure(BanditEntity.Role.ARCHER, 1);
        BanditEntity chief = helper.spawn(ModEntities.BANDIT.get(), new BlockPos(3, 1, 1));
        chief.configure(BanditEntity.Role.CHIEF, 1);
        double sd = sword.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double cd = chief.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        helper.assertTrue(sd <= 5.01, "tier 1 swordsman hits too hard: " + sd);
        helper.assertTrue(cd <= 8.01, "tier 1 chief hits too hard: " + cd);
        helper.assertTrue(chief.getMaxHealth() >= 40, "chief too frail");
        helper.assertTrue(archer.getMainHandItem().is(Items.BOW), "archer has no bow");
        // Against full iron armour (15 armour), even on Hard (x1.5), a swordsman's hit stays under 2.5 hearts.
        float hard = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb((float) sd * 1.5F, 15.0F, 0.0F);
        helper.assertTrue(hard <= 4.5F, "swordsman deals " + hard + " through iron armour on Hard");
        float chiefHard = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb((float) cd * 1.5F, 15.0F, 0.0F);
        helper.assertTrue(chiefHard <= 6.0F, "chief deals " + chiefHard + " through iron armour on Hard");
        ServerPlayer p = mockPlayer(helper);
        archer.performRangedAttack(p, 1.0F);
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void banditsDropGoldAndTheChiefHisLedger(GameTestHelper helper) {
        ServerPlayer p = mockPlayer(helper);
        BanditEntity chief = helper.spawn(ModEntities.BANDIT.get(), new BlockPos(2, 1, 2));
        chief.configure(BanditEntity.Role.CHIEF, 1);
        chief.hurt(p.damageSources().playerAttack(p), 1000F);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, chief.getBoundingBox().inflate(4));
        int coins = 0;
        boolean ledger = false, graded = false;
        for (var d : drops) {
            ItemStack s = d.getItem();
            if (s.is(ModItems.SILVER_COIN.get())) coins += s.getCount();
            if (s.is(ModItems.BANDIT_LEDGER.get())) ledger = true;
            if (Gear.gradeOf(s) != null) graded = true;
        }
        helper.assertTrue(coins >= 10, "chief dropped only " + coins + " silver coins");
        helper.assertTrue(ledger, "chief dropped no ledger");
        helper.assertTrue(graded, "chief dropped no graded weapon");
        leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void novelLootWorks(GameTestHelper helper) {
        var r = helper.getLevel().getRandom();
        ItemStack sword = Gear.make(Items.IRON_SWORD, TokenGrade.SILVER, r);
        double plain = 0, graded = 0;
        for (var m : new ItemStack(Items.IRON_SWORD).getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) plain += m.getAmount();
        for (var m : sword.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) graded += m.getAmount();
        helper.assertTrue(graded > plain, "graded sword is no stronger: " + graded + " vs " + plain);
        helper.assertTrue(Gear.gradeOf(sword) == TokenGrade.SILVER, "grade not stored");
        ItemStack bp = Blueprints.of("dining_hall");
        helper.assertTrue(bp.getHoverName().getString().contains("Dining Hall") || bp.getHoverName().getString().contains("dining_hall"),
                "blueprint name wrong: " + bp.getHoverName().getString());

        // The Intermediate Resources Bag fills the territory's stores, as in the novel.
        ServerPlayer p = lord(helper, CombatClass.GENERAL);
        ItemStack bag = ResourcesBagItem.of(1);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bag);
        bag.use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(bag.getCount() == 1, "a bag was used up without a territory");
        Territory t = Founding.found(p, helper.absolutePos(new BlockPos(1, 1, 1)).offset(3000, 0, 3000), TokenGrade.BRONZE, null);
        helper.assertTrue(t != null, "could not found a test village");
        bag.use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(t.food == 5000 && t.wood == 500 && t.stone == 500 && t.ironOre == 500,
                "Intermediate bag should give 5000/500/500/500, gave " + t.food + "/" + t.wood + "/" + t.stone + "/" + t.ironOre);

        // Coins: one gold breaks into 100 silver, and 100 silver combine back into one gold.
        p.getInventory().clearContent();
        ItemStack gold = new ItemStack(ModItems.GOLD_COIN.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gold);
        gold.use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getInventory().countItem(ModItems.SILVER_COIN.get()) == 100, "gold did not break into 100 silver");
        p.setShiftKeyDown(true);
        ItemStack silver = p.getInventory().getItem(p.getInventory().findSlotMatchingItem(new ItemStack(ModItems.SILVER_COIN.get())));
        silver.use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        helper.assertTrue(p.getInventory().countItem(ModItems.GOLD_COIN.get()) == 1 && p.getInventory().countItem(ModItems.SILVER_COIN.get()) == 0,
                "100 silver did not combine into a gold coin");
        leave(p);
        helper.succeed();
    }

    // ------------------------------------------------------------------ plumbing

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void profileSurvivesTravelAndSyncs(GameTestHelper helper) {
        ServerPlayer p = lord(helper, CombatClass.PRIEST);
        Profiles.get(p).setLevel(12);
        ServerLevel nether = p.server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "no Nether");
        p.teleportTo(nether, 0.5, 100, 0.5, 0F, 0F);
        Profile there = ProfileCapability.get(p).orElse(null);
        helper.assertTrue(there != null && there.getLevel() == 12 && there.isLord(), "profile lost in the Nether");
        p.teleportTo(helper.getLevel(), p.getX(), 100, p.getZ(), 0F, 0F);
        helper.assertTrue(ProfileCapability.get(p).map(Profile::getLevel).orElse(0) == 12, "profile lost coming back");

        SyncProfilePacket out = new SyncProfilePacket(p, Profiles.get(p));
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        out.encode(buf);
        new SyncProfilePacket(buf).handle(() -> null);
        helper.assertTrue(ClientProfileData.mode == PlayMode.LORD.ordinal() && ClientProfileData.level == 12
                && ClientProfileData.combat == CombatClass.PRIEST.ordinal(), "sync packet round trip lost data");
        leave(p);
        helper.succeed();
    }
}
