package dev.ismadev.stressroutes.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.data.NodeKind;
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
                       StressConfig.LineTheme theme) {
        int newSignature = Objects.hash(dimension, data.geometryHash,
            Double.hashCode(factor), theme.ordinal(), showRoutes, showNodes);
        if (built && newSignature == signature) {
            return;
        }
        signature = newSignature;
        built = true;

        try {
            rebuild(data, factor, showRoutes, showNodes, theme);
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

        int main = ABGR32.color(255, routeColor(theme));
        int halo = ABGR32.color(170, 8, 20, 12);

        if (showRoutes) {
            for (StressEdge edge : data.edges) {
                rasterEdge(edge, factor, halo, true);
            }
            for (StressEdge edge : data.edges) {
                rasterEdge(edge, factor, main, false);
            }
        }

        if (showNodes) {
            int outline = ABGR32.color(180, 8, 20, 12);
            for (StressNode node : data.nodes) {
                int x = Mth.floor(node.x() / factor);
                int z = Mth.floor(node.z() / factor);
                int color = ABGR32.color(255, nodeColor(node.kind()));
                setPixel(x - 1, z - 1, outline);
                setPixel(x + 2, z - 1, outline);
                setPixel(x - 1, z + 2, outline);
                setPixel(x + 2, z + 2, outline);
                setPixel(x, z, color);
                setPixel(x + 1, z, color);
                setPixel(x, z + 1, color);
                setPixel(x + 1, z + 1, color);
            }
        }

        prune();
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
                    for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            setPixel(ix1 + ox, iz1 + oz, color);
                        }
                    }
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

    private static int routeColor(StressConfig.LineTheme theme) {
        return switch (theme) {
            case AMBER -> 0xE8AA44;
            case TEAL -> 0x2FBFAE;
            default -> 0x3DDC6A;
        };
    }

    private static int nodeColor(int kind) {
        return switch (kind) {
            case NodeKind.SOURCE -> 0x6BFF9E;
            case NodeKind.CONSUMER -> 0xC8E85A;
            case NodeKind.OVERSTRESSED -> 0xFF4D4D;
            default -> 0x2E8F52;
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
