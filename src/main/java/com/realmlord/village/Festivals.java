package com.realmlord.village;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * The year's festivals. Shanhai kept them as the novel's chapters tell: New Year's Eve with a
 * banquet for every household and fireworks, the Sacrificial Ritual at the ancestral hall for those
 * who died in the beast tide, the Lantern Festival's lantern riddles on the river, and in time the
 * Dragon Boat races and the Mid-Autumn moon.
 *
 * <p>On the first day of a festival month the Rites office holds it, if the treasury can pay
 * (five copper a head, two at a village). People's Thoughts rise by five for the month (seven with
 * an ancestral hall, cemetery or temple for the rites); the Lord gains reputation; and a town's
 * festival draws travellers who leave silver in the inns and shops.
 */
public final class Festivals {
    private Festivals() {}

    /** Month -> festival. */
    public static String of(int month) {
        return switch (month) {
            case 1 -> "spring";
            case 2 -> "lantern";
            case 4 -> "qingming";
            case 5 -> "dragon_boat";
            case 8 -> "mid_autumn";
            default -> null;
        };
    }

    public static int morale(Territory t) {
        return t.ext.getInt("festivalMorale");
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        int month = GaiaCalendar.month(day);
        int key = GaiaCalendar.year(day) * 12 + month;
        // A festival's cheer lasts until its day runs out (a month for a festival, a week for a feast).
        long until = t.ext.getLong("festivalUntil");
        if (t.ext.getInt("festivalMorale") > 0 && (until > 0 ? until <= day : t.ext.getInt("festivalKey") != key)) t.ext.remove("festivalMorale");
        String fest = t.level.atLeast(TerritoryLevel.VILLAGE_2) ? of(month) : null;
        if (fest == null || t.ext.getInt("festivalKey") == key) return;
        t.ext.putInt("festivalKey", key);
        boolean town = t.level.atLeast(TerritoryLevel.TOWN_1);
        long cost = (long) t.residents.size() * (town ? 5 : 2);
        if (fest.equals("dragon_boat") && !Navy.hasDock(t)) return;
        if (t.treasury < cost) {
            Village.tell(level, t, Component.translatable("realmlord.festival.cannot", Component.translatable("realmlord.festival." + fest), t.name));
            return;
        }
        t.treasury -= cost;
        boolean rites = Village.countComplete(t, BuildingType.ANCESTRAL_HALL) + Village.countComplete(t, BuildingType.CEMETERY)
                + Village.countComplete(t, BuildingType.TEMPLE) > 0;
        int morale = (rites ? 7 : 5) + Office.power(t, Office.RITES) / 20;
        t.ext.putInt("festivalMorale", morale);
        t.ext.putLong("festivalUntil", day + GaiaCalendar.daysPerMonth());
        long income = town ? Math.round(cost * (1.2 + 0.2 * Village.countComplete(t, BuildingType.INN))) : 0;
        t.treasury += income;
        ServerPlayer lord = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (lord != null) com.realmlord.profile.Profiles.get(lord).addReputation(Math.max(5, t.residents.size() / 50));
        Village.tell(level, t, Component.translatable("realmlord.festival.held", Component.translatable("realmlord.festival." + fest), t.name,
                Coins.format(cost), morale, Coins.format(income)));
    }
}
