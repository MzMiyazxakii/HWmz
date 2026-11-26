package com.miyazaki.hwmz;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class LobbyInventoryMenu extends AbstractContainerMenu {
    private static final String STORAGE_KEY = "HWMZPersonalStorage";
    private static final String MARKER_KEY = "hwmz_warehouse_item";
    public static final int WAREHOUSE_SIZE = 80;
    public static final int WAREHOUSE_COLUMNS = 8;
    public static final int VISIBLE_ROWS = 5;
    private static final int VISIBLE_SLOTS = WAREHOUSE_COLUMNS * VISIBLE_ROWS;
    private int warehouseScrollRow;
    private final SimpleContainer warehouse;
    private int currency;

    public LobbyInventoryMenu(int id, Inventory playerInventory) {
        super(LobbyMenus.INVENTORY.get(), id);
        warehouse = new SimpleContainer(WAREHOUSE_SIZE);
        currency = HWMZCurrency.get(playerInventory.player);
        if (playerInventory.player instanceof ServerPlayer serverPlayer) load(serverPlayer, warehouse);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < WAREHOUSE_COLUMNS; col++) {
                addSlot(new WarehouseSlot(warehouse, 72 + col * 22, 73 + row * 22, row * WAREHOUSE_COLUMNS + col));
            }
        }
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(playerInventory, i, 75 + i * 22, 203));

        addDataSlot(new DataSlot() {
            @Override public int get() { return currency; }
            @Override public void set(int value) { currency = Math.max(0, value); }
        });
    }

    public LobbyInventoryMenu(int id, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(id, playerInventory);
    }

    public int getCurrency() { return currency; }

    public void setWarehouseScrollRow(int row) {
        int max = WAREHOUSE_SIZE / WAREHOUSE_COLUMNS - VISIBLE_ROWS;
        warehouseScrollRow = Math.max(0, Math.min(max, row));
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            ((WarehouseSlot) slots.get(i)).setStorageIndex(warehouseScrollRow * WAREHOUSE_COLUMNS + i);
        }
    }

    public boolean tryPurchaseCase13(ServerPlayer serverPlayer) {
        if (!LobbySessionManager.isLobbyPlayer(serverPlayer) || currency < 500) {
            if (currency < 500)
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Недостаточно валюты: нужно 500."));
            return false;
        }
        int empty = -1;
        for (int i = 0; i < warehouse.getContainerSize(); i++) {
            if (warehouse.getItem(i).isEmpty()) { empty = i; break; }
        }
        if (empty < 0) {
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("В складе нет свободного места."));
            return false;
        }
        currency -= 500;
        HWMZCurrency.set(serverPlayer, currency);
        ItemStack caseStack = new ItemStack(ModItems.CASE_13.get());
        caseStack.getOrCreateTag().putBoolean(MARKER_KEY, true);
        warehouse.setItem(empty, caseStack);
        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Кейс 13 хохлов куплен за 500 валюты."));
        return true;
    }

    @Override public boolean stillValid(Player player) { return LobbySessionManager.isLobbyPlayer(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        source.getOrCreateTag().putBoolean(MARKER_KEY, true);
        if (index < WAREHOUSE_SIZE) {
            if (!moveItemStackTo(source, WAREHOUSE_SIZE, WAREHOUSE_SIZE + 9, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(source, 0, WAREHOUSE_SIZE, false)) return ItemStack.EMPTY;
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
            HWMZCurrency.set(serverPlayer, currency);
        }
    }

    public static void returnWarehouseItems(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        ListTag stored = root.getList(STORAGE_KEY, 10);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.getOrCreateTag().getBoolean(MARKER_KEY)) continue;
            ItemStack remaining = stack.copy();
            remaining.getTag().remove(MARKER_KEY);
            for (int i = 0; i < WAREHOUSE_SIZE && !remaining.isEmpty(); i++) {
                ItemStack existing = i < stored.size() ? ItemStack.of(stored.getCompound(i)) : ItemStack.EMPTY;
                if (existing.isEmpty()) {
                    while (stored.size() <= i) stored.add(new CompoundTag());
                    stored.set(i, remaining.save(new CompoundTag()));
                    remaining = ItemStack.EMPTY;
                } else if (ItemHandlerHelper.canItemStacksStack(existing, remaining)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                    stored.set(i, existing.save(new CompoundTag()));
                }
            }
            if (remaining.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
            else {
                remaining.getOrCreateTag().remove(MARKER_KEY);
                player.getInventory().setItem(slot, remaining);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Не удалось вернуть часть складских предметов: в складе недостаточно места."));
            }
        }
        root.put(STORAGE_KEY, stored);
        player.getInventory().setChanged();
    }

    private static void load(ServerPlayer player, SimpleContainer container) {
        ListTag list = player.getPersistentData().getList(STORAGE_KEY, 10);
        for (int i = 0; i < Math.min(WAREHOUSE_SIZE, list.size()); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
            container.setItem(i, stack);
        }
    }

    private static void save(ServerPlayer player, SimpleContainer container) {
        ListTag list = new ListTag();
        for (int i = 0; i < WAREHOUSE_SIZE; i++) {
            ItemStack stack = container.getItem(i).copy();
            if (!stack.isEmpty()) stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
            list.add(stack.save(new CompoundTag()));
        }
        player.getPersistentData().put(STORAGE_KEY, list);
    }
}