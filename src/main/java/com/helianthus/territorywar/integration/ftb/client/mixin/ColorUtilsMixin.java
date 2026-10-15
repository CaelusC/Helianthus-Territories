package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.TopographicColorResolver;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces FTB Chunks' texture-based topography palette with the muted territory palette. */
@Mixin(value = ColorUtils.class, remap = false)
public abstract class ColorUtilsMixin {
    @Inject(method = "getTopographyPalette", at = @At("HEAD"), cancellable = true)
    private static void territorywar$topographicPalette(CallbackInfoReturnable<Color4I[]> cir) {
        cir.setReturnValue(TopographicColorResolver.getPalette());
    }
}
