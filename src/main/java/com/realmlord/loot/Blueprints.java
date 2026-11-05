package com.realmlord.loot;

import com.realmlord.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The village buildings from the novel that need a blueprint, by the village grade that unlocks them. */
public final class Blueprints {
    private Blueprints() {}

    public static final String TAG = "realmlord_building";

    public static final List<String> VILLAGE_1 = List.of(
            "dining_hall", "bathroom", "small_residential_courtyard", "blacksmith_workshop", "grocery_shop", "military_camp");
    public static final List<String> VILLAGE_2 = List.of(
            "ancestral_hall", "clinic", "private_school", "martial_arts_dojo", "granary", "tailor_workshop");

    public static ItemStack of(String building) {
        ItemStack s = new ItemStack(ModItems.BLUEPRINT.get());
        s.getOrCreateTag().putString(TAG, building);
        return s;
    }

    public static ItemStack random(RandomSource r, boolean allowGrade2) {
        List<String> pool = allowGrade2 && r.nextInt(4) == 0 ? VILLAGE_2 : VILLAGE_1;
        return of(pool.get(r.nextInt(pool.size())));
    }

    public static String buildingOf(ItemStack s) {
        return s.hasTag() ? s.getTag().getString(TAG) : "";
    }

    public static int gradeOf(String building) {
        if (VILLAGE_1.contains(building)) return 1;
        if (VILLAGE_2.contains(building)) return 2;
        return 0;
    }
}
