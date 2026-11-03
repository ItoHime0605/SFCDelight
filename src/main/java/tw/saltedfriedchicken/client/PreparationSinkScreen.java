package tw.saltedfriedchicken.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tw.saltedfriedchicken.PreparationSinkMenu;

/** Custom water level display; no server-modifying buttons or inventory slots. */
public final class PreparationSinkScreen extends AbstractContainerScreen<PreparationSinkMenu> {
    public PreparationSinkScreen(PreparationSinkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 126;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF25343B);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFD6D8D2);
        graphics.fill(x + 10, y + 25, x + 166, y + 71, 0xFF53656B);
        graphics.fill(x + 13, y + 28, x + 163, y + 68, 0xFF1E313A);

        int water = menu.washes();
        int barWidth = water * 142 / 20;
        graphics.fill(x + 17, y + 43, x + 159, y + 57, 0xFF102026);
        if (barWidth > 0)
            graphics.fill(x + 17, y + 43, x + 17 + barWidth, y + 57, 0xFF46B9D6);
        // Ten visual marks, one water bucket per mark (two washes).
        for (int i = 1; i < 10; i++) {
            int markX = x + 17 + i * 142 / 10;
            graphics.fill(markX, y + 43, markX + 1, y + 57, 0xFF284B5A);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 11, 11, 0xFF223037, false);
        graphics.drawString(font,
                Component.translatable("gui.sfcdelight.sink_washes", menu.washes(),
                        20), 17, 31, 0xFFE7F3F6, false);
        graphics.drawString(font, Component.translatable("gui.sfcdelight.sink_bucket"),
                12, 80, 0xFF223037, false);
        graphics.drawString(font, Component.translatable("gui.sfcdelight.sink_use"),
                12, 97, 0xFF223037, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
