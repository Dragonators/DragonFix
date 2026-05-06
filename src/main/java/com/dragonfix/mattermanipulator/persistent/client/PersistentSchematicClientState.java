package com.dragonfix.mattermanipulator.persistent.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PersistentSchematicClientState {

    private PersistentSchematicClientState() {}

    public static void setMode(PersistentSchematicMode mode, String fileName) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        ItemStack held = player.getHeldItem();
        if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return;

        MMState state = MatterManipulatorStateAccess.getState(held);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;

        bridge.dragonfix$setPersistentSchematicMode(mode);
        bridge.dragonfix$setPersistentSchematicId("");

        if (fileName != null) {
            bridge.dragonfix$setPersistentSchematicFile(PersistentSchematic.normalizeFileName(fileName));
        }

        if (mode != PersistentSchematicMode.NONE) {
            state.config.placeMode = PlaceMode.COPYING;
        }

        MatterManipulatorStateAccess.setState(held, state);
    }
}
