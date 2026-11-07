package com.realmlord.registry;

import com.realmlord.item.CraftItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * What the technical manuals teach players to make, and the manuals themselves: goods,
 * medicine, animal care, tools and charms, food and wine. (Arms and armour are in ModArms.)
 */
public final class ModCrafts {
    private ModCrafts() {}

    /** Loads this class so its items are registered with the rest. */
    public static void init() {}

    public static final RegistryObject<Item> TECH_MANUAL = ModItems.ITEMS.register("tech_manual",
            () -> new com.realmlord.item.TechManualItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SECRET_MANUAL = ModItems.ITEMS.register("secret_manual",
            () -> new com.realmlord.item.SecretManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> RAINBOW_SILK = ModItems.ITEMS.register("rainbow_silk", () -> new CraftItem(new Item.Properties().rarity(Rarity.UNCOMMON), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> SILK_COCOON = ModItems.ITEMS.register("silk_cocoon", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> PINE_TAR = ModItems.ITEMS.register("pine_tar", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> TEA_LEAVES = ModItems.ITEMS.register("tea_leaves", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> SOY_SAUCE = ModItems.ITEMS.register("soy_sauce", () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> PORCELAIN_VASE = ModItems.ITEMS.register("porcelain_vase", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> JADE_PENDANT = ModItems.ITEMS.register("jade_pendant", () -> new CraftItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), CraftItem.Kind.JADE));
    public static final RegistryObject<Item> DETOX_DRAUGHT = ModItems.ITEMS.register("detox_draught", () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.DETOX));
    public static final RegistryObject<Item> CLARITY_PILL = ModItems.ITEMS.register("clarity_pill", () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.CLARITY));
    public static final RegistryObject<Item> ACUPUNCTURE_NEEDLES = ModItems.ITEMS.register("acupuncture_needles", () -> new CraftItem(new Item.Properties().durability(24), CraftItem.Kind.NEEDLES));
    public static final RegistryObject<Item> RHINO_HORN_PILL = ModItems.ITEMS.register("rhino_horn_pill", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), CraftItem.Kind.RHINO_PILL));
    public static final RegistryObject<Item> PEIYUAN_PILL = ModItems.ITEMS.register("peiyuan_pill", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), CraftItem.Kind.PEIYUAN));
    public static final RegistryObject<Item> FODDER = ModItems.ITEMS.register("fodder", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.FODDER));
    public static final RegistryObject<Item> HORSE_FEED = ModItems.ITEMS.register("horse_feed", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.HORSE_FEED));
    public static final RegistryObject<Item> BAMBOO_FEED = ModItems.ITEMS.register("bamboo_feed", () -> new CraftItem(new Item.Properties(), CraftItem.Kind.BAMBOO_FEED));
    public static final RegistryObject<Item> ANIMAL_SALVE = ModItems.ITEMS.register("animal_salve", () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.ANIMAL_SALVE));
    public static final RegistryObject<Item> HORSE_TONIC = ModItems.ITEMS.register("horse_tonic", () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.HORSE_TONIC));
    public static final RegistryObject<Item> BEAST_WHISTLE = ModItems.ITEMS.register("beast_whistle", () -> new CraftItem(new Item.Properties().stacksTo(1), CraftItem.Kind.WHISTLE));
    public static final RegistryObject<Item> GUARDIAN_CHARM = ModItems.ITEMS.register("guardian_charm", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> LUOPAN = ModItems.ITEMS.register("luopan", () -> new CraftItem(new Item.Properties().stacksTo(1), CraftItem.Kind.LUOPAN));
    public static final RegistryObject<Item> FISHING_NET = ModItems.ITEMS.register("fishing_net", () -> new CraftItem(new Item.Properties().durability(48), CraftItem.Kind.NET));
    public static final RegistryObject<Item> SPRING_TALISMAN = ModItems.ITEMS.register("spring_talisman", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), CraftItem.Kind.TALISMAN));
    public static final RegistryObject<Item> SOUTH_POINTING_CHARIOT = ModItems.ITEMS.register("south_pointing_chariot", () -> new CraftItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), CraftItem.Kind.CHARIOT));
    public static final RegistryObject<Item> WOODEN_OX = ModItems.ITEMS.register("wooden_ox", () -> new CraftItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), CraftItem.Kind.WOODEN_OX));
    public static final RegistryObject<Item> SUBSTITUTE_DOLL = ModItems.ITEMS.register("substitute_doll", () -> new CraftItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> LIFEBUOY = ModItems.ITEMS.register("lifebuoy", () -> new CraftItem(new Item.Properties().stacksTo(1), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> DIVING_BELL = ModItems.ITEMS.register("diving_bell", () -> new CraftItem(new Item.Properties().durability(8), CraftItem.Kind.DIVING_BELL));
    public static final RegistryObject<Item> DRIED_FISH = ModItems.ITEMS.register("dried_fish", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.8F).build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> DUMPLINGS = ModItems.ITEMS.register("dumplings", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.5F).fast().build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> NOODLE_SOUP = ModItems.ITEMS.register("noodle_soup", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(10).saturationMod(0.9F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0), 1.0F).build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> GREEN_TEA = ModItems.ITEMS.register("green_tea", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0), 1.0F).build()), CraftItem.Kind.DRINK));
    public static final RegistryObject<Item> MOONCAKE = ModItems.ITEMS.register("mooncake", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0), 1.0F).build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> MILITARY_GRAIN_PILL = ModItems.ITEMS.register("military_grain_pill", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(20).saturationMod(1.2F).fast().alwaysEat().build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> ROAST_DUCK = ModItems.ITEMS.register("roast_duck", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder().nutrition(14).saturationMod(1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0F).build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> IMPERIAL_FEAST = ModItems.ITEMS.register("imperial_feast", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
            .food(new FoodProperties.Builder().nutrition(20).saturationMod(1.5F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0F).build()), CraftItem.Kind.FOOD));
    public static final RegistryObject<Item> DUKANG_WINE = ModItems.ITEMS.register("dukang_wine", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100, 0), 1.0F).build()), CraftItem.Kind.DRINK));
    public static final RegistryObject<Item> MAOTAI = ModItems.ITEMS.register("maotai", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0F).build()), CraftItem.Kind.DRINK));
    public static final RegistryObject<Item> MEDICINAL_WINE = ModItems.ITEMS.register("medicinal_wine", () -> new CraftItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1), 1.0F).build()), CraftItem.Kind.DRINK));
    public static final RegistryObject<Item> IMMORTAL_WINE = ModItems.ITEMS.register("immortal_wine", () -> new CraftItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.LUCK, 12000, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0), 1.0F).build()), CraftItem.Kind.DRINK));

    // ------------------------------------------------------------------ building and ships

    /** A building plan: a ready-made design or an architect's own. */
    public static final RegistryObject<Item> BUILDING_PLAN = ModItems.ITEMS.register("building_plan",
            () -> new com.realmlord.item.PlanItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** Lu Ban's square, the architect's drafting tool. */
    public static final RegistryObject<Item> DRAFTING_SQUARE = ModItems.ITEMS.register("drafting_square",
            () -> new com.realmlord.item.DraftingSquareItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FISHING_BOAT = ModItems.ITEMS.register("fishing_boat",
            () -> new com.realmlord.item.ShipItem(new Item.Properties().stacksTo(1), com.realmlord.ship.ShipKind.FISHING_BOAT, ModEntities.FISHING_BOAT));
    public static final RegistryObject<Item> CIMA_BOAT = ModItems.ITEMS.register("cima_boat",
            () -> new com.realmlord.item.ShipItem(new Item.Properties().stacksTo(1), com.realmlord.ship.ShipKind.CIMA_BOAT, ModEntities.CIMA_BOAT));
    public static final RegistryObject<Item> MERCHANT_SHIP = ModItems.ITEMS.register("merchant_ship",
            () -> new com.realmlord.item.ShipItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), com.realmlord.ship.ShipKind.MERCHANT_SHIP, ModEntities.MERCHANT_SHIP));
    public static final RegistryObject<Item> MENGCHONG_WARSHIP = ModItems.ITEMS.register("mengchong_warship",
            () -> new com.realmlord.item.ShipItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), com.realmlord.ship.ShipKind.MENGCHONG, ModEntities.MENGCHONG));
    public static final RegistryObject<Item> TURRETED_SHIP = ModItems.ITEMS.register("turreted_ship",
            () -> new com.realmlord.item.ShipItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), com.realmlord.ship.ShipKind.TURRETED_SHIP, ModEntities.TURRETED_SHIP));

    // ------------------------------------------------------------------ siege engines

    public static final RegistryObject<Item> HEAVY_BOLT = ModItems.ITEMS.register("heavy_bolt",
            () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> STONE_SHOT = ModItems.ITEMS.register("stone_shot",
            () -> new CraftItem(new Item.Properties().stacksTo(16), CraftItem.Kind.PLAIN));
    public static final RegistryObject<Item> SIEGE_LADDER = ModItems.ITEMS.register("siege_ladder",
            () -> new com.realmlord.item.SiegeLadderItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BED_CROSSBOW = ModItems.ITEMS.register("bed_crossbow",
            () -> new net.minecraft.world.item.BlockItem(com.realmlord.registry.ModBlocks.BED_CROSSBOW.get(), new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TREBUCHET = ModItems.ITEMS.register("trebuchet",
            () -> new net.minecraft.world.item.BlockItem(com.realmlord.registry.ModBlocks.TREBUCHET.get(), new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));

    public static List<RegistryObject<Item>> ships() {
        return List.of(FISHING_BOAT, CIMA_BOAT, MERCHANT_SHIP, MENGCHONG_WARSHIP, TURRETED_SHIP);
    }

    public static List<RegistryObject<Item>> all() {
        return List.of(DRAFTING_SQUARE, HEAVY_BOLT, STONE_SHOT, SIEGE_LADDER, BED_CROSSBOW, TREBUCHET, FISHING_BOAT, CIMA_BOAT, MERCHANT_SHIP, MENGCHONG_WARSHIP, TURRETED_SHIP, RAINBOW_SILK, SILK_COCOON, PINE_TAR, TEA_LEAVES, SOY_SAUCE, PORCELAIN_VASE, JADE_PENDANT, DETOX_DRAUGHT, CLARITY_PILL, ACUPUNCTURE_NEEDLES, RHINO_HORN_PILL, PEIYUAN_PILL, FODDER, HORSE_FEED, BAMBOO_FEED, ANIMAL_SALVE, HORSE_TONIC, BEAST_WHISTLE, GUARDIAN_CHARM, LUOPAN, FISHING_NET, SPRING_TALISMAN, SOUTH_POINTING_CHARIOT, WOODEN_OX, SUBSTITUTE_DOLL, LIFEBUOY, DIVING_BELL, DRIED_FISH, DUMPLINGS, NOODLE_SOUP, GREEN_TEA, MOONCAKE, MILITARY_GRAIN_PILL, ROAST_DUCK, IMPERIAL_FEAST, DUKANG_WINE, MAOTAI, MEDICINAL_WINE, IMMORTAL_WINE);
    }
}
