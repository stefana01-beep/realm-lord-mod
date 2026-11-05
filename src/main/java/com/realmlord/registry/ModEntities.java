package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RealmLord.MODID);

    public static final RegistryObject<EntityType<BanditEntity>> BANDIT = ENTITIES.register("bandit",
            () -> EntityType.Builder.<BanditEntity>of(BanditEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(RealmLord.id("bandit").toString()));

    public static final RegistryObject<EntityType<SettlerEntity>> SETTLER = ENTITIES.register("settler",
            () -> EntityType.Builder.<SettlerEntity>of(SettlerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(RealmLord.id("settler").toString()));
}
