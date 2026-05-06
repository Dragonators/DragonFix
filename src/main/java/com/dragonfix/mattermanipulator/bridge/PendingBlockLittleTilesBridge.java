package com.dragonfix.mattermanipulator.bridge;

import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;

public interface PendingBlockLittleTilesBridge {

    ITileAnalysisIntegration dragonfix$getLittleTilesAnalysis();

    void dragonfix$setLittleTilesAnalysis(ITileAnalysisIntegration analysis);

    ITileAnalysisIntegration dragonfix$getCarpentersBlocksAnalysis();

    void dragonfix$setCarpentersBlocksAnalysis(ITileAnalysisIntegration analysis);
}
