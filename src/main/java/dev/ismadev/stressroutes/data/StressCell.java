package dev.ismadev.stressroutes.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One aggregated heatmap cell: everything a player needs to spot a stressed area, without
 * sending one entry per machine.
 *
 * @param cellX    cell grid coordinate (not a block position)
 * @param cellZ    cell grid coordinate
 * @param stress   total SU consumed by consumers inside the cell
 * @param machines number of consuming blocks inside the cell
 */
public record StressCell(int cellX, int cellZ, float stress, int machines) {

    public static final StreamCodec<FriendlyByteBuf, StressCell> STREAM_CODEC =
        StreamCodec.of(StressCell::write, StressCell::read);

    private static void write(FriendlyByteBuf buf, StressCell cell) {
        buf.writeVarInt(cell.cellX);
        buf.writeVarInt(cell.cellZ);
        buf.writeFloat(cell.stress);
        buf.writeVarInt(cell.machines);
    }

    private static StressCell read(FriendlyByteBuf buf) {
        return new StressCell(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt());
    }

    public static void writeList(FriendlyByteBuf buf, List<StressCell> cells) {
        buf.writeVarInt(cells.size());
        for (StressCell cell : cells) {
            STREAM_CODEC.encode(buf, cell);
        }
    }

    public static List<StressCell> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StressCell> cells = new ArrayList<>(Math.min(size, 2048));
        for (int i = 0; i < size; i++) {
            cells.add(STREAM_CODEC.decode(buf));
        }
        return cells;
    }
}
