package com.dragonfix.mixin.mixins.mattermanipulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.analysis.AE2CondenserAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.CarpentersBlocksAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.DragonFixMultipartAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.EnderIOSoulBinderAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.LittleTilesAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.MalisisCustomDoorAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.OpenComputersMicrocontrollerAnalysisResult;
import com.dragonfix.mattermanipulator.bridge.PendingBlockAvaritiaddonsBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockDoorBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockLittleTilesBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockMachineInventoryBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockMalisisDoorsBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockOpenComputersBridge;
import com.recursive_pineapple.matter_manipulator.common.building.AEAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.CopyableProperty;
import com.recursive_pineapple.matter_manipulator.common.building.GTAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.ImmutableBlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.InventoryAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.recursive_pineapple.matter_manipulator.common.utils.Mods;

import cpw.mods.fml.common.Loader;

/**
 * Attaches DragonFix tile analysis integrations and transform fixes to MatterManipulator pending blocks.
 *
 * <p>
 * Integration hook structure adapted from GTNewHorizons/MatterManipulator PR #34 by Luca-Guettinger and
 * RecursivePineapple.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/34">MatterManipulator PR #34</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/9d76ed6e8ec87da8f55404893ea3b5ebe6912759">MatterManipulator
 *      commit 9d76ed6e</a>
 */
