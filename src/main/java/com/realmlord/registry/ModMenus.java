package com.realmlord.registry;

import com.realmlord.RealmLord;
import com.realmlord.menu.StorehouseMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RealmLord.MODID);

    /** The Lord's Manor storehouse: six pages of a double chest, with page, sort and take-all buttons. */
    public static final RegistryObject<MenuType<StorehouseMenu>> STOREHOUSE = MENUS.register("storehouse",
            () -> IForgeMenuType.create((id, inv, buf) -> new StorehouseMenu(id, inv, buf.readVarInt())));
}
