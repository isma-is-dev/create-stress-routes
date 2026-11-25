package dev.ismadev.stressroutes;

import dev.ismadev.stressroutes.net.StressNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create: Stress Routes.
 *
 * <p>Client-side visualisation of Create's package logistics on Xaero's World Map:
 * chain conveyor routes, frogports, postboxes and live package traffic. The mod adds
 * no blocks or items &mdash; it only reads Create's block entity data on the server and
 * mirrors it to the client for rendering.</p>
 */
@Mod(StressRoutes.MODID)
public final class StressRoutes {

    public static final String MODID = "stressroutes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create: Stress Routes");

    public StressRoutes(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, StressConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, StressConfig.SERVER_SPEC);
        StressNetwork.init(modBus);
    }
}