@Mixin(value = PendingBlock.class, remap = false)
public abstract class PendingBlockMixin
    implements PendingBlockLittleTilesBridge, PendingBlockAvaritiaddonsBridge, PendingBlockMachineInventoryBridge,
    PendingBlockMalisisDoorsBridge, PendingBlockOpenComputersBridge, PendingBlockDoorBridge {

    @Unique
    private static final double dragonfix$ROTATION_EPSILON = 1e-4d;

    @Unique
    private static final int dragonfix$ANALYZE_LT = 0b1 << 5;

    @Unique
    private static final int dragonfix$ANALYZE_CB = 0b1 << 6;

    @Unique
    private static final int dragonfix$ANALYZE_MP = 0b1 << 3;

    @Unique
    private static final int dragonfix$ANALYZE_INV = 0b1 << 4;

    @Shadow(remap = false)
    public ImmutableBlockSpec spec;

    @Shadow(remap = false)
    public ITileAnalysisIntegration mp;

    @Shadow(remap = false)
    public InventoryAnalysis inventory;

    @Shadow(remap = false)
    public ITileAnalysisIntegration gt;

    @Shadow(remap = false)
    public ITileAnalysisIntegration ae;

    @Shadow(remap = false)
    public ITileAnalysisIntegration arch;

    @Shadow(remap = false)
    public int renderOrder;

    @Shadow(remap = false)
    public int buildOrder;

    @Unique
    private ITileAnalysisIntegration dragonfix$littleTilesAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$carpentersBlocksAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$ae2CondenserAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$enderIOSoulBinderAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$malisisCustomDoorAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$openComputersMicrocontrollerAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$doorAnalysis;

    @Unique
    private String dragonfix$rotationBeforeTransform;

    /**
     * @author DragonFix
     * @reason Include DragonFix-managed tile integrations in every original integration consumer.
     */
    @Overwrite(remap = false)
    private List<ITileAnalysisIntegration> getIntegrations() {
        List<ITileAnalysisIntegration> integrations = new ArrayList<>();

        if (gt != null) {
            integrations.add(gt);
        }
        if (ae != null) {
            integrations.add(ae);
        }
        if (arch != null) {
            integrations.add(arch);
        }
        if (mp != null) {
            integrations.add(mp);
        }
        if (dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis != null) {
            integrations.add(dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis);
        }
        if (dragonfix$ae2CondenserAnalysis != null) {
            integrations.add(dragonfix$ae2CondenserAnalysis);
        }
        if (dragonfix$enderIOSoulBinderAnalysis != null) {
            integrations.add(dragonfix$enderIOSoulBinderAnalysis);
        }
        if (dragonfix$littleTilesAnalysis != null) {
            integrations.add(dragonfix$littleTilesAnalysis);
        }
        if (dragonfix$carpentersBlocksAnalysis != null) {
            integrations.add(dragonfix$carpentersBlocksAnalysis);
        }
        if (dragonfix$malisisCustomDoorAnalysis != null) {
            integrations.add(dragonfix$malisisCustomDoorAnalysis);
        }
        if (dragonfix$openComputersMicrocontrollerAnalysis != null) {
            integrations.add(dragonfix$openComputersMicrocontrollerAnalysis);
        }
        if (dragonfix$doorAnalysis != null) {
            integrations.add(dragonfix$doorAnalysis);
        }

        return integrations;
    }

    /**
     * @author DragonFix
     * @reason Clear DragonFix-managed integrations together with MM's own pending-block analysis state.
     */
    @Overwrite(remap = false)
    public PendingBlock reset() {
        spec = null;
        gt = null;
        ae = null;
        arch = null;
        mp = null;
        inventory = null;
        renderOrder = 0;
        buildOrder = 0;
        dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis = null;
        dragonfix$ae2CondenserAnalysis = null;
        dragonfix$enderIOSoulBinderAnalysis = null;
        dragonfix$littleTilesAnalysis = null;
        dragonfix$carpentersBlocksAnalysis = null;
        dragonfix$malisisCustomDoorAnalysis = null;
        dragonfix$openComputersMicrocontrollerAnalysis = null;
        dragonfix$doorAnalysis = null;

        return (PendingBlock) (Object) this;
    }

    @Inject(
        method = "clone()Lcom/recursive_pineapple/matter_manipulator/common/building/PendingBlock;",
        at = @At("RETURN"),
        remap = false)
    private void dragonfix$cloneLittleTilesIntegration(CallbackInfoReturnable<PendingBlock> cir) {
        ITileAnalysisIntegration analysis = dragonfix$littleTilesAnalysis;
        if (analysis != null) {
            ((PendingBlockLittleTilesBridge) cir.getReturnValue()).dragonfix$setLittleTilesAnalysis(analysis.clone());
        }

        analysis = dragonfix$carpentersBlocksAnalysis;
        if (analysis != null) {
            ((PendingBlockLittleTilesBridge) cir.getReturnValue())
                .dragonfix$setCarpentersBlocksAnalysis(analysis.clone());
        }

        analysis = dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis;
        if (analysis != null) {
            ((PendingBlockAvaritiaddonsBridge) cir.getReturnValue())
                .dragonfix$setAvaritiaddonsExtremeAutoCrafterAnalysis(analysis.clone());
        }

        analysis = dragonfix$ae2CondenserAnalysis;
        if (analysis != null) {
            ((PendingBlockMachineInventoryBridge) cir.getReturnValue())
                .dragonfix$setAE2CondenserAnalysis(analysis.clone());
        }

        analysis = dragonfix$enderIOSoulBinderAnalysis;
        if (analysis != null) {
            ((PendingBlockMachineInventoryBridge) cir.getReturnValue())
                .dragonfix$setEnderIOSoulBinderAnalysis(analysis.clone());
        }

        analysis = dragonfix$malisisCustomDoorAnalysis;
        if (analysis != null) {
            ((PendingBlockMalisisDoorsBridge) cir.getReturnValue())
                .dragonfix$setMalisisCustomDoorAnalysis(analysis.clone());
        }

        analysis = dragonfix$openComputersMicrocontrollerAnalysis;
        if (analysis != null) {
            ((PendingBlockOpenComputersBridge) cir.getReturnValue())
                .dragonfix$setOpenComputersMicrocontrollerAnalysis(analysis.clone());
        }

        analysis = dragonfix$doorAnalysis;
        if (analysis != null) {
            ((PendingBlockDoorBridge) cir.getReturnValue()).dragonfix$setDoorAnalysis(analysis.clone());
        }
    }

    /**
     * @author DragonFix
     * @reason Analyze DragonFix-supported tile data in the same pass as MM's built-in integrations.
     */
    @Overwrite(remap = false)
    public PendingBlock analyze(TileEntity te, int flags) {
        if (te != null) {
            if ((flags & PendingBlock.ANALYZE_GT) != 0 && Mods.GregTech.isModLoaded()) {
                gt = GTAnalysisResult.analyze(te);
            }
            if ((flags & PendingBlock.ANALYZE_AE) != 0 && Mods.AppliedEnergistics2.isModLoaded()) {
                ae = AEAnalysisResult.analyze(te);
            }
            if ((flags & PendingBlock.ANALYZE_ARCH) != 0 && Mods.ArchitectureCraft.isModLoaded()) {
                arch = ArchitectureCraftAnalysisResult.analyze(te);
            }
            if ((flags & dragonfix$ANALYZE_INV) != 0 && te instanceof IInventory inventoryTile) {
                inventory = InventoryAnalysis.fromInventory(inventoryTile, false);
            }
        }

        if (te != null && (flags & dragonfix$ANALYZE_MP) != 0 && Loader.isModLoaded("ForgeMultipart")) {
            mp = DragonFixMultipartAnalysisResult.analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_INV) != 0 && Loader.isModLoaded("avaritiaddons")) {
            dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis = AvaritiaddonsExtremeAutoCrafterAnalysisResult
                .analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_INV) != 0 && Loader.isModLoaded("appliedenergistics2")) {
            dragonfix$ae2CondenserAnalysis = AE2CondenserAnalysisResult.analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_INV) != 0 && Loader.isModLoaded("EnderIO")) {
            dragonfix$enderIOSoulBinderAnalysis = EnderIOSoulBinderAnalysisResult.analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_LT) != 0) {
            dragonfix$littleTilesAnalysis = LittleTilesAnalysisResult.analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_CB) != 0) {
            dragonfix$carpentersBlocksAnalysis = CarpentersBlocksAnalysisResult.analyze(te);
        }
        if (te != null && Loader.isModLoaded("malisisdoors")) {
            dragonfix$malisisCustomDoorAnalysis = MalisisCustomDoorAnalysisResult.analyze(te);
        }
        if (te != null && Loader.isModLoaded("OpenComputers")) {
            dragonfix$openComputersMicrocontrollerAnalysis = OpenComputersMicrocontrollerAnalysisResult.analyze(te);
            if (dragonfix$openComputersMicrocontrollerAnalysis != null) {
                inventory = null;
            }
        }

        return (PendingBlock) (Object) this;
    }

    @Inject(method = "transform*", at = @At("HEAD"), remap = false)
    private void dragonfix$captureRotationBeforeTransform(Transform transform, CallbackInfo ci) {
        dragonfix$rotationBeforeTransform = spec == null ? null : spec.getProperty(CopyableProperty.ROTATION);
    }

    @Redirect(
        method = "transform*",
        at = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I"),
        remap = false)
    private int dragonfix$parseRotationAsDouble(String rotation) {
        return (int) Double.parseDouble(rotation);
    }

    @Inject(method = "transform*", at = @At("RETURN"), remap = false)
    private void dragonfix$restorePreciseRotation(Transform transform, CallbackInfo ci) {
        String rotationText = dragonfix$rotationBeforeTransform;
        dragonfix$rotationBeforeTransform = null;

        if (rotationText == null || rotationText.isEmpty()) return;

        double rotation;
        try {
            rotation = Double.parseDouble(rotationText);
        } catch (NumberFormatException ignored) {
            return;
        }

        Vector3f v = new Vector3f(0, 0, 1).rotateAxis((float) Math.toRadians(rotation), 0, 1, 0)
            .mulTransposeDirection(transform.getRotation());

        rotation = Math.toDegrees(Math.atan2(v.x, v.z));
        rotation = (rotation % 360d + 360d) % 360d;

        EnumMap<CopyableProperty, String> properties = new EnumMap<>(CopyableProperty.class);
        for (CopyableProperty property : CopyableProperty.VALUES) {
            String value = spec.getProperty(property);
            if (value != null && !value.isEmpty()) properties.put(property, value);
        }

        properties.put(CopyableProperty.ROTATION, dragonfix$formatRotation(rotation));
        spec = spec.withProperties(properties);
    }

    /**
     * @author DragonFix
     * @reason Migrate DragonFix-managed integrations with MM's built-in integration data.
     */
    @Overwrite(remap = false)
    public PendingBlock migrate() {
        if (gt != null) {
            gt.migrate();
        }
        if (ae != null) {
            ae.migrate();
        }
        if (arch != null) {
            arch.migrate();
        }
        if (mp != null) {
            mp.migrate();
        }
        if (dragonfix$littleTilesAnalysis != null) {
            dragonfix$littleTilesAnalysis.migrate();
        }
        if (dragonfix$carpentersBlocksAnalysis != null) {
            dragonfix$carpentersBlocksAnalysis.migrate();
        }
        if (dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis != null) {
            dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis.migrate();
        }
        if (dragonfix$ae2CondenserAnalysis != null) {
            dragonfix$ae2CondenserAnalysis.migrate();
        }
        if (dragonfix$enderIOSoulBinderAnalysis != null) {
            dragonfix$enderIOSoulBinderAnalysis.migrate();
        }
        if (dragonfix$malisisCustomDoorAnalysis != null) {
            dragonfix$malisisCustomDoorAnalysis.migrate();
        }
        if (dragonfix$openComputersMicrocontrollerAnalysis != null) {
            dragonfix$openComputersMicrocontrollerAnalysis.migrate();
        }
        if (dragonfix$doorAnalysis != null) {
            dragonfix$doorAnalysis.migrate();
        }

        return (PendingBlock) (Object) this;
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includeLittleTilesInHashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$littleTilesAnalysis == null ? 0 : dragonfix$littleTilesAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$carpentersBlocksAnalysis == null ? 0 : dragonfix$carpentersBlocksAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue() + (dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis == null ? 0
                : dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$ae2CondenserAnalysis == null ? 0 : dragonfix$ae2CondenserAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$enderIOSoulBinderAnalysis == null ? 0 : dragonfix$enderIOSoulBinderAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$malisisCustomDoorAnalysis == null ? 0 : dragonfix$malisisCustomDoorAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue() + (dragonfix$openComputersMicrocontrollerAnalysis == null ? 0
                : dragonfix$openComputersMicrocontrollerAnalysis.hashCode()));
        cir.setReturnValue(
            31 * cir.getReturnValue() + (dragonfix$doorAnalysis == null ? 0 : dragonfix$doorAnalysis.hashCode()));
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includeLittleTilesInEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration other = ((PendingBlockLittleTilesBridge) obj).dragonfix$getLittleTilesAnalysis();
        if (dragonfix$littleTilesAnalysis == null) {
            cir.setReturnValue(other == null);
        } else {
            cir.setReturnValue(dragonfix$littleTilesAnalysis.equals(other));
        }

        if (!cir.getReturnValueZ()) return;

        other = ((PendingBlockLittleTilesBridge) obj).dragonfix$getCarpentersBlocksAnalysis();
        if (dragonfix$carpentersBlocksAnalysis == null) {
            cir.setReturnValue(other == null);
        } else {
            cir.setReturnValue(dragonfix$carpentersBlocksAnalysis.equals(other));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherAvaritiaddons = ((PendingBlockAvaritiaddonsBridge) obj)
            .dragonfix$getAvaritiaddonsExtremeAutoCrafterAnalysis();
        if (dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis == null) {
            cir.setReturnValue(otherAvaritiaddons == null);
        } else {
            cir.setReturnValue(dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis.equals(otherAvaritiaddons));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherAE2 = ((PendingBlockMachineInventoryBridge) obj)
            .dragonfix$getAE2CondenserAnalysis();
        if (dragonfix$ae2CondenserAnalysis == null) {
            cir.setReturnValue(otherAE2 == null);
        } else {
            cir.setReturnValue(dragonfix$ae2CondenserAnalysis.equals(otherAE2));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherEnderIO = ((PendingBlockMachineInventoryBridge) obj)
            .dragonfix$getEnderIOSoulBinderAnalysis();
        if (dragonfix$enderIOSoulBinderAnalysis == null) {
            cir.setReturnValue(otherEnderIO == null);
        } else {
            cir.setReturnValue(dragonfix$enderIOSoulBinderAnalysis.equals(otherEnderIO));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherMalisisDoor = ((PendingBlockMalisisDoorsBridge) obj)
            .dragonfix$getMalisisCustomDoorAnalysis();
        if (dragonfix$malisisCustomDoorAnalysis == null) {
            cir.setReturnValue(otherMalisisDoor == null);
        } else {
            cir.setReturnValue(dragonfix$malisisCustomDoorAnalysis.equals(otherMalisisDoor));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherMicrocontroller = ((PendingBlockOpenComputersBridge) obj)
            .dragonfix$getOpenComputersMicrocontrollerAnalysis();
        if (dragonfix$openComputersMicrocontrollerAnalysis == null) {
            cir.setReturnValue(otherMicrocontroller == null);
        } else {
            cir.setReturnValue(dragonfix$openComputersMicrocontrollerAnalysis.equals(otherMicrocontroller));
        }

        if (!cir.getReturnValueZ()) return;

        ITileAnalysisIntegration otherDoor = ((PendingBlockDoorBridge) obj).dragonfix$getDoorAnalysis();
        if (dragonfix$doorAnalysis == null) {
            cir.setReturnValue(otherDoor == null);
        } else {
            cir.setReturnValue(dragonfix$doorAnalysis.equals(otherDoor));
        }
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getLittleTilesAnalysis() {
        return dragonfix$littleTilesAnalysis;
    }

    @Override
    public void dragonfix$setLittleTilesAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$littleTilesAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getCarpentersBlocksAnalysis() {
        return dragonfix$carpentersBlocksAnalysis;
    }

    @Override
    public void dragonfix$setCarpentersBlocksAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$carpentersBlocksAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getAvaritiaddonsExtremeAutoCrafterAnalysis() {
        return dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis;
    }

    @Override
    public void dragonfix$setAvaritiaddonsExtremeAutoCrafterAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getAE2CondenserAnalysis() {
        return dragonfix$ae2CondenserAnalysis;
    }

    @Override
    public void dragonfix$setAE2CondenserAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$ae2CondenserAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getEnderIOSoulBinderAnalysis() {
        return dragonfix$enderIOSoulBinderAnalysis;
    }

    @Override
    public void dragonfix$setEnderIOSoulBinderAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$enderIOSoulBinderAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getMalisisCustomDoorAnalysis() {
        return dragonfix$malisisCustomDoorAnalysis;
    }

    @Override
    public void dragonfix$setMalisisCustomDoorAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$malisisCustomDoorAnalysis = analysis;
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getOpenComputersMicrocontrollerAnalysis() {
        return dragonfix$openComputersMicrocontrollerAnalysis;
    }

    @Override
    public void dragonfix$setOpenComputersMicrocontrollerAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$openComputersMicrocontrollerAnalysis = analysis;
        if (analysis != null) {
            inventory = null;
        }
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getDoorAnalysis() {
        return dragonfix$doorAnalysis;
    }

    @Override
    public void dragonfix$setDoorAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$doorAnalysis = analysis;
    }

    @Unique
    private static String dragonfix$formatRotation(double rotation) {
        if (rotation < dragonfix$ROTATION_EPSILON || 360d - rotation < dragonfix$ROTATION_EPSILON) {
            return "0";
        }

        double rounded = Math.rint(rotation);
        if (Math.abs(rotation - rounded) < dragonfix$ROTATION_EPSILON) {
            return Integer.toString((int) rounded);
        }

        return BigDecimal.valueOf(rotation)
            .stripTrailingZeros()
            .toPlainString();
    }
}
