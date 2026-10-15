package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.LargeMapRegionTools;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes large map clicks and drags to the territory admin selection and purchase dialog. */
@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin {
    @Inject(method = "mousePressed", at = @At("HEAD"), cancellable = true)
    private void territorywar$mousePressed(MouseButton button, CallbackInfoReturnable<Boolean> cir) {
        if (LargeMapRegionTools.onMapPressed((RegionMapPanel) (Object) this, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void territorywar$mouseReleased(MouseButton button, CallbackInfo ci) {
        LargeMapRegionTools.onMapReleased((RegionMapPanel) (Object) this, button);
    }

    @Inject(method = "draw", at = @At("TAIL"))
    private void territorywar$draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        LargeMapRegionTools.onMapDrawn((RegionMapPanel) (Object) this);
    }
}
