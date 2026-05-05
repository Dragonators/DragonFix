package com.dragonfix.mixin.mixins.gtnhlib;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@Mixin(value = VertexFormat.class, remap = false)
public abstract class VertexFormatMixin {

    @Shadow(remap = false)
    public abstract void writeQuad(ModelQuadView quad, ByteBuffer out);

    /**
     * Binary compatibility overload for mods compiled against GTNHLib's pre-CEL quad package.
     */
    @Deprecated
    public final void writeQuad(QuadView quad, ByteBuffer out) {
        writeQuad((ModelQuadView) quad, out);
    }
}
