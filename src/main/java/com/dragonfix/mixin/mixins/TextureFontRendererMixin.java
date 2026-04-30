package com.dragonfix.mixin.mixins;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mixin.OpenComputersTextureFontRendererBridge;

import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedUnicodeHelper;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.PackedColor.ColorFormat;
import li.cil.oc.util.RenderState;
import li.cil.oc.util.TextBuffer;

@Pseudo
@Mixin(targets = "li.cil.oc.client.renderer.font.TextureFontRenderer", remap = false)
public abstract class TextureFontRendererMixin implements OpenComputersTextureFontRendererBridge {

    @Shadow
    public abstract int charWidth();

    @Shadow
    public abstract int charHeight();

    @Shadow
    public abstract int textureCount();

    @Shadow
    public abstract void bindTexture(int index);

    @Shadow
    public abstract void drawChar(float tx, float ty, int character);

    @Inject(method = "drawBuffer(Lli/cil/oc/util/TextBuffer;II)V", at = @At("HEAD"), cancellable = true)
    private void dragonfix$drawBufferWithTessellator(TextBuffer buffer, int viewportWidth, int viewportHeight,
        CallbackInfo ci) {
        ColorFormat format = buffer.format();
        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glScalef(0.5F, 0.5F, 1);

        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        RenderState.checkError(getClass().getName() + ".drawBuffer: configure state");

        tessellator.startDrawingQuads();
        int rows = Math.min(viewportHeight, buffer.height());
        for (int y = 0; y < rows; y++) {
            short[] color = buffer.color()[y];
            int currentBackground = 0x000000;
            int x = 0;
            int width = 0;
            for (int n = 0; n < color.length && x + width < viewportWidth; n++) {
                int nextBackground = PackedColor.unpackBackground(color[n], format);
                if (nextBackground != currentBackground) {
                    dragonfix$drawQuad(tessellator, currentBackground, x, y, width);
                    currentBackground = nextBackground;
                    x += width;
                    width = 0;
                }
                width++;
            }
            dragonfix$drawQuad(tessellator, currentBackground, x, y, width);
        }
        tessellator.draw();

        RenderState.checkError(getClass().getName() + ".drawBuffer: background");

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (Settings.get()
            .textLinearFiltering()) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        }

        for (int y = 0; y < rows; y++) {
            int[] line = buffer.buffer()[y];
            short[] color = buffer.color()[y];
            float ty = y * charHeight();
            for (int i = 0; i < textureCount(); i++) {
                bindTexture(i);
                tessellator.startDrawingQuads();
                int currentForeground = -1;
                float tx = 0;
                for (int n = 0; n < viewportWidth; n++) {
                    int character = line[n];
                    int nextForeground = PackedColor.unpackForeground(color[n], format);
                    if (nextForeground != currentForeground) {
                        currentForeground = nextForeground;
                        tessellator.setColorOpaque_I(currentForeground);
                    }
                    if (character != ' ') {
                        drawChar(tx, ty, character);
                    }
                    tx += charWidth();
                }
                tessellator.draw();
            }
        }

        RenderState.checkError(getClass().getName() + ".drawBuffer: foreground");

        GL11.glPopAttrib();
        GL11.glPopMatrix();

        RenderState.checkError(getClass().getName() + ".drawBuffer: leaving");
        ci.cancel();
    }

    @Inject(method = "drawString(Ljava/lang/String;II)V", at = @At("HEAD"), cancellable = true)
    private void dragonfix$drawWhiteStringWithTessellator(String text, int x, int y, CallbackInfo ci) {
        dragonfix$drawString(text, x, y, 0xFFFFFF);
        ci.cancel();
    }

    @Override
    public void dragonfix$drawString(String text, int x, int y, int color) {
        int length = ExtendedUnicodeHelper.length(text);
        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(0.5F, 0.5F, 1);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        for (int i = 0; i < textureCount(); i++) {
            bindTexture(i);
            tessellator.startDrawingQuads();
            tessellator.setColorOpaque_I(color);
            float tx = 0;
            int cx = 0;
            for (int n = 0; n < length; n++) {
                int character = text.codePointAt(cx);
                if (character != ' ') {
                    drawChar(tx, 0, character);
                }
                tx += charWidth();
                cx = text.offsetByCodePoints(cx, 1);
            }
            tessellator.draw();
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void dragonfix$drawQuad(Tessellator tessellator, int color, int x, int y, int width) {
        if (color == 0 || width <= 0) {
            return;
        }

        int x0 = x * charWidth();
        int x1 = (x + width) * charWidth();
        int y0 = y * charHeight();
        int y1 = (y + 1) * charHeight();
        tessellator.setColorOpaque_I(color);
        tessellator.addVertex(x0, y1, 0);
        tessellator.addVertex(x1, y1, 0);
        tessellator.addVertex(x1, y0, 0);
        tessellator.addVertex(x0, y0, 0);
    }
}
