package com.miyazaki.hwmz;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HWmz.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HWMZCommands {
    private HWMZCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hwmz")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("currency")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000000))
                                                .executes(context -> {
                                                    var targets = EntityArgument.getPlayers(context, "player");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    for (ServerPlayer target : targets) {
                                                        HWMZCurrency.add(target, amount);
                                                        target.sendSystemMessage(Component.literal("Получено валюты: +" + amount));
                                                    }
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("Выдано " + amount + " валюты игрокам: " + targets.size()), true);
                                                    return targets.size();
                                                }))))));
    }
}
