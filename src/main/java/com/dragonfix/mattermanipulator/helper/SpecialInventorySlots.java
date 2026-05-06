package com.dragonfix.mattermanipulator.helper;

import net.minecraft.inventory.IInventory;

import com.dragonfix.mattermanipulator.Analysis.AE2CondenserAnalysisResult;
import com.dragonfix.mattermanipulator.Analysis.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.Analysis.EnderIOSoulBinderAnalysisResult;

public final class SpecialInventorySlots {

    public static boolean isHandledByDragonFix(IInventory inventory, int slot) {
        return AvaritiaddonsExtremeAutoCrafterAnalysisResult.isGhostOrOutputSlot(inventory, slot)
            || AE2CondenserAnalysisResult.isMatterCondenserStorageSlot(inventory, slot)
            || EnderIOSoulBinderAnalysisResult.isSoulBinderCapacitorSlot(inventory, slot);
    }

    public static boolean isExactInventoryClass(IInventory inventory, String className) {
        return inventory != null && className.equals(
            inventory.getClass()
                .getName());
    }
}
