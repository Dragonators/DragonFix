package com.dragonfix.mixin.mixins.myctmlib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.github.wohaopa.MyCTMLib.Textures;

@Mixin(value = Textures.class, remap = false)
public abstract class TexturesMixin {

    @ModifyConstant(method = "renderWorldBlock", constant = @Constant(floatValue = 1.0e-3F))
    private static float dragonfix$removeFixedFaceOffset(float offset) {
        return 0.0F;
    }
}
