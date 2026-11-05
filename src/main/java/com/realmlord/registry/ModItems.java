package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.item.BanditLedgerItem;
import com.realmlord.item.BlueprintItem;
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

    /** The world's currency, as in the novel. */
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BLUEPRINT = ITEMS.register("blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> RESOURCES_BAG = ITEMS.register("resources_bag",
            () -> new ResourcesBagItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SUMMONING_TOKEN = ITEMS.register("summoning_token",
            () -> new SummoningTokenItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

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
