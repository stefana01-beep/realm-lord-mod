package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Founding;
import com.realmlord.territory.Residence;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingDesigns;
import com.realmlord.village.BuildingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the blocks of the newer designs (the Lord's residence at each tier, the larger
 * courtyards, the palisade with its gateways and a tower, and the town wall-walk) to text files
 * in renders/, so they can be drawn and looked over outside the game.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class DesignDump {

    @GameTest(template = "empty", timeoutTicks = 400, batch = "design_dump")
    public static void dumpDesigns(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 8800);
        ServerLevel level = helper.getLevel();
        BlockPos c = t.center;
        int gy = c.getY() - 1;
        for (int x = -45; x <= 45; x++)
            for (int z = -45; z <= 45; z++) {
                level.setBlock(new BlockPos(c.getX() + x, gy, c.getZ() + z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 24; y++) level.setBlock(new BlockPos(c.getX() + x, gy + y, c.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            }
        try {
            Path dir = Path.of("renders");
            Files.createDirectories(dir);
            // The manor at each tier.
            Founding.buildManor(level, c, Direction.SOUTH);
            scan(level, c, 9, -2, 16, dir.resolve("manor_1.txt"));
            t.level = TerritoryLevel.VILLAGE_2;
            Residence.upgrade(level, t);
            scan(level, c, 9, -2, 16, dir.resolve("manor_2.txt"));
            t.level = TerritoryLevel.VILLAGE_3;
            Residence.upgrade(level, t);
            scan(level, c, 9, -2, 16, dir.resolve("manor_3.txt"));
            t.level = TerritoryLevel.VILLAGE_1;
            // The courtyards, as designed.
            for (BuildingType type : new BuildingType[]{BuildingType.SMALL_RESIDENTIAL_COURTYARD, BuildingType.MEDIUM_RESIDENTIAL_COURTYARD, BuildingType.LARGE_RESIDENTIAL_COURTYARD}) {
                Building b = new Building();
                b.id = 9100 + type.ordinal();
                b.type = type;
                b.origin = c.offset(300, 0, 300);
                write(BuildingDesigns.design(level, t, b), b.origin, dir.resolve(type.id() + ".txt"));
            }
            // The palisade with two gateways and a tower in the wall, then as a town's wall.
            Building fence = new Building();
            fence.id = 9200;
            fence.type = BuildingType.SIMPLE_FENCE;
            fence.origin = c;
            fence.fenceRadius = 24;
            fence.gates = new int[]{90, 270};
            t.buildings.add(fence);
            Building tower = new Building();
            tower.id = 9201;
            tower.type = BuildingType.ARROW_TOWER;
            tower.origin = c.offset(23, 0, -1);
            t.buildings.add(tower);
            List<BuildingDesigns.Placement> wall = new ArrayList<>(BuildingDesigns.design(level, t, fence));
            wall.addAll(BuildingDesigns.design(level, t, tower));
            write(wall, c, dir.resolve("palisade.txt"));
            t.level = TerritoryLevel.TOWN_1;
            BuildingDesigns.forget(fence);
            List<BuildingDesigns.Placement> town = new ArrayList<>(BuildingDesigns.design(level, t, fence));
            town.addAll(BuildingDesigns.design(level, t, tower));
            write(town, c, dir.resolve("town_wall.txt"));
            t.level = TerritoryLevel.VILLAGE_1;
            t.buildings.remove(fence);
            t.buildings.remove(tower);
        } catch (IOException e) {
            helper.fail("could not write the designs: " + e);
        }
        VillageTests.done(helper, p, t);
    }

    private static void scan(ServerLevel level, BlockPos c, int r, int y0, int y1, Path out) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                for (int y = y0; y <= y1; y++) {
                    BlockState s = level.getBlockState(c.offset(x, y, z));
                    if (!s.isAir()) sb.append(x).append(' ').append(y).append(' ').append(z).append(' ').append(s).append('\n');
                }
        Files.writeString(out, sb.toString());
    }

    private static void write(List<BuildingDesigns.Placement> design, BlockPos o, Path out) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (design != null) for (BuildingDesigns.Placement p : design) {
            if (p.state().isAir()) continue;
            BlockPos q = p.pos().subtract(o);
            sb.append(q.getX()).append(' ').append(q.getY()).append(' ').append(q.getZ()).append(' ').append(p.state()).append('\n');
        }
        Files.writeString(out, sb.toString());
    }
}
