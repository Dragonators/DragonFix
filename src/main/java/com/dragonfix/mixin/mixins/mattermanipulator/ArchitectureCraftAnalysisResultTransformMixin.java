package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dragonfix.mattermanipulator.bridge.ArchitectureCraftOrientationBridge;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

import gcewing.architecture.ArchitectureCraft;
import gcewing.architecture.common.shape.Shape;
import gcewing.architecture.common.tile.TileShape;
import gcewing.architecture.compat.IBlockState;
import gcewing.architecture.compat.MetaBlockState;
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

    @Shadow(remap = false)
    public PortableItemStack cladding;

    @Shadow(remap = false)
    public PortableItemStack material;

    @Shadow(remap = false)
    public int shape;

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

    /**
     * @author DragonFix
     * @reason Restore ArchitectureCraft shape data after real material requirements have been handled separately.
     */
    @Overwrite(remap = false)
    public boolean apply(IBlockApplyContext ctx) {
        TileEntity te = ctx.getTileEntity();
        if (!(te instanceof TileShape tileShape)) return false;

        IBlockState targetBase = dragonfix$toMaterialState(material);
        if (targetBase == null) return false;
        tileShape.baseBlockState = targetBase;

        Shape targetShape = Shape.forId(shape);
        if (targetShape == null) return false;

        if (!dragonfix$isSameSecondaryMaterial(tileShape.shape, tileShape.secondaryBlockState, targetShape, cladding)) {
            dragonfix$giveSecondaryMaterial(ctx, tileShape.shape, tileShape.secondaryBlockState);
            if (!dragonfix$consumeSecondaryMaterial(ctx, targetShape, cladding, true)) return false;
        }

        if (cladding == null) {
            tileShape.secondaryBlockState = null;
        } else {
            Block block = cladding.getBlock();
            if (block == null) return false;

            tileShape.secondaryBlockState = new MetaBlockState(block, cladding.getMeta());
        }

        tileShape.shape = targetShape;
        tileShape.setSide(dragonfix$side);
        tileShape.setTurn(dragonfix$turn);
        tileShape.setOffsetX(dragonfix$offsetX);
        tileShape.markChanged();
        return true;
    }

    /**
     * @author DragonFix
     * @reason ArchitectureCraft shape items and their cut-from base materials are free, but real secondary materials
     *         are not.
     */
    @Overwrite(remap = false)
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        if (context.getTileEntity() instanceof TileShape tileShape) {
            Shape targetShape = Shape.forId(shape);
            if (!dragonfix$isSameSecondaryMaterial(
                tileShape.shape,
                tileShape.secondaryBlockState,
                targetShape,
                cladding)) {
                dragonfix$giveSecondaryMaterial(context, tileShape.shape, tileShape.secondaryBlockState);
                return dragonfix$consumeSecondaryMaterial(context, targetShape, cladding, false);
            }

            return true;
        }

        return false;
    }

    /**
     * @author DragonFix
     * @reason ArchitectureCraft shape items and their cut-from base materials are free, but real secondary materials
     *         are not.
     */
    @Overwrite(remap = false)
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return dragonfix$consumeSecondaryMaterial(context, Shape.forId(shape), cladding, false);
    }

    @Unique
    private static boolean dragonfix$consumeSecondaryMaterial(IBlockApplyContext context, Shape shape,
        PortableItemStack portable, boolean warn) {
        ItemStack stack = dragonfix$getSecondaryMaterialStack(shape, portable);
        return stack == null || dragonfix$consumeMaterial(context, stack, warn);
    }

    @Unique
    private static boolean dragonfix$consumeMaterial(IBlockApplyContext context, ItemStack stack, boolean warn) {
        if (context.tryConsumeItems(stack)) return true;
        if (warn) context.warn("Could not find material: " + stack.getDisplayName());
        return false;
    }

    @Unique
    private static boolean dragonfix$isSameSecondaryMaterial(Shape existingShape, IBlockState existing,
        Shape expectedShape, PortableItemStack expected) {
        return dragonfix$isSameStack(
            dragonfix$getSecondaryMaterialStack(existingShape, existing),
            dragonfix$getSecondaryMaterialStack(expectedShape, expected));
    }

    @Unique
    private static boolean dragonfix$isSameStack(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage();
    }

    @Unique
    private static void dragonfix$giveSecondaryMaterial(IBlockApplyContext context, Shape shape, IBlockState state) {
        ItemStack stack = dragonfix$getSecondaryMaterialStack(shape, state);
        if (stack != null) context.givePlayerItems(stack);
    }

    @Unique
    private static ItemStack dragonfix$getSecondaryMaterialStack(Shape shape, PortableItemStack portable) {
        Block block = portable == null ? null : portable.getBlock();
        return block == null ? null
            : dragonfix$getSecondaryMaterialStack(shape, new MetaBlockState(block, portable.getMeta()));
    }

    @Unique
    private static ItemStack dragonfix$getSecondaryMaterialStack(Shape shape, IBlockState state) {
        if (shape == null || state == null) return null;
        if (shape.kind.acceptsCladding()) return null;

        ItemStack stack = shape.kind.newSecondaryMaterialStack(state);
        if (stack == null || stack.getItem() == ArchitectureCraft.content.itemCladding) return null;
        return stack;
    }

    @Unique
    private static IBlockState dragonfix$toMaterialState(PortableItemStack portable) {
        Block block = portable == null ? null : portable.getBlock();
        return block == null ? null : new MetaBlockState(block, portable.getMeta());
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
