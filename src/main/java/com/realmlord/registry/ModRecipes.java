package com.realmlord.registry;

import com.realmlord.RealmLord;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's recipe types: crafting that needs a technical manual. */
public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RealmLord.MODID);

    public static final RegistryObject<RecipeSerializer<com.realmlord.work.ManualRecipe>> MANUAL =
            SERIALIZERS.register("manual", com.realmlord.work.ManualRecipe.Serializer::new);
}
