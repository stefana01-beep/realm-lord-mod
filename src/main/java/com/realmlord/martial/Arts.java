package com.realmlord.martial;

import com.realmlord.martial.ArtData.Art;
import com.realmlord.martial.ArtData.Kind;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;

/** Looking up martial arts, their manuals, and the weapons they train. */
public final class Arts {
    private Arts() {}

    public static final String TAG = "realmlord_art";

    public static Art byId(String id) {
        for (Art a : ArtData.ALL) if (a.id().equals(id)) return a;
        return null;
    }

    /** Practice needed to go from a level to the next. */
    public static int need(Art a, int level) {
        return (int) Math.round(100 * a.grade().factor * level);
    }

    public static ItemStack manual(Art a) {
        ItemStack s = new ItemStack(com.realmlord.registry.ModCrafts.SECRET_MANUAL.get());
        s.getOrCreateTag().putString(TAG, a.id());
        return s;
    }

    public static Art of(ItemStack s) {
        return s.hasTag() && s.getTag().contains(TAG) ? byId(s.getTag().getString(TAG)) : null;
    }

    /** The arts of a grade range, for drops and the market. */
    public static List<Art> between(ArtGrade lo, ArtGrade hi) {
        List<Art> out = new ArrayList<>();
        for (Art a : ArtData.ALL) if (a.grade().ordinal() >= lo.ordinal() && a.grade().ordinal() <= hi.ordinal()) out.add(a);
        return out;
    }

    /** A manual of up to the given grade, lower grades far likelier (each grade a third as likely as the one below). */
    public static ItemStack random(RandomSource r, ArtGrade max) {
        ArtGrade g = ArtGrade.BRONZE;
        while (g.ordinal() < max.ordinal() && r.nextInt(3) == 0) g = ArtGrade.values()[g.ordinal() + 1];
        List<Art> pool = between(g, g);
        while (pool.isEmpty() && g.ordinal() > 0) {
            g = ArtGrade.values()[g.ordinal() - 1];
            pool = between(g, g);
        }
        return manual(pool.get(r.nextInt(pool.size())));
    }

    /** What an item in hand is for the martial arts: fist (empty hand), blade, spear, bow, or nothing. */
    public static Kind weaponKind(ItemStack s) {
        if (s.isEmpty()) return Kind.FIST;
        Item i = s.getItem();
        if (i instanceof BowItem || i instanceof CrossbowItem) return Kind.BOW;
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(i);
        String path = key == null ? "" : key.getPath();
        if (i instanceof TridentItem || path.contains("spear") || path.contains("guandao") || path.equals("ji") || path.contains("halberd")) return Kind.SPEAR;
        if (i instanceof SwordItem || path.contains("sabre") || path.contains("dao") || path.contains("jian")) return Kind.BLADE;
        if (i instanceof AxeItem) return null;
        return null;
    }
}
