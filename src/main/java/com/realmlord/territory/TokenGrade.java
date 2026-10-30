package com.realmlord.territory;

/**
 * Village Creation Token grades. A territory keeps its grade as it grows, and an affiliated
 * village founded from it is one grade lower.
 */
public enum TokenGrade {
    BRONZE("bronze", 100, 0xFFC08040),
    BLACK_IRON("black_iron", 200, 0xFF7A7F8A),
    SILVER("silver", 300, 0xFFDDE3EC),
    GOLD("gold", 400, 0xFFF2C744);

    public final String id;
    /** Merit awarded for completing the Village Creation quest at this grade. */
    public final int questMerit;
    public final int color;

    TokenGrade(String id, int questMerit, int color) {
        this.id = id;
        this.questMerit = questMerit;
        this.color = color;
    }

    public String key() {
        return "realmlord.grade." + id;
    }

    public TokenGrade lower() {
        return this == BRONZE ? BRONZE : values()[ordinal() - 1];
    }

    public static TokenGrade byId(int i) {
        TokenGrade[] v = values();
        return i >= 0 && i < v.length ? v[i] : BRONZE;
    }
}
