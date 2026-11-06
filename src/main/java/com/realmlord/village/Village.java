package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.RealmConfig;
import com.realmlord.loot.Blueprints;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.territory.Founding;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.territory.TokenGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Running a village: residents and their jobs, construction, the daily round of production,
 * food, taxes and immigration, the farming seasons, People's Thoughts, Security, the four
 * indexes and upgrading the village.
 */
public final class Village {
    private Village() {}

    /** Builders at which a building takes exactly its listed construction time. */
    public static final int NOMINAL_BUILDERS = 5;
    /** Half-width of the Lord's Manor yard, which nothing may be built on. */
    public static final int MANOR_YARD = 8;
    public static final int MANOR_HOUSING = 10;
    public static final int ADMIN_PLACES = 3;
    public static final long FARMLAND_MU = 10;
    public static final long FOOD_PER_MU = 200;

    // ================================================================== residents

    public static Resident add(Territory t, Resident r) {
        r.id = t.nextResidentId++;
        t.residents.add(r);
        t.population = t.residents.size();
        return r;
    }

    /**
     * The refugees who arrive when a village is founded, as in the novel: a village chief who
     * is a Bronze civil servant, two fruit farmers, a carpenter, a mason and five farmers,
     * plus any settlers freed from the bandits.
     */
    public static void settle(Territory t, RandomSource r, int extra) {
        if (t.settled) return;
        t.settled = true;
        int g = t.grade.ordinal();
        Resident chief = Resident.random(r, Occupation.CIVIL_SERVANT, g);
        chief.grade = 0;
        chief.politics = Math.max(chief.politics, 28);
        chief.specialty = Specialty.GREEN_THUMB;
        chief.loyalty = 75;
        add(t, chief);
        add(t, Resident.random(r, Occupation.FRUIT_FARMER, g));
        add(t, Resident.random(r, Occupation.FRUIT_FARMER, g));
        add(t, Resident.random(r, Occupation.CARPENTER, g));
        add(t, Resident.random(r, Occupation.MASON, g));
        for (int i = 0; i < 5; i++) add(t, Resident.random(r, Occupation.FARMER, g));
        for (int i = 0; i < extra; i++) add(t, Resident.random(r, Resident.randomOccupation(r, 1.0), g));
    }

    public static List<Resident> withJob(Territory t, Job job) {
        List<Resident> out = new ArrayList<>();
        for (Resident r : t.residents) if (r.job == job && !r.militia) out.add(r);
        return out;
    }

    public static int count(Territory t, Job job) {
        int n = 0;
        for (Resident r : t.residents) if (r.job == job && !r.militia) n++;
        return n;
    }

    // ================================================================== buildings

    public static List<Building> complete(Territory t, BuildingType type) {
        List<Building> out = new ArrayList<>();
        for (Building b : t.buildings) if (b.complete && b.type == type) out.add(b);
        return out;
    }

    public static int countComplete(Territory t, BuildingType type) {
        int n = 0;
        for (Building b : t.buildings) if (b.complete && b.type == type) n++;
        return n;
    }

    public static int housing(Territory t) {
        int h = MANOR_HOUSING;
        for (Building b : t.buildings) if (b.complete) h += b.type.housing();
        return h;
    }

    /** People the village can hold: its level's limit and the housing it has, whichever is lower. */
    public static int capacity(Territory t) {
        return Math.min(t.level.populationCap(), housing(t));
    }

    /** Places available for a job across the village. */
    public static int places(Territory t, Job job) {
        if (job == Job.CONSTRUCTION || job == Job.IDLE) return Integer.MAX_VALUE;
        if (job == Job.ADMINISTRATION) return ADMIN_PLACES;
        if (job == Job.STAFF) return 0;
        // A field takes any number of workers; each one adds to its output.
        for (Building b : t.buildings) if (b.complete && b.type.fieldJob() == job) return Integer.MAX_VALUE;
        return 0;
    }

    /** The resident staffing a building, or null. */
    public static Resident staffOf(Territory t, Building b) {
        for (Resident r : t.residents) if (r.job == Job.STAFF && r.workplace == b.id) return r;
        return null;
    }

    public static boolean isStaffing(Territory t, Resident r) {
        return r.job == Job.STAFF && t.building(r.workplace) != null;
    }

    /** True if a craftsman works in a finished workshop of his trade, so he can sell its goods. */
    public static boolean hasWorkshop(Territory t, Resident r) {
        if (r.job != Job.STAFF) return false;
        Building b = t.building(r.workplace);
        return b != null && b.complete;
    }

    // ================================================================== jobs

    /** Why a job cannot be given, or null if it can. */
    public static String canAssign(Territory t, Resident r, Job job, int workplace) {
        if (r.militia) return "realmlord.job.err.militia";
        if (job == Job.STAFF) {
            Building b = t.building(workplace);
            if (b == null || !b.complete) return "realmlord.job.err.no_building";
            if (b.type.staff() == null) return "realmlord.job.err.no_staff";
            if (b.type.staff() != r.occupation) return "realmlord.job.err.wrong_trade";
            Resident cur = staffOf(t, b);
            if (cur != null && cur != r) return "realmlord.job.err.staffed";
            return null;
        }
        if (job == Job.ADMINISTRATION && r.occupation != Occupation.CIVIL_SERVANT && r.occupation != Occupation.SCHOLAR) {
            return "realmlord.job.err.admin";
        }
        if (job != r.job && count(t, job) >= places(t, job)) return "realmlord.job.err.full";
        return null;
    }

