package com.dragonfix.mixin.mixins.myctmlib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;

import gregtech.api.interfaces.ITexture;
import gregtech.api.render.ISBRContext;
import gregtech.common.render.SBRContextBase;

@Mixin(value = SBRContextBase.class, remap = false)
public abstract class SBRContextBaseMixin {

    @Inject(
        method = { "renderNegativeYFacing", "renderPositiveYFacing", "renderNegativeZFacing", "renderPositiveZFacing",
            "renderNegativeXFacing", "renderPositiveXFacing" },
        at = @At("HEAD"))
    private void dragonfix$beginTextureArray(ITexture[] tex, CallbackInfo ci) {
        MyCTMLibGTRenderLayerContext.pushSbrTextureArray();
    }

    @Inject(
        method = { "renderNegativeYFacing", "renderPositiveYFacing", "renderNegativeZFacing", "renderPositiveZFacing",
            "renderNegativeXFacing", "renderPositiveXFacing" },
        at = @At("RETURN"))
    private void dragonfix$endTextureArray(ITexture[] tex, CallbackInfo ci) {
        MyCTMLibGTRenderLayerContext.popSbrTextureArray();
    }

    @Redirect(
        method = "renderNegativeYFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderYNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerYNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderYNeg(ctx));
    }

    @Redirect(
        method = "renderPositiveYFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderYPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerYPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderYPos(ctx));
    }

    @Redirect(
        method = "renderNegativeZFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderZNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerZNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderZNeg(ctx));
    }

    @Redirect(
        method = "renderPositiveZFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderZPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerZPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderZPos(ctx));
    }

    @Redirect(
        method = "renderNegativeXFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderXNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerXNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderXNeg(ctx));
    }

    @Redirect(
        method = "renderPositiveXFacing",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderXPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerXPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderTextureLayer(() -> texture.renderXPos(ctx));
    }

    @Unique
    private static void dragonfix$renderTextureLayer(Runnable renderer) {
        MyCTMLibGTRenderLayerContext.renderNextSbrTextureLayer(renderer);
    }
}
