package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.bridge.ArchitectureCraftOrientationBridge;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import gcewing.architecture.common.tile.TileShape;
import gcewing.architecture.compat.Trans3;
import gcewing.architecture.compat.Vector3;

@Mixin(value = ArchitectureCraftAnalysisResult.class, remap = false)
public abstract class ArchitectureCraftAnalysisResultTransformMixin implements ArchitectureCraftOrientationBridge {

    @Unique
    private static final int DRAGONFIX_AXIS_X = 0;

    @Unique
    private static final int DRAGONFIX_AXIS_Y = 1;

    @Unique
    private static final int DRAGONFIX_AXIS_Z = 2;

    @Unique
    private int dragonfix$side;

    @Unique
    private int dragonfix$turn;

    @Unique
    private double dragonfix$offsetX;

    @Inject(method = "analyze", at = @At("RETURN"), remap = false)
    private static void dragonfix$captureOrientation(TileEntity te,
        CallbackInfoReturnable<ArchitectureCraftAnalysisResult> cir) {
        ArchitectureCraftAnalysisResult result = cir.getReturnValue();
        if (!(te instanceof TileShape tileShape) || result == null) return;

        ArchitectureCraftOrientationBridge bridge = (ArchitectureCraftOrientationBridge) result;
        bridge.dragonfix$setArchitectureCraftSide(tileShape.side);
        bridge.dragonfix$setArchitectureCraftTurn(tileShape.turn);
        bridge.dragonfix$setArchitectureCraftOffsetX(tileShape.getOffsetX());
    }

    @Inject(method = "clone*", at = @At("RETURN"), remap = false)
    private void dragonfix$cloneOrientation(CallbackInfoReturnable<ArchitectureCraftAnalysisResult> cir) {
        ArchitectureCraftOrientationBridge bridge = (ArchitectureCraftOrientationBridge) cir.getReturnValue();
        bridge.dragonfix$setArchitectureCraftSide(dragonfix$side);
        bridge.dragonfix$setArchitectureCraftTurn(dragonfix$turn);
        bridge.dragonfix$setArchitectureCraftOffsetX(dragonfix$offsetX);
    }

    @Inject(method = "transform", at = @At("HEAD"), remap = false)
    private void dragonfix$transformOrientation(Transform transform, CallbackInfo ci) {
        ForgeDirection oldX = dragonfix$axis(dragonfix$side, dragonfix$turn, DRAGONFIX_AXIS_X);
        ForgeDirection oldY = dragonfix$axis(dragonfix$side, dragonfix$turn, DRAGONFIX_AXIS_Y);
        ForgeDirection oldZ = dragonfix$axis(dragonfix$side, dragonfix$turn, DRAGONFIX_AXIS_Z);

        ForgeDirection newX = transform.apply(oldX);
        ForgeDirection newY = transform.apply(oldY);
        ForgeDirection newZ = transform.apply(oldZ);

        int packed = dragonfix$findCandidate(newX, newY, newZ, true, true, true);
        if (packed < 0 && newX != oldX) packed = dragonfix$findCandidate(newX, newY, newZ, true, true, false);
        if (packed < 0 && newZ != oldZ) packed = dragonfix$findCandidate(newX, newY, newZ, false, true, true);
        if (packed < 0 && newY != oldY) packed = dragonfix$findCandidate(newX, newY, newZ, true, true, false);
        if (packed < 0) packed = dragonfix$findBestCandidate(newX, newY, newZ);
        if (packed < 0) return;

        int newSide = packed >> 2;
        int newTurn = packed & 3;
        ForgeDirection candidateX = dragonfix$axis(newSide, newTurn, DRAGONFIX_AXIS_X);

        dragonfix$side = newSide;
        dragonfix$turn = newTurn;
        if (candidateX == newX.getOpposite()) {
            dragonfix$offsetX = -dragonfix$offsetX;
        }
    }

