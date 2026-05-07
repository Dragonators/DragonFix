package com.dragonfix.mixin.mixins.mattermanipulator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;

@Mixin(value = BlockSpec.class, remap = false)
public interface BlockSpecAccessor {

    @Accessor("metadata")
    int dragonfix$getMetadata();
}
