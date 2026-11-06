package com.realmlord.network;

import com.realmlord.menu.Sorting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: tidy the open chest, or take everything out of it. */
public class ChestActionPacket {
    public static final int SORT = 0, TAKE_ALL = 1, STORE_ALL = 2;
    private final int containerId, action;

    public ChestActionPacket(int containerId, int action) {
        this.containerId = containerId;
        this.action = action;
    }

    public ChestActionPacket(FriendlyByteBuf buf) {
        containerId = buf.readVarInt();
        action = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        var menu = p.containerMenu;
        if (menu.containerId != containerId || !(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) return;
        if (!menu.stillValid(p)) return;
        switch (action) {
            case SORT -> Sorting.sortMenu(menu, p.getInventory());
            case TAKE_ALL -> Sorting.takeAll(menu, p);
            case STORE_ALL -> Sorting.storeAll(menu, p);
            default -> { }
        }
    }
}
