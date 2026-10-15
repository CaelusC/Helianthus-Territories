package com.helianthus.territorywar.integration.ftb.client.mixin;

import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.mapicon.TrackedPlayerMapIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops player, mob and other entity icons from the large map and minimap; waypoints still get through. */
@Mixin(value = MapIconEvent.class, remap = false)
public abstract class MapIconEventMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void territorywar$skipEntityIcons(MapIcon icon, CallbackInfo ci) {
        if (icon instanceof EntityMapIcon || icon instanceof TrackedPlayerMapIcon) {
            ci.cancel();
        }
    }
}
