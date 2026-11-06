package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Everything the Basic Market's Material Trading Platform deals in, at the novel's prices where
 * it gives them (copper per unit): grain 10, wood 20, iron ore 100, salt 20, a roll of linen 10
 * silver, fine silk 50 silver, a unit of Three Flower Wine 1 silver (brewed from 3 grain), a
 * barrel of fire oil 1 silver. Stone and the rest are priced to sit sensibly beside them.
 * The first four are the village's basic stores; the rest are kept as goods in the Lord's Manor.
 */
public enum Goods {
    GRAIN("grain", 10), WOOD("wood", 20), STONE("stone", 30), IRON_ORE("iron_ore", 100),
    SALT("salt", 20), MEAT("meat", 30), FISH("fish", 15), LEATHER("leather", 50), HERBS("herbs", 25),
    FLOUR("flour", 15), WINE("wine", 100), POTTERY("pottery", 50), TOOLS("tools", 300), WEAPONS("weapons", 800),
    LINEN("linen", 1000), FINE_SILK("fine_silk", 5000), FIRE_OIL("fire_oil", 100);

    public final String id;
    /** Copper per unit, before the market's 20% tax on sales. */
    public final long price;

    Goods(String id, long price) {
        this.id = id;
        this.price = price;
    }

    public String key() {
        return "realmlord.goods." + id;
    }

    public boolean basic() {
        return ordinal() < 4;
    }

    public static Goods byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    public long stock(Territory t) {
        return switch (this) {
            case GRAIN -> t.food;
            case WOOD -> t.wood;
            case STONE -> t.stone;
            case IRON_ORE -> t.ironOre;
            default -> t.goods[ordinal()];
        };
    }

    public void add(Territory t, long units) {
        switch (this) {
            case GRAIN -> t.food += units;
            case WOOD -> t.wood += units;
            case STONE -> t.stone += units;
            case IRON_ORE -> t.ironOre += units;
            default -> t.goods[ordinal()] += units;
        }
    }

    public long buyCost(long units) {
        return price * units;
    }

    public long sellIncome(long units) {
        return Math.round(price * units * (1.0 - Market.SALES_TAX));
    }

    /** What an item counts as at the market or the storehouse: {good, units per item}, or null. */
    public record Unit(Goods good, int units) {}

