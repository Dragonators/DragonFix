package com.dragonfix.mattermanipulator;

import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;

public interface PendingBlockLittleTilesBridge {

    ITileAnalysisIntegration dragonfix$getLittleTilesAnalysis();

    void dragonfix$setLittleTilesAnalysis(ITileAnalysisIntegration analysis);
}
