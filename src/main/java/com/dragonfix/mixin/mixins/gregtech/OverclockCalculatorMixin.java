package com.dragonfix.mixin.mixins.gregtech;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.util.DragonFixMath;

import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;

@Mixin(value = OverclockCalculator.class, remap = false)
public abstract class OverclockCalculatorMixin {

    @Shadow(remap = false)
    protected long recipeEUt;
    @Shadow(remap = false)
    protected long machineVoltage;
    @Shadow(remap = false)
    protected long machineAmperage;
    @Shadow(remap = false)
    protected int duration;
    @Shadow(remap = false)
    protected Supplier<Double> durationUnderOneTickSupplier;
    @Shadow(remap = false)
    protected int parallel;
    @Shadow(remap = false)
    protected double eutModifier;
    @Shadow(remap = false)
    protected double durationModifier;
    @Shadow(remap = false)
    protected double eutIncreasePerOC;
    @Shadow(remap = false)
    protected double durationDecreasePerOC;
    @Shadow(remap = false)
    protected boolean laserOC;
    @Shadow(remap = false)
    protected boolean amperageOC;
    @Shadow(remap = false)
    protected int maxOverclocks;
    @Shadow(remap = false)
    protected int maxRegularOverclocks;
    @Shadow(remap = false)
    protected int overclocks;
    @Shadow(remap = false)
    protected boolean noOverclock;
    @Shadow(remap = false)
    protected int currentParallel;
    @Shadow(remap = false)
    protected int recipeHeat;
    @Shadow(remap = false)
    protected int machineHeat;
    @Shadow(remap = false)
    @Final
    protected double durationDecreasePerHeatOC;
    @Shadow(remap = false)
    protected boolean heatOC;
    @Shadow(remap = false)
    protected int calculatedDuration;
    @Shadow(remap = false)
    protected long calculatedConsumption;

    @Shadow(remap = false)
    @Final
    protected static int HEAT_OVERCLOCK_THRESHOLD;

    @Shadow(remap = false)
    public abstract double calculateHeatDiscountMultiplier();

    @Unique
    private boolean dragonfix$hasExplicitCurrentParallel;

    @Unique
    private int dragonfix$originalParallel;

    /**
     * Capture the pre-clamp parallel. GTNH's setter then clamps {@code parallel} down to the actually used parallel
     * when inputs are limited, which loses the information needed to distinguish useful subtick OC from wasted OC.
     */
    @Inject(method = "setCurrentParallel", at = @At("HEAD"), remap = false)
    private void dragonfix$captureOriginalParallel(int currentParallel,
        CallbackInfoReturnable<OverclockCalculator> cir) {
        if (!dragonfix$hasExplicitCurrentParallel) {
            dragonfix$originalParallel = Math.max(1, this.parallel);
        }
        dragonfix$hasExplicitCurrentParallel = true;
    }

