package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.DragonFixComputerComponentItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.InventoryAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;

import cpw.mods.fml.common.Loader;

/**
 * Adapted from GTNewHorizons/MatterManipulator PR #47 by Vlamonster:
 * https://github.com/GTNewHorizons/MatterManipulator/pull/47
 * https://github.com/GTNewHorizons/MatterManipulator/commit/37acef5934de822cad490ec22875d48400c9791e
 */
@Mixin(value = InventoryAnalysis.class, remap = false)
public abstract class InventoryAnalysisMixin {

    @Inject(method = "getProviderFor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$getOpenComputersProvider(ItemStack stack, boolean fuzzy,
        CallbackInfoReturnable<IItemProvider> cir) {
        if (stack == null || stack.getItem() == null || !Loader.isModLoaded("OpenComputers")) return;

        IItemProvider component = DragonFixComputerComponentItemProvider.fromStack(stack);
        if (component != null) {
            cir.setReturnValue(component);
        }
    }
}
