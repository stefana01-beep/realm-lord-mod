package com.realmlord.world;

import com.realmlord.Notify;
import com.realmlord.entity.beast.Beasts;
import com.realmlord.entity.beast.BoarKingEntity;
import com.realmlord.entity.beast.WildBoarEntity;
import com.realmlord.registry.ModEntities;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Beast dens, the first dungeons of the novel's wilds, and the only places beast kings are
 * found: a Boar King and his sounder in a burrow in the hills; out in the snow and the northern
 * forests (600 blocks or more from the world's spawn), a Wolf King and his pack; and deep in the
 * jungle or the mountains (1,500 blocks or more), the White Tiger King with his leopards. They
 * appear in the wilds around players, one in each stretch of country, never near the world's
 * spawn; the king comes back some days after he is slain.
 */
public final class Dens {
    private Dens() {}

    /** No two dens closer than this. */
    public static final int SPACING = 450;
    public static final int RESPAWN_DAYS = 6;
    public static final int DISCOVER = 40;
    /** No den this close to the world's spawn, where new players begin. */
    public static final int CLEAR_OF_SPAWN = 250;
    public static final int BOAR = 0, WOLF = 1, TIGER = 2;

    public static boolean isKing(net.minecraft.world.entity.Entity e) {
        return e instanceof BoarKingEntity || e instanceof com.realmlord.entity.beast.WolfKingEntity
                || e instanceof com.realmlord.entity.beast.TigerKingEntity;
    }

    /** Which king keeps a lair here: tigers deep in jungle and mountains, wolves in the cold north, boars elsewhere. */
    public static int kindAt(ServerLevel level, BlockPos p) {
        BlockPos spawn = level.getSharedSpawnPos();
        double d = Math.sqrt(Math.pow(p.getX() - spawn.getX(), 2) + Math.pow(p.getZ() - spawn.getZ(), 2));
        var biome = level.getBiome(p);
        if (d >= 1500 && (biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE) || biome.is(net.minecraft.tags.BiomeTags.IS_MOUNTAIN))) return TIGER;
        if (d >= 600 && (biome.is(net.minecraftforge.common.Tags.Biomes.IS_SNOWY) || biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)
                || biome.is(net.minecraft.tags.BiomeTags.IS_MOUNTAIN))) return WOLF;
        return BOAR;
    }

    public static Component kingName(int kind) {
        return Component.translatable(kind == TIGER ? "entity.realmlord.tiger_king" : kind == WOLF ? "entity.realmlord.wolf_king" : "entity.realmlord.boar_king");
    }

    /** Every minute for each player: find the dens nearby, bring their kings back, and now and then raise a new one. */
    public static void tick(ServerPlayer sp) {
        if (sp.tickCount % 1200 != 300 || sp.isSpectator()) return;
        check(sp, true);
    }

    /** Finds and revives the dens near a player, and (if allowed) may raise a new one. */
    public static void check(ServerPlayer sp, boolean mayCreate) {
        ServerLevel level = sp.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        WorldState w = WorldState.get(sp.server);
        long today = GaiaCalendar.today(sp.server);
        boolean near = false;
        for (WorldState.Den d : w.dens) {
            double dist = Math.sqrt(sp.blockPosition().distSqr(d.pos));
            if (dist < SPACING) near = true;
            if (dist < DISCOVER && d.discoveredBy.add(sp.getUUID())) {
                Notify.player(sp, Component.translatable("realmlord.den.found", d.pos.getX(), d.pos.getZ(), kingName(d.kind)));
                w.touch();
            }
            if (d.cleared && today >= d.respawnDay && dist < 128 && level.isLoaded(d.pos)) {
                populate(level, d);
                w.touch();
            }
        }
        RandomSource r = sp.getRandom();
        if (!mayCreate || near || r.nextInt(5) != 0) return;
        BlockPos spot = findSpot(level, sp.blockPosition(), r);
        if (spot == null) return;
        WorldState.Den d = create(level, spot);
        w.dens.add(d);
        w.touch();
    }

    /** Open, dry ground 120 to 220 blocks away, clear of every territory. */
    private static BlockPos findSpot(ServerLevel level, BlockPos from, RandomSource r) {
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 120 + r.nextInt(100);
            int x = from.getX() + (int) (Math.cos(a) * d), z = from.getZ() + (int) (Math.sin(a) * d);
            BlockPos p = new BlockPos(x, 64, z);
            if (!level.isLoaded(p)) continue;
            BlockPos spawn = level.getSharedSpawnPos();
            if (Math.pow(x - spawn.getX(), 2) + Math.pow(z - spawn.getZ(), 2) < CLEAR_OF_SPAWN * CLEAR_OF_SPAWN) continue;
            boolean clear = true;
            for (Territory t : TerritoryData.get(level.getServer()).all()) {
                if (!t.dimension.equals(level.dimension())) continue;
                double reach = t.radius() + 64;
                if (t.center.distSqr(p) < reach * reach) clear = false;
            }
            if (!clear) continue;
            int y = Village.groundAt(level, x, z);
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            if (!below.getFluidState().isEmpty() || below.is(Blocks.SAND) || below.is(Blocks.ICE)) continue;
            return new BlockPos(x, y, z);
        }
        return null;
    }

    /** Digs the den (a burrow under a mound of earth and roots) and fills it. */
    public static WorldState.Den create(ServerLevel level, BlockPos c) {
        return create(level, c, -1);
    }

    /** As above, with the kind of king chosen (or -1 for whichever keeps a lair in this country). */
    public static WorldState.Den create(ServerLevel level, BlockPos c, int kind) {
        RandomSource r = level.getRandom();
        int R = 6;
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R) continue;
                int h = (int) Math.round(4.5 * (1 - (d / R) * (d / R)));
                BlockPos base = c.offset(dx, 0, dz);
                for (int y = -1; y <= h; y++) {
                    boolean inside = d <= R - 2.2 && y >= 0 && y < h - 0.5;
                    boolean door = dz >= 0 && Math.abs(dx) <= 1 && y >= 0 && y <= 1;
                    BlockState s;
                    if (inside || door) s = Blocks.AIR.defaultBlockState();
                    else if (y == -1) s = Blocks.PODZOL.defaultBlockState();
                    else s = switch (r.nextInt(6)) {
                        case 0 -> Blocks.ROOTED_DIRT.defaultBlockState();
                        case 1 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                        case 2 -> Blocks.MUD.defaultBlockState();
                        default -> Blocks.COARSE_DIRT.defaultBlockState();
                    };
                    level.setBlock(base.above(y), s, Block.UPDATE_CLIENTS);
                }
                if (d <= R - 2.2) {
                    if (r.nextInt(7) == 0) level.setBlock(base, Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    else if (r.nextInt(9) == 0) level.setBlock(base, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        level.setBlock(c, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        WorldState.Den den = new WorldState.Den();
        den.pos = c;
        den.kind = kind >= 0 ? kind : kindAt(level, c);
        populate(level, den);
        return den;
    }

    /** The king and four to six of his following: boars, grey wolves, or leopards. */
    public static void populate(ServerLevel level, WorldState.Den den) {
        RandomSource r = level.getRandom();
        net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> kingType = den.kind == TIGER ? ModEntities.TIGER_KING.get()
                : den.kind == WOLF ? ModEntities.WOLF_KING.get() : ModEntities.BOAR_KING.get();
        net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> followerType = den.kind == TIGER ? ModEntities.LEOPARD.get()
                : den.kind == WOLF ? ModEntities.GREY_WOLF.get() : ModEntities.WILD_BOAR.get();
        net.minecraft.world.entity.Mob king = kingType.create(level);
        if (king != null) {
            king.moveTo(den.pos.getX() + 0.5, den.pos.getY(), den.pos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
            king.finalizeSpawn(level, level.getCurrentDifficultyAt(den.pos), MobSpawnType.STRUCTURE, null, null);
            king.addTag(Beasts.DEN_TAG);
            king.setPersistenceRequired();
            level.addFreshEntity(king);
            den.king = king.getUUID();
        }
        int n = (den.kind == TIGER ? 2 : 4) + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            net.minecraft.world.entity.Mob b = followerType.create(level);
            if (b == null) continue;
            double a = r.nextDouble() * Math.PI * 2;
            int x = den.pos.getX() + (int) Math.round(Math.cos(a) * (6 + r.nextInt(4)));
            int z = den.pos.getZ() + (int) Math.round(Math.sin(a) * (6 + r.nextInt(4)));
            int y = Village.groundAt(level, x, z);
            b.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360F, 0F);
            b.finalizeSpawn(level, level.getCurrentDifficultyAt(den.pos), MobSpawnType.STRUCTURE, null, null);
            b.addTag(Beasts.DEN_TAG);
            b.setPersistenceRequired();
            level.addFreshEntity(b);
        }
        den.cleared = false;
    }

    /** A den's king is slain: the den is cleared until he returns. */
    public static void kingSlain(ServerLevel level, net.minecraft.world.entity.LivingEntity king, ServerPlayer by) {
        WorldState w = WorldState.get(level.getServer());
        for (WorldState.Den d : w.dens) {
            if (!king.getUUID().equals(d.king)) continue;
            d.cleared = true;
            d.king = null;
            d.respawnDay = GaiaCalendar.today(level.getServer()) + RESPAWN_DAYS;
            w.touch();
            if (by != null) {
                Notify.player(by, Component.translatable("realmlord.den.cleared", RESPAWN_DAYS, kingName(d.kind)));
                com.realmlord.profile.Profiles.grantMerit(by, d.kind == TIGER ? 400 : d.kind == WOLF ? 200 : 100,
                        Component.translatable("realmlord.reason.den_king", kingName(d.kind)));
            }
        }
    }
}
