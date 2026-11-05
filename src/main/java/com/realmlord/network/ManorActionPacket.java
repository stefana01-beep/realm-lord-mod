package com.realmlord.network;

import com.realmlord.Notify;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Coins;
import com.realmlord.village.Job;
import com.realmlord.village.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: buttons on the Manor screen. */
public class ManorActionPacket {
    public enum Action { MAP, REFRESH, SHIFT_JOB, BUILD_FENCE, CHOOSE_FREE, UPGRADE, BUY, SELL, BUY_BLUEPRINT, WITHDRAW, CANCEL_SITE, OPEN_RESIDENT }

    private final Action action;
    private final int a;
    private final long b;

    public ManorActionPacket(Action action, int a, long b) {
        this.action = action;
        this.a = a;
        this.b = b;
    }

    public ManorActionPacket(Action action) {
        this(action, 0, 0);
    }

    public ManorActionPacket(FriendlyByteBuf buf) {
        action = buf.readEnum(Action.class);
        a = buf.readVarInt();
        b = buf.readVarLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(a);
        buf.writeVarLong(b);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        TerritoryData data = TerritoryData.get(p.server);
        int id = Profiles.get(p).getTerritoryId();
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return;
        switch (action) {
            case REFRESH -> { }
            case SHIFT_JOB -> {
                Job job = Job.byId(a);
                if (job != Job.IDLE && job != Job.STAFF) Village.shift(t, job, (int) Math.max(-50, Math.min(50, b)));
            }
            case BUILD_FENCE -> {
                String err = Village.start(p, t, BuildingType.SIMPLE_FENCE, t.center, 0);
                if (err != null) Notify.player(p, err);
            }
            case CHOOSE_FREE -> {
                BuildingType type = BuildingType.byOrdinal(a);
                if (type != null && Village.chooseFree(t, type)) {
                    Notify.player(p, Component.translatable("realmlord.upgrade.free_chosen", Component.translatable(type.key())));
                }
            }
            case UPGRADE -> Village.upgrade(p, t);
            case BUY -> {
                if (!Village.buy(p, t, a, Math.max(1, Math.min(10_000, b)))) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case SELL -> {
                if (!Village.sell(p, t, a, Math.max(1, Math.min(10_000, b)))) Notify.player(p, "realmlord.market.not_enough");
            }
            case BUY_BLUEPRINT -> {
                BuildingType type = BuildingType.byOrdinal(a);
                if (type == null || !Village.buyBlueprint(p, type)) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case WITHDRAW -> {
                long amount = Math.max(0, Math.min(t.treasury, b));
                t.treasury -= amount;
                Coins.give(p, amount);
            }
            case CANCEL_SITE -> {
                Building site = t.building(a);
                if (site != null && !site.complete) Village.demolish(t, site);
            }
            case MAP -> {
                TerritoryMapPacket.send(p, t, b == 1);
                return;
            }
            case OPEN_RESIDENT -> {
                ResidentViewPacket.open(p, t.id, a);
                return;
            }
        }
        data.touch();
        ModNetwork.sync(p);
        ManorViewPacket.send(p, t, false);
    }
}
