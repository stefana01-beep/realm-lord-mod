package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryLevel;
import com.realmlord.village.Council;
import com.realmlord.village.Festivals;
import com.realmlord.village.GaiaCalendar;
import com.realmlord.village.Office;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Checks for the fixes of the bug sweep. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class FixTests {

    @GameTest(template = "empty", timeoutTicks = 300, batch = "fixes")
    public static void sweepFixesHold(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 22400);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            long today = GaiaCalendar.today(level.getServer());
            // A feast the Rites head holds lasts its week, not just one day.
            t.treasury = 1_000_000;
            CompoundTag feast = new CompoundTag();
            feast.putString("act", "festival");
            t.ext.putInt("festivalKey", -1);
            helper.assertTrue(Council.approveForTest(level, t, feast) == null, "the feast was not held");
            Festivals.day(level, t, today + 1, level.getRandom());
            helper.assertTrue(Festivals.morale(t) > 0, "the feast's cheer was gone the next day");
            Festivals.day(level, t, today + 8, level.getRandom());
            // An old or odd proposal fails quietly rather than throwing.
            CompoundTag odd = new CompoundTag();
            odd.putString("act", "build:NO_SUCH_BUILDING");
            helper.assertTrue("realmlord.council.err.gone".equals(Council.approveForTest(level, t, odd)), "an odd proposal was not turned away");
            // An official leaves the ranks.
            t.level = TerritoryLevel.VILLAGE_2;
            Resident r = null;
            for (Resident o : t.residents) if (o.office < 0 && !o.isFigure()) r = o;
            helper.assertTrue(r != null, "no one to appoint");
            r.unit = 0;
            r.militia = true;
            helper.assertTrue(Office.appoint(t, Office.STEWARD, r) == null && r.unit == -1 && !r.militia, "the official kept their place in the army");
            helper.assertTrue(!Village.ordinary(t, r), "an official counts as an ordinary resident to lose");
            // A prize that does not fit the storehouse is left at the manor, not lost.
            for (int i = 0; i < t.storage.getContainerSize(); i++) t.storage.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            Village.stow(level, t, new ItemStack(Items.DIAMOND));
            boolean onGround = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(t.center).inflate(8), e -> e.getItem().is(Items.DIAMOND)).isEmpty();
            helper.assertTrue(onGround, "the prize that did not fit was lost");
            t.storage.clearContent();
            VillageTests.done(helper, p, t);
        });
    }
}
