package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.Building;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Grounds;
import com.realmlord.village.Sites;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The village grounds levelled to the manor out past the Grade 3 wall; walls raised on the
 * ring of whichever grade is asked for; the territory reaching beyond its wall; and defences
 * standing out on a wall past the border.
 */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class GroundsTests {

    @GameTest(template = "empty", timeoutTicks = 400, batch = "grounds")
    public static void groundsAreLevelledAndWallsGoWhereAsked(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 13000);
        ServerLevel level = helper.getLevel();
        BlockPos c = t.center;
        int y = c.getY();
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) level.setChunkForced((c.getX() >> 4) + x, (c.getZ() >> 4) + z, true);

        // The territory keeps land beyond its wall.
        helper.assertTrue(t.radius() == 88 && t.level.wall() == 62 && TerritoryLevel.villageWall(2) == 78 && TerritoryLevel.villageWall(3) == 94,
                "wrong radii: border " + t.radius() + ", wall " + t.level.wall());
        helper.assertTrue(Grounds.radius() == 104, "the grounds should reach ten blocks past the Grade 3 wall: " + Grounds.radius());

        // Uneven ground: a rocky knoll, a tree, a flooded pit, bare rock, and things people made.
        BlockPos knoll = c.offset(20, 0, 0), pit = c.offset(-20, 0, 5), tree = c.offset(0, 0, 25), rock = c.offset(12, 0, -12);
        BlockPos far = c.offset(98, 0, 0), cobble = c.offset(30, 0, 30), yard = c.offset(3, 1, 3);
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 5; z++)
                for (int h = 0; h < 6; h++) level.setBlock(knoll.offset(x, h, z), Blocks.STONE.defaultBlockState(), 2);
        for (int h = 0; h < 6; h++) level.setBlock(far.above(h), Blocks.ANDESITE.defaultBlockState(), 2);
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++)
                for (int d = 1; d <= 3; d++) level.setBlock(pit.offset(x, -d, z), d == 3 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        for (int h = 0; h < 5; h++) level.setBlock(tree.above(h), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(tree.above(5), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        level.setBlock(rock.below(), Blocks.STONE.defaultBlockState(), 2);
        // A tree buried below the floor, and flowers in a hollow: earth all the way down instead.
        BlockPos buried = c.offset(-12, 0, 14);
        level.setBlock(buried.below(), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(buried.below(2), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        BlockPos hollow = c.offset(14, 0, 12);
        level.setBlock(hollow.below(), Blocks.POPPY.defaultBlockState(), 2);
        for (int h = 0; h < 3; h++) level.setBlock(cobble.above(h), Blocks.COBBLESTONE.defaultBlockState(), 2);
        level.setBlock(yard, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        // A building site: its ground is its own.
        Building site = new Building();
        site.id = t.nextBuildingId++;
        site.type = BuildingType.BATHROOM;
        site.w = BuildingType.BATHROOM.width;
        site.d = BuildingType.BATHROOM.depth;
        site.origin = c.offset(-40, 0, -40);
        site.workNeeded = 1000;
        t.buildings.add(site);
        level.setBlock(site.origin.offset(1, 1, 1), Blocks.STONE.defaultBlockState(), 2);

        // The wall: asked for on the Grade 3 ring though the village is Grade 1, and it waits for the levelling.
        t.wood = 1000;
        t.stone = 1000;
        Building job = Grounds.start(level, t);
        helper.assertTrue(job != null && Grounds.active(t) && Grounds.start(level, t) == null, "the levelling did not start once");
        helper.assertTrue(Village.start(p, t, BuildingType.SIMPLE_FENCE, c, 0, null, 3) == null, "could not order the Grade 3 wall");
        Building fence = null;
        for (Building b : t.buildings) if (b.type == BuildingType.SIMPLE_FENCE) fence = b;
        helper.assertTrue(fence != null && fence.fenceRadius == 94, "the wall is not on the Grade 3 ring: " + (fence == null ? "none" : fence.fenceRadius));
        helper.assertTrue(Sites.next(level, t, fence) == null, "the wall should wait for the ground to be levelled");
        helper.assertTrue(Village.moveWall(p, t, 1, null) == null && fence.fenceRadius == 62, "a wall not yet begun should move to the Grade 1 ring");
        helper.assertTrue(Village.moveWall(p, t, 3, null) == null && fence.fenceRadius == 94, "and back to the Grade 3 ring");

        // Defences may stand out on that wall beyond the border; other buildings may not.
        BlockPos onWall = c.offset(65, 0, 65);
        helper.assertTrue(Village.checkPlacement(level, t, BuildingType.ARROW_TOWER, onWall, 0) == null,
                "an arrow tower should stand on the Grade 3 wall beyond the border: " + Village.checkPlacement(level, t, BuildingType.ARROW_TOWER, onWall, 0));
        helper.assertTrue("realmlord.build.err.border".equals(Village.checkPlacement(level, t, BuildingType.DINING_HALL, c.offset(92, 0, 0), 0)),
                "a dining hall was allowed beyond the border");

        for (int i = 0; i < 60 && !Grounds.done(job); i++) Grounds.sweep(level, t, job, 200_000);
        helper.assertTrue(Grounds.done(job), "the levelling did not finish: " + job.groundsCursor + " of " + Grounds.columns(job));
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 5; z++)
                for (int h = 0; h < 6; h++) helper.assertTrue(level.getBlockState(knoll.offset(x, h, z)).isAir(), "the knoll was not cut away");
        helper.assertTrue(level.getBlockState(far).isAir() && level.getBlockState(far.above(5)).isAir(), "ground near the edge, past the border, was not levelled");
        for (int h = 0; h < 6; h++) helper.assertTrue(level.getBlockState(tree.above(h)).isAir(), "the tree still stands");
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++) {
                helper.assertTrue(level.getBlockState(pit.offset(x, -1, z)).is(Blocks.GRASS_BLOCK), "the pit was not filled level with grass on top");
                helper.assertTrue(level.getBlockState(pit.offset(x, -2, z)).is(Blocks.DIRT) && level.getBlockState(pit.offset(x, -3, z)).is(Blocks.DIRT),
                        "the pit was not filled with dirt (and its water) beneath");
            }
        helper.assertTrue(level.getBlockState(rock.below()).is(Blocks.GRASS_BLOCK), "bare rock at the floor was not given grass");
        helper.assertTrue(level.getBlockState(buried.below()).is(Blocks.GRASS_BLOCK) && level.getBlockState(buried.below(2)).is(Blocks.DIRT),
                "a buried trunk and leaves were left in the floor: " + level.getBlockState(buried.below()) + ", " + level.getBlockState(buried.below(2)));
        helper.assertTrue(level.getBlockState(hollow.below()).is(Blocks.GRASS_BLOCK), "a flower in a hollow was left instead of ground");
        helper.assertTrue(level.getBlockState(cobble.above(2)).is(Blocks.COBBLESTONE), "something a person built was cut away");
        helper.assertTrue(level.getBlockState(yard).is(Blocks.GOLD_BLOCK), "the manor yard was touched");
        helper.assertTrue(level.getBlockState(site.origin.offset(1, 1, 1)).is(Blocks.STONE), "a building site was levelled over");
        helper.assertTrue(Grounds.earth(level, c.getX(), y, c.getZ()) == Grounds.GRASS, "plains should be grass over dirt");
        // Finished: the job leaves the list and the wall may start.
        helper.assertTrue(!Grounds.active(t) && !t.buildings.contains(job), "the finished levelling is still listed");

        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) if (Math.abs(x) > 5 || Math.abs(z) > 5) level.setChunkForced((c.getX() >> 4) + x, (c.getZ() >> 4) + z, false);
        VillageTests.done(helper, p, t);
    }
}
