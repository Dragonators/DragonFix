package com.dragonfix.mixin.mixins.angelica;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.render.EmissiveTextureAutoloader;

@Mixin(value = EmissiveTextureAutoloader.class, remap = false)
public abstract class EmissiveTextureAutoloaderMixin {

    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$skipInvalidEmissiveResourcePath(String name, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation location = new ResourceLocation(name);

        try {
            Paths.get("textures/blocks/" + location.getResourcePath() + ".png");
        } catch (InvalidPathException ignored) {
            cir.setReturnValue(false);
        }
    }
}
