package com.realmlord.event;

import com.realmlord.RealmLord;
import com.realmlord.entity.BanditEntity;
import com.realmlord.entity.SettlerEntity;
import com.realmlord.profile.Profile;
import com.realmlord.registry.ModEntities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealmLord.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(Profile.class);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BANDIT.get(), BanditEntity.createAttributes().build());
        event.put(ModEntities.SETTLER.get(), SettlerEntity.createAttributes().build());
    }
}
