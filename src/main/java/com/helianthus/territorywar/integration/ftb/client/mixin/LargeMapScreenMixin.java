package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.LargeMapRegionTools;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the territory admin mode toggle to the FTB Chunks large map. */
@Mixin(value = LargeMapScreen.class, remap = false)
public abstract class LargeMapScreenMixin {
    @Unique
    private SimpleTextButton territorywar$adminButton;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void territorywar$addAdminButton(CallbackInfo ci) {
        LargeMapScreen screen = (LargeMapScreen) (Object) this;
        territorywar$adminButton = LargeMapRegionTools.createAdminButton(screen);
        screen.add(territorywar$adminButton);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void territorywar$alignAdminButton(CallbackInfo ci) {
        if (territorywar$adminButton != null) {
            LargeMapRegionTools.alignAdminButton(territorywar$adminButton, ((LargeMapScreen) (Object) this).width);
        }
    }
}
