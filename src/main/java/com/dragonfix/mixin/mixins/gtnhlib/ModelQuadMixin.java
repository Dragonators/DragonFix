package com.dragonfix.mixin.mixins.gtnhlib;

import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

@Mixin(value = ModelQuad.class, remap = false)
public abstract class ModelQuadMixin implements QuadView {
}
