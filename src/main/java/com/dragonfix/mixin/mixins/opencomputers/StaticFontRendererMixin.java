package com.dragonfix.mixin.mixins.opencomputers;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "li.cil.oc.client.renderer.font.StaticFontRenderer", remap = false)
public abstract class StaticFontRendererMixin {

    @Shadow(remap = false)
    public abstract String chars();

    @Shadow(remap = false)
    public abstract int charWidth();

    @Shadow(remap = false)
    public abstract int charHeight();

    @Shadow(remap = false)
    @Final
    private int cols;

    @Shadow(remap = false)
    @Final
    private double uStep;

    @Shadow(remap = false)
    @Final
    private double uSize;

    @Shadow(remap = false)
    @Final
    private double vStep;

    @Shadow(remap = false)
    @Final
    private double vSize;

    @Shadow(remap = false)
    @Final
    private double s;

    @Shadow(remap = false)
    @Final
    private double dw;

    @Shadow(remap = false)
    @Final
    private double dh;

    @Inject(method = "drawChar(FFI)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$drawWithTessellator(float tx, float ty, int character, CallbackInfo ci) {
        int found = chars().indexOf(character);
        int index = 1 + (found == -1 ? chars().indexOf('?') : found);
        int x = (index - 1) % cols;
        int y = (index - 1) / cols;
        double u = x * uStep;
        double v = y * vStep;

        Tessellator tessellator = Tessellator.instance;
        tessellator.addVertexWithUV(tx - dw, ty + charHeight() * s, 0, u, v + vSize);
        tessellator.addVertexWithUV(tx + charWidth() * s, ty + charHeight() * s, 0, u + uSize, v + vSize);
        tessellator.addVertexWithUV(tx + charWidth() * s, ty - dh, 0, u + uSize, v);
        tessellator.addVertexWithUV(tx - dw, ty - dh, 0, u, v);
        ci.cancel();
    }
}
