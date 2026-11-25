package dev.ismadev.stressroutes.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.data.NodeKind;
import dev.ismadev.stressroutes.data.StressCell;
import dev.ismadev.stressroutes.data.StressEdge;
import dev.ismadev.stressroutes.data.StressNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rasterises the kinetic network into 128x128 block sections, the same technique Create
 * uses for its train map. Rebuilds are keyed on the geometry hash, so stress values can
 * change every tick without touching the textures.
 */
public final class StressTextureCache {

    public static final StressTextureCache INSTANCE = new StressTextureCache();

    public static final int TILE = 128;

    private final Map<Long, Tile> tiles = new HashMap<>();
    private int signature;
    private boolean built;
    private boolean warnedCap;

    private StressTextureCache() {
    }

    public void ensure(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                       StressClientState.DimData data, double factor,
                       boolean showRoutes, boolean showNodes,
                       StressConfig.LineTheme theme, boolean heatmap) {
        int newSignature = Objects.hash(dimension, data.geometryHash,
            Double.hashCode(factor), theme.ordinal(), showRoutes, showNodes,
            heatmap, heatmap ? data.heatHash : 0);
        if (built && newSignature == signature) {
            return;
        }
        signature = newSignature;
        built = true;

        try {
            if (heatmap) {
                rebuildHeat(data, factor);
            } else {
                rebuild(data, factor, showRoutes, showNodes, theme);
            }
        } catch (Throwable t) {
            StressRoutes.LOGGER.error("Failed to rasterise the stress layer; disabling it for this session", t);
            clear();
            built = false;
        }
    }

    public void render(GuiGraphics graphics, Rect2i bounds, float alpha) {
        if (tiles.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        for (Tile tile : tiles.values()) {
            if (tile.isOutside(bounds)) {
                continue;
            }
            int x = tile.sectionX * TILE;
            int z = tile.sectionZ * TILE;
            tile.uploadIfNeeded();
            graphics.blit(tile.location, x, z, 0.0F, 0.0F, TILE, TILE, TILE, TILE);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.flush();
    }

    public void clear() {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        for (Tile tile : tiles.values()) {
            tile.untouched = true;
            textureManager.release(tile.location);
        }
        tiles.clear();
    }

    private void rebuild(StressClientState.DimData data, double factor,
                         boolean showRoutes, boolean showNodes, StressConfig.LineTheme theme) {
        for (Tile tile : tiles.values()) {
            tile.untouched = true;
        }
        warnedCap = false;

        int main = abgr(255, routeColor(theme));
        int halo = abgr(110, 0x08140C);

        if (showRoutes) {
            for (StressEdge edge : data.edges) {
                rasterEdge(edge, factor, halo, true);
            }
            for (StressEdge edge : data.edges) {
                rasterEdge(edge, factor, main, false);
            }
        }

        if (showNodes) {
            // One pixel per gear/shaft: the line already carries the shape, markers just
            // confirm where the actual blocks are.
            int nodeColor = abgr(255, routeColor(theme));
            for (StressNode node : data.nodes) {
                setPixel(Mth.floor(node.x() / factor), Mth.floor(node.z() / factor), nodeColor);
            }
        }

        prune();
    }

    private void rebuildHeat(StressClientState.DimData data, double factor) {
        for (Tile tile : tiles.values()) {
            tile.untouched = true;
        }
        warnedCap = false;

        int opacity = StressConfig.heatmapOpacity();
        int alpha = (int) Math.round(255.0D * opacity / 100.0D);
        int size = Math.max(1, data.cellSize);
        float max = Math.max(0.0001F, data.maxStress);

        for (StressCell cell : data.cells) {
            double startX = cell.cellX() * (double) size / factor;
            double startZ = cell.cellZ() * (double) size / factor;
            double endX = (cell.cellX() + 1.0D) * size / factor;
            double endZ = (cell.cellZ() + 1.0D) * size / factor;
            int color = heatColor(cell.stress() / max, alpha);
            int x0 = Mth.floor(startX);
            int z0 = Mth.floor(startZ);
            int x1 = Math.max(x0 + 1, Mth.ceil(endX));
            int z1 = Math.max(z0 + 1, Mth.ceil(endZ));
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    setPixel(x, z, color);
                }
            }
        }

        prune();
    }

    /** Green -&gt; yellow -&gt; orange -&gt; red ramp. */
    private static int heatColor(float ratio, int alpha) {
        float t = Mth.clamp(ratio, 0.0F, 1.0F);
        float red;
        float green;
        if (t < 0.5F) {
            float k = t * 2.0F;
            red = Mth.lerp(k, 60.0F, 245.0F);
            green = Mth.lerp(k, 210.0F, 225.0F);
        } else {
            float k = (t - 0.5F) * 2.0F;
            red = 245.0F;
            green = Mth.lerp(k, 225.0F, 60.0F);
        }
        return abgr(alpha, (Math.round(red) << 16) | (Math.round(green) << 8) | 60);
    }

