package com.miyazaki.hwmz;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HWmz.MOD_ID)
public final class HWmz {
    public static final String MOD_ID = "hwmz";

    public HWmz() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LobbyMenus.MENUS.register(modBus);
        ModItems.register(modBus);
        LobbyInventoryNetwork.register();
    }
}
