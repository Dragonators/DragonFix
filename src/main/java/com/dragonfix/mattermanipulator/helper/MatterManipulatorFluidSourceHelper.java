package com.dragonfix.mattermanipulator.helper;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;

import gregtech.api.util.GTUtility;

public final class MatterManipulatorFluidSourceHelper {

    private MatterManipulatorFluidSourceHelper() {}

    public static boolean isSupportedFluid(Block block) {
        return block == Blocks.water || block == Blocks.lava;
    }

    public static boolean isSupportedFluid(PendingBlock block) {
        return block != null && block.spec != null && isSupportedFluid(block.getBlock());
    }

    public static boolean isSupportedSource(Block block, int metadata) {
        return metadata == 0 && isSupportedFluid(block);
    }

    public static boolean isSupportedSource(PendingBlock block) {
        return block != null && block.spec != null && isSupportedSource(block.getBlock(), block.spec.getBlockMeta());
    }

    public static boolean isDisplayOnlyFlow(PendingBlock block) {
        return isSupportedFluid(block) && !isSupportedSource(block);
    }

    public static boolean isSameSourceKind(PendingBlock left, PendingBlock right) {
        if (!isSupportedSource(left) || !isSupportedSource(right)) return false;
        return left.getBlock() == right.getBlock();
    }

    public static Fluid getFluid(PendingBlock block) {
        if (!isSupportedSource(block)) return null;
        return block.getBlock() == Blocks.water ? FluidRegistry.WATER : FluidRegistry.LAVA;
    }

    public static FluidStack getFluidStack(PendingBlock block, int amount) {
        Fluid fluid = getFluid(block);
        return fluid == null ? null : new FluidStack(fluid, amount);
    }

    public static ItemStack getBucket(Fluid fluid, int amount) {
        if (fluid == FluidRegistry.WATER) return new ItemStack(Items.water_bucket, amount);
        if (fluid == FluidRegistry.LAVA) return new ItemStack(Items.lava_bucket, amount);
        return null;
    }

    public static ItemStack getEmptyBucket(int amount) {
        return new ItemStack(Items.bucket, amount);
    }

    public static ItemStack getFluidDisplayStack(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) return null;

        return GTUtility.getFluidDisplayStack(fluid, true);
    }

    public static FluidStack getFluidFromDisplayStack(ItemStack stack) {
        return stack == null ? null : GTUtility.getFluidFromDisplayStack(stack);
    }
}
