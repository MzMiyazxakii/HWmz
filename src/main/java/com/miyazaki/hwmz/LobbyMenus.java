package com.miyazaki.hwmz;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LobbyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, HWmz.MOD_ID);
    public static final RegistryObject<MenuType<LobbyInventoryMenu>> INVENTORY =
            MENUS.register("lobby_inventory", () -> IForgeMenuType.create(LobbyInventoryMenu::new));

    private LobbyMenus() {}
}
