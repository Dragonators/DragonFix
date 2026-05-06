package com.dragonfix.mattermanipulator;

import java.lang.reflect.InvocationTargetException;
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
 * Based on the public slot behavior of GTNH Applied Energistics 2 rv3-beta-690-GTNH TileCondenser/ContainerCondenser.
 */
public class AE2CondenserAnalysisResult implements ITileAnalysisIntegration {

    private static final String CONDENSER_CLASS = "appeng.tile.misc.TileCondenser";
    private static final int STORAGE_COMPONENT_SLOT = 2;

    @SerializedName("s")
    private PortableItemStack storageComponent;

    public static AE2CondenserAnalysisResult analyze(TileEntity tile) {
        IInventory internalInventory = dragonfix$getInternalInventory(tile);
        if (internalInventory == null) return null;

        ItemStack stack = internalInventory.getStackInSlot(STORAGE_COMPONENT_SLOT);
        if (stack == null) return null;

        AE2CondenserAnalysisResult result = new AE2CondenserAnalysisResult();
        result.storageComponent = PortableItemStack.withNBT(stack);
        return result;
    }

    public static boolean isMatterCondenserStorageSlot(IInventory inventory, int slot) {
        return inventory != null && slot == STORAGE_COMPONENT_SLOT
            && CONDENSER_CLASS.equals(
                inventory.getClass()
                    .getName());
    }

    private static IInventory dragonfix$getInternalInventory(TileEntity tile) {
        if (tile == null || !CONDENSER_CLASS.equals(
            tile.getClass()
                .getName())) {
            return null;
        }

        try {
            Object internalInventory = tile.getClass()
                .getMethod("getInternalInventory")
                .invoke(tile);
            return internalInventory instanceof IInventory ? (IInventory) internalInventory : null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        return dragonfix$replaceStorageComponent(ctx, true);
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        return dragonfix$replaceStorageComponent(context, false);
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return dragonfix$consumeStorageComponent(context);
    }

    private boolean dragonfix$replaceStorageComponent(IBlockApplyContext context, boolean mutate) {
        IInventory internalInventory = dragonfix$getInternalInventory(context.getTileEntity());
        if (internalInventory == null) return true;

        ItemStack existing = internalInventory.getStackInSlot(STORAGE_COMPONENT_SLOT);
        if (dragonfix$isSameStoredItem(existing, storageComponent)) return true;

        if (!dragonfix$consumeStorageComponent(context)) return false;

        if (existing != null) {
            context.givePlayerItems(existing.copy());
        }

        if (mutate) {
            ItemStack restored = storageComponent == null ? null : storageComponent.toStack();
            internalInventory.setInventorySlotContents(STORAGE_COMPONENT_SLOT, restored);
            internalInventory.markDirty();
            TileEntity tile = context.getTileEntity();
            if (tile != null) tile.markDirty();
        }

        return true;
    }

    private boolean dragonfix$consumeStorageComponent(IBlockApplyContext context) {
        ItemStack stack = storageComponent == null ? null : storageComponent.toStack();
        if (stack == null) return true;

        if (!context.tryConsumeItems(stack)) {
            context.warn("Could not find AE2 condenser storage component: " + stack.getDisplayName());
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
        if (storageComponent != null) tag.setBoolean("AE2CondenserStorageComponent", true);
    }

    @Override
    public void getItemDetails(List<String> details) {
        ItemStack stack = storageComponent == null ? null : storageComponent.toStack();
        if (stack != null) details.add("AE2 condenser storage: " + stack.getDisplayName());
    }

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @Override
    public AE2CondenserAnalysisResult clone() {
        AE2CondenserAnalysisResult dup = new AE2CondenserAnalysisResult();
        dup.storageComponent = storageComponent == null ? null : storageComponent.clone();
        return dup;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(storageComponent);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AE2CondenserAnalysisResult other)) return false;
        return Objects.equals(storageComponent, other.storageComponent);
    }
}
