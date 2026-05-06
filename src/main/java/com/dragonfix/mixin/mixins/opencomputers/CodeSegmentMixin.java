package com.dragonfix.mixin.mixins.opencomputers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mixin.OpenComputersTextureFontRendererBridge;

import li.cil.oc.client.renderer.font.TextureFontRenderer;
import li.cil.oc.client.renderer.markdown.segment.CodeSegment;

/**
 * Routes OpenComputers manual code text through DragonFix's colored Tessellator-backed font path.
 *
 * <p>
 * Adapted from GTNewHorizons/OpenComputers PR #184 by PinkYuDeer.
 *
 * @see <a href="https://github.com/GTNewHorizons/OpenComputers/pull/184">OpenComputers PR #184</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/OpenComputers/commit/e7fa0a1aec316bd99720cd8a0eb1c8f55449cef2">OpenComputers
 *      commit e7fa0a1</a>
 */
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
