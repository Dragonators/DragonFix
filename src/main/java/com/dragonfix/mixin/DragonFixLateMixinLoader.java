package com.dragonfix.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class DragonFixLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.dragonfix.late.json";
    }

    @Override
    @Nonnull
    public List<String> getMixins(Set<String> loadedMods) {
        return Collections.emptyList();
    }
}
