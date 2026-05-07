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
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicConfigData;
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
    @SerializedName("dragonfixPersistent")
    private PersistentSchematicConfigData dragonfix$persistent = new PersistentSchematicConfigData();

    @Inject(method = "getPasteVisualDeltas", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$getPersistentPasteVisualDeltas(World world, boolean doTransform,
        CallbackInfoReturnable<MMConfig.VoxelAABB> cir) {
        PersistentSchematicConfigData data = dragonfix$data();
        if (data.mode != PersistentSchematicMode.PASTE) return;
        if (world == null) {
            cir.setReturnValue(null);
            return;
        }

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork.getAvailableSchematic(data.id, data.file, world);
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
        PersistentSchematicConfigData data = dragonfix$data();
        cir.setReturnValue(31 * cir.getReturnValue() + Objects.hash(data.mode, data.file, data.id));
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includePersistentSchematicInEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        PersistentSchematicConfigBridge other = (PersistentSchematicConfigBridge) obj;
        PersistentSchematicConfigData data = dragonfix$data();
        cir.setReturnValue(
            data.mode == other.dragonfix$getPersistentSchematicMode()
                && Objects.equals(data.file, other.dragonfix$getPersistentSchematicFile())
                && Objects.equals(data.id, other.dragonfix$getPersistentSchematicId()));
    }

    @Override
    public PersistentSchematicMode dragonfix$getPersistentSchematicMode() {
        return dragonfix$data().mode == null ? PersistentSchematicMode.NONE : dragonfix$data().mode;
    }

    @Override
    public void dragonfix$setPersistentSchematicMode(PersistentSchematicMode mode) {
        dragonfix$data().mode = mode == null ? PersistentSchematicMode.NONE : mode;
    }

    @Override
    public String dragonfix$getPersistentSchematicFile() {
        return dragonfix$data().file;
    }

    @Override
    public void dragonfix$setPersistentSchematicFile(String fileName) {
        dragonfix$data().file = fileName == null ? "" : fileName;
    }

    @Override
    public UUID dragonfix$getPersistentSchematicId() {
        return dragonfix$data().id;
    }

    @Override
    public void dragonfix$setPersistentSchematicId(UUID id) {
        dragonfix$data().id = id;
    }

    @Override
    public int dragonfix$getPersistentPasteRestoreState() {
        return dragonfix$data().pasteRestore;
    }

    @Override
    public void dragonfix$setPersistentPasteRestoreState(int state) {
        dragonfix$data().pasteRestore = state;
    }

    @Override
    public long dragonfix$getPersistentPasteRestoreStartedMs() {
        return dragonfix$data().pasteRestoreStartedMs;
    }

    @Override
    public void dragonfix$setPersistentPasteRestoreStartedMs(long startedMs) {
        dragonfix$data().pasteRestoreStartedMs = startedMs;
    }

    @Override
    public void dragonfix$capturePersistentSchematic(PersistentSchematicMode mode) {
        if (mode == PersistentSchematicMode.PASTE) {
            PersistentSchematicConfigData data = dragonfix$data();
            data.pasteFile = data.file;
            data.pasteId = data.id;
            data.pasteRestore = data.pasteFile == null || data.pasteFile.isEmpty() ? RESTORE_NONE : RESTORE_PENDING;
            data.pasteRestoreStartedMs = 0L;
        }
    }

    @Override
    public void dragonfix$activatePersistentSchematic(PersistentSchematicMode mode, String fileName, UUID id) {
        PersistentSchematicConfigData data = dragonfix$data();
        if (mode == PersistentSchematicMode.PASTE) {
            if (fileName == null || fileName.isEmpty()) {
                data.file = data.pasteFile == null ? "" : data.pasteFile;
                data.id = data.pasteId;
            } else {
                data.file = PersistentSchematic.normalizeFileName(fileName);
                data.id = id;
                data.pasteFile = data.file;
                data.pasteId = id;
            }
            data.pasteRestore = data.file == null || data.file.isEmpty() ? RESTORE_NONE : RESTORE_PENDING;
            data.pasteRestoreStartedMs = 0L;
            return;
        }

        data.file = fileName == null || fileName.isEmpty() ? "" : PersistentSchematic.normalizeFileName(fileName);
        data.id = id;
    }

    @Override
    public void dragonfix$captureNormalSelection() {
        PersistentSchematicConfigData data = dragonfix$data();
        data.normalCoordA = dragonfix$copy(coordA);
        data.normalCoordB = dragonfix$copy(coordB);
        data.normalCoordC = dragonfix$copy(coordC);
        data.normalArraySpan = dragonfix$copy(arraySpan);
    }

    @Override
    public void dragonfix$capturePersistentSelection(PersistentSchematicMode mode) {
        PersistentSchematicConfigData data = dragonfix$data();
        if (mode == PersistentSchematicMode.COPY) {
            data.persistentCopyA = dragonfix$copy(coordA);
            data.persistentCopyB = dragonfix$copy(coordB);
        } else if (mode == PersistentSchematicMode.PASTE) {
            data.persistentPaste = dragonfix$copy(coordC);
            data.persistentArraySpan = dragonfix$copy(arraySpan);
        }
    }

    @Override
    public void dragonfix$syncPersistentCopyFromNormalSelection() {
        PersistentSchematicConfigData data = dragonfix$data();
        data.persistentCopyA = dragonfix$copy(coordA);
        data.persistentCopyB = dragonfix$copy(coordB);
    }

    @Override
    public void dragonfix$activateNormalSelection(boolean syncPersistentCopy) {
        PersistentSchematicConfigData data = dragonfix$data();
        if (syncPersistentCopy) {
            data.normalCoordA = dragonfix$copy(data.persistentCopyA);
            data.normalCoordB = dragonfix$copy(data.persistentCopyB);
        }

        coordA = dragonfix$copy(data.normalCoordA);
        coordB = dragonfix$copy(data.normalCoordB);
        coordC = dragonfix$copy(data.normalCoordC);
        arraySpan = dragonfix$copy(data.normalArraySpan);
    }

    @Override
    public void dragonfix$activatePersistentSelection(PersistentSchematicMode mode) {
        PersistentSchematicConfigData data = dragonfix$data();
        if (mode == PersistentSchematicMode.COPY) {
            coordA = dragonfix$copy(data.persistentCopyA);
            coordB = dragonfix$copy(data.persistentCopyB);
            coordC = null;
            arraySpan = null;
        } else if (mode == PersistentSchematicMode.PASTE) {
            coordA = null;
            coordB = null;
            coordC = dragonfix$copy(data.persistentPaste);
            arraySpan = dragonfix$copy(data.persistentArraySpan);
        }
    }

    @Override
    public void dragonfix$resetPersistentPasteSelection() {
        PersistentSchematicConfigData data = dragonfix$data();
        data.persistentPaste = null;
        data.persistentArraySpan = null;
        coordA = null;
        coordB = null;
        coordC = null;
        arraySpan = null;
    }

    @Override
    public void dragonfix$resetPersistentPasteSchematic() {
        PersistentSchematicConfigData data = dragonfix$data();
        data.pasteFile = "";
        data.pasteId = null;
        data.file = "";
        data.id = null;
        data.pasteRestore = RESTORE_NONE;
        data.pasteRestoreStartedMs = 0L;
    }

    @Override
    public void dragonfix$clearStoredPersistentPasteSession() {
        PersistentSchematicConfigData data = dragonfix$data();
        data.persistentPaste = null;
        data.persistentArraySpan = null;
        data.pasteFile = "";
        data.pasteId = null;
        data.pasteRestore = RESTORE_NONE;
        data.pasteRestoreStartedMs = 0L;
    }

    @Unique
    private PersistentSchematicConfigData dragonfix$data() {
        if (dragonfix$persistent == null) {
            dragonfix$persistent = new PersistentSchematicConfigData();
        }
        if (dragonfix$persistent.mode == null) {
            dragonfix$persistent.mode = PersistentSchematicMode.NONE;
        }
        if (dragonfix$persistent.file == null) {
            dragonfix$persistent.file = "";
        }
        if (dragonfix$persistent.pasteFile == null) {
            dragonfix$persistent.pasteFile = "";
        }

        return dragonfix$persistent;
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
