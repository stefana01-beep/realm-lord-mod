package com.realmlord.registry;

import com.realmlord.RealmLord;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Arms and materials of the novel's world, made from what beasts and bandits leave behind:
 * sinew and beast cores, black iron and refined steel, the sabre, spear, straight sword and
 * guandao, a tiger-bone bow, and hide and black iron armour. Crafted pieces come out graded
 * (hide Bronze, black iron Black Iron, steel Silver; forged again with a beast core, Gold), and
 * the manor's market buys and sells them.
 */
public final class ModArms {
    private ModArms() {}

    /** Loads this class so its items are registered with the rest. */
    public static void init() {}

    // ------------------------------------------------------------------ materials

    public static final RegistryObject<Item> BEAST_SINEW = ModItems.ITEMS.register("beast_sinew", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BEAST_CORE = ModItems.ITEMS.register("beast_core", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BANDIT_INSIGNIA = ModItems.ITEMS.register("bandit_insignia", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_IRON_INGOT = ModItems.ITEMS.register("black_iron_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REFINED_STEEL = ModItems.ITEMS.register("refined_steel", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ------------------------------------------------------------------ tiers

    public static final Tier BLACK_IRON = new SimpleTier(600, 7.0F, 2.5F, 2, 12, () -> Ingredient.of(BLACK_IRON_INGOT.get()));
    public static final Tier STEEL = new SimpleTier(1200, 8.0F, 3.5F, 3, 14, () -> Ingredient.of(REFINED_STEEL.get()));

    private record SimpleTier(int uses, float speed, float damage, int level, int enchant, java.util.function.Supplier<Ingredient> repair) implements Tier {
        public int getUses() { return uses; }
        public float getSpeed() { return speed; }
        public float getAttackDamageBonus() { return damage; }
        public int getLevel() { return level; }
        public int getEnchantmentValue() { return enchant; }
        public Ingredient getRepairIngredient() { return repair.get(); }
    }

    // ------------------------------------------------------------------ weapons

    public static final RegistryObject<Item> BANDIT_SABRE = ModItems.ITEMS.register("bandit_sabre",
            () -> new SwordItem(net.minecraft.world.item.Tiers.IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> BLACK_IRON_SABRE = ModItems.ITEMS.register("black_iron_sabre",
            () -> new SwordItem(BLACK_IRON, 3, -2.2F, new Item.Properties()));
    public static final RegistryObject<Item> BLACK_IRON_SPEAR = ModItems.ITEMS.register("black_iron_spear",
            () -> new SwordItem(BLACK_IRON, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_JIAN = ModItems.ITEMS.register("steel_jian",
            () -> new SwordItem(STEEL, 3, -2.2F, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GUANDAO = ModItems.ITEMS.register("guandao",
            () -> new SwordItem(STEEL, 6, -3.2F, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TIGER_BONE_BOW = ModItems.ITEMS.register("tiger_bone_bow",
            () -> new TigerBoneBow(new Item.Properties().durability(600).rarity(Rarity.UNCOMMON)));

    /** A bow of tiger bone and sinew: its arrows strike harder than a plain bow's. */
    public static class TigerBoneBow extends BowItem {
        public TigerBoneBow(Properties p) {
            super(p);
        }

        @Override
        public AbstractArrow customArrow(AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.5);
            return arrow;
        }
    }

    // ------------------------------------------------------------------ armour

    public static final ArmorMaterial HIDE = new Material("hide", 12, new int[]{1, 4, 5, 2}, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F,
            () -> Ingredient.of(ModItems.WOLF_PELT.get(), ModItems.BOAR_HIDE.get(), ModItems.LEOPARD_PELT.get(), ModItems.TIGER_PELT.get()));
    public static final ArmorMaterial BLACK_IRON_ARMOUR = new Material("black_iron", 22, new int[]{2, 6, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_IRON, 1.0F,
            () -> Ingredient.of(BLACK_IRON_INGOT.get()));

    /** defence: boots, leggings, chestplate, helmet. */
    private record Material(String id, int durability, int[] defence, int enchant, SoundEvent sound, float toughness,
                            java.util.function.Supplier<Ingredient> repair) implements ArmorMaterial {
        private static final int[] BASE = {13, 15, 16, 11};

        public int getDurabilityForType(ArmorItem.Type t) {
            return BASE[slot(t)] * durability;
        }

        public int getDefenseForType(ArmorItem.Type t) {
            return defence[slot(t)];
        }

        private static int slot(ArmorItem.Type t) {
            return switch (t) {
                case BOOTS -> 0;
                case LEGGINGS -> 1;
                case CHESTPLATE -> 2;
                case HELMET -> 3;
            };
        }

        public int getEnchantmentValue() { return enchant; }
        public SoundEvent getEquipSound() { return sound; }
        public Ingredient getRepairIngredient() { return repair.get(); }
        public String getName() { return RealmLord.MODID + ":" + id; }
        public float getToughness() { return toughness; }
        public float getKnockbackResistance() { return 0.0F; }
    }

    public static final RegistryObject<Item> HIDE_HELMET = armour("hide_helmet", HIDE, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> HIDE_JERKIN = armour("hide_jerkin", HIDE, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> HIDE_LEGGINGS = armour("hide_leggings", HIDE, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> HIDE_BOOTS = armour("hide_boots", HIDE, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> BLACK_IRON_HELMET = armour("black_iron_helmet", BLACK_IRON_ARMOUR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> BLACK_IRON_CUIRASS = armour("black_iron_cuirass", BLACK_IRON_ARMOUR, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> BLACK_IRON_GREAVES = armour("black_iron_greaves", BLACK_IRON_ARMOUR, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BLACK_IRON_BOOTS = armour("black_iron_boots", BLACK_IRON_ARMOUR, ArmorItem.Type.BOOTS);

    private static RegistryObject<Item> armour(String id, ArmorMaterial m, ArmorItem.Type t) {
        return ModItems.ITEMS.register(id, () -> new ArmorItem(m, t, new Item.Properties()));
    }

    // ------------------------------------------------------------------ groups

    public static List<RegistryObject<Item>> materials() {
        return List.of(BEAST_SINEW, BEAST_CORE, BANDIT_INSIGNIA, BLACK_IRON_INGOT, REFINED_STEEL);
    }

    public static List<RegistryObject<Item>> arms() {
        return List.of(BANDIT_SABRE, BLACK_IRON_SABRE, BLACK_IRON_SPEAR, STEEL_JIAN, GUANDAO, TIGER_BONE_BOW,
                HIDE_HELMET, HIDE_JERKIN, HIDE_LEGGINGS, HIDE_BOOTS, BLACK_IRON_HELMET, BLACK_IRON_CUIRASS, BLACK_IRON_GREAVES, BLACK_IRON_BOOTS);
    }

    /** The grade a crafted piece comes out at. */
    public static com.realmlord.territory.TokenGrade craftedGrade(Item item) {
        if (item == HIDE_HELMET.get() || item == HIDE_JERKIN.get() || item == HIDE_LEGGINGS.get() || item == HIDE_BOOTS.get()) return com.realmlord.territory.TokenGrade.BRONZE;
        if (item == STEEL_JIAN.get() || item == GUANDAO.get()) return com.realmlord.territory.TokenGrade.SILVER;
        if (item == BLACK_IRON_SABRE.get() || item == BLACK_IRON_SPEAR.get() || item == TIGER_BONE_BOW.get() || item == BLACK_IRON_HELMET.get()
                || item == BLACK_IRON_CUIRASS.get() || item == BLACK_IRON_GREAVES.get() || item == BLACK_IRON_BOOTS.get()) return com.realmlord.territory.TokenGrade.BLACK_IRON;
        return null;
    }
}
