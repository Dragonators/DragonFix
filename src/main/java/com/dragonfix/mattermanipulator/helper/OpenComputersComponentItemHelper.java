package com.dragonfix.mattermanipulator.helper;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;

public final class OpenComputersComponentItemHelper {

    private static final IntOpenHashSet FUZZY_COMPONENT_DAMAGE = new IntOpenHashSet();

    static {
        for (String name : new String[] { "cpu1", "cpu2", "cpu3", "apu1", "apu2", "apuCreative", "dataCard1",
            "dataCard2", "dataCard3", "internetCard", "lanCard", "wlanCard1", "wlanCard2", "linkedCard",
            "redstoneCard1", "redstoneCard2", "tpsCard", "debugCard", "graphicsCard1", "graphicsCard2", "graphicsCard3",
            "ram1", "ram2", "ram3", "ram4", "ram5", "ram6", }) {
            addFuzzyComponent(name);
        }
    }

    private static final ItemStack EEPROM = API.items.get("eeprom")
        .createItemStack(1);
    private static final ItemStack HDD_1 = API.items.get("hdd1")
        .createItemStack(1);
    private static final ItemStack HDD_2 = API.items.get("hdd2")
        .createItemStack(1);
    private static final ItemStack HDD_3 = API.items.get("hdd3")
        .createItemStack(1);
    private static final ItemStack FLOPPY = API.items.get("floppy")
        .createItemStack(1);

    private OpenComputersComponentItemHelper() {}

    public static boolean isHandledComponent(ItemStack stack) {
        return stack != null && API.items.get(stack) != null && isHandledComponentDamage(stack.getItemDamage());
    }

    public static boolean isFuzzyComponent(ItemStack stack) {
        return stack != null && FUZZY_COMPONENT_DAMAGE.contains(stack.getItemDamage());
    }

    public static boolean isEEPROM(ItemStack stack) {
        return stack != null && stack.getItemDamage() == EEPROM.getItemDamage();
    }

    public static boolean isHdd1(ItemStack stack) {
        return stack != null && stack.getItemDamage() == HDD_1.getItemDamage();
    }

    public static boolean isHdd2(ItemStack stack) {
        return stack != null && stack.getItemDamage() == HDD_2.getItemDamage();
    }

    public static boolean isHdd3(ItemStack stack) {
        return stack != null && stack.getItemDamage() == HDD_3.getItemDamage();
    }

    public static boolean isFloppy(ItemStack stack) {
        return stack != null && stack.getItemDamage() == FLOPPY.getItemDamage();
    }

    public static ItemStack defaultStack(ItemStack stack) {
        if (isEEPROM(stack)) return EEPROM;
        if (isHdd1(stack)) return HDD_1;
        if (isHdd2(stack)) return HDD_2;
        if (isHdd3(stack)) return HDD_3;
        if (isFloppy(stack)) return FLOPPY;
        return null;
    }

    public static boolean areEquivalentForRestore(ItemStack expected, ItemStack candidate) {
        if (expected == null || candidate == null) return expected == candidate;

        boolean expectedHandled = isHandledComponent(expected);
        boolean candidateHandled = isHandledComponent(candidate);

        if (expectedHandled || candidateHandled) {
            return expectedHandled && candidateHandled
                && Objects.equals(API.items.get(expected), API.items.get(candidate));
        }

        return ItemStack.areItemStacksEqual(expected, candidate);
    }

    public static ItemStack withoutAddress(ItemStack source) {
        ItemStack stripped = source.copy();

        if (!isEEPROM(stripped)) {
            stripped.setTagCompound(null);
            return stripped;
        }

        if (!stripped.hasTagCompound()) return stripped;
        NBTTagCompound tag = stripped.getTagCompound();

        if (!tag.hasKey("oc:data")) return stripped;
        NBTTagCompound data = tag.getCompoundTag("oc:data");

        if (data.hasKey("node")) {
            data.getCompoundTag("node")
                .removeTag("address");
        }

        return stripped;
    }

    public static int hashForRestore(ItemStack stack) {
        return isEEPROM(stack) ? Objects.hashCode(stack)
            : API.items.get(stack)
                .hashCode();
    }

    private static void addFuzzyComponent(String name) {
        ItemInfo item = API.items.get(name);
        if (item != null) {
            FUZZY_COMPONENT_DAMAGE.add(
                item.createItemStack(1)
                    .getItemDamage());
        }
    }

    private static boolean isHandledComponentDamage(int damage) {
        return FUZZY_COMPONENT_DAMAGE.contains(damage) || damage == EEPROM.getItemDamage()
            || damage == HDD_1.getItemDamage()
            || damage == HDD_2.getItemDamage()
            || damage == HDD_3.getItemDamage()
            || damage == FLOPPY.getItemDamage();
    }
}
