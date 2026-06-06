package com.dragonfix.mixin.mixins.angelica;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.ctm.TileOverride;
import com.prupe.mcpatcher.mal.resource.PropertiesFile;

@Mixin(value = TileOverride.class, remap = false)
public abstract class TileOverrideMatchTilesMixin {

    @Shadow
    @Final
    private PropertiesFile properties;

    @Inject(method = "getTileList", at = @At("RETURN"), remap = false)
    private void dragonfix$addExactPathfulMatchTileKeys(String key, CallbackInfoReturnable<Set<String>> cir) {
        Set<String> result = cir.getReturnValue();
        String property = properties.getString(key, "");
        for (String token : property.split("\\s+")) {
            if (!token.isEmpty() && token.contains("/")) {
                result.add(dragonfix$stripPngSuffix(token));
            }
        }
    }

    private static String dragonfix$stripPngSuffix(String tile) {
        return tile.endsWith(".png") ? tile.substring(0, tile.length() - 4) : tile;
    }
}
