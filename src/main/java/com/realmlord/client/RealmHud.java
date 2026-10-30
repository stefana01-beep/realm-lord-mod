package com.realmlord.client;

import com.realmlord.network.ClientProfileData;
import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.Skill;
import com.realmlord.profile.Title;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.quest.VillageQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Top-left readout: title, level, the selected skill and the current quest objective. */
public final class RealmHud {
    private RealmHud() {}

    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.options.renderDebug || mc.player == null || mc.player.isSpectator() || !ClientProfileData.synced) return;
        Font font = mc.font;
        int x = 4, y = 4;
        if (ClientProfileData.mode == PlayMode.NONE.ordinal()) {
            g.drawString(font, Component.translatable("realmlord.hud.choose", KeyBindings.OPEN_PANEL.getTranslatedKeyMessage()), x, y, Ui.GOLD, true);
            return;
        }
        boolean lord = ClientProfileData.mode == PlayMode.LORD.ordinal();
        Component occ = ClientProfileData.combat >= 0 ? Component.translatable(CombatClass.byId(ClientProfileData.combat).key())
                : Component.translatable(WorkOccupation.byId(Math.max(0, ClientProfileData.work)).key());
        Component line;
        boolean pureWork = !lord && ClientProfileData.work >= 0;
        if (pureWork) {
            line = Component.translatable("realmlord.hud.work", Component.translatable(WorkOccupation.tierKey(ClientProfileData.workLevel)), occ, ClientProfileData.workLevel);
        } else {
            line = Component.translatable("realmlord.hud.level", ClientProfileData.level, occ);
        }
        if (lord && ClientProfileData.title > 0) {
            line = Component.translatable(Title.byId(ClientProfileData.title).shortKey()).append(" · ").append(line);
        }
        g.drawString(font, line, x, y, Ui.GOLD, true);
        double frac = pureWork ? ClientProfileData.workXp / (double) Math.max(1, ClientProfileData.workNext)
                : ClientProfileData.xp / (double) Math.max(1, ClientProfileData.xpNext);
        Ui.bar(g, x, y + 11, 90, 2, frac, pureWork ? 0xFF5FA8D3 : 0xFF6FCF97);
        if (ClientProfileData.points > 0) {
            g.drawString(font, Component.translatable("realmlord.hud.points", ClientProfileData.points, KeyBindings.OPEN_PANEL.getTranslatedKeyMessage()),
                    x + 96, y + 8, Ui.JADE, true);
        }
        y += 18;

        // Selected skill
        int sel = ClientProfileData.selectedSkill;
        if (sel >= 0) {
            Skill s = Skill.byId(sel);
            int idx = -1;
            for (int i = 0; i < ClientProfileData.skills.length; i++) if (ClientProfileData.skills[i] == sel) idx = i;
            int left = ClientProfileData.cooldownLeft(idx);
            Component name = Component.translatable(s.key());
            Component text = left > 0 ? name.copy().append(Component.literal("  " + ((left + 19) / 20) + "s"))
                    : name.copy().append(Component.translatable("realmlord.hud.ready", KeyBindings.USE_SKILL.getTranslatedKeyMessage()));
            g.drawString(font, text, x, y, left > 0 ? Ui.MUTED : Ui.INK, true);
            y += 11;
        }

        // Quest objective
        if (lord) {
            Component obj = null;
            int st = ClientProfileData.questStage;
            if (st == VillageQuest.Stage.SUPPLIES.ordinal()) {
                obj = Component.translatable("realmlord.hud.quest_supplies", ClientProfileData.logs, VillageQuest.LOGS_NEEDED,
                        ClientProfileData.stone, VillageQuest.STONE_NEEDED, ClientProfileData.food, VillageQuest.FOOD_NEEDED);
            } else if (st == VillageQuest.Stage.BANDITS.ordinal()) {
                obj = ClientProfileData.banditsLeft > 0 && ClientProfileData.hasCamp
                        ? Component.translatable("realmlord.hud.quest_fight", ClientProfileData.banditsLeft)
                        : Component.translatable("realmlord.hud.quest_camp", ClientProfileData.campX, ClientProfileData.campZ);
            } else if (st == VillageQuest.Stage.CLEARED.ordinal()) {
                obj = Component.translatable("realmlord.hud.quest_claim", KeyBindings.OPEN_PANEL.getTranslatedKeyMessage());
            } else if (st == VillageQuest.Stage.CLAIMED.ordinal() && !ClientProfileData.hasTerritory) {
                obj = Component.translatable("realmlord.hud.quest_found");
            }
            if (obj != null) g.drawString(font, obj, x, y, 0xFFE0D2A8, true);
        }
    };
}