    @Inject(method = "apply", at = @At("RETURN"), remap = false)
    private void dragonfix$applyOrientation(IBlockApplyContext ctx, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        TileEntity te = ctx.getTileEntity();
        if (!(te instanceof TileShape tileShape)) return;

        tileShape.setSide(dragonfix$side);
        tileShape.setTurn(dragonfix$turn);
        tileShape.setOffsetX(dragonfix$offsetX);
        tileShape.markChanged();
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includeOrientationInHashCode(CallbackInfoReturnable<Integer> cir) {
        int result = cir.getReturnValue();
        result = 31 * result + dragonfix$side;
        result = 31 * result + dragonfix$turn;
        result = 31 * result + Double.hashCode(dragonfix$offsetX);
        cir.setReturnValue(result);
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    private void dragonfix$includeOrientationInEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        ArchitectureCraftOrientationBridge other = (ArchitectureCraftOrientationBridge) obj;
        cir.setReturnValue(
            dragonfix$side == other.dragonfix$getArchitectureCraftSide()
                && dragonfix$turn == other.dragonfix$getArchitectureCraftTurn()
                && Double.compare(dragonfix$offsetX, other.dragonfix$getArchitectureCraftOffsetX()) == 0);
    }

    @Override
    public int dragonfix$getArchitectureCraftSide() {
        return dragonfix$side;
    }

    @Override
    public void dragonfix$setArchitectureCraftSide(int side) {
        dragonfix$side = side;
    }

    @Override
    public int dragonfix$getArchitectureCraftTurn() {
        return dragonfix$turn;
    }

    @Override
    public void dragonfix$setArchitectureCraftTurn(int turn) {
        dragonfix$turn = turn;
    }

    @Override
    public double dragonfix$getArchitectureCraftOffsetX() {
        return dragonfix$offsetX;
    }

    @Override
    public void dragonfix$setArchitectureCraftOffsetX(double offsetX) {
        dragonfix$offsetX = offsetX;
    }

    @Unique
    private static int dragonfix$findBestCandidate(ForgeDirection x, ForgeDirection y, ForgeDirection z) {
        int bestPacked = -1;
        int bestScore = -1;

        for (int side = 0; side < 6; side++) {
            for (int turn = 0; turn < 4; turn++) {
                int score = 0;
                if (dragonfix$axis(side, turn, DRAGONFIX_AXIS_X) == x) score++;
                if (dragonfix$axis(side, turn, DRAGONFIX_AXIS_Y) == y) score++;
                if (dragonfix$axis(side, turn, DRAGONFIX_AXIS_Z) == z) score++;
                if (score > bestScore) {
                    bestScore = score;
                    bestPacked = (side << 2) | turn;
                }
            }
        }

        return bestScore == 0 ? -1 : bestPacked;
    }

    @Unique
    private static int dragonfix$findCandidate(ForgeDirection x, ForgeDirection y, ForgeDirection z, boolean matchX,
        boolean matchY, boolean matchZ) {
        for (int side = 0; side < 6; side++) {
            for (int turn = 0; turn < 4; turn++) {
                if (matchX && dragonfix$axis(side, turn, DRAGONFIX_AXIS_X) != x) continue;
                if (matchY && dragonfix$axis(side, turn, DRAGONFIX_AXIS_Y) != y) continue;
                if (matchZ && dragonfix$axis(side, turn, DRAGONFIX_AXIS_Z) != z) continue;
                return (side << 2) | turn;
            }
        }
        return -1;
    }

    @Unique
    private static ForgeDirection dragonfix$axis(int side, int turn, int axis) {
        Vector3 vector = switch (axis) {
            case DRAGONFIX_AXIS_X -> Trans3.sideTurn(side, turn & 3)
                .v(Vector3.unitX);
            case DRAGONFIX_AXIS_Y -> Trans3.sideTurn(side, turn & 3)
                .v(Vector3.unitY);
            case DRAGONFIX_AXIS_Z -> Trans3.sideTurn(side, turn & 3)
                .v(Vector3.unitZ);
            default -> throw new AssertionError();
        };
        return dragonfix$toForgeDirection(vector);
    }

    @Unique
    private static ForgeDirection dragonfix$toForgeDirection(Vector3 vector) {
        double absX = Math.abs(vector.x);
        double absY = Math.abs(vector.y);
        double absZ = Math.abs(vector.z);

        if (absY >= absX && absY >= absZ) return vector.y < 0 ? ForgeDirection.DOWN : ForgeDirection.UP;
        if (absX >= absZ) return vector.x < 0 ? ForgeDirection.WEST : ForgeDirection.EAST;
        return vector.z < 0 ? ForgeDirection.NORTH : ForgeDirection.SOUTH;
    }
}
