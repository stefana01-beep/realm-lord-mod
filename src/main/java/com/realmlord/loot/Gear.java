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
