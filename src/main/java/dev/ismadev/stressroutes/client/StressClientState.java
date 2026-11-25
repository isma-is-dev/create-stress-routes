package dev.ismadev.stressroutes.client;

import dev.ismadev.stressroutes.data.StressCell;
import dev.ismadev.stressroutes.data.StressEdge;
import dev.ismadev.stressroutes.data.StressNode;
import dev.ismadev.stressroutes.net.S2CStressHeat;
import dev.ismadev.stressroutes.net.S2CTopologyPart;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side mirror of the kinetic network. Mutations happen on the client thread.
 */
public final class StressClientState {

    /** Dimension currently displayed by Xaero's world map, updated by the overlay. */
    @Nullable
    public static ResourceKey<Level> viewedDimension;

    private static final Map<ResourceKey<Level>, DimData> DIMENSIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, PendingTopology> PENDING = new HashMap<>();

    private StressClientState() {
    }

    public static DimData get(ResourceKey<Level> dimension) {
        return DIMENSIONS.computeIfAbsent(dimension, key -> new DimData());
    }

    @Nullable
    public static DimData getIfPresent(ResourceKey<Level> dimension) {
        return DIMENSIONS.get(dimension);
    }

    public static void clear() {
        DIMENSIONS.clear();
        PENDING.clear();
    }

    public static void acceptTopology(S2CTopologyPart part) {
        ResourceKey<Level> dimension = part.dimension();
        PendingTopology pending = PENDING.get(dimension);
        if (pending == null || pending.requestId != part.requestId() || pending.parts != part.parts()) {
            pending = new PendingTopology(part.requestId(), part.parts());
            PENDING.put(dimension, pending);
        }

        pending.edges.addAll(part.edges());
        pending.nodes.addAll(part.nodes());
        pending.received++;

        if (pending.received >= pending.parts) {
            DimData data = get(dimension);
            data.geometryHash = part.geometryHash();
            data.edges = List.copyOf(pending.edges);
            data.nodes = List.copyOf(pending.nodes);
            data.networkCount = part.networkCount();
            PENDING.remove(dimension);
        }
    }

    public static void acceptHeat(S2CStressHeat packet) {
        DimData data = get(packet.dimension());
        data.cells = List.copyOf(packet.cells());
        data.cellSize = Math.max(1, packet.cellSize());
        data.maxStress = packet.maxStress();
        data.heatHash = packet.heatHash();
        data.cellIndex.clear();
        for (StressCell cell : data.cells) {
            data.cellIndex.put(cellKey(cell.cellX(), cell.cellZ()), cell);
        }
    }

    public static long cellKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static final class PendingTopology {
        final int requestId;
        final int parts;
        int received;
        final List<StressEdge> edges = new ArrayList<>();
        final List<StressNode> nodes = new ArrayList<>();

        PendingTopology(int requestId, int parts) {
            this.requestId = requestId;
            this.parts = Math.max(1, parts);
        }
    }

    public static final class DimData {
        public int geometryHash;
        public List<StressEdge> edges = List.of();
        public List<StressNode> nodes = List.of();
        public int networkCount;
        public List<StressCell> cells = List.of();
        public final Map<Long, StressCell> cellIndex = new HashMap<>();
        public int cellSize = 8;
        public float maxStress;
        public int heatHash;

        public boolean isEmpty() {
            return edges.isEmpty() && nodes.isEmpty();
        }
    }
}
