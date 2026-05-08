package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.block.material.MaterialLiquid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorFluidSourceHelper;
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
     * @reason MatterManipulator 0.0.51 skips the ForgeMultipart container block before tile analysis can capture parts.
     */
    @SuppressWarnings("ConstantValue")
    @Overwrite(remap = false)
    public static boolean skipWhenCopying(Block block, int metadata) {
        return block.getMaterial() instanceof MaterialLiquid
            && !MatterManipulatorFluidSourceHelper.isSupportedFluid(block) || dragonfix$isGTRenderer(block)
            || InteropConstants.BRIGHT_AIR.matches(block, metadata)
            || InteropConstants.ARCANE_LAMP_LIGHT.matches(block, metadata);
    }

    /**
     * @author DragonFix
     * @reason Treat LittleTiles' container block as replaceable without a post-return injection path.
     */
    @Overwrite(remap = false)
    public static boolean isFree(Block block, int metadata) {
        return block == net.minecraft.init.Blocks.air || InteropConstants.FMP_BLOCK.matches(block, metadata)
            || InteropConstants.AE_BLOCK_CABLE.matches(block, metadata)
            || metadata == 0 && block == dragonfix$getLittleTilesBlock()
            || MatterManipulatorFluidSourceHelper.isSupportedFluid(block);
    }

    @Unique
    private static Block dragonfix$getLittleTilesBlock() {
        if (!dragonfix$lookedUpLittleTilesBlock) {
            dragonfix$littleTilesBlock = GameRegistry.findBlock("littletiles", "BlockLittleTiles");
            dragonfix$lookedUpLittleTilesBlock = true;
        }

        return dragonfix$littleTilesBlock;
    }

    @Invoker(value = "isGTRenderer", remap = false)
    private static boolean dragonfix$isGTRenderer(Block block) {
        throw new AssertionError();
    }
}
