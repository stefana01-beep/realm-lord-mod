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
                out.accept(ModItems.BANDIT_LEDGER.get());
                out.accept(ModItems.LORDS_MANOR.get());
            })
            .build());
}
