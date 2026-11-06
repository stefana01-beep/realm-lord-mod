package com.realmlord.loot;

import com.google.common.collect.Multimap;
import com.realmlord.territory.TokenGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Graded equipment, as in the novel: ordinary weapons and armour that drop with a grade
 * (Bronze, Black Iron, Silver, Gold) and bonus attributes to match.
 */
public final class Gear {
    private Gear() {}

    public static final String TAG = "realmlord_grade";
    public static final String EFFECT = "realmlord_effect";

    /**
     * What graded gear can carry beyond its numbers: weapons that drink blood, burn, chill or
     * rend beasts; armour that lightens the step, steadies, or toughens.
     */
    public enum Effect {
        LIFESTEAL(true), FLAME(true), FROST(true), BEASTBANE(true),
        SWIFT(false), STALWART(false), VIGOR(false);

        public final boolean weapon;

        Effect(boolean weapon) {
            this.weapon = weapon;
        }

        public String key() {
            return "realmlord.gear.effect." + name().toLowerCase();
        }
    }

    /** The effect a graded item carries, or null. */
    public static Effect effectOf(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(EFFECT)) return null;
        int i = stack.getTag().getInt(EFFECT);
        return i >= 0 && i < Effect.values().length ? Effect.values()[i] : null;
    }

    public static ItemStack make(Item item, TokenGrade grade, RandomSource random) {
        ItemStack stack = new ItemStack(item);
        EquipmentSlot slot = item instanceof ArmorItem a ? a.getEquipmentSlot() : EquipmentSlot.MAINHAND;
        // Adding any modifier replaces the item's defaults, so copy those first.
        List<Map.Entry<Attribute, AttributeModifier>> defaults = new ArrayList<>();
        Multimap<Attribute, AttributeModifier> base = stack.getAttributeModifiers(slot);
        defaults.addAll(base.entries());
        for (var e : defaults) stack.addAttributeModifier(e.getKey(), e.getValue(), slot);

        int g = grade.ordinal() + 1;
        if (slot == EquipmentSlot.MAINHAND) {
            stack.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Mth.createInsecureUUID(random), "Grade bonus", 0.75 * g, AttributeModifier.Operation.ADDITION), slot);
        } else {
            stack.addAttributeModifier(Attributes.ARMOR,
                    new AttributeModifier(Mth.createInsecureUUID(random), "Grade bonus", 0.5 * g, AttributeModifier.Operation.ADDITION), slot);
            stack.addAttributeModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(Mth.createInsecureUUID(random), "Grade bonus", g, AttributeModifier.Operation.ADDITION), slot);
        }
        // Silver gear carries an effect half the time, Gold always.
        if (grade == TokenGrade.GOLD || grade == TokenGrade.SILVER && random.nextBoolean()) {
            List<Effect> fit = new ArrayList<>();
            for (Effect e : Effect.values()) if (e.weapon == (slot == EquipmentSlot.MAINHAND)) fit.add(e);
            Effect e = fit.get(random.nextInt(fit.size()));
            stack.getOrCreateTag().putInt(EFFECT, e.ordinal());
            switch (e) {
                case SWIFT -> stack.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(Mth.createInsecureUUID(random), "Swift", 0.03 * g, AttributeModifier.Operation.MULTIPLY_BASE), slot);
                case STALWART -> {
                    stack.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,
                            new AttributeModifier(Mth.createInsecureUUID(random), "Stalwart", 0.1, AttributeModifier.Operation.ADDITION), slot);
                    stack.addAttributeModifier(Attributes.ARMOR_TOUGHNESS,
                            new AttributeModifier(Mth.createInsecureUUID(random), "Stalwart", 1.0, AttributeModifier.Operation.ADDITION), slot);
                }
                case VIGOR -> stack.addAttributeModifier(Attributes.MAX_HEALTH,
                        new AttributeModifier(Mth.createInsecureUUID(random), "Vigor", g, AttributeModifier.Operation.ADDITION), slot);
                default -> { }
            }
        }
        stack.getOrCreateTag().putInt(TAG, grade.ordinal());
        stack.setHoverName(Component.translatable("realmlord.gear.named", Component.translatable(grade.key()), item.getDescription())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(grade.color & 0xFFFFFF)).withItalic(false)));
        return stack;
    }

    /** The grade of a graded item, or null. */
    public static TokenGrade gradeOf(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG) ? TokenGrade.byId(stack.getTag().getInt(TAG)) : null;
    }

    public static Component tooltip(TokenGrade grade) {
        return Component.translatable("realmlord.gear.grade", Component.translatable(grade.key()))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(grade.color & 0xFFFFFF)));
    }
}
