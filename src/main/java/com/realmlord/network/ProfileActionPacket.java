package com.realmlord.network;

import com.realmlord.profile.Profile;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Stat;
import com.realmlord.quest.QuestLogic;
import com.realmlord.skill.Skills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: buttons on the character panel and the skill hotkeys. */
public class ProfileActionPacket {
    public enum Action { ALLOCATE, USE_SKILL, NEXT_SKILL, PREV_SKILL, QUEST_SUBMIT, QUEST_CLAIM, QUEST_ABANDON, OPEN_PANEL }

    private final Action action;
    private final int arg;

    public ProfileActionPacket(Action action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    public ProfileActionPacket(Action action) {
        this(action, 0);
    }

    public ProfileActionPacket(FriendlyByteBuf buf) {
        action = buf.readEnum(Action.class);
        arg = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(arg);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        Profile p = Profiles.get(player);
        switch (action) {
            case ALLOCATE -> {
                if (arg >= 0 && arg < Stat.values().length && p.allocate(Stat.byId(arg))) Profiles.applyAttributes(player);
            }
            case USE_SKILL -> Skills.useSelected(player);
            case NEXT_SKILL -> p.cycleSkill(1);
            case PREV_SKILL -> p.cycleSkill(-1);
            case QUEST_SUBMIT -> QuestLogic.submitSupplies(player);
            case QUEST_CLAIM -> QuestLogic.claim(player);
            case QUEST_ABANDON -> QuestLogic.abandon(player);
            case OPEN_PANEL -> {
                ModNetwork.sync(player);
                ModNetwork.send(player, new OpenScreenPacket(p.hasChosen() ? OpenScreenPacket.CHARACTER : OpenScreenPacket.ROLE));
                return;
            }
        }
        ModNetwork.sync(player);
    }
}
