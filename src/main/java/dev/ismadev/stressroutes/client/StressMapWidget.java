package dev.ismadev.stressroutes.client;

import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Map button for the stress overlay. Its vertical position is configurable so Postal
 * Routes (48), Stress Routes (64) and Fluid Routes (80) stack instead of overlapping.
 */
public final class StressMapWidget {

    public static final int X = 3;
    public static final int WIDTH = 66;
    public static final int HEIGHT = 14;
    public static final int HEAT_X = X;
    public static final int HEAT_WIDTH = WIDTH;
    public static final int HEAT_GAP = 2;

    private static final int BACKGROUND_ON = 0xE01C1408;
    private static final int BACKGROUND_OFF = 0xE0181818;
    private static final int BORDER_ON = 0xFF3DDC6A;
    private static final int BORDER_OFF = 0xFF5E5E5E;
    private static final int TEXT_ON = 0xFFD8FFE4;
    private static final int TEXT_OFF = 0xFF9A9A9A;

    private StressMapWidget() {
    }

    public static boolean isEnabled() {
        return StressConfig.overlayEnabled();
    }

    private static int y() {
        return StressConfig.widgetY();
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY, int networkCount) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        boolean enabled = isEnabled();
        int y = y();
        boolean hovered = isHovered(mouseX, mouseY);

        int background = enabled ? BACKGROUND_ON : BACKGROUND_OFF;
        int border = enabled ? (hovered ? 0xFF8CFFB0 : BORDER_ON) : (hovered ? 0xFF8A8A8A : BORDER_OFF);

        graphics.fill(X, y, X + WIDTH, y + HEIGHT, background);
        graphics.fill(X, y, X + WIDTH, y + 1, border);
        graphics.fill(X, y + HEIGHT - 1, X + WIDTH, y + HEIGHT, border);
        graphics.fill(X, y, X + 1, y + HEIGHT, border);
        graphics.fill(X + WIDTH - 1, y, X + WIDTH, y + HEIGHT, border);

        // Small "network" glyph: a 3x3 dot cluster.
        StressGlyphs.box(graphics, X + 9, y + 7, 3, 0xFF3DDC6A, 0xC0100C04);

        graphics.drawString(font, Component.translatable("stressroutes.widget.label"),
            X + 16, y + 3, enabled ? TEXT_ON : TEXT_OFF, false);

        if (enabled && networkCount > 0) {
            String count = networkCount > 99 ? "99+" : Integer.toString(networkCount);
            int width = font.width(count);
            graphics.drawString(font, count, X + WIDTH - width - 4, y + 3, 0xFF3DDC6A, false);
        }

        if (hovered) {
            graphics.renderComponentTooltip(font, tooltip(enabled), mouseX, mouseY);
        }
    }

    public static void renderHeat(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        boolean heat = StressConfig.heatmapEnabled();
        int y = y() + HEIGHT + HEAT_GAP;
        boolean hovered = isHeatHovered(mouseX, mouseY);

        int background = heat ? 0xE02A1406 : 0xE0181818;
        int border = heat ? (hovered ? 0xFFFFB066 : 0xFFE86A2A) : (hovered ? 0xFF8A8A8A : 0xFF5E5E5E);

        graphics.fill(HEAT_X, y, HEAT_X + HEAT_WIDTH, y + HEIGHT, background);
        graphics.fill(HEAT_X, y, HEAT_X + HEAT_WIDTH, y + 1, border);
        graphics.fill(HEAT_X, y + HEIGHT - 1, HEAT_X + HEAT_WIDTH, y + HEIGHT, border);
        graphics.fill(HEAT_X, y, HEAT_X + 1, y + HEIGHT, border);
        graphics.fill(HEAT_X + HEAT_WIDTH - 1, y, HEAT_X + HEAT_WIDTH, y + HEIGHT, border);

        // Tiny heat ramp as the icon.
        StressGlyphs.box(graphics, HEAT_X + 8, y + 7, 3, heat ? 0xFFFF6A3D : 0xFF7A7A7A, 0xC0100C04);

        graphics.drawString(font, Component.translatable("stressroutes.widget.heat"),
            HEAT_X + 15, y + 3, heat ? 0xFFFFD6B0 : 0xFF9A9A9A, false);

        if (hovered) {
            graphics.renderComponentTooltip(font, List.of(
                Component.translatable("stressroutes.widget.heat_title").withStyle(ChatFormatting.GOLD),
                Component.translatable(heat ? "stressroutes.widget.heat_on" : "stressroutes.widget.heat_off")
                    .withStyle(heat ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                Component.translatable("stressroutes.widget.heat_hint").withStyle(ChatFormatting.DARK_GRAY)
            ), mouseX, mouseY);
        }
    }

    public static boolean isHeatHovered(double mouseX, double mouseY) {
        int y = y() + HEIGHT + HEAT_GAP;
        return mouseX >= HEAT_X && mouseX < HEAT_X + HEAT_WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public static boolean heatClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHeatHovered(mouseX, mouseY)) {
            return false;
        }
        try {
            StressConfig.CLIENT.heatmapEnabled.set(!StressConfig.heatmapEnabled());
        } catch (Throwable t) {
            StressRoutes.LOGGER.warn("Could not toggle the stress heatmap", t);
        }
        return true;
    }

    private static List<Component> tooltip(boolean enabled) {
        return List.of(
            Component.translatable("stressroutes.widget.title").withStyle(ChatFormatting.GREEN),
            Component.translatable(enabled ? "stressroutes.widget.on" : "stressroutes.widget.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY),
            Component.translatable("stressroutes.widget.hint").withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    public static boolean isHovered(double mouseX, double mouseY) {
        int y = y();
        return mouseX >= X && mouseX < X + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isHovered(mouseX, mouseY)) {
            return false;
        }
        toggle();
        return true;
    }

    public static void toggle() {
        try {
            boolean value = !StressConfig.overlayEnabled();
            StressConfig.CLIENT.enabled.set(value);
        } catch (Throwable t) {
            StressRoutes.LOGGER.warn("Could not toggle the stress overlay", t);
        }
    }
}
