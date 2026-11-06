package com.realmlord.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-wide state that belongs to no one territory: which historical figures have been
 * summoned (each exists once in the world, as in the novel), the beastwave, and the beast dens.
 */
public class WorldState extends SavedData {
    private static final String NAME = "realmlord_world";

    public final Set<String> summoned = new HashSet<>();
    /** The Gaia year of the last beastwave started, and the one warned of. */
    public int lastWaveYear;
    public int warnedYear;
    /** The beastwave running now (0 if none): its novel day, and each defender's kills. */
    public long waveDay = -1;
    public final Map<UUID, Integer> waveKills = new HashMap<>();
    public final Map<UUID, String> waveNames = new HashMap<>();
    public final List<Den> dens = new ArrayList<>();

    /** A beast den: where it is, whether its king is alive, and when it comes back. */
    public static class Den {
        public BlockPos pos;
        public boolean cleared;
        public long respawnDay;
        public final Set<UUID> discoveredBy = new HashSet<>();
        public UUID king;
        /** Whose lair: 0 a Boar King's, 1 a Wolf King's, 2 the White Tiger King's. */
        public int kind;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.put("pos", NbtUtils.writeBlockPos(pos));
            t.putBoolean("cleared", cleared);
            t.putLong("respawn", respawnDay);
            ListTag d = new ListTag();
            for (UUID u : discoveredBy) d.add(NbtUtils.createUUID(u));
            t.put("found", d);
            if (king != null) t.putUUID("king", king);
            t.putInt("kind", kind);
            return t;
        }

        static Den load(CompoundTag t) {
            Den d = new Den();
            d.pos = NbtUtils.readBlockPos(t.getCompound("pos"));
            d.cleared = t.getBoolean("cleared");
            d.respawnDay = t.getLong("respawn");
            for (Tag u : t.getList("found", Tag.TAG_INT_ARRAY)) d.discoveredBy.add(NbtUtils.loadUUID(u));
            d.king = t.hasUUID("king") ? t.getUUID("king") : null;
            d.kind = t.getInt("kind");
            return d;
        }
    }

    public static WorldState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorldState::load, WorldState::new, NAME);
    }

    public void touch() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag s = new ListTag();
        for (String id : summoned) s.add(StringTag.valueOf(id));
        tag.put("summoned", s);
        tag.putInt("lastWave", lastWaveYear);
        tag.putInt("warned", warnedYear);
        tag.putLong("waveDay", waveDay);
        ListTag k = new ListTag();
        for (var e : waveKills.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("p", e.getKey());
            c.putInt("n", e.getValue());
            c.putString("name", waveNames.getOrDefault(e.getKey(), ""));
            k.add(c);
        }
        tag.put("kills", k);
        ListTag d = new ListTag();
        for (Den den : dens) d.add(den.save());
        tag.put("dens", d);
        return tag;
    }

    public static WorldState load(CompoundTag tag) {
        WorldState w = new WorldState();
        for (Tag t : tag.getList("summoned", Tag.TAG_STRING)) w.summoned.add(t.getAsString());
        w.lastWaveYear = tag.getInt("lastWave");
        w.warnedYear = tag.getInt("warned");
        w.waveDay = tag.contains("waveDay") ? tag.getLong("waveDay") : -1;
        for (Tag t : tag.getList("kills", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            w.waveKills.put(c.getUUID("p"), c.getInt("n"));
            w.waveNames.put(c.getUUID("p"), c.getString("name"));
        }
        for (Tag t : tag.getList("dens", Tag.TAG_COMPOUND)) w.dens.add(Den.load((CompoundTag) t));
        return w;
    }
}
