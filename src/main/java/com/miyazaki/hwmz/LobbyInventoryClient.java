package com.miyazaki.hwmz;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LobbyInventoryClient {
    private LobbyInventoryClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(LobbyMenus.INVENTORY.get(), LobbyInventoryScreen::new));
    }
}

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class LobbyInventoryForgeEvents {
    private LobbyInventoryForgeEvents() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (event.getNewScreen() instanceof InventoryScreen
                && player != null
                && LobbySessionManager.isLobbyPlayer(player)) {
            event.setCanceled(true);
            LobbyInventoryNetwork.requestOpen();
        }
    }
}