    public static boolean assign(Territory t, Resident r, Job job, int workplace) {
        if (canAssign(t, r, job, workplace) != null) return false;
        r.job = job;
        r.workplace = job == Job.STAFF ? workplace : -1;
        return true;
    }

    /**
     * Moves up to n people into a job (from idle, plain farmers first) or, for a negative n,
     * back to idle. Returns how many moved.
     */
    public static int shift(Territory t, Job job, int n) {
        int moved = 0;
        if (n > 0) {
            List<Resident> idle = new ArrayList<>();
            for (Resident r : t.residents) if (r.job == Job.IDLE && !r.militia && canAssign(t, r, job, -1) == null) idle.add(r);
            idle.sort(Comparator.comparingDouble((Resident r) -> -r.skillAt(job)).thenComparing(Resident::isTalent));
            for (Resident r : idle) {
                if (moved >= n || !assign(t, r, job, -1)) break;
                moved++;
            }
        } else {
            List<Resident> working = withJob(t, job);
            working.sort(Comparator.comparingDouble((Resident r) -> r.skillAt(job)));
            for (Resident r : working) {
                if (moved >= -n) break;
                r.job = Job.IDLE;
                r.workplace = -1;
                moved++;
            }
        }
        return moved;
    }

    /** Jobs can outnumber places after a field is lost; send the extra people home. */
    private static void trimJobs(Territory t) {
        for (Job j : new Job[]{Job.LOGGING, Job.QUARRYING, Job.FARMING, Job.MINING, Job.ADMINISTRATION}) {
            int over = count(t, j) - places(t, j);
            if (over > 0) shift(t, j, -over);
        }
        for (Resident r : t.residents) {
            if (r.job == Job.STAFF) {
                Building b = t.building(r.workplace);
                if (b == null || b.type.staff() != r.occupation) {
                    r.job = Job.IDLE;
                    r.workplace = -1;
                }
            }
        }
    }

    // ================================================================== indexes and bonuses

    /** {political, economic, cultural, military}, each 0-100. */
    public static int[] indexes(Territory t) {
        int[] ix = {5, 5, 0, 10};
        for (Building b : t.buildings) {
            if (!b.complete) continue;
            int[] add = b.type.indexes();
            boolean staffed = b.type.staff() != null && staffOf(t, b) != null;
            for (int i = 0; i < 4; i++) ix[i] += staffed ? add[i] * 2 : add[i];
        }
        for (Resident r : withJob(t, Job.ADMINISTRATION)) {
            ix[0] += r.politics / 6;
            if (r.specialty == Specialty.CAPABLE_ADMINISTRATOR) ix[0] += 2;
        }
        int militia = 0;
        for (Resident r : t.residents) if (r.militia) militia++;
        ix[3] += Math.min(20, militia);
        for (int i = 0; i < 4; i++) ix[i] = Mth.clamp(ix[i], 0, 100);
        return ix;
    }

    /** The token's "production capacity of residents" bonus: Gold 20%, Silver 10%, Black Iron 5%. */
    public static double productionBonus(Territory t) {
        return switch (t.grade) {
            case GOLD -> 1.20;
            case SILVER -> 1.10;
            case BLACK_IRON -> 1.05;
            default -> 1.0;
        };
    }

    /** The token's crop bonus: Gold +50%, Silver +30%, Black Iron +15%. */
    public static double cropBonus(Territory t) {
        return switch (t.grade) {
            case GOLD -> 1.5;
            case SILVER -> 1.3;
            case BLACK_IRON -> 1.15;
            default -> 1.0;
        };
    }

    /** The token's immigration bonus: Gold +50%, Silver +30%, Black Iron +15%. */
    public static double immigrationBonus(Territory t) {
        return cropBonus(t);
    }

    /** Immigrants per day, as on the novel's panel: 10 x (1 + bonus), scaled by People's Thoughts. */
    public static double immigrationRate(Territory t) {
        return 10.0 * immigrationBonus(t) * Mth.clamp(t.peopleThoughts / 70.0, 0.3, 1.4);
    }

    public static long foodUse(Territory t) {
        long n = 0;
        for (Resident r : t.residents) n += r.militia ? 2 : 1;
        return n;
    }

    /** Wood, stone and iron ore made per day by the current workers. */
    public static long[] dailyOutput(Territory t) {
        double[] sum = new double[3];
        for (Resident r : t.residents) {
            double[] q = quota(t, r);
            for (int k = 0; k < 3; k++) sum[k] += q[k];
        }
        return new long[]{Math.round(sum[0]), Math.round(sum[1]), Math.round(sum[2])};
    }

    /** A resident's day of work: {wood, stone, iron ore}. */
    public static double[] quota(Territory t, Resident r) {
        double[] q = new double[3];
        if (r.militia) return q;
        double bonus = productionBonus(t);
        switch (r.job) {
            case LOGGING -> q[0] = 10 * r.skillAt(Job.LOGGING) * bonus;
            case QUARRYING -> q[1] = 5 * r.skillAt(Job.QUARRYING) * bonus;
            case MINING -> q[2] = 2 * r.skillAt(Job.MINING) * bonus;
            default -> { }
        }
        return q;
    }

