package dev.ismadev.stressroutes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

/**
 * Read-only access to the private camera state of Xaero's world map.
 *
 * <p>Only applied when Xaero's World Map is installed (see {@link StressMixinPlugin}).</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public interface GuiMapAccessor {

    @Accessor("cameraX")
    double stress$getCameraX();

    @Accessor("cameraZ")
    double stress$getCameraZ();

    @Accessor("scale")
    double stress$getScale();

    @Accessor("mapProcessor")
    MapProcessor stress$getMapProcessor();
}
