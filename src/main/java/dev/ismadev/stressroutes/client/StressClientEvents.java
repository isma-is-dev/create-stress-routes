package dev.ismadev.stressroutes.client;

import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.net.C2SRequestStress;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Keeps the server streaming while the world map is open, and forwards clicks on the
 * stress map widget.
 */
@EventBusSubscriber(modid = StressRoutes.MODID, value = Dist.CLIENT)
public final class StressClientEvents {

    private static final int REQUEST_INTERVAL_TICKS = 20;

    private static int ticks;
    private static boolean mapClassChecked;
    private static Class<?> mapScreenClass;
    private static boolean serverLacksMod;

    private StressClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || serverLacksMod) {
            return;
        }
        if (!isWorldMapOpen(mc)) {
            return;
        }
        if (!StressConfig.overlayEnabled()) {
            return;
        }
        if (ticks % REQUEST_INTERVAL_TICKS != 0) {
            return;
        }

        ResourceKey<Level> dimension = StressClientState.viewedDimension;
        if (dimension == null) {
            dimension = mc.level.dimension();
        }
        try {
            PacketDistributor.sendToServer(new C2SRequestStress(dimension, StressConfig.heatmapEnabled()));
        } catch (Throwable t) {
            // A server without the mod cannot handle our channel. Stop trying so the log
            // does not fill up once per second.
            serverLacksMod = true;
            StressRoutes.LOGGER.info("Stress Routes is not installed on this server; "
                + "the map overlay stays inactive.");
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != 1 || event.getButton() != 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!isWorldMapOpen(mc)) {
            return;
        }
        Window window = mc.getWindow();
        if (window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) {
            return;
        }
        // Raw input fires before GuiMap handles the click, which is what makes buttons that
        // overlap the map area clickable (the map would otherwise eat the event).
        double mouseX = mc.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
        if (StressMapWidget.heatClicked(mouseX, mouseY, event.getButton())
            || StressMapWidget.mouseClicked(mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
        }
    }

    public static boolean isWorldMapOpen(Minecraft mc) {
        return mc.screen != null && isWorldMapScreen(mc.screen);
    }

    private static boolean isWorldMapScreen(Object screen) {
        if (screen == null) {
            return false;
        }
        Class<?> type = worldMapClass();
        return type != null && type.isInstance(screen);
    }

    private static Class<?> worldMapClass() {
        if (!mapClassChecked) {
            mapClassChecked = true;
            if (ModList.get().isLoaded("xaeroworldmap")) {
                try {
                    mapScreenClass = Class.forName("xaero.map.gui.GuiMap");
                } catch (Throwable t) {
                    mapScreenClass = null;
                }
            }
        }
        return mapScreenClass;
    }

    @SubscribeEvent
    public static void onClientStopped(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        StressClientState.clear();
        StressClientState.viewedDimension = null;
    }
}
