package com.dragonfix.mattermanipulator.Analysis;

import java.util.List;
import java.util.Objects;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.dragonfix.mattermanipulator.helper.InventorySlotCopyHelper;
import com.dragonfix.mattermanipulator.helper.SpecialInventorySlots;
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
    private static final String CAPACITOR_NAME = "Ender IO soul binder capacitor";

    @SerializedName("c")
    private PortableItemStack capacitor;

    public static EnderIOSoulBinderAnalysisResult analyze(TileEntity tile) {
        if (!(tile instanceof IInventory inventory) || !isSoulBinder(inventory)) return null;

        PortableItemStack storedItem = InventorySlotCopyHelper.analyzeSlot(inventory, CAPACITOR_SLOT);
        if (storedItem == null) return null;

        EnderIOSoulBinderAnalysisResult result = new EnderIOSoulBinderAnalysisResult();
        result.capacitor = storedItem;
        return result;
    }

    public static boolean isSoulBinderCapacitorSlot(IInventory inventory, int slot) {
        return isSoulBinder(inventory) && slot == CAPACITOR_SLOT;
    }

    private static boolean isSoulBinder(IInventory inventory) {
        return SpecialInventorySlots.isExactInventoryClass(inventory, SOUL_BINDER_CLASS);
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
        return InventorySlotCopyHelper.consumeItem(context, capacitor, CAPACITOR_NAME);
    }

    private boolean dragonfix$replaceCapacitor(IBlockApplyContext context, boolean mutate) {
        TileEntity tile = context.getTileEntity();
        if (!(tile instanceof IInventory inventory) || !isSoulBinder(inventory)) return true;

        return InventorySlotCopyHelper
            .replaceSlot(context, inventory, tile, CAPACITOR_SLOT, capacitor, CAPACITOR_NAME, mutate);
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {
        if (capacitor != null) tag.setBoolean("EnderIOSoulBinderCapacitor", true);
    }

    @Override
    public void getItemDetails(List<String> details) {
        ItemStack stack = InventorySlotCopyHelper.toStack(capacitor);
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
        if (!(obj instanceof EnderIOSoulBinderAnalysisResult other)) return false;
        return Objects.equals(capacitor, other.capacitor);
    }
}
