package dev.ismadev.stressroutes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.data.NodeKind;
import dev.ismadev.stressroutes.data.StressCell;
import dev.ismadev.stressroutes.data.StressNode;
import dev.ismadev.stressroutes.mixin.GuiMapAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the kinetic/stress network on Xaero's World Map.
 */
public final class StressMapOverlay {

    private static final double PICK_RADIUS = 3.5D;

    private static boolean failed;

    private StressMapOverlay() {
    }

    public static void onRender(GuiGraphics graphics, GuiMap map, int mouseX, int mouseY, float partialTicks) {
        if (failed) {
            return;
        }
        try {
            GuiMapAccessor accessor = (GuiMapAccessor) map;
            MapProcessor processor = accessor.stress$getMapProcessor();
            MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
            MapDimension mapDimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
            if (mapDimension == null) {
                return;
            }

            ResourceKey<Level> dimension = mapDimension.getDimId();
            if (dimension == null) {
                return;
            }
            StressClientState.viewedDimension = dimension;

            StressClientState.DimData data = StressClientState.getIfPresent(dimension);
            int networks = data == null ? 0 : data.networkCount;
            StressMapWidget.render(graphics, mouseX, mouseY, networks);
            StressMapWidget.renderHeat(graphics, mouseX, mouseY);

            if (!StressMapWidget.isEnabled()) {
                return;
            }
            if (data == null || data.isEmpty()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            double cameraX = accessor.stress$getCameraX();
            double cameraZ = accessor.stress$getCameraZ();
            double mapScale = accessor.stress$getScale();
            if (!Double.isFinite(mapScale) || mapScale <= 0.0D) {
                return;
            }

            double guiScale = mc.getWindow().getGuiScale();
            if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
                guiScale = 1.0D;
            }
            double scale = mapScale / guiScale;
            if (!Double.isFinite(scale) || scale <= 0.0D) {
                return;
            }
            double factor = dimensionDivisor(processor, mapDimension, mc);
            if (!Double.isFinite(factor) || factor < 1.0E-4D) {
                factor = 1.0D;
            }

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate((float) map.width / 2.0F, (float) map.height / 2.0F, 0.0F);
            pose.scale((float) scale, (float) scale, 1.0F);
            pose.translate(-cameraX, -cameraZ, 0.0D);

            Rect2i bounds = visibleBounds(map.width, map.height, scale, cameraX, cameraZ);
            double mouseMapX = (mouseX - (double) map.width / 2.0D) / scale + cameraX;
            double mouseMapZ = (mouseY - (double) map.height / 2.0D) / scale + cameraZ;

            int opacity = Mth.clamp(StressConfig.routeOpacity(), 0, 100);
            float alpha = opacity / 100.0F;
            boolean heatmap = StressConfig.heatmapEnabled() && !data.cells.isEmpty();

            StressTextureCache.INSTANCE.ensure(dimension, data, factor,
                StressConfig.showRoutes(), StressConfig.showNodes(), StressConfig.lineTheme(), heatmap);
            StressTextureCache.INSTANCE.render(graphics, bounds, heatmap ? 1.0F : alpha);

            RenderSystem.enableBlend();
            StressNode hovered = null;
            StressCell hoveredCell = null;
            if (heatmap) {
                hoveredCell = cellAt(data, mouseMapX * factor, mouseMapZ * factor);
            } else if (StressConfig.showNodes()) {
                hovered = drawNodes(graphics, data, factor, scale, bounds, mouseMapX, mouseMapZ);
            }

            pose.popPose();
            graphics.flush();

            List<Component> tooltip = hoveredCell != null
                ? heatTooltip(hoveredCell, data.cellSize)
                : (hovered != null ? tooltipFor(hovered) : List.of());
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(mc.font, tooltip, mouseX, mouseY);
            }
        } catch (Throwable t) {
            failed = true;
            StressRoutes.LOGGER.error("Disabling the stress map overlay after an unexpected error", t);
        }
    }

    private static Rect2i visibleBounds(int width, int height, double scale, double cameraX, double cameraZ) {
        int margin = 32;
        int x = Mth.floor((double) (-width) / 2.0D / scale + cameraX) - margin;
        int z = Mth.floor((double) (-height) / 2.0D / scale + cameraZ) - margin;
        int w = Mth.floor((double) width / scale) + margin * 2;
        int h = Mth.floor((double) height / scale) + margin * 2;
        return new Rect2i(x, z, w, h);
    }

    private static double dimensionDivisor(MapProcessor processor, MapDimension mapDimension, Minecraft mc) {
        try {
            Registry<DimensionType> registry = processor.getWorldDimensionTypeRegistry();
            if (registry != null && mc.player != null) {
                double div = mapDimension.calculateDimDiv(registry, mc.player.level().dimensionType());
                if (div > 1.0E-4D) {
                    return div;
                }
            }
        } catch (Throwable ignored) {
            // Identity fallback; drawing unshifted is better than not drawing.
        }
        return 1.0D;
    }

    @Nullable
    private static StressNode drawNodes(GuiGraphics graphics, StressClientState.DimData data, double factor,
                                        double scale, Rect2i bounds, double mouseMapX, double mouseMapZ) {
        StressNode hovered = null;
        boolean detailed = scale >= 0.35D;

        for (StressNode node : data.nodes) {
            double x = node.x() / factor;
            double z = node.z() / factor;
            if (!bounds.contains(Mth.floor(x), Mth.floor(z))) {
                continue;
            }

            StressGlyphs.node(graphics, x, z, node.kind(), detailed);

            if (hovered == null
                && Math.abs(mouseMapX - x) <= PICK_RADIUS
                && Math.abs(mouseMapZ - z) <= PICK_RADIUS) {
                hovered = node;
            }
        }
        return hovered;
    }

    @Nullable
    private static StressCell cellAt(StressClientState.DimData data, double blockX, double blockZ) {
        if (data.cellSize <= 0) {
            return null;
        }
        int cellX = (int) Math.floor(blockX / data.cellSize);
        int cellZ = (int) Math.floor(blockZ / data.cellSize);
        return data.cellIndex.get(StressClientState.cellKey(cellX, cellZ));
    }

    private static List<Component> heatTooltip(StressCell cell, int cellSize) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable("stressroutes.tooltip.heat_title")
            .withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("stressroutes.tooltip.heat_cell",
                Math.round(cell.stress()), cell.machines())
            .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("stressroutes.tooltip.heat_area", cellSize, cellSize)
            .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static List<Component> tooltipFor(StressNode node) {
        List<Component> lines = new ArrayList<>(4);
        lines.add(Component.translatable(node.label()).withStyle(ChatFormatting.WHITE));
        if (node.capacity() > 0.0F) {
            int percent = Math.round(node.load() * 100.0F);
            lines.add(Component.translatable("stressroutes.tooltip.network",
                    Math.round(node.stress()), Math.round(node.capacity()), percent)
                .withStyle(percent > 100 ? ChatFormatting.RED : ChatFormatting.GREEN));
        } else if (node.kind() == NodeKind.TRANSMISSION) {
            lines.add(Component.translatable("stressroutes.tooltip.no_network")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }


}
