package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Affiliated villages. Once Shanhai was a town it set up affiliated villages (Beihai Village, then
 * Qiushui) under village chiefs it appointed, sent its overflow of people to them when the town hit
 * its population limit, and drew grain and taxes from them; in time they grew into towns of their
 * own.
 *
 * <p>From Grade 1 Town the Lord may found one affiliated village for each grade of the town
 * (from the Lord's Manor: ten gold, 500 wood and 300 stone, and fifty settlers who go to live
 * there). An affiliated village grows by itself, two people in a hundred a day, up to its grade's
 * limit (200, then 500, then 1,000), and rises a grade as it fills. Each day it sends the town a
 * copper a head in tax and half a grain a head; and when the town is over its limit the extra
 * people move out to the affiliated villages with room.
 */
public final class Affiliates {
    private Affiliates() {}

    public static final long COST = 100_000;
    public static final int WOOD = 500, STONE = 300, SETTLERS = 50;
    public static final int[] LIMITS = {200, 500, 1000};
    private static final String[] NAMES = {"Beihai", "Qiushui", "Lianhua", "Qingshan", "Baishui", "Taoyuan", "Yunxi", "Shilin", "Longtan",
            "Fengming", "Liuxi", "Jinsha"};

    public static ListTag list(Territory t) {
        return t.ext.getList("affiliates", Tag.TAG_COMPOUND);
    }

    public static int allowed(Territory t) {
        if (!t.level.atLeast(TerritoryLevel.TOWN_1)) return 0;
        return Math.min(3, t.level.ordinal() - TerritoryLevel.TOWN_1.ordinal() + 1);
    }

    public static int population(Territory t) {
        int n = 0;
        for (Tag tg : list(t)) n += ((CompoundTag) tg).getInt("pop");
        return n;
    }

    /** Founds an affiliated village. Returns an error key or null. */
    public static String found(ServerPlayer p, Territory t) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        ListTag l = list(t);
        if (l.size() >= allowed(t)) return allowed(t) == 0 ? "realmlord.affiliate.err.town" : "realmlord.affiliate.err.max";
        if (t.wood < WOOD || t.stone < STONE) return "realmlord.build.err.resources";
        int movable = 0;
        for (Resident r : t.residents) if (movable(r)) movable++;
        if (movable < SETTLERS + 20) return "realmlord.affiliate.err.people";
        if (!Coins.payWithTreasury(p, COST)) return "realmlord.market.cannot_afford";
        t.wood -= WOOD;
        t.stone -= STONE;
        int moved = 0;
        for (int i = t.residents.size() - 1; i >= 0 && moved < SETTLERS; i--) {
            Resident r = t.residents.get(i);
            if (!movable(r)) continue;
            Village.remove(t, r);
            moved++;
        }
        CompoundTag a = new CompoundTag();
        String name = NAMES[Math.floorMod(t.id * 7 + l.size() * 3, NAMES.length)];
        for (Tag tg : l) if (((CompoundTag) tg).getString("name").equals(name)) name = NAMES[(java.util.Arrays.asList(NAMES).indexOf(name) + 1) % NAMES.length];
        a.putString("name", name);
        a.putInt("pop", moved);
        a.putInt("grade", 1);
        a.putDouble("carry", 0);
        l.add(a);
        t.ext.put("affiliates", l);
        Notify.player(p, Component.translatable("realmlord.affiliate.founded", name, t.name, moved));
        return null;
    }

    static boolean movable(Resident r) {
        return !r.isFigure() && r.office < 0 && !r.militia && !r.follows && r.grade < 0;
    }

    public static void day(ServerLevel level, Territory t, long day, RandomSource rnd) {
        ListTag l = list(t);
        if (l.isEmpty()) return;
        long tax = 0, grain = 0;
        for (Tag tg : l) {
            CompoundTag a = (CompoundTag) tg;
            int g = Math.max(1, Math.min(3, a.getInt("grade")));
            int pop = a.getInt("pop");
            double carry = a.getDouble("carry") + pop * 0.02 + 1;
            int born = (int) carry;
            a.putDouble("carry", carry - born);
            pop = Math.min(LIMITS[g - 1], pop + born);
            if (pop >= LIMITS[g - 1] && g < 3) {
                g++;
                Village.tell(level, t, Component.translatable("realmlord.affiliate.grew", a.getString("name"), g));
            }
            a.putInt("grade", g);
            a.putInt("pop", pop);
            tax += pop;
            grain += pop / 2;
        }
        t.treasury += tax;
        t.food += grain;
        // A town over its limit sends its extra people out to the affiliated villages.
        int over = t.residents.size() - t.level.populationCap();
        for (int i = t.residents.size() - 1; i >= 0 && over > 0; i--) {
            Resident r = t.residents.get(i);
            if (!movable(r)) continue;
            CompoundTag room = null;
            for (Tag tg : l) {
                CompoundTag a = (CompoundTag) tg;
                if (a.getInt("pop") < LIMITS[Math.max(1, Math.min(3, a.getInt("grade"))) - 1]) room = a;
            }
            if (room == null) break;
            Village.remove(t, r);
            room.putInt("pop", room.getInt("pop") + 1);
            over--;
        }
        t.ext.put("affiliates", l);
    }
}
