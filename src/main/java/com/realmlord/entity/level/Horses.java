package com.realmlord.entity.level;

import com.realmlord.territory.TokenGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/**
 * Horses of four grades, as the novel's cavalry prized them: the sturdy Mongolian (Bronze), the
 * Hequ horse (Black Iron), the Dawan horse of Ferghana (Silver) and the legendary Bloodsweat
 * horse (Gold). The grade is shown over the horse's head and sets its speed, leap and health.
 * Foals take after their parents, and now and then surpass them.
 */
public final class Horses {
    private Horses() {}

    public static final String KEY = "realmlord_horse_grade";
    /** Out of 100: Bronze 62, Black Iron 26, Silver 9, Gold 3. */
    private static final int[] ODDS = {62, 26, 9, 3};
    private static final String[] BREEDS = {"mongolian", "hequ", "dawan", "bloodsweat"};
    /** Per grade: speed, jump strength and health (low, high). */
    private static final double[][] SPEED = {{0.17, 0.21}, {0.22, 0.25}, {0.26, 0.29}, {0.30, 0.34}};
    private static final double[][] JUMP = {{0.50, 0.65}, {0.65, 0.80}, {0.80, 0.95}, {0.95, 1.10}};
    private static final double[][] HEALTH = {{18, 22}, {22, 26}, {26, 30}, {30, 36}};

    public static boolean graded(AbstractHorse h) {
        return h.getType() == EntityType.HORSE;
    }

    public static int roll(RandomSource r) {
        int x = r.nextInt(100);
        for (int g = 0; g < ODDS.length; g++) {
            if (x < ODDS[g]) return g;
            x -= ODDS[g];
        }
        return 0;
    }

    public static TokenGrade grade(AbstractHorse h) {
        return h.getPersistentData().contains(KEY) ? TokenGrade.byId(h.getPersistentData().getInt(KEY)) : null;
    }

    /** First time in the world: a grade and the stats to go with it. After that only the name is shown again. */
    public static void onJoin(AbstractHorse h) {
        if (!graded(h)) return;
        RandomSource r = h.getRandom();
        if (!h.getPersistentData().contains(KEY)) {
            setGrade(h, roll(r), r);
        } else if (!h.getPersistentData().getBoolean("realmlord_horse_set")) {
            setGrade(h, h.getPersistentData().getInt(KEY), r);
        }
        name(h);
    }

    public static void setGrade(AbstractHorse h, int g, RandomSource r) {
        g = Mth.clamp(g, 0, 3);
        h.getPersistentData().putInt(KEY, g);
        h.getPersistentData().putBoolean("realmlord_horse_set", true);
        base(h, Attributes.MOVEMENT_SPEED, SPEED[g][0] + r.nextDouble() * (SPEED[g][1] - SPEED[g][0]));
        base(h, Attributes.JUMP_STRENGTH, JUMP[g][0] + r.nextDouble() * (JUMP[g][1] - JUMP[g][0]));
        base(h, Attributes.MAX_HEALTH, Math.round(HEALTH[g][0] + r.nextDouble() * (HEALTH[g][1] - HEALTH[g][0])));
        h.setHealth(h.getMaxHealth());
        name(h);
    }

    /** A foal's grade: as its better parent, often one lower, now and then one higher. */
    public static int foal(int a, int b, RandomSource r) {
        int best = Math.max(a, b);
        int x = r.nextInt(100);
        if (x < 15) return Math.min(3, best + 1);
        if (x < 55) return best;
        return Math.max(0, best - 1);
    }

    private static void name(AbstractHorse h) {
        TokenGrade g = grade(h);
        if (g == null) return;
        // A name the owner gave it with a name tag stays.
        if (h.hasCustomName() && !(h.getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("realmlord.horse.named"))) return;
        h.setCustomName(Component.translatable("realmlord.horse.named", Component.translatable(g.key()),
                Component.translatable("realmlord.horse.breed." + BREEDS[g.ordinal()])).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(g.color & 0xFFFFFF))));
        h.setCustomNameVisible(true);
        h.getPersistentData().putBoolean("realmlord_horse_named", true);
    }

    private static void base(AbstractHorse h, net.minecraft.world.entity.ai.attributes.Attribute a, double v) {
        var inst = h.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }
}
