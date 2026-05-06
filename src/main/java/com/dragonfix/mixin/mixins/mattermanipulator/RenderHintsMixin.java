package com.dragonfix.mixin.mixins.mattermanipulator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.dragonfix.mattermanipulator.DragonFixRenderHints;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;

@Mixin(value = RenderHints.class, remap = false)
public abstract class RenderHintsMixin {

    @ModifyArg(
        method = "onRenderWorldLast",
        at = @At(
            value = "INVOKE",
            target = "Lcom/recursive_pineapple/matter_manipulator/common/items/manipulator/RenderHints$VertexBuffer;ensureSize(JI)V"),
        index = 0,
        remap = false)
    private static long dragonfix$expandHintVboSize(long size) {
        return DragonFixRenderHints.expandVboSize(size);
    }
}
