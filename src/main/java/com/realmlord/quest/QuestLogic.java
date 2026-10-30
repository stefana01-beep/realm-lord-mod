package com.realmlord.quest;

import com.realmlord.Notify;
import com.realmlord.RealmConfig;
import com.realmlord.network.ModNetwork;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.WorkOccupation;
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
import net.minecraft.world.entity.Entity;
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
    }

    public static void start(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        q.load(new VillageQuest().save());
        q.stage = VillageQuest.Stage.SUPPLIES;
        q.startTick = player.server.overworld().getGameTime();
        Notify.player(player, Component.translatable("realmlord.quest.start",
                VillageQuest.LOGS_NEEDED, VillageQuest.STONE_NEEDED, VillageQuest.FOOD_NEEDED,
                String.format("%.1f", RealmConfig.questFailDays())));
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
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        int dist = 150 + player.getRandom().nextInt(71);
        q.camp = new BlockPos(origin.getX() + (int) (Math.cos(angle) * dist), 64, origin.getZ() + (int) (Math.sin(angle) * dist));
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
                if (left != q.banditsLeft) {
                    q.banditsLeft = left;
                    p.markDirty();
                }
                if (left == 0) clear(player);
            }
        }
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
        q.banditsLeft = 0;
        if (q.camp != null) {
            ServerLevel level = player.server.overworld();
            AABB box = new AABB(q.camp).inflate(48, 32, 48);
            for (Entity e : level.getEntitiesOfClass(Mob.class, box, e -> BanditCamp.ownedBy(e, BanditCamp.CAPTIVE_TAG, player.getUUID()))) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
                e.discard();
            }
        }
        p.markDirty();
        Notify.player(player, Component.translatable("realmlord.quest.cleared", q.captives, q.captives * 5));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
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
    }

    // ------------------------------------------------------------------ grading and the token

    public static TokenGrade grade(VillageQuest q, boolean goldClaimed) {
        double days = (q.clearTick - q.startTick) / (double) DAY;
        if (days <= RealmConfig.questSilverDays() && q.deaths == 0 && q.cacheFound) {
            return goldClaimed ? TokenGrade.SILVER : TokenGrade.GOLD;
        }
        if (days <= RealmConfig.questBlackIronDays()) return TokenGrade.BLACK_IRON;
        return TokenGrade.BRONZE;
    }

    public static void claim(ServerPlayer player) {
        Profile p = Profiles.get(player);
        VillageQuest q = p.quest();
        if (q.stage != VillageQuest.Stage.CLEARED) return;
        TerritoryData data = TerritoryData.get(player.server);
        TokenGrade g = grade(q, data.goldTokenClaimed());
        if (g == TokenGrade.GOLD) {
            data.claimGoldToken(player.getGameProfile().getName());
            Notify.world(player.server, Component.translatable("realmlord.quest.gold_world", player.getDisplayName()));
        }
        q.grade = g;
        q.stage = VillageQuest.Stage.CLAIMED;
        p.markDirty();
        ItemStack token = new ItemStack(ModItems.token(g));
        if (!player.getInventory().add(token)) player.drop(token, false);
        Notify.player(player, Component.translatable("realmlord.quest.claimed", Component.translatable(g.key())));
        Profiles.grantMerit(player, g.questMerit, Component.translatable("realmlord.reason.quest"));
        Profiles.grantXp(player, 300);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
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
