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
    public enum Action { SET_JOB, STAFF, TRADE, REFRESH, INVENTORY, FOLLOW, MILITIA, OFFICE, EQUIP }

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
                else {
                    com.realmlord.village.Workshops.dismiss(r);
                    Village.assign(t, r, job, -1);
                }
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
            case FOLLOW -> {
                if (!owner) return;
                if (a == 1) {
                    if (!r.isTalent()) Notify.player(p, "realmlord.companion.err.talent");
                    else if (r.office >= 0 || r.militia) Notify.player(p, "realmlord.companion.err.busy");
                    else if (r.woundedUntil > com.realmlord.village.GaiaCalendar.today(p.server)) Notify.player(p, "realmlord.companion.err.wounded");
                    else {
                        int n = 0;
                        for (Resident o : t.residents) if (o.follows) n++;
                        if (n >= com.realmlord.village.Village.MAX_COMPANIONS) Notify.player(p, Component.translatable("realmlord.companion.err.many", com.realmlord.village.Village.MAX_COMPANIONS));
                        else {
                            r.follows = true;
                            r.job = Job.IDLE;
                            r.workplace = -1;
                            Notify.player(p, Component.translatable("realmlord.companion.follows", r.name));
                        }
                    }
                } else {
                    r.follows = false;
                    Notify.player(p, Component.translatable("realmlord.companion.stays", r.name));
                }
            }
            case MILITIA -> {
                if (!owner) return;
                if (a == 1 && !r.militia) {
                    // Enlisting costs the Lord their spear and keep.
                    String err = Village.enlist(t, r, false) == null && r.office < 0 && Village.militiaCount(t) < Village.militiaCap(t) ? null : "realmlord.militia.err.full";
                    if (r.office >= 0) err = "realmlord.militia.err.office";
                    if (err == null && !com.realmlord.village.Coins.payWithTreasury(p, Village.ENLIST_COST)) err = "realmlord.market.cannot_afford";
                    if (err == null) err = Village.enlist(t, r, true);
                    if (err != null) Notify.player(p, err);
                    else Notify.player(p, Component.translatable("realmlord.militia.enlisted", r.name, com.realmlord.village.Coins.format(Village.ENLIST_COST)));
                } else if (a == 0) {
                    Village.enlist(t, r, false);
                }
            }
            case EQUIP -> {
                if (!owner || !r.militia || r.equipTier >= Village.KIT_COST.length) return;
                long cost = Village.KIT_COST[r.equipTier];
                if (!com.realmlord.village.Coins.payWithTreasury(p, cost)) {
                    Notify.player(p, "realmlord.market.cannot_afford");
                    return;
                }
                r.equipTier++;
                Notify.player(p, Component.translatable("realmlord.militia.equipped", r.name, Component.translatable("realmlord.militia.kit." + r.equipTier), com.realmlord.village.Coins.format(cost)));
            }
            case OFFICE -> {
                if (!owner) return;
                if (a < 0) {
                    if (r.office >= 0) com.realmlord.village.Office.appoint(t, com.realmlord.village.Office.byId(r.office), null);
                } else {
                    String err = com.realmlord.village.Office.appoint(t, com.realmlord.village.Office.byId(a), r);
                    if (err != null) Notify.player(p, err);
                    else Village.tell(p.serverLevel(), t, Component.translatable("realmlord.office.appointed", r.name, Component.translatable(com.realmlord.village.Office.byId(a).key())));
                }
            }
            case INVENTORY -> {
                if (!owner) return;
                p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inv, pl) -> new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inv, r.inv, 1),
                        Component.translatable("realmlord.resident.pack", r.name)));
                data.touch();
                return;
            }
        }
        data.touch();
        ResidentViewPacket.send(p, territory, resident, false);
    }

}
