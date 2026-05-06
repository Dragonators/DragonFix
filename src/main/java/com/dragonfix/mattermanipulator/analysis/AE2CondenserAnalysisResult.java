package com.dragonfix.mattermanipulator.analysis;

import java.util.List;
import java.util.Objects;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.dragonfix.mattermanipulator.helper.InventorySlotCopyHelper;
import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import appeng.tile.misc.TileCondenser;

public class AE2CondenserAnalysisResult implements ITileAnalysisIntegration {

    private static final int STORAGE_COMPONENT_SLOT = 2;
    private static final String STORAGE_COMPONENT_NAME = "AE2 condenser storage component";

    @SerializedName("s")
    private PortableItemStack storageComponent;

    public static AE2CondenserAnalysisResult analyze(TileEntity tile) {
        IInventory internalInventory = getInternalInventory(tile);
        if (internalInventory == null) return null;

        PortableItemStack storedItem = InventorySlotCopyHelper.analyzeSlot(internalInventory, STORAGE_COMPONENT_SLOT);
        if (storedItem == null) return null;

        AE2CondenserAnalysisResult result = new AE2CondenserAnalysisResult();
        result.storageComponent = storedItem;
        return result;
    }

    public static boolean isMatterCondenserStorageSlot(IInventory inventory, int slot) {
        return slot == STORAGE_COMPONENT_SLOT && inventory instanceof TileCondenser;
    }

    private static IInventory getInternalInventory(TileEntity tile) {
        return tile instanceof TileCondenser ? ((TileCondenser) tile).getInternalInventory() : null;
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        return replaceStorageComponent(ctx, true);
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        return replaceStorageComponent(context, false);
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return InventorySlotCopyHelper.consumeItem(context, storageComponent, STORAGE_COMPONENT_NAME);
    }

    private boolean replaceStorageComponent(IBlockApplyContext context, boolean mutate) {
        IInventory internalInventory = getInternalInventory(context.getTileEntity());
        if (internalInventory == null) return true;

        return InventorySlotCopyHelper.replaceSlot(
            context,
            internalInventory,
            context.getTileEntity(),
            STORAGE_COMPONENT_SLOT,
            storageComponent,
            STORAGE_COMPONENT_NAME,
            mutate);
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {
        if (storageComponent != null) tag.setBoolean("AE2CondenserStorageComponent", true);
    }

    @Override
    public void getItemDetails(List<String> details) {
        ItemStack stack = InventorySlotCopyHelper.toStack(storageComponent);
        if (stack != null) details.add("AE2 condenser storage: " + stack.getDisplayName());
    }

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @SuppressWarnings("MethodDoesntCallSuperMethod")
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
