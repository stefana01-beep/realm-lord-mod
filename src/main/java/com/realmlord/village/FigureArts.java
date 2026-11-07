package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.martial.ArtData.Art;
import com.realmlord.martial.Arts;
import com.realmlord.territory.Territory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * What the summoned historical figures bring beyond their numbers. In the novel each figure is
 * more than a stat block: Zheng He builds a navy, Lu Ban the city's engines, Hua Tuo the medical
 * hall, Fan Li the chambers of commerce. So every figure in a village lends that village the gift
 * of their kind while they are loyal (Loyalty 60 or more):
 *
 * <ul>
 * <li>general: +5 militia places each, and the militia's drill goes faster;</li>
 * <li>admiral: +10 sailors and better fishing and sea trade (see {@link Navy});</li>
 * <li>strategist: raids and water bandits are seen coming: raiders lose a tenth of their strength each;</li>
 * <li>minister: +5% tax each;</li>
 * <li>scholar: students pass the imperial examination more often;</li>
 * <li>engineer and carpenter: building work goes 15% faster each, and siege engines shoot harder;</li>
 * <li>doctor: the wounded mend faster and People's Thoughts rise;</li>
 * <li>smith and weaver: the workshops make 5% more each;</li>
 * <li>agronomist: crops yield 10% more each;</li>
 * <li>merchant: caravans and merchant ships bring 20% more each.</li>
 * </ul>
 *
 * <p>And a figure whose Loyalty reaches 90 will, once, teach the Lord their own art: Sun Wu his
 * Art of War, Xiang Yu the Overlord's halberd, Guan Yu the Green Dragon blade, Hua Tuo the Five
 * Animals Play, Bai Qi the Qin army's Killing Sword, and so on.
 */
public final class FigureArts {
    private FigureArts() {}

    public static final int LOYAL = 60;
    public static final int TEACHES_AT = 90;

    /** Loyal figures of a kind in the village. */
    public static int count(Territory t, String kind) {
        int n = 0;
        for (Resident r : t.residents) {
            if (!r.isFigure() || r.loyalty < LOYAL) continue;
            Figures.Figure f = Figures.byId(r.figure);
            if (f != null && f.kind().equals(kind)) n++;
        }
        return n;
    }

    /** 1 + per x the loyal figures of a kind. */
    public static double boost(Territory t, String kind, double per) {
        return 1.0 + per * count(t, kind);
    }

    private static final Map<String, String> SIGNATURE = Map.ofEntries(
            Map.entry("sun_wu", "art_of_war"), Map.entry("sun_bin", "art_of_war"), Map.entry("han_xin", "art_of_war"),
            Map.entry("zhuge_liang", "art_of_war"), Map.entry("jiang_ziya", "art_of_war"), Map.entry("wu_qi", "art_of_war"),
            Map.entry("xiang_yu", "overlord_halberd"), Map.entry("lu_bu", "overlord_halberd"),
            Map.entry("guan_yu", "green_dragon_blade"),
            Map.entry("zhao_yun", "liuhe_spear"), Map.entry("ma_chao", "liuhe_spear"), Map.entry("zhang_fei", "liuhe_spear"),
            Map.entry("bai_qi", "killing_sword"), Map.entry("wang_jian", "killing_sword"), Map.entry("meng_tian", "killing_sword"),
            Map.entry("zhang_han", "killing_sword"),
            Map.entry("hua_tuo", "five_animals_play"), Map.entry("huang_zhong", "hundred_pace_archery"),
            Map.entry("taishi_ci", "hundred_pace_archery"), Map.entry("lu_ban", "ladder_cloud_step"));

    /** The art a figure would teach, or null. Generals teach the War Soldier Code, warriors Bajiquan, the learned Daoyin. */
    public static Art signature(Resident r) {
        if (!r.isFigure()) return null;
        String id = SIGNATURE.get(r.figure);
        if (id == null) {
            Figures.Figure f = Figures.byId(r.figure);
            if (f == null) return null;
            id = switch (f.kind()) {
                case "general", "admiral" -> "war_soldier_code";
                case "warrior" -> "bajiquan";
                case "strategist", "minister", "scholar" -> "daoyin";
                case "doctor" -> "five_animals_play";
                default -> "basic_breathing";
            };
        }
        return Arts.byId(id);
    }

    public static boolean canTeach(Resident r) {
        return r.isFigure() && !r.taught && r.loyalty >= TEACHES_AT && signature(r) != null;
    }

    /** The figure teaches the Lord their art: its manual goes to the player. Returns an error key or null. */
    public static String teach(ServerPlayer p, Territory t, Resident r) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.figure.err.not_lord";
        if (!r.isFigure()) return "realmlord.figure.err.not_figure";
        if (r.taught) return "realmlord.figure.err.taught";
        if (r.loyalty < TEACHES_AT) return "realmlord.figure.err.loyalty";
        Art a = signature(r);
        if (a == null) return "realmlord.figure.err.no_art";
        r.taught = true;
        net.minecraft.world.item.ItemStack book = Arts.manual(a);
        if (!p.getInventory().add(book)) p.drop(book, false);
        Notify.player(p, Component.translatable("realmlord.figure.taught", r.name, Component.translatable(a.key())));
        return null;
    }
}
