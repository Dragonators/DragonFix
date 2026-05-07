package com.dragonfix.mattermanipulator.helper;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;

public final class MatterManipulatorPlacementHelper {

    private MatterManipulatorPlacementHelper() {}

    public static boolean canPlaceBlockAt(PendingBlock pending, Block block, World world, int x, int y, int z) {
        int metadata = pending == null || pending.spec == null ? world.getBlockMetadata(x, y, z)
            : pending.spec.getBlockMeta();

        if (BiomesOPlentyPlacementHelper.isWorldDecor(block)) {
            return BiomesOPlentyPlacementHelper.canPlaceWorldDecor(block, world, x, y, z, metadata);
        }
        if (DoorPlacementHelper.isDoor(block)) {
            return DoorPlacementHelper.canPlaceDoorAt(block, world, x, y, z);
        }

        return block.canPlaceBlockAt(world, x, y, z);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        if (DoorPlacementHelper.isDoor(block) && (metadata & 8) == 0) {
            return DoorPlacementHelper.setBlock(world, x, y, z, block, metadata, flags);
        }

        return world.setBlock(x, y, z, block, metadata, flags);
    }
}
