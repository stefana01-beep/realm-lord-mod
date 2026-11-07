package com.realmlord.ship;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The merchant ship: seats and a hold (open it crouching, as a chest boat). */
public class CargoShipEntity extends ChestBoat {
    public final ShipKind kind;

    public CargoShipEntity(EntityType<? extends Boat> type, Level level, ShipKind kind) {
        super(type, level);
        this.kind = kind;
    }

    @Override
    protected int getMaxPassengers() {
        return kind.seats;
    }

    @Override
    public double getPassengersRidingOffset() {
        return Ships.deck(kind);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        super.positionRider(passenger, move);
        if (!hasPassenger(passenger)) return;
        Vec3 seat = Ships.seat(kind, getPassengers().indexOf(passenger)).yRot(-getYRot() * ((float) Math.PI / 180F) - ((float) Math.PI / 2F));
        move.accept(passenger, getX() + seat.x, getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(), getZ() + seat.z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, Ships.hullDamage(kind, source, amount));
    }

    @Override
    public void tick() {
        super.tick();
        Ships.drive(this, kind);
    }

    @Override
    public Item getDropItem() {
        return ShipItems.item(kind);
    }
}
