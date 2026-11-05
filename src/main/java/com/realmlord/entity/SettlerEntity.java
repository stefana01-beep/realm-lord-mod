package com.realmlord.entity;

import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

/**
 * A settler, drawn with the player model. Held captive by bandits during the Village
 * Creation quest; once freed they follow their Lord to the new village and live there.
 */
public class SettlerEntity extends PathfinderMob {
    public static final int SKINS = 8;

    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(SettlerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAPTIVE = SynchedEntityData.defineId(SettlerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String[] SURNAMES = {"Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Zhao", "Huang", "Zhou", "Wu",
            "Xu", "Sun", "Hu", "Zhu", "Gao", "Lin", "He", "Guo", "Ma", "Luo", "Song", "Tang", "Han", "Cao"};
    private static final String[] GIVEN = {"Ming", "Hua", "Jun", "Fang", "Lan", "Qiang", "Mei", "Tao", "Wei", "Ying", "Hong",
            "Bo", "Xiu", "Ping", "Lei", "Yun", "Shan", "Rong", "Gang", "Li", "An", "Chun", "De", "Feng", "Hui", "Jie", "Kang", "Lu"};

    private String settlerName = "";
    private UUID owner;
    private BlockPos home;
    /** The territory and resident record this settler shows, or -1. */
    private int territoryId = -1;
    private int residentId = -1;

    public SettlerEntity(EntityType<? extends SettlerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKIN, 0);
        entityData.define(CAPTIVE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 0.75));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BanditEntity.class, 8.0F, 0.6, 0.75) {
            @Override
            public boolean canUse() {
                return !isCaptive() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static String randomName(RandomSource r) {
        String given = GIVEN[r.nextInt(GIVEN.length)];
        if (r.nextBoolean()) given += GIVEN[r.nextInt(GIVEN.length)].toLowerCase(java.util.Locale.ROOT);
        return SURNAMES[r.nextInt(SURNAMES.length)] + " " + given;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        entityData.set(SKIN, random.nextInt(SKINS));
        settlerName = randomName(random);
        refreshName();
        setLeftHanded(false);
        return out;
    }

    // ------------------------------------------------------------------ state

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public boolean isCaptive() {
        return entityData.get(CAPTIVE);
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockPos getHome() {
        return home;
    }

    public String getSettlerName() {
        return settlerName;
    }

    public void makeCaptive(UUID owner) {
        this.owner = owner;
        entityData.set(CAPTIVE, true);
        refreshName();
    }

    /** Freed from the bandits: they gather by the camp until their Lord founds a village. */
    public void free(BlockPos gather) {
        entityData.set(CAPTIVE, false);
        restrictTo(gather, 10);
        refreshName();
    }

    /** Settles in the Lord's village and stays near the manor. */
    public void settle(UUID owner, BlockPos home) {
        this.owner = owner;
        this.home = home;
        entityData.set(CAPTIVE, false);
        restrictTo(home, 20);
        refreshName();
    }

    private void refreshName() {
        if (isCaptive()) {
            setCustomName(Component.translatable("entity.realmlord.captive_settler"));
        } else if (!settlerName.isEmpty()) {
            setCustomName(Component.literal(settlerName));
        }
        setCustomNameVisible(false);
    }

    // ------------------------------------------------------------------ residents

    /** Ties this entity to a resident of a territory. */
    public void link(com.realmlord.territory.Territory t, com.realmlord.village.Resident r) {
        territoryId = t.id;
        residentId = r.id;
        owner = t.owner;
        home = t.center;
        settlerName = r.name;
        entityData.set(SKIN, Math.floorMod(r.skin, SKINS));
        entityData.set(CAPTIVE, false);
        restrictTo(t.center, 20);
        refreshName();
    }

    public int getTerritoryId() {
        return territoryId;
    }

    public int getResidentId() {
        return residentId;
    }

    /** Work by day, home by night. */
    public void routine(com.realmlord.territory.Territory t, com.realmlord.village.Resident r) {
        if (!settlerName.equals(r.name)) {
            settlerName = r.name;
            refreshName();
        }
        BlockPos work = level().isDay() ? com.realmlord.village.Residents.workplace(t, r) : null;
        if (work != null) restrictTo(work, 6);
        else restrictTo(t.center, level().isDay() ? 20 : 10);
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (residentId < 0 || isCaptive()) return super.mobInteract(player, hand);
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.realmlord.network.ResidentViewPacket.open(sp, territoryId, residentId);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Captives cannot be harmed, so a stray blast cannot spoil the quest. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCaptive() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    /**
     * Freed settlers left behind at the camp (in chunks that were not loaded when the
     * village was founded) leave once their Lord has a village; the village has their place.
     */
    @Override
    public void tick() {
        super.tick();
        if (tickCount % 100 != 7 || !(level() instanceof ServerLevel sl) || isCaptive()) return;
        if (territoryId >= 0) {
            // A resident's body leaves when its record is gone or another body has replaced it.
            Territory t = TerritoryData.get(sl.getServer()).byId(territoryId);
            var r = t == null ? null : t.resident(residentId);
            if (r == null || !getUUID().equals(r.entity)) discard();
            return;
        }
        if (owner == null) return;
        // Freed settlers from before villages had residents: the village records take their place.
        Territory t = TerritoryData.get(sl.getServer()).ownedBy(owner);
        if (t != null) discard();
    }

    /** A spot on the ground near a centre, for spawning settlers. */
    public static BlockPos groundNear(ServerLevel level, BlockPos center, RandomSource r, int spread) {
        int x = center.getX() + r.nextInt(spread * 2 + 1) - spread;
        int z = center.getZ() + r.nextInt(spread * 2 + 1) - spread;
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Skin", getSkin());
        tag.putBoolean("Captive", isCaptive());
        tag.putString("SettlerName", settlerName);
        if (owner != null) tag.putUUID("Owner", owner);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        if (hasRestriction()) tag.put("Restrict", NbtUtils.writeBlockPos(getRestrictCenter()));
        tag.putInt("Territory", territoryId);
        tag.putInt("Resident", residentId);
        tag.putFloat("RestrictRadius", getRestrictRadius());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SKIN, tag.getInt("Skin"));
        entityData.set(CAPTIVE, tag.getBoolean("Captive"));
        settlerName = tag.getString("SettlerName");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
        territoryId = tag.contains("Territory") ? tag.getInt("Territory") : -1;
        residentId = tag.contains("Resident") ? tag.getInt("Resident") : -1;
        if (tag.contains("Restrict")) restrictTo(NbtUtils.readBlockPos(tag.getCompound("Restrict")), (int) tag.getFloat("RestrictRadius"));
        refreshName();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, skin);
    }
}
