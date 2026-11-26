package com.miyazaki.hwmz;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LobbyInventoryScreen extends AbstractContainerScreen<LobbyInventoryMenu> {
    private static final ResourceLocation SLOT =
            new ResourceLocation(HWmz.MOD_ID, "textures/gui/slot.png");

    private static final int PANEL = 0xFF202329;
    private static final int PANEL_DARK = 0xFF171A1F;
    private static final int PANEL_LIGHT = 0xFF343A43;
    private static final int TEXT = 0xFFF0F0F0;
    private static final int MUTED = 0xFF9DA5AE;
    private static final int ACCENT = 0xFF7FA6C9;

    private boolean shopMode;

    public LobbyInventoryScreen(LobbyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 240;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0x88000000);
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 31, PANEL_LIGHT);
        graphics.fill(x + 1, y + 31, x + imageWidth - 1, y + 32, 0xFF101216);

        drawTab(graphics, 12, 38, 88, 21, "СКЛАД", !shopMode,
                inside(mouseX, mouseY, 12, 38, 88, 21));
        drawTab(graphics, 103, 38, 88, 21, "МАГАЗИН", shopMode,
                inside(mouseX, mouseY, 103, 38, 88, 21));

        graphics.fill(x + 12, y + 64, x + 308, y + 190, PANEL_DARK);
        graphics.fill(x + 13, y + 65, x + 307, y + 189, 0xFF252A31);
        graphics.fill(x + 13, y + 65, x + 307, y + 66, 0xFF414953);

        if (!shopMode) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int sx = 75 + col * 22;
                    int sy = 62 + row * 22;
                    graphics.blit(SLOT, x + sx, y + sy, 0, 0, 18, 18, 18, 18);
                }
            }
        } else {
            graphics.fill(x + 22, y + 78, x + 298, y + 176, 0xFF20252B);
            graphics.fill(x + 22, y + 78, x + 298, y + 79, 0xFF414953);
        }

        graphics.fill(x + 12, y + 198, x + 308, y + 230, PANEL_DARK);
        graphics.fill(x + 13, y + 199, x + 307, y + 229, 0xFF252A31);
        for (int i = 0; i < 9; i++) {
            graphics.blit(SLOT, x + 75 + i * 22, y + 203, 0, 0, 18, 18, 18, 18);
        }

        graphics.fill(x + 244, y + 6, x + 308, y + 26, 0xFF171A1F);
        graphics.fill(x + 244, y + 6, x + 308, y + 7, 0xFF59636E);
        graphics.fill(x + 244, y + 25, x + 308, y + 26, 0xFF0F1114);
        graphics.renderItem(new ItemStack(Items.COMPASS), x + 248, y + 8);
        graphics.drawString(font, Integer.toString(menu.getCurrency()), x + 270, y + 12, TEXT, false);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int w, int h,
                         String label, boolean selected, boolean hovered) {
        int left = leftPos + x;
        int top = topPos + y;
        int fill = selected ? 0xFF414A55 : hovered ? 0xFF343B44 : 0xFF292E35;
        int edge = selected ? ACCENT : 0xFF4A525C;
        graphics.fill(left, top, left + w, top + h, fill);
        graphics.fill(left, top, left + w, top + 1, edge);
        graphics.fill(left, top, left + 1, top + h, edge);
        graphics.fill(left + w - 1, top, left + w, top + h, 0xFF15181C);
        graphics.drawCenteredString(font, label, left + w / 2, top + 7, selected ? TEXT : 0xFFB8C0C8);
        if (selected) {
            graphics.fill(left + 8, top + h - 2, left + w - 8, top + h - 1, ACCENT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "ПЕРСОНАЛЬНОЕ СНАРЯЖЕНИЕ", 12, 11, TEXT, false);
        graphics.drawString(font, "ЛОББИ", 12, 23, MUTED, false);

        if (shopMode) {
            graphics.drawCenteredString(font, "МАГАЗИН", 160, 82, TEXT);
            graphics.drawCenteredString(font, "Пока здесь ничего нет", 160, 112, 0xFFD4D9DE);
            graphics.drawCenteredString(font, "Позже появятся кейсы с классами и облики", 160, 127, MUTED);
        } else {
            graphics.drawString(font, "ЛИЧНЫЙ СКЛАД", 20, 76, TEXT, false);
            graphics.drawString(font, "16 ячеек", 20, 90, MUTED, false);
            graphics.drawString(font, "Содержимое хранится отдельно", 20, 112, MUTED, false);
            graphics.drawString(font, "от обычного инвентаря.", 20, 124, MUTED, false);
            graphics.drawString(font, "В игровой мир не переносится.", 20, 146, 0xFFB8C9D8, false);
        }
        graphics.drawString(font, "ПАНЕЛЬ БЫСТРОГО ДОСТУПА", 14, 188, MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (inside(mouseX, mouseY, 244, 6, 64, 20)) {
            graphics.renderTooltip(font, Component.literal("Компасная валюта: " + menu.getCurrency()),
                    mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, 12, 38, 88, 21)) {
                shopMode = false;
                return true;
            }
            if (inside(mouseX, mouseY, 103, 38, 88, 21)) {
                shopMode = true;
                return true;
            }
            if (shopMode && (inside(mouseX, mouseY, 12, 64, 296, 126)
                    || inside(mouseX, mouseY, 12, 198, 296, 32))) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w
                && mouseY >= topPos + y && mouseY < topPos + y + h;
    }
}
