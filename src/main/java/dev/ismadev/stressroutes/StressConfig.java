package dev.ismadev.stressroutes;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for the stress overlay.
 *
 * <ul>
 *     <li>{@code stressroutes-client.toml} &mdash; purely visual, per player.</li>
 *     <li>{@code stressroutes-server.toml} &mdash; scan and network budget.</li>
 * </ul>
 */
public final class StressConfig {

    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();

        Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
    }

    private StressConfig() {
    }

    public enum LineTheme {
        /** Kinetic green, the default. */
        GREEN,
        /** Amber, for players who already use green elsewhere. */
        AMBER,
        /** Teal. */
        TEAL
    }

    private static <T> T safe(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            T loaded = value.get();
            return loaded == null ? fallback : loaded;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean overlayEnabled() {
        return safe(CLIENT.enabled, true);
    }

    public static boolean showRoutes() {
        return safe(CLIENT.showRoutes, true);
    }

    public static boolean showNodes() {
        return safe(CLIENT.showNodes, true);
    }

    public static boolean dashedRoutes() {
        return safe(CLIENT.dashedRoutes, false);
    }

    public static LineTheme lineTheme() {
        return safe(CLIENT.lineTheme, LineTheme.GREEN);
    }

    public static int routeOpacity() {
        return Math.max(0, Math.min(100, safe(CLIENT.routeOpacity, 100)));
    }

    public static int maxMapSections() {
        return Math.max(16, safe(CLIENT.maxMapSections, 512));
    }

    public static int widgetY() {
        return Math.max(0, safe(CLIENT.widgetY, 64));
    }

    public static int syncIntervalTicks() {
        return Math.max(1, safe(SERVER.syncIntervalTicks, 10));
    }

    public static int maxNodes() {
        return Math.max(64, safe(SERVER.maxNodes, 16384));
    }

    public static int maxDirtyChunksPerTick() {
        return Math.max(1, safe(SERVER.maxDirtyChunksPerTick, 4));
    }

    public static final class Client {

        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue showRoutes;
        public final ModConfigSpec.BooleanValue showNodes;
        public final ModConfigSpec.BooleanValue dashedRoutes;
        public final ModConfigSpec.EnumValue<LineTheme> lineTheme;
        public final ModConfigSpec.IntValue routeOpacity;
        public final ModConfigSpec.IntValue maxMapSections;
        public final ModConfigSpec.IntValue widgetY;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("Visual settings for the stress overlay on Xaero's World Map.").push("visuals");

            this.enabled = builder
                .comment("Master switch. Can also be toggled from the map widget.")
                .define("enabled", true);

            this.showRoutes = builder
                .comment("Draw the stress network as lines.")
                .define("showRoutes", true);

            this.showNodes = builder
                .comment("Draw sources, machines and transmission blocks as markers.")
                .define("showNodes", true);

            this.dashedRoutes = builder
                .comment("Dashed lines. Off by default: stress networks read better as solid lines.")
                .define("dashedRoutes", false);

            this.lineTheme = builder
                .comment("Colour palette for the stress network.")
                .defineEnum("lineTheme", LineTheme.GREEN);

            this.routeOpacity = builder
                .comment("Opacity of the network lines, 0-100.")
                .defineInRange("routeOpacity", 100, 0, 100);

            this.maxMapSections = builder
                .comment("Maximum number of 128x128 map sections kept in memory (64 KiB each).",
                    "Large factories can span a lot of sections; 512 is roughly 32 MiB of VRAM.")
                .defineInRange("maxMapSections", 512, 16, 4096);

            this.widgetY = builder
                .comment("Vertical position of the Stress button on the map, in GUI pixels.",
                    "Postal Routes uses 48 and Fluid Routes 80, so 64 keeps the three stacked.")
                .defineInRange("widgetY", 64, 0, 400);

            builder.pop();
        }
    }

    public static final class Server {

        public final ModConfigSpec.IntValue syncIntervalTicks;
        public final ModConfigSpec.IntValue maxNodes;
        public final ModConfigSpec.IntValue maxDirtyChunksPerTick;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("Server side scan limits for the stress overlay.").push("scan");

            this.syncIntervalTicks = builder
                .comment("How often (in ticks) the network is rebuilt while a player has the map open.",
                    "10 ticks (0.5 s) is plenty: kinetic networks do not change shape often.")
                .defineInRange("syncIntervalTicks", 10, 1, 100);

            this.maxNodes = builder
                .comment("Maximum number of kinetic blocks reported per dimension.")
                .defineInRange("maxNodes", 16384, 128, 65536);

            this.maxDirtyChunksPerTick = builder
                .comment("Chunks rescanned per tick after a block placement or removal.")
                .defineInRange("maxDirtyChunksPerTick", 4, 1, 32);

            builder.pop();
        }
    }
}
