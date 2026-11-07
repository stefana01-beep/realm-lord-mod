package com.realmlord.village;

import com.realmlord.registry.ModCrafts;
import com.realmlord.territory.Territory;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * The village's navy and fishing fleet, after the novel: a dock lets the village keep boats and
 * recruit sailors and marines; each fishing boat carries two fishermen; a sailor unit is one
 * turreted ship, four Mengchong warships and twelve cima boats for scouting. Ships are kept in
 * the storehouse (the shipyard puts them there), and sailors are soldiers of the militia who
 * serve on the water.
 */
public final class Navy {
    private Navy() {}

    public static final long ENLIST_COST = 3_000;
    /** A sailor unit: turreted ships, Mengchong warships, cima boats. */
    public static final int[] UNIT = {1, 4, 12};

    public static boolean hasDock(Territory t) {
        return Village.countComplete(t, BuildingType.DOCK) > 0;
    }

    /** Ten sailors for each dock, twenty more for each shipyard, more under an admiral. */
    public static int cap(Territory t) {
        int n = 10 * Village.countComplete(t, BuildingType.DOCK);
        if (n == 0) return 0;
        n += 20 * Village.countComplete(t, BuildingType.SHIPYARD);
        return n + 10 * FigureArts.count(t, "admiral");
    }

    public static int count(Territory t) {
        int n = 0;
        for (Resident r : t.residents) if (r.sailor) n++;
        return n;
    }

    /** Enlists a resident as a sailor (a soldier of the navy), or stands them down. */
    public static String enlist(Territory t, Resident r, boolean join) {
        if (!join) {
            r.sailor = false;
            r.militia = false;
            return null;
        }
        if (r.sailor) return null;
        if (r.unit >= 0) return "realmlord.army.err.soldier";
        if (r.office >= 0) return "realmlord.militia.err.office";
        if (cap(t) == 0) return "realmlord.navy.err.no_dock";
        if (count(t) >= cap(t)) return "realmlord.navy.err.full";
        r.sailor = true;
        r.militia = true;
        r.follows = false;
        r.job = Job.IDLE;
        r.workplace = -1;
        return null;
    }

    /** Ships of a kind kept in the storehouse. */
    public static int ships(Territory t, Item ship) {
        return t.storage.countItem(ship);
    }

    /** The fleet: {fishing boats, cima boats, merchant ships, Mengchong warships, turreted ships}. */
    public static int[] fleet(Territory t) {
        return new int[]{ships(t, ModCrafts.FISHING_BOAT.get()), ships(t, ModCrafts.CIMA_BOAT.get()), ships(t, ModCrafts.MERCHANT_SHIP.get()),
                ships(t, ModCrafts.MENGCHONG_WARSHIP.get()), ships(t, ModCrafts.TURRETED_SHIP.get())};
    }

    /** Full sailor units the fleet and sailors make up: each needs its ships and forty sailors. */
    public static int units(Territory t) {
        int[] f = fleet(t);
        int byShips = Math.min(f[4] / UNIT[0], Math.min(f[3] / UNIT[1], f[1] / UNIT[2]));
        return Math.min(byShips, count(t) / 40);
    }

    /** Places for fishermen: two to each fishing boat, if there is a dock to keep them at. */
    public static int fishingPlaces(Territory t) {
        return hasDock(t) ? 2 * ships(t, ModCrafts.FISHING_BOAT.get()) : 0;
    }

    /** The fishing fleet's catch and the sailors' drill on the water. */
    public static void day(Territory t, RandomSource rnd) {
        double fish = 0;
        for (Resident r : t.residents) {
            if (r.job != Job.FISHING || r.militia) continue;
            fish += 25 * (r.occupation == Occupation.FISHERMAN ? 1.3 + 0.15 * r.rank : 1.0);
        }
        if (fish > 0) {
            fish *= Village.productionBonus(t) * (1.0 + 0.1 * FigureArts.count(t, "admiral"));
            // Water bandits on the river nearby take their share of the catch.
            if (t.ext.getBoolean("waterBandits")) fish *= 0.6;
            t.goodsCarry[Goods.FISH.ordinal()] += fish;
            long whole = (long) Math.floor(t.goodsCarry[Goods.FISH.ordinal()]);
            t.goodsCarry[Goods.FISH.ordinal()] -= whole;
            Goods.FISH.add(t, whole);
        }
        if (!hasDock(t)) return;
        double boost = 1.0 + 0.5 * FigureArts.count(t, "admiral");
        for (Resident r : t.residents) {
            if (!r.sailor) continue;
            r.gainSoldierXp(Math.round(10 * boost));
            if (r.force < 70 && rnd.nextInt(8) == 0) r.force++;
        }
    }
}
