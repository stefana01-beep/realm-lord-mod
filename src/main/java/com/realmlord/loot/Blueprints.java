package com.realmlord.loot;

import com.realmlord.registry.ModItems;
import com.realmlord.village.BuildingType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Blueprints for the novel's buildings that need one, by the village grade that unlocks them. */
public final class Blueprints {
    private Blueprints() {}

    public static final String TAG = "realmlord_building";

    public static final List<String> VILLAGE_1 = ids(1);
    public static final List<String> VILLAGE_2 = ids(2);
    public static final List<String> VILLAGE_3 = ids(3);

    private static List<String> ids(int grade) {
        List<String> out = new ArrayList<>();
        for (BuildingType t : BuildingType.ofGrade(grade)) if (t.blueprint) out.add(t.id());
        return List.copyOf(out);
    }

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
        BuildingType t = BuildingType.byId(building);
        return t == null ? 0 : t.grade;
    }
}
