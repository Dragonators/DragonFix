package com.dragonfix.mixin.mixins.myctmlib;

import net.minecraft.client.renderer.RenderBlocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;

@Mixin(RenderBlocks.class)
public abstract class RenderBlocksOffsetMixin {

    @ModifyVariable(method = "renderFaceXPos", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double dragonfix$offsetRenderFaceXPosX(double x) {
        return dragonfix$applyNormalOffset(x, 1.0D);
    }

    @ModifyVariable(method = "renderFaceXNeg", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double dragonfix$offsetRenderFaceXNegX(double x) {
        return dragonfix$applyNormalOffset(x, -1.0D);
    }

    @ModifyVariable(method = "renderFaceYPos", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double dragonfix$offsetRenderFaceYPosY(double y) {
        return dragonfix$applyNormalOffset(y, 1.0D);
    }

    @ModifyVariable(method = "renderFaceYNeg", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double dragonfix$offsetRenderFaceYNegY(double y) {
        return dragonfix$applyNormalOffset(y, -1.0D);
    }

    @ModifyVariable(method = "renderFaceZPos", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private double dragonfix$offsetRenderFaceZPosZ(double z) {
        return dragonfix$applyNormalOffset(z, 1.0D);
    }

    @ModifyVariable(method = "renderFaceZNeg", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private double dragonfix$offsetRenderFaceZNegZ(double z) {
        return dragonfix$applyNormalOffset(z, -1.0D);
    }

    @Unique
    private static double dragonfix$applyNormalOffset(double coordinate, double direction) {
        double offset = MyCTMLibGTRenderLayerContext.getCurrentOffset();
        if (offset == 0.0D) {
            return coordinate;
        }
        return coordinate + offset * direction;
    }
}
