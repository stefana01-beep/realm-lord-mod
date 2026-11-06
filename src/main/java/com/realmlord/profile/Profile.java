package com.realmlord.profile;

import com.realmlord.quest.VillageQuest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the mod remembers about one player: game mode, occupation, attributes, level,
 * merit, quest progress and territory. Stored as a player capability.
 */
public class Profile {
    public static final int MAX_LEVEL = 100;
    /** As in the novel: attributes start at 5, each level gives 2 free points, and none may pass 100. */
    public static final int START_STAT = 5;
    public static final int POINTS_PER_LEVEL = 2;
    public static final int STARTING_POINTS = 2;
    public static final int MAX_STAT = 100;
    public static final int POINTS_PER_WORK_LEVEL = 2;
    public static final int DATA_VERSION = 2;

    private PlayMode mode = PlayMode.NONE;
    private CombatClass combatClass;     // set when the (sub-)occupation is a combat one
    private WorkOccupation work;         // set when the (sub-)occupation is a work one
    private int level = 1;
    private long xp;
    private int points;
    private final int[] base = new int[Stat.values().length];
    private final int[] added = new int[Stat.values().length];
    private final int[] innate = new int[Innate.values().length];
    private long merit;
    /** Coins carried in the purse, in copper: kept off the inventory, as the novel's system bag does. */
    private long purse;
    private long reputation;
    private int workLevel = 1;
    private long workXp;
    private int selectedSkill;
    private final long[] cooldowns = new long[Skill.values().length];
    private int territoryId = -1;
    private final VillageQuest quest = new VillageQuest();
    private boolean dirty = true;

    // ------------------------------------------------------------------ role

    /**
     * Sets the game mode and occupation. As in the novel, innate attributes are rolled between
     * 5 and 20, the four acquired attributes all start at 5, and there are 2 free points.
     */
    public void chooseRole(PlayMode mode, CombatClass combat, WorkOccupation work, RandomSource random) {
        this.mode = mode;
        this.combatClass = combat;
        this.work = combat == null ? work : null;
        level = 1;
        xp = 0;
        workLevel = 1;
        workXp = 0;
        selectedSkill = 0;
        java.util.Arrays.fill(added, 0);
        java.util.Arrays.fill(cooldowns, 0L);
        java.util.Arrays.fill(base, START_STAT);
        rollInnate(random);
        points = STARTING_POINTS;
        reputation = 0;
        markDirty();
    }

    public void rollInnate(RandomSource random) {
        for (Innate i : Innate.values()) innate[i.ordinal()] = Innate.MIN + random.nextInt(Innate.MAX - Innate.MIN + 1);
    }

    /** Back to an unchosen player, as if they had never joined. */
    public void reset() {
        mode = PlayMode.NONE;
        combatClass = null;
        work = null;
        level = 1;
        xp = 0;
        points = 0;
        java.util.Arrays.fill(base, 0);
        java.util.Arrays.fill(added, 0);
        java.util.Arrays.fill(innate, 0);
        merit = 0;
        reputation = 0;
        workLevel = 1;
        workXp = 0;
        selectedSkill = 0;
        java.util.Arrays.fill(cooldowns, 0L);
        territoryId = -1;
        quest.load(new VillageQuest().save());
        markDirty();
    }

    /** A Lord who fails the Village Creation quest keeps their occupation as an Adventurer. */
    public void demoteToAdventurer() {
        mode = PlayMode.ADVENTURER;
        markDirty();
    }

    public PlayMode getMode() {
        return mode;
    }

