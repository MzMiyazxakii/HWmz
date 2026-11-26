package com.miyazaki.hwmz;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HWmz.MOD_ID);
    public static final RegistryObject<Item> CASE_13 = ITEMS.register("case_13_hohlov",
            () -> new Case13Item(new Item.Properties().stacksTo(1)));

    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
