package com.miyazaki.hwmz;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LobbyInventoryClient {
    private LobbyInventoryClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(LobbyMenus.INVENTORY.get(), LobbyInventoryScreen::new);
    }

    @Mod.EventBusSubscriber(modid = HWmz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof InventoryScreen
                    && net.minecraft.client.Minecraft.getInstance().player != null
                    && LobbySessionManager.isLobbyPlayer(net.minecraft.client.Minecraft.getInstance().player)) {
                event.setCanceled(true);
                LobbyInventoryNetwork.requestOpen();
            }
        }
    }
}
