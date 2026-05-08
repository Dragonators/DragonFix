package com.dragonfix.mattermanipulator.helper;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.recursive_pineapple.matter_manipulator.common.building.IPseudoInventory;
import com.recursive_pineapple.matter_manipulator.common.building.MMInventory;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.utils.BigFluidStack;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;
import com.recursive_pineapple.matter_manipulator.common.utils.FluidId;
import com.recursive_pineapple.matter_manipulator.common.utils.Mods;

import appeng.api.config.Actionable;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;

public final class MatterManipulatorFluidSourceConsumer {

    private MatterManipulatorFluidSourceConsumer() {}

    public static boolean canBatchTogether(PendingBlock next, PendingBlock first) {
        if (MatterManipulatorFluidSourceHelper.isSupportedSource(first)) {
            return MatterManipulatorFluidSourceHelper.isSameSourceKind(next, first);
        }

        if (MatterManipulatorFluidSourceHelper.isSupportedSource(next)) return false;

        return next.spec.isEquivalent(first.spec);
    }

    public static boolean tryConsume(MMInventory inventory, FluidStack perBlock, long amount) {
        if (inventory.player.capabilities.isCreativeMode) return true;
        if (perBlock == null || perBlock.getFluid() == null || amount <= 0) return true;
        if (consume(inventory, perBlock.getFluid(), amount, true) > 0) return false;

        return consume(inventory, perBlock.getFluid(), amount, false) == 0;
    }

    public static void refund(MMInventory inventory, PendingBlock pending) {
        FluidStack fluid = MatterManipulatorFluidSourceHelper.getFluidStack(pending, 1000);
        if (fluid != null) inventory.givePlayerFluids(fluid);
    }

    private static long consume(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        long remaining = amount;

        remaining = consumePendingFluid(inventory, fluid, remaining, simulate);
        remaining = consumeAEFluid(inventory, fluid, remaining, simulate);
        remaining = consumeFluidDisplays(inventory, fluid, remaining, simulate);
        remaining = consumeBuckets(inventory, fluid, remaining, simulate);

        return remaining;
    }

    private static long consumePendingFluid(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0) return 0;

        FluidId id = FluidId.create(fluid);
        long stored = inventory.pendingFluids.getLong(id);
        long consumed = Math.min(stored, amount);

        if (consumed > 0 && !simulate) {
            long remainingStored = stored - consumed;
            if (remainingStored == 0) {
                inventory.pendingFluids.removeLong(id);
            } else {
                inventory.pendingFluids.put(id, remainingStored);
            }
        }

