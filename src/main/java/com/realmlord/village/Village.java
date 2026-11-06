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
    /** Clear blocks kept between buildings: room for a three-block path. */
    public static final int BUILDING_GAP = 3;
    /**
     * Time sped up for testing (1 = normal): the sun runs this many times as fast and the
     * village's work keeps pace. Set with /realm speed or the manor's Speed button in creative.
     */
    public static int speed = 1;
    /** Companions who may walk with the Lord at once. */
    public static final int MAX_COMPANIONS = 3;
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
        // Those freed from the bandits move in while there are beds; the rest wait for more.
        for (int i = 0; i < extra; i++) {
            if (openBeds(t) > 0) add(t, Resident.random(r, Resident.randomOccupation(r, 1.0), g));
            else t.waitingSettlers++;
        }
    }

    /** Beds nobody sleeps in yet (within the level's limit on people). */
    public static int openBeds(Territory t) {
        return Math.max(0, capacity(t) - t.residents.size());
    }

    /** Settlers waiting for beds move in as soon as there are some. Returns how many did. */
    public static int admitWaiting(ServerLevel level, Territory t) {
        int n = Math.min(t.waitingSettlers, openBeds(t));
        if (n <= 0) return 0;
        RandomSource r = level.getRandom();
        for (int i = 0; i < n; i++) add(t, Resident.random(r, Resident.randomOccupation(r, 1.0), t.grade.ordinal()));
        t.waitingSettlers -= n;
        ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        tell(level, t, Component.translatable("realmlord.village.waiting_joined", n, t.name, t.waitingSettlers));
        return n;
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

    // ================================================================== hidden buildings

    /**
     * Hidden buildings, as the novel's Mazu Temple: not in any list until a village meets their
     * condition. The Mazu Temple, to the goddess of the sea, is revealed to a village of Grade 2
     * or more that lies by the sea.
     */
    public static void revealHidden(ServerLevel level, Territory t) {
        if (t.revealed.contains(BuildingType.MAZU_TEMPLE.id())) return;
        if (t.level.ordinal() < TerritoryLevel.VILLAGE_2.ordinal() || !coastal(level, t)) return;
        t.revealed.add(BuildingType.MAZU_TEMPLE.id());
        tell(level, t, Component.translatable("realmlord.hidden.revealed", Component.translatable(BuildingType.MAZU_TEMPLE.key()), t.name));
    }

    /** True if the sea or a beach lies within the territory or just beyond it. */
    public static boolean coastal(ServerLevel level, Territory t) {
        for (int ring : new int[]{t.radius() / 2, t.radius(), t.radius() + 16})
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                BlockPos p = new BlockPos(t.center.getX() + (int) (Math.cos(a) * ring), 62, t.center.getZ() + (int) (Math.sin(a) * ring));
                if (!level.isLoaded(p)) continue;
                var b = level.getBiome(p);
                if (b.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || b.is(net.minecraft.tags.BiomeTags.IS_BEACH)) return true;
            }
        return false;
    }

    // ================================================================== the militia

    /** How many may serve in the militia: ten for a Military Camp, fifty for each Barracks, more under a good commander. */
    public static int militiaCap(Territory t) {
        int n = 0;
        for (Building b : t.buildings) {
            if (!b.complete) continue;
            if (b.type == BuildingType.MILITARY_CAMP) n += 10;
            if (b.type == BuildingType.BARRACKS) n += 50;
        }
        return n == 0 ? 0 : n + Office.power(t, Office.MILITARY) / 5;
    }

    public static int militiaCount(Territory t) {
        int n = 0;
        for (Resident r : t.residents) if (r.militia) n++;
        return n;
    }

    /** Enlists a resident in the militia, or stands them down. Returns an error key or null. */
    public static String enlist(Territory t, Resident r, boolean join) {
        if (!join) {
            r.militia = false;
            return null;
        }
        if (r.militia) return null;
        if (r.office >= 0) return "realmlord.militia.err.office";
        if (militiaCap(t) == 0) return "realmlord.militia.err.no_camp";
        if (militiaCount(t) >= militiaCap(t)) return "realmlord.militia.err.full";
        r.militia = true;
        r.follows = false;
        r.job = Job.IDLE;
        r.workplace = -1;
        return null;
    }

    /** The militia's daily drill: Force grows, faster under a good commander, up to what the person can reach. */
    private static void drill(Territory t, RandomSource rnd) {
        double chance = 0.25 * Office.boost(t, Office.MILITARY, 200);
        for (Resident r : t.residents) {
            if (!r.militia) continue;
            int cap = r.isFigure() ? 100 : r.grade >= 0 ? 70 : 45;
            if (r.force < cap && rnd.nextDouble() < chance) r.force++;
            if (r.occupation == Occupation.OFFICER && r.command < cap && rnd.nextDouble() < chance / 2) r.command++;
            // A day at the drill is worth something to every soldier, more under a good commander.
            r.gainSoldierXp(Math.round(15 * Office.boost(t, Office.MILITARY, 200)));
        }
    }

    /** Hunters' day out in the wilds: meat and hides. */
    private static void hunt(Territory t) {
        double meat = 0, leather = 0;
        for (Resident r : t.residents) {
            if (r.job != Job.HUNTING || r.militia) continue;
            double skill = r.occupation == Occupation.HUNTER ? 1.5 + 0.2 * r.rank : 1.0;
            meat += 6 * skill;
            leather += 1.5 * skill;
        }
        double boost = productionBonus(t) * Office.boost(t, Office.AGRICULTURE, 300);
        addFraction(t, Goods.MEAT, meat * boost);
        addFraction(t, Goods.LEATHER, leather * boost);
    }

    private static void addFraction(Territory t, Goods g, double units) {
        double total = t.goodsCarry[g.ordinal()] + units;
        long whole = (long) Math.floor(total);
        t.goodsCarry[g.ordinal()] = total - whole;
        g.add(t, whole);
    }

    /** Beds in finished residential courtyards of any size. */
    public static int courtyardBeds(Territory t) {
        int n = 0;
        for (Building b : t.buildings) if (b.complete) n += b.type.housing();
        return n;
    }

    /** Beds for residents in the Lord's Manor: the refugees' straw beds at Grade 1, none once it is the Lord's residence. */
    public static int manorHousing(Territory t) {
        return t.level == com.realmlord.territory.TerritoryLevel.VILLAGE_1 ? MANOR_HOUSING : 0;
    }

    public static int housing(Territory t) {
        int h = manorHousing(t);
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
        if (job == Job.HUNTING) return Integer.MAX_VALUE;
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
        if (r.office >= 0 && job != Job.IDLE) return "realmlord.job.err.office";
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
            for (Resident r : t.residents) if (r.job == Job.IDLE && !r.militia && r.trainingAt < 0 && canAssign(t, r, job, -1) == null) idle.add(r);
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
        // The offices: politics, economy, culture and the military.
        ix[0] += Office.power(t, Office.STEWARD) / 8 + Office.power(t, Office.TREASURER) / 12;
        ix[1] += Office.power(t, Office.TREASURER) / 10 + Office.power(t, Office.COMMERCE) / 8 + Office.power(t, Office.AGRICULTURE) / 12;
        ix[2] += Office.power(t, Office.RITES) / 5;
        ix[3] += Office.power(t, Office.MILITARY) / 8;
        // Scholars and figures of culture lift the cultural index.
        for (Resident r : t.residents) if (r.occupation == Occupation.SCHOLAR && r.grade >= 0) ix[2] += r.isFigure() ? 4 : 1;
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
        return 10.0 * immigrationBonus(t) * Mth.clamp(t.peopleThoughts / 70.0, 0.3, 1.4) * Office.boost(t, Office.STEWARD, 300);
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
        return Math.round(t.residents.size() * (1.0 + econ / 10.0) * Office.boost(t, Office.TREASURER, 250));
    }

    // ================================================================== the day

    /** Simulates any novel days that have passed since the territory was last updated. */
    public static void catchUp(ServerLevel level, Territory t) {
        long today = GaiaCalendar.today(level.getServer());
        // A jump the village can't have lived through (the clock set back, or far forward by a
        // command, or the change to Minecraft days): start counting again from today.
        if (t.lastDay < 0 || t.lastDay > today || today - t.lastDay > 30) {
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
    /** Yesterday's ledger, by index. */
    public static final int LEDGER_GRAIN = 0, LEDGER_EATEN = 1, LEDGER_WOOD = 2, LEDGER_STONE = 3, LEDGER_ORE = 4,
            LEDGER_TAX = 5, LEDGER_ARRIVED = 6, LEDGER_LEFT = 7, LEDGER_OTHER_FOOD = 8, LEDGER_DAY = 9;
    /** Goods eaten when the grain runs short, in this order (one unit feeds one person a day). */
    public static final Goods[] OTHER_FOODS = {Goods.FLOUR, Goods.MEAT, Goods.FISH};

    public static void day(ServerLevel level, Territory t, long day) {
        RandomSource rnd = level.getRandom();
        trimJobs(t);
        long food0 = t.food, wood0 = t.wood, stone0 = t.stone, ore0 = t.ironOre;
        long[] ledger = new long[10];
        ledger[LEDGER_DAY] = day;
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
        // The workshops' day, the hunt and the militia's drill.
        Goods.produce(t);
        Workshops.make(t, rnd);
        for (String name : Workshops.teach(t, rnd)) tell(level, t, Component.translatable("realmlord.workshop.learned", name, t.name));
        hunt(t);
        drill(t, rnd);
        revealHidden(level, t);
        farm(t, day);
        updateCrops(level, t, day);
        ledger[LEDGER_GRAIN] = t.food - food0;
        ledger[LEDGER_WOOD] = t.wood - wood0;
        ledger[LEDGER_STONE] = t.stone - stone0;
        ledger[LEDGER_ORE] = t.ironOre - ore0;
        // Food: grain first, then flour, meat and fish from the goods shelves.
        long need = foodUse(t);
        long fromGrain = Math.min(t.food, need);
        t.food -= fromGrain;
        long short_ = need - fromGrain;
        for (Goods g : OTHER_FOODS) {
            long take = Math.min(short_, g.stock(t));
            g.add(t, -take);
            short_ -= take;
            ledger[LEDGER_OTHER_FOOD] += take;
        }
        ledger[LEDGER_EATEN] = need - short_;
        boolean hungry = short_ > 0;
        t.hungryDays = hungry ? t.hungryDays + 1 : 0;
        // Taxes
        long tax = dailyTax(t);
        t.treasury += tax;
        ledger[LEDGER_TAX] = tax;
        int before = t.residents.size();
        // People's Thoughts drift towards what the village offers.
        int target = 60 + Office.power(t, Office.STEWARD) / 10 + (Office.holder(t, Office.RITES) != null ? 3 + Office.power(t, Office.RITES) / 20 : 0);
        for (Building b : t.buildings) if (b.complete) target += b.type.morale();
        if (hungry) target -= 30;
        if (t.residents.size() > housing(t)) target -= 10;
        target = Mth.clamp(target, 0, 100);
        t.peopleThoughts += Integer.signum(target - t.peopleThoughts) * Math.min(3, Math.abs(target - t.peopleThoughts));
        if (hungry) t.peopleThoughts = Math.max(0, t.peopleThoughts - 5);
        int secTarget = 50 + indexes(t)[3] / 4 + Office.power(t, Office.MILITARY) / 10;
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
        ledger[LEDGER_LEFT] = before - t.residents.size();
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
        ledger[LEDGER_ARRIVED] = arrivals;
        t.ledger = ledger;
        if (arrivals > 0) tell(level, t, Component.translatable("realmlord.village.immigrants", arrivals, t.name));
        if (ledger[LEDGER_LEFT] > 0) tell(level, t, Component.translatable("realmlord.village.left", ledger[LEDGER_LEFT], t.name));
        if (hungry) tell(level, t, Component.translatable("realmlord.village.hungry", t.name));
    }

    /** Days the stored food lasts at yesterday's rate (grain, flour, meat and fish), or -1 if it is not running down. */
    public static long foodDaysLeft(Territory t) {
        long stock = t.food;
        for (Goods g : OTHER_FOODS) stock += g.stock(t);
        long net = t.ledger[LEDGER_EATEN] - t.ledger[LEDGER_GRAIN];
        long need = foodUse(t);
        // Harvests come in lumps, so judge by what is eaten when no harvest came in.
        long burn = t.ledger[LEDGER_GRAIN] > need * 5 ? need : Math.max(net, 0);
        if (burn <= 0) return -1;
        return stock / burn;
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
                long food = Math.round(FARMLAND_MU * FOOD_PER_MU * f.sowYield * cropBonus(t) * Office.boost(t, Office.AGRICULTURE, 300) * Math.max(0.2, work) * Math.max(1.0, perField / BuildingType.FARMLAND.fieldPlaces()));
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
        if (!level.dimension().equals(t.dimension)) return "realmlord.build.err.border";
        boolean defence = defence(type);
        for (int[] c : corners) {
            BlockPos at = origin.offset(c[0], 0, c[1]);
            // Arrow towers, sentry posts and outposts may stand on a wall that lies beyond the border.
            if (defence ? t.distanceTo(at) > reach(t) && !nearRoad(t, at, ROADSIDE) : !t.contains(level.dimension(), at)) return "realmlord.build.err.border";
        }
        BlockPos m = t.center;
        if (origin.getX() - 1 < m.getX() + MANOR_YARD && m.getX() - MANOR_YARD < origin.getX() + sx + 1
                && origin.getZ() - 1 < m.getZ() + MANOR_YARD && m.getZ() - MANOR_YARD < origin.getZ() + sz + 1) {
            return "realmlord.build.err.manor";
        }
        // A lane of three blocks between buildings (one for towers and outposts), for a path to run.
        int gap = defence ? 1 : BUILDING_GAP;
        for (Building b : t.buildings) {
            if (!b.footprint()) continue;
            int g = b.type == BuildingType.CLEARING ? 1 : defence(b.type) ? 1 : gap;
            if (b.overlaps(origin.getX(), origin.getZ(), sx, sz, g)) return b.overlaps(origin.getX(), origin.getZ(), sx, sz, 1) ? "realmlord.build.err.overlap" : "realmlord.build.err.lane";
        }
        // Not on a path or paved area already laid: take the path up first.
        for (Building b : t.buildings) {
            if (b.type != BuildingType.PATH || b.pathCols == null || b.demolishing) continue;
            for (int i = 0; i + 3 < b.pathCols.length; i += 4) {
                int px = b.pathCols[i], pz = b.pathCols[i + 2];
                if (px >= origin.getX() && px < origin.getX() + sx && pz >= origin.getZ() && pz < origin.getZ() + sz) return "realmlord.build.err.path";
            }
        }
        // Nothing may block a gateway; only towers may stand on the fence.
        for (Building f : t.buildings) {
            if (f.type != BuildingType.SIMPLE_FENCE || f.demolishing) continue;
            int fr = f.fenceRadius > 0 ? f.fenceRadius : t.level.wall();
            for (int x = 0; x < sx; x++)
                for (int z = 0; z < sz; z++) {
                    int wx = origin.getX() + x, wz = origin.getZ() + z;
                    double dist = Math.hypot(wx - t.center.getX(), wz - t.center.getZ());
                    if (Math.abs(dist - fr) > 1.5) continue;
                    if (f.isGate(BuildingDesigns.bearing(t.center, wx, wz))) return "realmlord.build.err.gate";
                    if (type.style != BuildingType.Style.TOWER) return "realmlord.build.err.on_fence";
                }
        }
        // Grounds being levelled: the site will stand on the levelled ground, whatever is there now.
        boolean levelled = true;
        for (int[] c : corners) levelled &= Grounds.covers(t, origin.getX() + c[0], origin.getZ() + c[1]);
        if (levelled) return null;
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
        return start(p, t, type, origin, rotation, gates, 0);
    }

    /**
     * As above; for the fence, wallGrade (1-3) picks which grade's ring it is raised on (62, 78
     * or 94 blocks out), whatever the village's own grade, and 0 the village's own.
     */
    public static String start(ServerPlayer p, Territory t, BuildingType type, BlockPos origin, int rotation, int[] gates, int wallGrade) {
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
            Building grounds = Grounds.of(t);
            if (grounds != null && Grounds.covers(t, origin.getX(), origin.getZ())
                    && Grounds.covers(t, origin.getX() + b.sizeX() - 1, origin.getZ() + b.sizeZ() - 1)) y = grounds.origin.getY();
            b.origin = new BlockPos(origin.getX(), y, origin.getZ());
            b.stage = Building.CLEAR;
        } else {
            b.origin = t.center;
            b.stage = Building.BUILD;
            b.fenceRadius = wallGrade > 0 ? TerritoryLevel.villageWall(wallGrade) : t.level.wall();
            b.gates = gates == null ? new int[]{0, 90, 180, 270} : cleanGates(gates);
            b.fenceStyle = fenceStyle(t);
        }
        t.buildings.add(b);
        level.playSound(null, b.center(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        Notify.player(p, Component.translatable("realmlord.build.started", Component.translatable(type.key()), type.hours,
                count(t, Job.CONSTRUCTION)));
        return null;
    }

    /** How far from one of the village's roads an outpost or tower may stand, out in the wilds. */
    public static final int ROADSIDE = 16;

    /** True if a spot lies within some blocks of a path or road the village has laid. */
    public static boolean nearRoad(Territory t, BlockPos at, int within) {
        long w2 = (long) within * within;
        for (Building b : t.buildings) {
            if (b.type != BuildingType.PATH || b.pathCols == null) continue;
            for (int i = 0; i + 3 < b.pathCols.length; i += 4) {
                long dx = b.pathCols[i] - at.getX(), dz = b.pathCols[i + 2] - at.getZ();
                if (dx * dx + dz * dz <= w2) return true;
            }
        }
        return false;
    }

    /** Defences: as many as the Lord likes, and they may stand out on the wall, or along the village's roads. */
    public static boolean defence(BuildingType type) {
        return type.style == BuildingType.Style.TOWER || type.style == BuildingType.Style.OUTPOST;
    }

    /**
     * How far out the builders may clear and level: the border, or ten blocks past where the
     * Grade 3 wall will stand, whichever is further (the wall and its ground may lie beyond the border).
     */
    public static int reach(Territory t) {
        return Math.max(t.radius() - 3, Grounds.radius());
    }

    /** Largest area the builders clear and level in one order. */
    public static final int MAX_CLEARING = 48;
    /** Work per block cut away or filled in a clearing. */
    public static final int CLEARING_WORK = 20;

    /**
     * Orders the builders to clear and level a rectangle of ground (corners inclusive): trees,
     * plants and rock above the chosen level come away, and dips below it are filled with earth.
     * The level is the usual ground height there, raised or lowered by offset. Returns an error
     * key or null.
     */
    public static String startClearing(ServerPlayer p, Territory t, int x0, int z0, int x1, int z1, int offset) {
        return startClearing(p, t, x0, z0, x1, z1, offset, false);
    }

    /**
     * As above; graded (slope true), the ground is shaped into an even slope from the usual
     * ground height at the first corner to that at the second, for a ramp up to raised ground.
     */
    public static String startClearing(ServerPlayer p, Territory t, int x0, int z0, int x1, int z1, int offset, boolean slope) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(t.dimension)) return "realmlord.build.err.dimension";
        int ax = Math.min(x0, x1), az = Math.min(z0, z1), sx = Math.abs(x1 - x0) + 1, sz = Math.abs(z1 - z0) + 1;
        if (sx < 2 || sz < 2) return "realmlord.clear.err.small";
        if (sx > MAX_CLEARING || sz > MAX_CLEARING) return "realmlord.clear.err.big";
        // Anywhere the village's roads may go: its grounds, and out into the wilds beyond the
        // border (for a ramp up to a road, a cutting, a level site for an outpost), but never in
        // another Lord's land.
        for (int[] c : new int[][]{{ax, az}, {ax + sx - 1, az}, {ax, az + sz - 1}, {ax + sx - 1, az + sz - 1}}) {
            long dx = c[0] - t.center.getX(), dz = c[1] - t.center.getZ();
            if (dx * dx + dz * dz > (long) Paths.MAX_ROAD * Paths.MAX_ROAD) return "realmlord.build.err.border";
            if (othersLand(t, c[0], c[1])) return "realmlord.clear.err.others";
        }
        // Sites still being built are in the way; finished buildings are simply left alone.
        for (Building b : t.buildings) if (!b.complete && b.footprint() && b.overlaps(ax, az, sx, sz, 0)) return "realmlord.clear.err.site";
        java.util.Set<Long> keep = keptColumns(t, ax - SKIRT, az - SKIRT, sx + 2 * SKIRT, sz + 2 * SKIRT);
        List<Integer> heights = new ArrayList<>();
        for (int x = 0; x < sx; x++)
            for (int z = 0; z < sz; z++) {
                if (!level.isLoaded(new BlockPos(ax + x, 64, az + z))) return "realmlord.path.err.unloaded";
                if (!keep.contains(colKey(ax + x, az + z))) heights.add(groundAt(level, ax + x, az + z));
            }
        if (heights.isEmpty()) return "realmlord.clear.err.nothing";
        java.util.Collections.sort(heights);
        int off = Mth.clamp(offset, -8, 8);
        int y = heights.get(heights.size() / 2) + off;
        int ya = slope ? cornerHeight(level, x0, z0) + off : y, yb = slope ? cornerHeight(level, x1, z1) + off : y;
        double vx = x1 - x0, vz = z1 - z0, vv = Math.max(1, vx * vx + vz * vz);
        y = Math.min(ya, yb);
        List<int[]> cols = new ArrayList<>();
        long cut = 0, fill = 0;
        int top = Math.max(ya, yb);
        for (int x = -SKIRT; x < sx + SKIRT; x++)
            for (int z = -SKIRT; z < sz + SKIRT; z++) {
                int wx = ax + x, wz = az + z;
                if (keep.contains(colKey(wx, wz))) continue;
                int d = Math.max(Math.max(-x, x - (sx - 1)), Math.max(-z, z - (sz - 1)));
                // The level here: flat, or along the slope (taken at the nearest point inside the area).
                int ix = ax + Mth.clamp(x, 0, sx - 1), iz = az + Mth.clamp(z, 0, sz - 1);
                double f0 = Mth.clamp(((ix - x0) * vx + (iz - z0) * vz) / vv, 0, 1);
                int ty = slope ? (int) Math.round(ya + (yb - ya) * f0) : y;
                if (!level.isLoaded(new BlockPos(wx, 64, wz))) continue;
                int g = groundAt(level, wx, wz);
                if (d <= 0) {
                    cols.add(new int[]{wx, wz, ty, ty});
                    cut += Math.max(0, g - ty);
                    fill += Math.min(Sites.DEPTH, Math.max(0, ty - g));
                    top = Math.max(top, g);
                } else {
                    // The edges: cut back and banked up one block per block outwards, so no cliff is left.
                    int c = g > ty + d ? ty + d : Building.NO_CUT, f = g < ty - d ? ty - d : Building.NO_FILL;
                    if (c == Building.NO_CUT && f == Building.NO_FILL) continue;
                    cols.add(new int[]{wx, wz, c, f});
                    if (c != Building.NO_CUT) {
                        cut += g - c;
                        top = Math.max(top, g);
                    } else fill += Math.min(Sites.DEPTH, f - g);
                }
            }
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = BuildingType.CLEARING;
        b.origin = new BlockPos(ax, y, az);
        b.w = sx;
        b.d = sz;
        b.stage = Building.CLEAR;
        b.clearCols = new int[cols.size() * 4];
        for (int i = 0; i < cols.size(); i++) System.arraycopy(cols.get(i), 0, b.clearCols, i * 4, 4);
        // Up to the highest ground, with room for the trees on it.
        b.clearHeight = Mth.clamp(top - y + 12, 6, 48);
        b.workNeeded = Math.max(1, (cut + fill + (long) cols.size() / 4) * CLEARING_WORK);
        b.lastWork = level.getGameTime();
        t.buildings.add(b);
        Notify.player(p, Component.translatable("realmlord.clear.started", sx, sz, y, cut, fill));
        return null;
    }

    /**
     * Looks every finished building over: one that has lost blocks (to a creeper, fire, a
     * careless player) is marked for repair, and the builders go back to put it right.
     */
    public static void inspect(ServerLevel level, Territory t) {
        for (Building b : t.buildings) {
            if (!b.complete || b.demolishing || b.damaged || !Sites.repairable(b)) continue;
            int n = Sites.missing(level, t, b);
            if (n <= 0) continue;
            b.damaged = true;
            b.verified = false;
            b.verifyCursor = 0;
            tell(level, t, Component.translatable("realmlord.build.damaged", Component.translatable(b.type.key()), t.name, n));
        }
    }

    /** True if a column lies within another Lord's territory. */
    public static boolean othersLand(Territory t, int x, int z) {
        BlockPos p = new BlockPos(x, 64, z);
        for (Territory o : Paths.ALL.get()) if (o != t && o.dimension.equals(t.dimension) && o.distanceTo(p) <= o.radius()) return true;
        return false;
    }

    /** The usual ground height around a corner (the middle of a 5 x 5 patch). */
    private static int cornerHeight(ServerLevel level, int x, int z) {
        List<Integer> hs = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) if (level.isLoaded(new BlockPos(x + dx, 64, z + dz))) hs.add(groundAt(level, x + dx, z + dz));
        if (hs.isEmpty()) return groundAt(level, x, z);
        java.util.Collections.sort(hs);
        return hs.get(hs.size() / 2);
    }

    /** How far beyond a clearing its edges are sloped. */
    public static final int SKIRT = 8;

    public static long colKeyOf(int x, int z) {
        return colKey(x, z);
    }

    private static long colKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /**
     * Columns in an area that clearing must leave alone: finished buildings with a block around
     * them, paths, the fence ring and the manor yard, and anything near or past the border.
     */
    public static java.util.Set<Long> keptColumns(Territory t, int x0, int z0, int sx, int sz) {
        java.util.Set<Long> out = new java.util.HashSet<>();
        int cx = t.center.getX(), cz = t.center.getZ();
        Building fence = null;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) fence = b;
        for (int x = x0; x < x0 + sx; x++)
            for (int z = z0; z < z0 + sz; z++) {
                long dx = x - cx, dz = z - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                boolean kept = dist > Paths.MAX_ROAD || othersLand(t, x, z) || (Math.abs(dx) <= MANOR_YARD && Math.abs(dz) <= MANOR_YARD);
                if (!kept && fence != null) {
                    int fr = fence.fenceRadius > 0 ? fence.fenceRadius : t.level.wall();
                    kept = dist >= fr - 3 && dist <= fr + 2;
                }
                if (!kept) {
                    for (Building b : t.buildings) {
                        if (b.footprint() && b.type != BuildingType.CLEARING && b.overlaps(x, z, 1, 1, 1)) {
                            kept = true;
                            break;
                        }
                    }
                }
                if (kept) out.add(colKey(x, z));
            }
        for (Building b : t.buildings) {
            if (b.type != BuildingType.PATH || b.pathCols == null) continue;
            for (int i = 0; i + 3 < b.pathCols.length; i += 4) out.add(colKey(b.pathCols[i], b.pathCols[i + 2]));
        }
        return out;
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
            if (!site.needsWork()) continue;
            double work = 0;
            for (Resident r : t.residents) {
                if (r.job == Job.CONSTRUCTION && !r.militia && r.workplace == site.id) work += r.skillAt(Job.CONSTRUCTION);
            }
            if (work > 0) work(level, t, site, work * Office.boost(t, Office.WORKS, 300), ticks);
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
        for (Building b : t.buildings) if (b.needsWork()) sites.add(b);
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
        if (site.complete && site.damaged) {
            // Repairs: the builders put back what was lost, by hand if they are there.
            boolean byHand = buildersNear(level, t, site) && level.getGameTime() - site.lastWork < Sites.STALL_TICKS;
            if (!byHand) Sites.work(level, t, site, (int) Math.max(1, Math.round(work * ticks / 10.0)));
            if (site.verified) {
                site.damaged = false;
                for (Resident r : t.residents) if (r.workplace == site.id) r.workplace = -1;
                tell(level, t, Component.translatable("realmlord.build.repaired", Component.translatable(site.type.key()), t.name));
            }
            return;
        }
        if (site.type == BuildingType.GROUNDS) {
            // Builders work it by hand where they stand; the rest of the crew's work goes on all round.
            Grounds.sweep(level, t, site, (int) Math.max(20, Math.round(work * ticks * 2)));
            return;
        }
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
        BlockPos c = site.footprint() ? site.center() : t.center;
        double reach = site.footprint() ? 48 + Math.max(site.sizeX(), site.sizeZ()) / 2.0 : t.radius() + 16;
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

    static void finish(ServerLevel level, Territory t, Building b) {
        if (b.type == BuildingType.GROUNDS) {
            t.buildings.remove(b);
            for (Resident r : t.residents) if (r.workplace == b.id) r.workplace = -1;
            // A wall still waiting is laid out afresh on the level ground.
            for (Building f : t.buildings) {
                if (f.type != BuildingType.SIMPLE_FENCE || f.placed > 0 || f.complete) continue;
                f.fenceYs = null;
                f.fenceFill = null;
                BuildingDesigns.forget(f);
            }
            tell(level, t, Component.translatable("realmlord.grounds.done", t.name, b.clearHeight));
            return;
        }
        if (b.type == BuildingType.CLEARING) {
            // Once the ground is level there is nothing left to keep track of.
            t.buildings.remove(b);
            BuildingDesigns.forget(b);
            for (Resident r : t.residents) if (r.workplace == b.id) r.workplace = -1;
            ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
            tell(level, t, Component.translatable("realmlord.clear.done", b.sizeX(), b.sizeZ(), t.name));
            return;
        }
        b.complete = true;
        BuildingDesigns.forget(b);
        b.progress = b.workNeeded;
        level.playSound(null, b.center(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (b.quietRefit) {
            b.quietRefit = false;
            return;
        }
        tell(level, t, Component.translatable("realmlord.build.done", Component.translatable(b.type.key()), t.name));
        // The specialist the building needed moves in to run it.
        if (b.type.staff() != null && staffOf(t, b) == null) {
            for (Resident r : t.residents) {
                if (r.occupation == b.type.staff() && r.rank >= b.type.specialistRank && !isStaffing(t, r) && !r.militia) {
                    assign(t, r, Job.STAFF, b.id);
                    tell(level, t, Component.translatable("realmlord.build.staffed", r.name, Component.translatable(b.type.key())));
                    break;
                }
            }
        }
    }

    /** Tells the Lord (if online) and writes it in the village log. */
    public static void tell(ServerLevel level, Territory t, Component msg) {
        t.addLog(GaiaCalendar.today(level.getServer()), msg);
        ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
        if (owner != null) Notify.player(owner, msg);
    }

    /**
     * Takes a building down. Builders dismantle it from the top, carrying back three quarters of
     * the wood and stone that went into what stood; for a site, three quarters of what was never
     * used comes back at once. A site with nothing built yet is simply cleared away.
     */
    public static void demolish(ServerLevel level, Territory t, Building b) {
        if (b.demolishing) return;
        if (b.type == BuildingType.PATH) {
            Paths.remove(level, t, b);
            return;
        }
        if (b.type == BuildingType.CLEARING || b.type == BuildingType.GROUNDS) {
            // Called off: what is done stays done.
            for (Resident r : t.residents) if (r.workplace == b.id) r.workplace = -1;
            t.buildings.remove(b);
            BuildingDesigns.forget(b);
            return;
        }
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

    /** The fence design a territory calls for: the plank palisade, with a wall-walk from Town Grade 1. */
    public static int fenceStyle(Territory t) {
        return BuildingDesigns.walkway(t) ? 3 : 2;
    }

    /**
     * Sends the builders back to a fence to bring it in line with its design (new gates, the
     * palisade replacing the old post-and-rail fence, or a town's wall-walk). Nothing is charged;
     * blocks already right are passed over.
     */
    public static void refit(Territory t, Building fence) {
        BuildingDesigns.forget(fence);
        fence.fenceStyle = fenceStyle(t);
        fence.complete = false;
        fence.placed = 0;
        fence.verified = false;
        fence.verifyCursor = 0;
        fence.progress = fence.workNeeded;
        fence.stage = Building.BUILD;
    }

    /**
     * Puts the wall on another grade's ring. A wall not yet begun is simply laid out again; one
     * already standing is taken down (three quarters of it comes back) and the new one ordered.
     * Returns an error key or null.
     */
    public static String moveWall(ServerPlayer p, Territory t, int wallGrade, int[] gates) {
        Building fence = null;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) fence = b;
        int r = TerritoryLevel.villageWall(wallGrade);
        if (fence == null) return start(p, t, BuildingType.SIMPLE_FENCE, t.center, 0, gates, wallGrade);
        if (fence.fenceRadius == r) return null;
        if (!fence.complete && fence.placed == 0) {
            fence.fenceRadius = r;
            fence.fenceYs = null;
            fence.fenceFill = null;
            if (gates != null) fence.gates = cleanGates(gates);
            BuildingDesigns.forget(fence);
            return null;
        }
        String err = checkCost(p, t, BuildingType.SIMPLE_FENCE, t.freeBuilds.contains(BuildingType.SIMPLE_FENCE));
        if (err != null) return err;
        demolish(p.serverLevel(), t, fence);
        return start(p, t, BuildingType.SIMPLE_FENCE, t.center, 0, gates != null ? gates : fence.gates, wallGrade);
    }

    /** Moves the fence's gates; the builders change the wall over. */
    public static boolean setGates(ServerLevel level, Territory t, int[] gates) {
        Building fence = null;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE && !b.demolishing) fence = b;
        if (fence == null) return false;
        fence.gates = cleanGates(gates);
        if (fence.complete) {
            refit(t, fence);
        } else {
            BuildingDesigns.forget(fence);
            fence.placed = 0;
            fence.verified = false;
            fence.verifyCursor = 0;
        }
        return true;
    }

    /** Old fences, and fences of a territory that has grown into a town, are rebuilt to the current design. */
    public static void refitFence(ServerLevel level, Territory t) {
        for (Building b : t.buildings) {
            if (b.type != BuildingType.SIMPLE_FENCE || b.demolishing) continue;
            // A tower put up on the wall, or taken down: the wall makes room, or closes the gap.
            int sig = BuildingDesigns.towerSignature(t);
            if (b.towerSig != sig) {
                boolean first = b.towerSig == Integer.MIN_VALUE;
                b.towerSig = sig;
                BuildingDesigns.forget(b);
                if (!first && b.complete && b.fenceStyle >= fenceStyle(t)) {
                    int style = b.fenceStyle;
                    refit(t, b);
                    b.fenceStyle = style;
                    b.quietRefit = true;
                }
            }
            int want = fenceStyle(t);
            if (b.fenceStyle >= want) continue;
            if (b.complete) {
                refit(t, b);
                ServerPlayer owner = t.owner == null ? null : level.getServer().getPlayerList().getPlayer(t.owner);
                tell(level, t, Component.translatable("realmlord.fence.refit", t.name));
            } else {
                b.fenceStyle = want;
                BuildingDesigns.forget(b);
            }
        }
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
            if (bt.housing() > 0) {
                // The novel's ten courtyards are beds for 200: any mix of courtyards will do.
                int need = bt.housing() * bt.requiredCount, beds = courtyardBeds(t);
                if (beds < need) out.add(Component.translatable("realmlord.upgrade.need_housing", beds, need));
                continue;
            }
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
        if (!Coins.payWithTreasury(p, Market.buyCost(resource, units))) return false;
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
        if (g == null || units <= 0 || !Coins.payWithTreasury(p, g.buyCost(units))) return false;
        g.add(t, units);
        return true;
    }

    /** Sells the village's goods at the market; the coins go to the Lord, less the 20% tax. */
    /**
     * Takes goods out of the stores as items into the Lord's pack (up to count items; what does
     * not fit falls at their feet). Returns how many items were taken.
     */
    public static int takeGood(ServerPlayer p, Territory t, Goods g, int count) {
        if (g == null || count <= 0) return 0;
        Goods.Unit u = g.item();
        int n = (int) Math.min(count, g.stock(t) / u.units());
        if (n <= 0) return 0;
        g.add(t, -(long) n * u.units());
        net.minecraft.world.item.Item item = g.itemOf();
        int left = n;
        while (left > 0) {
            int batch = Math.min(left, new net.minecraft.world.item.ItemStack(item).getMaxStackSize());
            net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(item, batch);
            if (!p.getInventory().add(st)) p.drop(st, false);
            left -= batch;
        }
        return n;
    }

    public static boolean sellGood(ServerPlayer p, Territory t, Goods g, long units) {
        if (g == null || units <= 0 || g.stock(t) < units) return false;
        g.add(t, -units);
        Coins.give(p, g.sellIncome(units) + Math.round(g.price * units * Market.SALES_TAX * Math.min(1.0, Office.power(t, Office.COMMERCE) / 150.0)));
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
        if (!type.blueprint || !Coins.payWithTreasury(p, type.blueprintPrice())) return false;
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
            if (!Coins.payWithTreasury(p, o.price())) return "realmlord.market.cannot_afford";
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
        construct(level, t, 20 * speed);
        // Every two minutes or so, the village looks its finished buildings over for damage.
        if ((level.getGameTime() / 20) % 120 == Math.floorMod(t.id * 37, 120)) inspect(level, t);
        if (t.waitingSettlers > 0) admitWaiting(level, t);
        refitFence(level, t);
        if (com.realmlord.territory.Residence.upgrade(level, t)) {
            ServerPlayer owner = t.owner == null ? null : server.getPlayerList().getPlayer(t.owner);
            tell(level, t, Component.translatable("realmlord.manor.rebuilt", t.name));
            TerritoryData.get(server).touch();
        }
        Residents.sync(level, t);
    }

    /** Resident's grade name key. */
    public static String gradeKey(int grade) {
        return grade < 0 ? "realmlord.grade.common" : TokenGrade.byId(grade).key();
    }
}
