package com.helianthus.territorywar.integration.ftb.client.mixin;

import com.helianthus.territorywar.integration.ftb.client.LargeMapRegionTools;
import dev.ftb.mods.ftbchunks.client.gui.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws territory region and selection fills over each large map tile, beneath map icons. */
@Mixin(value = MapTileWidget.class, remap = false)
public abstract class MapTileWidgetMixin {
    @Shadow
    @Final
    public MapRegion region;

    @Inject(method = "draw", at = @At("TAIL"))
    private void territorywar$drawRegions(GuiGraphics graphics, Theme theme, int x, int y, int w, int h,
            CallbackInfo ci) {
        LargeMapRegionTools.drawTileOverlay(graphics, region, x, y, w, h);
    }
}
