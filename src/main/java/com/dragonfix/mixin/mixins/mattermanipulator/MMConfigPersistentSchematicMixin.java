package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.World;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

@Mixin(value = MMConfig.class, remap = false)
public abstract class MMConfigPersistentSchematicMixin implements PersistentSchematicConfigBridge {

    @Shadow(remap = false)
    public Location coordC;

    @Shadow(remap = false)
    @Nullable
    public Transform transform;

    @Shadow(remap = false)
    public Vector3i arraySpan;

    @Unique
    @SerializedName("dragonfixPersistentMode")
    private PersistentSchematicMode dragonfix$persistentSchematicMode = PersistentSchematicMode.NONE;

    @Unique
    @SerializedName("dragonfixSchematicFile")
    private String dragonfix$persistentSchematicFile = "";

    @Unique
    @SerializedName("dragonfixSchematicId")
    private UUID dragonfix$persistentSchematicId;

    @Inject(method = "getPasteVisualDeltas", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$getPersistentPasteVisualDeltas(World world, boolean doTransform,
        CallbackInfoReturnable<MMConfig.VoxelAABB> cir) {
        if (dragonfix$persistentSchematicMode != PersistentSchematicMode.PASTE) return;
        if (world == null) {
            cir.setReturnValue(null);
            return;
        }

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork
                .getAvailableSchematic(dragonfix$persistentSchematicId, dragonfix$persistentSchematicFile, world);
            if (schematic == null) {
                cir.setReturnValue(null);
                return;
            }
            cir.setReturnValue(
                schematic.getPasteVisualDeltas(
                    world.provider.dimensionId,
                    coordC,
                    doTransform ? transform : null,
                    arraySpan));
        } catch (Exception ignored) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includePersistentSchematicInHashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(
            31 * cir.getReturnValue() + Objects.hash(
                dragonfix$persistentSchematicMode,
                dragonfix$persistentSchematicFile,
                dragonfix$persistentSchematicId));
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includePersistentSchematicInEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        PersistentSchematicConfigBridge other = (PersistentSchematicConfigBridge) obj;
        cir.setReturnValue(
            dragonfix$persistentSchematicMode == other.dragonfix$getPersistentSchematicMode()
                && Objects.equals(dragonfix$persistentSchematicFile, other.dragonfix$getPersistentSchematicFile())
                && Objects.equals(dragonfix$persistentSchematicId, other.dragonfix$getPersistentSchematicId()));
    }

    @Override
    public PersistentSchematicMode dragonfix$getPersistentSchematicMode() {
        return dragonfix$persistentSchematicMode == null ? PersistentSchematicMode.NONE
            : dragonfix$persistentSchematicMode;
    }

    @Override
    public void dragonfix$setPersistentSchematicMode(PersistentSchematicMode mode) {
        dragonfix$persistentSchematicMode = mode == null ? PersistentSchematicMode.NONE : mode;
    }

    @Override
    public String dragonfix$getPersistentSchematicFile() {
        return dragonfix$persistentSchematicFile;
    }

    @Override
    public void dragonfix$setPersistentSchematicFile(String fileName) {
        dragonfix$persistentSchematicFile = fileName == null ? "" : fileName;
    }

    @Override
    public UUID dragonfix$getPersistentSchematicId() {
        return dragonfix$persistentSchematicId;
    }

    @Override
    public void dragonfix$setPersistentSchematicId(UUID id) {
        dragonfix$persistentSchematicId = id;
    }
}
