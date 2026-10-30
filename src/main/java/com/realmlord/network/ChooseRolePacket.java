package com.realmlord.network;

import com.realmlord.profile.CombatClass;
import com.realmlord.profile.PlayMode;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.quest.QuestLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: the player picked a game mode and an occupation. */
public class ChooseRolePacket {
    private final int mode;
    private final boolean combat;
    private final int occupation;

    public ChooseRolePacket(PlayMode mode, boolean combat, int occupation) {
        this.mode = mode.ordinal();
        this.combat = combat;
        this.occupation = occupation;
    }

    public ChooseRolePacket(FriendlyByteBuf buf) {
        mode = buf.readVarInt();
        combat = buf.readBoolean();
        occupation = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mode);
        buf.writeBoolean(combat);
        buf.writeVarInt(occupation);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        PlayMode m = PlayMode.byId(mode);
        if (m == PlayMode.NONE) return;
        if (combat && (occupation < 0 || occupation >= CombatClass.values().length)) return;
        if (!combat && (occupation < 0 || occupation >= WorkOccupation.values().length)) return;
        QuestLogic.chooseRole(player, m, combat ? CombatClass.byId(occupation) : null, combat ? null : WorkOccupation.byId(occupation));
    }
}
