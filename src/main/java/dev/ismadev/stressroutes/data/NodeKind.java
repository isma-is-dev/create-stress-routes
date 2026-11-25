package dev.ismadev.stressroutes.data;

/**
 * Node categories for the stress overlay.
 */
public final class NodeKind {

    /** A block that provides rotation capacity (motor, water wheel, windmill...). */
    public static final int SOURCE = 0;
    /** A block that consumes stress (machines, drills, mixers...). */
    public static final int CONSUMER = 1;
    /** A block that only transmits rotation (shafts, cogs, gearboxes). */
    public static final int TRANSMISSION = 2;
    /** Network is over capacity at snapshot time. */
    public static final int OVERSTRESSED = 3;

    private NodeKind() {
    }

    public static String translationKey(int kind) {
        return switch (kind) {
            case SOURCE -> "stressroutes.node.source";
            case CONSUMER -> "stressroutes.node.consumer";
            case OVERSTRESSED -> "stressroutes.node.overstressed";
            default -> "stressroutes.node.transmission";
        };
    }
}
