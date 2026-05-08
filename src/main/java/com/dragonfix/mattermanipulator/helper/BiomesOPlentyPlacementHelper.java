package com.dragonfix.mattermanipulator.helper;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.dragonfix.mattermanipulator.bridge.ProxiedWorldBridge;

import biomesoplenty.common.blocks.BlockBOPFlower;
import biomesoplenty.common.blocks.BlockBOPFoliage;
import biomesoplenty.common.blocks.BlockBOPLilypad;
import biomesoplenty.common.blocks.BlockBOPPlant;
import biomesoplenty.common.blocks.templates.BOPBlockWorldDecor;

public final class BiomesOPlentyPlacementHelper {

    private BiomesOPlentyPlacementHelper() {}

    public static boolean isWorldDecor(Block block) {
        return block instanceof BOPBlockWorldDecor;
    }

    public static boolean shouldPlaceDirectly(Block block, int metadata) {
        return block instanceof BlockBOPLilypad || block instanceof BlockBOPFoliage && metadata == 0
            || block instanceof BlockBOPPlant && metadata == 14;
    }

    public static boolean setBlockDirectly(World world, int x, int y, int z, Block block, int metadata) {
        return world.setBlock(x, y, z, block, metadata, 2);
    }

    public static boolean isGeneratedUpperHalf(Block block, int metadata) {
        return block instanceof BlockBOPFlower && metadata == 14 || block instanceof BlockBOPPlant && metadata == 6
            || block instanceof BlockBOPFoliage && metadata == 6;
    }

    public static boolean canPlaceWorldDecor(Block block, World world, int x, int y, int z, int metadata) {
        World validationWorld = world instanceof ProxiedWorldBridge bridge ? bridge.dragonfix$getWrappedWorld() : world;
        if (y <= 0 || y >= validationWorld.getHeight()) return false;
        if (!canReplace(world, x, y, z)) return false;

        // BOP world decor checks read World.provider; Matter Manipulator's ProxiedWorld leaves it null.
        return ((BOPBlockWorldDecor) block).isValidPosition(validationWorld, x, y, z, metadata);
    }

    private static boolean canReplace(World world, int x, int y, int z) {
        Block existing = world.getBlock(x, y, z);
        return existing.isAir(world, x, y, z) || existing.isReplaceable(world, x, y, z);
    }
}
