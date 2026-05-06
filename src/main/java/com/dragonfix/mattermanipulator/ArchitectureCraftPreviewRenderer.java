package com.dragonfix.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;

import gcewing.architecture.client.render.ShapeRenderDispatch;
import gcewing.architecture.client.render.target.RenderTargetBase;
import gcewing.architecture.common.shape.Shape;
import gcewing.architecture.common.tile.TileShape;
import gcewing.architecture.compat.BlockPos;
import gcewing.architecture.compat.Trans3;
import gcewing.architecture.compat.Vector3;

public final class ArchitectureCraftPreviewRenderer {

    private static final ShapeRenderer SHAPE_RENDERER = new ShapeRenderer();

    private ArchitectureCraftPreviewRenderer() {}

    public static void addHint(PendingBlock pendingBlock, int shapeId, Block materialBlock, int materialMeta, int side,
        int turn, double offsetX, short[] tint) {
        if (materialBlock == null) return;

        DragonFixRenderHints.addCustomHint(
            pendingBlock.x,
            pendingBlock.y,
            pendingBlock.z,
            materialBlock,
            materialMeta,
            tint,
            (tessellator, eyeX, eyeY, eyeZ, eyeXint, eyeYint, eyeZint) -> dragonfix$drawShape(
                tessellator,
                pendingBlock,
                shapeId,
                materialBlock,
                materialMeta,
                side,
                turn,
                offsetX,
                tint,
                eyeXint,
                eyeYint,
                eyeZint),
            dragonfix$countShapeQuads(pendingBlock, shapeId, materialBlock, materialMeta, side, turn, offsetX));
    }

    private static void dragonfix$drawShape(Tessellator tessellator, PendingBlock pendingBlock, int shapeId,
        Block materialBlock, int materialMeta, int side, int turn, double offsetX, short[] tint, int eyeXint,
        int eyeYint, int eyeZint) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        TileShape tile = new TileShape(Shape.forId(shapeId), materialBlock, materialMeta);
        tile.xCoord = pendingBlock.x;
        tile.yCoord = pendingBlock.y;
        tile.zCoord = pendingBlock.z;
        tile.setWorldObj(world);
        tile.setSide(side);
        tile.setTurn(turn);
        tile.setOffsetX(offsetX);

        BlockPos renderPos = new BlockPos(pendingBlock.x - eyeXint, pendingBlock.y - eyeYint, pendingBlock.z - eyeZint);
        Trans3 transform = Trans3.blockCenter(renderPos)
            .t(Trans3.sideTurn(side, turn))
            .translate(offsetX, 0, 0);

        SHAPE_RENDERER.dragonfix$renderShape(
            tile,
            new HintRenderTarget(pendingBlock.x, pendingBlock.y, pendingBlock.z, renderPos, tessellator, tint, world),
            transform,
            true,
            false);
    }

    private static int dragonfix$countShapeQuads(PendingBlock pendingBlock, int shapeId, Block materialBlock,
        int materialMeta, int side, int turn, double offsetX) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return 6;

        TileShape tile = new TileShape(Shape.forId(shapeId), materialBlock, materialMeta);
        tile.xCoord = pendingBlock.x;
        tile.yCoord = pendingBlock.y;
        tile.zCoord = pendingBlock.z;
        tile.setWorldObj(world);
        tile.setSide(side);
        tile.setTurn(turn);
        tile.setOffsetX(offsetX);

        BlockPos renderPos = new BlockPos(0, 0, 0);
        Trans3 transform = Trans3.blockCenter(renderPos)
            .t(Trans3.sideTurn(side, turn))
            .translate(offsetX, 0, 0);
        CountingRenderTarget target = new CountingRenderTarget(renderPos);

        try {
            SHAPE_RENDERER.dragonfix$renderShape(tile, target, transform, true, false);
        } catch (RuntimeException | LinkageError e) {
            return 6;
        }

        return Math.max(6, (target.vertexCount() + 3) / 4);
    }

    private static final class ShapeRenderer extends ShapeRenderDispatch {

        private void dragonfix$renderShape(TileShape tile, RenderTargetBase target, Trans3 transform,
            boolean renderBase, boolean renderSecondary) {
            renderShapeTE(tile, target, transform, renderBase, renderSecondary);
        }
    }

    private static final class HintRenderTarget extends RenderTargetBase {

        private final Tessellator tessellator;
        private final short[] tint;
        private final int brightness;

        private HintRenderTarget(int worldX, int worldY, int worldZ, BlockPos renderPos, Tessellator tessellator,
            short[] tint, World world) {
            super(renderPos.x, renderPos.y, renderPos.z, null);
            this.tessellator = tessellator;
            this.tint = tint;
            brightness = world.blockExists(worldX, 0, worldZ)
                ? world.getLightBrightnessForSkyBlocks(worldX, worldY, worldZ, 0)
                : 0;
            expandTrianglesToQuads = true;
        }

        @Override
        protected void rawAddVertex(Vector3 p, double u, double v) {
            tessellator.setColorRGBA(tint[0], tint[1], tint[2], 150);
            tessellator.setTextureUV(u, v);
            tessellator.setBrightness(brightness);
            tessellator.addVertex(p.x, p.y, p.z);
        }
    }

    private static final class CountingRenderTarget extends RenderTargetBase {

        private int vertices;

        private CountingRenderTarget(BlockPos renderPos) {
            super(renderPos.x, renderPos.y, renderPos.z, null);
            expandTrianglesToQuads = true;
        }

        private int vertexCount() {
            return vertices;
        }

        @Override
        protected void rawAddVertex(Vector3 p, double u, double v) {
            vertices++;
        }
    }
}
