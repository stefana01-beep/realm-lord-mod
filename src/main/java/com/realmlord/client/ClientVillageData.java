package com.realmlord.client;

import net.minecraft.nbt.CompoundTag;

/** The last territory and resident views the server sent. */
public final class ClientVillageData {
    /** The building window last sent (or the list of buildings). */
    public static net.minecraft.nbt.CompoundTag building;

    private ClientVillageData() {}

    public static CompoundTag manor;
    public static CompoundTag resident;
}
