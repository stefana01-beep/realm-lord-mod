package com.realmlord.skill;

import com.realmlord.Notify;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Skill;
import com.realmlord.profile.Stat;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carries out the combat skills. Intelligence lengthens effects and shortens cooldowns;
 * Force adds to physical skills' damage, Intelligence to stratagems and elixirs.
 */
public final class Skills {
    private Skills() {}

    public static void useSelected(ServerPlayer p) {
        Profile prof = Profiles.get(p);
        Skill s = prof.selectedSkill();
        if (s == null) {
            Notify.player(p, "realmlord.skill.none");
            return;
        }
        use(p, s);
    }

    /** Uses a skill if it is unlocked and ready. Returns true if it fired. */
    public static boolean use(ServerPlayer p, Skill s) {
        Profile prof = Profiles.get(p);
        if (!prof.unlockedSkills().contains(s)) return false;
        long now = p.serverLevel().getGameTime();
        if (prof.cooldownEnd(s) > now) {
            p.displayClientMessage(Component.translatable("realmlord.skill.cooldown", Component.translatable(s.key()),
                    (prof.cooldownEnd(s) - now + 19) / 20), true);
            return false;
        }
        boolean ok = perform(p, prof, s);
        if (ok) {
            prof.setCooldownEnd(s, now + cooldownTicks(prof, s));
            p.displayClientMessage(Component.translatable(s.key()).withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
        return ok;
    }

    public static int cooldownTicks(Profile prof, Skill s) {
        double cut = Math.min(0.5, prof.stat(Stat.INTELLIGENCE) * 0.005);
        return (int) Math.round(s.cooldownSeconds * 20 * (1.0 - cut));
    }

    /** Effect durations grow by 1% per point of Intelligence, up to double. */
    private static int dur(Profile prof, int ticks) {
        return (int) Math.round(ticks * (1.0 + Math.min(1.0, prof.stat(Stat.INTELLIGENCE) * 0.01)));
    }

    private static float forceDamage(Profile prof, float base, float per) {
        return base + prof.stat(Stat.FORCE) * per;
    }

    private static float intDamage(Profile prof, float base, float per) {
        return base + prof.stat(Stat.INTELLIGENCE) * per;
    }

    private static boolean perform(ServerPlayer p, Profile prof, Skill s) {
        ServerLevel level = p.serverLevel();
        switch (s) {
            // ------------------------------------------------ Strategist
            case FIRE_STRATAGEM -> {
                Vec3 at = lookPoint(p, 24);
                for (LivingEntity e : enemiesNear(p, at, 3.5)) {
                    e.hurt(p.damageSources().indirectMagic(p, p), intDamage(prof, 4F, 0.12F));
                    e.setSecondsOnFire(5);
                }
                burst(level, ParticleTypes.FLAME, at, 60, 1.6);
                burst(level, ParticleTypes.LARGE_SMOKE, at, 20, 1.2);
                sound(level, p, SoundEvents.BLAZE_SHOOT, 1.0F);
            }
            case CONFUSION_ARRAY -> {
                Vec3 at = lookPoint(p, 20);
                for (LivingEntity e : enemiesNear(p, at, 6)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(prof, 120), 1));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(prof, 120), 0));
                    if (e instanceof Mob m) m.setTarget(null);
                }
                burst(level, ParticleTypes.ENCHANT, at, 80, 3.0);
                sound(level, p, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F);
            }
            case BATTLE_INSIGHT -> {
                for (LivingEntity e : enemiesNear(p, p.position(), 32)) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur(prof, 300), 0));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur(prof, 300), 0));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur(prof, 400), 0));
                sound(level, p, SoundEvents.BEACON_POWER_SELECT, 1.2F);
            }
            // ------------------------------------------------ General
            case WAR_CRY -> {
                for (LivingEntity a : alliesNear(p, 8)) {
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur(prof, 160), 0));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur(prof, 160), 0));
                }
                for (LivingEntity e : enemiesNear(p, p.position(), 8)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(prof, 100), 0));
                burst(level, ParticleTypes.ANGRY_VILLAGER, p.position().add(0, 1, 0), 12, 1.5);
                sound(level, p, SoundEvents.RAVAGER_ROAR, 1.2F);
            }
            case CAVALRY_CHARGE -> {
                dash(p, 1.6);
                for (LivingEntity e : enemiesInCone(p, 5.0)) {
                    e.hurt(p.damageSources().playerAttack(p), forceDamage(prof, 6F, 0.12F));
                    Vec3 push = p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2);
                    e.push(push.x, 0.35, push.z);
                }
                burst(level, ParticleTypes.CLOUD, p.position(), 20, 0.8);
                sound(level, p, SoundEvents.HORSE_GALLOP, 1.0F);
            }
            case UNYIELDING_WILL -> {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur(prof, 200), 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur(prof, 200), 1));
                burst(level, ParticleTypes.TOTEM_OF_UNDYING, p.position().add(0, 1, 0), 30, 0.8);
                sound(level, p, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.8F);
            }
            // ------------------------------------------------ Chivalrous expert
            case SWIFT_SWORD -> {
                dash(p, 1.3);
                for (LivingEntity e : enemiesInCone(p, 4.0)) e.hurt(p.damageSources().playerAttack(p), forceDamage(prof, 5F, 0.14F));
                burst(level, ParticleTypes.SWEEP_ATTACK, p.position().add(p.getLookAngle().scale(1.5)).add(0, 1, 0), 6, 1.0);
                sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F);
            }
            case LIGHTNESS_SKILL -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur(prof, 200), 1));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, dur(prof, 200), 1));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur(prof, 200), 0));
                burst(level, ParticleTypes.CLOUD, p.position(), 16, 0.6);
                sound(level, p, SoundEvents.PHANTOM_FLAP, 1.4F);
            }
            case HUNDRED_STRIKES -> {
                List<LivingEntity> targets = enemiesNear(p, p.position(), 4);
                if (targets.isEmpty()) return fail(p);
                for (int hit = 0; hit < 3; hit++) {
                    for (LivingEntity e : targets) {
                        e.invulnerableTime = 0;
                        e.hurt(p.damageSources().playerAttack(p), forceDamage(prof, 3F, 0.07F));
                    }
                }
                burst(level, ParticleTypes.CRIT, p.position().add(0, 1, 0), 60, 2.5);
                sound(level, p, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F);
            }
            // ------------------------------------------------ Alchemist
            case TOXIC_FLASK -> {
                Vec3 at = lookPoint(p, 16);
                AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
                cloud.setOwner(p);
                cloud.setRadius(3.0F);
                cloud.setRadiusOnUse(-0.1F);
                cloud.setWaitTime(5);
                cloud.setDuration(dur(prof, 120));
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                level.addFreshEntity(cloud);
                sound(level, p, SoundEvents.SPLASH_POTION_BREAK, 1.0F);
            }
            case VITALITY_ELIXIR -> {
                for (LivingEntity a : alliesNear(p, 6)) {
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur(prof, 160), 1));
                    a.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur(prof, 400), 0));
                }
                burst(level, ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 1, 0), 20, 1.5);
                sound(level, p, SoundEvents.BREWING_STAND_BREW, 1.0F);
            }
            case THUNDER_PILL -> {
                Vec3 at = lookPoint(p, 20);
                float power = 2.0F + Math.min(2.0F, prof.stat(Stat.INTELLIGENCE) * 0.02F);
                level.explode(p, at.x, at.y, at.z, power, Level.ExplosionInteraction.NONE);
            }
            // ------------------------------------------------ Healer
            case REJUVENATE -> {
                float amount = intDamage(prof, 4F, 0.1F);
                for (LivingEntity a : alliesNear(p, 8)) a.heal(amount);
                burst(level, ParticleTypes.HEART, p.position().add(0, 1, 0), 12, 2.0);
                sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
            }
            case ACUPUNCTURE_NEEDLE -> {
                LivingEntity t = lookTarget(p, 20);
                if (t == null) return fail(p);
                t.hurt(p.damageSources().indirectMagic(p, p), intDamage(prof, 2F, 0.05F));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(prof, 80), 5));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(prof, 80), 1));
                burst(level, ParticleTypes.CRIT, t.position().add(0, t.getBbHeight() * 0.6, 0), 15, 0.3);
                sound(level, p, SoundEvents.ARROW_HIT_PLAYER, 1.6F);
            }
            case PURIFY -> {
                for (LivingEntity a : alliesNear(p, 8)) {
                    List<net.minecraft.world.effect.MobEffect> bad = new ArrayList<>();
                    for (MobEffectInstance i : a.getActiveEffects()) if (i.getEffect().getCategory() == MobEffectCategory.HARMFUL) bad.add(i.getEffect());
                    bad.forEach(a::removeEffect);
                    a.clearFire();
                    a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur(prof, 300), 0));
                }
                burst(level, ParticleTypes.END_ROD, p.position().add(0, 1, 0), 30, 1.5);
                sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.4F);
            }
            // ------------------------------------------------ Bailiff
            case IRON_SHACKLES -> {
                LivingEntity t = lookTarget(p, 16);
                if (t == null) return fail(p);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur(prof, 60), 9));
                t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur(prof, 60), 2));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur(prof, 60), 1));
                burst(level, ParticleTypes.ENCHANTED_HIT, t.position().add(0, 0.5, 0), 20, 0.5);
                sound(level, p, SoundEvents.CHAIN_PLACE, 0.8F);
            }
            case ARREST -> {
                LivingEntity t = lookTarget(p, 20);
                if (t == null) return fail(p);
                Vec3 pull = p.position().subtract(t.position());
                double d = pull.length();
                if (d > 0.1) {
                    Vec3 v = pull.normalize().scale(Math.min(2.2, 0.35 + d * 0.12));
                    t.setDeltaMovement(v.x, 0.4, v.z);
                    t.hurtMarked = true;
                }
                t.hurt(p.damageSources().playerAttack(p), forceDamage(prof, 3F, 0.05F));
                sound(level, p, SoundEvents.FISHING_BOBBER_RETRIEVE, 0.8F);
            }
            case PATROL_SENSE -> {
                for (LivingEntity e : enemiesNear(p, p.position(), 48)) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur(prof, 300), 0));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur(prof, 300), 0));
                sound(level, p, SoundEvents.BELL_BLOCK, 1.0F);
            }
        }
        return true;
    }

    private static boolean fail(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("realmlord.skill.no_target"), true);
        return false;
    }

    // ------------------------------------------------------------------ targeting

    /** Something a skill may hurt: monsters and anything hunting this player, never players or pets. */
    public static boolean isEnemy(Player p, LivingEntity e) {
        if (e == p || !e.isAlive() || e instanceof Player) return false;
        if (e instanceof TamableAnimal t && t.isTame()) return false;
        if (e instanceof Enemy) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    public static boolean isAlly(Player p, LivingEntity e) {
        if (e == p) return true;
        if (e instanceof Player) return e.isAlive() && !e.isSpectator();
        return e instanceof TamableAnimal t && t.isTame() && p.getUUID().equals(t.getOwnerUUID());
    }

    public static List<LivingEntity> enemiesNear(Player p, Vec3 at, double radius) {
        AABB box = new AABB(at, at).inflate(radius);
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e) && e.distanceToSqr(at) <= radius * radius);
    }

    public static List<LivingEntity> alliesNear(Player p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> isAlly(p, e));
    }

    private static List<LivingEntity> enemiesInCone(Player p, double reach) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 eye = p.position();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemiesNear(p, eye, reach)) {
            Vec3 to = e.position().subtract(eye).multiply(1, 0, 1);
            if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) > 0.35) out.add(e);
        }
        return out;
    }

    /** Where the player is looking: the first block hit, or the end of the ray. */
    public static Vec3 lookPoint(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        LivingEntity t = lookTarget(p, range);
        if (t != null && (hit.getType() == HitResult.Type.MISS || t.distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye))) return t.position();
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** The nearest enemy under the crosshair, not hidden behind blocks. */
    public static LivingEntity lookTarget(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult wall = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = wall.getType() == HitResult.Type.MISS ? range * range : wall.getLocation().distanceToSqr(eye);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        AABB area = p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.5);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, area, e -> isEnemy(p, e))) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.4).clip(eye, end);
            if (clip.isEmpty()) continue;
            double d = clip.get().distanceToSqr(eye);
            if (d <= max && d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static void dash(ServerPlayer p, double strength) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize().scale(strength);
        p.setDeltaMovement(look.x, Math.max(0.15, p.getDeltaMovement().y), look.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    private static void burst(ServerLevel level, ParticleOptions type, Vec3 at, int count, double spread) {
        level.sendParticles(type, at.x, at.y + 0.3, at.z, count, spread * 0.5, 0.4, spread * 0.5, 0.02);
    }

    private static void sound(ServerLevel level, Player p, SoundEvent s, float pitch) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 0.9F, pitch);
    }

    private static void sound(ServerLevel level, Player p, net.minecraft.core.Holder<SoundEvent> s, float pitch) {
        sound(level, p, s.value(), pitch);
    }
}
