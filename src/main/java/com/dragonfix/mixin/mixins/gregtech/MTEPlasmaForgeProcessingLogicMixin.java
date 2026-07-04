package com.dragonfix.mixin.mixins.gregtech;

import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.gregtech.PlasmaForgeCatalystAdjustment;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.multi.MTEPlasmaForge;

@Mixin(targets = "gregtech.common.tileentities.machines.multi.MTEPlasmaForge$2", remap = false)
public abstract class MTEPlasmaForgeProcessingLogicMixin extends ProcessingLogic {

    @Redirect(
        method = "createOverclockCalculator(Lgregtech/api/util/GTRecipe;)Lgregtech/api/util/OverclockCalculator;",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/tileentities/machines/multi/MTEPlasmaForge;recipeAfterAdjustments(Lgregtech/api/util/GTRecipe;[Lnet/minecraftforge/fluids/FluidStack;)Lgregtech/api/util/GTRecipe;"),
        remap = false,
        require = 1)
    private GTRecipe dragonfix$adjustRecipeForCalculator(MTEPlasmaForge plasmaForge, GTRecipe recipe,
        FluidStack[] inputFluids) {
        return ((PlasmaForgeCatalystAdjustment) plasmaForge)
            .dragonfix$recipeAfterAdjustmentsWithInputs(recipe, inputFluids, inputItems);
    }

    @Redirect(
        method = "createParallelHelper(Lgregtech/api/util/GTRecipe;)Lgregtech/api/util/ParallelHelper;",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/tileentities/machines/multi/MTEPlasmaForge;recipeAfterAdjustments(Lgregtech/api/util/GTRecipe;[Lnet/minecraftforge/fluids/FluidStack;)Lgregtech/api/util/GTRecipe;"),
        remap = false,
        require = 1)
    private GTRecipe dragonfix$adjustRecipeForParallelHelper(MTEPlasmaForge plasmaForge, GTRecipe recipe,
        FluidStack[] inputFluids) {
        return ((PlasmaForgeCatalystAdjustment) plasmaForge)
            .dragonfix$recipeAfterAdjustmentsWithInputs(recipe, inputFluids, inputItems);
    }
}
