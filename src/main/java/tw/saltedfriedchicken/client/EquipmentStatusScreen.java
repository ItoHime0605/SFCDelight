package tw.saltedfriedchicken.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tw.saltedfriedchicken.EquipmentStatusMenu;

/** Warm pixel-art, read-only equipment dashboard. All numbers arrive through vanilla menu data slots. */
public final class EquipmentStatusScreen extends AbstractContainerScreen<EquipmentStatusMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/equipment_status.png");
    // Original 16px equipment insignia rather than the same decorative icon for every device.
    private static final ResourceLocation ICON_CART =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/status_cart.png");
    private static final ResourceLocation ICON_FRYER =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/status_fryer.png");
    private static final ResourceLocation ICON_DEOILER =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/status_deoiler.png");
    private static final ResourceLocation ICON_PREPARATION =
            ResourceLocation.fromNamespaceAndPath("sfcdelight", "textures/gui/status_preparation.png");
    private static final int DARK = 0xFF382923;
    private static final int WHITE = 0xFFF9EDD6;
    private static final int MUTED = 0xFF775A44;
    private static final int TEAL = 0xFF78B7B8;
    private static final int GOLD = 0xFFDDA555;
    private static final int RED = 0xFFC66A5D;

    public EquipmentStatusScreen(EquipmentStatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 208;
        imageHeight = 180;
        inventoryLabelY = -1000;
    }

    private void txt(GuiGraphics graphics, String key, int x, int y, int color, Object... args) {
        graphics.drawString(font, Component.translatable(key, args), leftPos + x, topPos + y, color, false);
    }

    private void bar(GuiGraphics graphics, int left, int top, int width, int height,
                     int value, int maximum, int fill) {
        int x = leftPos + left, y = topPos + top;
        graphics.fill(x, y, x + width, y + height, DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF665344);
        int usable = width - 4;
        int filled = maximum <= 0 ? 0 : usable * Math.max(0, Math.min(value, maximum)) / maximum;
        if (filled > 0) graphics.fill(x + 2, y + 2, x + 2 + filled, y + height - 2, fill);
    }

    private void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xFF9A7754);
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + w - 1,
                topPos + y + h - 1, 0xFFF3E5C6);
    }

    private void cart(GuiGraphics graphics) {
        txt(graphics, "gui.sfcdelight.status_showcase", 17, 46, DARK);
        int frameMask = menu.stat(13);
        for (int slot = 0; slot < 12; slot++) {
            int col = slot % 4, row = slot / 4;
            int x = 18 + col * 26, y = 60 + row * 26;
            boolean installed = (frameMask & (1 << slot)) != 0;
            int count = menu.stat(slot + 1);
            graphics.fill(leftPos + x, topPos + y, leftPos + x + 23, topPos + y + 23,
                    installed ? 0xFF8D7558 : 0xFFB9AA91);
            graphics.fill(leftPos + x + 2, topPos + y + 2,
                    leftPos + x + 21, topPos + y + 21,
                    installed ? 0xFFDED2B5 : 0xFFD4CCBB);
            if (installed) {
                graphics.fill(leftPos + x + 4, topPos + y + 4, leftPos + x + 19, topPos + y + 6,
                        count > 0 ? 0xFFBF7B39 : 0xFFB3B5A5);
                String display = count > 0 ? Integer.toString(count) : "—";
                graphics.drawString(font, display, leftPos + x + 8, topPos + y + 9, DARK, false);
            }
        }
        panel(graphics, 128, 48, 64, 91);
        txt(graphics, "gui.sfcdelight.status_baskets", 133, 58, DARK, menu.stat(14));
        txt(graphics, "gui.sfcdelight.status_pepper", 133, 80, MUTED, menu.stat(15));
        txt(graphics, "gui.sfcdelight.status_chili", 133, 103, MUTED, menu.stat(16));
        txt(graphics, "gui.sfcdelight.status_showcase_hint", 17, 142, MUTED);
    }

    private void fryer(GuiGraphics graphics) {
        txt(graphics, "gui.sfcdelight.status_oil", 17, 50, DARK, menu.stat(1));
        bar(graphics, 19, 70, 167, 17, menu.stat(1), 20, GOLD);
        txt(graphics, menu.stat(2) == 1 ?
                "gui.sfcdelight.status_frying" : "gui.sfcdelight.status_fryer_idle",
                18, 102, DARK);
        for (int i = 0; i < 5; i++) {
            int x = 20 + i * 33;
            graphics.fill(leftPos + x, topPos + 121, leftPos + x + 29, topPos + 131,
                    i < menu.stat(3) ? GOLD : 0xFFAA9D87);
            graphics.fill(leftPos + x + 1, topPos + 122, leftPos + x + 28, topPos + 130,
                    i < menu.stat(3) ? 0xFFF0C46C : 0xFFDFD2B6);
        }
        txt(graphics, "gui.sfcdelight.status_fryer_hint", 18, 141, MUTED);
    }

    private void deoiler(GuiGraphics graphics) {
        boolean occupied = menu.stat(4) != 0;
        txt(graphics, "gui.sfcdelight.status_deoiling", 17, 50, DARK,
                occupied ? menu.stat(1) / 20 : 0);
        bar(graphics, 19, 70, 167, 17, occupied ? menu.stat(1) : 0, 160, TEAL);
        String state = !occupied ? "gui.sfcdelight.status_deoiler_empty" :
                menu.stat(3) == 1 ? "gui.sfcdelight.status_deoiler_done" :
                "gui.sfcdelight.status_deoiler_running";
        txt(graphics, state, 18, 104, DARK);
        txt(graphics, "gui.sfcdelight.status_deoiler_hint", 18, 141, MUTED);
    }

    private void preparation(GuiGraphics graphics) {
        txt(graphics, "gui.sfcdelight.status_marination", 17, 49, DARK, menu.stat(1));
        bar(graphics, 19, 65, 167, 14, menu.stat(1), 180, GOLD);
        String state = menu.stat(8) == 0 ? "gui.sfcdelight.status_prep_empty" :
                menu.stat(2) == 1 ? "gui.sfcdelight.status_prep_ready" :
                "gui.sfcdelight.status_prep_marinating";
        txt(graphics, state, 18, 87, DARK);
        txt(graphics, "gui.sfcdelight.status_prep_bowl", 18, 105, MUTED, menu.stat(3));
        txt(graphics, "gui.sfcdelight.status_prep_mix", 101, 105, MUTED, menu.stat(6));
        txt(graphics, "gui.sfcdelight.status_prep_water", 18, 122, MUTED, menu.stat(7));
        txt(graphics, "gui.sfcdelight.status_prep_hint", 18, 142, MUTED);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // Reserve a genuine parchment caption strip and a full-height wood footer.
        // Otherwise the low-positioned instructional labels collide with the frame.
        graphics.fill(leftPos + 9, topPos + 138, leftPos + 197, topPos + 156, 0xFFF1E3C4);
        graphics.fill(leftPos + 8, topPos + 156, leftPos + 197, topPos + 175, 0xFF513628);
        graphics.fill(leftPos + 10, topPos + 158, leftPos + 195, topPos + 173, 0xFF815638);
        graphics.fill(leftPos + 10, topPos + 158, leftPos + 195, topPos + 159, 0xFFB88853);
        String headline = switch (menu.kind()) {
            case EquipmentStatusMenu.FRYER -> "gui.sfcdelight.title_fryer";
            case EquipmentStatusMenu.DEOILER -> "gui.sfcdelight.title_deoiler";
            case EquipmentStatusMenu.PREPARATION -> "gui.sfcdelight.title_preparation";
            default -> "gui.sfcdelight.title_cart";
        };
        txt(graphics, headline, 17, 22, WHITE);
        // An inset icon on the awning: easy to recognise at native Minecraft UI scale.
        ResourceLocation insignia = switch (menu.kind()) {
            case EquipmentStatusMenu.FRYER -> ICON_FRYER;
            case EquipmentStatusMenu.DEOILER -> ICON_DEOILER;
            case EquipmentStatusMenu.PREPARATION -> ICON_PREPARATION;
            default -> ICON_CART;
        };
        graphics.fill(leftPos + 174, topPos + 13, leftPos + 195, topPos + 36, 0xFF56302D);
        graphics.fill(leftPos + 175, topPos + 14, leftPos + 194, topPos + 35, 0xFFB27B50);
        graphics.fill(leftPos + 176, topPos + 15, leftPos + 193, topPos + 34, 0xFF4A3330);
        graphics.blit(insignia, leftPos + 177, topPos + 16, 0, 0, 16, 16, 16, 16);
        switch (menu.kind()) {
            case EquipmentStatusMenu.FRYER -> fryer(graphics);
            case EquipmentStatusMenu.DEOILER -> deoiler(graphics);
            case EquipmentStatusMenu.PREPARATION -> preparation(graphics);
            default -> cart(graphics);
        }
        txt(graphics, "gui.sfcdelight.status_readonly", 16, 160, WHITE);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are drawn in renderBg, with explicit pixel-art alignment.
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
