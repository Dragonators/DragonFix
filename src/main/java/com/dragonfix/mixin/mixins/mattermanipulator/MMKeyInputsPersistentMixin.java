package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.client.PersistentSchematicClientRestore;
import com.recursive_pineapple.matter_manipulator.GlobalMMConfig.InteractionConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMKeyInputs;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;
import com.recursive_pineapple.matter_manipulator.common.networking.Messages;

import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;

@Mixin(value = MMKeyInputs.class, remap = false)
public abstract class MMKeyInputsPersistentMixin {

    /**
     * @author DragonFix
     * @reason Persistent schematic modes reuse copy/paste keys but must not pass through normal MM mode switching.
     */
    @Overwrite(remap = false)
    public static void onKeyPressed(KeyInputEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemMatterManipulator)) return;

        MMState state = PersistentSchematicClientRestore.getInitializedState(held);

        if (MMKeyInputs.CONTROL.getKeyCode() != 0 && !MMKeyInputs.CONTROL.getIsKeyPressed()) return;

        boolean copyPressed = MMKeyInputs.COPY.isPressed();
        boolean pastePressed = MMKeyInputs.PASTE.isPressed();
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        PersistentSchematicMode mode = bridge.dragonfix$getPersistentSchematicMode();

        if (mode == PersistentSchematicMode.COPY) {
            if (copyPressed) {
                Messages.MarkCopy.sendToServer();
                return;
            }
            if (pastePressed) return;
        } else if (mode == PersistentSchematicMode.PASTE) {
            if (pastePressed) {
                Messages.MarkPaste.sendToServer();
                return;
            }
            if (copyPressed) return;
        }

        if (MMKeyInputs.CUT.isPressed()) {
            if (state.config.placeMode != PlaceMode.MOVING) {
                Messages.SetPlaceMode.sendToServer(PlaceMode.MOVING);
            }

            if (InteractionConfig.pasteAutoClear) {
                Messages.ClearCoords.sendToServer();

                if (InteractionConfig.resetTransform) {
                    Messages.ClearTransform.sendToServer();
                    Messages.ResetArray.sendToServer();
                }
            }

            Messages.MarkCut.sendToServer();
            return;
        }

        if (copyPressed) {
            if (state.config.placeMode != PlaceMode.COPYING) {
                Messages.SetPlaceMode.sendToServer(PlaceMode.COPYING);
            }

            if (InteractionConfig.pasteAutoClear) {
                Messages.ClearCoords.sendToServer();

                if (InteractionConfig.resetTransform) {
                    Messages.ClearTransform.sendToServer();
                    Messages.ResetArray.sendToServer();
                }
            }

            Messages.MarkCopy.sendToServer();
            return;
        }

        if (pastePressed) {
            if (state.config.placeMode != PlaceMode.COPYING && state.config.placeMode != PlaceMode.MOVING) {
                Messages.SetPlaceMode.sendToServer(PlaceMode.COPYING);
            }

            Messages.MarkPaste.sendToServer();
            return;
        }

        if (MMKeyInputs.RESET.isPressed()) {
            Messages.ClearCoords.sendToServer();

            if (InteractionConfig.resetTransform) {
                Messages.ClearTransform.sendToServer();
                Messages.ResetArray.sendToServer();
            }
        }
    }
}
