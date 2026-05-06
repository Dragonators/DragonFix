package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.entity.player.EntityPlayer;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

@Mixin(value = MMUtils.class, remap = false)
public abstract class MMUtilsPersistentSchematicMixin {

    @Inject(
        method = "createPlanImpl",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;areCompatible(Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;)Z",
            ordinal = 0),
        remap = false)
    private static void dragonfix$providePersistentSchematicPlanRegion(EntityPlayer player, MMState state,
        ItemMatterManipulator manipulator, int flags, CallbackInfo ci) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;
        if (state.config.coordC == null || !state.config.coordC.isInWorld(player.worldObj)) return;

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork.getAvailableSchematic(
                bridge.dragonfix$getPersistentSchematicId(),
                bridge.dragonfix$getPersistentSchematicFile(),
                player.worldObj);
            if (schematic == null) return;
            Vector3i paste = state.config.coordC.toVec();

            state.config.coordA = state.config.coordC.clone();
            state.config.coordB = new Location(player.worldObj, new Vector3i(paste).add(schematic.deltas));
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not prepare persistent schematic planning region", e);
        }
    }
}
