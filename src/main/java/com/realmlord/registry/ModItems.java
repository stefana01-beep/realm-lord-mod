package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.item.BanditLedgerItem;
import com.realmlord.item.VillageTokenItem;
import com.realmlord.territory.TokenGrade;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

    public static final RegistryObject<Item> BANDIT_LEDGER = ITEMS.register("bandit_ledger",
            () -> new BanditLedgerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LORDS_MANOR = ITEMS.register("lords_manor",
            () -> new BlockItem(ModBlocks.LORDS_MANOR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static Item token(TokenGrade g) {
        return TOKENS.get(g).get();
    }
}
