package com.dragonfix.mattermanipulator.analysis;

import java.util.List;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

public class DoorAnalysisResult implements ITileAnalysisIntegration {

    @SerializedName("u")
    private int upperMeta = 8;

    public static DoorAnalysisResult analyze(World world, int x, int y, int z, Block block, int meta) {
        if (!(block instanceof BlockDoor) || (meta & 8) != 0) return null;

        DoorAnalysisResult result = new DoorAnalysisResult();

        if (world.getBlock(x, y + 1, z) == block) {
            int topMeta = world.getBlockMetadata(x, y + 1, z);
            if ((topMeta & 8) != 0) result.upperMeta = topMeta;
        }

        return result;
    }

    @Override
    public boolean apply(IBlockApplyContext context) {
        World world = context.getWorld();
        if (!(world.getBlock(context.getX(), context.getY(), context.getZ()) instanceof BlockDoor door)) return true;

        int x = context.getX();
        int y = context.getY();
        int z = context.getZ();

        if (world.getBlock(x, y + 1, z) != door) {
            world.setBlock(x, y + 1, z, door, upperMeta | 8, 2);
        } else {
            world.setBlockMetadataWithNotify(x, y + 1, z, upperMeta | 8, 2);
        }

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
    public void getItemTag(NBTTagCompound tag) {}

    @Override
    public void getItemDetails(List<String> details) {}

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public DoorAnalysisResult clone() {
        DoorAnalysisResult dup = new DoorAnalysisResult();
        dup.upperMeta = upperMeta;
        return dup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(upperMeta);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DoorAnalysisResult other)) return false;
        return upperMeta == other.upperMeta;
    }
}
