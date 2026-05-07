package com.dragonfix.mattermanipulator.analysis;

import java.util.List;
import java.util.Objects;

import net.malisis.doors.door.tileentity.CustomDoorTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

public class MalisisCustomDoorAnalysisResult implements ITileAnalysisIntegration {

    @SerializedName("t")
    private NBTTagCompound itemTag;

    public static MalisisCustomDoorAnalysisResult analyze(TileEntity tile) {
        if (!(tile instanceof CustomDoorTileEntity)) return null;

        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        tag.removeTag("x");
        tag.removeTag("y");
        tag.removeTag("z");

        if (!tag.hasKey("frame") || !tag.hasKey("topMaterial") || !tag.hasKey("bottomMaterial")) return null;

        MalisisCustomDoorAnalysisResult result = new MalisisCustomDoorAnalysisResult();
        result.itemTag = tag;
        return result;
    }

    @Override
    public boolean apply(IBlockApplyContext context) {
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
        if (itemTag == null) return;

        for (String name : itemTag.func_150296_c()) {
            tag.setTag(
                name,
                itemTag.getTag(name)
                    .copy());
        }
    }

    @Override
    public void getItemDetails(List<String> details) {}

    @Override
    public void transform(Transform transform) {}

    @Override
    public void migrate() {}

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public MalisisCustomDoorAnalysisResult clone() {
        MalisisCustomDoorAnalysisResult dup = new MalisisCustomDoorAnalysisResult();
        dup.itemTag = itemTag == null ? null : (NBTTagCompound) itemTag.copy();
        return dup;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(itemTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MalisisCustomDoorAnalysisResult other)) return false;
        return Objects.equals(itemTag, other.itemTag);
    }
}
