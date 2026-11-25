package dev.ismadev.stressroutes.net;

import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.client.StressClientPayloadHandler;
import dev.ismadev.stressroutes.data.StressEdge;
import dev.ismadev.stressroutes.data.StressNode;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * One part of a stress network snapshot. Large factories are split over several packets.
 */
public record S2CTopologyPart(int requestId, int part, int parts, int geometryHash,
                              ResourceKey<Level> dimension, int networkCount,
                              List<StressEdge> edges, List<StressNode> nodes)
    implements CustomPacketPayload {

    public static final Type<S2CTopologyPart> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(StressRoutes.MODID, "topology"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTopologyPart> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.requestId);
            buf.writeVarInt(payload.part);
            buf.writeVarInt(payload.parts);
            buf.writeInt(payload.geometryHash);
            ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, payload.dimension);
            buf.writeVarInt(payload.networkCount);
            StressEdge.writeList(buf, payload.edges);
            StressNode.writeList(buf, payload.nodes);
        },
        buf -> new S2CTopologyPart(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readInt(),
            ResourceKey.streamCodec(Registries.DIMENSION).decode(buf),
            buf.readVarInt(),
            StressEdge.readList(buf),
            StressNode.readList(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CTopologyPart payload, IPayloadContext context) {
        context.enqueueWork(() -> StressClientPayloadHandler.handleTopology(payload));
    }
}
