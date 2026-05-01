package com.dragonfix.mixin.mixins.galaxyspace;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.FMLCommonHandler;
import galaxyspace.core.item.ItemRocket;

@Mixin(ItemRocket.class)
public abstract class ItemRocketMixin {

    @Inject(
        method = "getRarity(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/EnumRarity;",
        at = @At("HEAD"),
        cancellable = true)
    private void dragonfix$useSafeServerRarity(ItemStack stack, CallbackInfoReturnable<EnumRarity> cir) {
        if (FMLCommonHandler.instance()
            .getSide()
            .isServer()) {
            cir.setReturnValue(EnumRarity.common);
        }
    }
}
