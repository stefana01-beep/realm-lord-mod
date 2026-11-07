package com.realmlord.village;

import com.realmlord.territory.Territory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * The Taoist priests of the village temple. In the novel the temples are where the people pray
 * and burn incense, and the priests refine pills and draw talismans; Shanhai Town even went
 * looking for an alchemist to run its alchemy workshop. Here a temple (the Village Temple or the
 * great Temple) is kept by a priest, who serves in one of five ways, chosen in the temple's window:
 *
 * <ul>
 * <li><b>Blessings</b>: incense, festivals and weddings: People's Thoughts +3 (+1 a rank);</li>
 * <li><b>Warding</b>: talismans on the gates and walls: Security +4 (+1 a rank), and raiders
 * and beasts come at the village weaker;</li>
 * <li><b>Praying for rain</b>: crops yield 10% more (+3% a rank);</li>
 * <li><b>Refining elixirs</b>: herbs into Clarity and Healing pills for the storehouse;</li>
 * <li><b>Drawing talismans</b>: Guardian Charms and Spring Talismans for the storehouse.</li>
 * </ul>
 *
 * <p>A village that builds a temple but has no priest sends for one: a wandering Taoist priest
 * comes to keep it for two gold from the treasury.
 */
public final class Temples {
    private Temples() {}

    /** Two gold, the priest's travelling money and the first year's incense. */
    public static final long INVITE_COST = 20_000;

    public static boolean temple(BuildingType type) {
        return type == BuildingType.VILLAGE_TEMPLE || type == BuildingType.TEMPLE;
    }

    /** The best priest serving in a mode, as a rank (0-2, +1 for a figure), or -1 if none is. */
    private static int serving(Territory t, String mode) {
        int best = -1;
        for (Building b : t.buildings) {
            if (!temple(b.type) || !b.complete || b.demolishing) continue;
            Workshops.Mode m = Workshops.mode(b);
            if (m == null || !m.id().equals(mode)) continue;
            Resident p = Village.staffOf(t, b);
            if (p == null) continue;
            best = Math.max(best, p.rank + (p.isFigure() ? 1 : 0));
        }
        return best;
    }

    public static int morale(Territory t) {
        int r = serving(t, "blessing");
        return r < 0 ? 0 : 3 + r;
    }

    public static int security(Territory t) {
        int r = serving(t, "warding");
        return r < 0 ? 0 : 4 + r;
    }

    /** How much weaker raiders and beasts come at a warded village: 0.9 down to 0.8. */
    public static double warding(Territory t) {
        int r = serving(t, "warding");
        return r < 0 ? 1.0 : 0.9 - 0.03 * r;
    }

    public static double cropBonus(Territory t) {
        int r = serving(t, "rain_prayer");
        return r < 0 ? 1.0 : 1.10 + 0.03 * r;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        Building unkept = null;
        for (Building b : t.buildings) {
            if (temple(b.type) && b.complete && !b.demolishing && Village.staffOf(t, b) == null) {
                unkept = b;
                break;
            }
        }
        if (unkept == null) return;
        // A priest already in the village takes it up; otherwise one is sent for.
        for (Resident r : t.residents) {
            if (r.occupation == Occupation.PRIEST && !r.militia && r.office < 0 && !r.follows && !Village.isStaffing(t, r) && r.trainingAt < 0) {
                Village.assign(t, r, Job.STAFF, unkept.id);
                return;
            }
        }
        if (t.treasury < INVITE_COST || Village.openBeds(t) <= 0) return;
        t.treasury -= INVITE_COST;
        Resident priest = Resident.random(rnd, Occupation.PRIEST, t.grade.ordinal());
        priest.rank = rnd.nextInt(4) == 0 ? 1 : 0;
        Village.add(t, priest);
        Village.assign(t, priest, Job.STAFF, unkept.id);
        Village.tell(level, t, Component.translatable("realmlord.temple.priest_came", priest.name, t.name,
                Component.translatable(unkept.type.key()), Coins.format(INVITE_COST)));
    }
}
