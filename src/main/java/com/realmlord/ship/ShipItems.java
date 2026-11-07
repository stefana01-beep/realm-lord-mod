package com.realmlord.ship;

import com.realmlord.registry.ModCrafts;
import net.minecraft.world.item.Item;

public final class ShipItems {
    private ShipItems() {}

    public static Item item(ShipKind k) {
        return switch (k) {
            case FISHING_BOAT -> ModCrafts.FISHING_BOAT.get();
            case CIMA_BOAT -> ModCrafts.CIMA_BOAT.get();
            case MERCHANT_SHIP -> ModCrafts.MERCHANT_SHIP.get();
            case MENGCHONG -> ModCrafts.MENGCHONG_WARSHIP.get();
            case TURRETED_SHIP -> ModCrafts.TURRETED_SHIP.get();
        };
    }
}
