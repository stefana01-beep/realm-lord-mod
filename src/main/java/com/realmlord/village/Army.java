package com.realmlord.village;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The standing army. In the novel Shanhai outgrew its militia once it became a town: Ouyang Shuo
 * set up a proper army in the City Defense Camp, with infantry, spearmen, archers, a crossbow
 * division and at last cavalry on Qingfu horses, and "ever since he had set up a proper army, the
 * expenses of the military had been increasing".
 *
 * <p>Soldiers are enlisted from the residents into one of six arms, each with its cost and pay.
 * They drill with the militia (a fifth faster at a City Defense Camp, more under the Meritorious
 * Service Division), fight alongside it, and rise through the ranks with their level: Soldier,
 * Squad Leader, Platoon Leader, Company Commander, Battalion Commander. Places: fifty at each City
 * Defense Camp, twenty at each Barracks, ten for each summoned general, and more under a good
 * Military Affairs head. Pay comes out of the treasury every day; unpaid soldiers lose loyalty.
 */
public final class Army {
    private Army() {}

    public enum Unit {
        INFANTRY("infantry", 2_000, 20, 1.0, null),
        SPEARMAN("spearman", 2_500, 25, 1.1, null),
        ARCHER("archer", 3_000, 30, 1.2, null),
        CROSSBOWMAN("crossbowman", 4_000, 40, 1.4, BuildingType.ARSENAL),
        CAVALRY("cavalry", 10_000, 80, 2.0, BuildingType.STABLE),
        /** The mountain barbarians' elite warriors, enlisted from a submitted tribe. */
        MOUNTAIN_WARRIOR("mountain_warrior", 3_000, 30, 1.6, null);

        public final String id;
        public final long cost, pay;
        public final double strength;
        public final BuildingType needs;

        Unit(String id, long cost, long pay, double strength, BuildingType needs) {
            this.id = id;
            this.cost = cost;
            this.pay = pay;
            this.strength = strength;
            this.needs = needs;
        }

        public String key() {
            return "realmlord.army.unit." + id;
        }

        public boolean ranged() {
            return this == ARCHER || this == CROSSBOWMAN;
        }

        public static Unit byId(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }

    public static final String[] RANKS = {"soldier", "squad_leader", "platoon_leader", "company_commander", "battalion_commander"};

    public static String rank(Resident r) {
        return RANKS[Math.min(RANKS.length - 1, (r.soldierLevel - 1) / 6)];
    }

    public static int cap(Territory t) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1)) return 0;
        int camps = Village.countComplete(t, BuildingType.CITY_DEFENSE_CAMP);
        int barracks = Village.countComplete(t, BuildingType.BARRACKS);
        if (camps + barracks == 0) return 0;
        return 50 * camps + 20 * barracks + 10 * FigureArts.count(t, "general") + Office.power(t, Office.MILITARY) / 4;
    }

    public static int count(Territory t) {
        int n = 0;
        for (Resident r : t.residents) if (r.unit >= 0) n++;
        return n;
    }

    public static int count(Territory t, Unit u) {
        int n = 0;
        for (Resident r : t.residents) if (r.unit == u.ordinal()) n++;
        return n;
    }

    /** What the army is worth in a fight: each soldier by arm, level and kit. */
    public static int strength(Territory t) {
        double s = 0;
        for (Resident r : t.residents) {
            Unit u = Unit.byId(r.unit);
            if (u == null) continue;
            double arm = u.strength * (u.ranged() ? Office.boost(t, Office.BOW_CROSSBOW, 300) : 1.0);
            s += arm * (1 + r.soldierLevel / 6.0) * (1 + 0.2 * r.equipTier);
        }
        return (int) Math.round(s);
    }

    /** The army's weight in the military index: a point for every five soldiers, up to 25. */
    public static int indexBonus(Territory t) {
        return Math.min(25, count(t) / 5);
    }

    public static double drillBonus(Territory t) {
        return (Village.countComplete(t, BuildingType.CITY_DEFENSE_CAMP) > 0 ? 1.2 : 1.0) * Office.boost(t, Office.MERIT, 300);
    }

    /** Enlists a resident in an arm (the Lord pays the cost), or discharges them (unit null). Returns an error key or null. */
    public static String enlist(Territory t, Resident r, Unit u) {
        if (u == null) {
            r.unit = -1;
            r.militia = false;
            return null;
        }
        if (r.unit == u.ordinal()) return null;
        if (r.office >= 0) return "realmlord.militia.err.office";
        if (r.sailor) return "realmlord.army.err.sailor";
        if (cap(t) == 0) return "realmlord.army.err.no_camp";
        if (r.unit < 0 && count(t) >= cap(t)) return "realmlord.army.err.full";
        if (u.needs != null && Village.countComplete(t, u.needs) == 0) return "realmlord.army.err.needs_" + u.needs.id();
        if (u == Unit.MOUNTAIN_WARRIOR && !r.mountain) return "realmlord.army.err.mountain";
        r.unit = u.ordinal();
        r.militia = true;
        r.follows = false;
        r.job = Job.IDLE;
        r.workplace = -1;
        return null;
    }

    /** The day's pay: soldiers unpaid lose heart. */
    public static void day(ServerLevel level, Territory t) {
        long pay = 0;
        for (Resident r : t.residents) {
            Unit u = Unit.byId(r.unit);
            if (u != null) pay += u.pay;
        }
        if (pay == 0) return;
        if (t.treasury >= pay) {
            t.treasury -= pay;
            return;
        }
        t.treasury = 0;
        for (Resident r : t.residents) if (r.unit >= 0) r.loyalty = Math.max(0, r.loyalty - 2);
        Village.tell(level, t, Component.translatable("realmlord.army.unpaid", t.name, Coins.format(pay)));
    }
}
