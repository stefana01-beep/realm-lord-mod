package com.realmlord.loot;

import com.realmlord.registry.ModArms;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Equipment blueprints: forging and alchemy manuals. The novel's market sells technical manuals
 * and bandits carry blueprints; learned at the village (right-click the manual), a manual lets
 * the workshops' masters make something new.
 */
public final class Manuals {
    private Manuals() {}

    public static final String TAG = "realmlord_plan";

    /** A manual: its id and the market's price for it (copper). */
    public record Plan(String id, long price) {
        public String key() {
            return "realmlord.plan." + id;
        }
    }

    public static final Plan LAMELLAR = new Plan("lamellar", 30_000);
    public static final Plan POLEARMS = new Plan("polearms", 40_000);
    public static final Plan MOUNTAIN_PATTERN = new Plan("mountain_pattern", 150_000);
    public static final Plan TIGER_WAR = new Plan("tiger_war", 300_000);
    public static final Plan ALCHEMY = new Plan("alchemy", 100_000);
    public static final Plan RATIONS = new Plan("rations", 5_000);
    /** The Mengchong and turreted warships: the shipyard can lay them down. */
    public static final Plan WARSHIPS = new Plan("warships", 200_000);
    /** Bed crossbows, trebuchets and heavy bolts: the carpenter's siege engines. */
    public static final Plan SIEGE_ENGINES = new Plan("siege_engines", 80_000);

    public static final List<Plan> ALL = List.of(RATIONS, LAMELLAR, POLEARMS, ALCHEMY, MOUNTAIN_PATTERN, TIGER_WAR, WARSHIPS, SIEGE_ENGINES);

    public static Plan byId(String id) {
        for (Plan p : ALL) if (p.id().equals(id)) return p;
        return null;
    }

    public static ItemStack of(Plan p) {
        ItemStack s = new ItemStack(ModArms.MANUAL.get());
        s.getOrCreateTag().putString(TAG, p.id());
        return s;
    }

    public static Plan planOf(ItemStack s) {
        return s.hasTag() && s.getTag().contains(TAG) ? byId(s.getTag().getString(TAG)) : null;
    }

    /** A manual a bandit chief might carry: the cheaper ones mostly. */
    public static ItemStack random(RandomSource r) {
        int x = r.nextInt(10);
        Plan p = x < 3 ? RATIONS : x < 6 ? LAMELLAR : x < 8 ? POLEARMS : x < 9 ? ALCHEMY : MOUNTAIN_PATTERN;
        return of(p);
    }
}
