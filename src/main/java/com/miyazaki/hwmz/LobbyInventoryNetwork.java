package com.miyazaki.hwmz;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class LobbyInventoryNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HWmz.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private LobbyInventoryNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(OpenInventoryRequest.class, 0)
                .encoder((msg, buf) -> {})
                .decoder(buf -> new OpenInventoryRequest())
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null && LobbySessionManager.isLobbyPlayer(player)) {
                        player.openMenu(new SimpleMenuProvider(
                                (id, inventory, p) -> new LobbyInventoryMenu(id, inventory),
                                net.minecraft.network.chat.Component.literal("Склад")));
                    }
                    ctx.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BuyCase13Request.class, 1)
                .encoder((msg, buf) -> {})
                .decoder(buf -> new BuyCase13Request())
                .consumerMainThread((msg, ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null && LobbySessionManager.isLobbyPlayer(player)
                            && player.containerMenu instanceof LobbyInventoryMenu menu) {
                        menu.tryPurchaseCase13(player);
                    }
                    ctx.get().setPacketHandled(true);
                }).add();
    }

    public static void requestOpen() { CHANNEL.sendToServer(new OpenInventoryRequest()); }
    public static void requestBuyCase13() { CHANNEL.sendToServer(new BuyCase13Request()); }
    public static final class OpenInventoryRequest {}
    public static final class BuyCase13Request {}
}
