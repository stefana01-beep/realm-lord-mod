package com.realmlord.world;

import com.realmlord.Notify;
import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.Army;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Coins;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Goods;
import com.realmlord.village.Navy;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The county trial. In the novel, when Shanhai Town applied to become a county the system answered:
 * "3 days later there will be a large scale raider attack on Shanhai Town. Please get ready." The
 * raiders came at three gates at once, scouting in the morning and throwing their elites at the
 * weakest gate in the afternoon; a town that held for twelve hours, or beat them, won its upgrade.
 * Bengalore Town and Free Town fell, were plundered, and had to try again.
 *
 * <p>Here: apply for the county upgrade with every other condition met, and three days later
 * barbarian raiders attack in five waves over half a day (a scouting wave from three sides, then
 * heavier ones, the last of elites under their chieftains). Hold the manor for the twelve hours, or
 * kill every raider, and the town becomes a Grade 1 County. If the raiders break in (five of them
 * at the manor at once), they plunder a third of the treasury, food and goods; the town may apply
 * again after seven days. A trial nobody is near is fought unseen: walls, soldiers, sailors,
 * militia and towers against the raiders' numbers.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID)
public final class Trials {
    private Trials() {}

    public static final String TAG = "realmlord_trial";
    public static final int WARNING_DAYS = 3;
    /** Twelve hours of the game's day. */
    public static final long LENGTH = 12_000;
    public static final int WAVES = 5;
    public static final int BREACH = 5;
    public static final long WIN_MERIT = 1_000;
    public static final int RETRY_DAYS = 7;

    public static boolean underway(Territory t) {
        return t.ext.getLong("trialDay") > 0 || t.ext.getLong("trialStart") > 0;
    }

    /** The Lord applies to become a county: the raiders are coming. */
    public static boolean apply(ServerPlayer p, Territory t) {
        long today = GaiaCalendar.today(p.server);
        if (underway(t)) {
            Notify.player(p, Component.translatable("realmlord.trial.underway"));
            return false;
        }
        if (t.ext.getLong("trialRetry") > today) {
            Notify.player(p, Component.translatable("realmlord.trial.retry", t.ext.getLong("trialRetry") - today));
            return false;
        }
        t.ext.putLong("trialDay", today + WARNING_DAYS);
        TerritoryData.get(p.server).touch();
        Notify.player(p, Component.translatable("realmlord.trial.applied", t.name, WARNING_DAYS));
        Village.tell(p.serverLevel(), t, Component.translatable("realmlord.trial.warning", t.name, WARNING_DAYS));
        return true;
    }

    /** Raiders in a wave: more for a bigger town, fewer for one with a temple warding its walls. */
    public static int waveSize(Territory t, int wave) {
        int base = 8 + Math.min(20, t.residents.size() / 400);
        double w = wave == 0 ? 0.7 : wave == WAVES - 1 ? 1.4 : 1.0;
        return (int) Math.max(4, Math.round(base * w * com.realmlord.village.Temples.warding(t)));
    }

    public static int tier(Territory t, int wave) {
        return Math.min(6, 3 + wave / 2);
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        long trial = t.ext.getLong("trialDay");
        if (trial <= 0 || trial > day) return;
        t.ext.remove("trialDay");
        if (level.isLoaded(t.center)) begin(level, t);
        else offScreen(level, t);
    }

    /** The attack begins: the first wave comes at three gates. */
    public static void begin(ServerLevel level, Territory t) {
        t.ext.putLong("trialStart", level.getGameTime());
        t.ext.putInt("trialWave", 0);
        Village.tell(level, t, Component.translatable("realmlord.trial.begins", t.name));
        Notify.world(level.getServer(), Component.translatable("realmlord.trial.begins.world", t.name));
        wave(level, t, 0);
    }

    private static void wave(ServerLevel level, Territory t, int wave) {
        int n = waveSize(t, wave), tier = tier(t, wave);
        // Three sides: north, west and east, as the raiders came at Shanhai's three gates.
        double[] sides = {-Math.PI / 2, Math.PI, 0};
        int made = 0;
        if (wave == WAVES - 1) {
            // The elites, all at one gate, with their chieftains.
            double side = sides[level.getRandom().nextInt(3)];
            made = Raids.spawnBand(level, t, n * 2, tier + 1, side, 2, TAG);
        } else {
            for (double side : sides) made += Raids.spawnBand(level, t, Math.max(2, n / 3), tier, side, 0, TAG);
        }
        t.ext.putInt("trialWave", wave + 1);
        Village.tell(level, t, Component.translatable(wave == WAVES - 1 ? "realmlord.trial.elites" : "realmlord.trial.wave", wave + 1, WAVES, made));
    }

