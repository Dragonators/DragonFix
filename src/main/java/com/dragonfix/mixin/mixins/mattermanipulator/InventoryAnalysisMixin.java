package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mattermanipulator.DragonFixComputerComponentItemProvider;
import com.dragonfix.mattermanipulator.helper.SpecialInventorySlots;
import com.recursive_pineapple.matter_manipulator.common.building.InventoryAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.building.providers.AECellItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.BatteryItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.PatternItemProvider;
import com.recursive_pineapple.matter_manipulator.common.utils.InventoryAdapter;
import com.recursive_pineapple.matter_manipulator.common.utils.Mods;

import cpw.mods.fml.common.Loader;

/**
 * Extends MatterManipulator inventory analysis for OpenComputers and DragonFix-managed special slots.
 *
 * <p>
 * OpenComputers provider logic adapted from GTNewHorizons/MatterManipulator PR #47 by Vlamonster.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/47">MatterManipulator PR #47</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/37acef5934de822cad490ec22875d48400c9791e">MatterManipulator
 *      commit 37acef59</a>
 */
@Mixin(value = InventoryAnalysis.class, remap = false)
public abstract class InventoryAnalysisMixin {

    /**
     * @author DragonFix
     * @reason Fold OpenComputers and special provider handling into the original provider selection order.
     */
    @Overwrite(remap = false)
    private static IItemProvider getProviderFor(ItemStack stack, boolean fuzzy) {
        if (stack == null || stack.getItem() == null) return null;

        if (Loader.isModLoaded("OpenComputers")) {
            IItemProvider component = DragonFixComputerComponentItemProvider.fromStack(stack);
            if (component != null) return component;
        }

        if (Mods.AppliedEnergistics2.isModLoaded()) {
            if (!fuzzy) {
                IItemProvider cell = AECellItemProvider.fromWorkbenchItem(stack);
                if (cell != null) return cell;
            }

            IItemProvider pattern = PatternItemProvider.fromPattern(stack);
            if (pattern != null) return pattern;
        }

        IItemProvider battery = BatteryItemProvider.fromStack(stack);
        if (battery != null) return battery;

        return fuzzy ? new PortableItemStack(stack) : PortableItemStack.withNBT(stack);
    }

    @Redirect(
        method = "fromInventory",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;isValidSlot(Lnet/minecraft/inventory/IInventory;I)Z"),
        remap = false)
    private static boolean dragonfix$isCopyableInventorySlot(InventoryAdapter adapter,
        net.minecraft.inventory.IInventory inv, int slot) {
        return adapter.isValidSlot(inv, slot) && !SpecialInventorySlots.isHandledByDragonFix(inv, slot);
    }

    @Redirect(
        method = "apply(Lcom/recursive_pineapple/matter_manipulator/common/building/BlockAnalyzer$IBlockApplyContext;Lnet/minecraft/inventory/IInventory;Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;ZZ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/utils/InventoryAdapter;isValidSlot(Lnet/minecraft/inventory/IInventory;I)Z"),
        remap = false)
    private boolean dragonfix$isRestorableInventorySlot(InventoryAdapter adapter,
        net.minecraft.inventory.IInventory inv, int slot) {
        return adapter.isValidSlot(inv, slot) && !SpecialInventorySlots.isHandledByDragonFix(inv, slot);
    }
}
