package dev.ismadev.stressroutes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One edge of the kinetic graph: two blocks that rotation actually propagates between.
 *
 * <p>Coordinates are offsets from {@link #from}'s block corner, which keeps them small and
 * float-precise even far from the world origin.</p>
 */
public record StressEdge(BlockPos from, BlockPos to,
                         float sx, float sy, float sz,
                         float ex, float ey, float ez) {

    public static final StreamCodec<FriendlyByteBuf, StressEdge> STREAM_CODEC =
        StreamCodec.of(StressEdge::write, StressEdge::read);

    private static void write(FriendlyByteBuf buf, StressEdge edge) {
        BlockPos.STREAM_CODEC.encode(buf, edge.from);
        BlockPos.STREAM_CODEC.encode(buf, edge.to);
        buf.writeFloat(edge.sx);
        buf.writeFloat(edge.sy);
        buf.writeFloat(edge.sz);
        buf.writeFloat(edge.ex);
        buf.writeFloat(edge.ey);
        buf.writeFloat(edge.ez);
    }

    private static StressEdge read(FriendlyByteBuf buf) {
        return new StressEdge(
            BlockPos.STREAM_CODEC.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeList(FriendlyByteBuf buf, List<StressEdge> edges) {
        buf.writeVarInt(edges.size());
        for (StressEdge edge : edges) {
            STREAM_CODEC.encode(buf, edge);
        }
    }

    public static List<StressEdge> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StressEdge> edges = new ArrayList<>(Math.min(size, 2048));
        for (int i = 0; i < size; i++) {
            edges.add(STREAM_CODEC.decode(buf));
        }
        return edges;
    }

    public double startX() {
        return from.getX() + sx;
    }

    public double startY() {
        return from.getY() + sy;
    }

    public double startZ() {
        return from.getZ() + sz;
    }

    public double endX() {
        return from.getX() + ex;
    }

    public double endY() {
        return from.getY() + ey;
    }

    public double endZ() {
        return from.getZ() + ez;
    }
}
