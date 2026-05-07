package com.dragonfix.mattermanipulator.persistent;

import java.util.UUID;

import javax.annotation.Nullable;

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

        bridge.dragonfix$setPersistentSchematicMode(mode);
        bridge.dragonfix$setPersistentSchematicId(schematicId);

        if (fileName == null || fileName.isEmpty()) {
            bridge.dragonfix$setPersistentSchematicFile("");
        } else {
            bridge.dragonfix$setPersistentSchematicFile(PersistentSchematic.normalizeFileName(fileName));
        }

        if (mode == PersistentSchematicMode.NONE) return;

        state.config.placeMode = PlaceMode.COPYING;
        state.config.action = null;

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
        if (!resetPasteSession(state)) return false;

        MatterManipulatorStateAccess.setState(stack, state);
        return true;
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
        bridge.dragonfix$setPersistentSchematicFile("");
        bridge.dragonfix$setPersistentSchematicId(null);
        state.config.placeMode = PlaceMode.COPYING;
        state.config.action = PendingAction.MARK_PASTE;
        state.config.coordC = null;
        state.config.arraySpan = null;

        return changed;
    }

}
