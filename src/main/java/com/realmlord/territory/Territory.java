package com.realmlord.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** One Lord's territory: where it is, who rules it, how big it has grown. */
public class Territory {
    public int id;
    public UUID owner;
    public String ownerName = "";
    public String name = "";
    public TokenGrade grade = TokenGrade.BRONZE;
    public TerritoryLevel level = TerritoryLevel.VILLAGE_1;
    public BlockPos center = BlockPos.ZERO;
    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public int population;
    /** 1 for the first village founded in the world, 2 for the second, and so on. */
    public int foundedOrder;
    public long foundedTick;
    /** The four basic resources of the novel, held in the territory's stores. */
    public long food, wood, stone, ironOre;
    /** Treasury, in copper (1 gold = 10,000 copper). */
    public long treasury;
    /** Goods beyond the four basic stores (salt, wine, linen...), by Goods ordinal, and part-made units. */
    public final long[] goods = new long[com.realmlord.village.Goods.values().length];
    public final double[] goodsCarry = new double[com.realmlord.village.Goods.values().length];
    /** The Lord's Manor storehouse: anything else the Lord keeps for later. */
    public final net.minecraft.world.SimpleContainer storage = new net.minecraft.world.SimpleContainer(STORAGE_PAGES * 54);
    /** Pages of a double chest's worth in the storehouse. */
    public static final int STORAGE_PAGES = 10;
    /** Building blueprints the village has learned (building ids): built as often as wanted. */
    public final java.util.Set<String> blueprints = new java.util.LinkedHashSet<>();
    /** People's Thoughts and Security (the novel's "Law and Order"), 0-100. */
    public int peopleThoughts = 70;
    public int security = 65;
    public final java.util.List<com.realmlord.village.Resident> residents = new java.util.ArrayList<>();
    public final java.util.List<com.realmlord.village.Building> buildings = new java.util.ArrayList<>();
    public int nextResidentId = 1;
    public int nextBuildingId = 1;
    /** The last novel day the territory was simulated. */
    public long lastDay = -1;
    public double immigrantCarry;
    public int hungryDays;
    /** Random buildings offered by the last upgrade, one of which the Lord may take for free. */
    public final java.util.List<com.realmlord.village.BuildingType> freeChoices = new java.util.ArrayList<>();
    /** Buildings granted free, waiting to be placed. */
    public final java.util.List<com.realmlord.village.BuildingType> freeBuilds = new java.util.ArrayList<>();
    /** Set once residents have been created for this territory. */
    public boolean settled;
    /** Settlers who would live here but have no bed yet; they move in as beds free up. */
    public int waitingSettlers;
    /** The manor standing now (see Residence: 1 the refugees' hall, 2 and 3 the Lord's residence), and the way its door faces (-1 unknown). */
    public int manorTier = 1;
    public int manorFront = -1;
    /** Hidden buildings this village has revealed. */
    public final java.util.Set<String> revealed = new java.util.HashSet<>();
    /** Players the Lord trusts to build and use chests in the territory (not the manor's). */
    public final java.util.Set<java.util.UUID> trusted = new java.util.HashSet<>();
    /** The village log, newest first: {novel day, message as JSON}. */
    public final java.util.List<LogEntry> log = new java.util.ArrayList<>();
    public static final int LOG_SIZE = 60;
    /** Yesterday's ledger (see Village.LEDGER_*). */
    public long[] ledger = new long[10];

    public record LogEntry(long day, String json) {}

    public void addLog(long day, net.minecraft.network.chat.Component msg) {
        log.add(0, new LogEntry(day, net.minecraft.network.chat.Component.Serializer.toJson(msg)));
        while (log.size() > LOG_SIZE) log.remove(log.size() - 1);
    }

    public boolean trusts(java.util.UUID player) {
        return player.equals(owner) || trusted.contains(player);
    }

    public com.realmlord.village.Resident resident(int id) {
        for (var r : residents) if (r.id == id) return r;
        return null;
    }

    public com.realmlord.village.Building building(int id) {
        for (var b : buildings) if (b.id == id) return b;
        return null;
    }

    public int radius() {
        return level.radius;
    }

    public boolean contains(ResourceKey<Level> dim, BlockPos pos) {
        if (!dim.equals(dimension)) return false;
        long dx = pos.getX() - center.getX();
        long dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (long) radius() * radius();
    }

