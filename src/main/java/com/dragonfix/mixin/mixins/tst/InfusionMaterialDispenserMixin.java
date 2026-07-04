package com.dragonfix.mixin.mixins.tst;

import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.Nxer.TwistSpaceTechnology.common.machine.TST_InfusionMaterialDispenser", remap = false)
public abstract class InfusionMaterialDispenserMixin {

    @Redirect(
        method = "collectAndOutputResults",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/reflect/Method;invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 0)
    private Object dragonfix$skipInvalidReturnedPearlsCall(Method method, Object receiver, Object[] args)
        throws ReflectiveOperationException {
        if (!method.getDeclaringClass()
            .isInstance(receiver)) {
            return 0;
        }
        return method.invoke(receiver, args);
    }
}
