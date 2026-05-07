package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.dragonfix.mattermanipulator.bridge.ProxiedWorldBridge;
import com.recursive_pineapple.matter_manipulator.common.building.ProxiedWorld;

@Mixin(value = ProxiedWorld.class, remap = false)
public abstract class ProxiedWorldAccessorMixin implements ProxiedWorldBridge {

    @Shadow(remap = false)
    @Final
    private World world;

    @Override
    public World dragonfix$getWrappedWorld() {
        return world;
    }
}
