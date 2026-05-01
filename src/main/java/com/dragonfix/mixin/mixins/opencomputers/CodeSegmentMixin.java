package com.dragonfix.mixin.mixins.opencomputers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mixin.OpenComputersTextureFontRendererBridge;

import li.cil.oc.client.renderer.font.TextureFontRenderer;
import li.cil.oc.client.renderer.markdown.segment.CodeSegment;

@Mixin(value = CodeSegment.class, remap = false)
public abstract class CodeSegmentMixin {

    @Redirect(
        method = "render(IIIILnet/minecraft/client/gui/FontRenderer;II)Lscala/Option;",
        at = @At(
            value = "INVOKE",
            target = "Lli/cil/oc/client/renderer/font/TextureFontRenderer;drawString(Ljava/lang/String;II)V"),
        remap = false)
    private void dragonfix$drawCodeStringWithColor(TextureFontRenderer renderer, String text, int x, int y) {
        ((OpenComputersTextureFontRendererBridge) renderer).dragonfix$drawString(text, x, y, 0xBFCCFF);
    }
}
