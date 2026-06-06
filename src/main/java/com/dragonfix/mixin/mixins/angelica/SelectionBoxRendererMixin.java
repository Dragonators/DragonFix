package com.dragonfix.mixin.mixins.angelica;

import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.render.SelectionBoxRenderer;

@Mixin(value = SelectionBoxRenderer.class, remap = false)
public abstract class SelectionBoxRendererMixin {

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$drawShaderOutlineThroughVanillaLines(AxisAlignedBB aabb, int color, CallbackInfo ci) {
        if (!IrisApi.getInstance()
            .isShaderPackInUse()) {
            return;
        }

        dragonfix$drawOutlinedBoundingBox(aabb, color);
        ci.cancel();
    }

    private static void dragonfix$drawOutlinedBoundingBox(AxisAlignedBB aabb, int color) {
        Tessellator tessellator = Tessellator.instance;

        tessellator.startDrawing(3);
        dragonfix$setColor(tessellator, color);
        tessellator.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        tessellator.draw();

        tessellator.startDrawing(3);
        dragonfix$setColor(tessellator, color);
        tessellator.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        tessellator.draw();

        tessellator.startDrawing(1);
        dragonfix$setColor(tessellator, color);
        tessellator.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        tessellator.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
        tessellator.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
        tessellator.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
        tessellator.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
        tessellator.draw();
    }

    private static void dragonfix$setColor(Tessellator tessellator, int color) {
        if (color != -1) {
            tessellator.setColorOpaque_I(color);
        }
    }
}
