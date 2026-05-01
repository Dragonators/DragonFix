package com.dragonfix.mixin.mixins.programmablehatches;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;

@Pseudo
@Mixin(targets = "reobf.proghatches.block.TileIOHub", remap = false)
public abstract class TileIOHubAnalyzableMixin implements Analyzable {

    @Shadow(remap = false)
    Map<String, Environment> subapi;

    @Override
    public Node[] onAnalyze(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        Environment api = subapi.get("all");
        return new Node[] { api == null ? null : api.node() };
    }
}
