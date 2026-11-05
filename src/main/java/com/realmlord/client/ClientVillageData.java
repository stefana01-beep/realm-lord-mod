package com.realmlord.client;

import net.minecraft.nbt.CompoundTag;

/** The last territory and resident views the server sent. */
public final class ClientVillageData {
    private ClientVillageData() {}

    public static CompoundTag manor;
    public static CompoundTag resident;
}
