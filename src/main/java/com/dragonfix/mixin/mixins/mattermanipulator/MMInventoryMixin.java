package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorMicrocontrollerConsumptionHelper;
import com.recursive_pineapple.matter_manipulator.common.building.IPseudoInventory;
import com.recursive_pineapple.matter_manipulator.common.building.MMInventory;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;
import com.recursive_pineapple.matter_manipulator.common.utils.Mods;

import appeng.api.networking.storage.IStorageGrid;
import it.unimi.dsi.fastutil.booleans.BooleanObjectImmutablePair;

@Mixin(value = MMInventory.class, remap = false)
public abstract class MMInventoryMixin implements IPseudoInventory {

    @Shadow(remap = false)
    public EntityPlayer player;

    @Shadow(remap = false)
    public MMState state;

    @org.spongepowered.asm.mixin.Final
    @Shadow(remap = false)
    private HashSet<IStorageGrid> visitedGrids;

    @Shadow(remap = false)
    private static ArrayList<BigItemStack> merge(List<BigItemStack> unmerged) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private void consumeItemsFromPending(List<BigItemStack> requestedItems, List<BigItemStack> extractedItems,
        int flags) {}

    @Shadow(remap = false)
    private void consumeItemsFromPlayer(List<BigItemStack> requestedItems, List<BigItemStack> extractedItems,
        int flags) {}

    @Shadow(remap = false)
    private void consumeItemsFromAE(List<BigItemStack> requestedItems, List<BigItemStack> extractedItems, int flags) {}

    @Shadow(remap = false)
    private void consumeItemsFromUplink(List<BigItemStack> requestedItems, List<BigItemStack> extractedItems,
        int flags) {}

    /**
     * @author DragonFix
     * @reason Add OpenComputers microcontroller equivalence to the normal item sourcing pipeline.
     */
    @Overwrite(remap = false)
    public BooleanObjectImmutablePair<List<BigItemStack>> tryConsumeItems(List<BigItemStack> items, int flags) {
        if ((flags & CONSUME_IGNORE_CREATIVE) == 0 && player.capabilities.isCreativeMode) {
            return BooleanObjectImmutablePair.of(true, items);
        }

        visitedGrids.clear();

        List<BigItemStack> simulated = MMUtils.mapToList(items, BigItemStack::copy);
        List<BigItemStack> extracted = new ArrayList<>();

        dragonfix$consumeItemsFromAllSources(simulated, extracted, flags | CONSUME_SIMULATED);

        if ((flags & CONSUME_PARTIAL) == 0) {
            if (simulated.stream()
                .anyMatch(stack -> stack.getStackSize() > 0)) {
                return BooleanObjectImmutablePair.of(false, null);
            }
        }

        if ((flags & CONSUME_SIMULATED) != 0) return BooleanObjectImmutablePair.of(true, merge(extracted));

        visitedGrids.clear();

        simulated = MMUtils.mapToList(items, BigItemStack::copy);
        extracted.clear();

        dragonfix$consumeItemsFromAllSources(simulated, extracted, flags);

        return BooleanObjectImmutablePair.of(true, merge(extracted));
    }

    @Unique
    private void dragonfix$consumeItemsFromAllSources(List<BigItemStack> requestedItems, List<BigItemStack> extracted,
        int flags) {
        boolean simulate = (flags & CONSUME_SIMULATED) != 0;

        consumeItemsFromPending(requestedItems, extracted, flags);
        consumeItemsFromPlayer(requestedItems, extracted, flags);

        if (state.hasCap(ItemMatterManipulator.CONNECTS_TO_AE) && Mods.AppliedEnergistics2.isModLoaded()) {
            consumeItemsFromAE(requestedItems, extracted, flags);
        }

        if (state.hasCap(ItemMatterManipulator.CONNECTS_TO_UPLINK) && Mods.GregTech.isModLoaded()) {
            consumeItemsFromUplink(requestedItems, extracted, flags);
        }

        MatterManipulatorMicrocontrollerConsumptionHelper
            .consumeFromPending((MMInventory) (Object) this, requestedItems, extracted, simulate);
        MatterManipulatorMicrocontrollerConsumptionHelper
            .consumeFromPlayer((MMInventory) (Object) this, requestedItems, extracted, simulate);

        if (state.hasCap(ItemMatterManipulator.CONNECTS_TO_AE) && Mods.AppliedEnergistics2.isModLoaded()) {
            MatterManipulatorMicrocontrollerConsumptionHelper
                .consumeFromAE((MMInventory) (Object) this, requestedItems, extracted, simulate);
        }
    }
}