    /**
     * @author DragonFix
     * @reason Cap subtick overclock power to the OC tiers that actually reduce duration or become real parallel.
     */
    @Overwrite(remap = false)
    protected void calculateOverclock() {
        double duration = durationUnderOneTickSupplier != null ? durationUnderOneTickSupplier.get()
            : this.duration * durationModifier;

        currentParallel = Math.max(currentParallel, parallel);

        double recipePower = recipeEUt * parallel * eutModifier * calculateHeatDiscountMultiplier();
        double machinePower = machineVoltage * (amperageOC ? machineAmperage : Math.min(machineAmperage, parallel));
        int tiersAbove = (int) GTUtility.log4((long) machinePower / Math.max((long) Math.ceil(recipePower), 32));

        if (noOverclock) {
            calculatedConsumption = (long) Math.ceil(recipePower);
            calculatedDuration = (int) Math.ceil(duration);
            return;
        }

        if (laserOC) {
            dragonfix$calculateLaserOverclock(duration, recipePower, machinePower);
            return;
        }

        overclocks = Math.min(maxOverclocks, tiersAbove);

        if (!amperageOC) {
            int voltageTierMachine = (int) Math.max(GTUtility.log4ceil(machineVoltage / 8), 1);
            int voltageTierRecipe = (int) Math.max(GTUtility.log4ceil(recipeEUt / 8), 1);
            overclocks = Math.min(overclocks, voltageTierMachine - voltageTierRecipe);
        }

        overclocks = Math.max(overclocks, 0);
        overclocks = Math.min(overclocks, dragonfix$getUsefulOverclockLimit(duration, overclocks));

        int heatOverclocks = Math.min(heatOC ? (machineHeat - recipeHeat) / HEAT_OVERCLOCK_THRESHOLD : 0, overclocks);
        int regularOverclocks = overclocks - heatOverclocks;

        calculatedConsumption = (long) Math.ceil(recipePower * GTUtility.powInt(eutIncreasePerOC, overclocks));
        duration /= GTUtility.powInt(durationDecreasePerHeatOC, heatOverclocks);
        duration /= GTUtility.powInt(durationDecreasePerOC, regularOverclocks);
        calculatedDuration = (int) Math.max(duration, 1);
    }

    @Unique
    private void dragonfix$calculateLaserOverclock(double duration, double recipePower, double machinePower) {
        int usefulOverclockLimit = dragonfix$getUsefulOverclockLimit(duration, Integer.MAX_VALUE);
        double eutOverclock = recipePower;

        int regularOverclocks = 0;
        while (eutOverclock * 4.0 < machinePower && regularOverclocks < maxRegularOverclocks
            && regularOverclocks < usefulOverclockLimit) {
            eutOverclock *= 4.0;
            regularOverclocks++;
        }

        double durationPerSlice = durationUnderOneTickSupplier != null ? durationUnderOneTickSupplier.get() : duration;

        int laserOverclocks = 0;
        while (regularOverclocks + laserOverclocks < usefulOverclockLimit) {
            double multiplier = 4.0 + 0.3 * (laserOverclocks + 1);
            double potentialEU = eutOverclock * multiplier;
            double estimatedDuration = duration
                / GTUtility.powInt(durationDecreasePerOC, regularOverclocks + laserOverclocks + 1);

            if (potentialEU >= machinePower) break;
            if (estimatedDuration <= duration / durationPerSlice) break;

            eutOverclock = potentialEU;
            laserOverclocks++;
        }

        overclocks = regularOverclocks + laserOverclocks;
        calculatedConsumption = (long) Math.ceil(eutOverclock);
        calculatedDuration = (int) Math.max(duration / GTUtility.powInt(durationDecreasePerOC, overclocks), 1);
    }

    @Unique
    private int dragonfix$getUsefulOverclockLimit(double duration, int availableOverclocks) {
        if (!dragonfix$hasExplicitCurrentParallel) return availableOverclocks;
        if (availableOverclocks <= 0) return 0;

        double requiredSpeed = duration
            * Math.max(1.0, (double) Math.max(1, currentParallel) / Math.max(1, dragonfix$originalParallel));
        if (requiredSpeed <= 1.0) return 0;

        int heatAvailable = Math.max(heatOC ? (machineHeat - recipeHeat) / HEAT_OVERCLOCK_THRESHOLD : 0, 0);
        int heatOverclocks = Math.min(
            Math.min(heatAvailable, availableOverclocks),
            DragonFixMath.ceilLog(requiredSpeed, durationDecreasePerHeatOC));
        requiredSpeed /= GTUtility.powInt(durationDecreasePerHeatOC, heatOverclocks);

        if (requiredSpeed <= 1.0) return heatOverclocks;

        int remainingOverclocks = availableOverclocks - heatOverclocks;
        int regularOverclocks = Math
            .min(remainingOverclocks, DragonFixMath.ceilLog(requiredSpeed, durationDecreasePerOC));
        return heatOverclocks + regularOverclocks;
    }
}
