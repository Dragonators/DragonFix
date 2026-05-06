package com.dragonfix.mattermanipulator.Analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.carpentersblocks.data.Slope;
import com.carpentersblocks.data.Stairs;
import com.carpentersblocks.tileentity.TEBase;
import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

/**
 * Adapted from GTNewHorizons/MatterManipulator PR #46 by Luca-Guettinger:
 * https://github.com/GTNewHorizons/MatterManipulator/pull/46
 */
public class CarpentersBlocksAnalysisResult implements ITileAnalysisIntegration {

    private static final byte TYPE_UNKNOWN = 0;
    private static final byte TYPE_STAIRS = 1;
    private static final byte TYPE_SLAB = 2;
    private static final byte TYPE_SLOPE = 3;

    private static final int SLOPE_COUNT = 65;

    // Slab data to ForgeDirection ordinal mapping, from Slab.DIR_MAP.
    private static final int[] SLAB_DIR_MAP = { 4, 5, 0, 1, 2, 3 };

    @SerializedName("d")
    private int data;

    @SerializedName("t")
    private byte blockType;

    @SerializedName("c")
    private PortableItemStack[] covers;

    public static CarpentersBlocksAnalysisResult analyze(TileEntity te) {
        if (!(te instanceof TEBase)) return null;

        TEBase cbTE = (TEBase) te;
        CarpentersBlocksAnalysisResult result = new CarpentersBlocksAnalysisResult();

        result.data = cbTE.getData();
        result.blockType = dragonfix$getBlockType(te);
        result.covers = dragonfix$analyzeCovers(cbTE);

        return result;
    }

    private static byte dragonfix$getBlockType(TileEntity te) {
        String blockName = te.getBlockType()
            .getUnlocalizedName();

        if (blockName.contains("Stairs")) return TYPE_STAIRS;
        if (blockName.contains("Slope")) return TYPE_SLOPE;
        if (blockName.contains("blockCarpentersBlock")) return TYPE_SLAB;
        return TYPE_UNKNOWN;
    }

    private static PortableItemStack[] dragonfix$analyzeCovers(TEBase cbTE) {
        PortableItemStack[] analyzedCovers = new PortableItemStack[7];
        boolean hasCovers = false;

        for (int i = 0; i < 7; i++) {
            ItemStack cover = cbTE.getAttribute(TEBase.ATTR_COVER[i]);
            if (cover != null) {
                analyzedCovers[i] = new PortableItemStack(cover);
                hasCovers = true;
            }
        }

        return hasCovers ? analyzedCovers : null;
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        TileEntity te = ctx.getTileEntity();
        if (!(te instanceof TEBase)) return false;

        TEBase cbTE = (TEBase) te;
        cbTE.setData(data);

        if (covers == null) return true;

        for (int i = 0; i < covers.length && i < 7; i++) {
            if (covers[i] == null) continue;

            ItemStack coverStack = covers[i].toStack();
            if (coverStack == null) continue;

            if (cbTE.hasAttribute(TEBase.ATTR_COVER[i])) {
                ItemStack existing = cbTE.getAttribute(TEBase.ATTR_COVER[i]);
                if (existing != null) {
                    ctx.givePlayerItems(existing.copy());
                }
                cbTE.onAttrDropped(TEBase.ATTR_COVER[i]);
            }

            if (!ctx.tryConsumeItems(coverStack)) {
                ctx.warn("Could not find cover: " + coverStack.getDisplayName());
                return false;
            }

            cbTE.addAttribute(TEBase.ATTR_COVER[i], coverStack);
        }

        return true;
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        TileEntity te = context.getTileEntity();
        if (!(te instanceof TEBase)) return false;

        if (covers == null) return true;

        TEBase cbTE = (TEBase) te;
        for (int i = 0; i < covers.length && i < 7; i++) {
            if (covers[i] == null) continue;

            ItemStack existing = cbTE.getAttribute(TEBase.ATTR_COVER[i]);
            if (existing != null) {
                PortableItemStack existingPortable = new PortableItemStack(existing);
                if (dragonfix$isSameItem(existingPortable, covers[i])) {
                    continue;
                }
                context.givePlayerItems(existing.copy());
            }

            ItemStack needed = covers[i].toStack();
            if (needed != null && !context.tryConsumeItems(needed)) return false;
        }

        return true;
    }

