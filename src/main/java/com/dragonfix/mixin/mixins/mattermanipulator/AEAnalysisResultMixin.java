package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.recursive_pineapple.matter_manipulator.common.building.AEAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

@Mixin(value = AEAnalysisResult.class, remap = false)
public abstract class AEAnalysisResultMixin {

    @Shadow(remap = false)
    public PortableItemStack[] mAEFacades;

    @Inject(
        method = "clone()Lcom/recursive_pineapple/matter_manipulator/common/building/AEAnalysisResult;",
        at = @At("RETURN"),
        remap = false)
    private void dragonfix$cloneFacades(CallbackInfoReturnable<AEAnalysisResult> cir) {
        cir.getReturnValue().mAEFacades = mAEFacades == null ? null
            : MMUtils.mapToArray(mAEFacades, PortableItemStack[]::new, stack -> stack == null ? null : stack.clone());
    }

    @Inject(method = "transform", at = @At("RETURN"), remap = false)
    private void dragonfix$transformFacades(Transform transform, CallbackInfo ci) {
        if (mAEFacades == null) return;

        PortableItemStack[] facadesOut = new PortableItemStack[ForgeDirection.VALID_DIRECTIONS.length];

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            facadesOut[transform.apply(dir)
                .ordinal()] = mAEFacades[dir.ordinal()];
        }

        mAEFacades = facadesOut;
    }
}
