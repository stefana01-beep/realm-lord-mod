package com.realmlord.event;

import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.profile.Profile;
import com.realmlord.registry.ModEntities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(Profile.class);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(ModEntities.SETTLER.get(), SettlerEntity.createAttributes().build());
        event.put(ModEntities.WILD_BOAR.get(), com.realmlord.entity.beast.WildBoarEntity.createAttributes().build());
        event.put(ModEntities.BOAR_KING.get(), com.realmlord.entity.beast.BoarKingEntity.createAttributes().build());
        event.put(ModEntities.GREY_WOLF.get(), com.realmlord.entity.beast.GreyWolfEntity.createAttributes().build());
        event.put(ModEntities.MOUNTAIN_TIGER.get(), com.realmlord.entity.beast.MountainTigerEntity.createAttributes().build());
        event.put(ModEntities.SNOW_BEAR.get(), com.realmlord.entity.beast.SnowBearEntity.createAttributes().build());
    }

    /** The wild beasts roam by day as well as by night, on dry ground, away from villages' fences. */
    @SubscribeEvent
    public static void spawns(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        for (var type : java.util.List.of(ModEntities.WILD_BOAR.get(), ModEntities.GREY_WOLF.get(), ModEntities.MOUNTAIN_TIGER.get(), ModEntities.SNOW_BEAR.get())) {
            register(event, type);
        }
    }

    private static <T extends net.minecraft.world.entity.Mob> void register(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event,
                                                                          net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && net.minecraft.world.entity.Mob.checkMobSpawnRules(t, level, reason, pos, random)
                        && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), t),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
