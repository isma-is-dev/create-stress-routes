package dev.ismadev.stressroutes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Screen-space markers for the stress overlay.
 *
 * <p>Only drivetrain blocks are drawn, as a single map pixel each: gears and shafts are the
 * read-worthy part of a kinetic network, machines and belts are not shown.</p>
 */
public final class StressGlyphs {

    private StressGlyphs() {
    }

    public static void node(GuiGraphics graphics, double x, double y, int kind, boolean detailed) {
        int size = detailed ? 1 : 1;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x - size / 2.0D, y - size / 2.0D, 0.0D);
        graphics.fill(0, 0, 1, 1, color(kind));
        pose.popPose();
    }

    public static int color(int kind) {
        return 0xFF3DDC6A;
    }

    public static void box(GuiGraphics graphics, double x, double y, int size, int fill, int outline) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x - size / 2.0D, y - size / 2.0D, 0.0D);
        graphics.fill(-1, -1, size + 1, size + 1, outline);
        graphics.fill(0, 0, size, size, fill);
        pose.popPose();
    }
}
