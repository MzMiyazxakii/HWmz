package com.miyazaki.hwmz;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HWmz.MOD_ID)
public final class HWmz {
    public static final String MOD_ID = "hwmz";

    public HWmz() {
        LobbyMenus.MENUS.register(FMLJavaModLoadingContext.get().getModEventBus());
        LobbyInventoryNetwork.register();
    }
}
