package dev.ismadev.stressroutes.mixin;

import dev.ismadev.stressroutes.StressRoutes;
import dev.ismadev.stressroutes.client.StressMapOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

/**
 * Hooks the world map render right before its last GUI blit, which is where Create draws
 * its train map: above map tiles and map elements, below the map's own buttons.
 *
 * <p>{@code require = 0} is deliberate. If a future Xaero update moves that call, the
 * overlay silently does not render instead of crashing the game.</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin {

    @Unique
    private static boolean stressroutes$loggedHook;

    @Unique
    private boolean stressroutes$failed;

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
        ),
        require = 0
    )
    private void stressroutes$renderStressLayer(GuiGraphics graphics, int mouseX, int mouseY,
                                                float partialTicks, CallbackInfo ci) {
        if (stressroutes$failed) {
            return;
        }
        if (!stressroutes$loggedHook) {
            stressroutes$loggedHook = true;
            StressRoutes.LOGGER.info("Stress Routes: Xaero world map hook is active");
        }
        try {
            StressMapOverlay.onRender(graphics, (GuiMap) (Object) this, mouseX, mouseY, partialTicks);
        } catch (Throwable t) {
            stressroutes$failed = true;
            StressRoutes.LOGGER.error("Stress Routes could not render on Xaero's World Map; "
                + "the overlay is disabled until the game restarts", t);
        }
    }
}
