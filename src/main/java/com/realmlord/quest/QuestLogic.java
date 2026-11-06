package com.realmlord.quest;

import com.realmlord.Notify;
import com.realmlord.RealmConfig;
import com.realmlord.network.ModNetwork;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.item.ResourcesBagItem;
import com.realmlord.item.SummoningTokenItem;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TokenGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** The Village Creation quest from start to token. */
public final class QuestLogic {
    private QuestLogic() {}

    public static final long DAY = 24000L;

    public static long failTicks() {
        return Math.round(RealmConfig.questFailDays() * DAY);
    }

    // ------------------------------------------------------------------ choosing a role

    public static void chooseRole(ServerPlayer player, PlayMode mode, CombatClass combat, WorkOccupation work) {
        Profile p = Profiles.get(player);
        if (p.hasChosen()) return;
        p.chooseRole(mode, combat, work, player.getRandom());
        Component occ = Component.translatable(combat != null ? combat.key() : work.key());
        if (mode == PlayMode.LORD) {
            Notify.player(player, Component.translatable("realmlord.msg.chose_lord", occ));
            start(player);
        } else {
            Notify.player(player, Component.translatable("realmlord.msg.chose_adventurer", occ));
        }
        Profiles.applyAttributes(player);
        Profiles.refreshName(player);
        ModNetwork.sync(player);
        // Then where in the world to begin.
        if (!p.isPlaced()) com.realmlord.territory.SpawnPicker.offer(player);
    }

    public static void start(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (ownsVillage(player)) {
            Notify.player(player, "realmlord.quest.not_needed");
            return;
        }
        q.load(new VillageQuest().save());
        q.stage = VillageQuest.Stage.SUPPLIES;
        q.startTick = player.server.overworld().getGameTime();
        Notify.player(player, Component.translatable("realmlord.quest.start",
                VillageQuest.LOGS_NEEDED, VillageQuest.STONE_NEEDED, VillageQuest.FOOD_NEEDED,
                fmt(RealmConfig.questFailDays()), fmt(RealmConfig.questGoldDays())));
        p.markDirty();
    }

    // ------------------------------------------------------------------ stage 1: supplies

