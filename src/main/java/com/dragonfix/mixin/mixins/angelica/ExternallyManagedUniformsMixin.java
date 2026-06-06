package com.dragonfix.mixin.mixins.angelica;

import net.coderbot.iris.gl.uniform.UniformType;
import net.coderbot.iris.uniforms.ExternallyManagedUniforms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ExternallyManagedUniforms.class, remap = false)
public abstract class ExternallyManagedUniformsMixin {

    @ModifyArg(
        method = "addExternallyManagedUniforms",
        at = @At(
            value = "INVOKE",
            target = "Lnet/coderbot/iris/gl/uniform/UniformHolder;externallyManagedUniform(Ljava/lang/String;Lnet/coderbot/iris/gl/uniform/UniformType;)Lnet/coderbot/iris/gl/uniform/UniformHolder;",
            ordinal = 1),
        index = 1,
        remap = false)
    private static UniformType dragonfix$useIntTypeForClipPlanesEnabled(UniformType type) {
        return UniformType.INT;
    }
}
