package com.realmlord.village;

import com.realmlord.loot.Gear;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TokenGrade;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Running the village's workshops and schools from their own windows: what each workshop makes,
 * and who it trains.
 *
 * <p><b>Making.</b> Some workshops can turn their hand to more than one thing. The smithy makes
 * tools for the market or forges iron arms and armour for the Lord; the weapons workshop makes
 * weapons stock or fine graded blades; the tailor weaves linen or sews leather armour; the clinic
 * gathers herbs or brews healing draughts. Things made as items go into the manor storehouse,
 * graded by the master's skill (Bronze for a Junior, up to Silver and now and then Gold for an
 * Advanced master).
 *
 * <p><b>Training.</b> A school takes up to ten students, the dojo ten pupils, and a workshop three
 * apprentices, taught by its master (who must be at work). Students grow in Intelligence and
 * Politics and in twenty days become scholars; pupils grow in Force and as soldiers, and become
 * martial artists; apprentices learn the master's trade in fifteen days, and someone already of
 * that trade rises a rank in thirty. A better master teaches faster.
 */
public final class Workshops {
    private Workshops() {}

    /** One thing a workshop can make: stock for the market, or items for the storehouse. */
    public record Mode(String id, Goods input, double in, Goods output, double out, Item[] items) {
        public String key() {
            return "realmlord.workshop.mode." + id;
        }

        public boolean makesItems() {
            return items != null;
        }
    }

    private static final Item[] IRON_GEAR = {Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS};
    private static final Item[] FINE_WEAPONS = {Items.IRON_SWORD, Items.IRON_AXE, Items.BOW, Items.CROSSBOW};
    private static final Item[] LEATHER_GEAR = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
    private static final Item[] REMEDIES = {Items.POTION};

    /** The things a building can make, in order (the first is what it makes unless told otherwise). */
    public static List<Mode> modes(BuildingType type) {
        List<Mode> out = new ArrayList<>();
        switch (type) {
            case BLACKSMITH_WORKSHOP -> {
                out.add(new Mode("tools", Goods.IRON_ORE, 10, Goods.TOOLS, 5, null));
                out.add(new Mode("iron_gear", Goods.IRON_ORE, 12, null, 1, IRON_GEAR));
            }
            case WEAPONS_WORKSHOP -> {
                out.add(new Mode("weapons", Goods.IRON_ORE, 9, Goods.WEAPONS, 3, null));
                out.add(new Mode("fine_weapons", Goods.IRON_ORE, 15, null, 1, FINE_WEAPONS));
            }
            case TAILOR_WORKSHOP -> {
                out.add(new Mode("linen", null, 0, Goods.LINEN, 2, null));
                out.add(new Mode("leather_armour", Goods.LEATHER, 4, null, 1, LEATHER_GEAR));
            }
            case CLINIC -> {
                out.add(new Mode("herbs", null, 0, Goods.HERBS, 10, null));
                out.add(new Mode("remedies", Goods.HERBS, 6, null, 2, REMEDIES));
            }
            default -> { }
        }
        return out;
    }

    public static Mode mode(Building b) {
        List<Mode> m = modes(b.type);
        if (m.isEmpty()) return null;
        return m.get(Math.max(0, Math.min(m.size() - 1, b.mode)));
    }

    /** True if the building's market stock recipe (in Goods) is what it is making now. */
    public static boolean makingStock(Building b) {
        Mode m = mode(b);
        return m == null || !m.makesItems();
    }

    /** One day of item-making: forged and sewn things go into the storehouse, graded by the master. */
    public static void make(Territory t, RandomSource rnd) {
        for (Building b : t.buildings) {
            if (!b.complete || b.demolishing) continue;
            Mode m = mode(b);
            if (m == null || !m.makesItems()) continue;
            Resident master = b.type.staff() != null ? Village.staffOf(t, b) : null;
            if (b.type.staff() != null && master == null) continue;
            for (int n = 0; n < (int) m.out(); n++) {
                if (m.input() != null) {
                    if (m.input().stock(t) < m.in()) break;
                    m.input().add(t, -(long) m.in());
                }
                ItemStack made = makeOne(m, master, rnd);
                ItemStack left = t.storage.addItem(made);
                if (!left.isEmpty()) break;
            }
        }
    }

