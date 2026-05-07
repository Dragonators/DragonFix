package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.dragonfix.mattermanipulator.DragonFixRenderHints;
import com.dragonfix.mattermanipulator.bridge.RenderHintsHintBridge;

@Mixin(targets = "com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints$Hint", remap = false)
public abstract class RenderHintsHintMixin implements RenderHintsHintBridge {

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

    @Unique
    private DragonFixRenderHints.Bounds dragonfix$bounds;

    @Unique
    private DragonFixRenderHints.CustomRenderer dragonfix$customRenderer;

    @Unique
    private int dragonfix$quadCount = 6;

    /**
     * @author DragonFix
     * @reason Support custom and non-half-block hint rendering without routing through the original cube path first.
     */
    @Overwrite(remap = false)
    public void draw(Tessellator tes, double eyeX, double eyeY, double eyeZ, int eyeXint, int eyeYint, int eyeZint) {
        if (dragonfix$customRenderer != null) {
            dragonfix$customRenderer.draw(tes, eyeX, eyeY, eyeZ, eyeXint, eyeYint, eyeZint);
            return;
        }

        DragonFixRenderHints.Bounds bounds = dragonfix$bounds;
        double minX = bounds == null ? 0.25 : bounds.minX;
        double minY = bounds == null ? 0.25 : bounds.minY;
        double minZ = bounds == null ? 0.25 : bounds.minZ;
        double maxX = bounds == null ? 0.75 : bounds.maxX;
        double maxY = bounds == null ? 0.75 : bounds.maxY;
        double maxZ = bounds == null ? 0.75 : bounds.maxZ;

        World world = Minecraft.getMinecraft().theWorld;
        int brightness = world.blockExists(x, 0, z) ? world.getLightBrightnessForSkyBlocks(x, y, z, 0) : 0;
        tes.setBrightness(brightness);
        tes.setColorRGBA(tint[0], tint[1], tint[2], 150);

        double x1 = (x - eyeXint) + minX;
        double y1 = (y - eyeYint) + minY;
        double z1 = (z - eyeZint) + minZ;
        double x2 = (x - eyeXint) + maxX;
        double y2 = (y - eyeYint) + maxY;
        double z2 = (z - eyeZint) + maxZ;
        double worldX1 = x + minX;
        double worldY1 = y + minY;
        double worldZ1 = z + minZ;
        double worldX2 = x + maxX;
        double worldY2 = y + maxY;
        double worldZ2 = z + maxZ;

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

    }

    @Override
    public void dragonfix$setBounds(DragonFixRenderHints.Bounds bounds) {
        dragonfix$bounds = bounds;
    }

    @Override
    public void dragonfix$setCustomRenderer(DragonFixRenderHints.CustomRenderer renderer) {
        dragonfix$customRenderer = renderer;
    }

    @Override
    public int dragonfix$getQuadCount() {
        return dragonfix$quadCount;
    }

    @Override
    public void dragonfix$setQuadCount(int quadCount) {
        dragonfix$quadCount = Math.max(6, quadCount);
    }
}
