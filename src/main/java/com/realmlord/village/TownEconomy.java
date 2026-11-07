package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.server.level.ServerLevel;

/**
 * The money a town's houses of business bring in, beyond the workshops' goods: the Bank's
 * interest on the treasury (a fifth of a percent a day, up to 5,000 copper, more under a good
 * Auditing head), the Pawnshop's profit, the Inn's travellers and the Farmers' Market's stall rents
 * (which grow with the town), and the Relay Station's fees from officials and messengers.
 */
public final class TownEconomy {
    private TownEconomy() {}

    public static long income(Territory t) {
        long n = 0;
        int people = t.residents.size();
        for (Building b : t.buildings) {
            if (!b.complete || b.demolishing) continue;
            boolean staffed = b.type.staff() == null || Village.staffOf(t, b) != null;
            if (!staffed) continue;
            n += switch (b.type) {
                case BANK -> Math.round(Math.min(5_000, t.treasury * 0.002) * Office.boost(t, Office.AUDITING, 300));
                case PAWNSHOP -> 300 + people / 20;
                case INN -> 400 + people / 10;
                case FARMERS_MARKET -> 300 + people / 10;
                case RELAY_STATION -> 200;
                default -> 0;
            };
        }
        return n;
    }

    public static void day(ServerLevel level, Territory t) {
        long n = income(t);
        if (n > 0) {
            t.treasury += n;
            t.ext.putLong("businessIncome", n);
        }
    }
}
