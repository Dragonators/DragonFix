package com.dragonfix.mattermanipulator.analysis;

import java.util.Arrays;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import wanion.avaritiaddons.block.extremeautocrafter.TileEntityExtremeAutoCrafter;

public class AvaritiaddonsExtremeAutoCrafterAnalysisResult implements ITileAnalysisIntegration {

    public static final int REAL_SLOT_COUNT = 81;
    public static final int GHOST_SLOT_COUNT = 81;
    public static final int FIRST_GHOST_SLOT = REAL_SLOT_COUNT;
    public static final int OUTPUT_SLOT = FIRST_GHOST_SLOT + GHOST_SLOT_COUNT;

    @SerializedName("g")
    private PortableItemStack[] ghostPattern;

    public static AvaritiaddonsExtremeAutoCrafterAnalysisResult analyze(TileEntity tile) {
        if (!(tile instanceof IInventory inventory) || !isExtremeAutoCrafter(inventory)) return null;

        AvaritiaddonsExtremeAutoCrafterAnalysisResult result = new AvaritiaddonsExtremeAutoCrafterAnalysisResult();
        result.ghostPattern = new PortableItemStack[GHOST_SLOT_COUNT];

        boolean hasPattern = false;
        for (int i = 0; i < GHOST_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(FIRST_GHOST_SLOT + i);
            if (stack == null) continue;

            result.ghostPattern[i] = PortableItemStack.withNBT(stack);
            hasPattern = true;
        }

        return hasPattern ? result : null;
    }

    public static boolean isExtremeAutoCrafter(IInventory inventory) {
        return inventory instanceof TileEntityExtremeAutoCrafter;
    }

    public static boolean isGhostOrOutputSlot(IInventory inventory, int slot) {
        return isExtremeAutoCrafter(inventory) && slot >= FIRST_GHOST_SLOT && slot <= OUTPUT_SLOT;
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        if (!(ctx.getTileEntity() instanceof IInventory inventory) || !isExtremeAutoCrafter(inventory)) return true;

        for (int i = 0; i < GHOST_SLOT_COUNT; i++) {
            PortableItemStack portable = ghostPattern == null ? null : ghostPattern[i];
            ItemStack stack = portable == null ? null : portable.toStack();
            if (stack != null) stack.stackSize = 0;

            inventory.setInventorySlotContents(FIRST_GHOST_SLOT + i, stack);
        }

        inventory.markDirty();
        return true;
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        return true;
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return true;
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {
        int count = 0;
        if (ghostPattern != null) {
            for (PortableItemStack stack : ghostPattern) {
                if (stack != null) count++;
            }
        }
        if (count > 0) tag.setInteger("AvaritiaddonsGhostPattern", count);
    }

    @Override
    public void getItemDetails(List<String> details) {
        int count = 0;
        if (ghostPattern != null) {
            for (PortableItemStack stack : ghostPattern) {
                if (stack != null) count++;
            }
        }
        if (count > 0) details.add(count + " ghost pattern slots");
    }

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AvaritiaddonsExtremeAutoCrafterAnalysisResult clone() {
        AvaritiaddonsExtremeAutoCrafterAnalysisResult dup = new AvaritiaddonsExtremeAutoCrafterAnalysisResult();

        if (ghostPattern != null) {
            dup.ghostPattern = new PortableItemStack[ghostPattern.length];
            for (int i = 0; i < ghostPattern.length; i++) {
                dup.ghostPattern[i] = ghostPattern[i] == null ? null : ghostPattern[i].clone();
            }
        }

        return dup;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ghostPattern);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AvaritiaddonsExtremeAutoCrafterAnalysisResult other = (AvaritiaddonsExtremeAutoCrafterAnalysisResult) obj;
        return Arrays.equals(ghostPattern, other.ghostPattern);
    }
}
