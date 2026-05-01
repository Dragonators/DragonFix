package com.dragonfix.mixin;

import java.util.ArrayDeque;

public final class MyCTMLibGTRenderLayerContext {

    private static final double GT_FIRST_LAYER_OFFSET = 3.0e-4D;
    private static final double GT_ADDITIONAL_LAYER_OFFSET = 7.5e-5D;
    private static final ThreadLocal<Integer> CURRENT_SBR_LAYER = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> CURRENT_MULTI_LAYER = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> SBR_LAYER_BIAS = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> RENDERING_GT_OVERLAY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ArrayDeque<Integer>> SBR_LAYER_CURSORS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ArrayDeque<Integer>> GT_MULTI_LAYER_CURSORS = ThreadLocal
        .withInitial(ArrayDeque::new);

    private MyCTMLibGTRenderLayerContext() {}

    public static void pushSbrTextureArray() {
        SBR_LAYER_CURSORS.get()
            .push(0);
    }

    public static void popSbrTextureArray() {
        ArrayDeque<Integer> cursors = SBR_LAYER_CURSORS.get();
        if (!cursors.isEmpty()) {
            cursors.pop();
        }
    }

    public static void renderNextSbrTextureLayer(Runnable renderer) {
        ArrayDeque<Integer> cursors = SBR_LAYER_CURSORS.get();
        int layerIndex = 0;
        if (!cursors.isEmpty()) {
            layerIndex = cursors.pop();
            cursors.push(layerIndex + 1);
        }

        int previousLayer = CURRENT_SBR_LAYER.get();
        CURRENT_SBR_LAYER.set(layerIndex);
        try {
            renderer.run();
        } finally {
            CURRENT_SBR_LAYER.set(previousLayer);
        }
    }

    public static void renderWithSbrLayerBias(int bias, Runnable renderer) {
        int previousBias = SBR_LAYER_BIAS.get();
        SBR_LAYER_BIAS.set(previousBias + bias);
        try {
            renderer.run();
        } finally {
            SBR_LAYER_BIAS.set(previousBias);
        }
    }

    public static void pushMultiTextureRender() {
        GT_MULTI_LAYER_CURSORS.get()
            .push(0);
    }

    public static void popMultiTextureRender() {
        ArrayDeque<Integer> cursors = GT_MULTI_LAYER_CURSORS.get();
        if (!cursors.isEmpty()) {
            cursors.pop();
        }
    }

    public static void renderNextMultiTextureLayer(Runnable renderer) {
        ArrayDeque<Integer> cursors = GT_MULTI_LAYER_CURSORS.get();
        int layerIndex = 0;
        if (!cursors.isEmpty()) {
            layerIndex = cursors.pop();
            cursors.push(layerIndex + 1);
        }

        int previousLayer = CURRENT_MULTI_LAYER.get();
        CURRENT_MULTI_LAYER.set(layerIndex);
        try {
            renderer.run();
        } finally {
            CURRENT_MULTI_LAYER.set(previousLayer);
        }
    }

    public static void renderOverlay(Runnable renderer) {
        boolean previousOverlay = RENDERING_GT_OVERLAY.get();
        RENDERING_GT_OVERLAY.set(true);
        try {
            renderer.run();
        } finally {
            RENDERING_GT_OVERLAY.set(previousOverlay);
        }
    }

    public static double getCurrentOffset() {
        int depth = SBR_LAYER_BIAS.get() + CURRENT_SBR_LAYER.get() + CURRENT_MULTI_LAYER.get();
        if (RENDERING_GT_OVERLAY.get()) {
            depth++;
        }
        if (depth <= 0) {
            return 0.0D;
        }
        return GT_FIRST_LAYER_OFFSET + (depth - 1) * GT_ADDITIONAL_LAYER_OFFSET;
    }
}
