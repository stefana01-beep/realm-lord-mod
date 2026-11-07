package com.realmlord.ship;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

/** What every ship does, whichever vanilla boat it is built on. */
public final class Ships {
    private Ships() {}

    /** Seats laid out along the deck, two abreast. */
    public static Vec3 seat(ShipKind k, int i) {
        if (k.seats <= 2) return new Vec3(i == 0 ? 0.2 : -0.6, 0, 0);
        int rows = (k.seats + 1) / 2, row = i / 2;
        double len = 1.2 * k.scale;
        double along = rows == 1 ? 0 : len / 2 - row * len / (rows - 1);
        double side = k.seats == 3 && i == 2 ? 0 : (i % 2 == 0 ? 0.35 : -0.35) * k.scale;
        return new Vec3(along, 0, side);
    }

    public static double deck(ShipKind k) {
        return -0.1 + 0.32 * (k.scale - 1.0);
    }

    /** Damage the hull takes: a creative player still breaks it at once. */
    public static float hullDamage(ShipKind k, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild) return amount;
        return amount / k.toughness;
    }

    /** Keeps a ship under way: a scout boat pulls ahead of a plain boat, a heavy ship lags. */
    public static void drive(Boat boat, ShipKind k) {
        if (!boat.isControlledByLocalInstance() || boat.getControllingPassenger() == null) return;
        if (!boat.level().getFluidState(boat.blockPosition()).isSource() && !boat.level().getFluidState(boat.blockPosition().below()).isSource()) return;
        Vec3 v = boat.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        if (speed < 0.02) return;
        double f = speed < k.topSpeed ? 1.0 + (k.topSpeed - 0.4) * 0.12 : k.topSpeed / speed;
        boat.setDeltaMovement(v.x * f, v.y, v.z * f);
    }

    /** The Mengchong rams: what it strikes at speed is hurt and thrown aside. */
    public static void ram(Boat boat) {
        Vec3 v = boat.getDeltaMovement();
        if (v.horizontalDistanceSqr() < 0.06) return;
        Entity driver = boat.getControllingPassenger();
        Vec3 ahead = boat.position().add(v.normalize().scale(1.4));
        for (LivingEntity e : boat.level().getEntitiesOfClass(LivingEntity.class, boat.getBoundingBox().move(v.normalize().scale(0.8)).inflate(0.3),
                e -> e.getVehicle() != boat && e != driver)) {
            e.hurt(driver instanceof LivingEntity d ? boat.damageSources().mobAttack(d) : boat.damageSources().generic(), 6.0F);
            Vec3 push = e.position().subtract(ahead).normalize();
            e.knockback(1.2, -push.x, -push.z);
        }
    }

    /** The turreted ship's crossbowmen loose at the nearest foe, and its clapper poles strike close ones. */
    public static void turret(Boat boat) {
        if (boat.tickCount % 30 == 0) {
            LivingEntity target = null;
            double best = 18 * 18;
            for (LivingEntity e : boat.level().getEntitiesOfClass(LivingEntity.class, boat.getBoundingBox().inflate(18), e -> e instanceof Enemy && e.isAlive())) {
                double dsq = e.distanceToSqr(boat);
                if (dsq < best) { best = dsq; target = e; }
            }
            if (target != null) {
                Arrow a = new Arrow(boat.level(), boat.getX(), boat.getY() + 2.2, boat.getZ());
                if (boat.getControllingPassenger() != null) a.setOwner(boat.getControllingPassenger());
                double dx = target.getX() - a.getX(), dz = target.getZ() - a.getZ();
                double dy = target.getY(0.33) - a.getY() + Math.sqrt(dx * dx + dz * dz) * 0.15;
                a.shoot(dx, dy, dz, 2.2F, 3.0F);
                a.setBaseDamage(4.0);
                a.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
                boat.level().addFreshEntity(a);
            }
        }
        if (boat.tickCount % 80 == 0) {
            for (LivingEntity e : boat.level().getEntitiesOfClass(LivingEntity.class, boat.getBoundingBox().inflate(4), e -> e instanceof Enemy)) {
                e.hurt(boat.damageSources().generic(), 8.0F);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
            }
        }
    }
}
