package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import com.recursive_pineapple.matter_manipulator.common.building.InteropConstants;

import cpw.mods.fml.common.registry.GameRegistry;

@Mixin(value = InteropConstants.class, remap = false)
public abstract class InteropConstantsMixin {

    @Unique
    private static Block dragonfix$littleTilesBlock;

    @Unique
    private static boolean dragonfix$lookedUpLittleTilesBlock;

    /**
     * @author DragonFix
     * @reason Treat LittleTiles' container block as replaceable without a post-return injection path.
     */
    @Overwrite(remap = false)
    public static boolean isFree(Block block, int metadata) {
        return block == net.minecraft.init.Blocks.air || InteropConstants.FMP_BLOCK.matches(block, metadata)
            || InteropConstants.AE_BLOCK_CABLE.matches(block, metadata)
            || metadata == 0 && block == dragonfix$getLittleTilesBlock();
    }

    @Unique
    private static Block dragonfix$getLittleTilesBlock() {
        if (!dragonfix$lookedUpLittleTilesBlock) {
            dragonfix$littleTilesBlock = GameRegistry.findBlock("littletiles", "BlockLittleTiles");
            dragonfix$lookedUpLittleTilesBlock = true;
        }

        return dragonfix$littleTilesBlock;
    }
}
