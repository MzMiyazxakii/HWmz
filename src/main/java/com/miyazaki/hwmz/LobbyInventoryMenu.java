package com.miyazaki.hwmz;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.items.ItemHandlerHelper;

public class LobbyInventoryMenu extends AbstractContainerMenu {
    private static final String STORAGE_KEY = "HWMZPersonalStorage";
    private static final String MARKER_KEY = "hwmz_warehouse_item";
    private final SimpleContainer warehouse;

    public LobbyInventoryMenu(int id, Inventory playerInventory) {
        super(LobbyMenus.INVENTORY.get(), id);
        warehouse = new SimpleContainer(16);
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            load(serverPlayer, warehouse);
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(warehouse, row * 4 + col, 43 + col * 33, 29 + row * 33) {
                    @Override
                    public ItemStack onTake(Player player, ItemStack stack) {
                        stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
                        return super.onTake(player, stack);
                    }
                });
            }
        }
        // The lobby screen exposes the player's hotbar, not the survival inventory grid.
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 43 + i * 18, 274));
        }
    }

    public LobbyInventoryMenu(int id, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(id, playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return LobbySessionManager.isLobbyPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return empty;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < 16) {
            if (!moveItemStackTo(source, 16, 25, true)) return empty;
            source.getOrCreateTag().putBoolean(MARKER_KEY, true);
        } else {
            if (!moveItemStackTo(source, 0, 16, false)) return empty;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            save(serverPlayer, warehouse);
        }
    }

    public static void returnWarehouseItems(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        ListTag stored = root.getCompound(Player.PERSISTED_NBT_TAG).getList(STORAGE_KEY, 10);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getOrCreateTag().getBoolean(MARKER_KEY)) {
                stack.getTag().remove(MARKER_KEY);
                boolean inserted = false;
                for (int i = 0; i < 16; i++) {
                    ItemStack existing = i < stored.size() ? ItemStack.of(stored.getCompound(i)) : ItemStack.EMPTY;
                    if (existing.isEmpty()) {
                        while (stored.size() <= i) stored.add(new CompoundTag());
                        stored.set(i, stack.save(new CompoundTag()));
                        inserted = true;
                        break;
                    }
                    if (ItemHandlerHelper.canItemStacksStack(existing, stack)
                            && existing.getCount() < existing.getMaxStackSize()) {
                        int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        existing.grow(moved);
                        stack.shrink(moved);
                        stored.set(i, existing.save(new CompoundTag()));
                        if (stack.isEmpty()) { inserted = true; break; }
                    }
                }
                if (inserted && stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
        root.getCompound(Player.PERSISTED_NBT_TAG).put(STORAGE_KEY, stored);
        player.getInventory().setChanged();
    }

    private static void load(ServerPlayer player, SimpleContainer container) {
        ListTag list = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(STORAGE_KEY, 10);
        for (int i = 0; i < Math.min(16, list.size()); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
            container.setItem(i, stack);
        }
    }

    private static void save(ServerPlayer player, SimpleContainer container) {
        ListTag list = new ListTag();
        for (int i = 0; i < 16; i++) {
            ItemStack stack = container.getItem(i).copy();
            if (!stack.isEmpty()) stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
            list.add(stack.save(new CompoundTag()));
        }
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).put(STORAGE_KEY, list);
    }
}
