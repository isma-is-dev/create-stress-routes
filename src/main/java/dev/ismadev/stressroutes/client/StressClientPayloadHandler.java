package dev.ismadev.stressroutes.client;

import dev.ismadev.stressroutes.net.S2CStressHeat;
import dev.ismadev.stressroutes.net.S2CTopologyPart;

/**
 * Client-only payload sink. Kept separate so the payload classes can be loaded on a
 * dedicated server without touching client classes.
 */
public final class StressClientPayloadHandler {

    private StressClientPayloadHandler() {
    }

    public static void handleTopology(S2CTopologyPart payload) {
        StressClientState.acceptTopology(payload);
    }

    public static void handleHeat(S2CStressHeat payload) {
        StressClientState.acceptHeat(payload);
    }
}
