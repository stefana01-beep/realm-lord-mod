package com.realmlord.item;

import com.realmlord.martial.ArtData.Art;
import com.realmlord.martial.ArtGrade;
import com.realmlord.martial.Arts;
import com.realmlord.profile.Innate;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A martial arts secret manual, as in the novel: a cultivation method or a weapon art of a grade
 * from Bronze to Saint. Right-click to learn it; then it must be trained. Higher grades need
 * the Comprehension to grasp them, and some need another art trained first.
 */
public class SecretManualItem extends Item {
    private static final String[] INNATE = {"body", "comprehension", "luck", "charm"};
    private static final String[] STATS = {"command", "force", "intelligence", "politics"};

    public SecretManualItem(Properties p) {
        super(p);
    }

    /** Teaches an art; returns null, or why not (a ready message). */
    public static Component learn(ServerPlayer sp, Art a) {
        Profile prof = Profiles.get(sp);
        if (prof.knowsArt(a.id())) return Component.translatable("realmlord.art.known", Component.translatable(a.key()));
        int comp = prof.innate(Innate.COMPREHENSION);
        if (comp < a.grade().comprehension)
            return Component.translatable("realmlord.art.need_comprehension", comp, Component.translatable(a.grade().key()), a.grade().comprehension);
        if (!a.requires().isEmpty() && prof.artLevel(a.requires()) < a.requiresLevel())
            return Component.translatable("realmlord.art.need_art", Component.translatable("realmlord.art." + a.requires()), a.requiresLevel());
        prof.learnArt(a.id());
        Profiles.applyAttributes(sp);
        Profiles.sync(sp);
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Art a = Arts.of(stack);
        if (a == null) return InteractionResultHolder.fail(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        Component err = learn(sp, a);
        if (err != null) {
            sp.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        sp.displayClientMessage(Component.translatable("realmlord.art.learned", Component.translatable(a.key())).withStyle(ChatFormatting.GOLD), false);
        sp.playNotifySound(net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Art a = Arts.of(stack);
        if (a == null) return super.getName(stack);
        return Component.translatable("item.realmlord.secret_manual.named", Component.translatable(a.key()))
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(a.grade().color)));
    }

    /** "Body +2, Force +1" for an art's per-level gains. */
    public static MutableComponent perLevel(Art a) {
        MutableComponent c = Component.empty();
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            if (a.innate()[i] > 0) {
                if (any) c.append(", ");
                c.append(Component.translatable("realmlord.art.stat." + INNATE[i], a.innate()[i]));
                any = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (a.stats()[i] > 0) {
                if (any) c.append(", ");
                c.append(Component.translatable("realmlord.art.stat." + STATS[i], a.stats()[i]));
                any = true;
            }
        }
        if (a.dmg() > 0) {
            if (any) c.append(", ");
            c.append(Component.translatable("realmlord.art.tooltip.damage", Math.round(a.dmg() * 100)));
            any = true;
        }
        if (a.speed() > 0) {
            if (any) c.append(", ");
            c.append(Component.translatable("realmlord.art.tooltip.speed", String.format("%.1f", a.speed() * 100)));
        }
        return c;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Art a = Arts.of(stack);
        if (a == null) return;
        MutableComponent kinds = Component.empty();
        int i = 0;
        for (var k : a.kinds()) {
            if (i++ > 0) kinds.append(" / ");
            kinds.append(Component.translatable("realmlord.art.kind." + k.name().toLowerCase(java.util.Locale.ROOT)));
        }
        tooltip.add(Component.translatable("realmlord.art.tooltip.grade", Component.translatable(a.grade().key()), kinds)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(a.grade().color))));
        tooltip.add(Component.translatable("realmlord.art.tooltip.levels", a.maxLevel()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("realmlord.art.tooltip.per_level", perLevel(a)).withStyle(ChatFormatting.AQUA));
        if (!a.special().isEmpty())
            tooltip.add(Component.translatable("realmlord.art.tooltip.special", a.specialLevel(), Component.translatable(a.key() + ".special")).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(a.key() + ".desc").withStyle(ChatFormatting.GRAY));
        if (a.grade().comprehension > 0) tooltip.add(Component.translatable("realmlord.art.tooltip.comprehension", a.grade().comprehension).withStyle(ChatFormatting.RED));
        if (!a.requires().isEmpty())
            tooltip.add(Component.translatable("realmlord.art.tooltip.requires", Component.translatable("realmlord.art." + a.requires()), a.requiresLevel()).withStyle(ChatFormatting.RED));
        if (com.realmlord.network.ClientProfileData.arts.contains(a.id())) tooltip.add(Component.translatable("realmlord.art.tooltip.known").withStyle(ChatFormatting.GREEN));
        else tooltip.add(Component.translatable("realmlord.art.tooltip.use").withStyle(ChatFormatting.GOLD));
    }
}
