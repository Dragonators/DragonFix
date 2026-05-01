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
import gregtech.common.render.GTMultiTextureRender;

@Mixin(value = GTMultiTextureRender.class, remap = false)
public abstract class GTMultiTextureRenderMixin {

    @Inject(
        method = { "renderXPos", "renderXNeg", "renderYPos", "renderYNeg", "renderZPos", "renderZNeg" },
        at = @At("HEAD"))
    private void dragonfix$beginMultiTexture(ISBRContext ctx, CallbackInfo ci) {
        MyCTMLibGTRenderLayerContext.pushMultiTextureRender();
    }

    @Inject(
        method = { "renderXPos", "renderXNeg", "renderYPos", "renderYNeg", "renderZPos", "renderZNeg" },
        at = @At("RETURN"))
    private void dragonfix$endMultiTexture(ISBRContext ctx, CallbackInfo ci) {
        MyCTMLibGTRenderLayerContext.popMultiTextureRender();
    }

    @Redirect(
        method = "renderXPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderXPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerXPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderXPos(ctx));
    }

    @Redirect(
        method = "renderXNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderXNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerXNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderXNeg(ctx));
    }

    @Redirect(
        method = "renderYPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderYPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerYPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderYPos(ctx));
    }

    @Redirect(
        method = "renderYNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderYNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerYNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderYNeg(ctx));
    }

    @Redirect(
        method = "renderZPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderZPos(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerZPos(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderZPos(ctx));
    }

    @Redirect(
        method = "renderZNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/interfaces/ITexture;renderZNeg(Lgregtech/api/render/ISBRContext;)V"))
    private void dragonfix$renderLayerZNeg(ITexture texture, ISBRContext ctx) {
        dragonfix$renderMultiTextureLayer(() -> texture.renderZNeg(ctx));
    }

    @Unique
    private static void dragonfix$renderMultiTextureLayer(Runnable renderer) {
        MyCTMLibGTRenderLayerContext.renderNextMultiTextureLayer(renderer);
    }
}
