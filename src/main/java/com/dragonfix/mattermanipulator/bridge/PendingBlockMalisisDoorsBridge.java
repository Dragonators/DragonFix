package com.dragonfix.mattermanipulator.bridge;

import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;

public interface PendingBlockMalisisDoorsBridge {

    ITileAnalysisIntegration dragonfix$getMalisisCustomDoorAnalysis();

    void dragonfix$setMalisisCustomDoorAnalysis(ITileAnalysisIntegration analysis);
}
