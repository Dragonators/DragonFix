package com.dragonfix.mixin.mixins.myctmlib;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;
import com.dragonfix.mixin.MyCTMLibRenderWorldBlock;

@Mixin(value = RenderBlocks.class, priority = 900)
public abstract class RenderBlocksOffsetMixin {

    @Shadow
    public IBlockAccess blockAccess;

    @Shadow
    public abstract boolean hasOverrideBlockTexture();

    @Inject(method = "renderFaceYNeg", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmYNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.DOWN, ci);
    }

    @Inject(method = "renderFaceYPos", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmYPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.UP, ci);
    }

    @Inject(method = "renderFaceZNeg", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmZNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.NORTH, ci);
    }

    @Inject(method = "renderFaceZPos", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmZPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.SOUTH, ci);
    }

    @Inject(method = "renderFaceXNeg", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmXNeg(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.WEST, ci);
    }

    @Inject(method = "renderFaceXPos", at = @At("HEAD"), cancellable = true)
    private void dragonfix$renderMyCtmXPos(Block block, double x, double y, double z, IIcon icon, CallbackInfo ci) {
        dragonfix$renderMyCtm(block, x, y, z, icon, ForgeDirection.EAST, ci);
    }

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

    @Unique
    private void dragonfix$renderMyCtm(Block block, double x, double y, double z, IIcon icon, ForgeDirection direction,
        CallbackInfo ci) {
        if (this.blockAccess == null || this.hasOverrideBlockTexture() || !MyCTMLibRenderWorldBlock.contains(icon)) {
            return;
        }
        if (MyCTMLibRenderWorldBlock
            .render((RenderBlocks) ((Object) this), this.blockAccess, block, x, y, z, icon, direction)) {
            ci.cancel();
        }
    }
}