    private static ItemStack makeOne(Mode m, Resident master, RandomSource rnd) {
        Item item = m.items()[rnd.nextInt(m.items().length)];
        if (item == Items.POTION) {
            return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),
                    master != null && master.rank >= 2 ? net.minecraft.world.item.alchemy.Potions.STRONG_HEALING : net.minecraft.world.item.alchemy.Potions.HEALING);
        }
        return Gear.make(item, gradeFor(master, rnd), rnd);
    }

    /** What grade a master turns out: a Junior Bronze, an Intermediate Black Iron, an Advanced Silver (now and then Gold). */
    public static TokenGrade gradeFor(Resident master, RandomSource rnd) {
        int rank = master == null ? 0 : master.rank + (master.isFigure() ? 1 : 0);
        int g = Math.min(2, rank);
        if (rank >= 2 && rnd.nextInt(10) == 0) g = 3;
        else if (rnd.nextInt(5) == 0 && g > 0) g--;
        return TokenGrade.values()[g];
    }

    // ------------------------------------------------------------------ training

    /** What a building teaches: the trade it makes of its learners, and how many it takes at once (0: it doesn't teach). */
    public static int places(BuildingType type) {
        return switch (type) {
            case PRIVATE_SCHOOL, MARTIAL_ARTS_DOJO -> 10;
            default -> type.staff() != null && type != BuildingType.MILITARY_CAMP ? 3 : 0;
        };
    }

    public static Occupation teaches(BuildingType type) {
        return places(type) > 0 ? type.staff() : null;
    }

    /** Days of lessons to learn the trade (or, for one already of it, to rise a rank). */
    public static int daysToLearn(BuildingType type, Resident r) {
        if (r.occupation == teaches(type)) return 30;
        return type == BuildingType.PRIVATE_SCHOOL || type == BuildingType.MARTIAL_ARTS_DOJO ? 20 : 15;
    }

    public static List<Resident> learners(Territory t, Building b) {
        List<Resident> out = new ArrayList<>();
        for (Resident r : t.residents) if (r.trainingAt == b.id) out.add(r);
        return out;
    }

    /** Who could be sent to learn here: anyone not a soldier, an official, a companion or busy running a building. */
    public static boolean canLearn(Territory t, Resident r) {
        return !r.militia && r.office < 0 && !r.follows && r.trainingAt < 0 && !r.isFigure() && !Village.isStaffing(t, r);
    }

    /** Sends a resident to learn at a building; returns an error key or null. */
    public static String enrol(Territory t, Building b, Resident r) {
        if (!b.complete || places(b.type) == 0) return "realmlord.workshop.err.no_lessons";
        if (!canLearn(t, r)) return "realmlord.workshop.err.busy";
        if (learners(t, b).size() >= places(b.type)) return "realmlord.workshop.err.full";
        if (r.occupation == teaches(b.type) && r.rank >= 2) return "realmlord.workshop.err.master";
        r.trainingAt = b.id;
        r.trainingDays = 0;
        r.job = Job.IDLE;
        r.workplace = -1;
        return null;
    }

    public static void dismiss(Resident r) {
        r.trainingAt = -1;
        r.trainingDays = 0;
    }

    /**
     * A day of lessons. Progress is counted in half days: two a day, and one more for each rank
     * of the master. Returns the residents who finished, with what they became.
     */
    public static List<String> teach(Territory t, RandomSource rnd) {
        List<String> done = new ArrayList<>();
        for (Resident r : t.residents) {
            if (r.trainingAt < 0) continue;
            Building b = t.building(r.trainingAt);
            if (b == null || !b.complete || b.demolishing) {
                dismiss(r);
                continue;
            }
            Resident master = Village.staffOf(t, b);
            if (master == null) continue;
            r.trainingDays += 2 + master.rank + (master.isFigure() ? 2 : 0);
            // What the lessons build day by day.
            if (b.type == BuildingType.PRIVATE_SCHOOL) {
                if (r.intelligence < 85) r.intelligence++;
                if (r.politics < 85 && rnd.nextBoolean()) r.politics++;
            } else if (b.type == BuildingType.MARTIAL_ARTS_DOJO) {
                if (r.force < 85) r.force++;
                r.gainSoldierXp(40);
            }
            if (r.trainingDays < daysToLearn(b.type, r) * 2) continue;
            Occupation trade = teaches(b.type);
            if (r.occupation == trade) r.rank = Math.min(2, r.rank + 1);
            else {
                r.occupation = trade;
                r.rank = 0;
            }
            dismiss(r);
            done.add(r.name);
        }
        return done;
    }
}
