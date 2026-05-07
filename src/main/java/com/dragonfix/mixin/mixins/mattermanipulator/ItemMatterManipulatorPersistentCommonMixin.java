package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

@Mixin(value = ItemMatterManipulator.class, remap = false)
public abstract class ItemMatterManipulatorPersistentCommonMixin {

    @Inject(method = "onUsingTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$stopMissingPersistentSchematicBuild(ItemStack stack, EntityPlayer player, int count,
        CallbackInfo ci) {
        if (player.worldObj.isRemote || Integer.MAX_VALUE - count != 1) return;

        MMState state = ItemMatterManipulator.getState(stack);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;

        if (bridge.dragonfix$getPersistentSchematicId() != null && PersistentSchematicNetwork.getUploadedSchematic(
            bridge.dragonfix$getPersistentSchematicId(),
            PersistentSchematicNetwork.playerId(player)) != null) {
            return;
        }

        PersistentSchematicState.resetPasteSession(state);
        ItemMatterManipulator.setState(stack, state);
        player.stopUsingItem();
        MMUtils
            .sendErrorToPlayer(player, "Persistent Matter Manipulator schematic is no longer uploaded; load it again.");
        ci.cancel();
    }

    /**
     * @author DragonFix
     * @reason Persistent paste arrays use schematic dimensions instead of normal copy A/B dimensions.
     */
    @Overwrite(remap = false)
    private void onMarkArray(World world, EntityPlayer player, ItemStack stack, MMState state) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        Vector3i lookingAt = MMUtils.getLookingAtLocation(player);

        if (!bridge.dragonfix$isPersistentSchematicPaste()) {
            if (!Location.areCompatible(state.config.coordA, state.config.coordB)) {
                MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.cannot_mark_copy"));
                state.config.arraySpan = null;
                return;
            }

            if (state.config.coordC == null || !state.config.coordC.isInWorld(world)) {
                MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.cannot_mark_paste"));
                state.config.arraySpan = null;
                return;
            }

            state.config.arraySpan = state.config
                .getArrayMult(world, state.config.coordA, state.config.coordB, state.config.coordC, lookingAt);
            return;
        }

        if (state.config.coordC == null || !state.config.coordC.isInWorld(world)) {
            MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.cannot_mark_paste"));
            state.config.arraySpan = null;
            return;
        }

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork.getAvailableSchematic(
                bridge.dragonfix$getPersistentSchematicId(),
                bridge.dragonfix$getPersistentSchematicFile(),
                world);
            if (schematic == null) throw new IllegalStateException("No persistent schematic has been uploaded");
            state.config.arraySpan = schematic
                .getArrayMult(world, state.config.coordC, lookingAt, state.config.transform);
        } catch (Exception e) {
            MMUtils.sendErrorToPlayer(player, "Could not load Matter Manipulator schematic: " + e.getMessage());
            state.config.arraySpan = null;
        }
    }
}
