package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.village.Goods;
import com.realmlord.village.Occupation;
import com.realmlord.village.Trades;
import com.realmlord.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The novel's goods as items: to and from the stores, sold by craftsmen, and what they do. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class ItemTests {

    @GameTest(template = "empty", timeoutTicks = 200, batch = "items")
    public static void goodsAsItems(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 8400);
        ServerLevel level = helper.getLevel();
        // Everyone starts with the handbook, and it has its pages.
        helper.assertTrue(p.getInventory().countItem(ModItems.VILLAGE_GUIDE.get()) == 1, "no Lord's Handbook after choosing a role");
        helper.assertTrue(com.realmlord.item.GuideItem.book().getTag().getList("pages", 8).size() == com.realmlord.item.GuideItem.PAGES, "the handbook is missing pages");
        // Every good can be taken out as an item, and that item hands back in as the same good.
        for (Goods g : Goods.values()) {
            g.add(t, 20L * g.item().units());
            p.getInventory().clearContent();
            long before = g.stock(t);
            int n = Village.takeGood(p, t, g, 5);
            helper.assertTrue(n == 5 && before - g.stock(t) == 5L * g.item().units(), g.id + ": took " + n);
            helper.assertTrue(p.getInventory().countItem(g.itemOf()) == 5, g.id + ": items not in the pack");
            Goods.Unit u = Goods.of(new ItemStack(g.itemOf()));
            helper.assertTrue(u != null && u.good() == g && u.units() == g.item().units(), g.id + ": its item does not hand back in as the same good");
            Goods.handIn(t, p.getInventory());
            helper.assertTrue(g.stock(t) == before, g.id + ": hand-in did not return the units");
        }
        long salt = Goods.SALT.stock(t);
        helper.assertTrue(Village.takeGood(p, t, Goods.SALT, 1_000_000) == salt && Goods.SALT.stock(t) == 0, "took more or less than the stores held");
        // Craftsmen sell the real goods.
        helper.assertTrue(Trades.of(Occupation.WINEMAKER).stream().anyMatch(o -> o.item() == ModItems.THREE_FLOWER_WINE.get()), "the winemaker sells no wine");
        helper.assertTrue(Trades.of(Occupation.TAILOR).stream().anyMatch(o -> o.item() == ModItems.FINE_SILK.get()), "the tailor sells no silk");
        helper.assertTrue(Trades.of(Occupation.GROCER).stream().anyMatch(o -> o.item() == ModItems.SALT.get()), "the grocer sells no salt");
        helper.assertTrue(Trades.of(Occupation.DOCTOR).stream().anyMatch(o -> o.item() == ModItems.MEDICINAL_HERBS.get()), "the doctor sells no herbs");
        // Wine lends strength; fire oil sets the ground and creatures alight.
        p.getInventory().clearContent();
        ItemStack wine = new ItemStack(ModItems.THREE_FLOWER_WINE.get());
        ModItems.THREE_FLOWER_WINE.get().finishUsingItem(wine, level, p);
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "wine gave no strength");
        // Fire oil: given a moment for the ground around to be loaded.
        helper.runAfterDelay(20, () -> {
            BlockPos spot = t.center.offset(40, 0, 40);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++) {
                    level.setBlock(new BlockPos(spot.getX() + x, y - 1, spot.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                    for (int k = 0; k < 4; k++) level.setBlock(new BlockPos(spot.getX() + x, y + k, spot.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
                }
            Cow cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.moveTo(spot.getX() + 1.5, y, spot.getZ() + 0.5, 0, 0);
            level.addFreshEntity(cow);
            com.realmlord.entity.FireOilEntity oil = new com.realmlord.entity.FireOilEntity(level, p);
            oil.setPos(spot.getX() + 0.5, y, spot.getZ() + 0.5);
            oil.burst(level, null);
            helper.assertTrue(cow.isOnFire(), "fire oil did not set the cow alight: cow alive " + cow.isAlive() + " removed " + cow.isRemoved() + " at " + cow.position()
                    + ", oil at " + oil.position() + ", near " + level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(oil.blockPosition()).inflate(2.5)));
            int fires = 0;
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++) if (level.getBlockState(new BlockPos(spot.getX() + x, y, spot.getZ() + z)).is(Blocks.FIRE)) fires++;
            helper.assertTrue(fires >= 8, "fire oil set too little alight: " + fires + "; at the burst " + level.getBlockState(new BlockPos(spot.getX(), y, spot.getZ()))
                    + " over " + level.getBlockState(new BlockPos(spot.getX(), y - 1, spot.getZ())) + ", oil at " + oil.blockPosition() + ", y " + y
                    + ", can place " + net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, new BlockPos(spot.getX() + 1, y, spot.getZ()), net.minecraft.core.Direction.UP));
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++) level.setBlock(new BlockPos(spot.getX() + x, y, spot.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
            cow.discard();
            oil.discard();
            VillageTests.done(helper, p, t);
        });
    }
}