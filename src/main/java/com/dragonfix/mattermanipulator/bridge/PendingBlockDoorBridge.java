package com.dragonfix.mattermanipulator.bridge;

import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;

public interface PendingBlockDoorBridge {

    ITileAnalysisIntegration dragonfix$getDoorAnalysis();

    void dragonfix$setDoorAnalysis(ITileAnalysisIntegration analysis);
}
