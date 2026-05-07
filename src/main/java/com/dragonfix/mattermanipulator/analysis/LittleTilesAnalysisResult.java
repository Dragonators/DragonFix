package com.dragonfix.mattermanipulator.analysis;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.IBlockApplyContext;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

public class LittleTilesAnalysisResult implements ITileAnalysisIntegration {

    private static final int GRID_SIZE = 16;
    private static final int CENTER = GRID_SIZE / 2;

    private NBTTagCompound tileData;

    public static class RenderBox {

        public Block block;
        public int meta;
        public int minX;
        public int minY;
        public int minZ;
        public int maxX;
        public int maxY;
        public int maxZ;
    }

    public static LittleTilesAnalysisResult analyze(TileEntity te) {
        if (!(te instanceof TileEntityLittleTiles)) return null;

        LittleTilesAnalysisResult result = new LittleTilesAnalysisResult();
        result.tileData = new NBTTagCompound();
        te.writeToNBT(result.tileData);

        return result;
    }

    @Override
    public boolean apply(IBlockApplyContext ctx) {
        TileEntity te = ctx.getTileEntity();

        if (!(te instanceof TileEntityLittleTiles)) {
            ctx.error("LittleTiles tile entity is missing");
            return false;
        }

        if (tileData == null) return true;

        if (!ctx.tryApplyAction(Math.max(1, tileData.getInteger("tilesCount")))) return false;

        NBTTagCompound copy = copyTileData();
        copy.setInteger("x", ctx.getX());
        copy.setInteger("y", ctx.getY());
        copy.setInteger("z", ctx.getZ());

        try {
            te.readFromNBT(copy);
            ctx.getWorld()
                .markBlockForUpdate(ctx.getX(), ctx.getY(), ctx.getZ());
            te.markDirty();
        } catch (Exception e) {
            ctx.error("Could not apply LittleTiles data: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean getRequiredItemsForExistingBlock(IBlockApplyContext context) {
        return true;
    }

    @Override
    public boolean getRequiredItemsForNewBlock(IBlockApplyContext context) {
        return true;
    }

    @Override
    public void getItemTag(NBTTagCompound tag) {
        if (tileData != null) tag.setTag("MatterManipulatorLittleTiles", copyTileData());
    }

    @Override
    public void getItemDetails(List<String> details) {
        if (tileData != null) details.add(tileData.getInteger("tilesCount") + " LT tiles");
    }

    public void getRenderBoxes(List<RenderBox> out) {
        if (tileData == null) return;

        int count = tileData.getInteger("tilesCount");

        for (int i = 0; i < count; i++) {
            String tileKey = "t" + i;
            if (!tileData.hasKey(tileKey)) continue;

            NBTTagCompound tile = tileData.getCompoundTag(tileKey);
            Block block = Block.getBlockFromName(tile.getString("block"));
            if (block == null) continue;

            int meta = tile.getInteger("meta");
            int boxCount = tile.getInteger("bSize");

            for (int j = 0; j < boxCount; j++) {
                String boxKey = "bBox" + j;
                RenderBox box = new RenderBox();
                box.block = block;
                box.meta = meta;
                box.minX = tile.getInteger(boxKey + "minX");
                box.minY = tile.getInteger(boxKey + "minY");
                box.minZ = tile.getInteger(boxKey + "minZ");
                box.maxX = tile.getInteger(boxKey + "maxX");
                box.maxY = tile.getInteger(boxKey + "maxY");
                box.maxZ = tile.getInteger(boxKey + "maxZ");

                if (box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ) {
                    out.add(box);
                }
            }
        }
    }

    @Override
    public void transform(Transform transform) {
        if (tileData == null) return;

        int count = tileData.getInteger("tilesCount");

        for (int i = 0; i < count; i++) {
            String tileKey = "t" + i;
            if (!tileData.hasKey(tileKey)) continue;

            NBTTagCompound tile = tileData.getCompoundTag(tileKey);
            transformTile(tile, transform);
            tileData.setTag(tileKey, tile);
        }
    }

    private void transformTile(NBTTagCompound tile, Transform transform) {
        int boxCount = tile.getInteger("bSize");

        for (int i = 0; i < boxCount; i++) {
            transformBox(tile, "bBox" + i, transform);
        }

        if (boxCount > 0) {
            tile.setInteger("cVecx", tile.getInteger("bBox0minX"));
            tile.setInteger("cVecy", tile.getInteger("bBox0minY"));
            tile.setInteger("cVecz", tile.getInteger("bBox0minZ"));
        }
    }

    private void transformBox(NBTTagCompound tag, String name, Transform transform) {
        int minX = tag.getInteger(name + "minX");
        int minY = tag.getInteger(name + "minY");
        int minZ = tag.getInteger(name + "minZ");
        int maxX = tag.getInteger(name + "maxX");
        int maxY = tag.getInteger(name + "maxY");
        int maxZ = tag.getInteger(name + "maxZ");

        int outMinX = Integer.MAX_VALUE;
        int outMinY = Integer.MAX_VALUE;
        int outMinZ = Integer.MAX_VALUE;
        int outMaxX = Integer.MIN_VALUE;
        int outMaxY = Integer.MIN_VALUE;
        int outMaxZ = Integer.MIN_VALUE;

        Vector3i point = new Vector3i();

        for (int corner = 0; corner < 8; corner++) {
            point.set(
                ((corner & 1) == 0 ? minX : maxX) - CENTER,
                ((corner & 2) == 0 ? minY : maxY) - CENTER,
                ((corner & 4) == 0 ? minZ : maxZ) - CENTER);
            transform.apply(point);
            point.add(CENTER, CENTER, CENTER);

            outMinX = Math.min(outMinX, point.x);
            outMinY = Math.min(outMinY, point.y);
            outMinZ = Math.min(outMinZ, point.z);
            outMaxX = Math.max(outMaxX, point.x);
            outMaxY = Math.max(outMaxY, point.y);
            outMaxZ = Math.max(outMaxZ, point.z);
        }

        tag.setInteger(name + "minX", outMinX);
        tag.setInteger(name + "minY", outMinY);
        tag.setInteger(name + "minZ", outMinZ);
        tag.setInteger(name + "maxX", outMaxX);
        tag.setInteger(name + "maxY", outMaxY);
        tag.setInteger(name + "maxZ", outMaxZ);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public LittleTilesAnalysisResult clone() {
        LittleTilesAnalysisResult dup = new LittleTilesAnalysisResult();
        dup.tileData = copyTileData();
        return dup;
    }

    private NBTTagCompound copyTileData() {
        return tileData == null ? null : (NBTTagCompound) tileData.copy();
    }

    @Override
    public void migrate() {}

    @Override
    public int hashCode() {
        return tileData == null ? 0 : tileData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LittleTilesAnalysisResult other)) return false;

        if (tileData == null) return other.tileData == null;
        return tileData.equals(other.tileData);
    }

    @Override
    public String toString() {
        return "LittleTilesAnalysisResult [tileData=" + tileData + "]";
    }
}
