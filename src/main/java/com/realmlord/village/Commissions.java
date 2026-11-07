package com.realmlord.village;

import com.realmlord.Notify;
import com.realmlord.build.Construction;
import com.realmlord.build.Design;
import com.realmlord.build.Designs;
import com.realmlord.RealmConfig;
import com.realmlord.territory.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Plans the village builds itself, as the novel's construction works: each building has its
 * construction conditions (timber and stone units, a craftsman to oversee it, a build time) and
 * the village's construction team raises it from the stores, not from the Lord's pack.
 */
public final class Commissions {
    private Commissions() {}

    /** Blocks per unit of timber or stone: a nine-by-seven hall of some 300 blocks took 30 timber and 20 stone. */
    public static final int BLOCKS_PER_UNIT = 4;
    /** Blocks the construction team raises in a novel hour (with the nominal crew). */
    public static final int BLOCKS_PER_HOUR = 15;

    /** The design a building is built to: its kind's own, or its commissioned plan. */
    public static Design designOf(ServerLevel level, Building b) {
        if (b.type != BuildingType.COMMISSION) return b.type.designId() == null ? null : Designs.builtin(b.type.designId());
        return resolve(level, b.designRef);
    }

    public static Design resolve(ServerLevel level, String ref) {
        if (ref.startsWith("builtin:")) return Designs.builtin(ref.substring(8));
        if (ref.startsWith("drawn:")) {
            try {
                return Designs.get(level.getServer()).drawn(UUID.fromString(ref.substring(6)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static final String[] WOODEN = {"oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "bamboo", "crimson", "warped",
            "log", "plank", "wood", "barrel", "bookshelf", "lectern", "crafting_table", "chest", "ladder", "fence", "door", "bed", "hay", "loom",
            "composter", "scaffolding"};

    public static boolean wooden(BlockState s) {
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
        String path = key == null ? "" : key.getPath();
        for (String w : WOODEN) if (path.contains(w)) return true;
        return false;
    }

    /** The construction conditions of a design: {timber, stone, hours}. */
    public static int[] conditions(Design d) {
        int wood = 0, stone = 0, n = 0;
        for (int c : d.cells) {
            if (c <= 0) continue;
            n++;
            if (wooden(d.palette[c])) wood++;
            else stone++;
        }
        return new int[]{(wood + BLOCKS_PER_UNIT - 1) / BLOCKS_PER_UNIT, (stone + BLOCKS_PER_UNIT - 1) / BLOCKS_PER_UNIT, Math.max(4, n / BLOCKS_PER_HOUR)};
    }

    /** The rotation index (0-3) that turns a design's front to face a player looking this way. */
    public static int rotationIndex(net.minecraft.core.Direction facing) {
        return switch (Construction.rotationFor(facing)) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
    }

    /**
     * Commissions a plan from the village: checks the site (in the Lord's own land, clear of other
     * buildings) and the conditions (timber, stone, a carpenter or mason to oversee it), takes the
     * materials from the stores and sets out the site for the construction team.
     * Returns an error key, or null.
     */
    public static String commission(ServerPlayer p, Territory t, Design design, String ref, Component name, BlockPos clicked,
                                    net.minecraft.core.Direction facing) {
        if (!p.getUUID().equals(t.owner)) return "realmlord.commission.err.not_lord";
        Design turned = design.rotated(Construction.rotationFor(facing));
        BlockPos origin = Construction.originFor(turned, clicked, facing);
        int sx = turned.w, sz = turned.d;
        int[][] corners = {{0, 0}, {sx - 1, 0}, {0, sz - 1}, {sx - 1, sz - 1}};
        for (int[] c : corners) if (!t.contains(p.level().dimension(), origin.offset(c[0], 0, c[1]))) return "realmlord.build.err.border";
        for (Building b : t.buildings) {
            if (!b.footprint() || b.demolishing) continue;
            if (b.overlaps(origin.getX(), origin.getZ(), sx, sz, 1)) return "realmlord.build.err.overlap";
        }
        if (!Village.hasSpecialist(t, Occupation.CARPENTER, 0) && !Village.hasSpecialist(t, Occupation.MASON, 0)) return "realmlord.commission.err.craftsman";
        int[] cond = conditions(design);
        if (t.wood < cond[0] || t.stone < cond[1]) return "realmlord.build.err.resources";
        t.wood -= cond[0];
        t.stone -= cond[1];
        Building b = new Building();
        b.id = t.nextBuildingId++;
        b.type = BuildingType.COMMISSION;
        b.designRef = ref;
        b.label = name.getString();
        b.w = design.w;
        b.d = design.d;
        b.rotation = rotationIndex(facing);
        // The plan's own ground line: the design starts one above the clicked block (lower for wells and piers).
        b.origin = new BlockPos(origin.getX(), clicked.getY() + 1, origin.getZ());
        b.stage = Building.BUILD;
        b.workNeeded = Math.max(1, (long) cond[2] * RealmConfig.ticksPerNovelHour() * Village.NOMINAL_BUILDERS);
        b.lastWork = p.level().getGameTime();
        t.buildings.add(b);
        com.realmlord.territory.TerritoryData.get(p.server).touch();
        Notify.player(p, Component.translatable("realmlord.commission.started", name, cond[0], cond[1], cond[2]));
        return null;
    }
}
