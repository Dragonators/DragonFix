package com.dragonfix.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

public interface PlasmaForgeCatalystAdjustment {

    GTRecipe dragonfix$recipeAfterAdjustmentsWithInputs(GTRecipe recipe, FluidStack[] inputFluids,
        ItemStack[] inputItems);
}
