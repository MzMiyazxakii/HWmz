package com.miyazaki.hwmz;

import net.minecraft.world.entity.player.Player;

public final class HWMZCurrency {
    private static final String KEY = "HWMZCompassCurrency";
    private HWMZCurrency() {}

    public static int get(Player player) {
        return Math.max(0, player.getPersistentData().getInt(KEY));
    }

    public static void set(Player player, int amount) {
        player.getPersistentData().putInt(KEY, Math.max(0, amount));
    }

    public static void add(Player player, int amount) {
        set(player, (int) Math.min(Integer.MAX_VALUE, (long) get(player) + amount));
    }
}
