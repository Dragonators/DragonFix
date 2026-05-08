package com.dragonfix.mattermanipulator.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import li.cil.oc.api.API;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public final class OpenComputersMicrocontrollerItemHelper {

    private static final String TIER_TAG = "oc:tier";
    private static final String COMPONENTS_TAG = "oc:components";

    private OpenComputersMicrocontrollerItemHelper() {}

    public static boolean isMicrocontroller(ItemStack stack) {
        return stack != null && Objects.equals(API.items.get(stack), API.items.get("microcontroller"));
    }

    public static boolean matches(ItemStack expected, ItemStack candidate) {
        if (!isMicrocontroller(expected) || !isMicrocontroller(candidate)) return false;
        NBTTagCompound expectedTag = expected.getTagCompound();
        NBTTagCompound candidateTag = candidate.getTagCompound();
        if (expectedTag == null || candidateTag == null) return false;
        if (expectedTag.getByte(TIER_TAG) != candidateTag.getByte(TIER_TAG)) return false;

        List<ItemStack> expectedComponents = readComponents(expectedTag);
        List<ItemStack> candidateComponents = readComponents(candidateTag);
        if (expectedComponents.size() != candidateComponents.size()) return false;

        for (int i = 0; i < expectedComponents.size(); i++) {
            if (!OpenComputersComponentItemHelper
                .areEquivalentForRestore(expectedComponents.get(i), candidateComponents.get(i))) return false;
        }

        return true;
    }

    private static List<ItemStack> readComponents(NBTTagCompound tag) {
        NBTTagList components = tag.getTagList(COMPONENTS_TAG, NBT.TAG_COMPOUND);
        ArrayList<ItemStack> out = new ArrayList<>(components.tagCount());

        for (int i = 0; i < components.tagCount(); i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(components.getCompoundTagAt(i));
            if (stack != null) out.add(stack);
        }

        return out;
    }
}
