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

import java.util.ArrayList;
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
        net.minecraft.nbt.ListTag goods = new net.minecraft.nbt.ListTag();
        double[] daily = Goods.daily(t);
        for (Goods g : Goods.values()) {
            CompoundTag gt = new CompoundTag();
            gt.putInt("i", g.ordinal());
            gt.putLong("stock", g.stock(t));
            gt.putDouble("daily", daily[g.ordinal()]);
            goods.add(gt);
        }
        v.put("goods", goods);
        v.putString("learned", String.join(",", t.blueprints));
        int stored = 0;
        for (int i = 0; i < t.storage.getContainerSize(); i++) if (!t.storage.getItem(i).isEmpty()) stored++;
        v.putInt("stored", stored);
        v.putLong("treasury", t.treasury);
        v.putLong("tax", Village.dailyTax(t));
        v.putLong("foodUse", Village.foodUse(t));
        v.putLongArray("output", Village.dailyOutput(t));
        v.putIntArray("indexes", Village.indexes(t));
        long day = GaiaCalendar.today(level.getServer());
        v.putIntArray("date", new int[]{GaiaCalendar.year(day), GaiaCalendar.month(day), GaiaCalendar.dayOfMonth(day)});
        v.putLong("coins", Coins.count(p));
        v.putInt("cx", t.center.getX());
        v.putInt("cz", t.center.getZ());
        v.putInt("radius", t.radius());
        v.putInt("wall", t.level.wall());
        v.putInt("speed", Village.speed);
        v.putInt("grade", Math.max(1, t.level.villageGrade() == 0 ? 3 : t.level.villageGrade()));
        v.putInt("reach", Village.reach(t));
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
            if (b.type == BuildingType.GROUNDS) bt.putInt("gr", b.clearHeight);
            if (b.type == BuildingType.SIMPLE_FENCE) {
                bt.putIntArray("gates", b.gates);
                bt.putInt("fr", b.fenceRadius);
            }
            if (b.type == BuildingType.PATH && b.pathPoints != null) {
                bt.putIntArray("pts", b.pathPoints);
                bt.putInt("mat", b.pathSurface);
                bt.putInt("w", b.pathWidth);
                bt.putInt("blocks", Paths.blocks(b));
            }
            Resident s = Village.staffOf(t, b);
            if (s != null) bt.putString("staff", s.name);
            if (b.type == BuildingType.FARMLAND) bt.putDouble("growth", Village.growth(b, day));
            bs.add(bt);
        }
        v.put("buildings", bs);
        // The village log and yesterday's ledger.
        net.minecraft.nbt.ListTag lg = new net.minecraft.nbt.ListTag();
        for (Territory.LogEntry e : t.log) {
            CompoundTag c = new CompoundTag();
            c.putLong("d", e.day());
            c.putString("m", e.json());
            lg.add(c);
        }
        v.put("log", lg);
        v.putLongArray("ledger", t.ledger);
        v.putLong("foodDays", Village.foodDaysLeft(t));
        long other = 0;
        for (Goods g : Village.OTHER_FOODS) other += g.stock(t);
        v.putLong("otherFood", other);
        v.putLong("foodNeed", Village.foodUse(t));
        // Construction options for this player
        ListTag opts = new ListTag();
        for (BuildingType type : BuildingType.values()) {
            if (type.hidden() || type.secret() && !t.revealed.contains(type.id())) continue;
            CompoundTag o = new CompoundTag();
            o.putInt("type", type.ordinal());
            o.putInt("built", Village.countComplete(t, type));
            boolean free = t.freeBuilds.contains(type);
            o.putBoolean("free", free);
            String err = Village.checkCost(p, t, type, free);
            if (err != null) o.putString("err", err);
            o.putBoolean("blueprint", Village.knowsBlueprint(p, t, type));
            o.putBoolean("learned", t.blueprints.contains(type.id()));
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
            if (!r.isTalent() && !r.militia && !r.isFigure()) continue;
            CompoundTag rt = new CompoundTag();
            rt.putInt("id", r.id);
            rt.putBoolean("fig", r.isFigure());
            rt.putBoolean("mil", r.militia);
            rt.putInt("slv", r.soldierLevel);
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
        // Duties: the militia, the offices, following the Lord.
        v.putInt("militiaCap", Village.militiaCap(t));
        v.putInt("militiaCount", Village.militiaCount(t));
        v.putBoolean("wounded", r.woundedUntil > GaiaCalendar.today(p.server));
        int companions = 0;
        for (Resident o : t.residents) if (o.follows) companions++;
        v.putInt("companions", companions);
        ListTag offices = new ListTag();
        for (Office o : Office.values()) {
            CompoundTag ot = new CompoundTag();
            ot.putBoolean("open", o.open(t));
            Resident h = Office.holder(t, o);
            if (h != null) ot.putString("by", h.name);
            offices.add(ot);
        }
        v.put("offices", offices);
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

    /** Buildings with a window of their own: workshops, schools, the dojo, anything with a master. */
    public static boolean managed(Building b) {
        return b.complete && !b.demolishing && (b.type.staff() != null || !Workshops.modes(b.type).isEmpty() || Workshops.places(b.type) > 0);
    }

    /**
     * A building's window: its master, what it makes (and the choices), and its learners and who
     * else could learn there. With no building, the list of buildings that have windows.
     */
    public static CompoundTag building(ServerPlayer p, Territory t, Building b) {
        CompoundTag v = new CompoundTag();
        v.putInt("territory", t.id);
        if (b == null) {
            ListTag list = new ListTag();
            for (Building o : t.buildings) {
                if (!managed(o)) continue;
                CompoundTag c = new CompoundTag();
                c.putInt("id", o.id);
                c.putInt("type", o.type.ordinal());
                Resident m = Village.staffOf(t, o);
                if (m != null) c.putString("staff", m.name);
                c.putInt("learners", Workshops.learners(t, o).size());
                c.putInt("places", Workshops.places(o.type));
                Workshops.Mode mode = Workshops.mode(o);
                if (mode != null) c.putString("mode", mode.key());
                list.add(c);
            }
            v.put("list", list);
            return v;
        }
        v.putInt("id", b.id);
        v.putInt("type", b.type.ordinal());
        Resident m = Village.staffOf(t, b);
        if (m != null) {
            v.putString("staff", m.name);
            v.putInt("staffRank", m.rank);
        }
        if (b.type.staff() != null) v.putInt("staffOcc", b.type.staff().ordinal());
        ListTag modes = new ListTag();
        for (Workshops.Mode md : Workshops.modes(b.type)) {
            CompoundTag c = new CompoundTag();
            c.putString("key", md.key());
            if (md.input() != null) {
                c.putString("in", md.input().key());
                c.putDouble("inN", md.in());
            }
            if (md.output() != null) c.putString("out", md.output().key());
            c.putDouble("outN", md.out());
            c.putBoolean("items", md.makesItems());
            modes.add(c);
        }
        v.put("modes", modes);
        v.putInt("mode", b.mode);
        int places = Workshops.places(b.type);
        v.putInt("places", places);
        if (places > 0) {
            Occupation teaches = Workshops.teaches(b.type);
            v.putInt("teaches", teaches.ordinal());
            ListTag ls = new ListTag();
            for (Resident r : Workshops.learners(t, b)) {
                CompoundTag c = new CompoundTag();
                c.putInt("id", r.id);
                c.putString("name", r.name);
                c.putInt("half", r.trainingDays);
                c.putInt("need", Workshops.daysToLearn(b.type, r) * 2);
                c.putInt("occ", r.occupation.ordinal());
                ls.add(c);
            }
            v.put("learners", ls);
            ListTag cs = new ListTag();
            List<Resident> cands = new ArrayList<>();
            for (Resident r : t.residents) if (Workshops.canLearn(t, r) && !(r.occupation == teaches && r.rank >= 2)) cands.add(r);
            // Those already of the trade first (they rise a rank), then commoners, then the rest.
            cands.sort(java.util.Comparator.comparing((Resident r) -> r.occupation != teaches).thenComparing(r -> r.isTalent()).thenComparing(r -> r.name));
            for (Resident r : cands) {
                CompoundTag c = new CompoundTag();
                c.putInt("id", r.id);
                c.putString("name", r.name);
                c.putInt("occ", r.occupation.ordinal());
                c.putInt("rank", r.rank);
                c.putInt("job", r.job.ordinal());
                cs.add(c);
            }
            v.put("candidates", cs);
        }
        return v;
    }
}
