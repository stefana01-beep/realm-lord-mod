package com.realmlord.profile;

import com.realmlord.RealmLord;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;

public class ProfileCapability {
    public static final Capability<Profile> PROFILE = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = RealmLord.id("profile");

    public static Optional<Profile> get(Player player) {
        return player.getCapability(PROFILE).resolve();
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final Profile data = new Profile();
        private LazyOptional<Profile> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap != PROFILE) return LazyOptional.empty();
            // Forge invalidates capabilities when a player changes dimension; the data itself
            // is untouched, so hand out a fresh handle to the same object.
            if (!optional.isPresent()) optional = LazyOptional.of(() -> data);
            return optional.cast();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
