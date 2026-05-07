package com.dragonfix.mattermanipulator.helper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.world.World;

import com.dragonfix.mattermanipulator.bridge.ProxiedWorldBridge;

public final class DoorPlacementHelper {

    private DoorPlacementHelper() {}

    public static boolean canPlaceBlockAt(Block block, World world, int x, int y, int z) {
        if (!(block instanceof BlockDoor) || !(world instanceof ProxiedWorldBridge bridge)) {
            return block.canPlaceBlockAt(world, x, y, z);
        }

        World realWorld = bridge.dragonfix$getWrappedWorld();
        if (y >= realWorld.getHeight() - 1) return false;
        if (!World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) return false;

        return canReplace(world, x, y, z) && canReplace(world, x, y + 1, z);
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        if (!(block instanceof BlockDoor) || (metadata & 8) != 0) {
            return world.setBlock(x, y, z, block, metadata, flags);
        }

        boolean lower = world.setBlock(x, y, z, block, metadata & 7, 2);
        boolean upper = world.setBlock(x, y + 1, z, block, 8, 2);

        world.notifyBlocksOfNeighborChange(x, y, z, block);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, block);

        return lower && upper;
    }

    private static boolean canReplace(World world, int x, int y, int z) {
        Block existing = world.getBlock(x, y, z);
        return existing.isAir(world, x, y, z) || existing.isReplaceable(world, x, y, z);
    }
}
