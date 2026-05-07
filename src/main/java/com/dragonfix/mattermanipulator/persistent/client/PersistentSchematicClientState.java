package com.dragonfix.mattermanipulator.persistent.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PersistentSchematicClientState {

    private PersistentSchematicClientState() {}

    public static void setMode(PersistentSchematicMode mode, String fileName) {
        updateState((state, player) -> {
            if (mode == PersistentSchematicMode.NONE) {
                PersistentSchematicState.leaveMode(state, false);
            } else {
                PersistentSchematicState.enterMode(state, player.worldObj, mode, fileName, null);
            }
        });
    }

    public static void leaveMode(boolean syncPersistentCopy) {
        updateState((state, player) -> PersistentSchematicState.leaveMode(state, syncPersistentCopy));
    }

    private static void updateState(StateUpdate update) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        ItemStack held = player.getHeldItem();
        if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return;

        MMState state = MatterManipulatorStateAccess.getState(held);
        update.apply(state, player);
        MatterManipulatorStateAccess.setState(held, state);
    }

    private interface StateUpdate {

        void apply(MMState state, EntityPlayer player);
    }
}