    private static List<Entity> raiders(ServerLevel level, Territory t) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e != null && e.isAlive() && e.getTags().contains(TAG) && e.getPersistentData().getInt(Raids.TERRITORY) == t.id) out.add(e);
        }
        return out;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = event.getServer();
        for (Territory t : TerritoryData.get(server).all()) {
            if (t.ext.getLong("trialStart") <= 0) continue;
            ServerLevel level = server.getLevel(t.dimension);
            if (level == null) continue;
            update(level, t);
        }
    }

    /** Waves on schedule; a breach at the manor; the twelve hours run out; the raiders all dead. */
    public static void update(ServerLevel level, Territory t) {
        long elapsed = level.getGameTime() - t.ext.getLong("trialStart");
        int wave = t.ext.getInt("trialWave");
        if (wave < WAVES && elapsed >= wave * (LENGTH / (WAVES + 1))) wave(level, t, wave);
        List<Entity> left = raiders(level, t);
        int atManor = 0;
        for (Entity e : left) if (e.distanceToSqr(t.center.getX() + 0.5, e.getY(), t.center.getZ() + 0.5) < 100) atManor++;
        if (atManor >= BREACH) {
            fail(level, t, left);
            return;
        }
        if (elapsed >= LENGTH || wave >= WAVES && left.isEmpty() && elapsed > 200) win(level, t, left);
    }

    public static void win(ServerLevel level, Territory t, List<Entity> left) {
        t.ext.remove("trialStart");
        t.ext.remove("trialWave");
        t.ext.putBoolean("trialWon", true);
        for (Entity e : left) e.discard();
        Village.tell(level, t, Component.translatable("realmlord.trial.won", t.name));
        ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (lord != null) {
            Profiles.grantMerit(lord, WIN_MERIT, Component.translatable("realmlord.reason.trial"));
            Village.upgrade(lord, t);
        } else {
            Notify.world(level.getServer(), Component.translatable("realmlord.trial.won.world", t.name));
        }
    }

    public static void fail(ServerLevel level, Territory t, List<Entity> left) {
        t.ext.remove("trialStart");
        t.ext.remove("trialWave");
        t.ext.putLong("trialRetry", GaiaCalendar.today(level.getServer()) + RETRY_DAYS);
        for (Entity e : left) e.discard();
        long coin = t.treasury / 3, food = t.food / 3;
        t.treasury -= coin;
        t.food -= food;
        for (Goods g : Goods.values()) {
            if (g == Goods.GRAIN || g == Goods.WOOD || g == Goods.STONE || g == Goods.IRON_ORE) continue;
            long s = g.stock(t);
            if (s > 0) g.add(t, -s / 3);
        }
        Village.tell(level, t, Component.translatable("realmlord.trial.lost", t.name, Coins.format(coin), food, RETRY_DAYS));
        Notify.world(level.getServer(), Component.translatable("realmlord.trial.lost.world", t.name));
    }

    /** The defenders' strength, for a trial fought unseen. */
    public static int defence(Territory t) {
        int d = Army.strength(t) + Village.militiaCount(t) + Navy.count(t) / 2;
        d += 30 * Village.countComplete(t, BuildingType.CITY_WALL);
        d += 4 * (Village.countComplete(t, BuildingType.ARROW_TOWER) + Village.countComplete(t, BuildingType.SENTRY_POST)
                + Village.countComplete(t, BuildingType.STONE_OUTPOST));
        return d;
    }

    public static int attack(Territory t) {
        int a = 0;
        for (int w = 0; w < WAVES; w++) a += waveSize(t, w) * (w == WAVES - 1 ? 2 : 1) * tier(t, w) / 2;
        return a;
    }

    private static void offScreen(ServerLevel level, Territory t) {
        if (defence(t) * 2 >= attack(t)) win(level, t, List.of());
        else fail(level, t, List.of());
    }

    /** Raiders still about when the server restarts mid-trial carry on; the tick picks them up. */
    public static boolean raiding(BlockPos at, Territory t) {
        return t.ext.getLong("trialStart") > 0;
    }

    static BanditEntity.Faction faction() {
        return BanditEntity.Faction.BARBARIAN;
    }

    static boolean county(Territory t) {
        return t.level.atLeast(TerritoryLevel.COUNTY_1);
    }
}
