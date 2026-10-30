package com.realmlord.profile;

import com.realmlord.Notify;
import com.realmlord.RealmConfig;
import com.realmlord.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/** Server-side helpers that change a profile and tell the player about it. */
public final class Profiles {
    private Profiles() {}

    private static final UUID HEALTH_ID = UUID.fromString("6f1b6c1e-3a6d-4a8e-9b1e-1f6a2b7d0a01");
    private static final UUID DAMAGE_ID = UUID.fromString("6f1b6c1e-3a6d-4a8e-9b1e-1f6a2b7d0a02");
    private static final UUID REACH_ID = UUID.fromString("6f1b6c1e-3a6d-4a8e-9b1e-1f6a2b7d0a03");
    private static final UUID SWIM_ID = UUID.fromString("6f1b6c1e-3a6d-4a8e-9b1e-1f6a2b7d0a04");

    public static Profile get(ServerPlayer p) {
        return ProfileCapability.get(p).orElseThrow(() -> new IllegalStateException("Player has no Realm Lord profile"));
    }

    /** Character experience from fighting; Adventurers learn a quarter faster. */
    public static void grantXp(ServerPlayer p, long amount) {
        Profile prof = get(p);
        if (!prof.hasChosen() || !prof.hasCharacterLevel()) return;
        double mult = RealmConfig.xpMultiplier() * (prof.getMode() == PlayMode.ADVENTURER ? 1.25 : 1.0);
        int before = prof.getLevel();
        int gained = prof.addXp(Math.round(amount * mult));
        if (gained > 0) {
            Notify.player(p, "realmlord.msg.level_up", prof.getLevel(), prof.getPoints());
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            for (Skill s : prof.unlockedSkills()) {
                if (s.unlockLevel > before && s.unlockLevel <= prof.getLevel()) {
                    Notify.player(p, Component.translatable("realmlord.msg.skill_unlocked", Component.translatable(s.key())));
                }
            }
            applyAttributes(p);
        }
    }

    public static void grantWorkXp(ServerPlayer p, long amount) {
        Profile prof = get(p);
        if (prof.getWork() == null) return;
        int gained = prof.addWorkXp(Math.round(amount * RealmConfig.xpMultiplier()));
        if (gained > 0) {
            Notify.player(p, Component.translatable("realmlord.msg.work_up",
                    Component.translatable(prof.getWork().key()), prof.getWorkLevel(),
                    Component.translatable(WorkOccupation.tierKey(prof.getWorkLevel())), prof.getPoints()));
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.3F);
            applyAttributes(p);
        }
    }

    /** Merit raises a Lord's title automatically when the threshold is reached. */
    public static void grantMerit(ServerPlayer p, long amount, Component reason) {
        Profile prof = get(p);
        Title before = prof.getTitle();
        boolean promoted = prof.addMerit(amount);
        if (reason != null) Notify.player(p, Component.translatable("realmlord.msg.merit", amount, reason));
        if (promoted) {
            Title now = prof.getTitle();
            Notify.player(p, Component.translatable("realmlord.msg.title", Component.translatable(now.key())));
            p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1.0F);
            if (now.atLeast(Title.VISCOUNT_3) && !before.atLeast(now)) {
                Notify.world(p.server, Component.translatable("realmlord.msg.title_world", p.getDisplayName(), Component.translatable(now.key())));
            }
            refreshName(p);
        }
    }

    public static void refreshName(ServerPlayer p) {
        p.refreshDisplayName();
        p.refreshTabListName();
    }

    /** Force strengthens the body; some occupations change reach or swimming. */
    public static void applyAttributes(ServerPlayer p) {
        Profile prof = get(p);
        int force = prof.hasChosen() ? prof.stat(Stat.FORCE) : 0;
        double health = prof.hasChosen() ? Math.min(40.0, Math.max(0, force - 5) * 0.5 + (prof.getLevel() - 1) * 0.2) : 0.0;
        double damage = prof.hasChosen() ? force * 0.05 : 0.0;
        set(p, Attributes.MAX_HEALTH, HEALTH_ID, "Realm Lord health", health, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Realm Lord damage", damage, AttributeModifier.Operation.ADDITION);
        int tier = prof.getWork() == null ? -1 : WorkOccupation.tier(prof.getWorkLevel());
        double reach = prof.getWork() == WorkOccupation.ARCHITECT ? 0.5 * (tier + 1) : 0.0;
        double swim = prof.getWork() == WorkOccupation.SHIPBUILDER ? 0.1 * (tier + 1) : 0.0;
        set(p, ForgeMod.BLOCK_REACH.get(), REACH_ID, "Realm Lord reach", reach, AttributeModifier.Operation.ADDITION);
        set(p, ForgeMod.SWIM_SPEED.get(), SWIM_ID, "Realm Lord swimming", swim, AttributeModifier.Operation.MULTIPLY_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(ServerPlayer p, Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.sync(p);
    }
}
