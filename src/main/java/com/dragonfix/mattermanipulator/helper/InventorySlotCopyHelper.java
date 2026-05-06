package com.dragonfix.mattermanipulator.helper;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;

public final class InventorySlotCopyHelper {

    public static PortableItemStack analyzeSlot(IInventory inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return stack == null ? null : PortableItemStack.withNBT(stack);
    }

    public static boolean replaceSlot(IBlockApplyContext context, IInventory inventory, TileEntity tile, int slot,
        PortableItemStack storedItem, String missingItemName, boolean mutate) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (isSameStoredItem(existing, storedItem)) return true;

        if (!consumeItem(context, storedItem, missingItemName)) return false;

        if (existing != null) {
            context.givePlayerItems(existing.copy());
        }

        if (mutate) {
            inventory.setInventorySlotContents(slot, toStack(storedItem));
            inventory.markDirty();
            if (tile != null) tile.markDirty();
        }

        return true;
    }

    public static boolean consumeItem(IBlockApplyContext context, PortableItemStack storedItem,
        String missingItemName) {
        ItemStack stack = toStack(storedItem);
        if (stack == null) return true;

        if (!context.tryConsumeItems(stack)) {
            context.warn("Could not find " + missingItemName + ": " + stack.getDisplayName());
            return false;
        }

        return true;
    }

    public static ItemStack toStack(PortableItemStack storedItem) {
        return storedItem == null ? null : storedItem.toStack();
    }

    private static boolean isSameStoredItem(ItemStack stack, PortableItemStack storedItem) {
        ItemStack target = toStack(storedItem);
        if (stack == null || target == null) return stack == target;
        return ItemStack.areItemStacksEqual(stack, target);
    }
}
