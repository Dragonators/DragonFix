package com.dragonfix.mixin.mixins.opencomputers;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import li.cil.oc.client.renderer.font.DynamicFontRenderer;

@Mixin(value = DynamicFontRenderer.CharIcon.class, remap = false)
public abstract class DynamicFontRendererCharIconMixin {

    @Shadow(remap = false)
    public abstract int w();

    @Shadow(remap = false)
    public abstract int h();

    @Shadow(remap = false)
    public abstract double u1();

    @Shadow(remap = false)
    public abstract double v1();

    @Shadow(remap = false)
    public abstract double u2();

    @Shadow(remap = false)
    public abstract double v2();

    /**
     * @author DragonFix
     * @reason Submit vertices through Tessellator so the active tessellation state is preserved.
     */
    @Overwrite(remap = false)
    public void draw(float tx, float ty) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.addVertexWithUV(tx, ty + h(), 0, u1(), v2());
        tessellator.addVertexWithUV(tx + w(), ty + h(), 0, u2(), v2());
        tessellator.addVertexWithUV(tx + w(), ty, 0, u2(), v1());
        tessellator.addVertexWithUV(tx, ty, 0, u1(), v1());
    }
}
