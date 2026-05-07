package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.Deque;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorPlacementHelper;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;

@Mixin(value = PendingBuild.class, remap = false)
public abstract class PendingBuildMixin {

    @Shadow(remap = false)
    @Final
    private Deque<PendingBlock> pendingBlocks;

    @Redirect(
        method = "tryPlaceBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;canPlaceBlockAt(Lnet/minecraft/world/World;III)Z",
            remap = true),
        remap = false)
    private boolean dragonfix$canPlaceDoor(Block block, World world, int x, int y, int z) {
        return MatterManipulatorPlacementHelper.canPlaceBlockAt(pendingBlocks.peekFirst(), block, world, x, y, z);
    }

    @Redirect(
        method = "tryPlaceBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z",
            remap = true),
        remap = false)
    private boolean dragonfix$placeDoor(World world, int x, int y, int z, Block block, int metadata, int flags) {
        return MatterManipulatorPlacementHelper.setBlock(world, x, y, z, block, metadata, flags);
    }
}
