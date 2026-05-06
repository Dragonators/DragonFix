package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.DragonFixComputerComponentItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.InventoryAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.utils.InventoryAdapter;

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

    @Redirect(
        method = "fromInventory",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;isValidSlot(Lnet/minecraft/inventory/IInventory;I)Z"),
        remap = false)
    private static boolean dragonfix$isCopyableInventorySlot(InventoryAdapter adapter,
        net.minecraft.inventory.IInventory inv, int slot) {
        return adapter.isValidSlot(inv, slot)
            && !AvaritiaddonsExtremeAutoCrafterAnalysisResult.isGhostOrOutputSlot(inv, slot);
    }

    @Redirect(
        method = "apply(Lcom/recursive_pineapple/matter_manipulator/common/building/BlockAnalyzer$IBlockApplyContext;Lnet/minecraft/inventory/IInventory;Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;ZZ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;isValidSlot(Lnet/minecraft/inventory/IInventory;I)Z"),
        remap = false)
    private boolean dragonfix$isRestorableInventorySlot(InventoryAdapter adapter,
        net.minecraft.inventory.IInventory inv, int slot) {
        return adapter.isValidSlot(inv, slot)
            && !AvaritiaddonsExtremeAutoCrafterAnalysisResult.isGhostOrOutputSlot(inv, slot);
    }
}
