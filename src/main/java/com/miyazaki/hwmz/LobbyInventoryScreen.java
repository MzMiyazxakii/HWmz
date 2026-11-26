package com.miyazaki.hwmz;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LobbyInventoryScreen extends AbstractContainerScreen<LobbyInventoryMenu> {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(HWmz.MOD_ID, "textures/gui/lobby_inventory.png");
    private static final ResourceLocation SLOT =
            new ResourceLocation(HWmz.MOD_ID, "textures/gui/slot.png");

    private boolean shopMode;
    private boolean warehouseHovered;
    private boolean shopHovered;

    private static final int PANEL_TEXT = 0xFFE9E2FF;
    private static final int MUTED_TEXT = 0xFFAAA0D5;
    private static final int ACCENT = 0xFFB9A0FF;
    private static final int DARK = 0xFF17132D;

    public LobbyInventoryScreen(LobbyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 256;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, 256, 256, 256, 256);

        warehouseHovered = inside(mouseX, mouseY, 183, 65, 66, 22);
        shopHovered = inside(mouseX, mouseY, 183, 92, 66, 22);

        drawTab(graphics, 183, 65, 66, 22, "СКЛАД", !shopMode, warehouseHovered);
        drawTab(graphics, 183, 92, 66, 22, "МАГАЗИН", shopMode, shopHovered);

        if (!shopMode) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int x = leftPos + 43 + col * 33;
                    int y = topPos + 29 + row * 33;
                    graphics.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);
                }
            }
        }
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int width, int height,
                         String label, boolean selected, boolean hovered) {
        int fill = selected ? 0xFF65558F : hovered ? 0xFF393052 : 0xFF28223F;
        int edge = selected ? 0xFFE0C9FF : hovered ? 0xFFB9A0FF : 0xFF786A9E;
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, fill);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 1, edge);
        graphics.fill(leftPos + x, topPos + y + height - 1, leftPos + x + width, topPos + y + height, 0xFF17132D);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 1, topPos + y + height, edge);
        graphics.fill(leftPos + x + width - 1, topPos + y, leftPos + x + width, topPos + y + height, 0xFF17132D);
        graphics.drawCenteredString(font, label, leftPos + x + width / 2,
                topPos + y + 7, selected ? 0xFFFFFFFF : PANEL_TEXT);
        if (selected) {
            graphics.fill(leftPos + x + 7, topPos + y + height - 3,
                    leftPos + x + width - 7, topPos + y + height - 2, ACCENT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, shopMode ? "МАГАЗИН" : "ЛИЧНЫЙ СКЛАД",
                128, 9, PANEL_TEXT);
        graphics.drawCenteredString(font, shopMode ? "СКОРО" : "ВАШИ ПРЕДМЕТЫ",
                218, 125, MUTED_TEXT);
        if (shopMode) {
            graphics.drawCenteredString(font, "Магазин пока пуст", 218, 145, PANEL_TEXT);
            graphics.drawCenteredString(font, "Новые товары появятся позже", 218, 158, MUTED_TEXT);
        } else {
            graphics.drawCenteredString(font, "Личные вещи", 218, 145, PANEL_TEXT);
            graphics.drawCenteredString(font, "Не переносятся в мир", 218, 158, MUTED_TEXT);
            graphics.drawCenteredString(font, "Сохраняются между сессиями", 218, 171, MUTED_TEXT);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (shopMode) {
            graphics.fill(leftPos + 38, topPos + 24, leftPos + 178, topPos + 166, 0xF018142B);
            graphics.drawCenteredString(font, "МАГАЗИН ПОКА ПУСТ", leftPos + 108, topPos + 83, PANEL_TEXT);
            graphics.drawCenteredString(font, "Здесь появятся кейсы и облики", leftPos + 108, topPos + 98, MUTED_TEXT);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, 183, 65, 66, 22)) {
                shopMode = false;
                return true;
            }
            if (inside(mouseX, mouseY, 183, 92, 66, 22)) {
                shopMode = true;
                return true;
            }
            if (shopMode && mouseX >= leftPos + 35 && mouseX <= leftPos + 180
                    && mouseY >= topPos + 20 && mouseY <= topPos + 170) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
