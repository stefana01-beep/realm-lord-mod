package com.realmlord.entity.level;

import com.realmlord.RealmLord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Levels and grades as creatures enter the world, and Gaia's overworld kept to its own
 * creatures: no zombies, skeletons, creepers, phantoms, raiders or the like.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LevelEvents {
    private LevelEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof AbstractHorse h) Horses.onJoin(h);
        else if (event.getEntity() instanceof Mob m && MobLevels.leveled(m)) MobLevels.onJoin(level, m);
    }

    @SubscribeEvent
    public static void foal(BabyEntitySpawnEvent event) {
        if (event.getChild() instanceof AbstractHorse child && event.getParentA() instanceof AbstractHorse a && event.getParentB() instanceof AbstractHorse b
                && Horses.graded(child)) {
            var ga = Horses.grade(a);
            var gb = Horses.grade(b);
            if (ga != null && gb != null) Horses.setGrade(child, Horses.foal(ga.ordinal(), gb.ordinal(), child.getRandom()), child.getRandom());
        }
    }

    /** The overworld's own monsters are Gaia's beasts and bandits; Minecraft's don't come. */
    @SubscribeEvent
    public static void spawning(MobSpawnEvent.FinalizeSpawn event) {
        if (!com.realmlord.RealmConfig.novelMobsOnly()) return;
        Mob m = event.getEntity();
        if (!m.level().dimension().equals(Level.OVERWORLD)) return;
        if (!"minecraft".equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(m.getType()).getNamespace())) return;
        if (m.getType().getCategory() != MobCategory.MONSTER) return;
        MobSpawnType t = event.getSpawnType();
        if (t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.PATROL || t == MobSpawnType.EVENT
                || t == MobSpawnType.REINFORCEMENT || t == MobSpawnType.JOCKEY) {
            event.setSpawnCancelled(true);
        }
    }
}
