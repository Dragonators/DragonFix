package com.dragonfix.mixin.mixins.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.util.item.AEStack;

@Mixin(value = AEStack.class, remap = false)
public abstract class AEStackMixin {

    @Shadow
    private long stackSize;

    @Inject(method = "incStackSize(J)V", at = @At("HEAD"), cancellable = true)
    private void dragonfix$incStackSizeSaturated(long amount, CallbackInfo ci) {
        this.stackSize = dragonfix$saturatedAdd(this.stackSize, amount);
        ci.cancel();
    }

    @Inject(method = "decStackSize(J)V", at = @At("HEAD"), cancellable = true)
    private void dragonfix$decStackSizeSaturated(long amount, CallbackInfo ci) {
        this.stackSize = dragonfix$saturatedSubtract(this.stackSize, amount);
        ci.cancel();
    }

    @Unique
    private static long dragonfix$saturatedAdd(long value, long amount) {
        long result = value + amount;
        if (((value ^ result) & (amount ^ result)) < 0) {
            return amount > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return result;
    }

    @Unique
    private static long dragonfix$saturatedSubtract(long value, long amount) {
        long result = value - amount;
        if (((value ^ amount) & (value ^ result)) < 0) {
            return amount > 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }
}
