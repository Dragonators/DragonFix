package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "createPlanImpl", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$validatePersistentSchematicPlan(EntityPlayer player, MMState state,
        ItemMatterManipulator manipulator, int flags, CallbackInfo ci) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return;

        if (state.config.coordC == null || !state.config.coordC.isInWorld(player.worldObj)) {
            MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.must_have_paste_region"));
            ci.cancel();
            return;
        }

        try {
            if (dragonfix$getPersistentSchematic(player, state) == null) {
                MMUtils.sendErrorToPlayer(
                    player,
                    "Persistent Matter Manipulator schematic is not uploaded; load it again.");
                ci.cancel();
            }
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not create persistent Matter Manipulator schematic plan", e);
            MMUtils.sendErrorToPlayer(player, "Could not load Matter Manipulator schematic: " + e.getMessage());
            ci.cancel();
        }
    }

    @Redirect(
        method = "createPlanImpl",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;areCompatible(Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/Location;)Z",
            ordinal = 0),
        remap = false,
        require = 1)
    private static boolean dragonfix$providePersistentSchematicPlanRegion(Location coordA, Location coordB,
        EntityPlayer player, MMState state, ItemMatterManipulator manipulator, int flags) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        if (!bridge.dragonfix$isPersistentSchematicPaste()) return Location.areCompatible(coordA, coordB);

        try {
            PersistentSchematic schematic = dragonfix$getPersistentSchematic(player, state);
            if (schematic == null || state.config.coordC == null) return false;

            Vector3i paste = state.config.coordC.toVec();
            state.config.coordA = state.config.coordC.clone();
            state.config.coordB = new Location(player.worldObj, new Vector3i(paste).add(schematic.deltas));
            return true;
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not prepare persistent schematic planning region", e);
            return false;
        }
    }

    @Unique
    private static PersistentSchematic dragonfix$getPersistentSchematic(EntityPlayer player, MMState state)
        throws java.io.IOException {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;
        return PersistentSchematicNetwork.getAvailableSchematic(
            bridge.dragonfix$getPersistentSchematicId(),
            bridge.dragonfix$getPersistentSchematicFile(),
            player.worldObj);
    }
}
