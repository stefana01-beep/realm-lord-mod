package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.item.BanditLedgerItem;
import com.realmlord.item.BlueprintItem;
import com.realmlord.item.CoinItem;
import com.realmlord.item.ResourcesBagItem;
import com.realmlord.item.SummoningTokenItem;
import com.realmlord.item.VillageTokenItem;
import com.realmlord.territory.TokenGrade;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealmLord.MODID);

    public static final Map<TokenGrade, RegistryObject<Item>> TOKENS = new EnumMap<>(TokenGrade.class);

    static {
        for (TokenGrade g : TokenGrade.values()) {
            Rarity rarity = switch (g) {
                case BRONZE -> Rarity.COMMON;
                case BLACK_IRON -> Rarity.UNCOMMON;
                case SILVER -> Rarity.RARE;
                case GOLD -> Rarity.EPIC;
            };
            TOKENS.put(g, ITEMS.register(g.id + "_village_token",
                    () -> new VillageTokenItem(g, new Item.Properties().stacksTo(1).rarity(rarity).fireResistant())));
        }
    }

    /** The world's currency, as in the novel: 1 gold = 100 silver = 10,000 copper. */
    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin",
            () -> new CoinItem(0, new Item.Properties()));
    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin",
            () -> new CoinItem(1, new Item.Properties()));
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new CoinItem(2, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BLUEPRINT = ITEMS.register("blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> RESOURCES_BAG = ITEMS.register("resources_bag",
            () -> new ResourcesBagItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SUMMONING_TOKEN = ITEMS.register("summoning_token",
            () -> new SummoningTokenItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    // The novel's goods, made in the village's workshops and traded on the market.
    public static final RegistryObject<Item> SALT = ITEMS.register("salt", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LINEN = ITEMS.register("linen", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FINE_SILK = ITEMS.register("fine_silk", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MEDICINAL_HERBS = ITEMS.register("medicinal_herbs", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(0.1F).alwaysEat().fast()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1), 1.0F).build())));
    public static final RegistryObject<Item> THREE_FLOWER_WINE = ITEMS.register("three_flower_wine",
            () -> new com.realmlord.item.WineItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FIRE_OIL = ITEMS.register("fire_oil",
            () -> new com.realmlord.item.FireOilItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> VILLAGE_GUIDE = ITEMS.register("village_guide",
            () -> new com.realmlord.item.GuideItem(new Item.Properties().stacksTo(1)));

    // What the wild beasts leave behind.
    public static final RegistryObject<Item> BOAR_HIDE = ITEMS.register("boar_hide", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BOAR_TUSK = ITEMS.register("boar_tusk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BOAR_KING_TUSK = ITEMS.register("boar_king_tusk", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WOLF_PELT = ITEMS.register("wolf_pelt", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIGER_PELT = ITEMS.register("tiger_pelt", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TIGER_BONE = ITEMS.register("tiger_bone", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BEAR_PAW = ITEMS.register("bear_paw", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationMod(0.3F).meat().build())));
    public static final RegistryObject<Item> BRAISED_BEAR_PAW = ITEMS.register("braised_bear_paw", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(12).saturationMod(1.2F).meat()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1), 1.0F)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 1), 1.0F).build())));
    public static final RegistryObject<Item> TIGER_BONE_WINE = ITEMS.register("tiger_bone_wine",
            () -> new com.realmlord.item.WineItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), 1));
    public static final RegistryObject<Item> TUSK_SPEAR = ITEMS.register("tusk_spear",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 4, -2.6F, new Item.Properties()));
    public static final RegistryObject<Item> BOAR_KING_SPEAR = ITEMS.register("boar_king_spear",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.DIAMOND, 5, -2.6F, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WILD_BOAR_SPAWN_EGG = ITEMS.register("wild_boar_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WILD_BOAR, 0x6b4a2e, 0xd9c7a0, new Item.Properties()));
    public static final RegistryObject<Item> BOAR_KING_SPAWN_EGG = ITEMS.register("boar_king_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOAR_KING, 0x3d2a1a, 0xe8c15a, new Item.Properties()));
    public static final RegistryObject<Item> GREY_WOLF_SPAWN_EGG = ITEMS.register("grey_wolf_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GREY_WOLF, 0x8a8a8a, 0x3a3a3a, new Item.Properties()));
    public static final RegistryObject<Item> MOUNTAIN_TIGER_SPAWN_EGG = ITEMS.register("mountain_tiger_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MOUNTAIN_TIGER, 0xe08a2a, 0x1a1a1a, new Item.Properties()));
    public static final RegistryObject<Item> SNOW_BEAR_SPAWN_EGG = ITEMS.register("snow_bear_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SNOW_BEAR, 0xeeeeee, 0x9a9a9a, new Item.Properties()));

    public static final RegistryObject<Item> BANDIT_LEDGER = ITEMS.register("bandit_ledger",
            () -> new BanditLedgerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LORDS_MANOR = ITEMS.register("lords_manor",
            () -> new BlockItem(ModBlocks.LORDS_MANOR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BANDIT_SPAWN_EGG = ITEMS.register("bandit_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BANDIT, 0x3A2D26, 0x8C1F1C, new Item.Properties()));

    public static final RegistryObject<Item> SETTLER_SPAWN_EGG = ITEMS.register("settler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SETTLER, 0xB0A082, 0x3C4B6E, new Item.Properties()));

    public static Item token(TokenGrade g) {
        return TOKENS.get(g).get();
    }
}
