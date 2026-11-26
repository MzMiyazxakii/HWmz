package com.miyazaki.hwmz;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LobbyInventoryScreen extends AbstractContainerScreen<LobbyInventoryMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HWmz.MOD_ID, "textures/gui/lobby_inventory.png");
    private boolean shopMode;

    public LobbyInventoryScreen(LobbyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 310;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Склад"), button -> shopMode = false)
                .bounds(leftPos + 185, topPos + 65, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Магазин"), button -> shopMode = true)
                .bounds(leftPos + 185, topPos + 89, 64, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 256, 256, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, shopMode ? "Магазин" : "Персональный склад",
                128, 10, 0xE5E1FF);
        if (shopMode) {
            graphics.drawCenteredString(font, "Пока пусто", 218, 145, 0xD5D0FF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (shopMode && mouseX >= leftPos + 40 && mouseX <= leftPos + 180
                && mouseY >= topPos + 25 && mouseY <= topPos + 165) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