    public static void submitSupplies(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.stage != VillageQuest.Stage.SUPPLIES) return;
        Inventory inv = player.getInventory();
        int gotLogs = 0, gotStone = 0, gotFood = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            int take = 0;
            if (s.is(ItemTags.LOGS) && q.logs < VillageQuest.LOGS_NEEDED) {
                take = Math.min(s.getCount(), VillageQuest.LOGS_NEEDED - q.logs);
                q.logs += take;
                gotLogs += take;
            } else if (s.is(ItemTags.STONE_CRAFTING_MATERIALS) && q.stone < VillageQuest.STONE_NEEDED) {
                take = Math.min(s.getCount(), VillageQuest.STONE_NEEDED - q.stone);
                q.stone += take;
                gotStone += take;
            } else if (s.isEdible() && q.food < VillageQuest.FOOD_NEEDED) {
                take = Math.min(s.getCount(), VillageQuest.FOOD_NEEDED - q.food);
                q.food += take;
                gotFood += take;
            }
            if (take > 0) s.shrink(take);
        }
        p.markDirty();
        if (gotLogs + gotStone + gotFood == 0) {
            Notify.player(player, Component.translatable("realmlord.quest.nothing",
                    q.logs, VillageQuest.LOGS_NEEDED, q.stone, VillageQuest.STONE_NEEDED, q.food, VillageQuest.FOOD_NEEDED));
            return;
        }
        Notify.player(player, Component.translatable("realmlord.quest.supplied",
                q.logs, VillageQuest.LOGS_NEEDED, q.stone, VillageQuest.STONE_NEEDED, q.food, VillageQuest.FOOD_NEEDED));
        if (q.suppliesComplete()) beginBandits(player);
    }

    // ------------------------------------------------------------------ stage 2: the bandit camp

    public static void beginBandits(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        q.stage = VillageQuest.Stage.BANDITS;
        ServerLevel level = player.server.overworld();
        BlockPos origin = player.level().dimension().equals(Level.OVERWORLD) ? player.blockPosition() : level.getSharedSpawnPos();
        // Somewhere dry: never on the sea, a lake, a river or a swamp.
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        int dist = 150 + player.getRandom().nextInt(71);
        q.camp = new BlockPos(origin.getX() + (int) (Math.cos(angle) * dist), 64, origin.getZ() + (int) (Math.sin(angle) * dist));
        for (int i = 0; i < 64; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            int d = 150 + player.getRandom().nextInt(71) + (i / 16) * 60;
            int x = origin.getX() + (int) (Math.cos(a) * d), z = origin.getZ() + (int) (Math.sin(a) * d);
            if (BanditCamp.goodSite(level, x, z)) {
                q.camp = new BlockPos(x, 64, z);
                dist = d;
                break;
            }
        }
        q.campBuilt = false;
        p.markDirty();
        Notify.player(player, Component.translatable("realmlord.quest.bandits",
                q.camp.getX(), q.camp.getZ(), Component.translatable(directionKey(origin, q.camp)), dist));
        player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.5F, 1.0F);
    }

    public static String directionKey(BlockPos from, BlockPos to) {
        double a = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
        // atan2 with +x = east, +z = south.
        int idx = Math.floorMod((int) Math.round(a / 45.0), 8);
        String[] dirs = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        return "realmlord.dir." + dirs[idx];
    }

    /** Called every second for players with an active quest. */
    public static void tick(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (!q.active() || !p.isLord()) return;
        // A Lord who already rules a village has no need of the Village Creation quest.
        if (ownsVillage(player)) {
            q.stage = VillageQuest.Stage.CLAIMED;
            p.markDirty();
            glow(player, new int[0]);
            Notify.player(player, "realmlord.quest.not_needed");
            ModNetwork.sync(player);
            return;
        }
        long now = player.server.overworld().getGameTime();
        long elapsed = now - q.startTick;
        if (q.stage != VillageQuest.Stage.CLEARED) {
            if (elapsed > failTicks()) {
                fail(player, "realmlord.quest.failed_time");
                return;
            }
            if (!q.warned && failTicks() - elapsed < DAY) {
                q.warned = true;
                Notify.player(player, "realmlord.quest.warning");
                p.markDirty();
            }
        }
        if (q.stage == VillageQuest.Stage.BANDITS && q.camp != null && player.level().dimension().equals(Level.OVERWORLD)) {
            ServerLevel level = player.serverLevel();
            double dx = player.getX() - q.camp.getX(), dz = player.getZ() - q.camp.getZ();
            double d2 = dx * dx + dz * dz;
            if (!q.campBuilt && d2 < 64 * 64 && level.hasChunkAt(q.camp)) {
                BanditCamp.build(level, player, q);
                p.markDirty();
                Notify.player(player, "realmlord.quest.camp_found");
            } else if (q.campBuilt && d2 < 80 * 80) {
                int left = countBandits(level, player, q);
                // The Lord (and only the Lord) sees the band outlined through jungle and hill.
                AABB box = new AABB(q.camp).inflate(128, 96, 128);
                glow(player, level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && BanditCamp.ownedBy(e, BanditCamp.BANDIT_TAG, player.getUUID()))
                        .stream().mapToInt(Mob::getId).toArray());
                if (left != q.banditsLeft) {
                    q.banditsLeft = left;
                    p.markDirty();
                }
                if (left == 0) clear(player);
            }
        }
    }

    private static void glow(ServerPlayer player, int[] ids) {
        ModNetwork.send(player, new com.realmlord.network.BanditGlowPacket(ids));
    }

    /** True if this player already rules a territory. */
    public static boolean ownsVillage(ServerPlayer player) {
        int id = Profiles.get(player).getTerritoryId();
        return id >= 0 && com.realmlord.territory.TerritoryData.get(player.server).byId(id) != null;
    }

    public static int countBandits(ServerLevel level, ServerPlayer player, VillageQuest q) {
        AABB box = new AABB(q.camp).inflate(128, 96, 128);
        List<Mob> list = level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && BanditCamp.ownedBy(e, BanditCamp.BANDIT_TAG, player.getUUID()));
        return list.size();
    }

    /** The camp is cleared: the captives go free and the token can be claimed. */
    public static void clear(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.stage != VillageQuest.Stage.BANDITS) return;
        q.stage = VillageQuest.Stage.CLEARED;
        q.clearTick = player.server.overworld().getGameTime();
        glow(player, new int[0]);
        q.banditsLeft = 0;
        if (q.camp != null) {
            ServerLevel level = player.server.overworld();
            BanditCamp.openPen(level, q.camp);
            AABB box = new AABB(q.camp).inflate(48, 32, 48);
            for (SettlerEntity s : level.getEntitiesOfClass(SettlerEntity.class, box, e -> BanditCamp.ownedBy(e, BanditCamp.CAPTIVE_TAG, player.getUUID()))) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1, s.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
                s.free(q.camp);
            }
        }
        p.markDirty();
        Notify.player(player, Component.translatable("realmlord.quest.cleared", q.captives, q.captives * 5));
        spoils(player, q);
        offerCamp(player);
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    /** Grain, timber, stone and iron ore the bandits had taken, kept for the village's stores. */
    public static final long[] SPOILS = {400, 200, 150, 30};
    /** Gold coins in the bandit chief's strongbox. */
    public static final int SPOIL_GOLD = 5;

    /**
     * The bandits' hoard: two village blueprints and the chief's gold straight to the Lord, and
     * the supplies they had stolen set aside for the stores of the village about to be founded.
     */
    public static void spoils(ServerPlayer player, VillageQuest q) {
        java.util.List<String> pool = new java.util.ArrayList<>(com.realmlord.loot.Blueprints.VILLAGE_1);
        java.util.Collections.shuffle(pool, new java.util.Random(player.getRandom().nextLong()));
        for (int i = 0; i < Math.min(2, pool.size()); i++) give(player, com.realmlord.loot.Blueprints.of(pool.get(i)));
        com.realmlord.village.Coins.give(player, SPOIL_GOLD * 10_000L);
        com.realmlord.village.Goods[] basic = {com.realmlord.village.Goods.GRAIN, com.realmlord.village.Goods.WOOD,
                com.realmlord.village.Goods.STONE, com.realmlord.village.Goods.IRON_ORE};
        for (int i = 0; i < basic.length; i++) q.spoils[basic[i].ordinal()] += SPOILS[i];
        Notify.player(player, Component.translatable("realmlord.quest.spoils", Math.min(2, pool.size()), SPOIL_GOLD,
                SPOILS[0], SPOILS[1], SPOILS[2], SPOILS[3]).withStyle(ChatFormatting.GOLD));
    }

    /** Moves the camp's spoils into a village's stores. Returns true if there were any. */
    public static boolean deliverSpoils(VillageQuest q, com.realmlord.territory.Territory t) {
        if (!q.hasSpoils()) return false;
        for (com.realmlord.village.Goods g : com.realmlord.village.Goods.values()) {
            g.add(t, q.spoils[g.ordinal()]);
            q.spoils[g.ordinal()] = 0;
        }
        return true;
    }

    /** Asks the Lord what to do with the cleared camp, with buttons in the chat. */
    public static void offerCamp(ServerPlayer player) {
        Component keep = Component.translatable("realmlord.camp.keep_button").withStyle(s -> s.withColor(ChatFormatting.GREEN)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/realm camp keep"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("realmlord.camp.keep_tip"))));
        Component salvage = Component.translatable("realmlord.camp.salvage_button").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/realm camp salvage"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("realmlord.camp.salvage_tip"))));
        player.sendSystemMessage(Component.translatable("realmlord.camp.choose", keep, salvage));
    }

    /** Keeps the camp standing, or takes it apart and gives the Lord every material in it. */
    public static boolean decideCamp(ServerPlayer player, boolean salvage) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (!q.campBuilt || q.camp == null || q.campChoice != 0
                || (q.stage != VillageQuest.Stage.CLEARED && q.stage != VillageQuest.Stage.CLAIMED)) {
            Notify.player(player, "realmlord.camp.not_now");
            return false;
        }
        q.campChoice = salvage ? 2 : 1;
        p.markDirty();
        if (!salvage) {
            Notify.player(player, "realmlord.camp.kept");
        } else {
            List<ItemStack> got = BanditCamp.salvage(player.server.overworld(), q.camp);
            // Timber, stone and the like go to the village's stores (now, or once it is founded); the rest to the Lord.
            com.realmlord.territory.Territory home = com.realmlord.territory.TerritoryData.get(player.server).ownedBy(player.getUUID());
            int n = 0, stored = 0;
            for (ItemStack s : got) {
                com.realmlord.village.Goods.Unit u = com.realmlord.village.Goods.of(s);
                if (u != null) {
                    long units = (long) u.units() * s.getCount();
                    if (home != null) u.good().add(home, units);
                    else q.spoils[u.good().ordinal()] += units;
                    stored += s.getCount();
                    continue;
                }
                n += s.getCount();
                give(player, s);
            }
            if (home != null) com.realmlord.territory.TerritoryData.get(player.server).touch();
            Notify.player(player, Component.translatable("realmlord.camp.salvaged", n));
            if (stored > 0) Notify.player(player, Component.translatable(home != null ? "realmlord.camp.salvaged_stores" : "realmlord.camp.salvaged_spoils", stored));
            player.playNotifySound(SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        ModNetwork.sync(player);
        return true;
    }

    // ------------------------------------------------------------------ the hidden cache

    public static void openedChest(ServerPlayer player, BlockPos pos) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.cache == null || q.cacheFound || !q.cache.equals(pos)) return;
        if (q.stage != VillageQuest.Stage.BANDITS && q.stage != VillageQuest.Stage.CLEARED) return;
        q.cacheFound = true;
        p.markDirty();
        Notify.player(player, Component.translatable("realmlord.quest.cache").withStyle(ChatFormatting.LIGHT_PURPLE));
        Profiles.grantMerit(player, 50, Component.translatable("realmlord.reason.treasure"));
    }

    // ------------------------------------------------------------------ grading and the token

    /**
     * The token a quest earns, by how long it took: Gold within 3 days (the world's only one;
     * later fast finishes get Silver), Silver within 5, Black Iron within 7.5, otherwise Bronze.
     */
    public static TokenGrade grade(long elapsedTicks, boolean goldClaimed) {
        double days = elapsedTicks / (double) DAY;
        if (days <= RealmConfig.questGoldDays()) return goldClaimed ? TokenGrade.SILVER : TokenGrade.GOLD;
        if (days <= RealmConfig.questSilverDays()) return TokenGrade.SILVER;
        if (days <= RealmConfig.questBlackIronDays()) return TokenGrade.BLACK_IRON;
        return TokenGrade.BRONZE;
    }

    /** The grade the quest would earn if finished now (or the grade it did earn). */
    public static TokenGrade prospect(VillageQuest q, long now, boolean goldClaimed) {
        if (q.grade != null) return q.grade;
        long end = q.stage == VillageQuest.Stage.CLEARED ? q.clearTick : now;
        return grade(end - q.startTick, goldClaimed);
    }

    public static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.1f", d);
    }

    /**
     * The novel's village creation set scales with the result. A perfect run (Gold, or Silver
     * once the Gold token is gone) earns an Emperor-rank Summoning Token and an Intermediate
     * Resources Bag; otherwise a King-rank token and a Basic bag.
     */
    public static boolean perfect(TokenGrade g) {
        return g == TokenGrade.GOLD || g == TokenGrade.SILVER;
    }

    /**
     * Completion bonus: the novel's perfect run paid 10,000 experience, 500 reputation and
     * 100 gold coins; slower runs earn less. Returns {experience, reputation, gold, silver}.
     */
    public static long[] bonus(TokenGrade g) {
        return switch (g) {
            case GOLD -> new long[]{10_000, 500, 100, 0};
            case SILVER -> new long[]{5_000, 250, 50, 0};
            case BLACK_IRON -> new long[]{2_000, 100, 10, 0};
            case BRONZE -> new long[]{1_000, 50, 0, 50};
        };
    }

    public static void claim(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.stage != VillageQuest.Stage.CLEARED) return;
        TerritoryData data = TerritoryData.get(player.server);
        TokenGrade g = grade(q.clearTick - q.startTick, data.goldTokenClaimed());
        if (g == TokenGrade.GOLD) {
            data.claimGoldToken(player.getGameProfile().getName());
            Notify.world(player.server, Component.translatable("realmlord.quest.gold_world", player.getDisplayName()));
        }
        q.grade = g;
        q.stage = VillageQuest.Stage.CLAIMED;
        p.markDirty();
        // The novel's village creation set: Village Creation Token, Summoning Token and Resources Bag.
        int rank = perfect(g) ? 1 : 0;
        int bag = perfect(g) ? 1 : 0;
        give(player, new ItemStack(ModItems.token(g)));
        give(player, SummoningTokenItem.of(rank));
        give(player, ResourcesBagItem.of(bag));
        Notify.player(player, Component.translatable("realmlord.quest.claimed", Component.translatable(g.key()),
                Component.translatable("realmlord.rank." + SummoningTokenItem.RANKS[rank]),
                Component.translatable("realmlord.bag.tier." + ResourcesBagItem.TIERS[bag])));
        long[] b = bonus(g);
        ResourcesBagItem.give(player, new ItemStack(ModItems.GOLD_COIN.get()), (int) b[2]);
        ResourcesBagItem.give(player, new ItemStack(ModItems.SILVER_COIN.get()), (int) b[3]);
        Notify.player(player, Component.translatable("realmlord.quest.bonus", b[0], b[1],
                b[2] > 0 ? Component.translatable("realmlord.coins.gold", b[2]) : Component.translatable("realmlord.coins.silver", b[3])));
        Profiles.grantMerit(player, g.questMerit, Component.translatable("realmlord.reason.quest"));
        Profiles.get(player).addReputation(b[1]);
        Profiles.grantXpRaw(player, b[0]);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // ------------------------------------------------------------------ failure

    public static void abandon(ServerPlayer player) {
        Profile p = Profiles.get(player);
        if (!p.quest().active() || p.quest().stage == VillageQuest.Stage.CLEARED) return;
        fail(player, "realmlord.quest.failed_abandon");
    }

    public static void fail(ServerPlayer player, String reasonKey) {
        Profile p = Profiles.get(player);
        p.quest().stage = VillageQuest.Stage.FAILED;
        p.demoteToAdventurer();
        Notify.player(player, reasonKey);
        Notify.player(player, "realmlord.quest.failed_now_adventurer");
        Profiles.refreshName(player);
        ModNetwork.sync(player);
    }

    public static void playerDied(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.stage == VillageQuest.Stage.SUPPLIES || q.stage == VillageQuest.Stage.BANDITS) {
            q.deaths++;
            p.markDirty();
        }
    }

    public static int clampDays(long ticks) {
        return Mth.floor(ticks / (double) DAY);
    }
}
