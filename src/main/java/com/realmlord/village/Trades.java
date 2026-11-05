package com.realmlord.village;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * What each occupation trades, in copper. Craftsmen who need a workshop only sell their best
 * goods while they staff one (a blacksmith without a forge has little to offer).
 */
public final class Trades {
    private Trades() {}

    /** sell = the resident sells to the player; otherwise the resident buys from the player. */
    public record Offer(Item item, int count, long price, boolean sell, boolean needsWorkshop) {
        public ItemStack stack() {
            return new ItemStack(item, count);
        }
    }

    public static List<Offer> of(Occupation o) {
        List<Offer> l = new ArrayList<>();
        switch (o) {
            case FARMER -> {
                l.add(sell(Items.WHEAT, 16, 60));
                l.add(sell(Items.BREAD, 4, 60));
                l.add(sell(Items.POTATO, 8, 40));
                l.add(sell(Items.CARROT, 8, 40));
                l.add(sell(Items.WHEAT_SEEDS, 16, 20));
                l.add(buy(Items.BONE_MEAL, 8, 30));
            }
            case FRUIT_FARMER -> {
                l.add(sell(Items.APPLE, 6, 60));
                l.add(sell(Items.SWEET_BERRIES, 12, 40));
                l.add(sell(Items.MELON_SLICE, 12, 40));
                l.add(sell(Items.OAK_SAPLING, 4, 30));
            }
            case CARPENTER -> {
                l.add(sell(Items.OAK_PLANKS, 32, 120));
                l.add(sell(Items.SPRUCE_DOOR, 1, 40));
                l.add(sell(Items.CHEST, 1, 60));
                l.add(sell(Items.LADDER, 8, 40));
                l.add(buy(Items.OAK_LOG, 16, 60));
            }
            case MASON -> {
                l.add(sell(Items.STONE_BRICKS, 32, 200));
                l.add(sell(Items.COBBLESTONE, 64, 120));
                l.add(sell(Items.SMOOTH_STONE, 16, 120));
                l.add(buy(Items.COBBLESTONE, 32, 40));
            }
            case BLACKSMITH -> {
                l.add(sell(Items.IRON_PICKAXE, 1, 800, true));
                l.add(sell(Items.IRON_AXE, 1, 800, true));
                l.add(sell(Items.IRON_HOE, 1, 500, true));
                l.add(sell(Items.IRON_SWORD, 1, 900, true));
                l.add(sell(Items.SHEARS, 1, 300, true));
                l.add(sell(Items.IRON_INGOT, 4, 400));
                l.add(buy(Items.RAW_IRON, 8, 300));
            }
            case GROCER -> {
                l.add(sell(Items.TORCH, 16, 60, true));
                l.add(sell(Items.CANDLE, 4, 60, true));
                l.add(sell(Items.STRING, 8, 60, true));
                l.add(sell(Items.BREAD, 6, 80, true));
                l.add(sell(Items.BOWL, 4, 20, true));
                l.add(buy(Items.WHEAT, 16, 50));
            }
            case TAILOR -> {
                l.add(sell(Items.LEATHER_CHESTPLATE, 1, 400, true));
                l.add(sell(Items.LEATHER_HELMET, 1, 250, true));
                l.add(sell(Items.WHITE_WOOL, 8, 120, true));
                l.add(sell(Items.RED_WOOL, 8, 150, true));
                l.add(buy(Items.LEATHER, 4, 120));
            }
            case DOCTOR -> {
                l.add(sell(Items.GLISTERING_MELON_SLICE, 1, 300));
                l.add(sell(Items.HONEY_BOTTLE, 2, 200, true));
                l.add(sell(Items.GOLDEN_CARROT, 2, 400, true));
                l.add(buy(Items.SPIDER_EYE, 4, 80));
            }
            case SCHOLAR -> {
                l.add(sell(Items.PAPER, 16, 120));
                l.add(sell(Items.BOOK, 2, 200));
                l.add(sell(Items.MAP, 1, 300));
                l.add(sell(Items.WRITABLE_BOOK, 1, 300));
            }
            case MARTIAL_ARTIST -> {
                l.add(sell(Items.STICK, 16, 20));
                l.add(sell(Items.SHIELD, 1, 500));
                l.add(buy(Items.ROTTEN_FLESH, 16, 30));
            }
            case POTTER -> {
                l.add(sell(Items.FLOWER_POT, 4, 60, true));
                l.add(sell(Items.BRICKS, 16, 160, true));
                l.add(sell(Items.DECORATED_POT, 1, 150, true));
                l.add(buy(Items.CLAY_BALL, 16, 60));
            }
            case WINEMAKER -> {
                l.add(sell(Items.HONEY_BOTTLE, 3, 240, true));
                l.add(sell(Items.GLASS_BOTTLE, 8, 80));
                l.add(sell(Items.SUGAR, 16, 80));
                l.add(buy(Items.SWEET_BERRIES, 16, 40));
            }
            case COOK -> {
                l.add(sell(Items.COOKED_BEEF, 4, 120));
                l.add(sell(Items.COOKED_PORKCHOP, 4, 120));
                l.add(sell(Items.MUSHROOM_STEW, 1, 50));
                l.add(sell(Items.PUMPKIN_PIE, 2, 90));
                l.add(buy(Items.BEEF, 4, 60));
            }
            case HUNTER -> {
                l.add(sell(Items.LEATHER, 4, 120));
                l.add(sell(Items.RABBIT_HIDE, 4, 60));
                l.add(sell(Items.ARROW, 16, 80));
                l.add(sell(Items.COOKED_MUTTON, 4, 100));
                l.add(buy(Items.FEATHER, 8, 30));
            }
            case FISHERMAN -> {
                l.add(sell(Items.COOKED_COD, 6, 90));
                l.add(sell(Items.COOKED_SALMON, 4, 100));
                l.add(sell(Items.FISHING_ROD, 1, 150));
                l.add(buy(Items.STRING, 4, 30));
            }
            case MINER -> {
                l.add(sell(Items.COAL, 16, 160));
                l.add(sell(Items.RAW_IRON, 4, 200));
                l.add(sell(Items.RAW_COPPER, 8, 120));
                l.add(buy(Items.TORCH, 16, 40));
            }
            case STABLEMAN -> {
                l.add(sell(Items.SADDLE, 1, 600, true));
                l.add(sell(Items.LEAD, 2, 150, true));
                l.add(sell(Items.HAY_BLOCK, 4, 120));
                l.add(buy(Items.WHEAT, 16, 50));
            }
            case CIVIL_SERVANT -> {
                l.add(sell(Items.PAPER, 8, 60));
                l.add(sell(Items.INK_SAC, 4, 60));
            }
            case OFFICER -> {
                l.add(sell(Items.BOW, 1, 300));
                l.add(sell(Items.ARROW, 16, 80));
                l.add(sell(Items.IRON_HELMET, 1, 600));
            }
        }
        return l;
    }

    private static Offer sell(Item item, int count, long price) {
        return new Offer(item, count, price, true, false);
    }

    private static Offer sell(Item item, int count, long price, boolean needsWorkshop) {
        return new Offer(item, count, price, true, needsWorkshop);
    }

    private static Offer buy(Item item, int count, long price) {
        return new Offer(item, count, price, false, false);
    }
}
