package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.territory.TokenGrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RealmLord.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.realmlord"))
            .icon(() -> new ItemStack(ModItems.token(TokenGrade.GOLD)))
            .displayItems((params, out) -> {
                for (TokenGrade g : TokenGrade.values()) out.accept(ModItems.token(g));
                out.accept(ModItems.COPPER_COIN.get());
                out.accept(ModItems.SILVER_COIN.get());
                out.accept(ModItems.GOLD_COIN.get());
                for (int r = 0; r < 4; r++) out.accept(com.realmlord.item.SummoningTokenItem.of(r));
                for (int t = 0; t < 3; t++) out.accept(com.realmlord.item.ResourcesBagItem.of(t));
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_1) out.accept(com.realmlord.loot.Blueprints.of(b));
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_2) out.accept(com.realmlord.loot.Blueprints.of(b));
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_3) out.accept(com.realmlord.loot.Blueprints.of(b));
                out.accept(ModItems.VILLAGE_GUIDE.get());
                out.accept(ModItems.SALT.get());
                out.accept(ModItems.MEDICINAL_HERBS.get());
                out.accept(ModItems.THREE_FLOWER_WINE.get());
                out.accept(ModItems.LINEN.get());
                out.accept(ModItems.FINE_SILK.get());
                out.accept(ModItems.FIRE_OIL.get());
                for (var it : java.util.List.of(ModItems.BOAR_HIDE, ModItems.BOAR_TUSK, ModItems.BOAR_KING_TUSK, ModItems.WOLF_PELT, ModItems.TIGER_PELT,
                        ModItems.TIGER_BONE, ModItems.BEAR_PAW, ModItems.BRAISED_BEAR_PAW, ModItems.TIGER_BONE_WINE, ModItems.TUSK_SPEAR, ModItems.BOAR_KING_SPEAR,
                        ModItems.LEOPARD_PELT, ModItems.WHITE_TIGER_PELT, ModItems.WOLF_KING_FANG, ModItems.OX_HORN, ModItems.FANG_DAGGER,
                        ModItems.BLACK_BEAR_SPAWN_EGG, ModItems.LEOPARD_SPAWN_EGG, ModItems.WOLF_KING_SPAWN_EGG, ModItems.TIGER_KING_SPAWN_EGG, ModItems.WILD_OX_SPAWN_EGG,
                        ModItems.WILD_BOAR_SPAWN_EGG, ModItems.BOAR_KING_SPAWN_EGG, ModItems.GREY_WOLF_SPAWN_EGG, ModItems.MOUNTAIN_TIGER_SPAWN_EGG, ModItems.SNOW_BEAR_SPAWN_EGG))
                    out.accept(it.get());
                out.accept(ModItems.BANDIT_LEDGER.get());
                out.accept(ModItems.BANDIT_SPAWN_EGG.get());
                out.accept(ModItems.SETTLER_SPAWN_EGG.get());
                out.accept(ModItems.LORDS_MANOR.get());
            })
            .build());
}
