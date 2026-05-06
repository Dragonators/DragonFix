package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.dragonfix.mattermanipulator.ArchitectureCraftOrientationBridge;
import com.dragonfix.mattermanipulator.ArchitectureCraftPreviewBridge;
import com.dragonfix.mattermanipulator.ArchitectureCraftPreviewRenderer;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;

@Mixin(value = ArchitectureCraftAnalysisResult.class, remap = false)
public abstract class ArchitectureCraftAnalysisResultMixin implements ArchitectureCraftPreviewBridge {

    @Shadow(remap = false)
    public int shape;

    @Shadow(remap = false)
    public PortableItemStack material;

    @Override
    public void dragonfix$addPreviewHint(PendingBlock pendingBlock, short[] tint) {
        Block materialBlock = material == null ? null : material.getBlock();
        if (materialBlock == null) return;

        ArchitectureCraftOrientationBridge orientation = (ArchitectureCraftOrientationBridge) this;
        ArchitectureCraftPreviewRenderer.addHint(
            pendingBlock,
            shape,
            materialBlock,
            material.getMeta(),
            orientation.dragonfix$getArchitectureCraftSide(),
            orientation.dragonfix$getArchitectureCraftTurn(),
            orientation.dragonfix$getArchitectureCraftOffsetX(),
            tint);
    }
}
