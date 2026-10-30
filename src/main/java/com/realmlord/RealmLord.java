package com.realmlord;

import com.mojang.logging.LogUtils;
import com.realmlord.network.ModNetwork;
import com.realmlord.registry.ModBlocks;
import com.realmlord.registry.ModCreativeTabs;
import com.realmlord.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RealmLord.MODID)
public class RealmLord {
    public static final String MODID = "realmlord";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealmLord() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RealmConfig.SPEC);
        LOGGER.info("Realm Lord loaded. The world awaits its lords.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
