package com.dragonfix.mattermanipulator.persistent.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PersistentSchematicClientRestore {

    private static final long STALE_RESTORE_MS = 30L * 1000L;
    private static final long SESSION_STARTED_MS = System.currentTimeMillis();

    private PersistentSchematicClientRestore() {}

    public static MMState getInitializedState(ItemStack stack) {
        MMState state = MatterManipulatorStateAccess.getState(stack);
        tryInitialize(stack, state);
        return state;
    }

    public static void tryInitialize(ItemStack stack, MMState state) {
        if (stack == null || state == null || !isHeldStack(stack)) return;

        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;

        String schematicFile = bridge.dragonfix$getPersistentSchematicFile();
        if (schematicFile == null || schematicFile.isEmpty()) return;

        UUID schematicId = bridge.dragonfix$getPersistentSchematicId();
        if (PersistentSchematicNetwork.isClientLoadedSchematic(schematicId)) {
            markRestoreReady(stack, state, bridge);
            return;
        }

        long now = System.currentTimeMillis();
        long restoreStartedMs = bridge.dragonfix$getPersistentPasteRestoreStartedMs();
        if (bridge.dragonfix$getPersistentPasteRestoreState() == PersistentSchematicConfigBridge.RESTORE_RESTORING
            && restoreStartedMs >= SESSION_STARTED_MS
            && now - restoreStartedMs < STALE_RESTORE_MS) {
            return;
        }

        bridge.dragonfix$setPersistentPasteRestoreState(PersistentSchematicConfigBridge.RESTORE_RESTORING);
        bridge.dragonfix$setPersistentPasteRestoreStartedMs(now);
        MatterManipulatorStateAccess.setState(stack, state);

        if (!PersistentSchematicNetwork.restoreClientLoadedSchematic(
            schematicFile,
            schematicId,
            restoredId -> completePersistentPasteRestore(stack, schematicFile, schematicId, restoredId),
            () -> resetFailedPersistentPasteRestore(stack, schematicFile, schematicId))) {
            resetFailedPersistentPasteRestore(stack, schematicFile, schematicId);
        }
    }

    private static boolean isHeldStack(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.theWorld != null && minecraft.thePlayer != null && minecraft.thePlayer.getHeldItem() == stack;
    }

    private static void markRestoreReady(ItemStack stack, MMState state, PersistentSchematicConfigBridge bridge) {
        if (bridge.dragonfix$getPersistentPasteRestoreState() == PersistentSchematicConfigBridge.RESTORE_READY
            && bridge.dragonfix$getPersistentPasteRestoreStartedMs() == 0L) {
            return;
        }

        bridge.dragonfix$setPersistentPasteRestoreState(PersistentSchematicConfigBridge.RESTORE_READY);
        bridge.dragonfix$setPersistentPasteRestoreStartedMs(0L);
        MatterManipulatorStateAccess.setState(stack, state);
    }

    private static void completePersistentPasteRestore(ItemStack stack, String schematicFile, UUID previousId,
        UUID restoredId) {
        if (!MatterManipulatorStateAccess.isMatterManipulator(stack)) return;

        MMState state = MatterManipulatorStateAccess.getState(stack);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;
        if (previousId != null && !previousId.equals(bridge.dragonfix$getPersistentSchematicId())) return;
        if (!matchesSchematicFile(schematicFile, bridge.dragonfix$getPersistentSchematicFile())) return;

        bridge.dragonfix$activatePersistentSchematic(PersistentSchematicMode.PASTE, schematicFile, restoredId);
        bridge.dragonfix$setPersistentPasteRestoreState(PersistentSchematicConfigBridge.RESTORE_READY);
        bridge.dragonfix$setPersistentPasteRestoreStartedMs(0L);
        MatterManipulatorStateAccess.setState(stack, state);
    }

    private static void resetFailedPersistentPasteRestore(ItemStack stack, String schematicFile, UUID schematicId) {
        if (!MatterManipulatorStateAccess.isMatterManipulator(stack)) return;

        MMState state = MatterManipulatorStateAccess.getState(stack);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;
        if (schematicId != null && !schematicId.equals(bridge.dragonfix$getPersistentSchematicId())) return;
        if (!matchesSchematicFile(schematicFile, bridge.dragonfix$getPersistentSchematicFile())) return;

        PersistentSchematicState.resetPasteSession(state);
        bridge.dragonfix$setPersistentPasteRestoreState(PersistentSchematicConfigBridge.RESTORE_NONE);
        bridge.dragonfix$setPersistentPasteRestoreStartedMs(0L);
        MatterManipulatorStateAccess.setState(stack, state);
        PersistentSchematicNetwork.sendResetPasteSessionToServer();
    }

    private static boolean matchesSchematicFile(String expected, String actual) {
        if (expected == null || expected.isEmpty()) return actual == null || actual.isEmpty();

        return PersistentSchematic.normalizeFileName(expected)
            .equals(actual);
    }
}
