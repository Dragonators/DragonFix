package com.dragonfix.mixin.mixins.mattermanipulator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorFluidSourceHelper;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;

@Mixin(value = BlockSpec.class, remap = false)
public abstract class BlockSpecMixin {

    @Shadow(remap = false)
    private int metadata;

    @Shadow(remap = false)
    public abstract net.minecraft.block.Block getBlock();

    @Inject(method = "getBlockMeta", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$getFluidBlockMeta(CallbackInfoReturnable<Integer> cir) {
        if (MatterManipulatorFluidSourceHelper.isSupportedFluid(getBlock())) {
            cir.setReturnValue(metadata);
        }
    }
}
