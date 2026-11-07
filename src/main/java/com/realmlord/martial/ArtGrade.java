package com.realmlord.martial;

/**
 * The grades of secret manuals, as the novel's market knew them: a Bronze ("copper") manual
 * for 50 silver, Black Iron 2 gold, Silver 10 gold, Gold 50 gold. King rank and above are
 * never sold; Emperor rank were hard to find, God rank rare, and Saint rank only legend.
 */
public enum ArtGrade {
    BRONZE(0xCD7F32, 5_000, 1.0, 0),
    BLACK_IRON(0x7A7A88, 20_000, 1.5, 0),
    SILVER(0xC8C8D0, 100_000, 2.0, 0),
    GOLD(0xFFD700, 500_000, 3.0, 0),
    KING(0xB050FF, 0, 4.0, 10),
    EMPEROR(0xFF4040, 0, 6.0, 14),
    GOD(0xFF9020, 0, 8.0, 18),
    SAINT(0x30E0FF, 0, 10.0, 22);

    public final int color;
    /** The market's price in copper, or 0 if it is never sold. */
    public final long price;
    /** How much more practice each level takes than a Bronze art's. */
    public final double factor;
    /** The Comprehension needed to begin it. */
    public final int comprehension;

    ArtGrade(int color, long price, double factor, int comprehension) {
        this.color = color;
        this.price = price;
        this.factor = factor;
        this.comprehension = comprehension;
    }

    public String key() {
        return "realmlord.art.grade." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
