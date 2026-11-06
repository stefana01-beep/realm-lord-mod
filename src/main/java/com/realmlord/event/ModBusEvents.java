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
        event.put(ModEntities.BLACK_BEAR.get(), com.realmlord.entity.beast.BlackBearEntity.createAttributes().build());
        event.put(ModEntities.LEOPARD.get(), com.realmlord.entity.beast.LeopardEntity.createAttributes().build());
        event.put(ModEntities.WOLF_KING.get(), com.realmlord.entity.beast.WolfKingEntity.createAttributes().build());
        event.put(ModEntities.TIGER_KING.get(), com.realmlord.entity.beast.TigerKingEntity.createAttributes().build());
        event.put(ModEntities.WILD_OX.get(), com.realmlord.entity.beast.WildOxEntity.createAttributes().build());
    }

    /** The wild beasts roam by day as well as by night, on dry ground, away from villages' fences. */
    @SubscribeEvent
    public static void spawns(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        for (var type : java.util.List.of(ModEntities.WILD_BOAR.get(), ModEntities.GREY_WOLF.get(), ModEntities.MOUNTAIN_TIGER.get(), ModEntities.SNOW_BEAR.get(),
                ModEntities.BLACK_BEAR.get(), ModEntities.LEOPARD.get(), ModEntities.WOLF_KING.get(), ModEntities.TIGER_KING.get())) {
            register(event, type);
        }
        // The wild ox grazes on grass like any herd animal.
        event.register(ModEntities.WILD_OX.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules, net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
        // Bandits roam the open country by night.
        event.register(ModEntities.BANDIT.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> reason != net.minecraft.world.entity.MobSpawnType.NATURAL
                        || net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(t, level, reason, pos, random) && level.canSeeSky(pos)
                        && crowd(level, pos) < 4,
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    /** How many of the novel's beasts and bandits are already about near a spot. */
    private static int crowd(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.core.BlockPos pos) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(48, 24, 48),
                m -> com.realmlord.entity.level.MobLevels.leveled(m)).size();
    }

    private static <T extends net.minecraft.world.entity.Mob> void register(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event,
                                                                          net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && net.minecraft.world.entity.Mob.checkMobSpawnRules(t, level, reason, pos, random)
                        && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), t)
                        // Not too thick on the ground: a few about at a time, more of them by night.
                        && (reason != net.minecraft.world.entity.MobSpawnType.NATURAL || level.canSeeSky(pos)
                            && crowd(level, pos) < (level.getLevel().isNight() ? 6 : 3)),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
