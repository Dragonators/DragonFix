package com.dragonfix.mixin.mixins.programmablehatches;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import reobf.proghatches.main.registration.PHRecipes;

@Pseudo
@Mixin(PHRecipes.class)
public abstract class PHRecipesMixin {

    @ModifyArg(
        method = "run()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/Item;II)V"),
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=ae2fc"),
            to = @At(
                value = "INVOKE",
                target = "Lgregtech/api/util/GTRecipeBuilder;itemInputs([Ljava/lang/Object;)Lgregtech/api/util/GTRecipeBuilder;",
                remap = false)),
        index = 2,
        require = 0,
        remap = false)
    private int dragonfix$useAe2FluidInterfaceMetaZero(int meta) {
        return 0;
    }
}
