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
                out.accept(ModItems.BANDIT_LEDGER.get());
                out.accept(ModItems.BANDIT_SPAWN_EGG.get());
                out.accept(ModItems.SETTLER_SPAWN_EGG.get());
                out.accept(ModItems.LORDS_MANOR.get());
            })
            .build());
}
