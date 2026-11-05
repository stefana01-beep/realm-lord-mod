package com.realmlord.village;

import com.realmlord.item.CoinItem;
import com.realmlord.item.ResourcesBagItem;
import com.realmlord.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Paying and receiving coins, counted in copper (1 gold = 100 silver = 10,000 copper). */
public final class Coins {
    private Coins() {}

    public static long count(ServerPlayer p) {
        long total = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) total += CoinItem.copperValue(inv.getItem(i));
        return total;
    }

    /** Takes the price from the player's coins, giving change. False if they cannot afford it. */
    public static boolean pay(ServerPlayer p, long copper) {
        if (copper <= 0) return true;
        if (count(p) < copper) return false;
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

    /** Gives an amount in the fewest coins. */
    public static void give(ServerPlayer p, long copper) {
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
