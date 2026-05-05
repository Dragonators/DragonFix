package com.dragonfix.mixin.mixins.opencomputers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.PacketHandler$;

@Mixin(value = PacketHandler$.class, remap = false)
public abstract class PacketHandlerMixin {

    @Redirect(
        method = "onTextBufferMultiColorChange",
        at = @At(value = "INVOKE", target = "Lli/cil/oc/api/internal/TextBuffer;setForegroundColor(IZ)V"),
        remap = false)
    private void dragonfix$setForegroundColor(TextBuffer buffer, int color, boolean isPalette) {
        dragonfix$setColor(buffer, color, isPalette, true);
    }

    @Redirect(
        method = "onTextBufferMultiColorChange",
        at = @At(value = "INVOKE", target = "Lli/cil/oc/api/internal/TextBuffer;setBackgroundColor(IZ)V"),
        remap = false)
    private void dragonfix$setBackgroundColor(TextBuffer buffer, int color, boolean isPalette) {
        dragonfix$setColor(buffer, color, isPalette, false);
    }

    @Redirect(
        method = "onTextBufferMultiPaletteChange",
        at = @At(value = "INVOKE", target = "Lli/cil/oc/api/internal/TextBuffer;setPaletteColor(II)V"),
        remap = false)
    private void dragonfix$setPaletteColor(TextBuffer buffer, int index, int color) {
        dragonfix$ensurePaletteDepth(buffer);
        try {
            buffer.setPaletteColor(index, color);
        } catch (Exception ignored) {
            // One-bit screens have no palette; ignore stale palette packets instead of dropping the whole multi packet.
        }
    }

    @Unique
    private void dragonfix$setColor(TextBuffer buffer, int color, boolean isPalette, boolean foreground) {
        if (isPalette) {
            dragonfix$ensurePaletteDepth(buffer);
        }

        try {
            if (foreground) {
                buffer.setForegroundColor(color, isPalette);
            } else {
                buffer.setBackgroundColor(color, isPalette);
            }
        } catch (IllegalArgumentException e) {
            if (!isPalette) {
                throw e;
            }

            int resolvedColor = dragonfix$resolvePaletteColor(buffer, color);
            if (foreground) {
                buffer.setForegroundColor(resolvedColor, false);
            } else {
                buffer.setBackgroundColor(resolvedColor, false);
            }
        }
    }

    @Unique
    private void dragonfix$ensurePaletteDepth(TextBuffer buffer) {
        if (buffer.getColorDepth() != TextBuffer.ColorDepth.OneBit) {
            return;
        }

        try {
            buffer.setColorDepth(TextBuffer.ColorDepth.FourBit);
        } catch (IllegalArgumentException ignored) {
            // Keep the fallback path in dragonfix$setColor for true one-bit screens.
        }
    }

    @Unique
    private int dragonfix$resolvePaletteColor(TextBuffer buffer, int index) {
        try {
            return buffer.getPaletteColor(index);
        } catch (Exception ignored) {
            return dragonfix$defaultPaletteColor(index);
        }
    }

    @Unique
    private int dragonfix$defaultPaletteColor(int index) {
        switch (index & 15) {
            case 0:
                return 0xFFFFFF;
            case 1:
                return 0xFFCC33;
            case 2:
                return 0xCC66CC;
            case 3:
                return 0x6699FF;
            case 4:
                return 0xFFFF33;
            case 5:
                return 0x33CC33;
            case 6:
                return 0xFF6699;
            case 7:
                return 0x333333;
            case 8:
                return 0xCCCCCC;
            case 9:
                return 0x336699;
            case 10:
                return 0x9933CC;
            case 11:
                return 0x333399;
            case 12:
                return 0x663300;
            case 13:
                return 0x336600;
            case 14:
                return 0xFF3333;
            case 15:
            default:
                return 0x000000;
        }
    }
}
