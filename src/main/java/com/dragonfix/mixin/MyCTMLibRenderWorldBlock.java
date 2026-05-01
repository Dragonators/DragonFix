package com.dragonfix.mixin;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.wohaopa.MyCTMLib.CTMIconManager;
import com.github.wohaopa.MyCTMLib.FastRandom;
import com.github.wohaopa.MyCTMLib.Textures;

public final class MyCTMLibRenderWorldBlock {

    private MyCTMLibRenderWorldBlock() {}

    public static boolean contains(IIcon icon) {
        return icon != null && Textures.contain(icon.getIconName());
    }

    public static boolean render(RenderBlocks renderBlocks, IBlockAccess blockAccess, Block block, double x, double y,
        double z, IIcon icon, ForgeDirection direction) {

        String iconName = normalizeIconName(icon.getIconName());
        int[] iconIndices = Textures.threadLocalIconIdx.get();

        CTMIconManager manager = Textures.ctmIconMap.get(iconName);
        if (manager == null) {
            return false;
        }
        if (!manager.hasInited()) {
            manager.init();
        }

        if (manager.detectionDiameter == CTMIconManager.DetectionDiameter.DIAMETER_1) {
            iconIndices[0] = 17;
            iconIndices[1] = 18;
            iconIndices[2] = 19;
            iconIndices[3] = 20;
        } else {
            Textures.buildConnect(blockAccess, (int) x, (int) y, (int) z, icon, direction, iconIndices);
        }

        if (Textures.ctmRandomMap.containsKey(iconName)) {
            List<CTMIconManager> randomManagers = Textures.ctmRandomMap.get(iconName);
            long worldSeed = blockAccess instanceof World ? ((World) blockAccess).getSeed() : 0L;
            int randomIndex = FastRandom
                .getRandomIndex(worldSeed, (int) x, (int) y, (int) z, randomManagers.size() + 1);
            if (randomIndex < randomManagers.size()) {
                manager = randomManagers.get(randomIndex);
            }
        }

        switch (direction) {
            case DOWN -> Textures.renderFaceYNeg(renderBlocks, x, y, z, manager, iconIndices);
            case UP -> Textures.renderFaceYPos(renderBlocks, x, y, z, manager, iconIndices);
            case NORTH -> Textures.renderFaceZNeg(renderBlocks, x, y, z, manager, iconIndices);
            case SOUTH -> Textures.renderFaceZPos(renderBlocks, x, y, z, manager, iconIndices);
            case WEST -> Textures.renderFaceXNeg(renderBlocks, x, y, z, manager, iconIndices);
            case EAST -> Textures.renderFaceXPos(renderBlocks, x, y, z, manager, iconIndices);
            default -> {
                return false;
            }
        }

        return true;
    }

    private static String normalizeIconName(String iconName) {
        int firstColon = iconName.indexOf(':');
        int secondColon = iconName.indexOf(':', firstColon + 1);
        if (secondColon == -1) {
            return iconName;
        }
        return iconName.substring(0, secondColon) + "&"
            + iconName.substring(secondColon + 1)
                .replace(":", "&");
    }
}
