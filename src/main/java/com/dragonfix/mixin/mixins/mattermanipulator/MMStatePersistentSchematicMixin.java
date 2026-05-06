package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator.ManipulatorTier;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

@Mixin(value = MMState.class, remap = false)
public abstract class MMStatePersistentSchematicMixin {

    @Shadow(remap = false)
    public MMConfig config;

    @Shadow(remap = false)
    public abstract Transform getTransform();

    @Inject(method = "getPendingBlocks", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$getPersistentSchematicBlocks(ManipulatorTier tier, World world,
        CallbackInfoReturnable<List<PendingBlock>> cir) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) config;

        if (bridge.dragonfix$isPersistentSchematicCopy()) {
            cir.setReturnValue(new ArrayList<>());
            return;
        }

        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;

        if (world == null || config.coordC == null || !config.coordC.isInWorld(world)) {
            cir.setReturnValue(new ArrayList<>());
            return;
        }

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork.getAvailableSchematic(
                bridge.dragonfix$getPersistentSchematicId(),
                bridge.dragonfix$getPersistentSchematicFile(),
                world);
            if (schematic == null) {
                cir.setReturnValue(new ArrayList<>());
                return;
            }
            cir.setReturnValue(
                schematic.getPendingBlocks(
                    world.provider.dimensionId,
                    config.coordC.toVec(),
                    getTransform(),
                    config.arraySpan));
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not load persistent Matter Manipulator schematic", e);
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