    private void rasterEdge(StressEdge edge, double factor, int color, boolean halo) {
        boolean dashed = StressConfig.dashedRoutes();
        rasterLine(edge.startX() / factor, edge.startZ() / factor,
            edge.endX() / factor, edge.endZ() / factor, color, dashed, halo);
    }

    private void rasterLine(double x1, double z1, double x2, double z2, int color,
                            boolean dashed, boolean halo) {
        int ix1 = Mth.floor(x1);
        int iz1 = Mth.floor(z1);
        int ix2 = Mth.floor(x2);
        int iz2 = Mth.floor(z2);

        int dx = Math.abs(ix2 - ix1);
        int dz = Math.abs(iz2 - iz1);
        int sx = ix1 < ix2 ? 1 : -1;
        int sz = iz1 < iz2 ? 1 : -1;
        int err = dx - dz;
        int step = 0;

        while (true) {
            boolean draw = !dashed || (step % 8) < 5;
            if (draw) {
                if (halo) {
                    // Four neighbours only: a thin outline instead of a fat 3x3 slab.
                    setPixel(ix1 - 1, iz1, color);
                    setPixel(ix1 + 1, iz1, color);
                    setPixel(ix1, iz1 - 1, color);
                    setPixel(ix1, iz1 + 1, color);
                } else {
                    setPixel(ix1, iz1, color);
                }
            }
            if (ix1 == ix2 && iz1 == iz2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                ix1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                iz1 += sz;
            }
            step++;
        }
    }

    private void setPixel(int x, int z, int argb) {
        Tile tile = getOrCreate(Math.floorDiv(x, TILE), Math.floorDiv(z, TILE));
        if (tile == null) {
            return;
        }
        int localX = Math.floorMod(x, TILE);
        int localZ = Math.floorMod(z, TILE);
        NativeImage image = tile.image();
        if (image != null) {
            image.setPixelRGBA(localX, localZ, argb);
        }
    }

    private Tile getOrCreate(int sectionX, int sectionZ) {
        long key = ((long) sectionX << 32) ^ (sectionZ & 0xFFFFFFFFL);
        Tile existing = tiles.get(key);
        if (existing != null) {
            existing.untouched = false;
            return existing;
        }

        int maxSections = StressConfig.maxMapSections();
        if (tiles.size() >= maxSections) {
            if (!warnedCap) {
                warnedCap = true;
                StressRoutes.LOGGER.warn("Stress map layer hit the {} section cap; "
                    + "raise client.maxMapSections if your factory is bigger than that.", maxSections);
            }
            return null;
        }

        Tile tile = new Tile(sectionX, sectionZ);
        tiles.put(key, tile);
        return tile;
    }

    private void prune() {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        tiles.values().removeIf(tile -> {
            if (!tile.untouched) {
                return false;
            }
            textureManager.release(tile.location);
            return true;
        });
    }

    /** Packs an 0xRRGGBB colour plus alpha into the ABGR layout NativeImage expects. */
    private static int abgr(int alpha, int rgb) {
        return ABGR32.fromArgb32((alpha << 24) | (rgb & 0xFFFFFF));
    }
    private static int routeColor(StressConfig.LineTheme theme) {
        return switch (theme) {
            case AMBER -> 0xE8AA44;
            case TEAL -> 0x2FBFAE;
            default -> 0x3DDC6A;
        };
    }

    private static final class Tile {

        private final int sectionX;
        private final int sectionZ;
        private final DynamicTexture texture;
        private final ResourceLocation location;
        private boolean untouched;
        private boolean requiresUpload = true;

        private Tile(int sectionX, int sectionZ) {
            this.sectionX = sectionX;
            this.sectionZ = sectionZ;
            this.texture = new DynamicTexture(TILE, TILE, true);
            this.texture.setFilter(true, false);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            this.location = textureManager.register(
                "stressroutes_map/" + sectionX + "_" + sectionZ, texture);
            this.untouched = false;
        }

        private NativeImage image() {
            this.untouched = false;
            this.requiresUpload = true;
            return texture.getPixels();
        }

        private void uploadIfNeeded() {
            if (requiresUpload) {
                requiresUpload = false;
                texture.upload();
            }
        }

        private boolean isOutside(Rect2i bounds) {
            int minX = sectionX * TILE;
            int minZ = sectionZ * TILE;
            return minX + TILE < bounds.getX()
                || minX > bounds.getX() + bounds.getWidth()
                || minZ + TILE < bounds.getY()
                || minZ > bounds.getY() + bounds.getHeight();
        }
    }
}
