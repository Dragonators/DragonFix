package com.dragonfix.mixin.mixins.mattermanipulator;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.AE2CondenserAnalysisResult;
import com.dragonfix.mattermanipulator.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.CarpentersBlocksAnalysisResult;
import com.dragonfix.mattermanipulator.DragonFixMultipartAnalysisResult;
import com.dragonfix.mattermanipulator.LittleTilesAnalysisResult;
import com.dragonfix.mattermanipulator.PendingBlockAvaritiaddonsBridge;
import com.dragonfix.mattermanipulator.PendingBlockLittleTilesBridge;
import com.dragonfix.mattermanipulator.PendingBlockMachineInventoryBridge;
import com.recursive_pineapple.matter_manipulator.common.building.CopyableProperty;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.ImmutableBlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import cpw.mods.fml.common.Loader;

/**
 * Contains mixin hooks adapted from GTNewHorizons/MatterManipulator PR #34 by Luca-Guettinger and RecursivePineapple:
 * https://github.com/GTNewHorizons/MatterManipulator/pull/34
 * https://github.com/GTNewHorizons/MatterManipulator/commit/9d76ed6e8ec87da8f55404893ea3b5ebe6912759
 */
@Mixin(value = PendingBlock.class, remap = false)
public abstract class PendingBlockMixin
    implements PendingBlockLittleTilesBridge, PendingBlockAvaritiaddonsBridge, PendingBlockMachineInventoryBridge {

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

    @Unique
    private ITileAnalysisIntegration dragonfix$littleTilesAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$carpentersBlocksAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis;

    @Unique
    private ITileAnalysisIntegration dragonfix$ae2CondenserAnalysis;

    @Unique
    private String dragonfix$rotationBeforeTransform;

    @Inject(method = "getIntegrations", at = @At("RETURN"), remap = false)
    private void dragonfix$addLittleTilesIntegration(CallbackInfoReturnable<List<ITileAnalysisIntegration>> cir) {
        if (mp != null) {
            cir.getReturnValue()
                .add(mp);
        }
        if (dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis != null) {
            cir.getReturnValue()
                .add(dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis);
        }
        if (dragonfix$ae2CondenserAnalysis != null) {
            cir.getReturnValue()
                .add(dragonfix$ae2CondenserAnalysis);
        }
        if (dragonfix$littleTilesAnalysis != null) {
            cir.getReturnValue()
                .add(dragonfix$littleTilesAnalysis);
        }
        if (dragonfix$carpentersBlocksAnalysis != null) {
            cir.getReturnValue()
                .add(dragonfix$carpentersBlocksAnalysis);
        }
    }

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private void dragonfix$resetLittleTilesIntegration(CallbackInfoReturnable<PendingBlock> cir) {
        mp = null;
        dragonfix$avaritiaddonsExtremeAutoCrafterAnalysis = null;
        dragonfix$ae2CondenserAnalysis = null;
        dragonfix$littleTilesAnalysis = null;
        dragonfix$carpentersBlocksAnalysis = null;
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
    }

    @Inject(method = "analyze", at = @At("RETURN"), remap = false)
    private void dragonfix$analyzeLittleTiles(TileEntity te, int flags, CallbackInfoReturnable<PendingBlock> cir) {
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
        if (te != null && (flags & dragonfix$ANALYZE_LT) != 0) {
            dragonfix$littleTilesAnalysis = LittleTilesAnalysisResult.analyze(te);
        }
        if (te != null && (flags & dragonfix$ANALYZE_CB) != 0) {
            dragonfix$carpentersBlocksAnalysis = CarpentersBlocksAnalysisResult.analyze(te);
        }
    }

    @Inject(method = "transform", at = @At("HEAD"), remap = false)
    private void dragonfix$captureRotationBeforeTransform(Transform transform, CallbackInfo ci) {
        dragonfix$rotationBeforeTransform = spec == null ? null : spec.getProperty(CopyableProperty.ROTATION);
    }

    @Redirect(
        method = "transform",
        at = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I"),
        remap = false)
    private int dragonfix$parseRotationAsDouble(String rotation) {
        return (int) Double.parseDouble(rotation);
    }

    @Inject(method = "transform", at = @At("RETURN"), remap = false)
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

    @Inject(method = "migrate", at = @At("TAIL"), remap = false)
    private void dragonfix$migrateLittleTilesIntegration(CallbackInfoReturnable<PendingBlock> cir) {
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
