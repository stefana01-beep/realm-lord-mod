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

    public static final RegistryObject<EntityType<com.realmlord.entity.FireOilEntity>> FIRE_OIL = ENTITIES.register("fire_oil",
            () -> EntityType.Builder.<com.realmlord.entity.FireOilEntity>of(com.realmlord.entity.FireOilEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(RealmLord.id("fire_oil").toString()));

    public static final RegistryObject<EntityType<com.realmlord.entity.beast.WildBoarEntity>> WILD_BOAR = ENTITIES.register("wild_boar",
            () -> EntityType.Builder.<com.realmlord.entity.beast.WildBoarEntity>of(com.realmlord.entity.beast.WildBoarEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.2F).clientTrackingRange(8).build(RealmLord.id("wild_boar").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.BoarKingEntity>> BOAR_KING = ENTITIES.register("boar_king",
            () -> EntityType.Builder.<com.realmlord.entity.beast.BoarKingEntity>of(com.realmlord.entity.beast.BoarKingEntity::new, MobCategory.MONSTER)
                    .sized(2.2F, 2.2F).clientTrackingRange(10).fireImmune().build(RealmLord.id("boar_king").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.GreyWolfEntity>> GREY_WOLF = ENTITIES.register("grey_wolf",
            () -> EntityType.Builder.<com.realmlord.entity.beast.GreyWolfEntity>of(com.realmlord.entity.beast.GreyWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).build(RealmLord.id("grey_wolf").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.MountainTigerEntity>> MOUNTAIN_TIGER = ENTITIES.register("mountain_tiger",
            () -> EntityType.Builder.<com.realmlord.entity.beast.MountainTigerEntity>of(com.realmlord.entity.beast.MountainTigerEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).build(RealmLord.id("mountain_tiger").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.SnowBearEntity>> SNOW_BEAR = ENTITIES.register("snow_bear",
            () -> EntityType.Builder.<com.realmlord.entity.beast.SnowBearEntity>of(com.realmlord.entity.beast.SnowBearEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.4F).clientTrackingRange(10).build(RealmLord.id("snow_bear").toString()));

    public static final RegistryObject<EntityType<SettlerEntity>> SETTLER = ENTITIES.register("settler",
            () -> EntityType.Builder.<SettlerEntity>of(SettlerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(RealmLord.id("settler").toString()));
}
