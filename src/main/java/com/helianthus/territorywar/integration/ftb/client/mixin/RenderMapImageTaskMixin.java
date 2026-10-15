package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.TopographicColorResolver;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbchunks.client.map.RenderMapImageTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces every FTB Chunks region image (large map and minimap) through the topography colour path,
 * and keeps the height used as a palette index inside the 256-row palette.
 */
@Mixin(value = RenderMapImageTask.class, remap = false)
public abstract class RenderMapImageTaskMixin {
    @ModifyVariable(method = "runMapTask", at = @At("STORE"))
    private MapMode territorywar$forceTopography(MapMode mode) {
        return MapMode.TOPOGRAPHY;
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private static void territorywar$clampHeight(MapMode mode, int waterHeightFactor, short waterLightAndBiome,
            short height, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(TopographicColorResolver.paletteHeight(height));
    }
}
