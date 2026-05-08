package com.dragonfix.mattermanipulator.persistent;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PendingAction;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;

public final class PersistentSchematicState {

    private PersistentSchematicState() {}

    public static void enterMode(MMState state, @Nullable World world, PersistentSchematicMode mode,
        @Nullable String fileName, @Nullable UUID schematicId) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        PersistentSchematicMode previousMode = bridge.dragonfix$getPersistentSchematicMode();

        boolean activatePersistentSelection = previousMode != mode;

        if (previousMode == PersistentSchematicMode.NONE && mode != PersistentSchematicMode.NONE) {
            bridge.dragonfix$captureNormalSelection();

            if (mode == PersistentSchematicMode.COPY) {
                bridge.dragonfix$syncPersistentCopyFromNormalSelection();
            }
        } else if (previousMode != PersistentSchematicMode.NONE && previousMode != mode) {
            bridge.dragonfix$capturePersistentSelection(previousMode);
            bridge.dragonfix$capturePersistentSchematic(previousMode);
        } else if (previousMode == mode && mode != PersistentSchematicMode.NONE) {
            bridge.dragonfix$capturePersistentSelection(mode);
            bridge.dragonfix$capturePersistentSchematic(mode);
        }

        bridge.dragonfix$setPersistentSchematicMode(mode);
        bridge.dragonfix$activatePersistentSchematic(mode, fileName, schematicId);

        if (mode == PersistentSchematicMode.NONE) {
            bridge.dragonfix$activateNormalSelection(false);
            return;
        }

        state.config.placeMode = PlaceMode.COPYING;
        state.config.action = null;
        if (activatePersistentSelection) {
            bridge.dragonfix$activatePersistentSelection(mode);
        }

        if (mode == PersistentSchematicMode.COPY && !hasCopySelection(state, world)) {
            state.config.action = PendingAction.MARK_COPY_A;
            state.config.coordA = null;
            state.config.coordB = null;
            state.config.arraySpan = null;
        } else if (mode == PersistentSchematicMode.PASTE && !hasPasteSelection(state, world)) {
            state.config.action = PendingAction.MARK_PASTE;
            state.config.coordC = null;
            state.config.arraySpan = null;
        }
    }

    public static boolean hasCopySelection(MMState state, @Nullable World world) {
        if (!Location.areCompatible(state.config.coordA, state.config.coordB)) return false;
        return world == null || state.config.coordA.isInWorld(world) && state.config.coordB.isInWorld(world);
    }

    public static boolean hasPasteSelection(MMState state, @Nullable World world) {
        return state.config.coordC != null && (world == null || state.config.coordC.isInWorld(world));
    }

    public static boolean resetPasteSessionStack(ItemStack stack) {
        if (!MatterManipulatorStateAccess.isMatterManipulator(stack)) return false;

        MMState state = MatterManipulatorStateAccess.getState(stack);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        boolean changed = bridge.dragonfix$isPersistentSchematicPaste() ? resetPasteSession(state)
            : clearStoredPasteSession(state);

        if (!changed) return false;
        MatterManipulatorStateAccess.setState(stack, state);
        return true;
    }

    public static boolean clearStoredPasteSession(MMState state) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        bridge.dragonfix$clearStoredPersistentPasteSession();
        return true;
    }

    public static boolean refreshPasteSchematicStack(ItemStack stack, String fileName, UUID schematicId) {
        if (!MatterManipulatorStateAccess.isMatterManipulator(stack)) return false;

        MMState state = MatterManipulatorStateAccess.getState(stack);
        if (!refreshPasteSchematic(state, fileName, schematicId)) return false;
        MatterManipulatorStateAccess.setState(stack, state);
        return true;
    }

    public static boolean refreshHeldPasteSchematic(EntityPlayer player, String fileName, UUID schematicId) {
        return player != null && refreshPasteSchematicStack(player.getHeldItem(), fileName, schematicId);
    }

    public static boolean refreshPasteSchematic(MMState state, String fileName, UUID schematicId) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        return bridge.dragonfix$refreshPersistentPasteSchematic(fileName, schematicId);
    }

    public static boolean resetPasteSession(MMState state) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return false;

        boolean changed = !bridge.dragonfix$getPersistentSchematicFile()
            .isEmpty() || bridge.dragonfix$getPersistentSchematicId() != null
            || state.config.coordC != null
            || state.config.arraySpan != null
            || state.config.action != PendingAction.MARK_PASTE;

        bridge.dragonfix$setPersistentSchematicMode(PersistentSchematicMode.PASTE);
        bridge.dragonfix$resetPersistentPasteSelection();
        bridge.dragonfix$resetPersistentPasteSchematic();
        state.config.placeMode = PlaceMode.COPYING;
        state.config.action = PendingAction.MARK_PASTE;

        return changed;
    }

    public static boolean leaveMode(MMState state, boolean syncPersistentCopy) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        PersistentSchematicMode previousMode = bridge.dragonfix$getPersistentSchematicMode();

        if (previousMode == PersistentSchematicMode.NONE) return false;

        bridge.dragonfix$capturePersistentSelection(previousMode);
        bridge.dragonfix$capturePersistentSchematic(previousMode);
        bridge.dragonfix$activateNormalSelection(syncPersistentCopy && previousMode == PersistentSchematicMode.COPY);
        bridge.dragonfix$setPersistentSchematicMode(PersistentSchematicMode.NONE);
        bridge.dragonfix$setPersistentSchematicFile("");
        bridge.dragonfix$setPersistentSchematicId(null);
        state.config.action = null;
        return true;
    }

}
