package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;

@Mixin(value = RenderHints.class, remap = false)
public interface RenderHintsAccessor {

    @Accessor("HINTS")
    static ArrayList<Object> dragonfix$getHints() {
        throw new AssertionError();
    }
}
