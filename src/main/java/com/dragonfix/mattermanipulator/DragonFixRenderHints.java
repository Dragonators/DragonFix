package com.dragonfix.mattermanipulator;

import java.util.ArrayList;

import net.minecraft.block.Block;

import com.dragonfix.mattermanipulator.bridge.RenderHintsHintBridge;
import com.dragonfix.mixin.mixins.mattermanipulator.RenderHintsAccessor;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;

public final class DragonFixRenderHints {

    private DragonFixRenderHints() {}

    public static void addHint(int x, int y, int z, double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ, Block block, int meta, short[] tint) {
        RenderHints.addHint(x, y, z, block, meta, tint);

        RenderHintsHintBridge hint = getLastHint();
        if (hint != null) {
            hint.dragonfix$setBounds(new Bounds(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    public static void addCustomHint(int x, int y, int z, Block block, int meta, short[] tint, CustomRenderer renderer,
        int quadCount) {
        RenderHints.addHint(x, y, z, block, meta, tint);

        RenderHintsHintBridge hint = getLastHint();
        if (hint != null) {
            hint.dragonfix$setCustomRenderer(renderer);
            hint.dragonfix$setQuadCount(quadCount);
        }
    }

    public static long expandVboSize(long originalSize) {
        if (originalSize <= 0) {
            return originalSize;
        }

        ArrayList<Object> hints = getHints();
        if (hints == null) return originalSize;

        int hintCount = hints.size();
        if (hintCount == 0) {
            return originalSize;
        }

        long bytesPerHint = originalSize / hintCount;
        if (bytesPerHint <= 0) {
            return originalSize;
        }

        long bytesPerQuad = bytesPerHint / 6;
        if (bytesPerQuad <= 0) {
            return originalSize;
        }

        long extraQuads = 0;
        for (Object hint : hints) {
            if (hint instanceof RenderHintsHintBridge) {
                int quadCount = ((RenderHintsHintBridge) hint).dragonfix$getQuadCount();
                if (quadCount <= 6) {
                    continue;
                }

                long hintExtraQuads = quadCount - 6L;
                if (extraQuads > Long.MAX_VALUE - hintExtraQuads) {
                    return originalSize;
                }
                extraQuads += hintExtraQuads;
            }
        }

        if (extraQuads <= 0) {
            return originalSize;
        }

        if (extraQuads > (Long.MAX_VALUE - originalSize) / bytesPerQuad) {
            return originalSize;
        }

        return originalSize + extraQuads * bytesPerQuad;
    }

    private static RenderHintsHintBridge getLastHint() {
        ArrayList<Object> hints = getHints();
        if (hints == null || hints.isEmpty()) return null;

        Object hint = hints.get(hints.size() - 1);
        return hint instanceof RenderHintsHintBridge ? (RenderHintsHintBridge) hint : null;
    }

    private static ArrayList<Object> getHints() {
        try {
            return RenderHintsAccessor.dragonfix$getHints();
        } catch (AssertionError | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    public static final class Bounds {

        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;

        private Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    public interface CustomRenderer {

        void draw(net.minecraft.client.renderer.Tessellator tessellator, double eyeX, double eyeY, double eyeZ,
            int eyeXint, int eyeYint, int eyeZint);
    }
}