    private static boolean dragonfix$isSameItem(PortableItemStack a, PortableItemStack b) {
        return a.item != null && a.item.equals(b.item) && Objects.equals(a.metadata, b.metadata);
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        if (covers == null) return true;

        for (int i = 0; i < covers.length && i < 7; i++) {
            if (covers[i] == null) continue;

            ItemStack needed = covers[i].toStack();
            if (needed != null && !context.tryConsumeItems(needed)) return false;
        }

        return true;
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {}

    @Override
    public void getItemDetails(List<String> details) {
        if (covers != null && covers.length > 6 && covers[6] != null) {
            ItemStack stack = covers[6].toStack();
            if (stack != null) details.add(stack.getDisplayName());
        }
    }

    @Override
    public void transform(Transform transform) {
        switch (blockType) {
            case TYPE_STAIRS:
                dragonfix$transformStairs(transform);
                break;
            case TYPE_SLOPE:
                dragonfix$transformSlope(transform);
                break;
            case TYPE_SLAB:
                dragonfix$transformSlab(transform);
                break;
            default:
                break;
        }
    }

    private static List<ForgeDirection> dragonfix$transformFacings(List<ForgeDirection> facings, Transform transform) {
        List<ForgeDirection> result = new ArrayList<>(facings.size());
        for (ForgeDirection facing : facings) {
            result.add(transform.apply(facing));
        }
        return result;
    }

    private static boolean dragonfix$facingsMatch(List<ForgeDirection> a, List<ForgeDirection> b) {
        return a.size() == b.size() && a.containsAll(b);
    }

    private static boolean dragonfix$stairsTypesCompatible(Stairs.Type a, Stairs.Type b) {
        if (a == b) return true;
        return (a == Stairs.Type.NORMAL_SIDE || a == Stairs.Type.NORMAL)
            && (b == Stairs.Type.NORMAL_SIDE || b == Stairs.Type.NORMAL);
    }

    private static boolean dragonfix$slopeTypesCompatible(Slope.Type a, Slope.Type b) {
        if (a == b) return true;
        return (a == Slope.Type.WEDGE_SIDE || a == Slope.Type.WEDGE)
            && (b == Slope.Type.WEDGE_SIDE || b == Slope.Type.WEDGE);
    }

    @SuppressWarnings("unchecked")
    private void dragonfix$transformStairs(Transform transform) {
        if (data < 0 || data >= Stairs.stairsList.length) return;

        Stairs stairs = Stairs.stairsList[data];
        if (stairs == null) return;

        List<ForgeDirection> facings = stairs.facings;
        List<ForgeDirection> newFacings = dragonfix$transformFacings(facings, transform);

        for (Stairs candidate : Stairs.stairsList) {
            if (candidate != null && dragonfix$stairsTypesCompatible(candidate.stairsType, stairs.stairsType)
                && dragonfix$facingsMatch(candidate.facings, newFacings)) {
                data = candidate.stairsID;
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void dragonfix$transformSlope(Transform transform) {
        Slope slope = Slope.getSlopeById(data);
        if (slope == null) return;

        List<ForgeDirection> facings = slope.facings;
        List<ForgeDirection> newFacings = dragonfix$transformFacings(facings, transform);

        for (int i = 0; i < SLOPE_COUNT; i++) {
            Slope candidate = Slope.getSlopeById(i);
            if (candidate != null && dragonfix$slopeTypesCompatible(candidate.type, slope.type)
                && dragonfix$facingsMatch(candidate.facings, newFacings)) {
                data = candidate.slopeID;
                return;
            }
        }
    }

    private void dragonfix$transformSlab(Transform transform) {
        if (data < 1 || data > 6) return;

        ForgeDirection dir = ForgeDirection.getOrientation(SLAB_DIR_MAP[data - 1]);
        ForgeDirection newDir = transform.apply(dir);

        for (int i = 0; i < SLAB_DIR_MAP.length; i++) {
            if (SLAB_DIR_MAP[i] == newDir.ordinal()) {
                data = i + 1;
                return;
            }
        }
    }

    @Override
    public void migrate() {}

    @Override
    public CarpentersBlocksAnalysisResult clone() {
        CarpentersBlocksAnalysisResult dup = new CarpentersBlocksAnalysisResult();

        dup.data = data;
        dup.blockType = blockType;
        if (covers != null) {
            dup.covers = new PortableItemStack[covers.length];
            for (int i = 0; i < covers.length; i++) {
                dup.covers[i] = covers[i] == null ? null : covers[i].clone();
            }
        }

        return dup;
    }

    @Override
    public int hashCode() {
        int result = data;
        result = 31 * result + blockType;
        result = 31 * result + Arrays.hashCode(covers);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CarpentersBlocksAnalysisResult)) return false;

        CarpentersBlocksAnalysisResult other = (CarpentersBlocksAnalysisResult) obj;
        return data == other.data && blockType == other.blockType && Arrays.equals(covers, other.covers);
    }

    @Override
    public String toString() {
        return "CarpentersBlocksAnalysisResult [data=" + data
            + ", blockType="
            + blockType
            + ", covers="
            + Arrays.toString(covers)
            + "]";
    }
}
