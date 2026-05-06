package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMKeyInputs;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.Messages;

import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;

@Mixin(value = MMKeyInputs.class, remap = false)
public abstract class MMKeyInputsPersistentMixin {

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$handlePersistentCopyPasteKeys(KeyInputEvent event, CallbackInfo ci) {
        PersistentSchematicMode mode = dragonfix$getPersistentSchematicMode();
        if (mode == PersistentSchematicMode.NONE) return;

        if (MMKeyInputs.CONTROL.getKeyCode() != 0 && !MMKeyInputs.CONTROL.getIsKeyPressed()) return;

        boolean copyPressed = MMKeyInputs.COPY.isPressed();
        boolean pastePressed = MMKeyInputs.PASTE.isPressed();

        if (mode == PersistentSchematicMode.COPY) {
            if (copyPressed) {
                Messages.MarkCopy.sendToServer();
                ci.cancel();
            } else if (pastePressed) {
                ci.cancel();
            }
        } else if (mode == PersistentSchematicMode.PASTE) {
            if (pastePressed) {
                Messages.MarkPaste.sendToServer();
                ci.cancel();
            } else if (copyPressed) {
                ci.cancel();
            }
        }
    }

    @Unique
    private static PersistentSchematicMode dragonfix$getPersistentSchematicMode() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return PersistentSchematicMode.NONE;

        ItemStack held = player.getHeldItem();
        if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return PersistentSchematicMode.NONE;

        MMState state = MatterManipulatorStateAccess.getState(held);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        return bridge.dragonfix$getPersistentSchematicMode();
    }
}
