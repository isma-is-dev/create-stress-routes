package dev.ismadev.stressroutes.mixin;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Xaero's mods are optional. Without this plugin, Mixin would fail to resolve
 * {@code xaero.map.gui.GuiMap} on a vanilla-ish install and the game would not boot.
 */
public final class StressMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("Create: Stress Routes");
    private static boolean loggedDecision;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean loaded = isXaeroWorldMapLoaded();
        if (!loggedDecision) {
            loggedDecision = true;
            LOGGER.info("Xaero's World Map {}; stress map integration {}",
                loaded ? "found" : "not installed", loaded ? "enabled" : "disabled");
        }
        return loaded;
    }

    public static boolean isXaeroWorldMapLoaded() {
        try {
            LoadingModList loading = LoadingModList.get();
            if (loading != null) {
                return loading.getModFileById("xaeroworldmap") != null;
            }
        } catch (Throwable ignored) {
            // LoadingModList may not be ready at every call site; try the runtime list.
        }
        try {
            return ModList.get().isLoaded("xaeroworldmap");
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
