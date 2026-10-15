package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.LargeMapRegionTools;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the territory admin toggle and admin tool buttons to the FTB Chunks large map, and removes FTB's
 * claimed chunks screen and settings buttons together with their C and S / Ctrl+S shortcuts.
 */
@Mixin(value = LargeMapScreen.class, remap = false)
public abstract class LargeMapScreenMixin {
    @Unique
    private static final int TERRITORYWAR$BUTTON_STEP = 18;

    @Shadow
    private Button claimChunksButton;
    @Shadow
    private Button waypointManagerButton;
    @Shadow
    private Button infoButton;
    @Shadow
    private Button infoSortScreen;
    @Shadow
    private Button clearDeathpointsButton;
    @Shadow
    private Button settingsButton;
    @Shadow
    private Button serverSettingsButton;

    @Unique
    private List<SimpleTextButton> territorywar$buttons;

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void territorywar$addButtons(CallbackInfo ci) {
        LargeMapScreen screen = (LargeMapScreen) (Object) this;
        screen.getWidgets().remove(claimChunksButton);
        screen.getWidgets().remove(settingsButton);
        if (serverSettingsButton != null) {
            screen.getWidgets().remove(serverSettingsButton);
        }
        territorywar$buttons = LargeMapRegionTools.createButtons(screen);
        territorywar$buttons.forEach(screen::add);
    }

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void territorywar$alignButtons(CallbackInfo ci) {
        // Close the gap left at the top of the left column by the removed claimed chunks button
        for (Button button : new Button[] {waypointManagerButton, infoButton, infoSortScreen, clearDeathpointsButton}) {
            button.setPos(button.posX, button.posY - TERRITORYWAR$BUTTON_STEP);
        }
        if (territorywar$buttons != null) {
            LargeMapRegionTools.alignButtons(territorywar$buttons, ((LargeMapScreen) (Object) this).width);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void territorywar$blockRemovedShortcuts(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (key.is(GLFW.GLFW_KEY_C) || key.is(GLFW.GLFW_KEY_S)) {
            cir.setReturnValue(false);
        }
    }
}