        return amount - consumed;
    }

    private static long consumeAEFluid(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0 || !inventory.state.hasCap(ItemMatterManipulator.CONNECTS_TO_AE)
            || !Mods.AppliedEnergistics2.isModLoaded()) {
            return amount;
        }

        if (inventory.state.encKey == null) return amount;
        if (!inventory.state.hasMEConnection() && !inventory.state.connectToMESystem()) return amount;
        if (!inventory.state.canInteractWithAE(inventory.player)) return amount;

        long extracted = 0;

        while (amount - extracted > 0) {
            int requestAmount = (int) Math.min(Integer.MAX_VALUE, amount - extracted);
            IAEFluidStack request = BigFluidStack.create(new FluidStack(fluid, requestAmount))
                .getAEFluidStack();
            IAEFluidStack result = inventory.state.storageGrid.getFluidInventory()
                .extractItems(
                    request,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                    new PlayerSource(inventory.player, inventory.state.securityTerminal));

            if (result == null || result.getStackSize() <= 0) break;

            extracted += result.getStackSize();
            if (result.getStackSize() < requestAmount) break;
        }

        return Math.max(0L, amount - extracted);
    }

    private static long consumeFluidDisplays(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        if (!Mods.GregTech.isModLoaded()) return amount;

        long remaining = consumeFluidDisplaysFromPlayer(inventory, fluid, amount, simulate);
        return consumeFluidDisplaysFromAE(inventory, fluid, remaining, simulate);
    }

    private static long consumeFluidDisplaysFromPlayer(MMInventory inventory, Fluid fluid, long amount,
        boolean simulate) {
        long remaining = amount;
        ItemStack[] playerInventory = inventory.player.inventory.mainInventory;

        for (int slot = 0; slot < playerInventory.length && remaining > 0; slot++) {
            ItemStack stack = playerInventory[slot];
            FluidStack display = MatterManipulatorFluidSourceHelper.getFluidFromDisplayStack(stack);
            if (!isMatchingDisplay(display, fluid)) continue;

            long perItem = display.amount;
            long stackFluid = perItem * stack.stackSize;
            if (simulate) {
                remaining -= Math.min(remaining, stackFluid);
                continue;
            }

            long fullItems = Math.min(stack.stackSize, remaining / perItem);
            remaining -= fullItems * perItem;
            stack.stackSize -= (int) fullItems;

            if (remaining > 0 && stack.stackSize > 0) {
                long refund = perItem - remaining;
                stack.stackSize--;
                remaining = 0;
                giveFluidDisplay(inventory, fluid, refund);
            }

            if (stack.stackSize <= 0) playerInventory[slot] = null;
            inventory.player.inventory.markDirty();
        }

        return Math.max(0L, remaining);
    }

    private static long consumeFluidDisplaysFromAE(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0 || !inventory.state.hasCap(ItemMatterManipulator.CONNECTS_TO_AE)
            || !Mods.AppliedEnergistics2.isModLoaded()) {
            return amount;
        }

        if (inventory.state.encKey == null) return amount;
        if (!inventory.state.hasMEConnection() && !inventory.state.connectToMESystem()) return amount;
        if (!inventory.state.canInteractWithAE(inventory.player)) return amount;

        long remaining = amount;

        for (IAEItemStack stored : inventory.state.itemStorage.getStorageList()) {
            if (remaining <= 0) break;
            if (stored == null || stored.getStackSize() <= 0) continue;

            ItemStack stack = stored.getItemStack();
            FluidStack display = MatterManipulatorFluidSourceHelper.getFluidFromDisplayStack(stack);
            if (!isMatchingDisplay(display, fluid)) continue;

            long perItem = display.amount;
            long neededItems = Math.min(stored.getStackSize(), (remaining + perItem - 1) / perItem);

            if (simulate) {
                remaining -= Math.min(remaining, neededItems * perItem);
                continue;
            }

            IAEItemStack request = stored.copy()
                .setStackSize(neededItems);
            IAEItemStack extracted = inventory.state.itemStorage.extractItems(
                request,
                Actionable.MODULATE,
                new PlayerSource(inventory.player, inventory.state.securityTerminal));
            if (extracted == null || extracted.getStackSize() <= 0) continue;

            long extractedFluid = extracted.getStackSize() * perItem;
            if (extractedFluid > remaining) {
                giveFluidDisplay(inventory, fluid, extractedFluid - remaining);
                remaining = 0;
            } else {
                remaining -= extractedFluid;
            }
        }

        return Math.max(0L, remaining);
    }

    private static boolean isMatchingDisplay(FluidStack display, Fluid fluid) {
        return display != null && display.getFluid() == fluid && display.amount > 0;
    }

    private static void giveFluidDisplay(MMInventory inventory, Fluid fluid, long amount) {
        while (amount > 0) {
            int stackAmount = (int) Math.min(Integer.MAX_VALUE, amount);
            ItemStack refund = MatterManipulatorFluidSourceHelper
                .getFluidDisplayStack(new FluidStack(fluid, stackAmount));
            if (refund == null) return;

            inventory.givePlayerItems(refund);
            amount -= stackAmount;
        }
    }

    private static long consumeBuckets(MMInventory inventory, Fluid fluid, long amount, boolean simulate) {
        if (amount <= 0) return 0;

        long bucketCount = (amount + 999L) / 1000L;
        if (bucketCount <= 0) return amount;

        ItemStack bucket = MatterManipulatorFluidSourceHelper.getBucket(fluid, 1);
        if (bucket == null) return amount;

        long consumed = 0;

        while (consumed < bucketCount) {
            long requestCount = Math.min(Integer.MAX_VALUE, bucketCount - consumed);
            ItemStack request = bucket.copy();
            request.stackSize = 1;

            List<BigItemStack> extractedStacks = inventory
                .tryConsumeItems(
                    Collections.singletonList(
                        BigItemStack.create(request)
                            .setStackSize(requestCount)),
                    IPseudoInventory.CONSUME_PARTIAL | (simulate ? IPseudoInventory.CONSUME_SIMULATED : 0))
                .right();

            long extracted = getExtractedCount(extractedStacks, request);
            if (extracted <= 0) break;

            consumed += extracted;

            if (!simulate) {
                giveEmptyBuckets(inventory, extracted);
            }

            if (extracted < requestCount) break;
        }

        long overdrawn = consumed * 1000L - amount;
        if (overdrawn > 0 && !simulate) {
            inventory.givePlayerFluids(new FluidStack(fluid, (int) overdrawn));
        }

        return Math.max(0L, amount - consumed * 1000L);
    }

    private static long getExtractedCount(List<BigItemStack> stacks, ItemStack request) {
        if (stacks == null) return 0;

        long count = 0;
        for (BigItemStack stack : stacks) {
            if (stack != null && stack.isSameType(request)) count += stack.stackSize;
        }
        return count;
    }

    private static void giveEmptyBuckets(MMInventory inventory, long amount) {
        while (amount > 0) {
            int stackSize = (int) Math.min(64L, amount);
            inventory.givePlayerItems(MatterManipulatorFluidSourceHelper.getEmptyBucket(stackSize));
            amount -= stackSize;
        }
    }
}
