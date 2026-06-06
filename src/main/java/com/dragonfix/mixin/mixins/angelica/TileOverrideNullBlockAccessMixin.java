package com.dragonfix.mixin.mixins.angelica;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.ctm.RenderBlockState;
import com.prupe.mcpatcher.ctm.TileOverride;

@Mixin(value = TileOverride.class, remap = false)
public abstract class TileOverrideNullBlockAccessMixin {

    @Inject(
        method = "shouldConnect(Lcom/prupe/mcpatcher/ctm/RenderBlockState;Lnet/minecraft/util/IIcon;[I)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$skipConnectedTextureWithoutBlockAccess(RenderBlockState renderBlockState, IIcon icon,
        int[] offset, CallbackInfoReturnable<Boolean> cir) {
        if (renderBlockState.getBlockAccess() == null) {
            cir.setReturnValue(false);
        }
    }
}
