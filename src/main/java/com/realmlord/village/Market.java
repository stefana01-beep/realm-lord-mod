package com.realmlord.village;

/**
 * The Basic Market from the novel: the four basic resources at its prices (a unit of grain 10
 * copper, wood 20, stone 30, iron ore 100), with a 20% tax on everything sold.
 */
public final class Market {
    private Market() {}

    /** Something the market's armoury sells: so many of an item, at a price (copper), at a grade (or none). */
    public record Ware(java.util.function.Supplier<net.minecraft.world.item.Item> item, int count, long price, com.realmlord.territory.TokenGrade grade,
                       com.realmlord.loot.Manuals.Plan plan) {
        public Ware(java.util.function.Supplier<net.minecraft.world.item.Item> item, int count, long price, com.realmlord.territory.TokenGrade grade) {
            this(item, count, price, grade, null);
        }

        /** A forging or alchemy manual, at its price. */
        public static Ware manual(com.realmlord.loot.Manuals.Plan p) {
            return new Ware(() -> com.realmlord.registry.ModArms.MANUAL.get(), 1, p.price(), null, p);
        }

        public net.minecraft.world.item.ItemStack stack(net.minecraft.util.RandomSource r) {
            if (plan != null) return com.realmlord.loot.Manuals.of(plan);
            if (grade != null) return com.realmlord.loot.Gear.make(item.get(), grade, r);
            return new net.minecraft.world.item.ItemStack(item.get(), count);
        }
    }

    /** The armoury: materials for the Lord's own smithing, and finished arms and armour at their grades. */
    public static final java.util.List<Ware> WARES = java.util.List.of(
            new Ware(() -> com.realmlord.registry.ModArms.BEAST_SINEW.get(), 4, 1_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_INGOT.get(), 2, 3_500, null),
            new Ware(() -> com.realmlord.registry.ModArms.REFINED_STEEL.get(), 1, 7_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.BEAST_CORE.get(), 1, 15_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.LAMELLAR_PLATES.get(), 8, 4_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.WHETSTONE.get(), 4, 400, null),
            new Ware(() -> com.realmlord.registry.ModArms.REFINING_STONE.get(), 1, 25_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.GOLDEN_WOUND_POWDER.get(), 4, 800, null),
            new Ware(() -> com.realmlord.registry.ModArms.HEALING_PILL.get(), 2, 3_000, null),
            new Ware(() -> com.realmlord.registry.ModArms.MILITARY_RATIONS.get(), 8, 600, null),
            Ware.manual(com.realmlord.loot.Manuals.RATIONS),
            Ware.manual(com.realmlord.loot.Manuals.LAMELLAR),
            Ware.manual(com.realmlord.loot.Manuals.POLEARMS),
            Ware.manual(com.realmlord.loot.Manuals.ALCHEMY),
            Ware.manual(com.realmlord.loot.Manuals.MOUNTAIN_PATTERN),
            Ware.manual(com.realmlord.loot.Manuals.TIGER_WAR),
            new Ware(() -> com.realmlord.registry.ModArms.HIDE_HELMET.get(), 1, 3_000, com.realmlord.territory.TokenGrade.BRONZE),
            new Ware(() -> com.realmlord.registry.ModArms.HIDE_JERKIN.get(), 1, 4_000, com.realmlord.territory.TokenGrade.BRONZE),
            new Ware(() -> com.realmlord.registry.ModArms.HIDE_LEGGINGS.get(), 1, 3_500, com.realmlord.territory.TokenGrade.BRONZE),
            new Ware(() -> com.realmlord.registry.ModArms.HIDE_BOOTS.get(), 1, 2_500, com.realmlord.territory.TokenGrade.BRONZE),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_SABRE.get(), 1, 18_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_SPEAR.get(), 1, 18_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.RING_DAO.get(), 1, 16_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.TIGER_BONE_BOW.get(), 1, 22_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_HELMET.get(), 1, 15_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_CUIRASS.get(), 1, 25_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_GREAVES.get(), 1, 20_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.BLACK_IRON_BOOTS.get(), 1, 12_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.LAMELLAR_HELMET.get(), 1, 14_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.LAMELLAR_COAT.get(), 1, 24_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.LAMELLAR_TASSETS.get(), 1, 19_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.LAMELLAR_BOOTS.get(), 1, 11_000, com.realmlord.territory.TokenGrade.BLACK_IRON),
            new Ware(() -> com.realmlord.registry.ModArms.STEEL_JIAN.get(), 1, 70_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.GUANDAO.get(), 1, 90_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.JI.get(), 1, 85_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.MOUNTAIN_HELM.get(), 1, 60_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.MOUNTAIN_ARMOUR.get(), 1, 110_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.MOUNTAIN_GREAVES.get(), 1, 90_000, com.realmlord.territory.TokenGrade.SILVER),
            new Ware(() -> com.realmlord.registry.ModArms.MOUNTAIN_BOOTS.get(), 1, 55_000, com.realmlord.territory.TokenGrade.SILVER));

    /** A manual for sale on the market's Manuals page: the book and its price (copper). */
    public record Book(java.util.function.Supplier<net.minecraft.world.item.ItemStack> stack, long price) {}

    private static java.util.List<Book> books;

    /**
     * The Manuals page: every technical manual (Junior 50 silver, Intermediate 3 gold, Advanced
     * 10 gold, Master 30 gold) and the secret manuals the novel's market sold: Bronze 50 silver,
     * Black Iron 2 gold, Silver 10 gold, Gold 50 gold. King rank and above are never sold.
     */
    public static java.util.List<Book> books() {
        if (books == null) {
            java.util.List<Book> out = new java.util.ArrayList<>();
            for (var a : com.realmlord.martial.ArtData.ALL)
                if (a.grade().price > 0) out.add(new Book(() -> com.realmlord.martial.Arts.manual(a), a.grade().price));
            for (var t : com.realmlord.work.TechData.ALL) out.add(new Book(() -> com.realmlord.work.TechManuals.manual(t), t.price()));
            books = java.util.List.copyOf(out);
        }
        return books;
    }

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