    public static Unit of(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.getTag() != null && (s.getTag().contains(com.realmlord.loot.Gear.TAG) || s.getTag().contains(com.realmlord.loot.Blueprints.TAG))) return null;
        Item i = s.getItem();
        if (s.is(ItemTags.LOGS)) return new Unit(WOOD, 2);
        if (s.is(ItemTags.PLANKS)) return new Unit(WOOD, 1);
        if (i == Items.COBBLESTONE || i == Items.STONE || i == Items.COBBLED_DEEPSLATE || i == Items.STONE_BRICKS
                || i == Items.ANDESITE || i == Items.DIORITE || i == Items.GRANITE) return new Unit(STONE, 1);
        if (i == Items.RAW_IRON || i == Items.IRON_ORE || i == Items.DEEPSLATE_IRON_ORE) return new Unit(IRON_ORE, 1);
        if (i == Items.WHEAT || i == Items.POTATO || i == Items.CARROT || i == Items.BEETROOT) return new Unit(GRAIN, 1);
        if (i == Items.BREAD) return new Unit(FLOUR, 3);
        if (i == Items.BEEF || i == Items.PORKCHOP || i == Items.MUTTON || i == Items.CHICKEN || i == Items.RABBIT) return new Unit(MEAT, 1);
        if (i == Items.COD || i == Items.SALMON) return new Unit(FISH, 1);
        if (i == Items.LEATHER) return new Unit(LEATHER, 1);
        if (i == Items.FLOWER_POT || i == Items.DECORATED_POT) return new Unit(POTTERY, 1);
        if (i == Items.IRON_PICKAXE || i == Items.IRON_AXE || i == Items.IRON_SHOVEL || i == Items.IRON_HOE) return new Unit(TOOLS, 1);
        if (i == Items.IRON_SWORD || i == Items.BOW || i == Items.CROSSBOW) return new Unit(WEAPONS, 1);
        return null;
    }

    /**
     * What the village's workshops make each day, as in the novel's industry: the Wine Workshop
     * brews Three Flower Wine from grain (3 to 1), the Mill grinds flour, the potters, smiths and
     * weaponsmiths work stone and iron, the tailor weaves linen, the Mulberry Shed yields fine
     * silk, the Clinic gathers herbs and the Grocery Shop brings in salt. A workshop needs its
     * specialist at work; inputs come from the stores.
     * Each recipe: building, input good (or null), input per day, output good, output per day.
     */
    public record Recipe(BuildingType building, Goods input, double in, Goods output, double out) {}

    public static final Recipe[] RECIPES = {
            new Recipe(BuildingType.WINE_WORKSHOP, GRAIN, 60, WINE, 20),
            new Recipe(BuildingType.MILL, GRAIN, 100, FLOUR, 100),
            new Recipe(BuildingType.POTTERY_WORKSHOP, STONE, 20, POTTERY, 20),
            new Recipe(BuildingType.BLACKSMITH_WORKSHOP, IRON_ORE, 10, TOOLS, 5),
            new Recipe(BuildingType.WEAPONS_WORKSHOP, IRON_ORE, 9, WEAPONS, 3),
            new Recipe(BuildingType.TAILOR_WORKSHOP, null, 0, LINEN, 2),
            new Recipe(BuildingType.MULBERRY_SHED, null, 0, FINE_SILK, 0.5),
            new Recipe(BuildingType.CLINIC, null, 0, HERBS, 10),
            new Recipe(BuildingType.GROCERY_SHOP, null, 0, SALT, 20),
    };

    /** One day of every workshop's work. */
    public static void produce(Territory t) {
        for (Building b : t.buildings) {
            if (!b.complete || b.demolishing) continue;
            for (Recipe r : RECIPES) {
                if (r.building() != b.type) continue;
                if (b.type.staff() != null && Village.staffOf(t, b) == null) continue;
                double scale = 1.0;
                if (r.input() != null) {
                    long have = r.input().stock(t);
                    scale = Math.min(1.0, have / r.in());
                    long used = Math.round(r.in() * scale);
                    r.input().add(t, -used);
                }
                t.goodsCarry[r.output().ordinal()] += r.out() * scale;
                long whole = (long) Math.floor(t.goodsCarry[r.output().ordinal()]);
                t.goodsCarry[r.output().ordinal()] -= whole;
                r.output().add(t, whole);
            }
        }
    }

    /** What the workshops would make in a day as things stand: per good. */
    public static double[] daily(Territory t) {
        double[] out = new double[values().length];
        for (Building b : t.buildings) {
            if (!b.complete || b.demolishing) continue;
            for (Recipe r : RECIPES) {
                if (r.building() != b.type) continue;
                if (b.type.staff() != null && Village.staffOf(t, b) == null) continue;
                out[r.output().ordinal()] += r.out();
                if (r.input() != null) out[r.input().ordinal()] -= r.in();
            }
        }
        return out;
    }

    /**
     * Moves everything the stores or the goods shelves take out of a container (a player's pack
     * or the storehouse) into the village. Returns the number of items handed in.
     */
    public static int handIn(Territory t, net.minecraft.world.Container from) {
        int n = 0;
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack s = from.getItem(i);
            Unit u = of(s);
            if (u == null) continue;
            u.good().add(t, (long) u.units() * s.getCount());
            n += s.getCount();
            from.setItem(i, ItemStack.EMPTY);
        }
        from.setChanged();
        return n;
    }

    /** Sells everything the market takes out of a container. Returns the copper earned after tax. */
    public static long sellAll(net.minecraft.world.Container from) {
        long copper = 0;
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack s = from.getItem(i);
            Unit u = of(s);
            if (u == null) continue;
            copper += u.good().sellIncome((long) u.units() * s.getCount());
            from.setItem(i, ItemStack.EMPTY);
        }
        from.setChanged();
        return copper;
    }
}
