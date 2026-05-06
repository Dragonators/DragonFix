package com.dragonfix.mattermanipulator;

import com.dragonfix.mattermanipulator.DragonFixRenderHints.Bounds;
import com.dragonfix.mattermanipulator.DragonFixRenderHints.CustomRenderer;

public interface RenderHintsHintBridge {

    Bounds dragonfix$getBounds();

    void dragonfix$setBounds(Bounds bounds);

    CustomRenderer dragonfix$getCustomRenderer();

    void dragonfix$setCustomRenderer(CustomRenderer renderer);

    int dragonfix$getQuadCount();

    void dragonfix$setQuadCount(int quadCount);
}
