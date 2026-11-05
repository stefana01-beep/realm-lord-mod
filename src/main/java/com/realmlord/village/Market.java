package com.realmlord.village;

/**
 * The Basic Market from the novel: the four basic resources at its prices (a unit of grain 10
 * copper, wood 20, stone 30, iron ore 100), with a 20% tax on everything sold.
 */
public final class Market {
    private Market() {}

    public static final int FOOD = 0, WOOD = 1, STONE = 2, IRON_ORE = 3;
    public static final String[] IDS = {"food", "wood", "stone", "iron_ore"};
    public static final long[] PRICE = {10, 20, 30, 100};
    public static final double SALES_TAX = 0.20;

    public static long buyCost(int resource, long units) {
        return PRICE[resource] * units;
    }

    public static long sellIncome(int resource, long units) {
        return Math.round(PRICE[resource] * units * (1.0 - SALES_TAX));
    }
}
