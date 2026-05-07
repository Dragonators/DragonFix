package com.dragonfix.mixin.mixins.opencomputers;

import net.minecraft.inventory.IInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.dragonfix.mattermanipulator.bridge.OpenComputersMicrocontrollerBridge;

import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.inventory.ComponentInventory;
import scala.Option;
import scala.collection.mutable.ArrayBuffer;

@Mixin(value = ComponentInventory.class, remap = false)
public interface ComponentInventoryMixin extends OpenComputersMicrocontrollerBridge, IInventory {

    @Shadow(remap = false)
    Option<ManagedEnvironment>[] components();

    @Shadow(remap = false)
    ArrayBuffer<ManagedEnvironment> updatingComponents();

    @Override
    default boolean dragonfix$resetComponentEnvironments() {
        Option<ManagedEnvironment>[] components = components();
        if (components.length != getSizeInventory()) return false;

        for (int i = 0; i < components.length; i++) {
            components[i] = Option.empty();
        }
        updatingComponents().clear();
        return true;
    }
}
