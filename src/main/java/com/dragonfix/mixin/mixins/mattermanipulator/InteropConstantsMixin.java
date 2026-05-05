package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.recursive_pineapple.matter_manipulator.common.building.InteropConstants;

import cpw.mods.fml.common.registry.GameRegistry;

@Mixin(value = InteropConstants.class, remap = false)
public abstract class InteropConstantsMixin {

    @Unique
    private static Block dragonfix$littleTilesBlock;

    @Unique
    private static boolean dragonfix$lookedUpLittleTilesBlock;

    @Inject(method = "isFree", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dragonfix$treatLittleTilesAsFree(Block block, int metadata,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && metadata == 0 && block == dragonfix$getLittleTilesBlock()) {
            cir.setReturnValue(true);
        }
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
