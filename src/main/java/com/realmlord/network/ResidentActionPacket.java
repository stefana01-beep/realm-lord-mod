package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Coins;
import com.realmlord.village.Job;
import com.realmlord.village.Resident;
import com.realmlord.village.Trades;
import com.realmlord.village.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Client to server: give a resident a job, or trade with them. */
public class ResidentActionPacket {
    public enum Action { SET_JOB, STAFF, TRADE, REFRESH }

    private final Action action;
    private final int territory, resident, a;

    public ResidentActionPacket(Action action, int territory, int resident, int a) {
        this.action = action;
        this.territory = territory;
        this.resident = resident;
        this.a = a;
    }

    public ResidentActionPacket(FriendlyByteBuf buf) {
        action = buf.readEnum(Action.class);
        territory = buf.readVarInt();
        resident = buf.readVarInt();
        a = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(territory);
        buf.writeVarInt(resident);
        buf.writeVarInt(a);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        Territory t = data.byId(territory);
        Resident r = t == null ? null : t.resident(resident);
        if (r == null) return;
        boolean owner = p.getUUID().equals(t.owner);
        switch (action) {
            case SET_JOB -> {
                if (!owner) return;
                Job job = Job.byId(a);
                String err = job == Job.STAFF ? "realmlord.job.err.no_building" : Village.canAssign(t, r, job, -1);
                if (err != null) Notify.player(p, err);
                else Village.assign(t, r, job, -1);
            }
            case STAFF -> {
                if (!owner) return;
                String err = Village.canAssign(t, r, Job.STAFF, a);
                if (err != null) Notify.player(p, err);
                else Village.assign(t, r, Job.STAFF, a);
            }
            case TRADE -> {
                String err = Village.trade(p, t, r, a);
                if (err != null) Notify.player(p, err);
            }
            case REFRESH -> { }
        }
        data.touch();
        ResidentViewPacket.send(p, territory, resident, false);
    }

}
