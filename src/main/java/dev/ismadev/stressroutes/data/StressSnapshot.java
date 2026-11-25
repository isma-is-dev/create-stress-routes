package dev.ismadev.stressroutes.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Immutable view of one dimension's kinetic network for a single server tick.
 *
 * @param topologyHash order independent hash of everything that affects the drawing,
 *                     including stress values quantised so tooltips refresh without
 *                     flooding the client every tick
 * @param geometryHash same hash without stress values: only this one invalidates the
 *                     rasterised map textures
 * @param networkCount number of distinct stress networks found
 */
public record StressSnapshot(ResourceKey<Level> dimension,
                             int topologyHash,
                             int geometryHash,
                             List<StressEdge> edges,
                             List<StressNode> nodes,
                             int networkCount) {

    public boolean isEmpty() {
        return edges.isEmpty() && nodes.isEmpty();
    }
}
