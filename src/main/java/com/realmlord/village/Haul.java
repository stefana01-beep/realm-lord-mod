package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What residents carry home. Logs, planks, stone, iron and grain go into the village stores
 * when a resident hands them in at the Lord's Manor; anything else stays in their pack.
 */
public final class Haul {
    private Haul() {}

    public static final int WOOD = 0, STONE = 1, ORE = 2, FOOD = 3;

    /** {resource, units per item}, or null if the stores do not take it. */
    public static int[] value(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.is(ItemTags.LOGS)) return new int[]{WOOD, 2};
        if (s.is(ItemTags.PLANKS)) return new int[]{WOOD, 1};
        if (s.is(Items.COBBLESTONE) || s.is(Items.STONE) || s.is(Items.COBBLED_DEEPSLATE) || s.is(Items.STONE_BRICKS)
                || s.is(Items.ANDESITE) || s.is(Items.DIORITE) || s.is(Items.GRANITE)) return new int[]{STONE, 1};
        if (s.is(Items.RAW_IRON) || s.is(Items.IRON_ORE) || s.is(Items.DEEPSLATE_IRON_ORE)) return new int[]{ORE, 1};
        if (s.is(Items.WHEAT)) return new int[]{FOOD, 10};
        return null;
    }

    /** Items in a pack the stores would take. */
    public static int load(SimpleContainer inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (value(inv.getItem(i)) != null) n += inv.getItem(i).getCount();
        return n;
    }

    public static boolean carriesAnything(SimpleContainer inv) {
        return load(inv) > 0;
    }

    /** Hands a resident's haul in to the stores. Returns the units delivered {wood, stone, ore, food}. */
    public static long[] deposit(Territory t, Resident r) {
        long[] got = new long[4];
        for (int i = 0; i < r.inv.getContainerSize(); i++) {
            ItemStack s = r.inv.getItem(i);
            int[] v = value(s);
            if (v == null) continue;
            got[v[0]] += (long) v[1] * s.getCount();
            r.inv.setItem(i, ItemStack.EMPTY);
        }
        t.wood += got[WOOD];
        t.stone += got[STONE];
        t.ironOre += got[ORE];
        t.food += got[FOOD];
        for (int k = 0; k < 3; k++) r.gathered[k] += got[k];
        return got;
    }

    /** Puts items in a pack; whatever does not fit is lost (or should be dropped by the caller). */
    public static ItemStack give(Resident r, ItemStack s) {
        return r.inv.addItem(s);
    }
}
