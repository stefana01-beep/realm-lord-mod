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
import net.minecraft.server.level.ServerPlayer;
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
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
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
        goalSelector.addGoal(1, new PanicGoal(this, 0.75) {
            @Override
            public boolean canUse() {
                return !fighter() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 0.9, true) {
            @Override
            public boolean canUse() {
                return fighter() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BanditEntity.class, 8.0F, 0.6, 0.75) {
            @Override
            public boolean canUse() {
                return !isCaptive() && !fighter() && super.canUse();
            }
        });
        // Fighters (companions, the militia and hunters on the hunt) go for what threatens the village.
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return fighter() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof SettlerEntity)) {
            @Override
            public boolean canUse() {
                return fighter() && super.canUse();
            }
        });
        targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, 20, true, false,
                e -> activity == Activity.HUNT && terr != null && !terr.contains(level().dimension(), e.blockPosition())
                        && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())) {
            @Override
            public boolean canUse() {
                return activity == Activity.HUNT && super.canUse();
            }
        });
        goalSelector.addGoal(3, new LaborGoal());
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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

    // ------------------------------------------------------------------ the working day

    /** What a resident is up to. Each has a text key for their card. */
    public enum Activity {
        IDLE, HOME, BUILD, CHOP, GATHER, FARM, STAFF, ADMIN, DEPOSIT, FOLLOW, PATROL, HUNT;

        public String key() {
            return "realmlord.activity." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private Territory terr;
    private com.realmlord.village.Resident res;
    private Activity activity = Activity.IDLE;
    private BlockPos spot;
    private int spotRadius = 8;
    /** Their own bed, for the night. */
    private BlockPos bed;
    private BlockPos wander;
    private int wanderUntil;
    // Building
    private com.realmlord.village.Sites.Op op;
    private com.realmlord.village.Building lastSite;
    private int opAge;
    // Felling a tree, top log first
    private java.util.List<BlockPos> treeLogs = new java.util.ArrayList<>();
    private BlockPos treeBase;
    private int treeSearchIn;
    private int strikes;
    // Quarrying, mining and farming
    private long lastHaul;
    private BlockPos tend;
    private int actIn;
    // Getting there
    private BlockPos goingTo;
    private double bestDist = Double.MAX_VALUE;
    private int noProgress;

    public com.realmlord.village.Resident resident() {
        return res;
    }

    public Activity getActivity() {
        return activity;
    }

    /** Called every second by the village: decide what to do now. */
    public void routine(Territory t, com.realmlord.village.Resident r) {
        terr = t;
        res = r;
        if (!settlerName.equals(r.name)) {
            settlerName = r.name;
            refreshName();
        }
        strengthen();
        if (following()) {
            activity = Activity.FOLLOW;
            if (isSleeping()) wake();
            r.activity = activity.key();
            clearRestriction();
            arm();
            return;
        }
        decide();
        if (isSleeping() && (activity != Activity.HOME || bed == null)) wake();
        // Woken some other way (their bed broken, say): free to move again.
        if (!isSleeping()) wake();
        r.activity = isSleeping() ? "realmlord.activity.sleeping" : activity.key();
        if (activity == Activity.HUNT || activity == Activity.PATROL) clearRestriction();
        else restrictTo(spot, spotRadius);
        if (activity == Activity.PATROL || activity == Activity.HUNT) arm();
        else equip();
    }

    // ------------------------------------------------------------------ fighting

    /** Fighting now: a companion at the Lord's side, the militia on duty, a hunter on the hunt. */
    public boolean fighter() {
        return activity == Activity.FOLLOW || activity == Activity.PATROL || activity == Activity.HUNT;
    }

    private ServerPlayer lord() {
        if (!(level() instanceof ServerLevel sl) || terr == null || terr.owner == null) return null;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(terr.owner);
        return p != null && p.level() == level() && !p.isSpectator() ? p : null;
    }

    /** Whether this resident is walking with the Lord now. */
    private boolean following() {
        if (res == null || !res.follows || !(level() instanceof ServerLevel sl)) return false;
        if (res.woundedUntil > com.realmlord.village.GaiaCalendar.today(sl.getServer())) return false;
        return lord() != null;
    }

    /** Health and strength follow Force: a great general is far harder to put down than a farmer. */
    private void strengthen() {
        if (res == null) return;
        double hp = 20 + (res.isFigure() || res.militia ? res.force / 4.0 : 0);
        var h = getAttribute(Attributes.MAX_HEALTH);
        if (h != null && h.getBaseValue() != hp) {
            h.setBaseValue(hp);
            if (getHealth() > hp) setHealth((float) hp);
        }
        var a = getAttribute(Attributes.ATTACK_DAMAGE);
        double dmg = 1 + res.force / 15.0;
        if (a != null && a.getBaseValue() != dmg) a.setBaseValue(dmg);
    }

    /** A weapon for the fight: a diamond sword for a Gold-grade talent, iron for the rest, a bow-less spear-hand for the militia. */
    private void arm() {
        net.minecraft.world.item.Item weapon = res != null && res.grade >= 3 ? net.minecraft.world.item.Items.DIAMOND_SWORD
                : activity == Activity.HUNT ? net.minecraft.world.item.Items.IRON_AXE : net.minecraft.world.item.Items.IRON_SWORD;
        hold(weapon);
    }

    /** One tick at the Lord's side: keep up (or catch up) and keep them safe. */
    private void accompany() {
        ServerPlayer p = lord();
        if (p == null || res == null || !res.follows) {
            activity = Activity.IDLE;
            return;
        }
        double d = distanceToSqr(p);
        if (d > 24 * 24) {
            // Lost the Lord: catch up at once, as companions do.
            BlockPos at = p.blockPosition();
            for (int i = 0; i < 16; i++) {
                int x = at.getX() + (i == 0 ? 0 : random.nextInt(5) - 2), z = at.getZ() + (i == 0 ? 0 : random.nextInt(5) - 2);
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos s = new BlockPos(x, at.getY() + dy, z);
                    // Grass, flowers and snow are no bar; only solid ground below and room to stand.
                    if (level().getBlockState(s).getCollisionShape(level(), s).isEmpty()
                            && level().getBlockState(s.above()).getCollisionShape(level(), s.above()).isEmpty()
                            && level().getBlockState(s.below()).isFaceSturdy(level(), s.below(), net.minecraft.core.Direction.UP)
                            && level().getFluidState(s).isEmpty()) {
                        getNavigation().stop();
                        teleportTo(x + 0.5, s.getY(), z + 0.5);
                        return;
                    }
                }
            }
            return;
        }
        // Whoever hurts the Lord, or whom the Lord fights, is the enemy.
        if (getTarget() == null) {
            net.minecraft.world.entity.LivingEntity foe = p.getLastHurtByMob();
            if (foe == null) foe = p.getLastHurtMob();
            if (foe != null && foe.isAlive() && !(foe instanceof SettlerEntity) && foe.distanceToSqr(this) < 20 * 20
                    && !(foe instanceof net.minecraft.world.entity.player.Player)) setTarget(foe);
        }
        if (getTarget() == null && d > 4 * 4) getNavigation().moveTo(p, 1.0);
    }

    /** The militia: by day half drill at the camp and half walk the fence; at night the watch walks it. */
    private BlockPos patrolSpot(Territory t) {
        com.realmlord.village.Building camp = null;
        for (com.realmlord.village.Building b : t.buildings) {
            if (b.complete && (b.type == com.realmlord.village.BuildingType.BARRACKS || b.type == com.realmlord.village.BuildingType.MILITARY_CAMP)) camp = b;
        }
        boolean walk = camp == null || (res.id + level().getDayTime() / 3000) % 2 == 0;
        if (!walk) return camp.center();
        double a = ((res.id * 47 + level().getDayTime() / 600) % 360) * Math.PI / 180;
        int r = Math.max(8, t.radius() - 6);
        int x = t.center.getX() + (int) (Math.cos(a) * r), z = t.center.getZ() + (int) (Math.sin(a) * r);
        return new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Hunters go out beyond the border to the wilds. */
    private BlockPos huntSpot(Territory t) {
        double a = ((res.id * 83 + level().getDayTime() / 2400) % 360) * Math.PI / 180;
        int r = t.radius() + 24;
        int x = t.center.getX() + (int) (Math.cos(a) * r), z = t.center.getZ() + (int) (Math.sin(a) * r);
        return new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private boolean late() {
        long time = level().getDayTime() % 24000L;
        return time >= 11000 && time < 13000;
    }

    private void decide() {
        Territory t = terr;
        com.realmlord.village.Resident r = res;
        Activity was = activity;
        boolean watch = r.militia && r.id % 2 == 1;
        if (!level().isDay() && !watch) {
            activity = Activity.HOME;
            bed = level() instanceof ServerLevel sl ? com.realmlord.village.Residents.bedOf(sl, t, r) : null;
            spot = bed != null ? bed : com.realmlord.village.Residents.home(t, r);
            spotRadius = bed != null ? 2 : 4;
        } else if (com.realmlord.village.Haul.load(r.inv) >= 24 || late() && com.realmlord.village.Haul.carriesAnything(r.inv)) {
            activity = Activity.DEPOSIT;
            spot = com.realmlord.village.Residents.manorSpot(t, r);
            spotRadius = 3;
        } else if (r.militia) {
            activity = Activity.PATROL;
            spot = patrolSpot(t);
            spotRadius = 6;
        } else if (r.office >= 0) {
            activity = Activity.ADMIN;
            spot = com.realmlord.village.Residents.manorSpot(t, r);
            spotRadius = 5;
        } else if (r.job == com.realmlord.village.Job.HUNTING) {
            activity = Activity.HUNT;
            spot = huntSpot(t);
            spotRadius = 16;
        } else {
            com.realmlord.village.Building field = com.realmlord.village.Residents.field(t, r);
            switch (r.job) {
                case CONSTRUCTION -> {
                    com.realmlord.village.Building site = com.realmlord.village.Residents.siteOf(t, r);
                    if (site != null) {
                        activity = Activity.BUILD;
                        spot = site.footprint() ? site.center() : (op != null ? op.pos() : t.center);
                        spotRadius = site.footprint() ? Math.max(site.sizeX(), site.sizeZ()) / 2 + 4 : 12;
                    } else idle(t);
                }
                case LOGGING -> work(field, Activity.CHOP, 12, t);
                case QUARRYING, MINING -> work(field, Activity.GATHER, 3, t);
                case FARMING -> work(field, Activity.FARM, field == null ? 4 : Math.max(field.sizeX(), field.sizeZ()) / 2, t);
                case STAFF -> {
                    com.realmlord.village.Building w = t.building(r.workplace);
                    if (w != null && w.complete) {
                        activity = Activity.STAFF;
                        spot = w.center();
                        spotRadius = 3;
                    } else idle(t);
                }
                case ADMINISTRATION -> {
                    activity = Activity.ADMIN;
                    spot = com.realmlord.village.Residents.manorSpot(t, r);
                    spotRadius = 5;
                }
                default -> idle(t);
            }
        }
        if (activity != was) {
            op = null;
            treeLogs.clear();
            tend = null;
            goingTo = null;
        }
    }

    private void work(com.realmlord.village.Building field, Activity a, int radius, Territory t) {
        if (field == null) {
            idle(t);
            return;
        }
        activity = a;
        spot = field.center();
        spotRadius = radius;
    }

    /** No work: wander about the village, a different part every while. */
    private void idle(Territory t) {
        activity = Activity.IDLE;
        if (wander == null || tickCount > wanderUntil) {
            int r = Math.max(10, (int) (t.radius() * 0.6));
            for (int i = 0; i < 8; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = 10 + random.nextDouble() * (r - 10);
                int x = t.center.getX() + (int) (Math.cos(a) * d), z = t.center.getZ() + (int) (Math.sin(a) * d);
                if (!level().isLoaded(new BlockPos(x, 64, z))) continue;
                int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (!level().getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) continue;
                wander = new BlockPos(x, y, z);
                break;
            }
            if (wander == null) wander = com.realmlord.village.Residents.manorSpot(t, res);
            wanderUntil = tickCount + 1200 + random.nextInt(1200);
        }
        spot = wander;
        spotRadius = 8;
    }

    /** The tool for the job in hand. */
    private void equip() {
        net.minecraft.world.item.Item want = switch (activity) {
            case BUILD -> net.minecraft.world.item.Items.IRON_SHOVEL;
            case CHOP -> net.minecraft.world.item.Items.IRON_AXE;
            case GATHER -> res.job == com.realmlord.village.Job.MINING ? net.minecraft.world.item.Items.IRON_PICKAXE : net.minecraft.world.item.Items.STONE_PICKAXE;
            case FARM -> net.minecraft.world.item.Items.IRON_HOE;
            case DEPOSIT -> {
                for (int i = 0; i < res.inv.getContainerSize(); i++) {
                    if (com.realmlord.village.Haul.value(res.inv.getItem(i)) != null) yield res.inv.getItem(i).getItem();
                }
                yield net.minecraft.world.item.Items.AIR;
            }
            default -> net.minecraft.world.item.Items.AIR;
        };
        hold(want);
    }

    private void hold(net.minecraft.world.item.Item item) {
        if (!getMainHandItem().is(item)) {
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, item == net.minecraft.world.item.Items.AIR
                    ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item));
            setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    /** Lies down in their bed, and stays put until morning. */
    private void sleepIn(BlockPos head) {
        var state = level().getBlockState(head);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) || state.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED)) return;
        getNavigation().stop();
        startSleeping(head);
        goalSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE);
        goalSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK);
        goalSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP);
    }

    private void wake() {
        if (isSleeping()) stopSleeping();
        goalSelector.enableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE);
        goalSelector.enableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK);
        goalSelector.enableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP);
    }

    private boolean busy() {
        if (activity == Activity.FOLLOW) return res != null && getTarget() == null;
        if (terr == null || res == null || spot == null || isCaptive() || isSleeping()) return false;
        if (activity == Activity.HOME && bed != null) return true;
        return switch (activity) {
            case BUILD, CHOP, GATHER, FARM, DEPOSIT, FOLLOW -> true;
            case PATROL, HUNT -> getTarget() == null;
            default -> horizontalDist(spot) > spotRadius + 2;
        };
    }

    private double horizontalDist(BlockPos p) {
        double dx = getX() - (p.getX() + 0.5), dz = getZ() - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Walks towards a block until within reach (sideways). Returns true once there. */
    private boolean approach(BlockPos target, double reach) {
        double d = horizontalDist(target);
        if (d <= reach && Math.abs(getY() - target.getY()) < 20) {
            getNavigation().stop();
            goingTo = null;
            return true;
        }
        if (!target.equals(goingTo)) {
            goingTo = target;
            bestDist = d;
            noProgress = 0;
        }
        if (getNavigation().isDone() || tickCount % 40 == 0) {
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
            getNavigation().moveTo(target.getX() + 0.5, Math.min(y, target.getY() + 1), target.getZ() + 0.5, 0.9);
        }
        if (tickCount % 20 == 0) {
            if (d < bestDist - 0.5) {
                bestDist = d;
                noProgress = 0;
            } else if (++noProgress >= 8) {
                // Stuck on the way: find a way round, as villagers in the novel always seem to.
                hop(target);
                noProgress = 0;
                bestDist = horizontalDist(target);
            }
        }
        return false;
    }

    /** Moves to open ground beside a spot. */
    private void hop(BlockPos target) {
        for (int i = 0; i < 12; i++) {
            int x = target.getX() + random.nextInt(7) - 3, z = target.getZ() + random.nextInt(7) - 3;
            if (!level().isLoaded(new BlockPos(x, 64, z))) continue;
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (!level().getBlockState(at.below()).getFluidState().isEmpty()) continue;
            if (!level().getBlockState(at).isAir() || !level().getBlockState(at.above()).isAir()) continue;
            getNavigation().stop();
            teleportTo(x + 0.5, y, z + 0.5);
            return;
        }
    }

    private void act(BlockPos at) {
        getLookControl().setLookAt(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private int pace(com.realmlord.village.Job job) {
        return Math.max(4, (int) Math.round(10 / res.skillAt(job)));
    }

    /** One tick of whatever the resident is doing. */
    private void labor() {
        if (!(level() instanceof ServerLevel sl)) return;
        switch (activity) {
            case FOLLOW -> accompany();
            case PATROL, HUNT -> approach(spot, spotRadius);
            case BUILD -> build(sl);
            case CHOP -> chop(sl);
            case GATHER -> gather(sl);
            case FARM -> farm(sl);
            case DEPOSIT -> {
                if (approach(spot, 3)) {
                    act(terr.center);
                    com.realmlord.village.Haul.deposit(terr, res);
                    decide();
                    equip();
                }
            }
            case HOME -> {
                if (bed != null) {
                    if (approach(bed, 1.6)) sleepIn(bed);
                } else {
                    approach(spot, spotRadius);
                }
            }
            default -> approach(spot, spotRadius);
        }
    }

    private void build(ServerLevel sl) {
        com.realmlord.village.Building site = com.realmlord.village.Residents.siteOf(terr, res);
        if (site == null) return;
        if (op != null && !site.equals(lastSite)) op = null;
        lastSite = site;
        if (op == null || ++opAge > 40) {
            op = com.realmlord.village.Sites.next(sl, terr, site);
            opAge = 0;
        }
        if (op == null) {
            // Waiting for the work to catch up, or for the area to load: stand by the site.
            if (approach(spot, Math.max(4, spotRadius - 2))) site.lastWork = sl.getGameTime();
            return;
        }
        if (!approach(op.pos(), 4.5)) return;
        if (--actIn > 0) return;
        actIn = pace(com.realmlord.village.Job.CONSTRUCTION);
        if (op.kind() == com.realmlord.village.Sites.Kind.PLACE || op.kind() == com.realmlord.village.Sites.Kind.FILL || op.kind() == com.realmlord.village.Sites.Kind.REPAIR) {
            net.minecraft.world.item.Item item = op.state().getBlock().asItem();
            if (item != net.minecraft.world.item.Items.AIR) hold(item);
        } else {
            hold(net.minecraft.world.item.Items.IRON_SHOVEL);
        }
        act(op.pos());
        if (com.realmlord.village.Sites.apply(sl, terr, site, op, this)) {
            op = null;
        } else if (getBoundingBox().intersects(new net.minecraft.world.phys.AABB(op.pos()))) {
            hop(op.pos().offset(3, 0, 0));
        }
    }

    private double carried(int resource) {
        double units = 0;
        for (int i = 0; i < res.inv.getContainerSize(); i++) {
            int[] v = com.realmlord.village.Haul.value(res.inv.getItem(i));
            if (v != null && v[0] == resource) units += v[1] * res.inv.getItem(i).getCount();
        }
        return units + res.gathered[resource];
    }

    private void chop(ServerLevel sl) {
        com.realmlord.village.Building field = com.realmlord.village.Residents.field(terr, res);
        if (field == null) return;
        double quota = com.realmlord.village.Village.quota(terr, res)[0];
        if (treeLogs.isEmpty()) {
            if (carried(com.realmlord.village.Haul.WOOD) >= quota) {
                // The day's wood is cut: tidy up at the field.
                tendField(field, 12);
                return;
            }
            if (--treeSearchIn <= 0) {
                treeSearchIn = 100;
                BlockPos base = findTree(sl, field.center(), 20);
                if (base != null) {
                    treeBase = base;
                    treeLogs = new java.util.ArrayList<>(com.realmlord.village.Sites.tree(sl, base));
                }
            }
            if (treeLogs.isEmpty()) {
                tendField(field, 12);
                return;
            }
        }
        if (!approach(treeBase, 2.5)) return;
        BlockPos log = treeLogs.get(0);
        if (!com.realmlord.village.Sites.isTreeLog(sl.getBlockState(log))) {
            treeLogs.remove(0);
            strikes = 0;
            if (treeLogs.isEmpty()) replant(sl);
            return;
        }
        if (--actIn > 0) return;
        actIn = pace(com.realmlord.village.Job.LOGGING);
        act(log);
        sl.playSound(null, log, net.minecraft.sounds.SoundEvents.WOOD_HIT, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 0.9F);
        strikes++;
        sl.destroyBlockProgress(getId(), log, Math.min(9, strikes * 3));
        if (strikes < 3) return;
        strikes = 0;
        sl.destroyBlockProgress(getId(), log, -1);
        net.minecraft.world.level.block.state.BlockState state = sl.getBlockState(log);
        for (net.minecraft.world.item.ItemStack drop : net.minecraft.world.level.block.Block.getDrops(state, sl, log, null)) {
            com.realmlord.village.Sites.store(terr, res, drop);
        }
        sl.destroyBlock(log, false, this);
        treeLogs.remove(0);
        if (treeLogs.isEmpty()) replant(sl, state);
    }

    private void replant(ServerLevel sl) {
        replant(sl, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
    }

    /** A sapling of the same kind where the tree stood. */
    private void replant(ServerLevel sl, net.minecraft.world.level.block.state.BlockState log) {
        if (treeBase == null || !sl.getBlockState(treeBase).isAir()) return;
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(log.getBlock());
        net.minecraft.world.level.block.Block sapling = net.minecraft.world.level.block.Blocks.OAK_SAPLING;
        if (key != null) {
            String path = key.getPath().replace("_log", "_sapling");
            if (path.startsWith("mangrove")) path = "mangrove_propagule";
            var b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(key.getNamespace(), path));
            if (b != null && b != net.minecraft.world.level.block.Blocks.AIR) sapling = b;
        }
        if (sapling.defaultBlockState().canSurvive(sl, treeBase)) {
            sl.setBlock(treeBase, sapling.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            sl.playSound(null, treeBase, net.minecraft.sounds.SoundEvents.GRASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        treeBase = null;
    }

    /** The base of the nearest living tree around a point, inside the village but not on its buildings. */
    private BlockPos findTree(ServerLevel sl, BlockPos around, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int x = around.getX() + dx, z = around.getZ() + dz;
                BlockPos col = new BlockPos(x, 64, z);
                if (!sl.isLoaded(col) || !terr.contains(sl.dimension(), col)) continue;
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos top = new BlockPos(x, y, z);
                if (!com.realmlord.village.Sites.isNaturalLog(sl.getBlockState(top)) || !com.realmlord.village.Sites.hasLeaves(sl, top)) continue;
                if (onBuilding(x, z)) continue;
                BlockPos base = top;
                while (com.realmlord.village.Sites.isNaturalLog(sl.getBlockState(base.below())) && top.getY() - base.getY() < 32) base = base.below();
                double d = distanceToSqr(x + 0.5, base.getY(), z + 0.5);
                if (d < bestD) {
                    bestD = d;
                    best = base;
                }
            }
        return best;
    }

    private boolean onBuilding(int x, int z) {
        int m = com.realmlord.village.Village.MANOR_YARD;
        if (Math.abs(x - terr.center.getX()) <= m && Math.abs(z - terr.center.getZ()) <= m) return true;
        for (com.realmlord.village.Building b : terr.buildings) {
            if (!b.type.placed() || b.type == com.realmlord.village.BuildingType.LOGGING_FIELD) continue;
            if (b.overlaps(x, z, 1, 1, 1)) return true;
        }
        return false;
    }

    /** Working at a field: walk about it, now and then working the ground. */
    private void tendField(com.realmlord.village.Building field, int r) {
        if (tend == null || random.nextInt(200) == 0) {
            int x = field.origin.getX() + 1 + random.nextInt(Math.max(1, field.sizeX() - 2));
            int z = field.origin.getZ() + 1 + random.nextInt(Math.max(1, field.sizeZ() - 2));
            tend = new BlockPos(x, field.origin.getY(), z);
        }
        if (!approach(tend, 1.5)) return;
        if (--actIn > 0) return;
        actIn = 30 + random.nextInt(30);
        act(tend.below());
    }

    private void gather(ServerLevel sl) {
        com.realmlord.village.Building field = com.realmlord.village.Residents.field(terr, res);
        if (field == null) return;
        boolean mine = res.job == com.realmlord.village.Job.MINING;
        if (!approach(field.center(), 3.5)) return;
        // Work the rock face in the pit.
        BlockPos face = field.center();
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos p = field.center().below(dy);
            if (!sl.getBlockState(p).isAir() && !sl.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LADDER)) {
                face = p;
                break;
            }
        }
        if (--actIn > 0) return;
        actIn = 20;
        act(face);
        sl.playSound(null, face, net.minecraft.sounds.SoundEvents.STONE_HIT, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.9F);
        sl.levelEvent(2001, face, net.minecraft.world.level.block.Block.getId(sl.getBlockState(face)));
        int resource = mine ? com.realmlord.village.Haul.ORE : com.realmlord.village.Haul.STONE;
        double quota = com.realmlord.village.Village.quota(terr, res)[resource];
        if (quota <= 0 || carried(resource) >= quota) return;
        long interval = Math.max(100, (long) (com.realmlord.RealmConfig.ticksPerNovelDay() / 2 / quota));
        if (sl.getGameTime() - lastHaul < interval) return;
        lastHaul = sl.getGameTime();
        com.realmlord.village.Sites.store(terr, res, new net.minecraft.world.item.ItemStack(mine ? net.minecraft.world.item.Items.RAW_IRON : net.minecraft.world.item.Items.COBBLESTONE));
    }

    private void farm(ServerLevel sl) {
        com.realmlord.village.Building field = com.realmlord.village.Residents.field(terr, res);
        if (field != null) tendField(field, 6);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Never stuck inside a wall going up around them.
        if (tickCount % 10 == 0 && isInWall()) {
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
            teleportTo(getX(), y, getZ());
        }
    }

    /** Goes to work and does it; strolling only fills the time in between. */
    private class LaborGoal extends net.minecraft.world.entity.ai.goal.Goal {
        LaborGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return busy();
        }

        @Override
        public boolean canContinueToUse() {
            return busy();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            labor();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (residentId < 0 || isCaptive()) return super.mobInteract(player, hand);
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.realmlord.network.ResidentViewPacket.open(sp, territoryId, residentId);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** A resident who dies is gone from the village for good; what they carried falls where they lie. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl) || territoryId < 0) return;
        Territory t = TerritoryData.get(sl.getServer()).byId(territoryId);
        var r = t == null ? null : t.resident(residentId);
        if (r == null || !getUUID().equals(r.entity)) return;
        for (int i = 0; i < r.inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack st = r.inv.getItem(i);
            if (!st.isEmpty()) spawnAtLocation(st.copy());
        }
        t.residents.remove(r);
        t.population = t.residents.size();
        com.realmlord.village.Village.tell(sl, t, Component.translatable("realmlord.village.died", t.name, source.getLocalizedDeathMessage(this)));
        TerritoryData.get(sl.getServer()).touch();
    }

    /** Captives cannot be harmed, so a stray blast cannot spoil the quest. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCaptive() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        if (isSleeping()) wake();
        // A figure from history is not lost in a skirmish: struck down, they are carried home to recover.
        if (res != null && res.isFigure() && amount >= getHealth() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && level() instanceof ServerLevel sl && terr != null) {
            res.follows = false;
            res.woundedUntil = com.realmlord.village.GaiaCalendar.today(sl.getServer()) + 1;
            setHealth(getMaxHealth() / 2);
            setTarget(null);
            BlockPos home = com.realmlord.village.Residents.home(terr, res);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.getX(), home.getZ());
            teleportTo(home.getX() + 0.5, y, home.getZ() + 0.5);
            com.realmlord.village.Village.tell(sl, terr, Component.translatable("realmlord.companion.wounded", res.name));
            return false;
        }
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
        if (getTags().contains(com.realmlord.village.Wanderers.TAG)) {
            com.realmlord.village.Wanderers.check(sl, this);
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
