package com.dragonfix.mattermanipulator;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

/**
 * Rotates ForgeMultipart side, microblock shape, and orientation NBT for MatterManipulator transforms.
 *
 * <p>
 * Adapted from GTNewHorizons/MatterManipulator PR #34 by Luca-Guettinger and RecursivePineapple.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/34">MatterManipulator PR #34</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/9d76ed6e8ec87da8f55404893ea3b5ebe6912759">MatterManipulator
 *      commit 9d76ed6e</a>
 */
public final class FMPPartTransforms {

    private FMPPartTransforms() {}

    public static void transformSide(NBTTagCompound nbt, Transform transform) {
        if (!nbt.hasKey("side")) return;

        int side = nbt.getByte("side") & 0xFF;
        if (side >= 6) return;

        nbt.setByte("side", (byte) rotateFace(side, transform));
    }

    public static void transformMicroblockShape(String typeId, NBTTagCompound nbt, Transform transform) {
        if (typeId == null || !nbt.hasKey("shape")) return;

        int shape = nbt.getByte("shape") & 0xFF;
        int slot = shape & 0x0F;
        int sizeBits = shape & 0xF0;

        int newSlot;
        switch (typeId) {
            case "mcr_face":
            case "mcr_hllw":
                newSlot = transformFaceSlot(slot, transform);
                break;
            case "mcr_edge":
                newSlot = transformEdgeSlot(slot, transform);
                break;
            case "mcr_cnr":
                newSlot = transformCornerSlot(slot, transform);
                break;
            default:
                newSlot = -1;
                break;
        }

        if (newSlot >= 0) {
            nbt.setByte("shape", (byte) (sizeBits | newSlot));
        }
    }

    private static int rotateFace(int faceOrdinal, Transform transform) {
        return transform.apply(ForgeDirection.getOrientation(faceOrdinal))
            .ordinal();
    }

    private static int transformFaceSlot(int slot, Transform transform) {
        return slot < 6 ? rotateFace(slot, transform) : slot;
    }

    private static final int[][] EDGE_FACES = { { 2, 4 }, { 3, 4 }, { 2, 5 }, { 3, 5 }, { 0, 4 }, { 0, 5 }, { 1, 4 },
        { 1, 5 }, { 0, 2 }, { 1, 2 }, { 0, 3 }, { 1, 3 }, };

    private static int transformEdgeSlot(int edgeSlot, Transform transform) {
        if (edgeSlot < 0 || edgeSlot >= EDGE_FACES.length) return edgeSlot;

        int f1 = rotateFace(EDGE_FACES[edgeSlot][0], transform);
        int f2 = rotateFace(EDGE_FACES[edgeSlot][1], transform);

        return findEdge(Math.min(f1, f2), Math.max(f1, f2));
    }

    private static int findEdge(int face1, int face2) {
        for (int i = 0; i < EDGE_FACES.length; i++) {
            if (EDGE_FACES[i][0] == face1 && EDGE_FACES[i][1] == face2) return i;
        }
        return -1;
    }

    private static final int[][] CORNER_FACES = { { 0, 2, 4 }, { 1, 2, 4 }, { 0, 3, 4 }, { 1, 3, 4 }, { 0, 2, 5 },
        { 1, 2, 5 }, { 0, 3, 5 }, { 1, 3, 5 }, };

    private static int transformCornerSlot(int cornerSlot, Transform transform) {
        if (cornerSlot < 0 || cornerSlot >= CORNER_FACES.length) return cornerSlot;

        int[] rotated = { rotateFace(CORNER_FACES[cornerSlot][0], transform),
            rotateFace(CORNER_FACES[cornerSlot][1], transform), rotateFace(CORNER_FACES[cornerSlot][2], transform), };
        Arrays.sort(rotated);

        return findCorner(rotated);
    }

    private static int findCorner(int[] sortedFaces) {
        for (int i = 0; i < CORNER_FACES.length; i++) {
            if (CORNER_FACES[i][0] == sortedFaces[0] && CORNER_FACES[i][1] == sortedFaces[1]
                && CORNER_FACES[i][2] == sortedFaces[2]) return i;
        }
        return -1;
    }

    private static final int[] SIDE_ROT_MAP = { 3, 4, 2, 5, 3, 5, 2, 4, 1, 5, 0, 4, 1, 4, 0, 5, 1, 2, 0, 3, 1, 3, 0,
        2, };

    public static void transformOrient(NBTTagCompound nbt, Transform transform) {
        if (!nbt.hasKey("orient")) return;

        int orient = nbt.getByte("orient") & 0xFF;
        int oldSide = orient >> 2;
        int oldRot = orient & 0x3;

        if (oldSide >= 6) return;

        int newSide = rotateFace(oldSide, transform);
        int frontDir = SIDE_ROT_MAP[oldSide << 2 | oldRot];
        int newFrontDir = rotateFace(frontDir, transform);

        int newRot = 0;
        for (int r = 0; r < 4; r++) {
            if (SIDE_ROT_MAP[newSide << 2 | r] == newFrontDir) {
                newRot = r;
                break;
            }
        }

        nbt.setByte("orient", (byte) ((newSide << 2) | newRot));
    }

    public static void clearConnMap(NBTTagCompound nbt) {
        if (nbt.hasKey("connMap")) {
            nbt.setInteger("connMap", 0);
        }
    }
}
