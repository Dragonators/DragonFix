package com.dragonfix.mattermanipulator;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.dragonfix.mattermanipulator.helper.OpenComputersComponentItemHelper;
import com.recursive_pineapple.matter_manipulator.common.building.IPseudoInventory;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;

import it.unimi.dsi.fastutil.booleans.BooleanObjectImmutablePair;
import li.cil.oc.api.API;

/**
 * Provides equivalent OpenComputers components without requiring copied address-bearing NBT.
 *
 * <p>
 * Adapted from GTNewHorizons/MatterManipulator PR #47 by Vlamonster.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/47">MatterManipulator PR #47</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/37acef5934de822cad490ec22875d48400c9791e">MatterManipulator
 *      commit 37acef59</a>
 */
public class DragonFixComputerComponentItemProvider implements IItemProvider {

    private final ItemStack component;

    public DragonFixComputerComponentItemProvider(ItemStack component) {
        this.component = component;
    }

    public static DragonFixComputerComponentItemProvider fromStack(ItemStack stack) {
        if (!OpenComputersComponentItemHelper.isHandledComponent(stack)) return null;
        return new DragonFixComputerComponentItemProvider(OpenComputersComponentItemHelper.withoutAddress(stack));
    }

    @Override
    public ItemStack getStack(IPseudoInventory inv, boolean consume) {
        if (!consume) return component;

        if (OpenComputersComponentItemHelper.isFuzzyComponent(component)) {
            BooleanObjectImmutablePair<List<BigItemStack>> result = inv.tryConsumeItems(
                Collections.singletonList(BigItemStack.create(component)),
                IPseudoInventory.CONSUME_FUZZY);
            return result.leftBoolean() ? API.items.get(component)
                .createItemStack(1) : null;
        }

        if (OpenComputersComponentItemHelper.isEEPROM(component)) {
            BooleanObjectImmutablePair<List<BigItemStack>> result = inv.tryConsumeItems(
                Collections.singletonList(BigItemStack.create(component)),
                IPseudoInventory.CONSUME_REAL_ONLY);
            return result.leftBoolean() || inv.tryConsumeItems(OpenComputersComponentItemHelper.defaultStack(component))
                ? component.copy()
                : null;
        }

        ItemStack defaultStack = OpenComputersComponentItemHelper.defaultStack(component);
        if (defaultStack != null) return inv.tryConsumeItems(defaultStack) ? defaultStack.copy() : null;

        return null;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public IItemProvider clone() {
        return new DragonFixComputerComponentItemProvider(component);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DragonFixComputerComponentItemProvider provider)) return false;

        return OpenComputersComponentItemHelper.areEquivalentForRestore(component, provider.component);
    }

    @Override
    public int hashCode() {
        return OpenComputersComponentItemHelper.hashForRestore(component);
    }
}
