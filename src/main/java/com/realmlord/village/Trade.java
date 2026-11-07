package com.realmlord.village;

import com.realmlord.registry.ModCrafts;
import com.realmlord.territory.Territory;
import com.realmlord.worldgen.ChinaMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Chambers of Commerce and their caravans. In the novel the Song and Chui Chambers of Commerce
 * carried Shanhai's salt, wine and pearls out to the other territories and the system cities,
 * by road and by sea in their own merchant ships, paying the system's 10% tax on the way.
 *
 * <p>A Chamber of Commerce, run by a merchant, sends a caravan to the capital chosen in its window
 * (one of the nine on the map of China). The caravan loads up to half of each trade good in the
 * storehouse (salt, wine, pottery, linen, silk, tools, herbs, leather, copper, coal), travels
 * there and back in days that grow with the distance, and sells for more the farther it goes:
 * the market price, plus a fifth, plus up to 120% for the road; a better merchant and merchants
 * among the summoned figures add more. The proceeds, less the 10% tax, go to the treasury.
 *
 * <p>The roads are dangerous: a caravan may be robbed (15%), less with inns and relay stations
 * built in the village to rest at (each -3%), militia to escort it, and strategists to plan the
 * route. A village with a dock and a merchant ship sends a coastal capital's caravan by sea:
 * faster, three times the cargo, robbed only by water bandits, whom Mengchong warships see off.
 */
public final class Trade {
    private Trade() {}

    public static final Goods[] CARGO = {Goods.FINE_SILK, Goods.LINEN, Goods.WINE, Goods.TOOLS, Goods.POTTERY, Goods.SALT, Goods.HERBS,
            Goods.LEATHER, Goods.COPPER, Goods.COAL};
    /** The capitals ships can reach: Jingdu (by the Bohai), Jianye (up the Yangtze), Quanzhou (on the coast). */
    public static final boolean[] BY_SEA = {true, false, false, false, false, false, false, true, true};
    public static final double TAX = 0.10;

    public static boolean chamber(Building b) {
        return b.type == BuildingType.CHAMBER_OF_COMMERCE && b.complete && !b.demolishing;
    }

    /** The capital a chamber trades with (its mode). */
    public static int destination(Building b) {
        return Math.max(0, Math.min(ChinaMap.CAPITALS.length - 1, b.mode));
    }

    public static double distance(Territory t, int capital) {
        try {
            int[] at = ChinaMap.blockOf(ChinaMap.CAPITAL_LONLAT[capital][0], ChinaMap.CAPITAL_LONLAT[capital][1]);
            double dx = at[0] - t.center.getX(), dz = at[1] - t.center.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        } catch (RuntimeException e) {
            return 5000;
        }
    }

    public static boolean bySea(Territory t, int capital) {
        return BY_SEA[capital] && Navy.hasDock(t) && Navy.ships(t, ModCrafts.MERCHANT_SHIP.get()) > 0;
    }

    /** Days there and back: two, plus one for every 4,000 blocks; by sea 40% less. */
    public static int days(Territory t, int capital) {
        double d = 2 + distance(t, capital) / 4000.0;
        if (bySea(t, capital)) d *= 0.6;
        return (int) Math.max(2, Math.min(16, Math.round(d)));
    }

    /** The price rise at the far end: a fifth, plus up to 120% for distance. */
    public static double markup(Territory t, int capital) {
        return 1.2 + Math.min(1.2, distance(t, capital) / 20000.0);
    }

    public static int inns(Territory t) {
        int n = 0;
        for (Building b : t.buildings) {
            if (b.complete && (b.designRef.endsWith(":road_inn") || b.designRef.endsWith(":relay_station"))) n++;
        }
        return n;
    }

    /** The chance a caravan is robbed. */
    public static double risk(Territory t, int capital) {
        if (bySea(t, capital)) {
            double r = 0.06 + (t.ext.getBoolean("waterBandits") ? 0.15 : 0);
            r -= 0.04 * Navy.ships(t, ModCrafts.MENGCHONG_WARSHIP.get()) + 0.05 * FigureArts.count(t, "admiral");
            return Math.max(0.01, Math.min(0.4, r));
        }
        double r = 0.15 - 0.03 * inns(t) - Math.min(0.08, Village.militiaCount(t) * 0.004) - 0.03 * FigureArts.count(t, "strategist");
        return Math.max(0.02, Math.min(0.4, r));
    }

    public static long capacity(Territory t, Resident merchant, int capital) {
        long c = 300L * (1 + merchant.rank + (merchant.isFigure() ? 2 : 0));
        return bySea(t, capital) ? 3 * c : c;
    }

    public static ListTag caravans(Territory t) {
        return t.ext.getList("caravans", Tag.TAG_COMPOUND);
    }

    public static CompoundTag caravanOf(Territory t, int building) {
        ListTag l = caravans(t);
        for (int i = 0; i < l.size(); i++) if (l.getCompound(i).getInt("from") == building) return l.getCompound(i);
        return null;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        ListTag l = caravans(t);
        // Caravans coming home.
        for (int i = l.size() - 1; i >= 0; i--) {
            CompoundTag c = l.getCompound(i);
            if (c.getLong("back") > day) continue;
            l.remove(i);
            String to = ChinaMap.CAPITALS[Math.max(0, Math.min(ChinaMap.CAPITALS.length - 1, c.getInt("to")))];
            if (rnd.nextDouble() < c.getDouble("risk")) {
                Village.tell(level, t, Component.translatable(c.getBoolean("sea") ? "realmlord.trade.robbed_sea" : "realmlord.trade.robbed",
                        t.name, to, Coins.format(c.getLong("value"))));
                continue;
            }
            long gross = c.getLong("sale");
            long net = Math.round(gross * (1 - TAX));
            t.treasury += net;
            t.ext.putLong("tradeTotal", t.ext.getLong("tradeTotal") + net);
            Village.tell(level, t, Component.translatable("realmlord.trade.returned", t.name, to, Coins.format(net), Coins.format(gross - net)));
        }
        // Chambers with no caravan out send one.
        for (Building b : t.buildings) {
            if (!chamber(b) || caravanOf(t, b.id) != null) continue;
            Resident m = Village.staffOf(t, b);
            if (m == null) continue;
            int to = destination(b);
            long room = capacity(t, m, to);
            long value = 0;
            for (Goods g : CARGO) {
                if (room <= 0) break;
                long take = Math.min(room, g.stock(t) / 2);
                if (take <= 0) continue;
                g.add(t, -take);
                room -= take;
                value += take * g.price;
            }
            if (value <= 0) continue;
            double skill = 1.0 + 0.1 * m.rank + (m.isFigure() ? 0.2 : 0) + m.specialtyBonus();
            long sale = Math.round(value * markup(t, to) * skill * FigureArts.boost(t, "merchant", 0.2));
            CompoundTag c = new CompoundTag();
            c.putInt("from", b.id);
            c.putInt("to", to);
            c.putLong("value", value);
            c.putLong("sale", sale);
            c.putLong("back", day + days(t, to));
            c.putBoolean("sea", bySea(t, to));
            c.putDouble("risk", risk(t, to));
            l.add(c);
            Village.tell(level, t, Component.translatable(c.getBoolean("sea") ? "realmlord.trade.sailed" : "realmlord.trade.departed",
                    m.name, t.name, ChinaMap.CAPITALS[to], Coins.format(value), days(t, to)));
        }
        t.ext.put("caravans", l);
    }
}
