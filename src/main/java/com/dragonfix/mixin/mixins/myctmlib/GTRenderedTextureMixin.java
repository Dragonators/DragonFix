package com.dragonfix.mixin.mixins.myctmlib;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mixin.MyCTMLibGTRenderLayerContext;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import gregtech.api.render.ISBRContext;
import gregtech.common.render.GTRenderedTexture;

@Mixin(value = GTRenderedTexture.class, remap = false)
public abstract class GTRenderedTextureMixin {

    @Shadow
    protected abstract void renderFaceXPos(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Shadow
    protected abstract void renderFaceXNeg(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Shadow
    protected abstract void renderFaceYPos(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Shadow
    protected abstract void renderFaceYNeg(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Shadow
    protected abstract void renderFaceZPos(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Shadow
    protected abstract void renderFaceZNeg(ISBRContext ctx, IIcon icon, ExtendedFacing extendedFacing);

    @Redirect(
        method = "renderXPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceXPos(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayXPos(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceXPos(ctx, icon, facing));
    }

    @Redirect(
        method = "renderXNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceXNeg(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayXNeg(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceXNeg(ctx, icon, facing));
    }

    @Redirect(
        method = "renderYPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceYPos(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayYPos(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceYPos(ctx, icon, facing));
    }

    @Redirect(
        method = "renderYNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceYNeg(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayYNeg(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceYNeg(ctx, icon, facing));
    }

    @Redirect(
        method = "renderZPos",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceZPos(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayZPos(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceZPos(ctx, icon, facing));
    }

    @Redirect(
        method = "renderZNeg",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/render/GTRenderedTexture;renderFaceZNeg(Lgregtech/api/render/ISBRContext;Lnet/minecraft/util/IIcon;Lcom/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing;)V",
            ordinal = 1))
    private void dragonfix$redirectOverlayZNeg(GTRenderedTexture instance, ISBRContext ctx, IIcon icon,
        ExtendedFacing facing) {
        MyCTMLibGTRenderLayerContext.renderOverlay(() -> this.renderFaceZNeg(ctx, icon, facing));
    }
}
