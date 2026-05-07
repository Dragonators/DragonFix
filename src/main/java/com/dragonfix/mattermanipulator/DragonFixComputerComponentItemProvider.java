package com.dragonfix.mattermanipulator;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.recursive_pineapple.matter_manipulator.common.building.IPseudoInventory;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;

import it.unimi.dsi.fastutil.booleans.BooleanObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;

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

    private final ItemStack component;

    public DragonFixComputerComponentItemProvider(ItemStack component) {
        this.component = component;
    }

    public static DragonFixComputerComponentItemProvider fromStack(ItemStack stack) {
        if (stack == null || API.items.get(stack) == null || !isHandledComponent(stack)) return null;
        return new DragonFixComputerComponentItemProvider(withoutAddress(stack));
    }

    private static void addFuzzyComponent(String name) {
        ItemInfo item = API.items.get(name);
        if (item != null) {
            FUZZY_COMPONENT_DAMAGE.add(
                item.createItemStack(1)
                    .getItemDamage());
        }
    }

    private static boolean isHandledComponent(ItemStack stack) {
        int damage = stack.getItemDamage();
        return FUZZY_COMPONENT_DAMAGE.contains(damage) || damage == EEPROM.getItemDamage()
            || damage == HDD_1.getItemDamage()
            || damage == HDD_2.getItemDamage()
            || damage == HDD_3.getItemDamage()
            || damage == FLOPPY.getItemDamage();
    }

    @Override
    public ItemStack getStack(IPseudoInventory inv, boolean consume) {
        if (!consume) return component;

        if (FUZZY_COMPONENT_DAMAGE.contains(component.getItemDamage())) {
            BooleanObjectImmutablePair<List<BigItemStack>> result = inv.tryConsumeItems(
                Collections.singletonList(BigItemStack.create(component)),
                IPseudoInventory.CONSUME_FUZZY);
            return result.leftBoolean() ? API.items.get(component)
                .createItemStack(1) : null;
        }

        if (component.getItemDamage() == EEPROM.getItemDamage()) {
            BooleanObjectImmutablePair<List<BigItemStack>> result = inv.tryConsumeItems(
                Collections.singletonList(BigItemStack.create(component)),
                IPseudoInventory.CONSUME_REAL_ONLY);
            return result.leftBoolean() || inv.tryConsumeItems(EEPROM) ? component.copy() : null;
        }

        if (component.getItemDamage() == HDD_1.getItemDamage()) return inv.tryConsumeItems(HDD_1) ? HDD_1.copy() : null;
        if (component.getItemDamage() == HDD_2.getItemDamage()) return inv.tryConsumeItems(HDD_2) ? HDD_2.copy() : null;
        if (component.getItemDamage() == HDD_3.getItemDamage()) return inv.tryConsumeItems(HDD_3) ? HDD_3.copy() : null;
        if (component.getItemDamage() == FLOPPY.getItemDamage())
            return inv.tryConsumeItems(FLOPPY) ? FLOPPY.copy() : null;

        return null;
    }

    private static ItemStack withoutAddress(ItemStack source) {
        ItemStack stripped = source.copy();

        if (stripped.getItemDamage() != EEPROM.getItemDamage()) {
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

    @Override
    public IItemProvider clone() {
        return new DragonFixComputerComponentItemProvider(component);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DragonFixComputerComponentItemProvider provider)) return false;

        if (component.getItemDamage() != EEPROM.getItemDamage()) {
            return API.items.get(component)
                .equals(API.items.get(provider.component));
        }

        return ItemStack.areItemStacksEqual(component, provider.component);
    }

    @Override
    public int hashCode() {
        return component.getItemDamage() == EEPROM.getItemDamage() ? java.util.Objects.hashCode(component)
            : API.items.get(component)
                .hashCode();
    }
}
