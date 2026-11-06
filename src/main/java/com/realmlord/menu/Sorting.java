package com.realmlord.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tidying containers: merging part stacks and putting like with like. */
public final class Sorting {
    private Sorting() {}

    /** Merges and sorts slots [from, to) of a container: by kind (blocks, then items), then name, fullest first. */
    public static void sort(Container c, int from, int to) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = from; i < to; i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty()) items.add(s.copy());
            c.setItem(i, ItemStack.EMPTY);
        }
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : items) {
            for (ItemStack m : merged) {
                if (s.isEmpty()) break;
                if (ItemStack.isSameItemSameTags(m, s) && m.getCount() < m.getMaxStackSize()) {
                    int move = Math.min(s.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    s.shrink(move);
                }
            }
            if (!s.isEmpty()) merged.add(s);
        }
        merged.sort(Comparator.comparing((ItemStack s) -> !(s.getItem() instanceof net.minecraft.world.item.BlockItem))
                .thenComparing(s -> String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())))
                .thenComparing(s -> -s.getCount()));
        int i = from;
        for (ItemStack s : merged) {
            if (i >= to) break;
            c.setItem(i++, s);
        }
        c.setChanged();
    }

    /** Sorts the non-player slots of an open menu (a chest, a barrel, a shulker box). */
    public static void sortMenu(AbstractContainerMenu menu, Inventory playerInv) {
        Container c = null;
        int lo = Integer.MAX_VALUE, hi = -1;
        for (Slot s : menu.slots) {
            if (s.container == playerInv) continue;
            if (c == null) c = s.container;
            if (s.container != c) continue;
            lo = Math.min(lo, s.getContainerSlot());
            hi = Math.max(hi, s.getContainerSlot());
        }
        if (c != null && hi >= lo) sort(c, lo, hi + 1);
        menu.broadcastChanges();
    }

    /** Moves everything it can from the container's slots into the player's inventory. */
    public static void takeAll(AbstractContainerMenu menu, net.minecraft.world.entity.player.Player p) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.container == p.getInventory() || !s.hasItem()) continue;
            menu.quickMoveStack(p, i);
        }
        menu.broadcastChanges();
    }

    /** Moves everything from the player's main inventory (not the hotbar) into the container. */
    public static void storeAll(AbstractContainerMenu menu, net.minecraft.world.entity.player.Player p) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.container != p.getInventory() || !s.hasItem() || s.getContainerSlot() < 9 || s.getContainerSlot() >= 36) continue;
            menu.quickMoveStack(p, i);
        }
        menu.broadcastChanges();
    }
}
