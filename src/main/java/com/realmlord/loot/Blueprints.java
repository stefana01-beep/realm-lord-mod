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

    /** A blueprint of grade 1 to maxGrade: mostly Grade 1, a quarter Grade 2, one in eight Grade 3. */
    public static ItemStack random(RandomSource r, int maxGrade) {
        if (maxGrade >= 4 && r.nextInt(3) == 0) {
            // A town plan: Grade 1 to 3 of the town.
            List<String> town = new java.util.ArrayList<>();
            for (int g = 4; g <= Math.min(6, maxGrade); g++) for (BuildingType bt : BuildingType.ofGrade(g)) if (bt.blueprint) town.add(bt.id());
            if (!town.isEmpty()) return of(town.get(r.nextInt(town.size())));
        }
        int x = r.nextInt(8);
        List<String> pool = maxGrade >= 3 && x == 0 ? VILLAGE_3 : maxGrade >= 2 && x <= 2 ? VILLAGE_2 : VILLAGE_1;
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
