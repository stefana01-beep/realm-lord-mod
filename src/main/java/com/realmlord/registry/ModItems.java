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
