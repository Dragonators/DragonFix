package com.dragonfix.mixin.mixins.gregtech;

import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.gregtech.PlasmaForgeCatalystAdjustment;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.MaterialsUEVplus;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.multi.MTEPlasmaForge;

@Mixin(value = MTEPlasmaForge.class, remap = false)
public abstract class MTEPlasmaForgeMixin implements PlasmaForgeCatalystAdjustment {

    @Shadow(remap = false)
    private double discount;
    @Shadow(remap = false)
    private boolean enoughCatalyst;
    @Shadow(remap = false)
    private int extraCatalystNeeded;

    @Shadow(remap = false)
    @Final
    private static HashMap<Fluid, Pair<Long, Float>> FUEL_ENERGY_VALUES;

    @Shadow(remap = false)
    protected abstract GTRecipe recipeAfterAdjustments(GTRecipe recipe, FluidStack[] inputFluids);

    @Unique
    private ItemStack[] dragonfix$currentCatalystItemInputs;

    @Override
    public GTRecipe dragonfix$recipeAfterAdjustmentsWithInputs(GTRecipe recipe, FluidStack[] inputFluids,
        ItemStack[] inputItems) {
        ItemStack[] previousInputs = dragonfix$currentCatalystItemInputs;
        dragonfix$currentCatalystItemInputs = inputItems != null ? inputItems : GTValues.emptyItemStackArray;
        try {
            return recipeAfterAdjustments(recipe, inputFluids);
        } finally {
            dragonfix$currentCatalystItemInputs = previousInputs;
        }
    }

    @Redirect(
        method = "recipeAfterAdjustments(Lgregtech/api/util/GTRecipe;[Lnet/minecraftforge/fluids/FluidStack;)Lgregtech/api/util/GTRecipe;",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/tileentities/machines/multi/MTEPlasmaForge;calculateCatalystIncrease(Lgregtech/api/util/GTRecipe;[Lnet/minecraftforge/fluids/FluidStack;I)V"),
        remap = false,
        require = 2)
    private void dragonfix$calculateCatalystIncrease(MTEPlasmaForge plasmaForge, GTRecipe recipe,
        FluidStack[] inputFluids, int fuelIndex) {
        if (fuelIndex < 0 || fuelIndex >= recipe.mFluidInputs.length) {
            enoughCatalyst = false;
            extraCatalystNeeded = 0;
            return;
        }

        FluidStack validFuelStack = recipe.mFluidInputs[fuelIndex];
        if (validFuelStack == null || validFuelStack.getFluid() == null) {
            enoughCatalyst = false;
            extraCatalystNeeded = 0;
            return;
        }

        Pair<Long, Float> fuelValues = FUEL_ENERGY_VALUES.get(validFuelStack.getFluid());
        if (fuelValues == null || fuelValues.getLeft() == null || fuelValues.getLeft() <= 0) {
            enoughCatalyst = false;
            extraCatalystNeeded = 0;
            return;
        }

        int selectedOverclocks = dragonfix$selectEffectivePerfectOverclocks(recipe, inputFluids, fuelIndex, fuelValues);
        if (selectedOverclocks < 0) {
            enoughCatalyst = false;
            extraCatalystNeeded = 0;
            return;
        }

        extraCatalystNeeded = dragonfix$calculateExtraCatalyst(recipe, fuelValues, selectedOverclocks);
        enoughCatalyst = true;

        recipe.mFluidInputs[fuelIndex].amount = dragonfix$saturatingAdd(
            recipe.mFluidInputs[fuelIndex].amount,
            extraCatalystNeeded);
        dragonfix$addResidueOutput(recipe, fuelValues);
    }

    @Unique
    private int dragonfix$selectEffectivePerfectOverclocks(GTRecipe recipe, FluidStack[] inputFluids, int fuelIndex,
        Pair<Long, Float> fuelValues) {
        int availableOverclocks = dragonfix$getAvailablePowerOverclocks(recipe);
        ItemStack[] inputItems = dragonfix$currentCatalystItemInputs != null ? dragonfix$currentCatalystItemInputs
            : GTValues.emptyItemStackArray;

        for (int overclocks = 0; overclocks <= availableOverclocks; overclocks++) {
            int extraCatalyst = dragonfix$calculateExtraCatalyst(recipe, fuelValues, overclocks);
            GTRecipe candidateRecipe = dragonfix$copyForInputCheck(recipe, fuelIndex, extraCatalyst);
            int possibleParallel = dragonfix$getPossibleInputParallel(candidateRecipe, inputFluids, inputItems);
            if (possibleParallel < 1) continue;

            int requiredOverclocks = Math
                .min(availableOverclocks, (int) GTUtility.log4ceil((long) recipe.mDuration * possibleParallel));
            if (overclocks >= requiredOverclocks) return overclocks;
        }
        return -1;
    }

    @Unique
    private int dragonfix$getAvailablePowerOverclocks(GTRecipe recipe) {
        if (recipe.mEUt <= 0) return 0;

        long powerRatio = ((MTEExtendedPowerMultiBlockBase<?>) (Object) this).getMaxInputEu() / recipe.mEUt;
        if (powerRatio <= 0) return 0;

        return Math.max(0, (int) Math.min(Integer.MAX_VALUE, GTUtility.log4(powerRatio)));
    }

    @Unique
    private int dragonfix$calculateExtraCatalyst(GTRecipe recipe, Pair<Long, Float> fuelValues, int overclocks) {
        if (overclocks <= 0) return 0;

        double extraPowerNeeded = (GTUtility.powInt(2.0D, overclocks) - 1.0D) * recipe.mEUt * recipe.mDuration;
        double extraCatalyst = extraPowerNeeded / fuelValues.getLeft();
        if (Double.isNaN(extraCatalyst) || Double.isInfinite(extraCatalyst) || extraCatalyst >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) extraCatalyst);
    }

