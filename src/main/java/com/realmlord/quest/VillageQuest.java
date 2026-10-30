package com.realmlord.quest;

import com.realmlord.territory.TokenGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * Progress through the Village Creation quest, the Lord-only quest that decides the grade
 * of a player's Village Creation Token.
 *
 * <ol>
 *   <li>SUPPLIES: hand over logs, stone and food for the settlers.</li>
 *   <li>BANDITS: clear the bandit camp that holds the settlers captive.</li>
 *   <li>CLEARED: the camp is cleared; claim the token (the hidden cache can still be found).</li>
 *   <li>CLAIMED / FAILED: finished.</li>
 * </ol>
 */
public class VillageQuest {
    public enum Stage { NOT_STARTED, SUPPLIES, BANDITS, CLEARED, CLAIMED, FAILED }

    public static final int LOGS_NEEDED = 64;
    public static final int STONE_NEEDED = 64;
    public static final int FOOD_NEEDED = 32;

    public Stage stage = Stage.NOT_STARTED;
    public long startTick;
    public long clearTick;
    public int logs;
    public int stone;
    public int food;
    public BlockPos camp;          // centre of the bandit camp (y filled in once built)
    public boolean campBuilt;
    public int banditsLeft;
    public int captives;
    public boolean chiefKilled;
    public BlockPos cache;
    public boolean cacheFound;
    public int deaths;
    public boolean warned;
    public TokenGrade grade;       // set once claimed

    public boolean active() {
        return stage == Stage.SUPPLIES || stage == Stage.BANDITS || stage == Stage.CLEARED;
    }

    public boolean suppliesComplete() {
        return logs >= LOGS_NEEDED && stone >= STONE_NEEDED && food >= FOOD_NEEDED;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("stage", stage.ordinal());
        t.putLong("start", startTick);
        t.putLong("clear", clearTick);
        t.putInt("logs", logs);
        t.putInt("stone", stone);
        t.putInt("food", food);
        if (camp != null) t.put("camp", NbtUtils.writeBlockPos(camp));
        t.putBoolean("built", campBuilt);
        t.putInt("bandits", banditsLeft);
        t.putInt("captives", captives);
        t.putBoolean("chief", chiefKilled);
        if (cache != null) t.put("cache", NbtUtils.writeBlockPos(cache));
        t.putBoolean("cacheFound", cacheFound);
        t.putInt("deaths", deaths);
        t.putBoolean("warned", warned);
        if (grade != null) t.putInt("grade", grade.ordinal());
        return t;
    }

    public void load(CompoundTag t) {
        Stage[] stages = Stage.values();
        int s = t.getInt("stage");
        stage = s >= 0 && s < stages.length ? stages[s] : Stage.NOT_STARTED;
        startTick = t.getLong("start");
        clearTick = t.getLong("clear");
        logs = t.getInt("logs");
        stone = t.getInt("stone");
        food = t.getInt("food");
        camp = t.contains("camp") ? NbtUtils.readBlockPos(t.getCompound("camp")) : null;
        campBuilt = t.getBoolean("built");
        banditsLeft = t.getInt("bandits");
        captives = t.getInt("captives");
        chiefKilled = t.getBoolean("chief");
        cache = t.contains("cache") ? NbtUtils.readBlockPos(t.getCompound("cache")) : null;
        cacheFound = t.getBoolean("cacheFound");
        deaths = t.getInt("deaths");
        warned = t.getBoolean("warned");
        grade = t.contains("grade") ? TokenGrade.byId(t.getInt("grade")) : null;
    }

    public void copyFrom(VillageQuest o) {
        load(o.save());
    }
}
