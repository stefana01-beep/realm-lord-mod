package com.realmlord.network;

import com.realmlord.RealmConfig;
import com.realmlord.profile.Profile;
import com.realmlord.profile.Skill;
import com.realmlord.profile.WorkOccupation;
import com.realmlord.quest.QuestLogic;
import com.realmlord.quest.VillageQuest;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Server to client: the player's whole profile, territory and the world records. */
public class SyncProfilePacket {
    private final FriendlyByteBuf data;

    public SyncProfilePacket(ServerPlayer player, Profile p) {
        data = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        data.writeVarInt(p.getMode().ordinal());
        data.writeVarInt(p.getCombatClass() == null ? -1 : p.getCombatClass().ordinal());
        data.writeVarInt(p.getWork() == null ? -1 : p.getWork().ordinal());
        data.writeVarInt(p.getLevel());
        data.writeVarLong(p.getXp());
        data.writeVarLong(Profile.xpToNext(p.getLevel()));
        data.writeVarInt(p.getPoints());
        for (var s : com.realmlord.profile.Stat.values()) {
            data.writeVarInt(p.baseStat(s));
            data.writeVarInt(p.addedStat(s));
        }
        data.writeVarLong(p.getMerit());
        data.writeVarInt(p.getTitle().ordinal());
        data.writeVarInt(p.getWorkLevel());
        data.writeVarLong(p.getWorkXp());
        data.writeVarLong(WorkOccupation.xpToNext(p.getWorkLevel()));
        Skill sel = p.selectedSkill();
        data.writeVarInt(sel == null ? -1 : sel.ordinal());
        List<Skill> skills = p.unlockedSkills();
        long now = player.serverLevel().getGameTime();
        data.writeVarInt(skills.size());
        for (Skill s : skills) {
            data.writeVarInt(s.ordinal());
            data.writeVarInt((int) Math.max(0, Math.min(Integer.MAX_VALUE, p.cooldownEnd(s) - now)));
        }
        data.writeBoolean(RealmConfig.lordRestriction());

        VillageQuest q = p.quest();
        data.writeVarInt(q.stage.ordinal());
        data.writeVarInt(q.logs);
        data.writeVarInt(q.stone);
        data.writeVarInt(q.food);
        data.writeBoolean(q.camp != null);
        data.writeVarInt(q.camp == null ? 0 : q.camp.getX());
        data.writeVarInt(q.camp == null ? 0 : q.camp.getZ());
        data.writeVarInt(q.banditsLeft);
        data.writeVarInt(q.captives);
        data.writeBoolean(q.chiefKilled);
        data.writeBoolean(q.cacheFound);
        long end = q.stage == VillageQuest.Stage.CLEARED || q.stage == VillageQuest.Stage.CLAIMED ? q.clearTick : player.server.overworld().getGameTime();
        data.writeVarLong(q.stage == VillageQuest.Stage.NOT_STARTED ? 0 : Math.max(0, end - q.startTick));
        data.writeVarLong(QuestLogic.failTicks());
        data.writeVarInt(q.deaths);
        data.writeVarInt(q.grade == null ? -1 : q.grade.ordinal());

        TerritoryData td = TerritoryData.get(player.server);
        Territory t = p.getTerritoryId() >= 0 ? td.byId(p.getTerritoryId()) : null;
        data.writeBoolean(t != null);
        if (t != null) {
            data.writeUtf(t.name);
            data.writeVarInt(t.level.ordinal());
            data.writeVarInt(t.grade.ordinal());
            data.writeVarInt(t.population);
            data.writeVarInt(t.center.getX());
            data.writeVarInt(t.center.getY());
            data.writeVarInt(t.center.getZ());
            data.writeVarInt(t.radius());
            data.writeVarInt(t.foundedOrder);
        }
        data.writeVarInt(td.villagesFounded());
        data.writeBoolean(td.goldTokenClaimed());
    }

    public SyncProfilePacket(FriendlyByteBuf buf) {
        data = new FriendlyByteBuf(buf.readBytes(buf.readVarInt()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(data.readableBytes());
        buf.writeBytes(data, data.readerIndex(), data.readableBytes());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        FriendlyByteBuf b = data;
        ClientProfileData.mode = b.readVarInt();
        ClientProfileData.combat = b.readVarInt();
        ClientProfileData.work = b.readVarInt();
        ClientProfileData.level = b.readVarInt();
        ClientProfileData.xp = b.readVarLong();
        ClientProfileData.xpNext = b.readVarLong();
        ClientProfileData.points = b.readVarInt();
        int[] base = new int[4], added = new int[4];
        for (int i = 0; i < 4; i++) {
            base[i] = b.readVarInt();
            added[i] = b.readVarInt();
        }
        ClientProfileData.base = base;
        ClientProfileData.added = added;
        ClientProfileData.merit = b.readVarLong();
        ClientProfileData.title = b.readVarInt();
        ClientProfileData.workLevel = b.readVarInt();
        ClientProfileData.workXp = b.readVarLong();
        ClientProfileData.workNext = b.readVarLong();
        ClientProfileData.selectedSkill = b.readVarInt();
        int n = b.readVarInt();
        int[] skills = new int[n], cds = new int[n];
        for (int i = 0; i < n; i++) {
            skills[i] = b.readVarInt();
            cds[i] = b.readVarInt();
        }
        ClientProfileData.skills = skills;
        ClientProfileData.cooldownTicks = cds;
        ClientProfileData.cooldownReceivedMs = System.currentTimeMillis();
        ClientProfileData.restriction = b.readBoolean();

        ClientProfileData.questStage = b.readVarInt();
        ClientProfileData.logs = b.readVarInt();
        ClientProfileData.stone = b.readVarInt();
        ClientProfileData.food = b.readVarInt();
        ClientProfileData.hasCamp = b.readBoolean();
        ClientProfileData.campX = b.readVarInt();
        ClientProfileData.campZ = b.readVarInt();
        ClientProfileData.banditsLeft = b.readVarInt();
        ClientProfileData.captives = b.readVarInt();
        ClientProfileData.chiefKilled = b.readBoolean();
        ClientProfileData.cacheFound = b.readBoolean();
        ClientProfileData.questElapsed = b.readVarLong();
        ClientProfileData.questLimit = b.readVarLong();
        ClientProfileData.deaths = b.readVarInt();
        ClientProfileData.questGrade = b.readVarInt();

        ClientProfileData.hasTerritory = b.readBoolean();
        if (ClientProfileData.hasTerritory) {
            ClientProfileData.territoryName = b.readUtf();
            ClientProfileData.territoryLevel = b.readVarInt();
            ClientProfileData.territoryGrade = b.readVarInt();
            ClientProfileData.population = b.readVarInt();
            ClientProfileData.cx = b.readVarInt();
            ClientProfileData.cy = b.readVarInt();
            ClientProfileData.cz = b.readVarInt();
            ClientProfileData.radius = b.readVarInt();
            ClientProfileData.foundedOrder = b.readVarInt();
        }
        ClientProfileData.villagesFounded = b.readVarInt();
        ClientProfileData.goldClaimed = b.readBoolean();
        ClientProfileData.synced = true;
    }
}
