package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Building;
import com.realmlord.village.Resident;
import com.realmlord.village.Views;
import com.realmlord.village.Workshops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: open a building's window, choose what it makes, or send someone to learn there (or call them back). */
public class BuildingActionPacket {
    public enum Action { OPEN, MODE, ENROL, DISMISS }

    private final int building;
    private final Action action;
    private final int arg;

    public BuildingActionPacket(int building, Action action, int arg) {
        this.building = building;
        this.action = action;
        this.arg = arg;
    }

    public BuildingActionPacket(FriendlyByteBuf buf) {
        building = buf.readVarInt();
        action = buf.readEnum(Action.class);
        arg = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(building);
        buf.writeEnum(action);
        buf.writeVarInt(arg);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return;
        Building b = building < 0 ? null : t.building(building);
        if (b != null && !Views.managed(b)) b = null;
        switch (action) {
            case MODE -> {
                if (b != null && arg >= 0 && arg < Workshops.modes(b.type).size()) b.mode = arg;
            }
            case ENROL -> {
                Resident r = t.resident(arg);
                if (b != null && r != null) {
                    String err = Workshops.enrol(t, b, r);
                    if (err != null) Notify.player(p, err);
                }
            }
            case DISMISS -> {
                Resident r = t.resident(arg);
                if (r != null && b != null && r.trainingAt == b.id) Workshops.dismiss(r);
            }
            default -> { }
        }
        data.touch();
        BuildingViewPacket.send(p, t, b, action == Action.OPEN);
    }
}
