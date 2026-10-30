package com.realmlord.profile;

import java.util.ArrayList;
import java.util.List;

/** Active skills of the combat occupations. Cooldowns are in seconds before Intelligence. */
public enum Skill {
    // Strategist
    FIRE_STRATAGEM(CombatClass.STRATEGIST, 1, 15),
    CONFUSION_ARRAY(CombatClass.STRATEGIST, 10, 25),
    BATTLE_INSIGHT(CombatClass.STRATEGIST, 25, 45),
    // General
    WAR_CRY(CombatClass.GENERAL, 1, 30),
    CAVALRY_CHARGE(CombatClass.GENERAL, 10, 12),
    UNYIELDING_WILL(CombatClass.GENERAL, 25, 60),
    // Chivalrous expert
    SWIFT_SWORD(CombatClass.CHIVALROUS_EXPERT, 1, 8),
    LIGHTNESS_SKILL(CombatClass.CHIVALROUS_EXPERT, 10, 30),
    HUNDRED_STRIKES(CombatClass.CHIVALROUS_EXPERT, 25, 20),
    // Alchemist
    TOXIC_FLASK(CombatClass.ALCHEMIST, 1, 12),
    VITALITY_ELIXIR(CombatClass.ALCHEMIST, 10, 40),
    THUNDER_PILL(CombatClass.ALCHEMIST, 25, 30),
    // Healer
    REJUVENATE(CombatClass.HEALER, 1, 15),
    ACUPUNCTURE_NEEDLE(CombatClass.HEALER, 10, 15),
    PURIFY(CombatClass.HEALER, 25, 45),
    // Bailiff
    IRON_SHACKLES(CombatClass.BAILIFF, 1, 15),
    ARREST(CombatClass.BAILIFF, 10, 12),
    PATROL_SENSE(CombatClass.BAILIFF, 25, 40);

    public final CombatClass owner;
    public final int unlockLevel;
    public final int cooldownSeconds;

    Skill(CombatClass owner, int unlockLevel, int cooldownSeconds) {
        this.owner = owner;
        this.unlockLevel = unlockLevel;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String key() {
        return "realmlord.skill." + id();
    }

    public String descKey() {
        return "realmlord.skill." + id() + ".desc";
    }

    public static List<Skill> of(CombatClass c) {
        List<Skill> out = new ArrayList<>();
        for (Skill s : values()) if (s.owner == c) out.add(s);
        return out;
    }

    public static Skill byId(int i) {
        Skill[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
