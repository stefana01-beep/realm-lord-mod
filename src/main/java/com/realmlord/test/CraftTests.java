package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.item.SecretManualItem;
import com.realmlord.item.TechManualItem;
import com.realmlord.martial.ArtData;
import com.realmlord.martial.Arts;
import com.realmlord.martial.MartialEvents;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.Innate;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Stat;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.registry.ModCrafts;
import com.realmlord.work.ManualRecipe;
import com.realmlord.work.TechManuals;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Technical manuals that teach players to make things, the trades' techniques, and the martial arts. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class CraftTests {

    @GameTest(template = "empty", timeoutTicks = 100, batch = "crafts_manuals")
    public static void manualsTeachCraftingAndArtsTrain(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        ServerLevel level = helper.getLevel();
        var rm = level.getRecipeManager();
        // Every trade has at least five manuals, and every manual recipe belongs to a manual.
        for (WorkOccupation w : WorkOccupation.values())
            helper.assertTrue(TechManuals.ofTrade(w).size() >= 5, w + " has only " + TechManuals.ofTrade(w).size() + " manuals");
        long gated = 0;
        for (var r : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(r instanceof ManualRecipe mr)) continue;
            gated++;
            helper.assertTrue(TechManuals.byId(mr.manual()) != null, "recipe " + r.getId() + " names an unknown manual " + mr.manual());
        }
        helper.assertTrue(gated >= 120, "only " + gated + " manual recipes loaded");

        // Unknown, the recipe does nothing; learned, it works.
        var grid = p.inventoryMenu.getCraftSlots();
        grid.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
        grid.setItem(1, new ItemStack(ModCrafts.TEA_LEAVES.get()));
        grid.setItem(2, new ItemStack(ModCrafts.TEA_LEAVES.get()));
        Recipe<?> tea = rm.byKey(new ResourceLocation(RealmLord.MODID, "manual/green_tea")).orElse(null);
        helper.assertTrue(tea instanceof ManualRecipe, "the green tea recipe is missing");
        ManualRecipe teaRecipe = (ManualRecipe) tea;
        helper.assertTrue(!teaRecipe.matches(grid, level), "green tea could be made without the Tea Classic");
        helper.assertTrue(TechManualItem.learn(p, TechManuals.byId("tea_brewing")) == null, "anyone should be able to learn a Junior recipe manual");
        helper.assertTrue(teaRecipe.matches(grid, level), "green tea could not be made after learning the Tea Classic");
        grid.clearContent();
        // Advanced manuals need the trade.
        helper.assertTrue(TechManualItem.learn(p, TechManuals.byId("mingguang_armour")) != null, "a General learned an Advanced blacksmith's manual");

        // Martial arts: learned, practised, levelled; they add to attributes and damage.
        Profile prof = Profiles.get(p);
        int force = prof.stat(Stat.FORCE), body = prof.innate(Innate.BODY);
        helper.assertTrue(SecretManualItem.learn(p, Arts.byId("basic_sabre")) == null, "could not learn the Basic Sabre Technique");
        helper.assertTrue(SecretManualItem.learn(p, Arts.byId("basic_sabre")) != null, "learned an art twice");
        prof.setInnate(Innate.COMPREHENSION, 10);
        MartialEvents.practise(p, ArtData.Kind.BLADE, 1000);
        helper.assertTrue(prof.artLevel("basic_sabre") == 5, "1000 practice should master the Basic Sabre Technique, level " + prof.artLevel("basic_sabre"));
        helper.assertTrue(SecretManualItem.learn(p, Arts.byId("five_animals_play")) == null, "could not learn the Five Animals Play");
        helper.assertTrue(prof.innate(Innate.BODY) == body + 1 && prof.stat(Stat.FORCE) == force + 1,
                "Five Animals Play level 1 should add 1 Body and 1 Force (body " + body + "->" + prof.innate(Innate.BODY) + ", force " + force + "->" + prof.stat(Stat.FORCE) + ")");
        prof.setInnate(Innate.COMPREHENSION, 5);
        helper.assertTrue(SecretManualItem.learn(p, Arts.byId("yang_family_spear")) != null, "an Emperor-rank art was learned with Comprehension 5");
        prof.setInnate(Innate.COMPREHENSION, 30);
        helper.assertTrue(SecretManualItem.learn(p, Arts.byId("yellow_court_scripture")) != null, "the Yellow Court Scripture was learned without the Yellow Emperor technique");
        // A mastered sabre art cuts deeper: 4 damage with a sword becomes 4.8.
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        var pig = EntityType.PIG.create(level);
        pig.moveTo(p.getX() + 2, p.getY(), p.getZ());
        level.addFreshEntity(pig);
        pig.hurt(level.damageSources().playerAttack(p), 4.0F);
        helper.assertTrue(pig.getHealth() <= 10.0F - 4.6F, "the sabre art added no damage (pig at " + pig.getHealth() + ")");
        pig.discard();

        // The substitute doll dies in your place.
        p.getInventory().add(new ItemStack(ModCrafts.SUBSTITUTE_DOLL.get()));
        p.hurt(level.damageSources().fellOutOfWorld(), 10_000.0F);
        helper.assertTrue(p.isAlive() && p.getHealth() > 0 && p.getInventory().countItem(ModCrafts.SUBSTITUTE_DOLL.get()) == 0,
                "the substitute doll did not save its bearer");
        // The market sells the manuals.
        helper.assertTrue(com.realmlord.village.Market.books().size() >= 90, "the market's Manuals page is short");
        RealmTests.leave(p);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "crafts_building")
    public static void plansBuildAndShipsSail(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        ServerLevel level = helper.getLevel();
        // Every structure manual's design ships with the mod, and turning a design four ways brings it back.
        for (var t : com.realmlord.work.TechData.ALL)
            for (String st : t.structures()) {
                var d = com.realmlord.build.Designs.builtin(st);
                helper.assertTrue(d != null && d.blocks() > 0, "design " + st + " is missing");
                var back = d.rotated(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).rotated(net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
                helper.assertTrue(java.util.Arrays.equals(back.cells, d.cells), "turning " + st + " round and back changed it");
            }
        // Learning a structure manual gives its plan.
        helper.assertTrue(TechManualItem.learn(p, TechManuals.byId("mail_post_plan")) == null, "could not learn the mail post drawings");
        helper.assertTrue(p.getInventory().countItem(ModCrafts.BUILDING_PLAN.get()) == 1, "the mail post plan was not given");

        // Build the mail post on flat ground from the player's own materials.
        net.minecraft.core.BlockPos base = helper.absolutePos(new net.minecraft.core.BlockPos(4, 1, 4));
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                level.setBlock(base.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                for (int y = 1; y < 8; y++) level.setBlock(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        var design = com.realmlord.build.Designs.builtin("mail_post");
        for (var e : com.realmlord.build.Construction.materials(design).entrySet()) p.getInventory().add(new ItemStack(e.getKey(), e.getValue()));
        var facing = net.minecraft.core.Direction.SOUTH;
        var turned = design.rotated(com.realmlord.build.Construction.rotationFor(facing));
        var origin = com.realmlord.build.Construction.originFor(turned, base, facing);
        int placed = com.realmlord.build.Construction.buildNow(p, turned, origin);
        helper.assertTrue(placed == design.blocks(), "placed " + placed + " of " + design.blocks() + " blocks");
        helper.assertTrue(level.getBlockState(origin.offset(1, 1, 1)).is(net.minecraft.world.level.block.Blocks.LECTERN), "no lectern where the plan puts it");
        helper.assertTrue(p.getInventory().countItem(Items.LECTERN) == 0, "the lectern was not paid for");
        // Drawing it again gives the same building.
        var drawn = com.realmlord.build.Design.capture(level, origin, origin.offset(turned.w - 1, turned.h - 1, turned.d - 1));
        helper.assertTrue(drawn.blocks() == design.blocks(), "the drawn copy has " + drawn.blocks() + " blocks, not " + design.blocks());

        // Ships: a turreted ship shrugs off blows a boat would not; the merchant ship has a hold.
        var ship = com.realmlord.registry.ModEntities.TURRETED_SHIP.get().create(level);
        ship.moveTo(base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5);
        level.addFreshEntity(ship);
        ship.hurt(level.damageSources().generic(), 10.0F);
        helper.assertTrue(ship.isAlive() && ship.getDamage() < 20.0F, "the turreted ship took " + ship.getDamage() + " hull damage from a 10-point blow");
        helper.assertTrue(ship.getDropItem() == ModCrafts.TURRETED_SHIP.get(), "the turreted ship drops the wrong item");
        ship.discard();
        var cargo = com.realmlord.registry.ModEntities.MERCHANT_SHIP.get().create(level);
        helper.assertTrue(cargo.getContainerSize() == 27, "the merchant ship has no hold");
        for (var r : new String[]{"fishing_boat", "cima_boat", "merchant_ship", "mengchong_warship", "turreted_ship"})
            helper.assertTrue(level.getRecipeManager().byKey(new ResourceLocation(RealmLord.MODID, "manual/" + r)).isPresent(), "no recipe for the " + r);
        RealmTests.leave(p);
        helper.succeed();
    }
}
