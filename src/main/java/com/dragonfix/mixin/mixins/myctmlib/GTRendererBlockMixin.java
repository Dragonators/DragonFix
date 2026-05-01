package com.dragonfix.mixin.mixins.myctmlib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;

import gregtech.api.interfaces.ITexture;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTRendererBlock;

@Mixin(value = GTRendererBlock.class, remap = false)
public abstract class GTRendererBlockMixin {

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeYFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayYNeg(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderNegativeYFacing(tex));
    }

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveYFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayYPos(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderPositiveYFacing(tex));
    }

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeZFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayZNeg(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderNegativeZFacing(tex));
    }

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveZFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayZPos(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderPositiveZFacing(tex));
    }

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderNegativeXFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayXNeg(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderNegativeXFacing(tex));
    }

    @Redirect(
        method = "renderStandardBlock(Lgregtech/api/render/ISBRWorldContext;[[Lgregtech/api/interfaces/ITexture;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/render/ISBRWorldContext;renderPositiveXFacing([Lgregtech/api/interfaces/ITexture;)V",
            ordinal = 1))
    private void dragonfix$renderOverlayXPos(ISBRWorldContext ctx, ITexture[] tex) {
        MyCTMLibGTRenderLayerContext.renderWithSbrLayerBias(1, () -> ctx.renderPositiveXFacing(tex));
    }
}
