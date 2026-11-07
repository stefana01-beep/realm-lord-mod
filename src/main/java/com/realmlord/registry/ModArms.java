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
    public static final RegistryObject<Item> BANDIT_INSIGNIA = ModItems.ITEMS.register("bandit_insignia", () -> new com.realmlord.item.InsigniaItem(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_IRON_INGOT = ModItems.ITEMS.register("black_iron_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REFINED_STEEL = ModItems.ITEMS.register("refined_steel", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Small black iron scales, punched for lacing: what lamellar armour is sewn from. */
    public static final RegistryObject<Item> LAMELLAR_PLATES = ModItems.ITEMS.register("lamellar_plates", () -> new Item(new Item.Properties()));
    /** A whetstone: hones the blade (or mends the armour) held in the other hand. */
    public static final RegistryObject<Item> WHETSTONE = ModItems.ITEMS.register("whetstone",
            () -> new com.realmlord.item.WhetstoneItem(new Item.Properties().stacksTo(16)));
    /** A refining stone of black iron and a beast core: raises a piece of gear in the other hand a grade. */
    public static final RegistryObject<Item> REFINING_STONE = ModItems.ITEMS.register("refining_stone",
            () -> new com.realmlord.item.RefiningStoneItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    /** A forging or alchemy manual: teaches the village's masters a new thing to make. */
    public static final RegistryObject<Item> MANUAL = ModItems.ITEMS.register("manual",
            () -> new com.realmlord.item.ManualItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ------------------------------------------------------------------ medicine and provisions

    /** Golden wound powder: the wuxia healer's staple, sprinkled on a wound to close it. */
    public static final RegistryObject<Item> GOLDEN_WOUND_POWDER = ModItems.ITEMS.register("golden_wound_powder",
            () -> new com.realmlord.item.PillItem(new Item.Properties().stacksTo(32), com.realmlord.item.PillItem.Kind.WOUND_POWDER));
    /** A rejuvenation pill: heals at once and goes on healing. */
    public static final RegistryObject<Item> HEALING_PILL = ModItems.ITEMS.register("healing_pill",
            () -> new com.realmlord.item.PillItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), com.realmlord.item.PillItem.Kind.HEALING));
    /** A marrow-cleansing pill: one of the special pills that raise an innate attribute (Body Structure). */
    public static final RegistryObject<Item> MARROW_CLEANSING_PILL = ModItems.ITEMS.register("marrow_cleansing_pill",
            () -> new com.realmlord.item.PillItem(new Item.Properties().stacksTo(8).rarity(Rarity.EPIC), com.realmlord.item.PillItem.Kind.MARROW));
    /** An enlightenment pill: raises Comprehension. */
    public static final RegistryObject<Item> ENLIGHTENMENT_PILL = ModItems.ITEMS.register("enlightenment_pill",
            () -> new com.realmlord.item.PillItem(new Item.Properties().stacksTo(8).rarity(Rarity.EPIC), com.realmlord.item.PillItem.Kind.ENLIGHTENMENT));
    /** Military rations: dry, salted and filling, for the march. */
    public static final RegistryObject<Item> MILITARY_RATIONS = ModItems.ITEMS.register("military_rations", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationMod(1.0F).build())));

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

    /** The ring-pommel dao: the Han soldier's single-edged sword. */
    public static final RegistryObject<Item> RING_DAO = ModItems.ITEMS.register("ring_dao",
            () -> new SwordItem(BLACK_IRON, 3, -2.3F, new Item.Properties()));
    /** The ji: a spear with a crescent blade at its side, for hooking and cutting. */
    public static final RegistryObject<Item> JI = ModItems.ITEMS.register("ji",
            () -> new SwordItem(STEEL, 5, -3.0F, new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** The Tiger Soul Sabre: steel quenched with a beast core; its blow carries a tiger's roar. */
    public static final RegistryObject<Item> TIGER_SOUL_SABRE = ModItems.ITEMS.register("tiger_soul_sabre",
            () -> new TigerSoulSabre(new Item.Properties().rarity(Rarity.EPIC)));

    public static class TigerSoulSabre extends SwordItem {
        public TigerSoulSabre(Properties p) {
            super(STEEL, 5, -2.2F, p);
        }

        @Override
        public boolean hurtEnemy(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
            // One blow in four roars: the foe falters and slows.
            if (!attacker.level().isClientSide && attacker.getRandom().nextInt(4) == 0) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0));
                attacker.level().playSound(null, target.blockPosition(), SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            return super.hurtEnemy(stack, target, attacker);
        }
    }

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

    public static final ArmorMaterial HIDE = new Material("hide", 12, new int[]{1, 4, 5, 2}, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(ModItems.WOLF_PELT.get(), ModItems.BOAR_HIDE.get(), ModItems.LEOPARD_PELT.get(), ModItems.TIGER_PELT.get()));
    public static final ArmorMaterial BLACK_IRON_ARMOUR = new Material("black_iron", 22, new int[]{2, 6, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F,
            () -> Ingredient.of(BLACK_IRON_INGOT.get()));
    /** Lamellar: rows of small black iron scales laced with sinew, light and supple. */
    public static final ArmorMaterial LAMELLAR = new Material("lamellar", 25, new int[]{2, 5, 7, 2}, 11, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 0.0F,
            () -> Ingredient.of(LAMELLAR_PLATES.get()));
    /** Mountain-pattern armour: interlocking steel scales in the shape of the character 山, worn by generals. */
    public static final ArmorMaterial MOUNTAIN_PATTERN = new Material("mountain_pattern", 33, new int[]{3, 6, 8, 3}, 12, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.05F,
            () -> Ingredient.of(REFINED_STEEL.get()));
    /** A tiger-hide war coat over steel, with a tiger-head hood: a champion's armour. */
    public static final ArmorMaterial TIGER_WAR = new Material("tiger_war", 30, new int[]{3, 6, 8, 3}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 2.5F, 0.1F,
            () -> Ingredient.of(ModItems.TIGER_PELT.get()));

    /** defence: boots, leggings, chestplate, helmet. */
    private record Material(String id, int durability, int[] defence, int enchant, SoundEvent sound, float toughness, float knockback,
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
        public float getKnockbackResistance() { return knockback; }
    }

    public static final RegistryObject<Item> HIDE_HELMET = armour("hide_helmet", HIDE, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> HIDE_JERKIN = armour("hide_jerkin", HIDE, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> HIDE_LEGGINGS = armour("hide_leggings", HIDE, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> HIDE_BOOTS = armour("hide_boots", HIDE, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> BLACK_IRON_HELMET = armour("black_iron_helmet", BLACK_IRON_ARMOUR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> BLACK_IRON_CUIRASS = armour("black_iron_cuirass", BLACK_IRON_ARMOUR, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> BLACK_IRON_GREAVES = armour("black_iron_greaves", BLACK_IRON_ARMOUR, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BLACK_IRON_BOOTS = armour("black_iron_boots", BLACK_IRON_ARMOUR, ArmorItem.Type.BOOTS);

    public static final RegistryObject<Item> LAMELLAR_HELMET = armour("lamellar_helmet", LAMELLAR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> LAMELLAR_COAT = armour("lamellar_coat", LAMELLAR, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> LAMELLAR_TASSETS = armour("lamellar_tassets", LAMELLAR, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> LAMELLAR_BOOTS = armour("lamellar_boots", LAMELLAR, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> MOUNTAIN_HELM = armour("mountain_helm", MOUNTAIN_PATTERN, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> MOUNTAIN_ARMOUR = armour("mountain_armour", MOUNTAIN_PATTERN, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> MOUNTAIN_GREAVES = armour("mountain_greaves", MOUNTAIN_PATTERN, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> MOUNTAIN_BOOTS = armour("mountain_boots", MOUNTAIN_PATTERN, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> TIGER_HOOD = armour("tiger_hood", TIGER_WAR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> TIGER_WAR_COAT = armour("tiger_war_coat", TIGER_WAR, ArmorItem.Type.CHESTPLATE);

    private static RegistryObject<Item> armour(String id, ArmorMaterial m, ArmorItem.Type t) {
        return ModItems.ITEMS.register(id, () -> new ArmorItem(m, t, new Item.Properties()));
    }

    // ------------------------------------------------------------------ groups

    public static List<RegistryObject<Item>> materials() {
        return List.of(BEAST_SINEW, BEAST_CORE, BANDIT_INSIGNIA, BLACK_IRON_INGOT, REFINED_STEEL, LAMELLAR_PLATES, WHETSTONE, REFINING_STONE,
                GOLDEN_WOUND_POWDER, HEALING_PILL, MARROW_CLEANSING_PILL, ENLIGHTENMENT_PILL, MILITARY_RATIONS);
    }

    public static List<RegistryObject<Item>> arms() {
        return List.of(BANDIT_SABRE, BLACK_IRON_SABRE, RING_DAO, BLACK_IRON_SPEAR, STEEL_JIAN, GUANDAO, JI, TIGER_SOUL_SABRE, TIGER_BONE_BOW,
                HIDE_HELMET, HIDE_JERKIN, HIDE_LEGGINGS, HIDE_BOOTS, BLACK_IRON_HELMET, BLACK_IRON_CUIRASS, BLACK_IRON_GREAVES, BLACK_IRON_BOOTS,
                LAMELLAR_HELMET, LAMELLAR_COAT, LAMELLAR_TASSETS, LAMELLAR_BOOTS, MOUNTAIN_HELM, MOUNTAIN_ARMOUR, MOUNTAIN_GREAVES, MOUNTAIN_BOOTS,
                TIGER_HOOD, TIGER_WAR_COAT);
    }

    /** The grade a crafted piece comes out at. */
    public static com.realmlord.territory.TokenGrade craftedGrade(Item item) {
        if (item == HIDE_HELMET.get() || item == HIDE_JERKIN.get() || item == HIDE_LEGGINGS.get() || item == HIDE_BOOTS.get()) return com.realmlord.territory.TokenGrade.BRONZE;
        if (item == TIGER_SOUL_SABRE.get() || item == TIGER_HOOD.get() || item == TIGER_WAR_COAT.get()) return com.realmlord.territory.TokenGrade.GOLD;
        if (item == STEEL_JIAN.get() || item == GUANDAO.get() || item == JI.get() || item == MOUNTAIN_HELM.get() || item == MOUNTAIN_ARMOUR.get()
                || item == MOUNTAIN_GREAVES.get() || item == MOUNTAIN_BOOTS.get()) return com.realmlord.territory.TokenGrade.SILVER;
        if (item == BLACK_IRON_SABRE.get() || item == RING_DAO.get() || item == BLACK_IRON_SPEAR.get() || item == TIGER_BONE_BOW.get() || item == BLACK_IRON_HELMET.get()
                || item == BLACK_IRON_CUIRASS.get() || item == BLACK_IRON_GREAVES.get() || item == BLACK_IRON_BOOTS.get() || item == LAMELLAR_HELMET.get()
                || item == LAMELLAR_COAT.get() || item == LAMELLAR_TASSETS.get() || item == LAMELLAR_BOOTS.get()) return com.realmlord.territory.TokenGrade.BLACK_IRON;
        return null;
    }
}
