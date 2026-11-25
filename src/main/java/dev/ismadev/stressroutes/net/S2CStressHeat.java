package dev.ismadev.stressroutes.net;

import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.client.StressClientPayloadHandler;
import dev.ismadev.stressroutes.data.StressCell;
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
 * Aggregated stress consumption, one entry per cell of the heatmap grid.
 *
 * @param cellSize  grid size in blocks
 * @param maxStress highest cell value in this snapshot, used to normalise colours
 * @param heatHash  quantised hash: the client only rebuilds its heat texture when it changes
 */
public record S2CStressHeat(ResourceKey<Level> dimension, int cellSize, float maxStress,
                            int heatHash, List<StressCell> cells) implements CustomPacketPayload {

    public static final Type<S2CStressHeat> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(StressRoutes.MODID, "heatmap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CStressHeat> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, payload.dimension);
            buf.writeVarInt(payload.cellSize);
            buf.writeFloat(payload.maxStress);
            buf.writeInt(payload.heatHash);
            StressCell.writeList(buf, payload.cells);
        },
        buf -> new S2CStressHeat(
            ResourceKey.streamCodec(Registries.DIMENSION).decode(buf),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readInt(),
            StressCell.readList(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CStressHeat payload, IPayloadContext context) {
        context.enqueueWork(() -> StressClientPayloadHandler.handleHeat(payload));
    }
}
