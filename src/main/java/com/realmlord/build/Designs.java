package com.realmlord.build;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realmlord.RealmLord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where designs live: the ready-made ones the structure manuals teach (shipped with the mod),
 * and the ones architects draw themselves (kept with the world; a plan item names one by id).
 */
public final class Designs extends SavedData {
    private static final Map<String, Design> BUILTIN = new HashMap<>();
    private final Map<UUID, Design> drawn = new HashMap<>();

    /** A ready-made design by id, or null. */
    public static synchronized Design builtin(String id) {
        if (BUILTIN.containsKey(id)) return BUILTIN.get(id);
        Design d = null;
        try (var in = Designs.class.getResourceAsStream("/data/" + RealmLord.MODID + "/designs/" + id + ".json")) {
            if (in != null) {
                JsonObject j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                d = Design.fromJson(j);
            }
        } catch (Exception e) {
            RealmLord.LOGGER.warn("Could not read design {}", id, e);
        }
        BUILTIN.put(id, d);
        return d;
    }

    public static Designs get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Designs::load, Designs::new, RealmLord.MODID + "_designs");
    }

    public Design drawn(UUID id) {
        return drawn.get(id);
    }

    public UUID add(Design d) {
        UUID id = UUID.randomUUID();
        drawn.put(id, d);
        setDirty();
        return id;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        CompoundTag all = new CompoundTag();
        for (var e : drawn.entrySet()) all.put(e.getKey().toString(), e.getValue().save());
        t.put("designs", all);
        return t;
    }

    static Designs load(CompoundTag t) {
        Designs d = new Designs();
        CompoundTag all = t.getCompound("designs");
        for (String k : all.getAllKeys()) {
            try {
                d.drawn.put(UUID.fromString(k), Design.load(all.getCompound(k)));
            } catch (Exception ignored) {
            }
        }
        return d;
    }
}
