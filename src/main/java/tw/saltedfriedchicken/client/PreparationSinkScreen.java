package tw.saltedfriedchicken.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tw.saltedfriedchicken.PreparationSinkMenu;

/** Read-only sink gauge in the shared night-market palette; server owns the water count. */
public final class PreparationSinkScreen extends AbstractContainerScreen<PreparationSinkMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/preparation_sink.png");

    public PreparationSinkScreen(PreparationSinkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 126;
        inventoryLabelY = -1000;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        int water = menu.washes();
        int barWidth = water * 136 / 20;
        graphics.fill(x + 19, y + 62, x + 155, y + 69, 0xFF304B50);
        if (barWidth > 0)
            graphics.fill(x + 19, y + 62, x + 19 + barWidth, y + 69, 0xFF80C5C9);
        for (int i = 1; i < 4; i++) {
            int markX = x + 19 + i * 136 / 4;
            graphics.fill(markX, y + 61, markX + 1, y + 70, 0xFF28464E);
        }
        graphics.drawString(font, title, x + 13, y + 22, 0xFFF7E9D0, false);
        graphics.drawString(font,
                Component.translatable("gui.sfcdelight.sink_washes", water, 20),
                x + 17, y + 50, 0xFFF4EED8, false);
        graphics.drawString(font, Component.translatable("gui.sfcdelight.sink_bucket"),
                x + 14, y + 81, 0xFF433328, false);
        graphics.drawString(font, Component.translatable("gui.sfcdelight.sink_use"),
                x + 14, y + 100, 0xFFF7E4C5, false);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All text is aligned to the custom texture in renderBg.
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
