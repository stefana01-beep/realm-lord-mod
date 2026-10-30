package com.realmlord.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.realmlord.RealmLord;
import com.realmlord.network.ClientProfileData;
import com.realmlord.network.ModNetwork;
import com.realmlord.network.ProfileActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** J: character panel · R: use the selected skill · Y: next skill (sneak + Y: previous). */
public final class KeyBindings {
    private KeyBindings() {}

    public static final String CATEGORY = "key.categories.realmlord";
    public static final KeyMapping OPEN_PANEL = new KeyMapping("key.realmlord.panel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping USE_SKILL = new KeyMapping("key.realmlord.use_skill",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping CYCLE_SKILL = new KeyMapping("key.realmlord.cycle_skill",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);

    @Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Register {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_PANEL);
            event.register(USE_SKILL);
            event.register(CYCLE_SKILL);
        }
    }

    @Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Handler {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (OPEN_PANEL.consumeClick()) {
                ClientAccess.open(ClientProfileData.mode == 0 ? com.realmlord.network.OpenScreenPacket.ROLE
                        : com.realmlord.network.OpenScreenPacket.CHARACTER);
            }
            while (USE_SKILL.consumeClick()) {
                ModNetwork.toServer(new ProfileActionPacket(ProfileActionPacket.Action.USE_SKILL));
            }
            while (CYCLE_SKILL.consumeClick()) {
                ModNetwork.toServer(new ProfileActionPacket(mc.player.isShiftKeyDown()
                        ? ProfileActionPacket.Action.PREV_SKILL : ProfileActionPacket.Action.NEXT_SKILL));
            }
        }
    }
}
