package com.realmlord.entity.level;

import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.beast.Sized;
import com.realmlord.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;

/**
 * Levels, as Gaia shows them over every creature's head: a wild boar might be level 3, the
 * Tiger King level 40. A creature's level comes from its kind and how far it roams from the
 * world's heart, and it sets its health, its strength and how big it has grown.
 */
public final class MobLevels {
    private MobLevels() {}

    public static final String KEY = "realmlord_lvl";
    private static final UUID HEALTH = UUID.fromString("6a1f6f2e-2c1d-4f6b-9a64-2b3c1d7e4f01");
    private static final UUID DAMAGE = UUID.fromString("6a1f6f2e-2c1d-4f6b-9a64-2b3c1d7e4f02");

    public record Range(int min, int max) {}

    private static Map<EntityType<?>, Range> ranges;

    public static Map<EntityType<?>, Range> ranges() {
        if (ranges == null) {
            ranges = Map.ofEntries(
                    Map.entry(ModEntities.WILD_BOAR.get(), new Range(1, 8)),
                    Map.entry(ModEntities.WILD_OX.get(), new Range(3, 10)),
                    Map.entry(ModEntities.GREY_WOLF.get(), new Range(3, 12)),
                    Map.entry(ModEntities.LEOPARD.get(), new Range(8, 16)),
                    Map.entry(ModEntities.BLACK_BEAR.get(), new Range(10, 18)),
                    Map.entry(ModEntities.SNOW_BEAR.get(), new Range(12, 22)),
                    Map.entry(ModEntities.MOUNTAIN_TIGER.get(), new Range(14, 26)),
                    Map.entry(ModEntities.WOLF_KING.get(), new Range(24, 32)),
                    Map.entry(ModEntities.BOAR_KING.get(), new Range(28, 36)),
                    Map.entry(ModEntities.TIGER_KING.get(), new Range(36, 45)),
                    Map.entry(ModEntities.BANDIT.get(), new Range(5, 15)));
        }
        return ranges;
    }

    public static boolean leveled(Mob m) {
        return ranges().containsKey(m.getType());
    }

    public static int level(Mob m) {
        return m.getPersistentData().getInt(KEY);
    }

    /** Gives a creature its level the first time it enters the world, and shows it again after every load. */
    public static void onJoin(ServerLevel level, Mob m) {
        Range r = ranges().get(m.getType());
        if (r == null) return;
        if (!m.getPersistentData().contains(KEY)) m.getPersistentData().putInt(KEY, roll(level, m, r, m.getRandom()));
        apply(m, r);
    }

    /** A level within the kind's range, a little higher the further from the world's spawn (up to +8). */
    public static int roll(ServerLevel level, Mob m, Range r, RandomSource rnd) {
        int lv = r.min() + rnd.nextInt(r.max() - r.min() + 1);
        if (m instanceof BanditEntity b && b.getRole() == BanditEntity.Role.CHIEF) lv += 8;
        BlockPos spawn = level.getSharedSpawnPos();
        double dist = Math.sqrt(m.blockPosition().distSqr(new BlockPos(spawn.getX(), m.getBlockY(), spawn.getZ())));
        lv += Math.min(8, (int) (dist / 1000));
        return Math.max(1, lv);
    }

    public static void set(Mob m, int lv) {
        m.getPersistentData().putInt(KEY, lv);
        Range r = ranges().get(m.getType());
        if (r != null) apply(m, r);
    }

    /** Health and strength scale with level; size grows across the kind's range; the level is shown above its head. */
    public static void apply(Mob m, Range r) {
        int lv = level(m);
        boolean full = m.getHealth() >= m.getMaxHealth() - 0.01F;
        modifier(m.getAttribute(Attributes.MAX_HEALTH), HEALTH, (lv - 1) * 0.06);
        modifier(m.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, (lv - 1) * 0.04);
        if (full) m.setHealth(m.getMaxHealth());
        if (m instanceof Sized s) {
            float rel = r.max() == r.min() ? 0.5F : Mth.clamp((lv - r.min()) / (float) (r.max() - r.min()), 0F, 1.3F);
            s.setSize(0.85F + 0.35F * rel);
        }
        m.setCustomName(Component.translatable("realmlord.mob.level", lv, baseName(m)));
        m.setCustomNameVisible(true);
    }

    private static Component baseName(Mob m) {
        if (m instanceof BanditEntity b) {
            return switch (b.getRole()) {
                case ARCHER -> Component.translatable("entity.realmlord.bandit_archer");
                case CHIEF -> Component.translatable("entity.realmlord.bandit_chief");
                default -> m.getType().getDescription();
            };
        }
        return m.getType().getDescription();
    }

    private static void modifier(AttributeInstance a, UUID id, double amount) {
        if (a == null) return;
        a.removeModifier(id);
        if (amount != 0) a.addPermanentModifier(new AttributeModifier(id, "Level", amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
