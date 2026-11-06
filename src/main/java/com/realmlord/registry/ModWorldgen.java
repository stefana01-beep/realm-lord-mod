package com.realmlord.registry;

import com.mojang.serialization.Codec;
import com.realmlord.RealmLord;
import com.realmlord.worldgen.ChinaDensity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldgen {
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, RealmLord.MODID);

    public static final RegistryObject<Codec<? extends DensityFunction>> CHINA_MAP =
            DENSITY_FUNCTIONS.register("china_map", ChinaDensity.CODEC::codec);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, RealmLord.MODID);

    public static final RegistryObject<com.realmlord.worldgen.RiverWaterFeature> RIVER_WATER =
            FEATURES.register("river_water", com.realmlord.worldgen.RiverWaterFeature::new);
}
