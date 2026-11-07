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

    public static final RegistryObject<EntityType<com.realmlord.entity.beast.BlackBearEntity>> BLACK_BEAR = ENTITIES.register("black_bear",
            () -> EntityType.Builder.<com.realmlord.entity.beast.BlackBearEntity>of(com.realmlord.entity.beast.BlackBearEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 1.3F).clientTrackingRange(10).build(RealmLord.id("black_bear").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.LeopardEntity>> LEOPARD = ENTITIES.register("leopard",
            () -> EntityType.Builder.<com.realmlord.entity.beast.LeopardEntity>of(com.realmlord.entity.beast.LeopardEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 0.9F).clientTrackingRange(10).build(RealmLord.id("leopard").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.WolfKingEntity>> WOLF_KING = ENTITIES.register("wolf_king",
            () -> EntityType.Builder.<com.realmlord.entity.beast.WolfKingEntity>of(com.realmlord.entity.beast.WolfKingEntity::new, MobCategory.MONSTER)
                    .sized(1.1F, 1.5F).clientTrackingRange(10).build(RealmLord.id("wolf_king").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.TigerKingEntity>> TIGER_KING = ENTITIES.register("tiger_king",
            () -> EntityType.Builder.<com.realmlord.entity.beast.TigerKingEntity>of(com.realmlord.entity.beast.TigerKingEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.7F).clientTrackingRange(10).build(RealmLord.id("tiger_king").toString()));
    public static final RegistryObject<EntityType<com.realmlord.entity.beast.WildOxEntity>> WILD_OX = ENTITIES.register("wild_ox",
            () -> EntityType.Builder.<com.realmlord.entity.beast.WildOxEntity>of(com.realmlord.entity.beast.WildOxEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.4F).clientTrackingRange(10).build(RealmLord.id("wild_ox").toString()));

    public static final RegistryObject<EntityType<SettlerEntity>> SETTLER = ENTITIES.register("settler",
            () -> EntityType.Builder.<SettlerEntity>of(SettlerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(RealmLord.id("settler").toString()));

    // ------------------------------------------------------------------ ships

    private static <T extends net.minecraft.world.entity.vehicle.Boat> RegistryObject<EntityType<T>> ship(String id, EntityType.EntityFactory<T> f, com.realmlord.ship.ShipKind k) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MISC).sized(1.375F * k.scale, 0.5625F * Math.max(1.0F, k.scale * 0.8F))
                .clientTrackingRange(10).build(RealmLord.id(id).toString()));
    }

    public static final RegistryObject<EntityType<com.realmlord.ship.ShipEntity>> FISHING_BOAT = ship("fishing_boat",
            (t, l) -> new com.realmlord.ship.ShipEntity(t, l, com.realmlord.ship.ShipKind.FISHING_BOAT), com.realmlord.ship.ShipKind.FISHING_BOAT);
    public static final RegistryObject<EntityType<com.realmlord.ship.ShipEntity>> CIMA_BOAT = ship("cima_boat",
            (t, l) -> new com.realmlord.ship.ShipEntity(t, l, com.realmlord.ship.ShipKind.CIMA_BOAT), com.realmlord.ship.ShipKind.CIMA_BOAT);
    public static final RegistryObject<EntityType<com.realmlord.ship.CargoShipEntity>> MERCHANT_SHIP = ship("merchant_ship",
            (t, l) -> new com.realmlord.ship.CargoShipEntity(t, l, com.realmlord.ship.ShipKind.MERCHANT_SHIP), com.realmlord.ship.ShipKind.MERCHANT_SHIP);
    public static final RegistryObject<EntityType<com.realmlord.ship.ShipEntity>> MENGCHONG = ship("mengchong_warship",
            (t, l) -> new com.realmlord.ship.ShipEntity(t, l, com.realmlord.ship.ShipKind.MENGCHONG), com.realmlord.ship.ShipKind.MENGCHONG);
    public static final RegistryObject<EntityType<com.realmlord.ship.ShipEntity>> TURRETED_SHIP = ship("turreted_ship",
            (t, l) -> new com.realmlord.ship.ShipEntity(t, l, com.realmlord.ship.ShipKind.TURRETED_SHIP), com.realmlord.ship.ShipKind.TURRETED_SHIP);
}
