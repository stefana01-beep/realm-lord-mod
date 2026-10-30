package com.realmlord.event;

import com.realmlord.Notify;
import com.realmlord.RealmConfig;
import com.realmlord.RealmLord;
import com.realmlord.command.RealmCommand;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.OpenScreenPacket;
import com.realmlord.profile.Profile;
import com.realmlord.profile.ProfileCapability;
import com.realmlord.profile.Profiles;
import com.realmlord.profile.Title;
import com.realmlord.quest.BanditCamp;
import com.realmlord.quest.QuestLogic;
import com.realmlord.registry.ModItems;
import com.realmlord.territory.Territory;
import com.realmlord.territory.TerritoryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

    // ------------------------------------------------------------------ capability lifecycle

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player && !event.getObject().getCapability(ProfileCapability.PROFILE).isPresent()) {
            ProfileCapability.Provider provider = new ProfileCapability.Provider();
            event.addCapability(ProfileCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        ProfileCapability.get(original).ifPresent(old ->
                ProfileCapability.get(event.getEntity()).ifPresent(now -> now.copyFrom(old)));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        Profiles.applyAttributes(sp);
        Profiles.refreshName(sp);
        ModNetwork.sync(sp);
        if (!Profiles.get(sp).hasChosen()) {
            Notify.player(sp, "realmlord.msg.welcome");
            ModNetwork.send(sp, new OpenScreenPacket(OpenScreenPacket.ROLE));
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Profiles.applyAttributes(sp);
            ModNetwork.sync(sp);
        }
    }

    @SubscribeEvent
    public static void changedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().reviveCaps();
        if (event.getEntity() instanceof ServerPlayer sp) {
            ProfileCapability.get(sp).ifPresent(Profile::markDirty);
            Profiles.applyAttributes(sp);
            ModNetwork.sync(sp);
        }
    }

    // ------------------------------------------------------------------ names show titles

    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        Component prefix = titlePrefix(event.getEntity());
        if (prefix != null) event.setDisplayname(prefix.copy().append(event.getDisplayname()));
    }

    @SubscribeEvent
    public static void tabName(PlayerEvent.TabListNameFormat event) {
        Component prefix = titlePrefix(event.getEntity());
        if (prefix != null) event.setDisplayName(prefix.copy().append(event.getEntity().getName()));
    }

    private static Component titlePrefix(Player p) {
        if (p.level().isClientSide) return null;
        Profile prof = ProfileCapability.get(p).orElse(null);
        if (prof == null || !prof.isLord() || prof.getTitle() == Title.NONE) return null;
        return Component.literal("[").append(Component.translatable(prof.getTitle().shortKey())).append("] ").withStyle(ChatFormatting.GOLD);
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 == 0) {
            QuestLogic.tick(sp);
            if (RealmConfig.lordRestriction()) enforceRestriction(sp);
        }
        if (sp.tickCount % 10 == 0) ModNetwork.syncIfDirty(sp);
    }

    /**
     * The novel's rule, when switched on: outside of war, a Lord stays inside their own
     * territory. Other dimensions (battlefields) are exempt.
     */
    private static void enforceRestriction(ServerPlayer sp) {
        Profile p = Profiles.get(sp);
        if (!p.isLord() || p.getTerritoryId() < 0 || sp.isCreative() || sp.isSpectator()) return;
        if (!sp.level().dimension().equals(Level.OVERWORLD)) return;
        Territory t = TerritoryData.get(sp.server).byId(p.getTerritoryId());
        if (t == null) return;
        double over = t.distanceTo(sp.blockPosition()) - t.radius() - RealmConfig.roamMargin();
        var data = sp.getPersistentData();
        if (over <= 0) {
            data.remove("realmlord_outside");
            return;
        }
        int secs = data.getInt("realmlord_outside") + 1;
        data.putInt("realmlord_outside", secs);
        if (secs == 1 || secs % 5 == 0) Notify.player(sp, Component.translatable("realmlord.restriction.warning", Math.max(0, 10 - secs)));
        if (secs >= 10) {
            BlockPos home = t.center.relative(net.minecraft.core.Direction.SOUTH, 6);
            sp.teleportTo(sp.server.overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, sp.getYRot(), sp.getXRot());
            data.remove("realmlord_outside");
            Notify.player(sp, "realmlord.restriction.sent_home");
        }
    }

    // ------------------------------------------------------------------ fighting

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer sp) {
            QuestLogic.playerDied(sp);
            long lost = Profiles.get(sp).deathPenalty();
            if (lost > 0) Notify.player(sp, Component.translatable("realmlord.msg.death_penalty", lost));
            return;
        }
        // Character experience for whoever made the kill.
        if (event.getSource().getEntity() instanceof ServerPlayer killer && (dead instanceof Enemy || dead.getTags().contains(BanditCamp.BANDIT_TAG))) {
            long xp = Math.max(1, Math.round(dead.getMaxHealth() * 2.0));
            Profiles.grantXp(killer, xp);
        }
        // The bandit chief carries a ledger.
        if (dead.getTags().contains(BanditCamp.CHIEF_TAG)) {
            UUID owner = BanditCamp.owner(dead);
            ItemEntity ledger = new ItemEntity(dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(ModItems.BANDIT_LEDGER.get()));
            dead.level().addFreshEntity(ledger);
            if (owner != null && dead.level().getServer() != null) {
                ServerPlayer op = dead.level().getServer().getPlayerList().getPlayer(owner);
                if (op != null) {
                    Profiles.get(op).quest().chiefKilled = true;
                    Profiles.get(op).markDirty();
                    Notify.player(op, "realmlord.quest.chief_down");
                }
            }
        }
    }

    /** Bandits ignore their caged captives. */
    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && target.getTags().contains(BanditCamp.CAPTIVE_TAG)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ChestBlock) {
            QuestLogic.openedChest(sp, event.getPos());
        }
    }

    // ------------------------------------------------------------------ commands

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        RealmCommand.register(event.getDispatcher());
    }
}
