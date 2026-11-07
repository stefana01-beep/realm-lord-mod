package com.realmlord.village;

import com.realmlord.loot.Gear;
import com.realmlord.registry.ModArms;
import com.realmlord.registry.ModItems;
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

    /**
     * One thing a building can do: make stock for the market, make items for the storehouse
     * (spending stock, and sometimes materials kept in the storehouse), or, for a school, the
     * dojo or the military camp, a discipline to teach or drill. Some need a manual learned first.
     *
     * @param out  for stock, units a day; for items, how many are made a day
     * @param each how many of the item each making yields
     */
    public record Mode(String id, Goods input, double in, Goods output, double out, Item[] items, String plan,
                       java.util.function.Supplier<Item> need, int needN, int each) {
        public Mode(String id, Goods input, double in, Goods output, double out, Item[] items) {
            this(id, input, in, output, out, items, null, null, 0, 1);
        }

        public String key() {
            return "realmlord.workshop.mode." + id;
        }

        public boolean makesItems() {
            return items != null;
        }

        /** A discipline: taught or drilled, it makes nothing. */
        public boolean discipline() {
            return items == null && output == null;
        }

        public Mode plan(com.realmlord.loot.Manuals.Plan p) {
            return new Mode(id, input, in, output, out, items, p.id(), need, needN, each);
        }

        public Mode need(java.util.function.Supplier<Item> item, int n) {
            return new Mode(id, input, in, output, out, items, plan, item, n, each);
        }

        public Mode each(int n) {
            return new Mode(id, input, in, output, out, items, plan, need, needN, n);
        }

        public boolean known(Territory t) {
            return plan == null || t.plans.contains(plan);
        }
    }

    private static Mode items(String id, Goods in, double inN, double perDay, Item... items) {
        return new Mode(id, in, inN, null, perDay, items);
    }

    private static Mode stock(String id, Goods in, double inN, Goods out, double outN) {
        return new Mode(id, in, inN, out, outN, null);
    }

    private static Mode discipline(String id) {
        return new Mode(id, null, 0, null, 0, null);
    }

    private static final Item[] IRON_GEAR = {Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS};
    private static final Item[] FINE_WEAPONS = {Items.IRON_SWORD, Items.IRON_AXE, Items.BOW, Items.CROSSBOW};
    private static final Item[] LEATHER_GEAR = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
    private static final Item[] REMEDIES = {Items.POTION};

    private static Item[] of(java.util.List<net.minecraftforge.registries.RegistryObject<Item>> l) {
        Item[] out = new Item[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = l.get(i).get();
        return out;
    }

    /** The things a building can do, in order (the first is what it does unless told otherwise). */
    public static List<Mode> modes(BuildingType type) {
        List<Mode> out = new ArrayList<>();
        switch (type) {
            case BLACKSMITH_WORKSHOP -> {
                out.add(stock("tools", Goods.IRON_ORE, 10, Goods.TOOLS, 5));
                out.add(items("iron_gear", Goods.IRON_ORE, 12, 1, IRON_GEAR));
                out.add(items("black_iron", Goods.IRON_ORE, 6, 2, ModArms.BLACK_IRON_INGOT.get()));
                out.add(items("whetstones", Goods.STONE, 4, 2, ModArms.WHETSTONE.get()).each(2));
                out.add(items("lamellar_plates", Goods.IRON_ORE, 8, 1, ModArms.LAMELLAR_PLATES.get()).each(4).need(ModArms.BEAST_SINEW, 1));
                out.add(items("black_iron_arms", Goods.IRON_ORE, 14, 1, of(List.of(ModArms.BLACK_IRON_SABRE, ModArms.RING_DAO))));
                out.add(items("lamellar_armour", Goods.IRON_ORE, 16, 1, of(List.of(ModArms.LAMELLAR_HELMET, ModArms.LAMELLAR_COAT,
                        ModArms.LAMELLAR_TASSETS, ModArms.LAMELLAR_BOOTS))).need(ModArms.BEAST_SINEW, 2).plan(com.realmlord.loot.Manuals.LAMELLAR));
                out.add(items("horse_armour", Goods.IRON_ORE, 16, 1, Items.IRON_HORSE_ARMOR));
            }
            case WEAPONS_WORKSHOP -> {
                out.add(stock("weapons", Goods.IRON_ORE, 9, Goods.WEAPONS, 3));
                out.add(items("fine_weapons", Goods.IRON_ORE, 15, 1, FINE_WEAPONS));
                out.add(items("arrows", Goods.WOOD, 4, 4, Items.ARROW).each(16));
                out.add(items("refined_steel", Goods.IRON_ORE, 24, 1, ModArms.REFINED_STEEL.get()));
                out.add(items("steel_blades", Goods.IRON_ORE, 30, 1, of(List.of(ModArms.STEEL_JIAN, ModArms.RING_DAO))));
                out.add(items("bows", Goods.WOOD, 8, 1, ModArms.TIGER_BONE_BOW.get()).need(ModItems.TIGER_BONE, 1));
                out.add(items("polearms", Goods.IRON_ORE, 30, 1, of(List.of(ModArms.BLACK_IRON_SPEAR, ModArms.GUANDAO, ModArms.JI)))
                        .plan(com.realmlord.loot.Manuals.POLEARMS));
                out.add(items("mountain_armour", Goods.IRON_ORE, 40, 1, of(List.of(ModArms.MOUNTAIN_HELM, ModArms.MOUNTAIN_ARMOUR,
                        ModArms.MOUNTAIN_GREAVES, ModArms.MOUNTAIN_BOOTS))).plan(com.realmlord.loot.Manuals.MOUNTAIN_PATTERN));
                out.add(items("tiger_soul", Goods.IRON_ORE, 30, 1, ModArms.TIGER_SOUL_SABRE.get()).need(ModArms.BEAST_CORE, 1)
                        .plan(com.realmlord.loot.Manuals.TIGER_WAR));
                out.add(items("heavy_bolts", Goods.IRON_ORE, 4, 2, com.realmlord.registry.ModCrafts.HEAVY_BOLT.get()).each(4));
                out.add(items("stone_shots", Goods.STONE, 4, 2, com.realmlord.registry.ModCrafts.STONE_SHOT.get()).each(2));
                out.add(items("siege_ladders", Goods.WOOD, 10, 2, com.realmlord.registry.ModCrafts.SIEGE_LADDER.get()));
                out.add(items("siege_engines", Goods.WOOD, 60, 0.5, com.realmlord.registry.ModCrafts.BED_CROSSBOW.get(),
                        com.realmlord.registry.ModCrafts.TREBUCHET.get()).plan(com.realmlord.loot.Manuals.SIEGE_ENGINES));
            }
            case TAILOR_WORKSHOP -> {
                out.add(stock("linen", null, 0, Goods.LINEN, 2));
                out.add(items("leather_armour", Goods.LEATHER, 4, 1, LEATHER_GEAR));
                out.add(items("hide_armour", Goods.LEATHER, 6, 1, of(List.of(ModArms.HIDE_HELMET, ModArms.HIDE_JERKIN, ModArms.HIDE_LEGGINGS,
                        ModArms.HIDE_BOOTS))).need(ModArms.BEAST_SINEW, 1));
                out.add(items("banners", Goods.LINEN, 1, 2, Items.RED_BANNER));
                out.add(items("bedding", Goods.LINEN, 1, 2, Items.WHITE_BED));
                out.add(items("tiger_war_coat", Goods.LEATHER, 8, 1, of(List.of(ModArms.TIGER_HOOD, ModArms.TIGER_WAR_COAT)))
                        .need(ModItems.TIGER_PELT, 2).plan(com.realmlord.loot.Manuals.TIGER_WAR));
            }
            case CLINIC -> {
                out.add(stock("herbs", null, 0, Goods.HERBS, 10));
                out.add(items("remedies", Goods.HERBS, 6, 2, REMEDIES));
                out.add(items("wound_powder", Goods.HERBS, 3, 3, ModArms.GOLDEN_WOUND_POWDER.get()).each(2));
                out.add(items("healing_pills", Goods.HERBS, 8, 2, ModArms.HEALING_PILL.get()).need(ModArms.BEAST_SINEW, 1)
                        .plan(com.realmlord.loot.Manuals.ALCHEMY));
                out.add(items("marrow_pills", Goods.HERBS, 40, 1, ModArms.MARROW_CLEANSING_PILL.get()).need(ModArms.BEAST_CORE, 1)
                        .plan(com.realmlord.loot.Manuals.ALCHEMY));
                out.add(items("enlightenment_pills", Goods.HERBS, 40, 1, ModArms.ENLIGHTENMENT_PILL.get()).need(ModArms.BEAST_CORE, 1)
                        .plan(com.realmlord.loot.Manuals.ALCHEMY));
            }
            case WINE_WORKSHOP -> {
                out.add(stock("wine", Goods.GRAIN, 60, Goods.WINE, 20));
                out.add(items("tiger_bone_wine", Goods.GRAIN, 20, 1, ModItems.TIGER_BONE_WINE.get()).each(3).need(ModItems.TIGER_BONE, 1));
                out.add(items("bottled_wine", Goods.GRAIN, 15, 1, ModItems.THREE_FLOWER_WINE.get()).each(4));
            }
            case GROCERY_SHOP -> {
                out.add(stock("salt", null, 0, Goods.SALT, 20));
                out.add(items("rations", Goods.GRAIN, 8, 3, ModArms.MILITARY_RATIONS.get()).each(2).plan(com.realmlord.loot.Manuals.RATIONS));
                out.add(items("cured_meat", Goods.MEAT, 4, 2, Items.COOKED_BEEF).each(2));
                out.add(items("dried_fish", Goods.FISH, 4, 2, Items.COOKED_COD).each(2));
            }
            case POTTERY_WORKSHOP -> {
                out.add(stock("pottery", Goods.STONE, 20, Goods.POTTERY, 20));
                out.add(items("bricks", Goods.CLAY, 4, 4, Items.BRICKS).each(4));
                out.add(items("roof_tiles", Goods.CLAY, 4, 4, Items.TERRACOTTA).each(4));
                out.add(items("flower_pots", Goods.CLAY, 2, 2, Items.FLOWER_POT, Items.DECORATED_POT));
            }
            case STABLE -> {
                out.add(discipline("tend_horses"));
                out.add(items("saddles", Goods.LEATHER, 5, 1, Items.SADDLE));
                out.add(items("leads", Goods.LEATHER, 2, 1, Items.LEAD).each(2));
                out.add(items("leather_barding", Goods.LEATHER, 6, 1, Items.LEATHER_HORSE_ARMOR));
            }
            case MARTIAL_ARTS_DOJO -> {
                out.add(discipline("fist_and_palm"));
                out.add(discipline("weapon_forms"));
                out.add(discipline("internal_arts"));
                out.add(discipline("archery"));
                out.add(discipline("qinggong"));
            }
            case PRIVATE_SCHOOL -> {
                out.add(discipline("classics"));
                out.add(discipline("statecraft"));
                out.add(discipline("strategy"));
                out.add(discipline("medicine"));
                out.add(discipline("accounts"));
            }
            case ACADEMY -> {
                out.add(discipline("exam_prep"));
                out.add(discipline("classics"));
                out.add(discipline("statecraft"));
                out.add(discipline("strategy"));
                out.add(discipline("medicine"));
                out.add(discipline("accounts"));
            }
            case VILLAGE_TEMPLE, TEMPLE -> {
                out.add(discipline("blessing"));
                out.add(discipline("warding"));
                out.add(discipline("rain_prayer"));
                out.add(items("elixirs", Goods.HERBS, 10, 2, com.realmlord.registry.ModCrafts.CLARITY_PILL.get(), ModArms.HEALING_PILL.get()));
                out.add(items("talismans", Goods.LINEN, 1, 2, com.realmlord.registry.ModCrafts.GUARDIAN_CHARM.get(),
                        com.realmlord.registry.ModCrafts.SPRING_TALISMAN.get()));
            }
            case SHIPYARD -> {
                out.add(items("fishing_boats", Goods.WOOD, 50, 1, com.realmlord.registry.ModCrafts.FISHING_BOAT.get()));
                out.add(items("cima_boats", Goods.WOOD, 80, 0.5, com.realmlord.registry.ModCrafts.CIMA_BOAT.get()));
                out.add(items("merchant_ships", Goods.WOOD, 300, 0.25, com.realmlord.registry.ModCrafts.MERCHANT_SHIP.get())
                        .need(com.realmlord.registry.ModCrafts.PINE_TAR, 2));
                out.add(items("mengchong_warships", Goods.WOOD, 400, 1.0 / 3, com.realmlord.registry.ModCrafts.MENGCHONG_WARSHIP.get())
                        .need(com.realmlord.registry.ModCrafts.PINE_TAR, 4).plan(com.realmlord.loot.Manuals.WARSHIPS));
                out.add(items("turreted_ships", Goods.WOOD, 1200, 0.1, com.realmlord.registry.ModCrafts.TURRETED_SHIP.get())
                        .need(com.realmlord.registry.ModCrafts.PINE_TAR, 10).plan(com.realmlord.loot.Manuals.WARSHIPS));
            }
            case CHAMBER_OF_COMMERCE -> {
                for (String c : com.realmlord.worldgen.ChinaMap.CAPITALS) out.add(discipline("route_" + c.toLowerCase(java.util.Locale.ROOT)));
            }
            case MILITARY_CAMP -> {
                out.add(discipline("drill"));
                out.add(discipline("formation"));
                out.add(discipline("weapons_practice"));
                out.add(discipline("archery_drill"));
                out.add(discipline("patrol"));
                out.add(discipline("rest"));
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
            if (m == null || !m.makesItems() || !m.known(t)) continue;
            Resident master = b.type.staff() != null ? Village.staffOf(t, b) : null;
            if (b.type.staff() != null && master == null) continue;
            int count = (int) m.out();
            if (m.out() < 1) {
                // Big things (ships) take days: the work carries over until one is done.
                if (b.modeCarry < 1) b.modeCarry += m.out() * (master != null && master.isFigure() ? 1.5 : 1.0 + 0.15 * (master == null ? 0 : master.rank));
                count = b.modeCarry >= 1 ? 1 : 0;
            }
            for (int n = 0; n < count; n++) {
                if (m.input() != null && m.input().stock(t) < m.in()) break;
                if (m.need() != null && t.storage.countItem(m.need().get()) < m.needN()) break;
                ItemStack made = makeOne(m, master, rnd);
                if (!fits(t.storage, made)) break;
                if (m.input() != null) m.input().add(t, -(long) m.in());
                if (m.need() != null) take(t.storage, m.need().get(), m.needN());
                t.storage.addItem(made);
                if (m.out() < 1) b.modeCarry = Math.max(0, b.modeCarry - 1);
            }
        }
    }

    private static ItemStack makeOne(Mode m, Resident master, RandomSource rnd) {
        Item item = m.items()[rnd.nextInt(m.items().length)];
        if (item == Items.POTION) {
            return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),
                    master != null && master.rank >= 2 ? net.minecraft.world.item.alchemy.Potions.STRONG_HEALING : net.minecraft.world.item.alchemy.Potions.HEALING);
        }
        if (!graded(item)) return new ItemStack(item, m.each());
        TokenGrade base = ModArms.craftedGrade(item);
        return Gear.make(item, base == null ? gradeFor(master, rnd) : gradeAround(base, master, rnd), rnd);
    }

    /** Weapons and armour come out graded; materials, food and the like as they are. */
    public static boolean graded(Item item) {
        return item instanceof net.minecraft.world.item.ArmorItem && !(item instanceof net.minecraft.world.item.HorseArmorItem)
                || item instanceof net.minecraft.world.item.TieredItem || item instanceof net.minecraft.world.item.BowItem
                || item instanceof net.minecraft.world.item.CrossbowItem;
    }

    /** A piece with a grade of its own: a Junior master sometimes falls a grade short, an Advanced one sometimes rises one above. */
    public static TokenGrade gradeAround(TokenGrade base, Resident master, RandomSource rnd) {
        int rank = master == null ? 0 : master.rank + (master.isFigure() ? 1 : 0);
        int g = base.ordinal();
        if (rank == 0 && rnd.nextInt(3) == 0) g--;
        else if (rank >= 2 && rnd.nextInt(10) == 0) g++;
        return TokenGrade.values()[Math.max(0, Math.min(TokenGrade.values().length - 1, g))];
    }

    private static boolean fits(net.minecraft.world.SimpleContainer c, ItemStack s) {
        int room = 0;
        for (int i = 0; i < c.getContainerSize() && room < s.getCount(); i++) {
            ItemStack h = c.getItem(i);
            if (h.isEmpty()) room += s.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(h, s)) room += h.getMaxStackSize() - h.getCount();
        }
        return room >= s.getCount();
    }

    private static void take(net.minecraft.world.SimpleContainer c, Item item, int n) {
        for (int i = 0; i < c.getContainerSize() && n > 0; i++) {
            ItemStack h = c.getItem(i);
            if (!h.is(item)) continue;
            int k = Math.min(n, h.getCount());
            h.shrink(k);
            n -= k;
            if (h.isEmpty()) c.setItem(i, ItemStack.EMPTY);
        }
        c.setChanged();
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
            case PRIVATE_SCHOOL, MARTIAL_ARTS_DOJO, ACADEMY -> 10;
            default -> type.staff() != null && type != BuildingType.MILITARY_CAMP ? 3 : 0;
        };
    }

    public static Occupation teaches(BuildingType type) {
        return places(type) > 0 ? type.staff() : null;
    }

    /**
     * What a building's learners become, by what it teaches now: the school's classics make
     * scholars, statecraft civil servants, strategy officers, medicine doctors and accounts
     * grocers; the dojo's archery makes hunters, its other forms martial artists.
     */
    public static Occupation teaches(Building b) {
        Occupation base = teaches(b.type);
        if (base == null) return null;
        Mode m = mode(b);
        if (m == null) return base;
        return switch (m.id()) {
            case "statecraft" -> Occupation.CIVIL_SERVANT;
            case "strategy" -> Occupation.OFFICER;
            case "medicine" -> Occupation.DOCTOR;
            case "accounts" -> Occupation.GROCER;
            case "archery" -> Occupation.HUNTER;
            default -> base;
        };
    }

    /** Days of lessons to learn the trade (or, for one already of it, to rise a rank). */
    public static int daysToLearn(BuildingType type, Resident r) {
        if (r.occupation == teaches(type)) return 30;
        return type == BuildingType.PRIVATE_SCHOOL || type == BuildingType.MARTIAL_ARTS_DOJO || type == BuildingType.ACADEMY ? 20 : 15;
    }

    public static int daysToLearn(Building b, Resident r) {
        Occupation trade = teaches(b);
        if (r.occupation == trade) return 30;
        Mode m = mode(b);
        if (m != null && (m.id().equals("internal_arts") || m.id().equals("strategy"))) return 25;
        if (m != null && m.id().equals("qinggong")) return 15;
        return daysToLearn(b.type, r);
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
        if (r.occupation == teaches(b) && r.rank >= 2) return "realmlord.workshop.err.master";
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

    /** What a day's lessons build, by what is taught. */
    static void lesson(Building b, Resident r, RandomSource rnd) {
        Mode m = mode(b);
        String d = m == null ? "" : m.id();
        if (b.type == BuildingType.ACADEMY) {
            // The academy teaches as the school does, half again as well.
            Building school = new Building();
            school.type = BuildingType.PRIVATE_SCHOOL;
            school.mode = Math.max(0, b.mode - 1);
            lesson(school, r, rnd);
            if (rnd.nextBoolean()) lesson(school, r, rnd);
            return;
        }
        if (b.type == BuildingType.PRIVATE_SCHOOL) {
            switch (d) {
                case "statecraft" -> {
                    if (r.politics < 90) r.politics++;
                    if (r.intelligence < 85 && rnd.nextBoolean()) r.intelligence++;
                }
                case "strategy" -> {
                    if (r.command < 85) r.command++;
                    if (r.intelligence < 85 && rnd.nextBoolean()) r.intelligence++;
                }
                case "accounts" -> {
                    if (r.politics < 80 && rnd.nextBoolean()) r.politics++;
                    if (r.intelligence < 80 && rnd.nextBoolean()) r.intelligence++;
                }
                case "medicine" -> {
                    if (r.intelligence < 90) r.intelligence++;
                }
                default -> {
                    if (r.intelligence < 85) r.intelligence++;
                    if (r.politics < 85 && rnd.nextBoolean()) r.politics++;
                }
            }
        } else if (b.type == BuildingType.MARTIAL_ARTS_DOJO) {
            switch (d) {
                case "weapon_forms" -> {
                    if (r.force < 85 && rnd.nextBoolean()) r.force++;
                    r.gainSoldierXp(80);
                }
                case "internal_arts" -> {
                    if (r.force < 92) r.force++;
                    if (r.force < 92 && rnd.nextBoolean()) r.force++;
                    r.gainSoldierXp(15);
                }
                case "archery" -> {
                    if (r.force < 80 && rnd.nextBoolean()) r.force++;
                    r.gainSoldierXp(50);
                }
                case "qinggong" -> {
                    if (r.force < 80 && rnd.nextInt(3) == 0) r.force++;
                    r.gainSoldierXp(30);
                }
                default -> {
                    if (r.force < 85) r.force++;
                    r.gainSoldierXp(40);
                }
            }
        }
    }

    /** The military camp's drill today (its chosen discipline), or "drill" with no camp. */
    public static String drillOf(Territory t) {
        for (Building b : t.buildings) {
            if (b.type != BuildingType.MILITARY_CAMP || !b.complete || b.demolishing) continue;
            Mode m = mode(b);
            if (m != null) return m.id();
        }
        return "drill";
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
            r.trainingDays += 2 + master.rank + (master.isFigure() ? 2 : 0) + (b.type == BuildingType.ACADEMY ? 1 : 0);
            lesson(b, r, rnd);
            if (r.trainingDays < daysToLearn(b, r) * 2) continue;
            Occupation trade = teaches(b);
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
