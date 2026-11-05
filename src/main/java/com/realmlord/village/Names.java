package com.realmlord.village;

import net.minecraft.util.RandomSource;

/** Chinese names for residents. */
public final class Names {
    private Names() {}

    private static final String[] SURNAMES = {"Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Zhao", "Huang", "Zhou", "Wu",
            "Xu", "Sun", "Hu", "Zhu", "Gao", "Lin", "He", "Guo", "Ma", "Luo", "Song", "Tang", "Han", "Cao", "Feng", "Deng",
            "Xie", "Shen", "Lu", "Jiang", "Cui", "Gu", "Pan", "Du", "Dai", "Xia", "Zhong", "Tian", "Ren", "Fang"};
    private static final String[] GIVEN = {"Ming", "Hua", "Jun", "Fang", "Lan", "Qiang", "Mei", "Tao", "Wei", "Ying", "Hong",
            "Bo", "Xiu", "Ping", "Lei", "Yun", "Shan", "Rong", "Gang", "Li", "An", "Chun", "De", "Feng", "Hui", "Jie", "Kang",
            "Lu", "Qing", "Song", "Wen", "Xin", "Yan", "Zhi", "Yi", "Bao", "Cai", "Fu", "Guang", "Hai", "Jin", "Ning", "Shu"};

    public static String random(RandomSource r) {
        String given = GIVEN[r.nextInt(GIVEN.length)];
        if (r.nextBoolean()) given += GIVEN[r.nextInt(GIVEN.length)].toLowerCase(java.util.Locale.ROOT);
        return SURNAMES[r.nextInt(SURNAMES.length)] + " " + given;
    }
}
