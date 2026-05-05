package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.DragonFixRenderHints;

@Mixin(targets = "com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints$Hint", remap = false)
public abstract class RenderHintsHintMixin {

    @Shadow(remap = false)
    public int x;

    @Shadow(remap = false)
    public int y;

    @Shadow(remap = false)
    public int z;

    @Shadow(remap = false)
    public IIcon[] icons;

    @Shadow(remap = false)
    public short[] tint;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void dragonfix$drawSizedHint(Tessellator tes, double eyeX, double eyeY, double eyeZ, int eyeXint,
        int eyeYint, int eyeZint, CallbackInfo ci) {
        DragonFixRenderHints.CustomRenderer customRenderer = DragonFixRenderHints.getCustomRenderer(this);
        if (customRenderer != null) {
            customRenderer.draw(tes, eyeX, eyeY, eyeZ, eyeXint, eyeYint, eyeZint);
            ci.cancel();
            return;
        }

        DragonFixRenderHints.Bounds bounds = DragonFixRenderHints.getBounds(this);
        if (bounds == null) {
            return;
        }

        World world = Minecraft.getMinecraft().theWorld;
        int brightness = world.blockExists(x, 0, z) ? world.getLightBrightnessForSkyBlocks(x, y, z, 0) : 0;
        tes.setBrightness(brightness);
        tes.setColorRGBA(tint[0], tint[1], tint[2], 150);

        double x1 = (x - eyeXint) + bounds.minX;
        double y1 = (y - eyeYint) + bounds.minY;
        double z1 = (z - eyeZint) + bounds.minZ;
        double x2 = (x - eyeXint) + bounds.maxX;
        double y2 = (y - eyeYint) + bounds.maxY;
        double z2 = (z - eyeZint) + bounds.maxZ;
        double worldX1 = x + bounds.minX;
        double worldY1 = y + bounds.minY;
        double worldZ1 = z + bounds.minZ;
        double worldX2 = x + bounds.maxX;
        double worldY2 = y + bounds.maxY;
        double worldZ2 = z + bounds.maxZ;

        for (int pass = 0; pass < 2; pass++) {
            boolean unobstructedPass = pass == 1;
            for (int side = 0; side < 6; side++) {
                IIcon icon = icons[side];
                if (icon == null) continue;

                double u = icon.getMinU();
                double maxU = icon.getMaxU();
                double v = icon.getMinV();
                double maxV = icon.getMaxV();

                switch (side) {
                    case 0:
                        if ((worldY1 >= eyeY) != unobstructedPass) continue;
                        tes.setNormal(0, -1, 0);
                        tes.addVertexWithUV(x1, y1, z1, u, v);
                        tes.addVertexWithUV(x2, y1, z1, maxU, v);
                        tes.addVertexWithUV(x2, y1, z2, maxU, maxV);
                        tes.addVertexWithUV(x1, y1, z2, u, maxV);
                        break;
                    case 1:
                        if ((worldY2 <= eyeY) != unobstructedPass) continue;
                        tes.setNormal(0, 1, 0);
                        tes.addVertexWithUV(x1, y2, z1, u, v);
                        tes.addVertexWithUV(x1, y2, z2, u, maxV);
                        tes.addVertexWithUV(x2, y2, z2, maxU, maxV);
                        tes.addVertexWithUV(x2, y2, z1, maxU, v);
                        break;
                    case 2:
                        if ((worldZ1 >= eyeZ) != unobstructedPass) continue;
                        tes.setNormal(0, 0, -1);
                        tes.addVertexWithUV(x1, y1, z1, maxU, maxV);
                        tes.addVertexWithUV(x1, y2, z1, maxU, v);
                        tes.addVertexWithUV(x2, y2, z1, u, v);
                        tes.addVertexWithUV(x2, y1, z1, u, maxV);
                        break;
                    case 3:
                        if ((worldZ2 <= eyeZ) != unobstructedPass) continue;
                        tes.setNormal(0, 0, 1);
                        tes.addVertexWithUV(x2, y1, z2, maxU, maxV);
                        tes.addVertexWithUV(x2, y2, z2, maxU, v);
                        tes.addVertexWithUV(x1, y2, z2, u, v);
                        tes.addVertexWithUV(x1, y1, z2, u, maxV);
                        break;
                    case 4:
                        if ((worldX1 >= eyeX) != unobstructedPass) continue;
                        tes.setNormal(-1, 0, 0);
                        tes.addVertexWithUV(x1, y1, z2, maxU, maxV);
                        tes.addVertexWithUV(x1, y2, z2, maxU, v);
                        tes.addVertexWithUV(x1, y2, z1, u, v);
                        tes.addVertexWithUV(x1, y1, z1, u, maxV);
                        break;
                    case 5:
                        if ((worldX2 <= eyeX) != unobstructedPass) continue;
                        tes.setNormal(1, 0, 0);
                        tes.addVertexWithUV(x2, y1, z1, maxU, maxV);
                        tes.addVertexWithUV(x2, y2, z1, maxU, v);
                        tes.addVertexWithUV(x2, y2, z2, u, v);
                        tes.addVertexWithUV(x2, y1, z2, u, maxV);
                        break;
                    default:
                        break;
                }
            }
        }

        ci.cancel();
    }
}
