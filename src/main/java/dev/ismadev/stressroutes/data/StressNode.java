package dev.ismadev.stressroutes.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One kinetic block on the map.
 *
 * @param label    block description id, translated client-side
 * @param stress   stress currently used by the whole network
 * @param capacity stress capacity of the whole network
 */
public record StressNode(BlockPos pos, float dx, float dy, float dz,
                         int kind, String label, float stress, float capacity) {

    public static final StreamCodec<FriendlyByteBuf, StressNode> STREAM_CODEC =
        StreamCodec.of(StressNode::write, StressNode::read);

    private static void write(FriendlyByteBuf buf, StressNode node) {
        BlockPos.STREAM_CODEC.encode(buf, node.pos);
        buf.writeFloat(node.dx);
        buf.writeFloat(node.dy);
        buf.writeFloat(node.dz);
        buf.writeByte(node.kind);
        buf.writeUtf(node.label, 128);
        buf.writeFloat(node.stress);
        buf.writeFloat(node.capacity);
    }

    private static StressNode read(FriendlyByteBuf buf) {
        return new StressNode(
            BlockPos.STREAM_CODEC.decode(buf),
            buf.readFloat(), buf.readFloat(), buf.readFloat(),
            buf.readByte(),
            buf.readUtf(128),
            buf.readFloat(), buf.readFloat());
    }

    public static void writeList(FriendlyByteBuf buf, List<StressNode> nodes) {
        buf.writeVarInt(nodes.size());
        for (StressNode node : nodes) {
            STREAM_CODEC.encode(buf, node);
        }
    }

    public static List<StressNode> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StressNode> nodes = new ArrayList<>(Math.min(size, 512));
        for (int i = 0; i < size; i++) {
            nodes.add(STREAM_CODEC.decode(buf));
        }
        return nodes;
    }

    public double x() {
        return pos.getX() + dx;
    }

    public double y() {
        return pos.getY() + dy;
    }

    public double z() {
        return pos.getZ() + dz;
    }

    public float load() {
        return capacity <= 0.0F ? 0.0F : stress / capacity;
    }
}
