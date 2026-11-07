package com.realmlord.entity;

import com.realmlord.loot.Gear;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.TokenGrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A human bandit, drawn with the player model. Swordsmen close in, archers shoot from range
 * and the chief leads them. The tier sets how dangerous they are: tier 1 is the band of the
 * Village Creation quest, beatable by a player in iron armour; later raids bring higher tiers.
 */
public class BanditEntity extends Monster implements RangedAttackMob {
    public enum Role { SWORDSMAN, ARCHER, CHIEF }

    /** Whose men: highwaymen, the water bandits of the rivers, or barbarians off the steppe. */
    public enum Faction { BANDIT, WATER, BARBARIAN, MOUNTAIN }

    public static final int WATER_SKINS = 2;
    public static final int BARBARIAN_SKINS = 3;
    public static final int MOUNTAIN_SKINS = 3;
    /** Persistent data: where a raiding band is marching. */
    public static final String RAID_TARGET = "realmlord_raid_target";

    public static final int SKINS = 4;
    public static final int ARCHER_SKINS = 2;

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);

    private final RangedBowAttackGoal<BanditEntity> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 30, 15.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.1, false);
    private int tier = 1;

    public BanditEntity(EntityType<? extends BanditEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, Role.SWORDSMAN.ordinal());
        entityData.define(SKIN, 0);
        entityData.define(FACTION, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(4, new MarchGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Bandits prey on villagers too, though never on their own captives.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, com.realmlord.entity.SettlerEntity.class, 10, true, false,
                e -> e instanceof com.realmlord.entity.SettlerEntity s && !s.isCaptive() && !s.sheltered()));
    }

    // ------------------------------------------------------------------ role and strength

    public Role getRole() {
        int r = entityData.get(ROLE);
        return Role.values()[Math.max(0, Math.min(r, Role.values().length - 1))];
    }

    public Faction getFaction() {
        int f = entityData.get(FACTION);
        return Faction.values()[Math.max(0, Math.min(f, Faction.values().length - 1))];
    }

    public void setFaction(Faction f) {
        entityData.set(FACTION, f.ordinal());
        if (getRole() == Role.CHIEF) setCustomName(Component.translatable(chiefKey()));
    }

    private String chiefKey() {
        return switch (getFaction()) {
            case WATER -> "entity.realmlord.water_bandit_chief";
            case BARBARIAN -> "entity.realmlord.barbarian_chief";
            case MOUNTAIN -> "entity.realmlord.mountain_chief";
            default -> "entity.realmlord.bandit_chief";
        };
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public int getTier() {
        return tier;
    }

    /**
     * Sets role and tier, then the matching health, damage and gear. Tier 1 numbers (raw
     * melee damage before difficulty and armour): swordsman 5 with a stone sword, chief 7
     * with a graded iron sword, archers about 3 per arrow.
     */
    public void configure(Role role, int tier) {
        this.tier = Math.max(1, tier);
        entityData.set(ROLE, role.ordinal());
        int t = this.tier - 1;
        double health, damage, armor;
        switch (role) {
            case CHIEF -> {
                health = 50 + 25 * t;
                damage = 2 + 2 * t;
                armor = 6 + 2 * t;
                setItemSlot(EquipmentSlot.MAINHAND, Gear.make(Items.IRON_SWORD, TokenGrade.BLACK_IRON, random));
                base(Attributes.KNOCKBACK_RESISTANCE, 0.5);
            }
            case ARCHER -> {
                health = 16 + 6 * t;
                damage = 1 + t;
                armor = 1 + t;
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            default -> {
                health = 20 + 8 * t;
                damage = 1 + 1.5 * t;
                armor = 2 + 2 * t;
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(t >= 1 ? Items.IRON_SWORD : Items.STONE_SWORD));
            }
        }
        base(Attributes.MAX_HEALTH, health);
        base(Attributes.ATTACK_DAMAGE, damage);
        base(Attributes.ARMOR, armor);
        setHealth(getMaxHealth());
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setLeftHanded(false);
        if (role == Role.CHIEF) {
            setCustomName(Component.translatable(chiefKey()));
            setCustomNameVisible(true);
        }
        reassessWeaponGoal();
    }

    private void base(net.minecraft.world.entity.ai.attributes.Attribute a, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    public void reassessWeaponGoal() {
        if (level() == null || level().isClientSide) return;
        goalSelector.removeGoal(meleeGoal);
        goalSelector.removeGoal(bowGoal);
        if (getMainHandItem().getItem() instanceof BowItem) {
            bowGoal.setMinAttackInterval(level().getDifficulty() == net.minecraft.world.Difficulty.HARD ? 25 : 40);
            goalSelector.addGoal(2, bowGoal);
        } else {
            goalSelector.addGoal(2, meleeGoal);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        entityData.set(SKIN, random.nextInt(SKINS));
        boolean wild = reason == MobSpawnType.NATURAL;
        // Roaming bands: now and then led by a chief.
        Role role = wild && random.nextInt(20) == 0 ? Role.CHIEF : random.nextInt(10) < 3 ? Role.ARCHER : Role.SWORDSMAN;
        configure(role, 1);
        if (wild) getPersistentData().putBoolean("realmlord_wild", true);
        return out;
    }

    @Override
    protected Component getTypeName() {
        if (getFaction() == Faction.MOUNTAIN) return Component.translatable(getRole() == Role.CHIEF ? chiefKey() : "entity.realmlord.mountain_barbarian");
        if (getFaction() == Faction.WATER) return Component.translatable(getRole() == Role.CHIEF ? chiefKey() : "entity.realmlord.water_bandit");
        if (getFaction() == Faction.BARBARIAN) return Component.translatable(getRole() == Role.CHIEF ? chiefKey()
                : getRole() == Role.ARCHER ? "entity.realmlord.barbarian_archer" : "entity.realmlord.barbarian");
        return switch (getRole()) {
            case ARCHER -> Component.translatable("entity.realmlord.bandit_archer");
            case CHIEF -> Component.translatable("entity.realmlord.bandit_chief");
            default -> super.getTypeName();
        };
    }

    // ------------------------------------------------------------------ archery

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack arrowStack = getProjectile(getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, power);
        if (getMainHandItem().getItem() instanceof BowItem bow) arrow = bow.customArrow(arrow);
        double dx = target.getX() - getX();
        double dy = target.getY(1.0 / 3.0) - arrow.getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dist * 0.2, dz, 1.6F, (float) (14 - level().getDifficulty().getId() * 4));
        arrow.setBaseDamage(1.25 + 0.5 * (tier - 1));
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    // ------------------------------------------------------------------ loot

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        RandomSource r = random;
        if (getFaction() != Faction.BANDIT && getRole() == Role.CHIEF) {
            com.realmlord.world.Raids.chiefDrops(this, r, looting);
        }
        switch (getRole()) {
            case CHIEF -> {
                spawnAtLocation(new ItemStack(ModItems.SILVER_COIN.get(), 10 + r.nextInt(11) + looting * 3));
                spawnAtLocation(getMainHandItem().copy());
                if (getFaction() == Faction.BANDIT) spawnAtLocation(new ItemStack(ModItems.BANDIT_LEDGER.get()));
            }
            case ARCHER -> {
                spawnAtLocation(new ItemStack(ModItems.COPPER_COIN.get(), Math.min(64, 15 + r.nextInt(26) + looting * 5)));
                if (r.nextInt(2) == 0) spawnAtLocation(new ItemStack(Items.ARROW, 2 + r.nextInt(5)));
            }
            default -> spawnAtLocation(new ItemStack(ModItems.COPPER_COIN.get(), Math.min(64, 15 + r.nextInt(26) + looting * 5)));
        }
        // As in the novel, human NPCs often carry building blueprints.
        if (getRole() != Role.CHIEF && r.nextFloat() < 0.08F + 0.02F * looting) {
            spawnAtLocation(com.realmlord.loot.Blueprints.random(r, false));
        }
        if (getRole() != Role.CHIEF && recentlyHit && r.nextFloat() < 0.06F + 0.02F * looting) {
            Item[] pool = {Items.IRON_SWORD, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.IRON_AXE};
            spawnAtLocation(Gear.make(pool[r.nextInt(pool.length)], TokenGrade.BRONZE, r));
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(ROLE));
        tag.putInt("Skin", getSkin());
        tag.putInt("Tier", tier);
        tag.putInt("Faction", entityData.get(FACTION));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        entityData.set(SKIN, tag.getInt("Skin"));
        tier = Math.max(1, tag.getInt("Tier"));
        entityData.set(FACTION, tag.getInt("Faction"));
        reassessWeaponGoal();
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, skin);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        // Roaming bandits come and go; those of a camp stay.
        return getPersistentData().getBoolean("realmlord_wild");
    }

    public static boolean isServer(Level level) {
        return level instanceof ServerLevel;
    }

    /** Outlined for the Lord hunting this band (and nobody else), so they can be found in jungle and on hills. */
    @Override
    public boolean isCurrentlyGlowing() {
        if (level().isClientSide && net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && ClientGlow.glows(getId())) return true;
        return super.isCurrentlyGlowing();
    }

    /** A raiding band marches on the village's manor until it has someone to fight. */
    static final class MarchGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final BanditEntity mob;
        private int recalc;

        MarchGoal(BanditEntity mob) {
            this.mob = mob;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        private net.minecraft.core.BlockPos target() {
            var d = mob.getPersistentData();
            return d.contains(RAID_TARGET) ? net.minecraft.core.BlockPos.of(d.getLong(RAID_TARGET)) : null;
        }

        @Override
        public boolean canUse() {
            var t = target();
            return t != null && mob.getTarget() == null && mob.blockPosition().distSqr(t) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (--recalc > 0) return;
            recalc = 40;
            var t = target();
            if (t == null) return;
            // Long marches go in stages the pathfinder can manage.
            double dx = t.getX() - mob.getX(), dz = t.getZ() - mob.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            double step = Math.min(1.0, 24.0 / Math.max(1, d));
            mob.getNavigation().moveTo(mob.getX() + dx * step, t.getY(), mob.getZ() + dz * step, 1.0);
        }
    }

    /** Keeps client classes out of the server's way. */
    private static final class ClientGlow {
        static boolean glows(int id) {
            return com.realmlord.client.QuestGlow.glows(id);
        }
    }
}
