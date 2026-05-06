package com.dragonfix.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class MyCTMLibGTRenderLayerContext {

    private static final double GT_LAYER_OFFSET_STEP = 1.0e-3D;
    private static final ThreadLocal<int[]> CURRENT_SBR_LAYER = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<int[]> CURRENT_MULTI_LAYER = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<int[]> SBR_LAYER_BIAS = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<int[]> RENDERING_GT_OVERLAY = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<IntArrayList> SBR_LAYER_CURSORS = ThreadLocal.withInitial(IntArrayList::new);
    private static final ThreadLocal<IntArrayList> GT_MULTI_LAYER_CURSORS = ThreadLocal.withInitial(IntArrayList::new);

    private MyCTMLibGTRenderLayerContext() {}

    public static void pushSbrTextureArray() {
        SBR_LAYER_CURSORS.get()
            .add(0);
    }

    public static void popSbrTextureArray() {
        IntArrayList cursors = SBR_LAYER_CURSORS.get();
        if (!cursors.isEmpty()) {
            cursors.removeInt(cursors.size() - 1);
        }
    }

    public static void renderNextSbrTextureLayer(Runnable renderer) {
        IntArrayList cursors = SBR_LAYER_CURSORS.get();
        int layerIndex = 0;
        if (!cursors.isEmpty()) {
            int last = cursors.size() - 1;
            layerIndex = cursors.getInt(last);
            cursors.set(last, layerIndex + 1);
        }

        int[] currentLayer = CURRENT_SBR_LAYER.get();
        int previousLayer = currentLayer[0];
        currentLayer[0] = layerIndex;
        try {
            renderer.run();
        } finally {
            currentLayer[0] = previousLayer;
        }
    }

    public static void renderWithSbrLayerBias(int bias, Runnable renderer) {
        int[] currentBias = SBR_LAYER_BIAS.get();
        int previousBias = currentBias[0];
        currentBias[0] = previousBias + bias;
        try {
            renderer.run();
        } finally {
            currentBias[0] = previousBias;
        }
    }

    public static void pushMultiTextureRender() {
        GT_MULTI_LAYER_CURSORS.get()
            .add(0);
    }

    public static void popMultiTextureRender() {
        IntArrayList cursors = GT_MULTI_LAYER_CURSORS.get();
        if (!cursors.isEmpty()) {
            cursors.removeInt(cursors.size() - 1);
        }
    }

    public static void renderNextMultiTextureLayer(Runnable renderer) {
        IntArrayList cursors = GT_MULTI_LAYER_CURSORS.get();
        int layerIndex = 0;
        if (!cursors.isEmpty()) {
            int last = cursors.size() - 1;
            layerIndex = cursors.getInt(last);
            cursors.set(last, layerIndex + 1);
        }

        int[] currentLayer = CURRENT_MULTI_LAYER.get();
        int previousLayer = currentLayer[0];
        currentLayer[0] = layerIndex;
        try {
            renderer.run();
        } finally {
            currentLayer[0] = previousLayer;
        }
    }

    public static void renderOverlay(Runnable renderer) {
        int[] currentOverlay = RENDERING_GT_OVERLAY.get();
        int previousOverlay = currentOverlay[0];
        currentOverlay[0] = 1;
        try {
            renderer.run();
        } finally {
            currentOverlay[0] = previousOverlay;
        }
    }

    public static double getCurrentOffset() {
        int depth = SBR_LAYER_BIAS.get()[0] + CURRENT_SBR_LAYER.get()[0] + CURRENT_MULTI_LAYER.get()[0];
        if (RENDERING_GT_OVERLAY.get()[0] != 0) {
            depth++;
        }
        if (depth <= 0) {
            return 0.0D;
        }
        return depth * GT_LAYER_OFFSET_STEP;
    }
}
