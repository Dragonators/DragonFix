package com.dragonfix.mixin.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Pseudo
@Mixin(targets = "reobf.proghatches.main.registration.PHRecipes")
public abstract class PHRecipesMixin {

    @ModifyArg(
        method = "run()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/Item;II)V"),
        slice = @Slice(
            from = @At(value = "CONSTANT", args = "stringValue=ae2fc"),
            to = @At(
                value = "FIELD",
                target = "Lreobf/proghatches/main/MyMod;iohub:Lnet/minecraft/item/Item;",
                remap = false)),
        index = 2,
        require = 1)
    private int dragonfix$useAe2FluidInterfaceMetaZero(int meta) {
        return 0;
    }
}