    public static long dailyTax(Territory t) {
        int econ = indexes(t)[1];
        return Math.round(t.residents.size() * (1.0 + econ / 10.0));
    }

    // ================================================================== the day

    /** Simulates any novel days that have passed since the territory was last updated. */
    public static void catchUp(ServerLevel level, Territory t) {
        long today = GaiaCalendar.day(level.getServer().overworld().getGameTime());
        if (t.lastDay < 0) {
            t.lastDay = today;
            return;
        }
        int days = 0;
        while (t.lastDay < today && days < 30) {
            t.lastDay++;
            day(level, t, t.lastDay);
            days++;
        }
        if (t.lastDay < today) t.lastDay = today;
    }

    /** One novel day in the life of the village. */
    public static void day(ServerLevel level, Territory t, long day) {
        RandomSource rnd = level.getRandom();
        trimJobs(t);
        // Work: what residents carried in by hand counts towards their day; the rest comes in now.
        double[] owed = {0, 0, 0};
        for (Resident r : t.residents) {
            if (r.entity == null) Haul.deposit(t, r);
            double[] q = quota(t, r);
            for (int k = 0; k < 3; k++) owed[k] += Math.max(0, q[k] - r.gathered[k]);
            java.util.Arrays.fill(r.gathered, 0);
        }
        t.wood += Math.round(owed[0]);
        t.stone += Math.round(owed[1]);
        t.ironOre += Math.round(owed[2]);
        // The workshops' day.
        Goods.produce(t);
        farm(t, day);
        updateCrops(level, t, day);
        // Food
        long need = foodUse(t);
        boolean hungry = t.food < need;
        t.food = Math.max(0, t.food - need);
        t.hungryDays = hungry ? t.hungryDays + 1 : 0;
        // Taxes
        t.treasury += dailyTax(t);
        // People's Thoughts drift towards what the village offers.
        int target = 60;
        for (Building b : t.buildings) if (b.complete) target += b.type.morale();
        if (hungry) target -= 30;
        if (t.residents.size() > housing(t)) target -= 10;
        target = Mth.clamp(target, 0, 100);
        t.peopleThoughts += Integer.signum(target - t.peopleThoughts) * Math.min(3, Math.abs(target - t.peopleThoughts));
        if (hungry) t.peopleThoughts = Math.max(0, t.peopleThoughts - 5);
        int secTarget = 50 + indexes(t)[3] / 4;
        for (Building b : t.buildings) if (b.complete) secTarget += b.type.security();
        secTarget = Mth.clamp(secTarget, 0, 100);
        t.security += Integer.signum(secTarget - t.security) * Math.min(3, Math.abs(secTarget - t.security));
        // People leave a hungry or unhappy village.
        if ((t.hungryDays >= 2 || t.peopleThoughts < 30) && t.residents.size() > 5) {
            int leave = 1 + rnd.nextInt(Math.max(1, t.residents.size() / 20));
            for (int i = 0; i < leave && t.residents.size() > 5; i++) {
                Resident r = t.residents.get(rnd.nextInt(t.residents.size()));
                if (r.occupation == Occupation.CIVIL_SERVANT && r.grade >= 0) continue;
                t.residents.remove(r);
            }
        }
        // Immigration, into free housing up to the level's limit.
        int room = capacity(t) - t.residents.size();
        t.immigrantCarry += immigrationRate(t);
        int arrivals = (int) Math.floor(t.immigrantCarry);
        t.immigrantCarry -= arrivals;
        arrivals = Math.min(arrivals, Math.max(0, room));
        double specialBoost = 1.0 + 0.2 * t.grade.ordinal();
        for (int i = 0; i < arrivals; i++) add(t, Resident.random(rnd, Resident.randomOccupation(rnd, specialBoost), t.grade.ordinal()));
        if (room <= 0) t.immigrantCarry = 0;
        t.population = t.residents.size();
        ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (owner != null) {
            if (arrivals > 0) Notify.player(owner, Component.translatable("realmlord.village.immigrants", arrivals, t.name));
            if (hungry) Notify.player(owner, Component.translatable("realmlord.village.hungry", t.name));
        }
    }

    // ================================================================== farming

    private static void farm(Territory t, long day) {
        int month = GaiaCalendar.month(day);
        List<Resident> farmers = withJob(t, Job.FARMING);
        double skill = 0;
        for (Resident r : farmers) skill += r.skillAt(Job.FARMING);
        List<Building> fields = complete(t, BuildingType.FARMLAND);
        if (fields.isEmpty()) return;
        double perField = skill / fields.size();
        for (Building f : fields) {
            if (!f.sown) {
                if (perField > 0) {
                    f.sown = true;
                    f.sowDay = day;
                    f.sowYield = GaiaCalendar.sowingYield(month);
                }
            } else if (day - f.sowDay >= GaiaCalendar.GROWING_MONTHS * GaiaCalendar.daysPerMonth()) {
                double work = Math.min(1.0, perField / BuildingType.FARMLAND.fieldPlaces());
                long food = Math.round(FARMLAND_MU * FOOD_PER_MU * f.sowYield * cropBonus(t) * Math.max(0.2, work) * Math.max(1.0, perField / BuildingType.FARMLAND.fieldPlaces()));
                t.food += food;
                f.sown = false;
            }
        }
    }