    @Unique
    private GTRecipe dragonfix$copyForInputCheck(GTRecipe recipe, int fuelIndex, int extraCatalyst) {
        GTRecipe candidateRecipe = recipe.copy();
        FluidStack fuelStack = candidateRecipe.mFluidInputs[fuelIndex];
        fuelStack.amount = dragonfix$getDiscountedFuelAmount(fuelStack.amount, extraCatalyst);
        return candidateRecipe;
    }

    @Unique
    private int dragonfix$getDiscountedFuelAmount(int fuelAmount, int extraCatalyst) {
        long totalFuel = Math.max(0L, (long) fuelAmount + extraCatalyst);
        if (fuelAmount <= 0) return (int) Math.min(Integer.MAX_VALUE, totalFuel / 2L);

        double discountedFuel = totalFuel * discount;
        if (Double.isNaN(discountedFuel) || Double.isInfinite(discountedFuel) || discountedFuel >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) Math.round(discountedFuel));
    }

    @Unique
    private int dragonfix$getPossibleInputParallel(GTRecipe candidateRecipe, FluidStack[] inputFluids,
        ItemStack[] inputItems) {
        FluidStack[] safeFluidInputs = inputFluids != null ? inputFluids : GTValues.emptyFluidStackArray;
        ItemStack[] safeItemInputs = inputItems != null ? inputItems : GTValues.emptyItemStackArray;

        double possibleParallel = candidateRecipe
            .maxParallelCalculatedByInputs(Integer.MAX_VALUE, safeFluidInputs, safeItemInputs);
        if (possibleParallel <= 0.0D) return 0;
        if (possibleParallel >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(0, (int) possibleParallel);
    }

    @Unique
    private void dragonfix$addResidueOutput(GTRecipe recipe, Pair<Long, Float> fuelValues) {
        if (extraCatalystNeeded <= 0 || fuelValues.getRight() == null) return;

        int extraResidue = dragonfix$toIntSaturated(extraCatalystNeeded * (double) fuelValues.getRight());
        for (FluidStack outputFluid : recipe.mFluidOutputs) {
            if (outputFluid != null
                && outputFluid.isFluidEqual(MaterialsUEVplus.DimensionallyTranscendentResidue.getFluid(1))) {
                outputFluid.amount = dragonfix$saturatingAdd(outputFluid.amount, extraResidue);
            }
        }
    }

    @Unique
    private static int dragonfix$saturatingAdd(int left, int right) {
        long result = (long) left + right;
        if (result <= 0L) return 0;
        if (result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) result;
    }

    @Unique
    private static int dragonfix$toIntSaturated(double value) {
        if (Double.isNaN(value) || value <= 0.0D) return 0;
        if (Double.isInfinite(value) || value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }

}
