package com.dragonfix.mattermanipulator.analysis;

import java.util.List;
import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.NBT;

import com.dragonfix.mattermanipulator.bridge.OpenComputersMicrocontrollerBridge;
import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import li.cil.oc.common.tileentity.Microcontroller;

public class OpenComputersMicrocontrollerAnalysisResult implements ITileAnalysisIntegration {

    private static final String INFO_TAG = "oc:info";
    private static final String COMPONENTS_TAG = "oc:components";
    private static final String STORED_ENERGY_TAG = "oc:storedEnergy";
    private static final String STABLE_ITEM_ID_TAG = "dragonfix:item";

    @SerializedName("t")
    private NBTTagCompound itemTag;

    public static OpenComputersMicrocontrollerAnalysisResult analyze(TileEntity tile) {
        if (!(tile instanceof Microcontroller microcontroller)) return null;

        microcontroller.saveComponents();

        NBTTagCompound tileTag = new NBTTagCompound();
        tile.writeToNBT(tileTag);

        if (!tileTag.hasKey(INFO_TAG, NBT.TAG_COMPOUND)) return null;

        OpenComputersMicrocontrollerAnalysisResult result = new OpenComputersMicrocontrollerAnalysisResult();
        result.itemTag = tileTag.getCompoundTag(INFO_TAG);
        result.writeStableComponentIds(microcontroller);
        return result;
    }

    @Override
    public boolean apply(IBlockApplyContext context) {
        if (itemTag == null || context.getWorld().isRemote) {
            return true;
        }

        TileEntity tile = context.getTileEntity();
        if (!(tile instanceof Microcontroller microcontroller)) return true;

        microcontroller.disconnectComponents();
        microcontroller.info()
            .load(resolveComponentIds((NBTTagCompound) itemTag.copy()));
        if (!resetComponents(context, microcontroller)) return false;
        microcontroller.connectComponents();
        microcontroller.snooperNode()
            .changeBuffer(
                itemTag.getInteger(STORED_ENERGY_TAG) - microcontroller.snooperNode()
                    .localBuffer());
        ((TileEntity) microcontroller).markDirty();
        return true;
    }

    private void writeStableComponentIds(Microcontroller microcontroller) {
        NBTTagList components = itemTag.getTagList(COMPONENTS_TAG, NBT.TAG_COMPOUND);
        int index = 0;

        for (ItemStack stack : microcontroller.internalComponents()) {
            if (stack == null) continue;
            if (index >= components.tagCount()) break;

            UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            if (id != null) {
                components.getCompoundTagAt(index)
                    .setString(STABLE_ITEM_ID_TAG, id.modId + ":" + id.name);
            }
            index++;
        }
    }

    private static NBTTagCompound resolveComponentIds(NBTTagCompound info) {
        NBTTagList components = info.getTagList(COMPONENTS_TAG, NBT.TAG_COMPOUND);
        for (int i = 0; i < components.tagCount(); i++) {
            resolveComponentId(components.getCompoundTagAt(i));
        }
        return info;
    }

    private static void resolveComponentId(NBTTagCompound stackTag) {
        String stableId = stackTag.getString(STABLE_ITEM_ID_TAG);
        int separator = stableId.indexOf(':');
        if (separator <= 0 || separator == stableId.length() - 1) return;

        Item item = GameRegistry.findItem(stableId.substring(0, separator), stableId.substring(separator + 1));
        if (item != null) {
            stackTag.setShort("id", (short) Item.getIdFromItem(item));
        }
    }

    private static boolean resetComponents(IBlockApplyContext context, Microcontroller microcontroller) {
        if (!(microcontroller instanceof OpenComputersMicrocontrollerBridge bridge)) {
            context.error("Could not access OpenComputers microcontroller component reset bridge.");
            return false;
        }

        if (!bridge.dragonfix$resetComponentEnvironments()) {
            context.error("Could not reset OpenComputers microcontroller components.");
            return false;
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
    public void getItemTag(NBTTagCompound tag) {
        if (itemTag == null) return;

        NBTTagCompound resolved = resolveComponentIds((NBTTagCompound) itemTag.copy());
        for (String name : resolved.func_150296_c()) {
            tag.setTag(
                name,
                resolved.getTag(name)
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
    public OpenComputersMicrocontrollerAnalysisResult clone() {
        OpenComputersMicrocontrollerAnalysisResult dup = new OpenComputersMicrocontrollerAnalysisResult();
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
        if (!(obj instanceof OpenComputersMicrocontrollerAnalysisResult other)) return false;
        return Objects.equals(itemTag, other.itemTag);
    }
}
