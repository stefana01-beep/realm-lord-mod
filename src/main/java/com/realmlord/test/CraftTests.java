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
}
