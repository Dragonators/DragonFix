package com.dragonfix.mattermanipulator.helper;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.matter_manipulator.common.building.MMInventory;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;

import appeng.api.config.Actionable;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;

public final class MatterManipulatorMicrocontrollerConsumptionHelper {

    private MatterManipulatorMicrocontrollerConsumptionHelper() {}

    public static void consumeFromPending(MMInventory inventory, List<BigItemStack> requestedItems,
        List<BigItemStack> extractedItems, boolean simulate) {
        for (BigItemStack request : requestedItems) {
            if (request.getStackSize() == 0) continue;
            if (!OpenComputersMicrocontrollerItemHelper.isMicrocontroller(request.getItemStack())) continue;

            var iterator = inventory.pendingItems.object2LongEntrySet()
                .iterator();

            while (iterator.hasNext() && request.getStackSize() > 0) {
                var entry = iterator.next();
                long stored = entry.getLongValue();
                if (stored == 0) continue;

                ItemStack candidate = entry.getKey()
                    .getItemStack();
                if (!OpenComputersMicrocontrollerItemHelper.matches(request.getItemStack(), candidate)) continue;

                long toRemove = Math.min(stored, request.getStackSize());
                recordEquivalentExtraction(request, extractedItems, toRemove);

                stored -= toRemove;
                request.decStackSize(toRemove);

                if (!simulate) {
                    if (stored == 0) {
                        iterator.remove();
                    } else {
                        entry.setValue(stored);
                    }
                }
            }
        }
    }

    public static void consumeFromPlayer(MMInventory inventory, List<BigItemStack> requestedItems,
        List<BigItemStack> extractedItems, boolean simulate) {
        ItemStack[] playerInventory = inventory.player.inventory.mainInventory;

        for (BigItemStack request : requestedItems) {
            if (request.getStackSize() == 0) continue;
            if (!OpenComputersMicrocontrollerItemHelper.isMicrocontroller(request.getItemStack())) continue;

            for (int i = 0; i < playerInventory.length && request.getStackSize() > 0; i++) {
                ItemStack candidate = playerInventory[i];
                if (candidate == null || candidate.getItem() == null || candidate.stackSize == 0) continue;
                if (!OpenComputersMicrocontrollerItemHelper.matches(request.getItemStack(), candidate)) continue;

                if (candidate.stackSize == 111) {
                    recordEquivalentExtraction(request, extractedItems, request.getStackSize());
                    request.setStackSize(0);
                    continue;
                }

                long toRemove = Math.min(candidate.stackSize, request.getStackSize());
                recordEquivalentExtraction(request, extractedItems, toRemove);
                request.decStackSize(toRemove);

                if (!simulate) {
                    candidate.stackSize -= (int) toRemove;
                    if (candidate.stackSize == 0) {
                        playerInventory[i] = null;
                    }
                    inventory.player.inventory.markDirty();
                }
            }
        }
    }

    public static void consumeFromAE(MMInventory inventory, List<BigItemStack> requestedItems,
        List<BigItemStack> extractedItems, boolean simulate) {
        if (inventory.state.encKey == null) return;
        if (!inventory.state.hasMEConnection() && !inventory.state.connectToMESystem()) return;
        if (!inventory.state.canInteractWithAE(inventory.player)) return;

        for (BigItemStack request : requestedItems) {
            if (request.getStackSize() == 0) continue;
            if (!OpenComputersMicrocontrollerItemHelper.isMicrocontroller(request.getItemStack())) continue;

            for (IAEItemStack stored : inventory.state.itemStorage.getStorageList()) {
                if (request.getStackSize() == 0) break;
                if (stored == null) continue;

                ItemStack candidate = stored.getItemStack();
                if (!OpenComputersMicrocontrollerItemHelper.matches(request.getItemStack(), candidate)) continue;

                IAEItemStack toExtract = stored.copy()
                    .setStackSize(request.getStackSize());
                IAEItemStack extracted = inventory.state.itemStorage.extractItems(
                    toExtract,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                    new PlayerSource(inventory.player, inventory.state.securityTerminal));

                if (extracted == null) continue;

                recordEquivalentExtraction(request, extractedItems, extracted.getStackSize());
                request.decStackSize(extracted.getStackSize());
            }
        }
    }

    private static void recordEquivalentExtraction(BigItemStack request, List<BigItemStack> extractedItems,
        long amount) {
        extractedItems.add(
            request.copy()
                .setStackSize(amount));
    }
}
