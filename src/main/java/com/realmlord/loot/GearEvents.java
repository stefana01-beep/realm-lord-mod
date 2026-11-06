package com.realmlord.loot;

import com.realmlord.RealmLord;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Graded weapons' effects as they strike. */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GearEvents {
    private GearEvents() {}

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        Gear.Effect e = Gear.effectOf(attacker.getMainHandItem());
        if (e == null || !e.weapon) return;
        LivingEntity target = event.getEntity();
        switch (e) {
            case LIFESTEAL -> attacker.heal(event.getAmount() * 0.15F);
            case FLAME -> target.setSecondsOnFire(4);
            case FROST -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case BEASTBANE -> {
                if (target instanceof Mob m && com.realmlord.entity.level.MobLevels.leveled(m)
                        && !(m instanceof com.realmlord.entity.BanditEntity)) event.setAmount(event.getAmount() * 1.3F);
            }
            default -> { }
        }
    }
}
