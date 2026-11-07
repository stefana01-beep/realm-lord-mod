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
    public enum Action { MAP, REFRESH, SHIFT_JOB, BUILD_FENCE, CHOOSE_FREE, UPGRADE, BUY, SELL, BUY_BLUEPRINT, WITHDRAW, CANCEL_SITE, OPEN_RESIDENT, DEMOLISH, CREW, BUY_GOOD, SELL_GOOD, SELL_SPECIAL, HAND_IN, SELL_STORAGE, OPEN_STORAGE, TAKE_GOOD, GROUNDS, DEPOSIT, SPEED, BUY_WARE, BUY_BOOK, BUILD_CITY_WALL, FOUND_AFFILIATE, RECRUIT_VISITOR, AUCTION_BID, COUNCIL_APPROVE, COUNCIL_DISMISS, COUNCIL_AUTO }

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
            case GROUNDS -> {
                // Begin levelling the grounds, or call it off (what is done stays done).
                Building g = com.realmlord.village.Grounds.of(t);
                if (g != null) {
                    Village.demolish(p.serverLevel(), t, g);
                    Notify.player(p, Component.translatable("realmlord.grounds.stopped"));
                } else {
                    com.realmlord.village.Grounds.start(p.serverLevel(), t);
                    Notify.player(p, Component.translatable("realmlord.grounds.started", com.realmlord.village.Grounds.radius(), t.center.getY()));
                }
            }
            case BUILD_CITY_WALL -> {
                String err = Village.start(p, t, BuildingType.CITY_WALL, t.center, 0);
                Notify.player(p, err != null ? Component.translatable(err) : Component.translatable("realmlord.city_wall.started", t.name));
            }
            case FOUND_AFFILIATE -> {
                String err = com.realmlord.village.Affiliates.found(p, t);
                if (err != null) Notify.player(p, err);
            }
            case RECRUIT_VISITOR -> {
                String err = com.realmlord.village.Visitors.recruit(p, t);
                if (err != null) Notify.player(p, err);
            }
            case COUNCIL_APPROVE -> {
                String err = com.realmlord.village.Council.approve(p, t, a);
                if (err != null) Notify.player(p, err);
            }
            case COUNCIL_DISMISS -> com.realmlord.village.Council.dismiss(p, t, a);
            case COUNCIL_AUTO -> com.realmlord.village.Council.setAuto(t, a != 0);
            case AUCTION_BID -> {
                String err = com.realmlord.village.Auction.bid(p, t, a);
                if (err != null) Notify.player(p, err);
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
            case BUY_GOOD -> {
                if (!Village.buyGood(p, t, com.realmlord.village.Goods.byOrdinal(a), Math.max(1, Math.min(100_000, b)))) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case SELL_GOOD -> {
                var g = com.realmlord.village.Goods.byOrdinal(a);
                long units = b < 0 && g != null ? g.stock(t) : Math.max(1, Math.min(100_000, b));
                if (units <= 0 || !Village.sellGood(p, t, g, units)) Notify.player(p, "realmlord.market.not_enough");
            }
            case TAKE_GOOD -> {
                var g = com.realmlord.village.Goods.byOrdinal(a);
                int n = Village.takeGood(p, t, g, (int) Math.max(1, Math.min(640, b)));
                if (n <= 0) Notify.player(p, "realmlord.market.not_enough");
                else p.displayClientMessage(Component.translatable("realmlord.market.taken", n, new net.minecraft.world.item.ItemStack(g.itemOf()).getHoverName()), true);
            }
            case BUY_WARE -> {
                if (!Village.buyWare(p, a)) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case BUY_BOOK -> {
                if (!Village.buyBook(p, a)) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case SELL_SPECIAL -> {
                // a: one inventory slot, or -1 for everything the market buys.
                long got = Village.sellSpecial(p, a);
                Notify.player(p, got > 0 ? Component.translatable("realmlord.market.sold_special", com.realmlord.village.Coins.format(got))
                        : Component.translatable("realmlord.market.nothing_special"));
            }
            case HAND_IN -> {
                int n = com.realmlord.village.Goods.handIn(t, p.getInventory());
                Notify.player(p, Component.translatable("realmlord.storage.handed_in", n));
            }
            case SELL_STORAGE -> {
                // a: one good (its ordinal), or -1 for everything the market takes.
                com.realmlord.village.Goods only = a >= 0 ? com.realmlord.village.Goods.byOrdinal(a) : null;
                long got = only != null ? com.realmlord.village.Goods.sellOf(t.storage, only) : a < 0 ? com.realmlord.village.Goods.sellAll(t.storage) : 0;
                com.realmlord.village.Coins.give(p, got);
                Notify.player(p, Component.translatable("realmlord.storage.sold", com.realmlord.village.Coins.format(got)));
            }
            case OPEN_STORAGE -> {
                // The storehouse keeps itself in order: it is tidied each time it is opened.
                com.realmlord.menu.Sorting.sort(t.storage, 0, t.storage.getContainerSize());
                int pages = t.storage.getContainerSize() / com.realmlord.menu.StorehouseMenu.PAGE;
                net.minecraftforge.network.NetworkHooks.openScreen(p, new net.minecraft.world.SimpleMenuProvider(
                        (win, inv, pl) -> new com.realmlord.menu.StorehouseMenu(win, inv, t.storage, data::touch),
                        Component.translatable("realmlord.storage.title", t.name)), buf -> buf.writeVarInt(pages));
                data.touch();
                return;
            }
            case BUY_BLUEPRINT -> {
                BuildingType type = BuildingType.byOrdinal(a);
                if (type == null || !Village.buyBlueprint(p, type)) Notify.player(p, "realmlord.market.cannot_afford");
            }
            case SPEED -> {
                // For testing: creative players and operators may speed time up.
                if (p.isCreative() || p.hasPermissions(2)) {
                    Village.speed = Math.max(1, Math.min(60, a));
                    Notify.player(p, Component.translatable("realmlord.speed.set", Village.speed));
                }
            }
            case DEPOSIT -> {
                // From the purse (and any coins carried) into the manor's treasury, for the market later.
                Coins.pocket(p);
                long amount = Math.max(0, Math.min(Coins.purse(p), b));
                if (amount > 0 && Coins.pay(p, amount)) {
                    t.treasury += amount;
                    Notify.player(p, Component.translatable("realmlord.manor.deposited", Coins.format(amount)));
                }
            }
            case WITHDRAW -> {
                long amount = Math.max(0, Math.min(t.treasury, b));
                t.treasury -= amount;
                Coins.give(p, amount);
            }
            case CANCEL_SITE -> {
                Building site = t.building(a);
                if (site != null && !site.complete && !site.demolishing) Village.demolish(p.serverLevel(), t, site);
            }
            case CREW -> {
                // b: +1 or -1 builders on site a. Below zero returns the site to sharing the free builders.
                Building site = t.building(a);
                if (site == null || site.complete) return;
                Village.allocateBuilders(t);
                int now = Village.buildersAt(t, site);
                int want = (int) (now + b);
                if (want < 0 || (b < 0 && site.crew < 0 && now == 0)) {
                    site.crew = -1;
                } else {
                    site.crew = want;
                    // Not enough builders for all the crews asked for: take on idle residents.
                    int asked = 0;
                    for (Building o : t.buildings) if (!o.complete && o.crew > 0) asked += o.crew;
                    int have = Village.count(t, com.realmlord.village.Job.CONSTRUCTION);
                    if (asked > have) Village.shift(t, com.realmlord.village.Job.CONSTRUCTION, asked - have);
                }
                Village.allocateBuilders(t);
            }
            case DEMOLISH -> {
                Building b = t.building(a);
                if (b != null && b.complete) {
                    Village.demolish(p.serverLevel(), t, b);
                    Notify.player(p, Component.translatable("realmlord.build.demolishing", Component.translatable(b.type.key())));
                }
            }
            case MAP -> {
                if (a >= 1 && a <= 3) TerritoryMapPacket.send(p, t, b == 1, a - 1);
                else TerritoryMapPacket.send(p, t, b == 1);
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
