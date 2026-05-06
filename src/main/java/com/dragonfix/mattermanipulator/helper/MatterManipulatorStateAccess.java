package com.dragonfix.mattermanipulator.helper;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;

public final class MatterManipulatorStateAccess {

    private MatterManipulatorStateAccess() {}

    public static boolean isMatterManipulator(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemMatterManipulator;
    }

    public static MMState getState(ItemStack stack) {
        return ItemMatterManipulator.getState(stack);
    }

    public static void setState(ItemStack stack, MMState state) {
        ItemMatterManipulator.setState(stack, state);
    }
}