    /** Whether the player has chosen where in the world to begin. */
    private boolean placed;

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
        markDirty();
    }

    public boolean hasChosen() {
        return mode != PlayMode.NONE;
    }

    public boolean isLord() {
        return mode == PlayMode.LORD;
    }

    public CombatClass getCombatClass() {
        return combatClass;
    }

    public WorkOccupation getWork() {
        return work;
    }

    /** Pure work Adventurers have no character level, as in the novel; everyone else does. */
    public boolean hasCharacterLevel() {
        return !(mode == PlayMode.ADVENTURER && work != null);
    }

    // ------------------------------------------------------------------ attributes

    public int stat(Stat s) {
        return Math.min(MAX_STAT, base[s.ordinal()] + added[s.ordinal()]);
    }

    public int innate(Innate i) {
        return innate[i.ordinal()];
    }

    public void setInnate(Innate i, int v) {
        innate[i.ordinal()] = Mth.clamp(v, 0, 100);
        markDirty();
    }

    public int baseStat(Stat s) {
        return base[s.ordinal()];
    }

    public int addedStat(Stat s) {
        return added[s.ordinal()];
    }

    public int getPoints() {
        return points;
    }

    public boolean allocate(Stat s) {
        if (points <= 0 || base[s.ordinal()] + added[s.ordinal()] >= MAX_STAT) return false;
        points--;
        added[s.ordinal()]++;
        markDirty();
        return true;
    }

    public void setPoints(int p) {
        points = Math.max(0, p);
        markDirty();
    }

    // ------------------------------------------------------------------ levels

    public int getLevel() {
        return level;
    }

    public long getXp() {
        return xp;
    }

    /** Experience for the current level, from the novel: EXP = 30(N^3 + 5N) - 80. */
    public static long xpToNext(int level) {
        long n = level;
        return 30L * (n * n * n + 5 * n) - 80;
    }

    /** Reaching level n gives 2 free points, and every fifth level +1 to all four attributes. */
    private void levelReached(int newLevel) {
        points += POINTS_PER_LEVEL;
        if (newLevel % 5 == 0) for (int i = 0; i < base.length; i++) base[i]++;
    }

    /** Adds character experience; returns the number of levels gained. */
    public int addXp(long amount) {
        if (!hasCharacterLevel() || amount <= 0 || level >= MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            gained++;
            levelReached(level);
        }
        if (level >= MAX_LEVEL) xp = 0;
        markDirty();
        return gained;
    }

    public void setLevel(int newLevel) {
        newLevel = Mth.clamp(newLevel, 1, MAX_LEVEL);
        while (level < newLevel) levelReached(++level);
        level = newLevel;
        xp = 0;
        markDirty();
    }

    /** Combat players lose a fifth of a level's experience on death (never a level). */
    public long deathPenalty() {
        if (!hasCharacterLevel()) return 0;
        long loss = Math.min(xp, xpToNext(level) / 5);
        xp -= loss;
        markDirty();
        return loss;
    }

    public int getWorkLevel() {
        return workLevel;
    }

    public long getWorkXp() {
        return workXp;
    }

    /** Adds occupation experience; returns the number of skill levels gained. */
    public int addWorkXp(long amount) {
        if (work == null || amount <= 0 || workLevel >= WorkOccupation.MAX_LEVEL) return 0;
        workXp += amount;
        int gained = 0;
        while (workLevel < WorkOccupation.MAX_LEVEL && workXp >= WorkOccupation.xpToNext(workLevel)) {
            workXp -= WorkOccupation.xpToNext(workLevel);
            workLevel++;
            gained++;
            points += POINTS_PER_WORK_LEVEL;
        }
        if (workLevel >= WorkOccupation.MAX_LEVEL) workXp = 0;
        markDirty();
        return gained;
    }

    public void setWorkLevel(int lvl) {
        lvl = Mth.clamp(lvl, 1, WorkOccupation.MAX_LEVEL);
        if (lvl > workLevel) points += (lvl - workLevel) * POINTS_PER_WORK_LEVEL;
        workLevel = lvl;
        workXp = 0;
        markDirty();
    }

    // ------------------------------------------------------------------ merit

    public long getMerit() {
        return merit;
    }

    public Title getTitle() {
        return isLord() ? Title.forMerit(merit) : Title.NONE;
    }

    /** Adds merit; returns true if the player's title rose. */
    public boolean addMerit(long amount) {
        Title before = getTitle();
        merit = Math.max(0, merit + amount);
        markDirty();
        return getTitle().ordinal() > before.ordinal();
    }

    public long getReputation() {
        return reputation;
    }

    public void addReputation(long r) {
        reputation = Math.max(0, reputation + r);
        markDirty();
    }

    public long getPurse() {
        return purse;
    }

    public void setPurse(long copper) {
        purse = Math.max(0, copper);
        markDirty();
    }

    public void setMerit(long m) {
        merit = Math.max(0, m);
        markDirty();
    }

    // ------------------------------------------------------------------ skills

    public List<Skill> unlockedSkills() {
        List<Skill> out = new ArrayList<>();
        if (combatClass == null) return out;
        for (Skill s : Skill.of(combatClass)) if (level >= s.unlockLevel) out.add(s);
        return out;
    }

    public Skill selectedSkill() {
        List<Skill> list = unlockedSkills();
        if (list.isEmpty()) return null;
        return list.get(Math.floorMod(selectedSkill, list.size()));
    }

    public void cycleSkill(int dir) {
        List<Skill> list = unlockedSkills();
        if (list.isEmpty()) return;
        selectedSkill = Math.floorMod(selectedSkill + dir, list.size());
        markDirty();
    }

    public long cooldownEnd(Skill s) {
        return cooldowns[s.ordinal()];
    }

    public void setCooldownEnd(Skill s, long gameTime) {
        cooldowns[s.ordinal()] = gameTime;
        markDirty();
    }

    // ------------------------------------------------------------------ territory and quest

    public int getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(int id) {
        territoryId = id;
        markDirty();
    }

    public VillageQuest quest() {
        return quest;
    }

    // ------------------------------------------------------------------ sync bookkeeping

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    // ------------------------------------------------------------------ persistence

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("mode", mode.ordinal());
        t.putInt("combat", combatClass == null ? -1 : combatClass.ordinal());
        t.putInt("work", work == null ? -1 : work.ordinal());
        t.putInt("level", level);
        t.putLong("xp", xp);
        t.putInt("points", points);
        t.putIntArray("base", base);
        t.putIntArray("added", added);
        t.putLong("merit", merit);
        t.putLong("purse", purse);
        t.putIntArray("innate", innate);
        t.putLong("reputation", reputation);
        t.putInt("ver", DATA_VERSION);
        t.putInt("workLevel", workLevel);
        t.putLong("workXp", workXp);
        t.putInt("selected", selectedSkill);
        t.putLongArray("cooldowns", cooldowns);
        t.putInt("territory", territoryId);
        t.put("quest", quest.save());
        t.putBoolean("placed", placed);
        return t;
    }

    public void load(CompoundTag t) {
        mode = PlayMode.byId(t.getInt("mode"));
        int c = t.contains("combat") ? t.getInt("combat") : -1;
        combatClass = c < 0 ? null : CombatClass.byId(c);
        int w = t.contains("work") ? t.getInt("work") : -1;
        work = w < 0 ? null : WorkOccupation.byId(w);
        level = Math.max(1, t.getInt("level"));
        xp = t.getLong("xp");
        points = t.getInt("points");
        copyInto(t.getIntArray("base"), base);
        copyInto(t.getIntArray("added"), added);
        merit = t.getLong("merit");
        purse = t.getLong("purse");
        copyInto(t.getIntArray("innate"), innate);
        reputation = t.getLong("reputation");
        workLevel = Math.max(1, t.getInt("workLevel"));
        workXp = t.getLong("workXp");
        selectedSkill = t.getInt("selected");
        long[] cd = t.getLongArray("cooldowns");
        java.util.Arrays.fill(cooldowns, 0L);
        System.arraycopy(cd, 0, cooldowns, 0, Math.min(cd.length, cooldowns.length));
        territoryId = t.contains("territory") ? t.getInt("territory") : -1;
        quest.load(t.getCompound("quest"));
        // Players from before the starting-place choice are already where they want to be.
        placed = t.contains("placed") ? t.getBoolean("placed") : mode != PlayMode.NONE;
        if (t.getInt("ver") < DATA_VERSION && mode != PlayMode.NONE) migrate();
        markDirty();
    }

    /**
     * Characters made before the novel's attribute rules: attributes back to 5 plus the
     * every-fifth-level bonuses, all their points refunded, and innate attributes rolled.
     */
    private void migrate() {
        java.util.Arrays.fill(base, START_STAT + level / 5);
        java.util.Arrays.fill(added, 0);
        points = STARTING_POINTS + POINTS_PER_LEVEL * (level - 1) + (work != null ? POINTS_PER_WORK_LEVEL * (workLevel - 1) : 0);
        rollInnate(RandomSource.create());
        if (xp >= xpToNext(level)) xp = xpToNext(level) - 1;
    }

    private static void copyInto(int[] from, int[] to) {
        java.util.Arrays.fill(to, 0);
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    public void copyFrom(Profile other) {
        load(other.save());
    }
}
