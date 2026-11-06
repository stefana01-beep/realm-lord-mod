package com.realmlord.entity.beast;

import com.realmlord.entity.SettlerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * What the wild beasts share: a beastwave sends them at a village's manor, and on the way they
 * go for any player or resident they come across.
 */
public final class Beasts {
    private Beasts() {}

    public static final String WAVE_TAG = "realmlord_beastwave";
    public static final String DEN_TAG = "realmlord_den";

    /** Where a beastwave is driving this beast, or null. */
    public static BlockPos raid(Mob m) {
        CompoundTag d = m.getPersistentData();
        return d.contains("realmlord_raid") ? NbtUtils.readBlockPos(d.getCompound("realmlord_raid")) : null;
    }

    public static void setRaid(Mob m, BlockPos target) {
        m.getPersistentData().put("realmlord_raid", NbtUtils.writeBlockPos(target));
        m.setPersistenceRequired();
        m.addTag(WAVE_TAG);
    }

    /** Called every tick on the server: find prey, or march on the manor. */
    public static void think(Mob m) {
        if (m.tickCount % 20 != 0) return;
        BlockPos raid = raid(m);
        LivingEntity target = m.getTarget();
        if (target == null || !target.isAlive()) {
            double reach = raid != null ? 20 : 12;
            LivingEntity prey = null;
            double best = reach * reach;
            for (LivingEntity e : m.level().getEntitiesOfClass(LivingEntity.class, new AABB(m.blockPosition()).inflate(reach),
                    e -> e.isAlive() && (e instanceof Player p && !p.isCreative() && !p.isSpectator() || e instanceof SettlerEntity s && !s.isCaptive() && !s.sheltered()))) {
                double d = e.distanceToSqr(m);
                if (d < best && m.getSensing().hasLineOfSight(e)) {
                    best = d;
                    prey = e;
                }
            }
            if (prey != null) {
                m.setTarget(prey);
                if (m instanceof net.minecraft.world.entity.monster.hoglin.Hoglin h) {
                    h.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, prey);
                }
                return;
            }
            if (raid != null && (m.getNavigation().isDone() || m.tickCount % 100 == 0)) {
                m.getNavigation().moveTo(raid.getX() + 0.5, raid.getY(), raid.getZ() + 0.5, 1.0);
            }
        }
    }
}
