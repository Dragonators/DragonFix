package com.dragonfix.mixin.mixins.opencomputers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import li.cil.oc.client.renderer.TextBufferRenderCache$;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.client.renderer.font.TextureFontRenderer;
import li.cil.oc.util.RenderState;
import scala.Tuple2;

@Mixin(value = TextBufferRenderCache$.class, remap = false)
public abstract class TextBufferRenderCacheMixin {

    @Shadow(remap = false)
    public abstract TextureFontRenderer renderer();

    @Inject(
        method = "render(Lli/cil/oc/client/renderer/font/TextBufferRenderData;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$renderWithoutDisplayList(TextBufferRenderData buffer, CallbackInfo ci) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (buffer.dirty()) {
            for (int[] line : buffer.data()
                .buffer()) {
                renderer().generateChars(line);
            }
            buffer.dirty_$eq(false);
        }

        Tuple2<Object, Object> viewport = buffer.viewport();
        renderer().drawBuffer(buffer.data(), (Integer) viewport._1(), (Integer) viewport._2());

        RenderState.checkError(getClass().getName() + ".render: leaving");
        ci.cancel();
    }
}
