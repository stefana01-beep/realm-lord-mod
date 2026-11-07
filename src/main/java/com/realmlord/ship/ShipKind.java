package com.realmlord.ship;

/**
 * The novel's craft, from the fishing boat that carries two fishermen to the turreted ship with
 * its clapper poles: scale (of the boat's size), seats, top speed, toughness, and the vanilla
 * hull texture it is painted with.
 */
public enum ShipKind {
    /** "An ordinary fishing boat ... can carry two fishermen out to the sea." Fishing from it brings in more. */
    FISHING_BOAT(1.0F, 2, 0.40, 1.0F, "birch", false),
    /** The cima ("red horse") boat: the fleet's small, fast scout. */
    CIMA_BOAT(1.05F, 2, 0.62, 1.0F, "mangrove", false),
    /** A merchant ship with a hold. */
    MERCHANT_SHIP(1.35F, 3, 0.34, 2.0F, "spruce", true),
    /** The Mengchong: a warship covered in rawhide, built to ram. */
    MENGCHONG(1.4F, 4, 0.46, 4.0F, "acacia", false),
    /** The turreted ship: a floating fortress with crossbowmen and six clapper poles. */
    TURRETED_SHIP(2.0F, 6, 0.32, 7.0F, "dark_oak", false);

    public final float scale;
    public final int seats;
    public final double topSpeed;
    /** Damage taken is divided by this. */
    public final float toughness;
    public final String wood;
    public final boolean cargo;

    ShipKind(float scale, int seats, double topSpeed, float toughness, String wood, boolean cargo) {
        this.scale = scale;
        this.seats = seats;
        this.topSpeed = topSpeed;
        this.toughness = toughness;
        this.wood = wood;
        this.cargo = cargo;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