    /** Shows each field's crops at their current growth. */
    public static void updateCrops(ServerLevel level, Territory t, long day) {
        for (Building f : complete(t, BuildingType.FARMLAND)) {
            if (!level.isLoaded(f.center())) continue;
            int age = f.sown ? (int) Math.round(growth(f, day) * 7) : 0;
            for (int x = 0; x < f.sizeX(); x++)
                for (int z = 0; z < f.sizeZ(); z++) {
                    BlockPos p = f.origin.offset(x, 0, z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop && s.getValue(net.minecraft.world.level.block.CropBlock.AGE) != age) {
                        level.setBlock(p, crop.getStateForAge(age), Block.UPDATE_CLIENTS);
                    }
                }
        }
    }

    /** 0-1 growth of a sown field, for showing its crops. */
    public static double growth(Building f, long day) {
        if (!f.sown) return 0;
        return Mth.clamp((day - f.sowDay) / (double) (GaiaCalendar.GROWING_MONTHS * GaiaCalendar.daysPerMonth()), 0, 1);
    }

    // ================================================================== construction

    /** Why this building cannot be started here, or null if it can. */
    public static String checkPlacement(ServerLevel level, Territory t, BuildingType type, BlockPos origin, int rotation) {
        if (!type.placed()) return "realmlord.build.err.not_placed";
        int sx = rotation % 2 == 0 ? type.width : type.depth;
        int sz = rotation % 2 == 0 ? type.depth : type.width;
        int[][] corners = {{0, 0}, {sx - 1, 0}, {0, sz - 1}, {sx - 1, sz - 1}};
        for (int[] c : corners) {
            if (!t.contains(level.dimension(), origin.offset(c[0], 0, c[1]))) return "realmlord.build.err.border";
        }
        BlockPos m = t.center;
        if (origin.getX() - 1 < m.getX() + MANOR_YARD && m.getX() - MANOR_YARD < origin.getX() + sx + 1
                && origin.getZ() - 1 < m.getZ() + MANOR_YARD && m.getZ() - MANOR_YARD < origin.getZ() + sz + 1) {
            return "realmlord.build.err.manor";
        }
        for (Building b : t.buildings) if (b.type.placed() && b.overlaps(origin.getX(), origin.getZ(), sx, sz, 1)) return "realmlord.build.err.overlap";
        // Ground: settlers fill in shallow water, ponds and marsh, but not deep water; slopes are limited.
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int x = 0; x < sx; x += Math.max(1, Math.min(2, sx - 1 - x))) {
            for (int z = 0; z < sz; z += Math.max(1, Math.min(2, sz - 1 - z))) {
                int y = groundAt(level, origin.getX() + x, origin.getZ() + z);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
                if (waterDepth(level, origin.getX() + x, y - 1, origin.getZ() + z) > MAX_FILL_DEPTH) return "realmlord.build.err.water";
                if (z == sz - 1) break;
            }
            if (x == sx - 1) break;
        }
        // Builders clear and fill up to this much themselves.
        if (hi - lo > MAX_SLOPE) return "realmlord.build.err.slope";
        return null;
    }

    /** The greatest rise across a site that builders will cut and fill to level. */
    public static final int MAX_SLOPE = 10;

    /** The deepest water settlers will fill in to build on. */
    public static final int MAX_FILL_DEPTH = 3;

    /** How many blocks of water stand at a column, counting down from a block. */
    public static int waterDepth(ServerLevel level, int x, int top, int z) {
        int d = 0;
        while (d <= 16 && !level.getFluidState(new BlockPos(x, top - d, z)).isEmpty()) d++;
        return d;
    }

    /** What is missing to construct a building (resources, blueprint, specialist), or null. */
    public static String checkCost(ServerPlayer p, Territory t, BuildingType type, boolean free) {
        if (type.grade > t.level.villageGrade() && t.level.villageGrade() > 0) return "realmlord.build.err.grade";
        if (free) return null;
        if (t.wood < type.wood || t.stone < type.stone) return "realmlord.build.err.resources";
        if (type.blueprint && !knowsBlueprint(p, t, type)) return "realmlord.build.err.blueprint";
        if (type.specialist != null && !hasSpecialist(t, type.specialist, type.specialistRank)) return "realmlord.build.err.specialist";
        return null;
    }

    public static boolean hasSpecialist(Territory t, Occupation o, int rank) {
        for (Resident r : t.residents) if (r.occupation == o && r.rank >= rank) return true;
        return false;
    }

    private static int findBlueprint(ServerPlayer p, BuildingType type) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof com.realmlord.item.BlueprintItem && type.id().equals(Blueprints.buildingOf(s))) return i;
        }
        return -1;
    }

    public static long workNeeded(BuildingType type) {
        return (long) type.hours * RealmConfig.ticksPerNovelHour() * NOMINAL_BUILDERS;
    }

    /**
     * Starts a building: pays for it, levels the ground and sets out the site. The fence ignores
     * the position and rings the territory border instead.
     */
    public static String start(ServerPlayer p, Territory t, BuildingType type, BlockPos origin, int rotation) {
        return start(p, t, type, origin, rotation, null);
    }

    /** As {@link #start(ServerPlayer, Territory, BuildingType, BlockPos, int)}; gates (degrees) for the fence. */
    public static String start(ServerPlayer p, Territory t, BuildingType type, BlockPos origin, int rotation, int[] gates) {
        ServerLevel level = p.serverLevel();
        boolean free = t.freeBuilds.contains(type);
        String err = checkCost(p, t, type, free);
        if (err != null) return err;
        if (type == BuildingType.SIMPLE_FENCE) {
            for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) return "realmlord.build.err.fence_exists";
            origin = t.center;
        } else {
            err = checkPlacement(level, t, type, origin, rotation);
            if (err != null) return err;
        }
        if (free) {
            t.freeBuilds.remove(type);
        } else {
            t.wood -= type.wood;
            t.stone -= type.stone;
            // A blueprint carried but not yet learned is learned now; a learned one is never used up.
            if (type.blueprint && !t.blueprints.contains(type.id()) && findBlueprint(p, type) >= 0) {
                p.getInventory().getItem(findBlueprint(p, type)).shrink(1);
                t.blueprints.add(type.id());
            }
        }
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = type;
        b.rotation = rotation & 3;
        b.free = free;
        b.workNeeded = Math.max(1, workNeeded(type));
        b.w = type.width;
        b.d = type.depth;
        b.lastWork = level.getGameTime();
        if (type.placed()) {
            // The builders clear and level the ground themselves; nothing is put down yet.
            int y = groundLevel(level, origin, b.sizeX(), b.sizeZ());
            b.origin = new BlockPos(origin.getX(), y, origin.getZ());
            b.stage = Building.CLEAR;
        } else {
            b.origin = t.center;
            b.stage = Building.BUILD;
            b.fenceRadius = t.radius() - 2;
            b.gates = gates == null ? new int[]{0, 90, 180, 270} : cleanGates(gates);
        }
        t.buildings.add(b);
        level.playSound(null, b.center(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        Notify.player(p, Component.translatable("realmlord.build.started", Component.translatable(type.key()), type.hours,
                count(t, Job.CONSTRUCTION)));
        return null;
    }

    /** The most common ground height across a footprint. */
    public static int groundLevel(ServerLevel level, BlockPos origin, int sx, int sz) {
        java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
        for (int x = 0; x < sx; x += Math.max(1, sx / 4))
            for (int z = 0; z < sz; z += Math.max(1, sz / 4))
                freq.merge(groundAt(level, origin.getX() + x, origin.getZ() + z), 1, Integer::sum);
        return freq.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(origin.getY());
    }

    /**
     * The height of the first free block above the real ground (or a water surface), looking
     * through trees, mangrove roots, giant mushrooms, vines and plants, which builders clear away.
     */
    public static int groundAt(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int k = 0; k < 48; k++) {
            BlockState b = level.getBlockState(new BlockPos(x, y - 1, z));
            if (!growth(b)) break;
            y--;
        }
        return y;
    }

    /** Things that grow on the ground rather than being it. */
    public static boolean growth(BlockState b) {
        if (b.isAir()) return true;
        if (!b.getFluidState().isEmpty()) return false;
        return b.is(net.minecraft.tags.BlockTags.LOGS) || b.is(net.minecraft.tags.BlockTags.LEAVES)
                || b.is(Blocks.MANGROVE_ROOTS) || b.is(Blocks.VINE) || b.is(Blocks.MOSS_CARPET) || b.is(Blocks.BAMBOO)
                || b.is(Blocks.BROWN_MUSHROOM_BLOCK) || b.is(Blocks.RED_MUSHROOM_BLOCK) || b.is(Blocks.MUSHROOM_STEM)
                || b.is(Blocks.CACTUS) || b.is(Blocks.SUGAR_CANE) || b.is(Blocks.SNOW) || b.canBeReplaced();
    }

    /**
     * Construction for one second: all builders work on the oldest unfinished building, the
     * design rising block by block as the work is done.
     */
    public static void construct(ServerLevel level, Territory t, int ticks) {
        allocateBuilders(t);
        for (Building site : new ArrayList<>(t.buildings)) {
            if (site.complete) continue;
            double work = 0;
            for (Resident r : t.residents) {
                if (r.job == Job.CONSTRUCTION && !r.militia && r.workplace == site.id) work += r.skillAt(Job.CONSTRUCTION);
            }
            if (work > 0) work(level, t, site, work, ticks);
        }
    }

    /**
     * Shares the builders out among the sites. A site the Lord gave a crew gets that many (in
     * order, while builders last); the rest are split evenly over the other sites, the oldest
     * getting any left over. Every builder adds work, so more builders on a site finish it
     * sooner, and with enough builders several buildings rise at once.
     */
    public static void allocateBuilders(Territory t) {
        List<Building> sites = new ArrayList<>();
        for (Building b : t.buildings) if (!b.complete) sites.add(b);
        List<Resident> builders = withJob(t, Job.CONSTRUCTION);
        builders.removeIf(r -> r.militia);
        builders.sort(Comparator.comparingInt(r -> r.id));
        for (Resident r : builders) r.workplace = -1;
        if (sites.isEmpty() || builders.isEmpty()) return;
        int next = 0;
        List<Building> shared = new ArrayList<>();
        for (Building b : sites) {
            if (b.crew < 0) {
                shared.add(b);
                continue;
            }
            for (int i = 0; i < b.crew && next < builders.size(); i++) builders.get(next++).workplace = b.id;
        }
        if (shared.isEmpty()) {
            // Builders nobody asked for help at the oldest site.
            while (next < builders.size()) builders.get(next++).workplace = sites.get(0).id;
            return;
        }
        int left = builders.size() - next, each = left / shared.size(), extra = left % shared.size();
        for (int s = 0; s < shared.size(); s++) {
            int n = each + (s < extra ? 1 : 0);
            for (int i = 0; i < n; i++) builders.get(next++).workplace = shared.get(s).id;
        }
    }

    /** How many builders are working a site now. */
    public static int buildersAt(Territory t, Building b) {
        int n = 0;
        for (Resident r : t.residents) if (r.job == Job.CONSTRUCTION && !r.militia && r.workplace == b.id) n++;
        return n;
    }

    private static void work(ServerLevel level, Territory t, Building site, double work, int ticks) {
        if (!site.demolishing) site.progress = Math.min(site.workNeeded, site.progress + Math.round(work * ticks));
        // Builders on the site do the work by hand; otherwise the village gets on with it.
        boolean byHand = buildersNear(level, t, site) && level.getGameTime() - site.lastWork < Sites.STALL_TICKS;
        if (!byHand) {
            int budget = site.demolishing ? (int) Math.max(1, Math.round(work * ticks / 10.0)) : 400;
            Sites.work(level, t, site, budget);
        }
        if (!site.demolishing && site.stage == Building.BUILD && t.buildings.contains(site)) {
            int size = BuildingDesigns.size(level, t, site);
            if (site.progress >= site.workNeeded && size >= 0 && site.placed >= size && site.verified) finish(level, t, site);
        }
    }

    /** True if a builder is walking about near a site. */
    public static boolean buildersNear(ServerLevel level, Territory t, Building site) {
        BlockPos c = site.type.placed() ? site.center() : t.center;
        double reach = site.type.placed() ? 48 : t.radius() + 16;
        for (Resident r : t.residents) {
            if (r.job != Job.CONSTRUCTION || r.militia || r.entity == null || r.workplace != site.id) continue;
            net.minecraft.world.entity.Entity e = level.getEntity(r.entity);
            if (e != null && e.isAlive() && e.distanceToSqr(c.getX() + 0.5, e.getY(), c.getZ() + 0.5) < reach * reach) return true;
        }
        return false;
    }

    /** Works a site with the village's own hands, up to its current progress. */
    public static void raise(ServerLevel level, Territory t, Building b) {
        Sites.work(level, t, b, 400);
    }

    private static void finish(ServerLevel level, Territory t, Building b) {
        b.complete = true;
        BuildingDesigns.forget(b);
        b.progress = b.workNeeded;
        level.playSound(null, b.center(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);
        ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (owner != null) {
            Notify.player(owner, Component.translatable("realmlord.build.done", Component.translatable(b.type.key()), t.name));
            // The specialist the building needed moves in to run it.
            if (b.type.staff() != null && staffOf(t, b) == null) {
                for (Resident r : t.residents) {
                    if (r.occupation == b.type.staff() && r.rank >= b.type.specialistRank && !isStaffing(t, r) && !r.militia) {
                        assign(t, r, Job.STAFF, b.id);
                        Notify.player(owner, Component.translatable("realmlord.build.staffed", r.name, Component.translatable(b.type.key())));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Takes a building down. Builders dismantle it from the top, carrying back three quarters of
     * the wood and stone that went into what stood; for a site, three quarters of what was never
     * used comes back at once. A site with nothing built yet is simply cleared away.
     */
    public static void demolish(ServerLevel level, Territory t, Building b) {
        if (b.demolishing) return;
        int size = BuildingDesigns.size(level, t, b);
        double built = b.complete ? 1.0 : size > 0 ? Math.min(1.0, b.placed / (double) size) : 0.0;
        double share = b.free ? 0 : Sites.SALVAGE;
        t.wood += Math.round(b.type.wood * share * (1 - built));
        t.stone += Math.round(b.type.stone * share * (1 - built));
        for (Resident r : t.residents) if (r.workplace == b.id && r.job != Job.CONSTRUCTION) {
            r.job = Job.IDLE;
            r.workplace = -1;
        }
        if (b.placed <= 0 || size < 0 && !b.complete) {
            t.buildings.remove(b);
            BuildingDesigns.forget(b);
            return;
        }
        if (b.complete && size >= 0) b.placed = size;
        b.salvageWood = (int) Math.round(b.type.wood * share * built);
        b.salvageStone = (int) Math.round(b.type.stone * share * built);
        b.demolishFrom = b.placed;
        b.demolishing = true;
        b.complete = false;
        b.lastWork = level.getGameTime();
        // Taken down before anything newer is built.
        t.buildings.remove(b);
        int at = 0;
        while (at < t.buildings.size() && t.buildings.get(at).complete) at++;
        t.buildings.add(at, b);
    }

    /** Gate bearings, each 0-359, without near-duplicates, at most 16. */
    public static int[] cleanGates(int[] gates) {
        List<Integer> out = new ArrayList<>();
        for (int g : gates) {
            int d = Math.floorMod(g, 360);
            boolean close = false;
            for (int o : out) if (Math.abs(((d - o) % 360 + 540) % 360 - 180) < 6) close = true;
            if (!close && out.size() < 16) out.add(d);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Moves the fence's gates. Posts already up are changed over at once. */
    public static boolean setGates(ServerLevel level, Territory t, int[] gates) {
        Building fence = null;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) fence = b;
        if (fence == null) return false;
        fence.gates = cleanGates(gates);
        BuildingDesigns.forget(fence);
        List<BuildingDesigns.Placement> design = BuildingDesigns.design(level, t, fence);
        if (design == null) return true;
        int upTo = fence.complete ? design.size() : Math.min(fence.placed, design.size());
        for (int i = 0; i < upTo; i++) {
            BuildingDesigns.Placement p = design.get(i);
            if (!level.isLoaded(p.pos())) continue;
            BlockState now = level.getBlockState(p.pos());
            boolean fenceBit = now.isAir() || now.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
                    || now.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock;
            if (fenceBit && now != p.state()) level.setBlock(p.pos(), p.state(), Block.UPDATE_ALL);
        }
        return true;
    }

    // ================================================================== upgrading

    /** What the next upgrade still needs, as text keys with arguments; empty when ready. */
    public static List<Component> upgradeMissing(Territory t, Profile owner) {
        List<Component> out = new ArrayList<>();
        TerritoryLevel next = t.level.next();
        if (next == null) return out;
        if (t.level.villageGrade() == 0 || next.villageGrade() == 0) {
            out.add(Component.translatable("realmlord.upgrade.town_later"));
        }
        if (t.residents.size() < next.population) out.add(Component.translatable("realmlord.upgrade.need_pop", t.residents.size(), next.population));
        if (owner != null && !owner.getTitle().atLeast(next.title)) out.add(Component.translatable("realmlord.upgrade.need_title", Component.translatable(next.title.key())));
        for (BuildingType bt : BuildingType.values()) {
            if (bt.requiredFor != t.level.villageGrade() || bt.requiredFor == 0) continue;
            int have = countComplete(t, bt);
            if (have < bt.requiredCount) out.add(Component.translatable("realmlord.upgrade.need_building", Component.translatable(bt.key()), have, bt.requiredCount));
        }
        if (next.minIndex > 0) {
            int[] ix = indexes(t);
            for (int i : ix) if (i < next.minIndex) {
                out.add(Component.translatable("realmlord.upgrade.need_index", next.minIndex));
                break;
            }
        }
        return out;
    }

    /**
     * Upgrades the village when every condition is met. As in the novel: merit by the world's
     * order (1,100 for the first), the Lord's Manor grows, and the Lord picks one of three random
     * buildings of the new grade to be granted.
     */
    public static boolean upgrade(ServerPlayer p, Territory t) {
        if (!upgradeMissing(t, Profiles.get(p)).isEmpty()) return false;
        TerritoryLevel next = t.level.next();
        TerritoryData data = TerritoryData.get(p.server);
        t.level = next;
        int place = data.reach(next);
        int merit = Founding.foundingMerit(place);
        Component where = Component.translatable(next.kindKey());
        if (place <= 10) {
            Notify.world(p.server, Component.translatable("realmlord.upgrade.world", p.getDisplayName(), place, where, next.grade, merit));
        }
        Profiles.grantMerit(p, merit, Component.translatable("realmlord.reason.upgrade"));
        t.freeChoices.clear();
        List<BuildingType> pool = new ArrayList<>();
        for (BuildingType bt : BuildingType.ofGrade(next.villageGrade())) if (bt.placed()) pool.add(bt);
        RandomSource r = p.getRandom();
        while (t.freeChoices.size() < 3 && !pool.isEmpty()) t.freeChoices.add(pool.remove(r.nextInt(pool.size())));
        data.touch();
        p.serverLevel().playSound(null, t.center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        Notify.player(p, Component.translatable("realmlord.upgrade.done", t.name, where, next.grade));
        return true;
    }

    public static boolean chooseFree(Territory t, BuildingType type) {
        if (!t.freeChoices.contains(type)) return false;
        t.freeChoices.clear();
        t.freeBuilds.add(type);
        return true;
    }

    // ================================================================== market

    public static boolean buy(ServerPlayer p, Territory t, int resource, long units) {
        if (resource < 0 || resource > 3 || units <= 0) return false;
        if (!Coins.pay(p, Market.buyCost(resource, units))) return false;
        add(t, resource, units);
        return true;
    }

    public static boolean sell(ServerPlayer p, Territory t, int resource, long units) {
        if (resource < 0 || resource > 3 || units <= 0 || stock(t, resource) < units) return false;
        add(t, resource, -units);
        Coins.give(p, Market.sellIncome(resource, units));
        return true;
    }

    /** Buys goods for the village at the market. */
    public static boolean buyGood(ServerPlayer p, Territory t, Goods g, long units) {
        if (g == null || units <= 0 || !Coins.pay(p, g.buyCost(units))) return false;
        g.add(t, units);
        return true;
    }

    /** Sells the village's goods at the market; the coins go to the Lord, less the 20% tax. */
    public static boolean sellGood(ServerPlayer p, Territory t, Goods g, long units) {
        if (g == null || units <= 0 || g.stock(t) < units) return false;
        g.add(t, -units);
        Coins.give(p, g.sellIncome(units));
        return true;
    }

    /**
     * The Special Items Trading Platform: graded gear and building blueprints from the Lord's
     * pack, bought by the market (gear by grade, blueprints at half their price). Returns copper.
     */
    public static long sellSpecial(ServerPlayer p) {
        long copper = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = inv.getItem(i);
            long each = specialPrice(s);
            if (each <= 0) continue;
            copper += Math.round(each * s.getCount() * (1.0 - Market.SALES_TAX));
            inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        Coins.give(p, copper);
        return copper;
    }

    /** What the market pays for a special item before tax (copper), or 0. */
    public static long specialPrice(net.minecraft.world.item.ItemStack s) {
        if (s.isEmpty()) return 0;
        var grade = com.realmlord.loot.Gear.gradeOf(s);
        if (grade != null) return switch (grade) {
            case BRONZE -> 2_000;          // 20 silver
            case BLACK_IRON -> 10_000;     // 1 gold
            case SILVER -> 50_000;         // 5 gold
            case GOLD -> 200_000;          // 20 gold
        };
        String building = com.realmlord.loot.Blueprints.buildingOf(s);
        if (building != null && !building.isEmpty()) {
            BuildingType bt = BuildingType.byId(building);
            if (bt != null) return bt.blueprintPrice() / 2;
        }
        return 0;
    }

    /** Buys a blueprint: the item, to learn by right-clicking it (or to keep, give or sell). */
    public static boolean buyBlueprint(ServerPlayer p, BuildingType type) {
        if (!type.blueprint || !Coins.pay(p, type.blueprintPrice())) return false;
        com.realmlord.item.ResourcesBagItem.give(p, Blueprints.of(type.id()), 1);
        return true;
    }

    /** True if the village has learned a building's blueprint, or the Lord carries one. */
    public static boolean knowsBlueprint(ServerPlayer p, Territory t, BuildingType type) {
        return !type.blueprint || t.blueprints.contains(type.id()) || p.getAbilities().instabuild || findBlueprint(p, type) >= 0;
    }

    /**
     * Teaches the Lord's village a blueprint: the village's builders keep the plan, so every
     * later building of that kind needs no blueprint. Returns a message key.
     */
    public static String learnBlueprint(ServerPlayer p, String building) {
        BuildingType type = BuildingType.byId(building);
        if (type == null || !type.blueprint) return "realmlord.blueprint.learn.unknown";
        int id = com.realmlord.profile.Profiles.get(p).getTerritoryId();
        com.realmlord.territory.TerritoryData data = com.realmlord.territory.TerritoryData.get(p.server);
        Territory t = id >= 0 ? data.byId(id) : null;
        if (t == null || !p.getUUID().equals(t.owner)) return "realmlord.blueprint.learn.no_village";
        if (!t.blueprints.add(type.id())) return "realmlord.blueprint.learn.known";
        data.touch();
        return null;
    }

    public static long stock(Territory t, int resource) {
        return switch (resource) {
            case Market.FOOD -> t.food;
            case Market.WOOD -> t.wood;
            case Market.STONE -> t.stone;
            default -> t.ironOre;
        };
    }

    private static void add(Territory t, int resource, long units) {
        switch (resource) {
            case Market.FOOD -> t.food += units;
            case Market.WOOD -> t.wood += units;
            case Market.STONE -> t.stone += units;
            default -> t.ironOre += units;
        }
    }

    // ================================================================== trading

    /**
     * Trades once with a resident. A visitor's money goes into the village treasury; a Lord
     * buying from his own people simply spends it. Returns an error key, or null.
     */
    public static String trade(ServerPlayer p, Territory t, Resident r, int index) {
        List<Trades.Offer> offers = Trades.of(r.occupation);
        if (index < 0 || index >= offers.size()) return "realmlord.trade.none";
        Trades.Offer o = offers.get(index);
        boolean owner = p.getUUID().equals(t.owner);
        if (o.sell()) {
            if (o.needsWorkshop() && !hasWorkshop(t, r)) return "realmlord.trade.no_workshop";
            if (!Coins.pay(p, o.price())) return "realmlord.market.cannot_afford";
            if (!owner) t.treasury += o.price();
            com.realmlord.item.ResourcesBagItem.give(p, o.stack(), o.count());
            p.displayClientMessage(Component.translatable("realmlord.trade.bought", o.count(), o.stack().getHoverName(), Coins.format(o.price())), true);
        } else {
            if (p.getInventory().countItem(o.item()) < o.count()) return "realmlord.trade.need_items";
            int left = o.count();
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(o.item())) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
            Coins.give(p, o.price());
            p.displayClientMessage(Component.translatable("realmlord.trade.sold", o.count(), o.stack().getHoverName(), Coins.format(o.price())), true);
        }
        return null;
    }

    // ================================================================== ticking

    /** Called every second on the server for every territory. */
    public static void tick(MinecraftServer server, Territory t) {
        ServerLevel level = server.getLevel(t.dimension);
        if (level == null) return;
        if (!t.settled) settle(t, level.getRandom(), Math.max(0, t.population - 10));
        catchUp(level, t);
        construct(level, t, 20);
        Residents.sync(level, t);
    }

    /** Resident's grade name key. */
    public static String gradeKey(int grade) {
        return grade < 0 ? "realmlord.grade.common" : TokenGrade.byId(grade).key();
    }
}