    public double distanceTo(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        if (owner != null) t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
        t.putString("name", name);
        t.putInt("grade", grade.ordinal());
        t.putInt("level", level.ordinal());
        t.put("center", NbtUtils.writeBlockPos(center));
        t.putString("dim", dimension.location().toString());
        t.putInt("population", population);
        t.putInt("order", foundedOrder);
        t.putLong("founded", foundedTick);
        t.putLong("food", food);
        t.putLong("wood", wood);
        t.putLong("stone", stone);
        t.putLong("ironOre", ironOre);
        t.putLong("treasury", treasury);
        t.putInt("thoughts", peopleThoughts);
        t.putInt("security", security);
        net.minecraft.nbt.ListTag rs = new net.minecraft.nbt.ListTag();
        for (var r : residents) rs.add(r.save());
        t.put("residents", rs);
        net.minecraft.nbt.ListTag bs = new net.minecraft.nbt.ListTag();
        for (var b : buildings) bs.add(b.save());
        t.put("buildings", bs);
        t.putInt("nextResident", nextResidentId);
        t.putInt("nextBuilding", nextBuildingId);
        t.putLong("lastDay", lastDay);
        t.putDouble("immCarry", immigrantCarry);
        t.putInt("hungry", hungryDays);
        t.putInt("waiting", waitingSettlers);
        t.putInt("manorTier", manorTier);
        t.putInt("manorFront", manorFront);
        net.minecraft.nbt.ListTag tr = new net.minecraft.nbt.ListTag();
        for (var u : trusted) tr.add(net.minecraft.nbt.NbtUtils.createUUID(u));
        t.put("trusted", tr);
        net.minecraft.nbt.ListTag lg = new net.minecraft.nbt.ListTag();
        for (var e : log) {
            CompoundTag c = new CompoundTag();
            c.putLong("d", e.day());
            c.putString("m", e.json());
            lg.add(c);
        }
        t.put("log", lg);
        t.putLongArray("ledger", ledger);
        t.putString("revealed", String.join(",", revealed));
        t.putString("freeChoices", String.join(",", freeChoices.stream().map(com.realmlord.village.BuildingType::id).toList()));
        t.putString("freeBuilds", String.join(",", freeBuilds.stream().map(com.realmlord.village.BuildingType::id).toList()));
        t.putBoolean("settled", settled);
        CompoundTag g = new CompoundTag();
        for (var good : com.realmlord.village.Goods.values()) {
            if (goods[good.ordinal()] != 0) g.putLong(good.id, goods[good.ordinal()]);
            if (goodsCarry[good.ordinal()] != 0) g.putDouble(good.id + "_carry", goodsCarry[good.ordinal()]);
        }
        t.put("goods", g);
        t.put("storage", storage.createTag());
        t.putString("blueprints", String.join(",", blueprints));
        return t;
    }

    public static Territory load(CompoundTag t) {
        Territory r = new Territory();
        r.id = t.getInt("id");
        if (t.hasUUID("owner")) r.owner = t.getUUID("owner");
        r.ownerName = t.getString("ownerName");
        r.name = t.getString("name");
        r.grade = TokenGrade.byId(t.getInt("grade"));
        r.level = TerritoryLevel.byId(t.getInt("level"));
        r.center = NbtUtils.readBlockPos(t.getCompound("center"));
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
        r.dimension = dim == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dim);
        r.population = t.getInt("population");
        r.foundedOrder = t.getInt("order");
        r.foundedTick = t.getLong("founded");
        r.food = t.getLong("food");
        r.wood = t.getLong("wood");
        r.stone = t.getLong("stone");
        r.ironOre = t.getLong("ironOre");
        r.treasury = t.getLong("treasury");
        CompoundTag g = t.getCompound("goods");
        for (var good : com.realmlord.village.Goods.values()) {
            r.goods[good.ordinal()] = g.getLong(good.id);
            r.goodsCarry[good.ordinal()] = g.getDouble(good.id + "_carry");
        }
        r.storage.fromTag(t.getList("storage", net.minecraft.nbt.Tag.TAG_COMPOUND));
        for (String s : t.getString("blueprints").split(",")) if (!s.isBlank()) r.blueprints.add(s);
        r.peopleThoughts = t.contains("thoughts") ? t.getInt("thoughts") : 70;
        r.security = t.contains("security") ? t.getInt("security") : 65;
        net.minecraft.nbt.ListTag rs = t.getList("residents", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < rs.size(); i++) r.residents.add(com.realmlord.village.Resident.load(rs.getCompound(i)));
        net.minecraft.nbt.ListTag bs = t.getList("buildings", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            var b = com.realmlord.village.Building.load(bs.getCompound(i));
            if (b != null) r.buildings.add(b);
        }
        r.nextResidentId = Math.max(1, t.getInt("nextResident"));
        r.nextBuildingId = Math.max(1, t.getInt("nextBuilding"));
        r.lastDay = t.contains("lastDay") ? t.getLong("lastDay") : -1;
        r.immigrantCarry = t.getDouble("immCarry");
        r.hungryDays = t.getInt("hungry");
        r.waitingSettlers = t.getInt("waiting");
        r.manorTier = t.contains("manorTier") ? t.getInt("manorTier") : 1;
        r.manorFront = t.contains("manorFront") ? t.getInt("manorFront") : -1;
        for (var u : t.getList("trusted", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) r.trusted.add(net.minecraft.nbt.NbtUtils.loadUUID(u));
        for (var e : t.getList("log", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) e;
            r.log.add(new LogEntry(c.getLong("d"), c.getString("m")));
        }
        long[] lg = t.getLongArray("ledger");
        if (lg.length == r.ledger.length) r.ledger = lg;
        for (String s : t.getString("revealed").split(",")) if (!s.isEmpty()) r.revealed.add(s);
        for (String s : t.getString("freeChoices").split(",")) {
            var bt = com.realmlord.village.BuildingType.byId(s);
            if (bt != null) r.freeChoices.add(bt);
        }
        for (String s : t.getString("freeBuilds").split(",")) {
            var bt = com.realmlord.village.BuildingType.byId(s);
            if (bt != null) r.freeBuilds.add(bt);
        }
        r.settled = t.getBoolean("settled");
        return r;
    }
}
