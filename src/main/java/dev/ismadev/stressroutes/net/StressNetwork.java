package dev.ismadev.stressroutes.net;

import dev.ismadev.stressroutes.StressRoutes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class StressNetwork {

    /** Bumped whenever a payload layout changes. */
    private static final String PROTOCOL_VERSION = "2";

    private StressNetwork() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(StressNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(C2SRequestStress.TYPE, C2SRequestStress.STREAM_CODEC, C2SRequestStress::handle);
        registrar.playToClient(S2CTopologyPart.TYPE, S2CTopologyPart.STREAM_CODEC, S2CTopologyPart::handle);
        registrar.playToClient(S2CStressHeat.TYPE, S2CStressHeat.STREAM_CODEC, S2CStressHeat::handle);
    }

    public static String protocolVersion() {
        return PROTOCOL_VERSION;
    }
}
