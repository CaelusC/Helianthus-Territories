package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.LargeMapRegionTools;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the territory admin toggle and admin tool buttons to the FTB Chunks large map. */
@Mixin(value = LargeMapScreen.class, remap = false)
public abstract class LargeMapScreenMixin {
    @Unique
    private List<SimpleTextButton> territorywar$buttons;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void territorywar$addButtons(CallbackInfo ci) {
        LargeMapScreen screen = (LargeMapScreen) (Object) this;
        territorywar$buttons = LargeMapRegionTools.createButtons(screen);
        territorywar$buttons.forEach(screen::add);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void territorywar$alignButtons(CallbackInfo ci) {
        if (territorywar$buttons != null) {
            LargeMapRegionTools.alignButtons(territorywar$buttons, ((LargeMapScreen) (Object) this).width);
        }
    }
}
