package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.ArchitectureCraftPreviewBridge;
import com.dragonfix.mattermanipulator.ArchitectureCraftPreviewRenderer;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;

import gcewing.architecture.common.tile.TileShape;

@Mixin(value = ArchitectureCraftAnalysisResult.class, remap = false)
public abstract class ArchitectureCraftAnalysisResultMixin implements ArchitectureCraftPreviewBridge {

    @Unique
    private int dragonfix$previewSide;

    @Unique
    private int dragonfix$previewTurn;

    @Unique
    private double dragonfix$previewOffsetX;

    @Shadow(remap = false)
    public int shape;

    @Shadow(remap = false)
    public PortableItemStack material;

    @Inject(method = "analyze", at = @At("RETURN"), remap = false)
    private static void dragonfix$capturePreviewOrientation(TileEntity te,
        CallbackInfoReturnable<ArchitectureCraftAnalysisResult> cir) {
        ArchitectureCraftAnalysisResult result = cir.getReturnValue();
        if (!(te instanceof TileShape) || result == null) return;

        TileShape tileShape = (TileShape) te;
        ArchitectureCraftAnalysisResultMixin bridge = (ArchitectureCraftAnalysisResultMixin) (Object) result;
        bridge.dragonfix$previewSide = tileShape.side;
        bridge.dragonfix$previewTurn = tileShape.turn;
        bridge.dragonfix$previewOffsetX = tileShape.getOffsetX();
    }

    @Inject(method = "clone", at = @At("RETURN"), remap = false)
    private void dragonfix$clonePreviewOrientation(CallbackInfoReturnable<ArchitectureCraftAnalysisResult> cir) {
        ArchitectureCraftAnalysisResultMixin bridge = (ArchitectureCraftAnalysisResultMixin) (Object) cir
            .getReturnValue();
        bridge.dragonfix$previewSide = dragonfix$previewSide;
        bridge.dragonfix$previewTurn = dragonfix$previewTurn;
        bridge.dragonfix$previewOffsetX = dragonfix$previewOffsetX;
    }

    @Override
    public void dragonfix$addPreviewHint(PendingBlock pendingBlock, short[] tint) {
        Block materialBlock = material == null ? null : material.getBlock();
        if (materialBlock == null) return;

        ArchitectureCraftPreviewRenderer.addHint(
            pendingBlock,
            shape,
            materialBlock,
            material.getMeta(),
            dragonfix$previewSide,
            dragonfix$previewTurn,
            dragonfix$previewOffsetX,
            tint);
    }
}
