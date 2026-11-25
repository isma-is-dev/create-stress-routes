package dev.ismadev.stressroutes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tiny procedurally drawn markers for kinetic blocks. Sources are the brightest, pure
 * transmission blocks are a small dim dot, so a factory reads at a glance without the
 * overlay covering the base.
 */
public final class StressGlyphs {

    private static final int OUTLINE = 0xC0100C04;

    private StressGlyphs() {
    }

    public static void node(GuiGraphics graphics, double x, double y, int kind, boolean detailed) {
        int size = switch (kind) {
            case 0 -> detailed ? 3 : 2; // source
            case 1 -> detailed ? 3 : 2; // consumer
            case 3 -> 3;                // overstressed
            default -> detailed ? 2 : 1; // transmission
        };
        int color = color(kind);
        box(graphics, x, y, Math.max(1, size), color, OUTLINE);
    }

    public static int color(int kind) {
        return switch (kind) {
            case 0 -> 0xFF6BFF9E; // source
            case 1 -> 0xFFC8E85A; // consumer
            case 3 -> 0xFFFF4D4D; // overstressed
            default -> 0xFF2E8F52; // transmission
        };
    }

    /**
     * Filled square with a 1px rim, centred on the point.
     */
    public static void box(GuiGraphics graphics, double x, double y, int size, int fill, int outline) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x - size / 2.0D, y - size / 2.0D, 0.0D);
        graphics.fill(-1, -1, size + 1, size + 1, outline);
        graphics.fill(0, 0, size, size, fill);
        pose.popPose();
    }
}
