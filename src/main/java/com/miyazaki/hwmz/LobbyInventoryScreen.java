package com.miyazaki.hwmz;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LobbyInventoryScreen extends AbstractContainerScreen<LobbyInventoryMenu> {
    private static final int PANEL = 0xFF202329, PANEL_DARK = 0xFF171A1F, PANEL_LIGHT = 0xFF343A43;
    private static final int TEXT = 0xFFF0F0F0, MUTED = 0xFF9DA5AE, ACCENT = 0xFF7FA6C9;
    private boolean shopMode;

    public LobbyInventoryScreen(LobbyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320; imageHeight = 240;
        inventoryLabelY = 10000; titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0x88000000);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 31, PANEL_LIGHT);
        g.fill(x + 1, y + 31, x + imageWidth - 1, y + 32, 0xFF101216);

        drawTab(g, 12, 38, 88, 21, "СКЛАД", !shopMode, inside(mouseX, mouseY, 12, 38, 88, 21));
        drawTab(g, 103, 38, 88, 21, "МАГАЗИН", shopMode, inside(mouseX, mouseY, 103, 38, 88, 21));

        g.fill(x + 12, y + 64, x + 308, y + 190, PANEL_DARK);
        g.fill(x + 13, y + 65, x + 307, y + 189, 0xFF252A31);
        g.fill(x + 13, y + 65, x + 307, y + 66, 0xFF414953);

        if (!shopMode) {
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    drawSlot(g, x + 75 + col * 22, y + 62 + row * 22);
        } else {
            g.fill(x + 22, y + 77, x + 298, y + 178, 0xFF20252B);
            g.fill(x + 22, y + 77, x + 298, y + 78, 0xFF414953);
            g.fill(x + 31, y + 87, x + 289, y + 168, 0xFF292F37);
            g.fill(x + 31, y + 87, x + 289, y + 88, 0xFF4A535E);
            g.renderItem(new ItemStack(ModItems.CASE_13.get()), x + 42, y + 108);
            g.fill(x + 213, y + 132, x + 280, y + 156, 0xFF354D3C);
            g.fill(x + 213, y + 132, x + 280, y + 133, 0xFF648B6D);
            g.drawCenteredString(font, "КУПИТЬ", x + 246, y + 140, 0xFFFFFFFF);
        }

        g.fill(x + 12, y + 198, x + 308, y + 230, PANEL_DARK);
        g.fill(x + 13, y + 199, x + 307, y + 229, 0xFF252A31);
        for (int i = 0; i < 9; i++) drawSlot(g, x + 75 + i * 22, y + 203);

        g.fill(x + 244, y + 6, x + 308, y + 26, 0xFF171A1F);
        g.fill(x + 244, y + 6, x + 308, y + 7, 0xFF59636E);
        g.fill(x + 244, y + 25, x + 308, y + 26, 0xFF0F1114);
        g.renderItem(new ItemStack(Items.COMPASS), x + 248, y + 8);
        g.drawString(font, Integer.toString(menu.getCurrency()), x + 270, y + 12, TEXT, false);
    }

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 20, y + 20, 0xFF0D0F12);
        g.fill(x, y, x + 19, y + 1, 0xFF555D67);
        g.fill(x, y, x + 1, y + 19, 0xFF555D67);
        g.fill(x + 1, y + 1, x + 19, y + 19, 0xFF303640);
        g.fill(x + 2, y + 2, x + 18, y + 18, 0xFF242A32);
        g.fill(x + 1, y + 19, x + 20, y + 20, 0xFF090B0E);
        g.fill(x + 19, y + 1, x + 20, y + 20, 0xFF090B0E);
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, int h, String label, boolean selected, boolean hovered) {
        int left = leftPos + x, top = topPos + y;
        int fill = selected ? 0xFF414A55 : hovered ? 0xFF343B44 : 0xFF292E35;
        int edge = selected ? ACCENT : 0xFF4A525C;
        g.fill(left, top, left + w, top + h, fill);
        g.fill(left, top, left + w, top + 1, edge);
        g.fill(left, top, left + 1, top + h, edge);
        g.fill(left + w - 1, top, left + w, top + h, 0xFF15181C);
        g.drawCenteredString(font, label, left + w / 2, top + 7, selected ? TEXT : 0xFFB8C0C8);
        if (selected) g.fill(left + 8, top + h - 2, left + w - 8, top + h - 1, ACCENT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, shopMode ? "МАГАЗИН" : "ЛИЧНЫЙ СКЛАД", 12, 11, TEXT, false);
        if (shopMode) {
            g.drawString(font, "Кейс 13 хохлов", 82, 105, TEXT, false);
            g.drawString(font, "500", 82, 123, 0xFFFFD36A, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (inside(mouseX, mouseY, 244, 6, 64, 20))
            g.renderTooltip(font, Component.literal("Компасная валюта: " + menu.getCurrency()), mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, 12, 38, 88, 21)) { shopMode = false; return true; }
            if (inside(mouseX, mouseY, 103, 38, 88, 21)) { shopMode = true; return true; }
            if (shopMode && inside(mouseX, mouseY, 213, 132, 67, 24)) {
                LobbyInventoryNetwork.requestBuyCase13();
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
