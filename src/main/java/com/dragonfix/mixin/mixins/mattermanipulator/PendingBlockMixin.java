package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.List;

import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.LittleTilesAnalysisResult;
import com.dragonfix.mattermanipulator.PendingBlockLittleTilesBridge;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;

@Mixin(value = PendingBlock.class, remap = false)
public abstract class PendingBlockMixin implements PendingBlockLittleTilesBridge {

    @Unique
    private static final int dragonfix$ANALYZE_LT = 0b1 << 5;

    @Unique
    private ITileAnalysisIntegration dragonfix$littleTilesAnalysis;

    @Inject(method = "getIntegrations", at = @At("RETURN"), remap = false)
    private void dragonfix$addLittleTilesIntegration(CallbackInfoReturnable<List<ITileAnalysisIntegration>> cir) {
        if (dragonfix$littleTilesAnalysis != null) {
            cir.getReturnValue()
                .add(dragonfix$littleTilesAnalysis);
        }
    }

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private void dragonfix$resetLittleTilesIntegration(CallbackInfoReturnable<PendingBlock> cir) {
        dragonfix$littleTilesAnalysis = null;
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
    }

    @Inject(method = "analyze", at = @At("RETURN"), remap = false)
    private void dragonfix$analyzeLittleTiles(TileEntity te, int flags, CallbackInfoReturnable<PendingBlock> cir) {
        if (te != null && (flags & dragonfix$ANALYZE_LT) != 0) {
            dragonfix$littleTilesAnalysis = LittleTilesAnalysisResult.analyze(te);
        }
    }

    @Inject(method = "migrate", at = @At("TAIL"), remap = false)
    private void dragonfix$migrateLittleTilesIntegration(CallbackInfoReturnable<PendingBlock> cir) {
        if (dragonfix$littleTilesAnalysis != null) {
            dragonfix$littleTilesAnalysis.migrate();
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includeLittleTilesInHashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(
            31 * cir.getReturnValue()
                + (dragonfix$littleTilesAnalysis == null ? 0 : dragonfix$littleTilesAnalysis.hashCode()));
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
    }

    @Override
    public ITileAnalysisIntegration dragonfix$getLittleTilesAnalysis() {
        return dragonfix$littleTilesAnalysis;
    }

    @Override
    public void dragonfix$setLittleTilesAnalysis(ITileAnalysisIntegration analysis) {
        dragonfix$littleTilesAnalysis = analysis;
    }
}
