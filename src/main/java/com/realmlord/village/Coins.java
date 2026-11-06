package com.realmlord.village;

import com.realmlord.item.CoinItem;
import com.realmlord.item.ResourcesBagItem;
import com.realmlord.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Paying and receiving coins, counted in copper (1 gold = 100 silver = 10,000 copper). Coins live
 * in the player's purse, off the inventory: coins picked up or carried go into it, payments come
 * out of it (and out of any coins still carried), and the Lord can keep coins in the manor's
 * treasury too, which the market draws on when the purse runs short.
 */
public final class Coins {
    private Coins() {}

    public static long purse(ServerPlayer p) {
        return com.realmlord.profile.Profiles.get(p).getPurse();
    }

    /** Coin items carried (not yet in the purse). */
    public static long carried(ServerPlayer p) {
        long total = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) total += CoinItem.copperValue(inv.getItem(i));
        return total;
    }

    /** Everything the player can spend: the purse and any coins carried. */
    public static long count(ServerPlayer p) {
        return purse(p) + carried(p);
    }

    /** Puts every coin carried into the purse. Returns the copper moved. */
    public static long pocket(ServerPlayer p) {
        long moved = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            long v = CoinItem.copperValue(s);
            if (v <= 0) continue;
            moved += v;
            inv.setItem(i, ItemStack.EMPTY);
        }
        if (moved > 0) addToPurse(p, moved);
        return moved;
    }

    public static void addToPurse(ServerPlayer p, long copper) {
        var prof = com.realmlord.profile.Profiles.get(p);
        prof.setPurse(prof.getPurse() + copper);
        com.realmlord.profile.Profiles.sync(p);
    }

    /** What the player can spend at their own village's market: purse, coins carried and the manor treasury. */
    public static long countWithTreasury(ServerPlayer p) {
        com.realmlord.territory.Territory t = treasuryOf(p);
        return count(p) + (t == null ? 0 : t.treasury);
    }

    private static com.realmlord.territory.Territory treasuryOf(ServerPlayer p) {
        int id = com.realmlord.profile.Profiles.get(p).getTerritoryId();
        com.realmlord.territory.Territory t = id >= 0 ? com.realmlord.territory.TerritoryData.get(p.server).byId(id) : null;
        return t != null && p.getUUID().equals(t.owner) ? t : null;
    }

    /** Pays from the purse and coins carried, and then from the Lord's manor treasury. */
    public static boolean payWithTreasury(ServerPlayer p, long copper) {
        if (copper <= 0) return true;
        long own = count(p);
        if (own >= copper) return pay(p, copper);
        com.realmlord.territory.Territory t = treasuryOf(p);
        if (t == null || own + t.treasury < copper) return false;
        long fromTreasury = copper - own;
        if (own > 0 && !pay(p, own)) return false;
        t.treasury -= fromTreasury;
        com.realmlord.territory.TerritoryData.get(p.server).touch();
        return true;
    }

    /** Takes the price from the purse first, then from any coins carried. False if they cannot afford it. */
    public static boolean pay(ServerPlayer p, long copper) {
        if (copper <= 0) return true;
        if (count(p) < copper) return false;
        long fromPurse = Math.min(purse(p), copper);
        if (fromPurse > 0) {
            var prof = com.realmlord.profile.Profiles.get(p);
            prof.setPurse(prof.getPurse() - fromPurse);
            copper -= fromPurse;
            com.realmlord.profile.Profiles.sync(p);
            if (copper <= 0) return true;
        }
        long taken = 0;
        Inventory inv = p.getInventory();
        // Spend copper first, then silver, then gold.
        for (int tier = 0; tier <= 2 && taken < copper; tier++) {
            long each = tier == 0 ? 1 : tier == 1 ? 100 : 10_000;
            for (int i = 0; i < inv.getContainerSize() && taken < copper; i++) {
                ItemStack s = inv.getItem(i);
                if (!(s.getItem() instanceof CoinItem c) || c.tier != tier) continue;
                while (!s.isEmpty() && taken < copper) {
                    s.shrink(1);
                    taken += each;
                }
            }
        }
        give(p, taken - copper);
        return true;
    }

    /** Gives an amount into the purse. */
    public static void give(ServerPlayer p, long copper) {
        if (copper <= 0) return;
        addToPurse(p, copper);
    }

    /** Gives an amount as coins in hand, in the fewest coins (to trade or hand to another player). */
    public static void giveItems(ServerPlayer p, long copper) {
        if (copper <= 0) return;
        long gold = copper / 10_000, silver = (copper % 10_000) / 100, cop = copper % 100;
        ResourcesBagItem.give(p, new ItemStack(ModItems.GOLD_COIN.get()), (int) Math.min(gold, 100_000));
        ResourcesBagItem.give(p, new ItemStack(ModItems.SILVER_COIN.get()), (int) silver);
        ResourcesBagItem.give(p, new ItemStack(ModItems.COPPER_COIN.get()), (int) cop);
    }

    /** "1 gold 20 silver 5 copper". */
    public static Component format(long copper) {
        long gold = copper / 10_000, silver = (copper % 10_000) / 100, cop = copper % 100;
        if (copper == 0) return Component.translatable("realmlord.money.copper", 0);
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        boolean first = true;
        if (gold > 0) {
            out.append(Component.translatable("realmlord.money.gold", gold));
            first = false;
        }
        if (silver > 0) {
            if (!first) out.append(" ");
            out.append(Component.translatable("realmlord.money.silver", silver));
            first = false;
        }
        if (cop > 0) {
            if (!first) out.append(" ");
            out.append(Component.translatable("realmlord.money.copper", cop));
        }
        return out;
    }
}
