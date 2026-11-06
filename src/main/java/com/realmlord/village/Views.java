package com.realmlord.village;

import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Territory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Snapshots of a territory and its residents, sent to the client for the screens. */
public final class Views {
    private Views() {}

    public static CompoundTag manor(ServerPlayer p, Territory t) {
        Village.allocateBuilders(t);
        ServerLevel level = p.serverLevel();
        CompoundTag v = new CompoundTag();
        v.putInt("id", t.id);
        v.putString("name", t.name);
        v.putInt("level", t.level.ordinal());
        v.putInt("grade", t.grade.ordinal());
        v.putInt("thoughts", t.peopleThoughts);
        v.putInt("security", t.security);
        v.putInt("pop", t.residents.size());
        v.putInt("cap", t.level.populationCap());
        v.putInt("housing", Village.housing(t));
        v.putDouble("immigration", Village.immigrationRate(t));
        v.putLong("food", t.food);
        v.putLong("wood", t.wood);
        v.putLong("stone", t.stone);
        v.putLong("ore", t.ironOre);
        v.putLong("treasury", t.treasury);
        v.putLong("tax", Village.dailyTax(t));
        v.putLong("foodUse", Village.foodUse(t));
        v.putLongArray("output", Village.dailyOutput(t));
        v.putIntArray("indexes", Village.indexes(t));
        long day = GaiaCalendar.day(level.getServer().overworld().getGameTime());
        v.putIntArray("date", new int[]{GaiaCalendar.year(day), GaiaCalendar.month(day), GaiaCalendar.dayOfMonth(day)});
        v.putLong("coins", Coins.count(p));
        v.putInt("cx", t.center.getX());
        v.putInt("cz", t.center.getZ());
        v.putInt("radius", t.radius());
        // Buildings
        ListTag bs = new ListTag();
        for (Building b : t.buildings) {
            CompoundTag bt = new CompoundTag();
            bt.putInt("id", b.id);
            bt.putInt("type", b.type.ordinal());
            bt.putInt("x", b.origin.getX());
            bt.putInt("z", b.origin.getZ());
            bt.putInt("sx", b.sizeX());
            bt.putInt("sz", b.sizeZ());
            bt.putBoolean("complete", b.complete);
            bt.putDouble("fraction", b.demolishing ? (b.demolishFrom > 0 ? b.placed / (double) b.demolishFrom : 0) : b.fraction());
            bt.putBoolean("demolish", b.demolishing);
            if (!b.complete) {
                bt.putInt("crew", b.crew);
                bt.putInt("builders", Village.buildersAt(t, b));
            }
            bt.putInt("stage", b.stage);
            if (b.type == BuildingType.SIMPLE_FENCE) {
                bt.putIntArray("gates", b.gates);
                bt.putInt("fr", b.fenceRadius);
            }
            Resident s = Village.staffOf(t, b);
            if (s != null) bt.putString("staff", s.name);
            if (b.type == BuildingType.FARMLAND) bt.putDouble("growth", Village.growth(b, day));
            bs.add(bt);
        }
        v.put("buildings", bs);
        // Construction options for this player
        ListTag opts = new ListTag();
        for (BuildingType type : BuildingType.values()) {
            CompoundTag o = new CompoundTag();
            o.putInt("type", type.ordinal());
            o.putInt("built", Village.countComplete(t, type));
            boolean free = t.freeBuilds.contains(type);
            o.putBoolean("free", free);
            String err = Village.checkCost(p, t, type, free);
            if (err != null) o.putString("err", err);
            o.putBoolean("blueprint", !type.blueprint || hasBlueprint(p, type));
            o.putBoolean("specialist", type.specialist == null || Village.hasSpecialist(t, type.specialist, type.specialistRank));
            opts.add(o);
        }
        v.put("options", opts);
        int[] jobs = new int[Job.values().length], places = new int[Job.values().length];
        for (Job j : Job.values()) {
            jobs[j.ordinal()] = Village.count(t, j);
            places[j.ordinal()] = Math.min(9999, Village.places(t, j));
        }
        v.putIntArray("jobs", jobs);
        v.putIntArray("places", places);
        // Talents and named residents
        ListTag rs = new ListTag();
        for (Resident r : t.residents) {
            if (!r.isTalent()) continue;
            CompoundTag rt = new CompoundTag();
            rt.putInt("id", r.id);
            rt.putString("name", r.name);
            rt.putInt("grade", r.grade);
            rt.putInt("occ", r.occupation.ordinal());
            rt.putInt("rank", r.rank);
            rt.putInt("job", r.job.ordinal());
            Building w = t.building(r.workplace);
            if (w != null) rt.putInt("work", w.type.ordinal());
            rs.add(rt);
        }
        v.put("talents", rs);
        // Upgrade
        Profile owner = Profiles.get(p);
        ListTag missing = new ListTag();
        for (Component c : Village.upgradeMissing(t, owner)) missing.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        v.put("missing", missing);
        v.putBoolean("canUpgrade", t.level.next() != null && missing.isEmpty());
        v.putIntArray("freeChoices", t.freeChoices.stream().mapToInt(Enum::ordinal).toArray());
        v.putIntArray("freeBuilds", t.freeBuilds.stream().mapToInt(Enum::ordinal).toArray());
        return v;
    }

    private static boolean hasBlueprint(ServerPlayer p, BuildingType type) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof com.realmlord.item.BlueprintItem && type.id().equals(com.realmlord.loot.Blueprints.buildingOf(s))) return true;
        }
        return false;
    }

    public static CompoundTag resident(ServerPlayer p, Territory t, Resident r) {
        CompoundTag v = new CompoundTag();
        v.putInt("territory", t.id);
        v.putString("village", t.name);
        v.putBoolean("owner", p.getUUID().equals(t.owner));
        v.put("resident", r.save());
        v.putString("activity", r.activity);
        Building w = t.building(r.workplace);
        if (w != null) v.putInt("workType", w.type.ordinal());
        v.putLong("coins", Coins.count(p));
        // Trades
        ListTag trades = new ListTag();
        boolean shop = Village.hasWorkshop(t, r);
        List<Trades.Offer> offers = Trades.of(r.occupation);
        for (Trades.Offer o : offers) {
            CompoundTag ot = new CompoundTag();
            ot.put("item", o.stack().save(new CompoundTag()));
            ot.putLong("price", o.price());
            ot.putBoolean("sell", o.sell());
            ot.putBoolean("open", !o.needsWorkshop() || shop);
            trades.add(ot);
        }
        v.put("trades", trades);
        // Jobs
        int[] jobs = new int[Job.values().length], places = new int[Job.values().length];
        ListTag errs = new ListTag();
        for (Job j : Job.values()) {
            jobs[j.ordinal()] = Village.count(t, j);
            places[j.ordinal()] = Math.min(9999, Village.places(t, j));
            String e = j == Job.STAFF ? null : Village.canAssign(t, r, j, -1);
            errs.add(StringTag.valueOf(e == null ? "" : e));
        }
        v.putIntArray("jobs", jobs);
        v.putIntArray("places", places);
        v.put("jobErrors", errs);
        // Workplaces this resident could staff
        ListTag staff = new ListTag();
        for (Building b : t.buildings) {
            if (!b.complete || b.type.staff() != r.occupation) continue;
            CompoundTag st = new CompoundTag();
            st.putInt("id", b.id);
            st.putInt("type", b.type.ordinal());
            Resident cur = Village.staffOf(t, b);
            if (cur != null) st.putString("by", cur.name);
            staff.add(st);
        }
        v.put("staff", staff);
        return v;
    }
}
