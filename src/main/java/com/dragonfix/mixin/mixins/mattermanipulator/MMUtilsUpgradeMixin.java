package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.IPseudoInventory;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;
import com.recursive_pineapple.matter_manipulator.common.utils.ItemId;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import appeng.api.implementations.items.IUpgradeModule;
import appeng.parts.automation.UpgradeInventory;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

@Mixin(value = MMUtils.class, remap = false)
public abstract class MMUtilsUpgradeMixin {

    /**
     * @author DragonFix
     * @reason Avoid mutating the consumed upgrade list into zero-sized stacks before installation.
     */
    @Overwrite(remap = false)
    public static boolean installUpgrades(IPseudoInventory src, UpgradeInventory dest, PortableItemStack[] pupgrades,
        boolean consume, boolean simulate) {
        boolean success = true;

        List<ItemStack> stacks = MMUtils.mapToList(pupgrades, PortableItemStack::toStack);

        stacks.removeIf(i -> i == null || !(i.getItem() instanceof IUpgradeModule));

        for (ItemStack stack : stacks) {
            stack.stackSize = Math
                .min(stack.stackSize, dest.getMaxInstalled(((IUpgradeModule) stack.getItem()).getType(stack)));
        }

        Object2LongOpenHashMap<ItemId> actual = MMUtils
            .getItemStackHistogram(Arrays.asList(MMUtils.inventoryToArray(dest)));
        Object2LongOpenHashMap<ItemId> target = MMUtils.getItemStackHistogram(stacks);

        MMUtils.StackMapDiff diff = MMUtils.getStackMapDiff(actual, target);

        if (diff.removed.isEmpty() && diff.added.isEmpty()) return success;

        List<ItemStack> toInstall = MMUtils.getStacksOfSize(diff.added, dest.getInventoryStackLimit());

        long installable = dest.getSizeInventory() - actual.values()
            .longStream()
            .sum()
            + diff.removed.values()
                .longStream()
                .sum();

        List<BigItemStack> toInstallBig = toInstall.subList(0, Math.min(toInstall.size(), (int) installable))
            .stream()
            .map(BigItemStack::create)
            .collect(Collectors.toList());

        List<BigItemStack> extracted;

        if (consume) {
            List<BigItemStack> request = dragonfix$copyBigStacks(toInstallBig);
            var result = src.tryConsumeItems(request, IPseudoInventory.CONSUME_PARTIAL);

            extracted = dragonfix$normalizeExtracted(toInstallBig, request, result.right());

            if (src instanceof IBlockApplyContext ctx) {
                for (BigItemStack wanted : dragonfix$getMissingStacks(toInstallBig, extracted)) {
                    ctx.warn(
                        StatCollector.translateToLocalFormatted(
                            "dragonfix.mm.warning.missing_upgrade",
                            wanted.getItemStack()
                                .getDisplayName(),
                            wanted.stackSize));
                    success = false;
                }
            }
        } else {
            extracted = dragonfix$copyBigStacks(toInstallBig);
        }

        if (!simulate) {
            for (var e : diff.removed.object2LongEntrySet()) {
                long amount = e.getLongValue();

                for (int slot = 0; slot < dest.getSizeInventory(); slot++) {
                    if (amount <= 0) break;

                    ItemStack inSlot = dest.getStackInSlot(slot);

                    if (e.getKey()
                        .isSameAs(inSlot)) {
                        src.givePlayerItems(inSlot);
                        dest.setInventorySlotContents(slot, null);

                        amount--;
                    }
                }
            }

            int slot = 0;

            outer: for (BigItemStack stack : extracted) {
                for (ItemStack split : stack.copy()
                    .toStacks(1)) {
                    while (dest.getStackInSlot(slot) != null) {
                        slot++;

                        if (slot >= dest.getSizeInventory()) {
                            if (src instanceof IBlockApplyContext ctx) {
                                ctx.error(StatCollector.translateToLocal("dragonfix.mm.error.too_many_upgrades"));
                            }
                            break outer;
                        }
                    }

                    dest.setInventorySlotContents(slot++, split);
                }
            }

            dest.markDirty();
        }

        return success;
    }

    @Unique
    private static List<BigItemStack> dragonfix$copyBigStacks(List<BigItemStack> stacks) {
        List<BigItemStack> out = new ArrayList<>(stacks.size());
        for (BigItemStack stack : stacks) {
            out.add(stack.copy());
        }
        return out;
    }

    @Unique
    private static List<BigItemStack> dragonfix$normalizeExtracted(List<BigItemStack> requested,
        List<BigItemStack> request, List<BigItemStack> returned) {
        if (returned == null) return new ArrayList<>();
        if (returned != request) return returned;

        List<BigItemStack> extracted = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            BigItemStack original = requested.get(i);
            BigItemStack remaining = request.get(i);
            long amount = original.stackSize - Math.max(remaining.stackSize, 0L);
            if (amount > 0) {
                extracted.add(
                    original.copy()
                        .setStackSize(amount));
            }
        }

        return extracted.isEmpty() ? dragonfix$copyBigStacks(requested) : extracted;
    }

    @Unique
    private static List<BigItemStack> dragonfix$getMissingStacks(List<BigItemStack> requested,
        List<BigItemStack> extracted) {
        List<BigItemStack> missing = dragonfix$copyBigStacks(requested);
        for (BigItemStack found : extracted) {
            for (BigItemStack wanted : missing) {
                if (!found.isSameType(wanted)) continue;
                wanted.stackSize -= found.stackSize;
            }
        }

        missing.removeIf(stack -> stack.stackSize <= 0);
        return missing;
    }
}
