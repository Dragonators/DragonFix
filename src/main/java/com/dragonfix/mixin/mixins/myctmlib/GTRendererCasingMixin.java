package com.dragonfix.mixin.mixins.myctmlib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;

import gregtech.api.interfaces.ITexture;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTRendererCasing;

@Mixin(value = GTRendererCasing.class, remap = false)
public abstract class GTRendererCasingMixin {

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeYFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderYNeg(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderNegativeYFacing(tex));
    }

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveYFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderYPos(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderPositiveYFacing(tex));
    }

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeZFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderZNeg(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderNegativeZFacing(tex));
    }

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveZFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderZPos(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderPositiveZFacing(tex));
    }

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeXFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderXNeg(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderNegativeXFacing(tex));
    }

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveXFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 0))
    private void dragonfix$renderXPos(ISBRWorldContext ctx, ITexture[] tex) {
        dragonfix$renderWithBias(tex, () -> ctx.renderPositiveXFacing(tex));
    }

    @Unique
    private static void dragonfix$renderWithBias(ITexture[] tex, Runnable renderer) {
        int bias = tex != null && tex.length > 1 && tex[1] != null ? 1 : 0;
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(bias, renderer);
    }
}
