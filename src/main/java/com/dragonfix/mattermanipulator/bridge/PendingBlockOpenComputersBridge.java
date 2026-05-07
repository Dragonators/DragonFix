package com.dragonfix.mattermanipulator.bridge;

import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;

public interface PendingBlockOpenComputersBridge {

    ITileAnalysisIntegration dragonfix$getOpenComputersMicrocontrollerAnalysis();

    void dragonfix$setOpenComputersMicrocontrollerAnalysis(ITileAnalysisIntegration analysis);
}
