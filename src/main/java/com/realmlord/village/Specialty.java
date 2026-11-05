package com.realmlord.village;

/**
 * A talent's specialty, like the novel's character cards ("good at agricultural water
 * management"). It boosts the output of one kind of work while the resident does it.
 */
public enum Specialty {
    GREEN_THUMB("green_thumb", Job.FARMING),
    TIMBER_SENSE("timber_sense", Job.LOGGING),
    STONECUTTER("stonecutter", Job.QUARRYING),
    ORE_SEEKER("ore_seeker", Job.MINING),
    MASTER_BUILDER("master_builder", Job.CONSTRUCTION),
    CAPABLE_ADMINISTRATOR("capable_administrator", Job.ADMINISTRATION),
    SKILLED_HANDS("skilled_hands", Job.STAFF);

    public final String id;
    public final Job job;

    Specialty(String id, Job job) {
        this.id = id;
        this.job = job;
    }

    public String key() {
        return "realmlord.specialty." + id;
    }

    public static Specialty byId(int i) {
        Specialty[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    /** The specialty that suits an occupation best. */
    public static Specialty forOccupation(Occupation o) {
        return switch (o) {
            case FARMER, FRUIT_FARMER -> GREEN_THUMB;
            case CARPENTER, HUNTER -> TIMBER_SENSE;
            case MASON -> STONECUTTER;
            case MINER -> ORE_SEEKER;
            case CIVIL_SERVANT, SCHOLAR -> CAPABLE_ADMINISTRATOR;
            case OFFICER, MARTIAL_ARTIST -> MASTER_BUILDER;
            default -> SKILLED_HANDS;
        };
    }
}
