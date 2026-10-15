package com.helianthus.territorywar.integration.ftb.client.mixin;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the client map surface scan look through tree canopies and trunks down to the ground. */
@Mixin(value = FTBChunksClient.class, remap = false)
public abstract class FTBChunksClientMixin {
    @Inject(method = "skipBlock", at = @At("HEAD"), cancellable = true)
    private void territorywar$skipTrees(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            cir.setReturnValue(true);
        }
    }
}
