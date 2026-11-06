package com.realmlord.village;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.realmlord.Notify;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TokenGrade;
import com.realmlord.world.WorldState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The historical figures of the novel's summoning: generals, ministers, strategists, scholars,
 * physicians and craftsmen from China's past, each of a rank (King, Emperor, Divine, Saint).
 * A Summoning Token calls one of its own rank who has not yet been called anywhere in the world.
 */
public final class Figures {
    private Figures() {}

    public record Figure(String id, String name, String era, String kind, int rank, Occupation occupation,
                         Specialty specialty, int command, int force, int intelligence, int politics) {
        public String key() {
            return "realmlord.figure." + id;
        }

        /** King-rank figures come as Silver-grade talents; the higher ranks as Gold. */
        public int grade() {
            return rank == 0 ? TokenGrade.SILVER.ordinal() : TokenGrade.GOLD.ordinal();
        }
    }

    private static List<Figure> all;

    public static List<Figure> all() {
        if (all != null) return all;
        List<Figure> out = new ArrayList<>();
        try (var in = Figures.class.getResourceAsStream("/data/realmlord/figures.json")) {
            if (in != null) {
                JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    Occupation occ = Occupation.FARMER;
                    for (Occupation x : Occupation.values()) if (x.id.equals(o.get("occupation").getAsString())) occ = x;
                    Specialty spec = null;
                    for (Specialty x : Specialty.values()) if (x.id.equals(o.get("specialty").getAsString())) spec = x;
                    out.add(new Figure(o.get("id").getAsString(), o.get("name").getAsString(), o.get("era").getAsString(),
                            o.get("kind").getAsString(), o.get("rank").getAsInt(), occ, spec, o.get("command").getAsInt(),
                            o.get("force").getAsInt(), o.get("intelligence").getAsInt(), o.get("politics").getAsInt()));
                }
            }
        } catch (Exception ex) {
            com.realmlord.RealmLord.LOGGER.error("Could not read the historical figures", ex);
        }
        all = Collections.unmodifiableList(out);
        return all;
    }

    public static Figure byId(String id) {
        for (Figure f : all()) if (f.id().equals(id)) return f;
        return null;
    }

    /**
     * Picks a figure for a token of this rank: one of the same rank not yet summoned in the
     * world, or (if all are taken) the nearest rank below, then above. Null if none are left.
     */
    public static Figure pick(WorldState w, int rank, RandomSource r) {
        for (int d = 0; d < 4; d++) {
            for (int rk : new int[]{rank - d, rank + d}) {
                if (rk < 0 || rk > 3 || d > 0 && rk == rank) continue;
                List<Figure> free = new ArrayList<>();
                for (Figure f : all()) if (f.rank() == rk && !w.summoned.contains(f.id())) free.add(f);
                if (!free.isEmpty()) return free.get(r.nextInt(free.size()));
            }
        }
        return null;
    }

    /** The figure as a resident of a territory: their stats, trade and a specialty to suit. */
    public static Resident resident(Figure f, RandomSource r) {
        Resident res = new Resident();
        res.name = f.name();
        res.figure = f.id();
        res.grade = f.grade();
        res.occupation = f.occupation();
        res.rank = 2;
        res.loyalty = 85 + r.nextInt(11);
        res.command = f.command();
        res.force = f.force();
        res.intelligence = f.intelligence();
        res.politics = f.politics();
        res.specialty = f.specialty() != null ? f.specialty() : Specialty.forOccupation(f.occupation());
        res.skin = r.nextInt(64);
        return res;
    }

    /**
     * Uses a Summoning Token: the figure arrives in the Lord's village and is theirs from now on.
     * Returns the new resident, or null after telling the player why not.
     */
    public static Resident summon(ServerPlayer p, Territory t, int rank) {
        ServerLevel level = p.serverLevel();
        WorldState w = WorldState.get(p.server);
        Figure f = pick(w, rank, p.getRandom());
        if (f == null) {
            Notify.player(p, "realmlord.summoning.none_left");
            return null;
        }
        w.summoned.add(f.id());
        w.touch();
        Resident res = Village.add(t, resident(f, p.getRandom()));
        Village.tell(level, t, Component.translatable("realmlord.summoning.arrived", Component.translatable(f.key()),
                Component.translatable("realmlord.rank." + com.realmlord.item.SummoningTokenItem.RANKS[f.rank()]),
                Component.translatable(f.key() + ".desc"), t.name));
        if (f.rank() >= 2) {
            Notify.world(p.server, Component.translatable("realmlord.summoning.world", p.getDisplayName(),
                    Component.translatable("realmlord.rank." + com.realmlord.item.SummoningTokenItem.RANKS[f.rank()]), Component.translatable(f.key())));
        }
        com.realmlord.territory.TerritoryData.get(p.server).touch();
        return res;
    }
}
