package dev.ismadev.stressroutes.server;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ismadev.stressroutes.StressConfig;
import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.data.NodeKind;
import dev.ismadev.stressroutes.data.StressEdge;
import dev.ismadev.stressroutes.data.StressNode;
import dev.ismadev.stressroutes.data.StressSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks every kinetic block of one server level and rebuilds the stress graph on demand.
 *
 * <p>An edge is only drawn between two blocks when Create's own
 * {@link RotationPropagator#isConnected} agrees, so gearboxes, cased shafts, cog meshing and
 * chain drives are all respected instead of a naive "touching blocks" guess.</p>
 */
public final class StressTracker {

    private static final Direction[] POSITIVE_DIRECTIONS = {
        Direction.EAST, Direction.UP, Direction.SOUTH
    };
    private static final float[] NO_STATS = {0.0F, 0.0F};

    private final ServerLevel level;

    private final Map<ChunkPosKey, Set<BlockPos>> byChunk = new HashMap<>();
    private final Set<BlockPos> positions = new HashSet<>();
    private final Set<ChunkPosKey> dirty = new HashSet<>();

    private StressSnapshot cachedSnapshot;
    private long cachedTick = Long.MIN_VALUE;
    private boolean seeded;
    private int ambientTicks;

    public StressTracker(ServerLevel level) {
        this.level = level;
    }

    public ServerLevel level() {
        return level;
    }

    public int trackedBlocks() {
        return positions.size();
    }

    // ------------------------------------------------------------------
    // Discovery
    // ------------------------------------------------------------------

    public void onChunkLoad(LevelChunk chunk) {
        if (chunk.getLevel() != level) {
            return;
        }
        scan(chunk);
    }

    public void onChunkUnload(LevelChunk chunk) {
        ChunkPosKey key = ChunkPosKey.of(chunk.getPos());
        Set<BlockPos> old = byChunk.remove(key);
        if (old != null) {
            positions.removeAll(old);
        }
        dirty.remove(key);
        invalidate();
    }

    public void markDirty(BlockPos pos) {
        dirty.add(ChunkPosKey.of(pos));
        invalidate();
    }

    public void processDirty() {
        ambientTicks++;
        if (ambientTicks >= 20) {
            ambientTicks = 0;
            // Self-healing sweep for blocks placed by commands or other mods.
            rescanAroundPlayers(3);
        }
        if (dirty.isEmpty()) {
            return;
        }
        int budget = StressConfig.maxDirtyChunksPerTick();
        Iterator<ChunkPosKey> iterator = dirty.iterator();
        while (iterator.hasNext() && budget-- > 0) {
            ChunkPosKey key = iterator.next();
            iterator.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(key.x(), key.z());
            if (chunk != null) {
                scan(chunk);
            }
        }
    }

    public int rescanAroundPlayers(int radius) {
        int scanned = 0;
        try {
            for (ServerPlayer player : level.players()) {
                scanned += rescanArea(player.chunkPosition(), radius);
            }
        } catch (Throwable t) {
            StressRoutes.LOGGER.debug("Could not rescan kinetic chunks in {}", level.dimension().location(), t);
        }
        return scanned;
    }

    public int rescanArea(ChunkPos center, int radius) {
        int scanned = 0;
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
                    if (chunk != null) {
                        scan(chunk);
                        scanned++;
                    }
                }
            }
        } catch (Throwable t) {
            StressRoutes.LOGGER.debug("Could not rescan kinetic chunks in {}", level.dimension().location(), t);
        }
        return scanned;
    }

    private void scan(LevelChunk chunk) {
        Set<BlockPos> found = null;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (entry.getValue() instanceof KineticBlockEntity) {
                if (found == null) {
                    found = new HashSet<>();
                }
                found.add(entry.getKey().immutable());
            }
        }

        ChunkPosKey key = ChunkPosKey.of(chunk.getPos());
        Set<BlockPos> old = byChunk.remove(key);
        if (old != null) {
            positions.removeAll(old);
        }
        if (found != null) {
            byChunk.put(key, found);
            positions.addAll(found);
        }
        invalidate();
    }

    private void invalidate() {
        cachedTick = Long.MIN_VALUE;
        cachedSnapshot = null;
    }

    // ------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------

    public StressSnapshot snapshot() {
        long tick = level.getServer() == null ? level.getGameTime() : level.getServer().getTickCount();
        if (cachedSnapshot != null && cachedTick == tick) {
            return cachedSnapshot;
        }

        seedIfNeeded();

        List<StressEdge> edges = new ArrayList<>();
        List<StressNode> nodes = new ArrayList<>();
        Set<Long> networks = new HashSet<>();
        Map<Long, float[]> statsByNetwork = new HashMap<>();
        int maxNodes = StressConfig.maxNodes();

        List<BlockPos> ordered = new ArrayList<>(positions.size());
        ordered.addAll(positions);
        ordered.sort(Comparator.comparingLong(BlockPos::asLong));

        int visited = 0;
        for (BlockPos pos : ordered) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                positions.remove(pos);
                continue;
            }
            if (!(be instanceof KineticBlockEntity kinetic)) {
                positions.remove(pos);
                continue;
            }
            if (visited++ >= maxNodes) {
                continue;
            }

            Long networkId = kinetic.network;
            if (networkId != null) {
                networks.add(networkId);
            }
            float[] stats = statsByNetwork.computeIfAbsent(
                networkId == null ? Long.MIN_VALUE : networkId,
                key -> networkStats(kinetic));

            nodes.add(new StressNode(pos, 0.5F, 0.5F, 0.5F, kindOf(kinetic, stats),
                kinetic.getBlockState().getBlock().getDescriptionId(), stats[0], stats[1]));

            for (Direction direction : POSITIVE_DIRECTIONS) {
                BlockPos neighbourPos = pos.relative(direction);
                if (!level.isLoaded(neighbourPos)) {
                    continue;
                }
                if (level.getBlockEntity(neighbourPos) instanceof KineticBlockEntity other
                    && isConnected(kinetic, other)) {
                    edges.add(new StressEdge(pos, neighbourPos,
                        0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F));
                }
            }
        }

        int topologyHash = hash(edges, nodes, networks.size(), true);
        int geometryHash = hash(edges, nodes, networks.size(), false);
        StressSnapshot snapshot = new StressSnapshot(level.dimension(), topologyHash, geometryHash,
            List.copyOf(edges), List.copyOf(nodes), networks.size());
        cachedSnapshot = snapshot;
        cachedTick = tick;
        return snapshot;
    }

    private float[] networkStats(KineticBlockEntity be) {
        try {
            if (!be.hasNetwork()) {
                return NO_STATS;
            }
            KineticNetwork network = be.getOrCreateNetwork();
            return new float[]{network.calculateStress(), network.calculateCapacity()};
        } catch (Throwable t) {
            return NO_STATS;
        }
    }

    private static int kindOf(KineticBlockEntity be, float[] stats) {
        if (stats[1] > 0.0F && stats[0] > stats[1]) {
            return NodeKind.OVERSTRESSED;
        }
        try {
            if (be.calculateAddedStressCapacity() > 0.0F) {
                return NodeKind.SOURCE;
            }
            if (be.calculateStressApplied() > 0.0F) {
                return NodeKind.CONSUMER;
            }
        } catch (Throwable ignored) {
            // Fall through to transmission.
        }
        return NodeKind.TRANSMISSION;
    }

    private static boolean isConnected(KineticBlockEntity from, KineticBlockEntity to) {
        try {
            return RotationPropagator.isConnected(from, to) || RotationPropagator.isConnected(to, from);
        } catch (Throwable t) {
            return false;
        }
    }

    private void seedIfNeeded() {
        if (seeded) {
            return;
        }
        seeded = true;
        rescanAroundPlayers(12);
    }

    private static int hash(List<StressEdge> edges, List<StressNode> nodes,
                            int networkCount, boolean includeStats) {
        int hash = 1 + networkCount * 131;
        for (StressEdge edge : edges) {
            hash ^= 31 * hashPos(edge.from()) + hashPos(edge.to());
        }
        for (StressNode node : nodes) {
            int nodeHash = 17 * hashPos(node.pos()) + node.kind() * 7 + node.label().hashCode();
            if (includeStats) {
                // Quantised so the tooltip refreshes when the load actually moves, without
                // resending the snapshot on every tick.
                int quantisedStress = Math.round(node.stress() / 32.0F);
                int quantisedCapacity = Math.round(node.capacity() / 32.0F);
                nodeHash += quantisedStress * 31 + quantisedCapacity * 13;
            }
            hash ^= nodeHash;
        }
        return hash;
    }

    private static int hashPos(BlockPos pos) {
        return pos.getX() * 73856093 ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
    }

    /** Chunk position key without relying on mutable fastutil types in maps. */
    public record ChunkPosKey(int x, int z) {

        public static ChunkPosKey of(ChunkPos pos) {
            return new ChunkPosKey(pos.x, pos.z);
        }

        public static ChunkPosKey of(BlockPos pos) {
            return new ChunkPosKey(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }
}
