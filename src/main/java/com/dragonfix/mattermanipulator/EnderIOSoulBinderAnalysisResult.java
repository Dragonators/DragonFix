package com.dragonfix.mattermanipulator;

import java.util.List;
import java.util.Objects;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

/**
 * Based on the public slot behavior of GTNH Ender IO 2.9.28 TileSoulBinder/AbstractPoweredMachineEntity.
 */
public class EnderIOSoulBinderAnalysisResult implements ITileAnalysisIntegration {

    private static final String SOUL_BINDER_CLASS = "crazypants.enderio.machine.soul.TileSoulBinder";
    private static final int CAPACITOR_SLOT = 4;

    @SerializedName("c")
    private PortableItemStack capacitor;

    public static EnderIOSoulBinderAnalysisResult analyze(TileEntity tile) {
        if (!(tile instanceof IInventory inventory) || !isSoulBinder(inventory)) return null;

        ItemStack stack = inventory.getStackInSlot(CAPACITOR_SLOT);
        if (stack == null) return null;

        EnderIOSoulBinderAnalysisResult result = new EnderIOSoulBinderAnalysisResult();
        result.capacitor = PortableItemStack.withNBT(stack);
        return result;
    }

    public static boolean isSoulBinderCapacitorSlot(IInventory inventory, int slot) {
        return isSoulBinder(inventory) && slot == CAPACITOR_SLOT;
    }

    private static boolean isSoulBinder(IInventory inventory) {
        return inventory != null && SOUL_BINDER_CLASS.equals(
            inventory.getClass()
                .getName());
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        return dragonfix$replaceCapacitor(ctx, true);
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        return dragonfix$replaceCapacitor(context, false);
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return dragonfix$consumeCapacitor(context);
    }

    private boolean dragonfix$replaceCapacitor(IBlockApplyContext context, boolean mutate) {
        TileEntity tile = context.getTileEntity();
        if (!(tile instanceof IInventory inventory) || !isSoulBinder(inventory)) return true;

        ItemStack existing = inventory.getStackInSlot(CAPACITOR_SLOT);
        if (dragonfix$isSameStoredItem(existing, capacitor)) return true;

        if (!dragonfix$consumeCapacitor(context)) return false;

        if (existing != null) {
            context.givePlayerItems(existing.copy());
        }

        if (mutate) {
            ItemStack restored = capacitor == null ? null : capacitor.toStack();
            inventory.setInventorySlotContents(CAPACITOR_SLOT, restored);
            inventory.markDirty();
            tile.markDirty();
        }

        return true;
    }

    private boolean dragonfix$consumeCapacitor(IBlockApplyContext context) {
        ItemStack stack = capacitor == null ? null : capacitor.toStack();
        if (stack == null) return true;

        if (!context.tryConsumeItems(stack)) {
            context.warn("Could not find Ender IO soul binder capacitor: " + stack.getDisplayName());
            return false;
        }

        return true;
    }

    private static boolean dragonfix$isSameStoredItem(ItemStack stack, PortableItemStack portable) {
        ItemStack target = portable == null ? null : portable.toStack();
        if (stack == null || target == null) return stack == target;
        return ItemStack.areItemStacksEqual(stack, target);
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {
        if (capacitor != null) tag.setBoolean("EnderIOSoulBinderCapacitor", true);
    }

    @Override
    public void getItemDetails(List<String> details) {
        ItemStack stack = capacitor == null ? null : capacitor.toStack();
        if (stack != null) details.add("Ender IO soul binder capacitor: " + stack.getDisplayName());
    }

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @Override
    public EnderIOSoulBinderAnalysisResult clone() {
        EnderIOSoulBinderAnalysisResult dup = new EnderIOSoulBinderAnalysisResult();
        dup.capacitor = capacitor == null ? null : capacitor.clone();
        return dup;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(capacitor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EnderIOSoulBinderAnalysisResult)) return false;
        EnderIOSoulBinderAnalysisResult other = (EnderIOSoulBinderAnalysisResult) obj;
        return Objects.equals(capacitor, other.capacitor);
    }
}
