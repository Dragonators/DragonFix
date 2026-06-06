package com.dragonfix.mixin.mixins.angelica;

import net.coderbot.iris.uniforms.custom.CustomUniformFixedInputUniformsHolder;
import net.coderbot.iris.uniforms.custom.CustomUniforms;
import net.coderbot.iris.uniforms.custom.cached.CachedUniform;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import kroppeb.stareval.expression.Expression;

@Mixin(value = CustomUniforms.class, remap = false)
public abstract class CustomUniformsMixin {

    @Shadow
    @Final
    private CustomUniformFixedInputUniformsHolder inputHolder;

    @Inject(method = "addVariable", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$skipVariablesShadowingBuiltInUniforms(Expression expression, CachedUniform uniform,
        CallbackInfo ci) {
        if (inputHolder.containsKey(uniform.getName())) {
            ci.cancel();
        }
    }
}
