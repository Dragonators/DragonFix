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
    public Location coordA;

    @Shadow(remap = false)
    public Location coordB;

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

    @Unique
    @SerializedName("dragonfixNormalA")
    private Location dragonfix$normalCoordA;

    @Unique
    @SerializedName("dragonfixNormalB")
    private Location dragonfix$normalCoordB;

    @Unique
    @SerializedName("dragonfixNormalC")
    private Location dragonfix$normalCoordC;

    @Unique
    @SerializedName("dragonfixNormalArray")
    private Vector3i dragonfix$normalArraySpan;

    @Unique
    @SerializedName("dragonfixPersistentCopyA")
    private Location dragonfix$persistentCopyA;

    @Unique
    @SerializedName("dragonfixPersistentCopyB")
    private Location dragonfix$persistentCopyB;

    @Unique
    @SerializedName("dragonfixPersistentPaste")
    private Location dragonfix$persistentPaste;

    @Unique
    @SerializedName("dragonfixPersistentArray")
    private Vector3i dragonfix$persistentArraySpan;

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

    @Override
    public void dragonfix$captureNormalSelection() {
        dragonfix$normalCoordA = dragonfix$copy(coordA);
        dragonfix$normalCoordB = dragonfix$copy(coordB);
        dragonfix$normalCoordC = dragonfix$copy(coordC);
        dragonfix$normalArraySpan = dragonfix$copy(arraySpan);
    }

    @Override
    public void dragonfix$capturePersistentSelection(PersistentSchematicMode mode) {
        if (mode == PersistentSchematicMode.COPY) {
            dragonfix$persistentCopyA = dragonfix$copy(coordA);
            dragonfix$persistentCopyB = dragonfix$copy(coordB);
        } else if (mode == PersistentSchematicMode.PASTE) {
            dragonfix$persistentPaste = dragonfix$copy(coordC);
            dragonfix$persistentArraySpan = dragonfix$copy(arraySpan);
        }
    }

    @Override
    public void dragonfix$syncPersistentCopyFromNormalSelection() {
        dragonfix$persistentCopyA = dragonfix$copy(coordA);
        dragonfix$persistentCopyB = dragonfix$copy(coordB);
    }

    @Override
    public void dragonfix$activateNormalSelection(boolean syncPersistentCopy) {
        if (syncPersistentCopy) {
            dragonfix$normalCoordA = dragonfix$copy(dragonfix$persistentCopyA);
            dragonfix$normalCoordB = dragonfix$copy(dragonfix$persistentCopyB);
        }

        coordA = dragonfix$copy(dragonfix$normalCoordA);
        coordB = dragonfix$copy(dragonfix$normalCoordB);
        coordC = dragonfix$copy(dragonfix$normalCoordC);
        arraySpan = dragonfix$copy(dragonfix$normalArraySpan);
    }

    @Override
    public void dragonfix$activatePersistentSelection(PersistentSchematicMode mode) {
        if (mode == PersistentSchematicMode.COPY) {
            coordA = dragonfix$copy(dragonfix$persistentCopyA);
            coordB = dragonfix$copy(dragonfix$persistentCopyB);
            coordC = null;
            arraySpan = null;
        } else if (mode == PersistentSchematicMode.PASTE) {
            coordA = null;
            coordB = null;
            coordC = dragonfix$copy(dragonfix$persistentPaste);
            arraySpan = dragonfix$copy(dragonfix$persistentArraySpan);
        }
    }

    @Override
    public void dragonfix$resetPersistentPasteSelection() {
        dragonfix$persistentPaste = null;
        dragonfix$persistentArraySpan = null;
        coordA = null;
        coordB = null;
        coordC = null;
        arraySpan = null;
    }

    @Unique
    private static Location dragonfix$copy(Location location) {
        return location == null ? null : location.clone();
    }

    @Unique
    private static Vector3i dragonfix$copy(Vector3i vector) {
        return vector == null ? null : new Vector3i(vector);
    }
}
