package com.dragonfix.mattermanipulator.helper;

import net.minecraft.inventory.IInventory;

import com.dragonfix.mattermanipulator.analysis.AE2CondenserAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.EnderIOSoulBinderAnalysisResult;

public final class SpecialInventorySlots {

    public static boolean isHandledByDragonFix(IInventory inventory, int slot) {
        return AvaritiaddonsExtremeAutoCrafterAnalysisResult.isGhostOrOutputSlot(inventory, slot)
            || AE2CondenserAnalysisResult.isMatterCondenserStorageSlot(inventory, slot)
            || EnderIOSoulBinderAnalysisResult.isSoulBinderCapacitorSlot(inventory, slot);
    }
}
