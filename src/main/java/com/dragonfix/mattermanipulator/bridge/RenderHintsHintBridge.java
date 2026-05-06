package com.dragonfix.mattermanipulator.bridge;

import com.dragonfix.mattermanipulator.DragonFixRenderHints.Bounds;
import com.dragonfix.mattermanipulator.DragonFixRenderHints.CustomRenderer;

public interface RenderHintsHintBridge {

    void dragonfix$setBounds(Bounds bounds);

    void dragonfix$setCustomRenderer(CustomRenderer renderer);

    int dragonfix$getQuadCount();

    void dragonfix$setQuadCount(int quadCount);
}
