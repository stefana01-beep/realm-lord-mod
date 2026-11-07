package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.registry.ModArms;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TokenGrade;
import com.realmlord.world.WorldState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The Auction House. The novel's auctions (chapters 100-102) sold building blueprints, graded
 * weapons, secret manuals and rarer things, and the lords of China bid against one another, and
 * against the great families, for them.
 *
 * <p>The auction is held for the whole world. Five lots go up at a time and stay open for a
 * month: rare town blueprints, Gold-grade weapons, Gold-grade secret manuals, technical manuals, the
 * warship plans, and now and then a Summoning Token. A Lord whose town has an Auction House bids
 * from its window (the market's Auction platform): each bid is a tenth over the last and is paid
 * at once from the treasury; outbid, the coins come back. Other bidders (the great families and
 * lords of the world) raise the price each day up to what the lot is worth to them. When the lot
 * closes, the highest bidder's prize goes to their storehouse.
 */
public final class Auction {
    private Auction() {}

    public static final int LOTS = 5;
    private static final String[] RIVALS = {"Di Chen", "Snow-War Rose", "the Feng Family", "Jian Shang", "the Song Family", "Chun Shenjun",
            "the Fallen Moon Guild", "Shi Jun"};

    public static ListTag lots(WorldState w) {
        return w.ext.getList("auction", Tag.TAG_COMPOUND);
    }

    public static boolean open(Territory t) {
        return Village.countComplete(t, BuildingType.AUCTION_HOUSE) > 0;
    }

    /** Once a day for the world: rivals bid, lots close, new lots go up. */
    public static void day(ServerLevel level, long day) {
        WorldState w = WorldState.get(level.getServer());
        if (w.ext.getLong("auctionDay") == day) return;
        w.ext.putLong("auctionDay", day);
        RandomSource r = level.getRandom();
        ListTag l = lots(w);
        for (int i = l.size() - 1; i >= 0; i--) {
            CompoundTag lot = l.getCompound(i);
            if (lot.getLong("ends") <= day) {
                close(level, lot);
                l.remove(i);
                continue;
            }
            long price = lot.getLong("price");
            if (lot.getLong("ceiling") > price * 11 / 10 && r.nextInt(2) == 0) {
                refund(level, lot);
                lot.putLong("price", price * 115 / 100);
                lot.putString("bidder", "");
                lot.putString("rival", RIVALS[r.nextInt(RIVALS.length)]);
            }
        }
        while (l.size() < LOTS) l.add(newLot(r, day));
        w.ext.put("auction", l);
        w.touch();
    }

    private static CompoundTag newLot(RandomSource r, long day) {
        ItemStack item;
        long value;
        switch (r.nextInt(10)) {
            case 0, 1 -> {
                BuildingType bt = BuildingType.ofGrade(4 + r.nextInt(3)).get(0);
                java.util.List<BuildingType> bps = new java.util.ArrayList<>();
                for (int g = 4; g <= 6; g++) for (BuildingType b : BuildingType.ofGrade(g)) if (b.blueprint) bps.add(b);
                if (!bps.isEmpty()) bt = bps.get(r.nextInt(bps.size()));
                item = com.realmlord.loot.Blueprints.of(bt.id());
                value = bt.blueprintPrice() * 3 / 2;
            }
            case 2, 3 -> {
                var arms = ModArms.arms();
                item = com.realmlord.loot.Gear.make(arms.get(r.nextInt(arms.size())).get(), TokenGrade.GOLD, r);
                value = 300_000;
            }
            case 4, 5 -> {
                item = com.realmlord.martial.Arts.random(r, com.realmlord.martial.ArtGrade.GOLD);
                value = 400_000;
            }
            case 6, 7 -> {
                item = com.realmlord.work.TechManuals.random(r, 2);
                value = 120_000;
            }
            case 8 -> {
                item = com.realmlord.loot.Manuals.of(r.nextBoolean() ? com.realmlord.loot.Manuals.WARSHIPS : com.realmlord.loot.Manuals.SIEGE_ENGINES);
                value = 250_000;
            }
            default -> {
                item = com.realmlord.item.SummoningTokenItem.of(0);
                value = 1_500_000;
            }
        }
        CompoundTag lot = new CompoundTag();
        lot.put("item", item.save(new CompoundTag()));
        lot.putLong("price", value * 4 / 10);
        lot.putLong("ceiling", Math.round(value * (0.8 + r.nextDouble() * 0.8)));
        lot.putLong("ends", day + 2L * GaiaCalendar.daysPerMonth());
        lot.putString("bidder", "");
        lot.putString("rival", "");
        return lot;
    }

    public static ItemStack item(CompoundTag lot) {
        return ItemStack.of(lot.getCompound("item"));
    }

    /** The next bid on a lot: a tenth over the price (the opening price if nobody has bid). */
    public static long nextBid(CompoundTag lot) {
        boolean anyone = !lot.getString("bidder").isEmpty() || !lot.getString("rival").isEmpty();
        return anyone ? lot.getLong("price") * 11 / 10 : lot.getLong("price");
    }

    /** A Lord bids on lot i. Returns an error key or null. */
    public static String bid(ServerPlayer p, Territory t, int i) {
        if (!open(t)) return "realmlord.auction.err.house";
        WorldState w = WorldState.get(p.server);
        ListTag l = lots(w);
        if (i < 0 || i >= l.size()) return "realmlord.auction.err.lot";
        CompoundTag lot = l.getCompound(i);
        if (lot.getString("bidder").equals(p.getUUID().toString())) return "realmlord.auction.err.leading";
        long bid = nextBid(lot);
        if (!Coins.payWithTreasury(p, bid)) return "realmlord.market.cannot_afford";
        refund(p.serverLevel(), lot);
        lot.putLong("price", bid);
        lot.putString("bidder", p.getUUID().toString());
        lot.putInt("territory", t.id);
        lot.putString("rival", "");
        w.ext.put("auction", l);
        w.touch();
        Notify.player(p, Component.translatable("realmlord.auction.bid", item(lot).getHoverName(), Coins.format(bid)));
        return null;
    }

    /** Gives the leading Lord their coins back (they have been outbid). */
    private static void refund(ServerLevel level, CompoundTag lot) {
        String who = lot.getString("bidder");
        if (who.isEmpty()) return;
        Territory t = TerritoryData.get(level.getServer()).byId(lot.getInt("territory"));
        if (t != null) t.treasury += lot.getLong("price");
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(UUID.fromString(who));
        if (p != null) Notify.player(p, Component.translatable("realmlord.auction.outbid", item(lot).getHoverName(), Coins.format(lot.getLong("price"))));
        lot.putString("bidder", "");
    }

    private static void close(ServerLevel level, CompoundTag lot) {
        String who = lot.getString("bidder");
        if (who.isEmpty()) return;
        Territory t = TerritoryData.get(level.getServer()).byId(lot.getInt("territory"));
        if (t == null) return;
        t.storage.addItem(item(lot));
        Village.tell(level, t, Component.translatable("realmlord.auction.won", item(lot).getHoverName(), Coins.format(lot.getLong("price")), t.name));
    }
}
