package com.realmlord.work;

import com.realmlord.profile.Profile;
import com.realmlord.profile.ProfileCapability;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.work.TechData.Tech;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Technical manuals, as in the novel: "After use, gain knowledge of crafting ...". Each belongs
 * to a trade. Junior manuals that only teach recipes can be learned by anyone (any player can
 * craft with the right knowledge); the rest need that trade at the manual's rank. Members of
 * the trade also make more of what the manuals teach.
 */
public final class TechManuals {
    private TechManuals() {}

    public static final String TAG = "realmlord_tech";

    /** What this client's player knows, synced from the server (for the recipe book's preview). */
    public static final java.util.Set<String> CLIENT_KNOWN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static Tech byId(String id) {
        for (Tech t : TechData.ALL) if (t.id().equals(id)) return t;
        return null;
    }

    public static ItemStack manual(Tech t) {
        ItemStack s = new ItemStack(com.realmlord.registry.ModCrafts.TECH_MANUAL.get());
        s.getOrCreateTag().putString(TAG, t.id());
        return s;
    }

    public static Tech of(ItemStack s) {
        return s.hasTag() && s.getTag().contains(TAG) ? byId(s.getTag().getString(TAG)) : null;
    }

    public static List<Tech> ofTrade(WorkOccupation w) {
        List<Tech> out = new ArrayList<>();
        for (Tech t : TechData.ALL) if (t.trade() == w) out.add(t);
        return out;
    }

    /** True if anyone may learn it: a Junior manual that only teaches things to make. */
    public static boolean open(Tech t) {
        return t.tier() == 0 && !t.passive();
    }

    /** Why the player can't learn a manual (a message key), or null if they can. */
    public static String cannotLearn(Profile prof, Tech t) {
        if (prof.knowsTech(t.id())) return "realmlord.tech.known";
        if (open(t)) return null;
        if (prof.getWork() != t.trade() || WorkOccupation.tier(prof.getWorkLevel()) < t.tier()) return "realmlord.tech.need_trade";
        return null;
    }

    public static boolean knows(Player p, String id) {
        Profile prof = ProfileCapability.get(p).orElse(null);
        return prof != null && prof.knowsTech(id);
    }

    /** The player's rank (0-3) in a trade if it is theirs, else -1. */
    public static int tradeTier(Player p, WorkOccupation w) {
        Profile prof = ProfileCapability.get(p).orElse(null);
        if (prof == null || prof.getWork() != w) return -1;
        return WorkOccupation.tier(prof.getWorkLevel());
    }

    /** The manual that teaches an item, if the player knows one that does. */
    public static Tech teaching(Player p, String itemId) {
        Profile prof = ProfileCapability.get(p).orElse(null);
        if (prof == null) return null;
        for (Tech t : TechData.ALL) {
            if (!prof.knowsTech(t.id())) continue;
            for (String o : t.outputs()) if (o.equals(itemId)) return t;
        }
        return null;
    }

    /** A manual of up to the given rank, the lower ranks likelier. */
    public static ItemStack random(RandomSource r, int maxTier) {
        int tier = 0;
        while (tier < maxTier && r.nextInt(3) == 0) tier++;
        List<Tech> pool = new ArrayList<>();
        for (Tech t : TechData.ALL) if (t.tier() == tier) pool.add(t);
        return manual(pool.get(r.nextInt(pool.size())));
    }
}
