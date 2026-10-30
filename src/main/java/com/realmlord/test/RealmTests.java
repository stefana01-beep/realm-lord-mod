package com.realmlord.test;

import com.realmlord.RealmConfig;
import com.realmlord.RealmLord;
import com.realmlord.network.ClientProfileData;
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
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
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

    private static ServerPlayer lord(GameTestHelper helper, CombatClass c) {
        ServerPlayer p = mockPlayer(helper);
        QuestLogic.chooseRole(p, PlayMode.LORD, c, null);
        return p;
    }

    private static void leave(ServerPlayer p) {
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
        helper.assertTrue(prof.stat(Stat.COMMAND) >= 10 && prof.stat(Stat.POLITICS) >= 10, "Lord lacks Command/Politics");
        helper.assertTrue(prof.getPoints() == 10, "Lord should start with 10 free points");
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
    public static void levelsGivePointsAndForceGivesHealth(GameTestHelper helper) {
        ServerPlayer p = mockPlayer(helper);
        QuestLogic.chooseRole(p, PlayMode.ADVENTURER, CombatClass.CHIVALROUS_EXPERT, null);
        Profile prof = Profiles.get(p);
        int points = prof.getPoints();
        Profiles.grantXp(p, 100_000);
        helper.assertTrue(prof.getLevel() > 10, "100k experience gave only level " + prof.getLevel());
        helper.assertTrue(prof.getPoints() >= points + (prof.getLevel() - 1) * Profile.ADVENTURER_POINTS_PER_LEVEL, "level-up points missing");
        float before = p.getMaxHealth();
        for (int i = 0; i < 20; i++) prof.allocate(Stat.FORCE);
        Profiles.applyAttributes(p);
        helper.assertTrue(p.getMaxHealth() > before, "Force did not raise max health");
        long xp = prof.getXp();
        prof.deathPenalty();
        helper.assertTrue(prof.getXp() <= xp && prof.getLevel() > 10, "death penalty removed a level");
        leave(p);
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
        q.camp = helper.absolutePos(new BlockPos(8, 1, 8));
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

        QuestLogic.openedChest(p, q.cache);
        helper.assertTrue(q.cacheFound, "hidden cache not recorded");
        TerritoryData data = TerritoryData.get(level.getServer());
        boolean goldBefore = data.goldTokenClaimed();
        QuestLogic.claim(p);
        helper.assertTrue(q.stage == VillageQuest.Stage.CLAIMED, "token not claimed");
        TokenGrade expect = goldBefore ? TokenGrade.SILVER : TokenGrade.GOLD;
        helper.assertTrue(q.grade == expect, "expected " + expect + " token, got " + q.grade);
        helper.assertTrue(p.getInventory().contains(new ItemStack(ModItems.token(expect))), "token not in inventory");
        helper.assertTrue(data.goldTokenClaimed(), "the world's Gold token should now be taken");

        // A second perfect run can only reach Silver; a slow one gets Bronze.
        VillageQuest other = new VillageQuest();
        other.startTick = 0;
        other.clearTick = 1000;
        other.cacheFound = true;
        helper.assertTrue(QuestLogic.grade(other, true) == TokenGrade.SILVER, "second perfect run should be Silver");
        other.clearTick = (long) (QuestLogic.DAY * 5);
        helper.assertTrue(QuestLogic.grade(other, true) == TokenGrade.BRONZE, "slow run should be Bronze");
        other.clearTick = (long) (QuestLogic.DAY * 2);
        helper.assertTrue(QuestLogic.grade(other, true) == TokenGrade.BLACK_IRON, "two-day run should be Black Iron");
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
        helper.assertTrue(level.getBlockState(t.center).is(ModBlocks.LORDS_MANOR.get()), "Lord's Manor not placed");
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

    // ------------------------------------------------------------------ plumbing

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void profileSurvivesTravelAndSyncs(GameTestHelper helper) {
        ServerPlayer p = lord(helper, CombatClass.ALCHEMIST);
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
                && ClientProfileData.combat == CombatClass.ALCHEMIST.ordinal(), "sync packet round trip lost data");
        leave(p);
        helper.succeed();
    }
}
