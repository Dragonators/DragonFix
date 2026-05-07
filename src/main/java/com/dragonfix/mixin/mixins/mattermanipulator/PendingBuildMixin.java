package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dragonfix.mattermanipulator.helper.DoorPlacementHelper;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;

@Mixin(value = PendingBuild.class, remap = false)
public abstract class PendingBuildMixin {

    @Redirect(
        method = "tryPlaceBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;canPlaceBlockAt(Lnet/minecraft/world/World;III)Z",
            remap = true),
        remap = false)
    private boolean dragonfix$canPlaceDoor(Block block, World world, int x, int y, int z) {
        return DoorPlacementHelper.canPlaceBlockAt(block, world, x, y, z);
    }

    @Redirect(
        method = "tryPlaceBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z",
            remap = true),
        remap = false)
    private boolean dragonfix$placeDoor(World world, int x, int y, int z, Block block, int metadata, int flags) {
        return DoorPlacementHelper.setBlock(world, x, y, z, block, metadata, flags);
    }
}
