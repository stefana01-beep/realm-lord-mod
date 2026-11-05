package com.realmlord.village;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.UUID;

/**
 * One person living in a territory, with a character card like the novel's NPCs: grade,
 * occupation, craft rank, loyalty, Command, Force, Intelligence, Politics, an optional
 * specialty, and the job the Lord gave them.
 */
public class Resident {
    /** Commoners have no grade; talents are Bronze, Black Iron, Silver or Gold. */
    public static final int COMMON = -1;

    public int id;
    public String name = "";
    public int grade = COMMON;
    public Occupation occupation = Occupation.FARMER;
    /** 0 Junior, 1 Intermediate, 2 Advanced. */
    public int rank;
    public int loyalty = 70;
    public int command, force, intelligence, politics;
    public Specialty specialty;
    public Job job = Job.IDLE;
    /** Building id for STAFF and field jobs (-1 if none). */
    public int workplace = -1;
    public int skin;
    /** The spawned entity, if this resident is currently shown in the world. */
    public UUID entity;
    public boolean militia;
    /** What they carry: tools, and their haul until they hand it in. */
    public final net.minecraft.world.SimpleContainer inv = new net.minecraft.world.SimpleContainer(9);
    /** Wood, stone and iron ore handed in today, which counts towards their day's work. */
    public final long[] gathered = new long[3];
    /** What they are doing right now (a text key), for their card. */
    public String activity = "";

    public boolean isTalent() {
        return grade != COMMON || occupation.special;
    }

    /** Output multiplier for a job: the right trade, a better rank and a matching specialty all help. */
    public double skillAt(Job j) {
        double m = 1.0;
        boolean trade = switch (j) {
            case LOGGING -> occupation == Occupation.CARPENTER || occupation == Occupation.HUNTER;
            case QUARRYING -> occupation == Occupation.MASON;
            case MINING -> occupation == Occupation.MINER;
            case FARMING -> occupation == Occupation.FARMER || occupation == Occupation.FRUIT_FARMER;
            case CONSTRUCTION -> occupation == Occupation.CARPENTER || occupation == Occupation.MASON;
            default -> false;
        };
        if (trade) m += 0.2 + 0.1 * rank;
        if (specialty != null && specialty.job == j) m += specialtyBonus();
        return m;
    }

    /** A specialty adds 10% at Bronze, 15% Black Iron, 20% Silver, 30% Gold. */
    public double specialtyBonus() {
        return switch (grade) {
            case 0 -> 0.10;
            case 1 -> 0.15;
            case 2 -> 0.20;
            case 3 -> 0.30;
            default -> 0.05;
        };
    }

    /**
     * A new resident. Commoners have stats of 5 to 20; talents are graded, with better stats
     * and a specialty. tokenGrade (0 Bronze .. 3 Gold) raises the chance of a better talent.
     */
    public static Resident random(RandomSource r, Occupation occ, int tokenGrade) {
        Resident res = new Resident();
        res.name = Names.random(r);
        res.occupation = occ;
        res.skin = r.nextInt(64);
        res.loyalty = 60 + r.nextInt(21);
        if (occ.special) {
            float roll = r.nextFloat() / (1.0F + 0.2F * tokenGrade);
            res.grade = roll < 0.005F ? 2 : roll < 0.05F ? 1 : roll < 0.35F ? 0 : COMMON;
            float rk = r.nextFloat();
            res.rank = rk < 0.03F ? 2 : rk < 0.20F ? 1 : 0;
        }
        int lo = switch (res.grade) {
            case 0 -> 10;
            case 1 -> 25;
            case 2 -> 40;
            case 3 -> 55;
            default -> 5;
        };
        int span = 16;
        res.command = lo + r.nextInt(span);
        res.force = lo + r.nextInt(span);
        res.intelligence = lo + r.nextInt(span);
        res.politics = lo + r.nextInt(span);
        switch (occ) {
            case CIVIL_SERVANT, SCHOLAR -> res.politics += 10;
            case OFFICER, MARTIAL_ARTIST -> {
                res.command += 8;
                res.force += 10;
            }
            case DOCTOR -> res.intelligence += 10;
            default -> { }
        }
        if (res.grade != COMMON) res.specialty = Specialty.forOccupation(occ);
        return res;
    }

    public static Occupation randomOccupation(RandomSource r, double specialBoost) {
        double total = 0;
        for (Occupation o : Occupation.values()) total += o.special ? o.weight * specialBoost : o.weight;
        double pick = r.nextDouble() * total;
        for (Occupation o : Occupation.values()) {
            pick -= o.special ? o.weight * specialBoost : o.weight;
            if (pick <= 0) return o;
        }
        return Occupation.FARMER;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("name", name);
        t.putInt("grade", grade);
        t.putInt("occ", occupation.ordinal());
        t.putInt("rank", rank);
        t.putInt("loyalty", loyalty);
        t.putIntArray("stats", new int[]{command, force, intelligence, politics});
        t.putInt("spec", specialty == null ? -1 : specialty.ordinal());
        t.putInt("job", job.ordinal());
        t.putInt("work", workplace);
        t.putInt("skin", skin);
        if (entity != null) t.putUUID("entity", entity);
        t.putBoolean("militia", militia);
        t.put("inv", inv.createTag());
        t.putLongArray("gathered", gathered);
        return t;
    }

    public static Resident load(CompoundTag t) {
        Resident r = new Resident();
        r.id = t.getInt("id");
        r.name = t.getString("name");
        r.grade = t.getInt("grade");
        r.occupation = Occupation.byId(t.getInt("occ"));
        r.rank = t.getInt("rank");
        r.loyalty = t.getInt("loyalty");
        int[] s = t.getIntArray("stats");
        if (s.length == 4) {
            r.command = s[0];
            r.force = s[1];
            r.intelligence = s[2];
            r.politics = s[3];
        }
        int spec = t.getInt("spec");
        r.specialty = spec < 0 ? null : Specialty.byId(spec);
        r.job = Job.byId(t.getInt("job"));
        r.workplace = t.getInt("work");
        r.skin = t.getInt("skin");
        r.entity = t.hasUUID("entity") ? t.getUUID("entity") : null;
        r.militia = t.getBoolean("militia");
        r.inv.fromTag(t.getList("inv", net.minecraft.nbt.Tag.TAG_COMPOUND));
        long[] g = t.getLongArray("gathered");
        System.arraycopy(g, 0, r.gathered, 0, Math.min(g.length, 3));
        return r;
    }

    public void clampStats() {
        loyalty = Mth.clamp(loyalty, 0, 100);
    }
}
