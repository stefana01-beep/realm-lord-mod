package com.realmlord.martial;

import com.realmlord.Notify;
import com.realmlord.RealmLord;
import com.realmlord.martial.ArtData.Art;
import com.realmlord.martial.ArtData.Kind;
import com.realmlord.profile.Innate;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Training and using the martial arts. As in the novel, a manual must be practised: weapon arts
 * by fighting with their weapons, cultivation methods by meditating (crouch and keep still) and
 * a little with every minute, lightness skills by running. Comprehension sets how fast. Each
 * level adds the art's attributes; weapon arts add damage with their weapons, and the great
 * arts unlock their special moves.
 */
@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MartialEvents {
    private MartialEvents() {}

    private static final String LAST_HIT = "realmlord_art_hit", INTENT = "realmlord_art_intent", SHIELD = "realmlord_art_shield";
    private static boolean special;

    /** How fast this player learns: Comprehension 10 is the measure, 20 half again as fast. */
    public static double pace(Profile prof) {
        return Math.max(0.3, 0.5 + prof.innate(Innate.COMPREHENSION) / 20.0);
    }

    /** Practises every learned art of a kind (or all arts, for null); tells of levels gained. */
    public static void practise(ServerPlayer p, Kind kind, double amount) {
        Profile prof = Profiles.get(p);
        int gain = Math.max(1, (int) Math.round(amount * pace(prof)));
        boolean up = false;
        for (String id : java.util.List.copyOf(prof.arts().keySet())) {
            Art a = Arts.byId(id);
            if (a == null || kind != null && !a.kinds().contains(kind)) continue;
            int levels = prof.practise(id, gain);
            if (levels > 0) {
                up = true;
                int lv = prof.artLevel(id);
                Notify.player(p, Component.translatable(lv >= a.maxLevel() ? "realmlord.art.mastered" : "realmlord.art.level_up", Component.translatable(a.key()), lv)
                        .withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            }
        }
        if (up) {
            Profiles.applyAttributes(p);
            Profiles.sync(p);
        }
    }

    /** The level of an art if it has reached its special move, else 0. */
    private static int specialOf(Profile prof, String special) {
        for (var e : prof.arts().entrySet()) {
            Art a = Arts.byId(e.getKey());
            if (a != null && a.special().equals(special) && e.getValue()[0] >= a.specialLevel()) return e.getValue()[0];
        }
        return 0;
    }

    private static boolean has(Profile prof, String special, Kind kind) {
        for (var e : prof.arts().entrySet()) {
            Art a = Arts.byId(e.getKey());
            if (a != null && a.special().equals(special) && e.getValue()[0] >= a.specialLevel() && (kind == null || a.kinds().contains(kind))) return true;
        }
        return false;
    }

    private static void announce(ServerPlayer p, String special) {
        p.displayClientMessage(Component.translatable("realmlord.art.special", Component.translatable("realmlord.art.special." + special))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (special) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        // The attacker's arts.
        if (event.getSource().getEntity() instanceof ServerPlayer p && p != victim) {
            Kind kind = event.getSource().getDirectEntity() instanceof AbstractArrow ? Kind.BOW
                    : event.getSource().getDirectEntity() == p ? Arts.weaponKind(p.getMainHandItem()) : null;
            if (kind != null) {
                Profile prof = Profiles.get(p);
                double mult = 1.0;
                for (var e : prof.arts().entrySet()) {
                    Art a = Arts.byId(e.getKey());
                    if (a != null && a.kinds().contains(kind)) mult += a.dmg() * e.getValue()[0];
                }
                var r = p.getRandom();
                if (kind == Kind.BLADE && has(prof, "flurry", Kind.BLADE) && r.nextInt(7) == 0) {
                    mult += 0.4;
                    announce(p, "flurry");
                }
                if ((kind == Kind.FIST || kind == Kind.SPEAR) && has(prof, "cling_to_mountain", kind) && r.nextInt(7) == 0) {
                    mult += 0.5;
                    Vec3 push = victim.position().subtract(p.position()).normalize();
                    victim.knockback(1.2, -push.x, -push.z);
                    announce(p, "cling_to_mountain");
                }
                if (kind == Kind.SPEAR && has(prof, "throat_killing", Kind.SPEAR) && r.nextInt(8) == 0) {
                    mult += 1.0;
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    announce(p, "throat_killing");
                }
                if (kind == Kind.FIST && has(prof, "dragon_fist", Kind.FIST) && r.nextInt(10) == 0) {
                    special = true;
                    try {
                        float splash = event.getAmount() * 0.5F;
                        for (Mob m : victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(4), m -> m != victim && m instanceof Enemy)) {
                            m.hurt(p.damageSources().playerAttack(p), splash);
                            Vec3 push = m.position().subtract(victim.position()).normalize();
                            m.knockback(1.0, -push.x, -push.z);
                        }
                    } finally {
                        special = false;
                    }
                    announce(p, "dragon_fist");
                }
                event.setAmount((float) (event.getAmount() * mult));
                // Practice: a hit at most every half second counts.
                long now = p.level().getGameTime();
                if (now - p.getPersistentData().getLong(LAST_HIT) >= 10 && !prof.arts().isEmpty()) {
                    p.getPersistentData().putLong(LAST_HIT, now);
                    practise(p, kind, 1);
                }
            }
        }
        // The defender's arts.
        if (victim instanceof ServerPlayer v) {
            Profile prof = Profiles.get(v);
            if (prof.arts().isEmpty()) return;
            long now = v.level().getGameTime();
            var src = event.getSource();
            if (src.getDirectEntity() instanceof LivingEntity && src.getDirectEntity() == src.getEntity()
                    && Arts.weaponKind(v.getMainHandItem()) == Kind.BLADE && has(prof, "guicang_parry", Kind.BLADE) && v.getRandom().nextInt(7) == 0) {
                event.setCanceled(true);
                v.level().playSound(null, v.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.3F);
                announce(v, "guicang_parry");
                return;
            }
            if (specialOf(prof, "qi_shield") > 0 && v.getAbsorptionAmount() <= 0 && now - v.getPersistentData().getLong(SHIELD) >= 600) {
                v.getPersistentData().putLong(SHIELD, now);
                v.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                announce(v, "qi_shield");
            }
            if (specialOf(prof, "fighting_intent") > 0 && v.getHealth() - event.getAmount() < v.getMaxHealth() * 0.3F
                    && now - v.getPersistentData().getLong(INTENT) >= 6000) {
                v.getPersistentData().putLong(INTENT, now);
                v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
                announce(v, "fighting_intent");
            }
        }
    }

    @SubscribeEvent
    public static void kill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() == p) return;
        Profile prof = Profiles.get(p);
        if (prof.arts().isEmpty()) return;
        Kind kind = event.getSource().getDirectEntity() instanceof AbstractArrow ? Kind.BOW : Arts.weaponKind(p.getMainHandItem());
        if (kind == null) return;
        practise(p, kind, 5);
        if (kind == Kind.BLADE && has(prof, "seven_kills", Kind.BLADE)) {
            MobEffectInstance cur = p.getEffect(MobEffects.DAMAGE_BOOST);
            int amp = cur == null ? 0 : Math.min(2, cur.getAmplifier() + 1);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, amp));
            announce(p, "seven_kills");
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 100 != 0) return;
        Profile prof = Profiles.get(p);
        if (prof.arts().isEmpty()) return;
        Vec3 v = p.getDeltaMovement();
        boolean meditating = p.isCrouching() && p.onGround() && v.x * v.x + v.z * v.z < 1.0E-4 && p.hurtTime == 0;
        if (meditating) practise(p, Kind.INTERNAL, 3);
        else if (p.tickCount % 1200 == 0) practise(p, Kind.INTERNAL, 1);
        if (p.isSprinting()) practise(p, Kind.MOVEMENT, 1);
        if (specialOf(prof, "golden_qi") > 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0F);
    }

    /** Lightness skills soften falls. */
    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Profile prof = Profiles.get(p);
        int light = 0;
        for (var e : prof.arts().entrySet()) {
            Art a = Arts.byId(e.getKey());
            if (a != null && a.special().equals("light_fall")) light += e.getValue()[0];
        }
        if (light > 0) event.setDistance(Math.max(0, event.getDistance() - light * 0.75F));
    }

    /** Hundred Pace Archery: every arrow flies true. */
    @SubscribeEvent
    public static void arrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer p)) return;
        if (specialOf(Profiles.get(p), "hundred_pace") > 0) {
            arrow.setCritArrow(true);
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
        }
    }

    /** Movement speed from lightness skills (for Profiles.applyAttributes). */
    public static double speedBonus(Profile prof) {
        double s = 0;
        for (var e : prof.arts().entrySet()) {
            Art a = Arts.byId(e.getKey());
            if (a != null) s += a.speed() * e.getValue()[0];
        }
        return s;
    }
}
