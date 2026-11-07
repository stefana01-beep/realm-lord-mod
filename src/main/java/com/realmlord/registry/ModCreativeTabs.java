package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.territory.TokenGrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The mod's creative tabs, one for each kind of thing: the Lord's tokens, coins and blueprints;
 * materials and goods; weapons and armour (shown at each grade); and creatures and blocks.
 */
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
                out.accept(ModItems.VILLAGE_GUIDE.get());
                out.accept(ModItems.BANDIT_LEDGER.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> BLUEPRINTS = TABS.register("blueprints", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.realmlord.blueprints"))
            .withTabsBefore(MAIN.getKey())
            .icon(() -> com.realmlord.loot.Blueprints.of(com.realmlord.loot.Blueprints.VILLAGE_1.get(0)))
            .displayItems((params, out) -> {
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_1) out.accept(com.realmlord.loot.Blueprints.of(b));
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_2) out.accept(com.realmlord.loot.Blueprints.of(b));
                for (String b : com.realmlord.loot.Blueprints.VILLAGE_3) out.accept(com.realmlord.loot.Blueprints.of(b));
            })
            .build());

    public static final RegistryObject<CreativeModeTab> MATERIALS = TABS.register("materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.realmlord.materials"))
            .withTabsBefore(BLUEPRINTS.getKey())
            .icon(() -> new ItemStack(ModArms.BEAST_CORE.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> it : List.of(ModItems.SALT, ModItems.MEDICINAL_HERBS, ModItems.THREE_FLOWER_WINE, ModItems.LINEN, ModItems.FINE_SILK,
                        ModItems.FIRE_OIL, ModItems.BOAR_HIDE, ModItems.BOAR_TUSK, ModItems.BOAR_KING_TUSK, ModItems.WOLF_PELT, ModItems.TIGER_PELT,
                        ModItems.TIGER_BONE, ModItems.LEOPARD_PELT, ModItems.WHITE_TIGER_PELT, ModItems.WOLF_KING_FANG, ModItems.OX_HORN,
                        ModItems.BEAR_PAW, ModItems.BRAISED_BEAR_PAW, ModItems.TIGER_BONE_WINE)) out.accept(it.get());
                for (RegistryObject<Item> it : ModArms.materials()) out.accept(it.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> ARMS = TABS.register("arms", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.realmlord.arms"))
            .withTabsBefore(MATERIALS.getKey())
            .icon(() -> new ItemStack(ModArms.GUANDAO.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> it : List.of(ModItems.TUSK_SPEAR, ModItems.BOAR_KING_SPEAR, ModItems.FANG_DAGGER)) out.accept(it.get());
                for (RegistryObject<Item> it : ModArms.arms()) out.accept(it.get());
                // Each new piece as it comes at its grade.
                net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(42);
                for (RegistryObject<Item> it : ModArms.arms()) {
                    TokenGrade g = ModArms.craftedGrade(it.get());
                    if (g != null) out.accept(com.realmlord.loot.Gear.make(it.get(), g, r));
                }
            })
            .build());

    public static final RegistryObject<CreativeModeTab> CREATURES = TABS.register("creatures", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.realmlord.creatures"))
            .withTabsBefore(ARMS.getKey())
            .icon(() -> new ItemStack(ModItems.WOLF_KING_SPAWN_EGG.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> it : List.of(ModItems.WILD_BOAR_SPAWN_EGG, ModItems.BOAR_KING_SPAWN_EGG, ModItems.GREY_WOLF_SPAWN_EGG,
                        ModItems.WOLF_KING_SPAWN_EGG, ModItems.MOUNTAIN_TIGER_SPAWN_EGG, ModItems.LEOPARD_SPAWN_EGG, ModItems.TIGER_KING_SPAWN_EGG,
                        ModItems.SNOW_BEAR_SPAWN_EGG, ModItems.BLACK_BEAR_SPAWN_EGG, ModItems.WILD_OX_SPAWN_EGG, ModItems.BANDIT_SPAWN_EGG,
                        ModItems.SETTLER_SPAWN_EGG, ModItems.LORDS_MANOR)) out.accept(it.get());
            })
            .build());
}
