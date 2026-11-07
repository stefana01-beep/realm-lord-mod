package com.realmlord.test;

import com.realmlord.RealmLord;
import com.realmlord.profile.CombatClass;
import com.realmlord.territory.Territory;
import com.realmlord.village.BuildingType;
import com.realmlord.village.Council;
import com.realmlord.village.Job;
import com.realmlord.village.Office;
import com.realmlord.village.Resident;
import com.realmlord.village.Village;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The council: officials propose, the Lord approves, and they carry it out. */
@GameTestHolder(RealmLord.MODID)
@PrefixGameTestTemplate(false)
public class CouncilTests {

    private static Resident official(Territory t, Office o) {
        for (Resident r : t.residents) {
            if (r.office >= 0 || r.isFigure() || r.militia) continue;
            r.politics = 99;
            r.intelligence = 99;
            r.command = 99;
            Office.appoint(t, o, r);
            return r;
        }
        return null;
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "council")
    public static void officialsProposeAndCarryOut(GameTestHelper helper) {
        ServerPlayer p = RealmTests.lord(helper, CombatClass.GENERAL);
        Territory t = VillageTests.village(helper, p, 20400);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            // The Steward sees the beds running out and proposes housing.
            helper.assertTrue(official(t, Office.STEWARD) != null, "no one to appoint");
            t.blueprints.add(BuildingType.SMALL_RESIDENTIAL_COURTYARD.id());
            t.wood = 2_000;
            t.stone = 2_000;
            while (Village.openBeds(t) > 2) Village.add(t, Resident.random(level.getRandom(), com.realmlord.village.Occupation.FARMER, 0));
            long day = t.lastDay + 1;
            for (int i = 0; i < 20 && Council.list(t).isEmpty(); i++) Council.day(level, t, day, level.getRandom());
            helper.assertTrue(!Council.list(t).isEmpty(), "the Steward proposed nothing");
            CompoundTag s = Council.list(t).getCompound(0);
            helper.assertTrue(s.getString("act").startsWith("build:") || s.getString("act").startsWith("jobs:"), "an odd proposal: " + s.getString("act"));
            int buildings = t.buildings.size();
            String err = Council.approve(p, t, s.getInt("id"));
            helper.assertTrue(err == null, "the approved proposal failed: " + err);
            helper.assertTrue(Council.list(t).isEmpty() && !Council.log(t).isEmpty(), "the proposal was not carried out and logged");
            if (s.getString("act").startsWith("build:")) helper.assertTrue(t.buildings.size() == buildings + 1, "no building was laid out");
            // Left to decide routine matters, the Agriculture head moves idle people to the fields himself.
            Council.setAuto(t, true);
            t.level = com.realmlord.territory.TerritoryLevel.VILLAGE_2;
            VillageTests.built(t, BuildingType.FARMLAND);
            helper.assertTrue(official(t, Office.AGRICULTURE) != null && Office.power(t, Office.AGRICULTURE) > 0, "no Agriculture head");
            t.food = 10;
            int farmers = Village.count(t, Job.FARMING);
            for (int i = 0; i < 20 && Village.count(t, Job.FARMING) == farmers; i++) Council.day(level, t, day + 1, level.getRandom());
            helper.assertTrue(Village.count(t, Job.FARMING) > farmers, "the Agriculture head did not send people to the fields");
            VillageTests.done(helper, p, t);
        });
    }
}
