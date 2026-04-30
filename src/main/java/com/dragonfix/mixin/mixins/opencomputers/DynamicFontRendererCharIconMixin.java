package com.dragonfix.mixin.mixins.opencomputers;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "li.cil.oc.client.renderer.font.DynamicFontRenderer$CharIcon", remap = false)
public abstract class DynamicFontRendererCharIconMixin {

    @Shadow
    public abstract int w();

    @Shadow
    public abstract int h();

    @Shadow
    public abstract double u1();

    @Shadow
    public abstract double v1();

    @Shadow
    public abstract double u2();

    @Shadow
    public abstract double v2();

    @Inject(method = "draw(FF)V", at = @At("HEAD"), cancellable = true)
    private void dragonfix$drawWithTessellator(float tx, float ty, CallbackInfo ci) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.addVertexWithUV(tx, ty + h(), 0, u1(), v2());
        tessellator.addVertexWithUV(tx + w(), ty + h(), 0, u2(), v2());
        tessellator.addVertexWithUV(tx + w(), ty, 0, u2(), v1());
        tessellator.addVertexWithUV(tx, ty, 0, u1(), v1());
        ci.cancel();
    }
}
