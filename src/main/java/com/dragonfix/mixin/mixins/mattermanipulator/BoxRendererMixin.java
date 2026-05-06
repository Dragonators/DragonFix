package com.dragonfix.mixin.mixins.mattermanipulator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.recursive_pineapple.matter_manipulator.client.rendering.BoxRenderer;

/**
 * Reuses MatterManipulator box renderer buffers instead of recreating them every frame.
 *
 * <p>
 * Adapted from GTNewHorizons/MatterManipulator commit 184f6db by RecursivePineapple.
 *
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/184f6db9df8b3e49ff01bdc3dd9842306fdbc675">MatterManipulator
 *      commit 184f6db</a>
 */
@Mixin(value = BoxRenderer.class, remap = false)
public abstract class BoxRendererMixin {

    @Unique
    private VertexBuffer dragonfix$buffer;

    @Redirect(
        method = "finish",
        at = @At(value = "NEW", target = "com/gtnewhorizon/gtnhlib/client/renderer/vbo/VertexBuffer"),
        remap = false)
    private VertexBuffer dragonfix$reuseVertexBuffer(VertexFormat format, int drawMode) {
        if (dragonfix$buffer == null) {
            dragonfix$buffer = new VertexBuffer(format, drawMode);
        }
        return dragonfix$buffer;
    }

    @Redirect(
        method = "finish",
        at = @At(value = "INVOKE", target = "Lcom/gtnewhorizon/gtnhlib/client/renderer/vbo/VertexBuffer;close()V"),
        remap = false)
    private void dragonfix$keepVertexBuffer(VertexBuffer buffer) {}
}
