package com.dragonfix.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;

import gcewing.architecture.client.render.ShapeRenderDispatch;
import gcewing.architecture.client.render.target.RenderTargetBase;
import gcewing.architecture.common.shape.Shape;
import gcewing.architecture.common.shape.Window;
import gcewing.architecture.common.tile.TileShape;
import gcewing.architecture.compat.BlockPos;
import gcewing.architecture.compat.Trans3;
import gcewing.architecture.compat.Vector3;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class ArchitectureCraftPreviewRenderer {

    private static final ShapeRenderer SHAPE_RENDERER = new ShapeRenderer();
    private static final Int2IntOpenHashMap SHAPE_QUAD_COUNTS = new Int2IntOpenHashMap();

    static {
        SHAPE_QUAD_COUNTS.defaultReturnValue(-1);
    }

    private ArchitectureCraftPreviewRenderer() {}

    public static void addHint(PendingBlock pendingBlock, int shapeId, Block materialBlock, int materialMeta, int side,
        int turn, double offsetX, short[] tint) {
        if (materialBlock == null) return;

        Shape shape = Shape.forId(shapeId);
        ArchitectureCraftHintRenderer renderer = new ArchitectureCraftHintRenderer(
            pendingBlock.x,
            pendingBlock.y,
            pendingBlock.z,
            shape,
            materialBlock,
            materialMeta,
            side,
            turn,
            offsetX,
            tint);

        DragonFixRenderHints.addCustomHint(
            pendingBlock.x,
            pendingBlock.y,
            pendingBlock.z,
            materialBlock,
            materialMeta,
            tint,
            renderer,
            renderer.quadCount());
    }

    private static void dragonfix$drawShape(Tessellator tessellator, int x, int y, int z, Shape shape,
        Block materialBlock, int materialMeta, int side, int turn, double offsetX, short[] tint, int eyeXint,
        int eyeYint, int eyeZint) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        TileShape tile = new TileShape(shape, materialBlock, materialMeta);
        tile.xCoord = x;
        tile.yCoord = y;
        tile.zCoord = z;
        tile.setWorldObj(world);
        tile.setSide(side);
        tile.setTurn(turn);
        tile.setOffsetX(offsetX);

        BlockPos renderPos = new BlockPos(x - eyeXint, y - eyeYint, z - eyeZint);
        Trans3 transform = Trans3.blockCenter(renderPos)
            .t(Trans3.sideTurn(side, turn))
            .translate(offsetX, 0, 0);

        SHAPE_RENDERER.dragonfix$renderShape(
            tile,
            new HintRenderTarget(x, y, z, renderPos, tessellator, tint, world),
            transform,
            true,
            false);
    }

    private static int dragonfix$getShapeQuadCount(int x, int y, int z, Shape shape, Block materialBlock,
        int materialMeta, int side, int turn, double offsetX) {
        if (!(shape.kind instanceof Window)) {
            synchronized (SHAPE_QUAD_COUNTS) {
                int cached = SHAPE_QUAD_COUNTS.get(shape.id);
                if (cached >= 6) {
                    return cached;
                }
            }
        }

        int quadCount = dragonfix$countShapeQuads(x, y, z, shape, materialBlock, materialMeta, side, turn, offsetX);
        if (quadCount < 6) {
            return 6;
        }

        if (!(shape.kind instanceof Window)) {
            synchronized (SHAPE_QUAD_COUNTS) {
                SHAPE_QUAD_COUNTS.put(shape.id, quadCount);
            }
        }

        return quadCount;
    }

    private static int dragonfix$countShapeQuads(int x, int y, int z, Shape shape, Block materialBlock,
        int materialMeta, int side, int turn, double offsetX) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return -1;

        TileShape tile = new TileShape(shape, materialBlock, materialMeta);
        tile.xCoord = x;
        tile.yCoord = y;
        tile.zCoord = z;
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
            return -1;
        }

        return Math.max(6, (target.vertexCount() + 3) / 4);
    }

    private static final class ArchitectureCraftHintRenderer implements DragonFixRenderHints.CustomRenderer {

        private final int x;
        private final int y;
        private final int z;
        private final Shape shape;
        private final Block materialBlock;
        private final int materialMeta;
        private final int side;
        private final int turn;
        private final double offsetX;
        private final short[] tint;
        private final int quadCount;

        private ArchitectureCraftHintRenderer(int x, int y, int z, Shape shape, Block materialBlock, int materialMeta,
            int side, int turn, double offsetX, short[] tint) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.shape = shape;
            this.materialBlock = materialBlock;
            this.materialMeta = materialMeta;
            this.side = side;
            this.turn = turn;
            this.offsetX = offsetX;
            this.tint = tint;
            quadCount = dragonfix$getShapeQuadCount(x, y, z, shape, materialBlock, materialMeta, side, turn, offsetX);
        }

        private int quadCount() {
            return quadCount;
        }

        @Override
        public void draw(Tessellator tessellator, double eyeX, double eyeY, double eyeZ, int eyeXint, int eyeYint,
            int eyeZint) {
            dragonfix$drawShape(
                tessellator,
                x,
                y,
                z,
                shape,
                materialBlock,
                materialMeta,
                side,
                turn,
                offsetX,
                tint,
                eyeXint,
                eyeYint,
                eyeZint);
        }
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
