package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.world.World;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.dragonfix.mattermanipulator.analysis.DoorAnalysisResult;
import com.dragonfix.mattermanipulator.bridge.PendingBlockDoorBridge;
import com.dragonfix.mattermanipulator.helper.BiomesOPlentyPlacementHelper;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorFluidSourceHelper;
import com.recursive_pineapple.matter_manipulator.GlobalMMConfig.DebugConfig;
import com.recursive_pineapple.matter_manipulator.MMMod;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(value = BlockAnalyzer.class, remap = false)
public abstract class BlockAnalyzerMixin {

    /**
     * Door upper halves are not independently placeable blocks. MM normally serializes them as air because they do not
     * drop an item, which later deletes the upper half created by the lower door placement.
     *
     * @author DragonFix
     * @reason The original method has no per-block extension point between BlockSpec creation and list insertion.
     */
    @Overwrite(remap = false)
    public static BlockAnalyzer.RegionAnalysis analyzeRegion(World world, Location a, Location b, boolean checkTiles) {
        if (a == null || b == null || world.provider.dimensionId != a.worldId || a.worldId != b.worldId) return null;

        long pre = System.nanoTime();

        BlockAnalyzer.RegionAnalysis analysis = new BlockAnalyzer.RegionAnalysis();

        Vector3i deltas = MMUtils.getRegionDeltas(a, b);
        analysis.deltas = deltas;

        analysis.blocks = new ObjectArrayList<>();

        for (Vector3i voxel : MMUtils.getBlocksInBB(a, deltas)) {
            Block block = world.getBlock(voxel.x, voxel.y, voxel.z);
            int meta = world.getBlockMetadata(voxel.x, voxel.y, voxel.z);
            if (block instanceof BlockDoor && (meta & 8) != 0) continue;
            if (BiomesOPlentyPlacementHelper.isGeneratedUpperHalf(block, meta)) continue;

            BlockSpec spec = BlockSpec.fromBlock(null, world, voxel.x, voxel.y, voxel.z);
            if (MatterManipulatorFluidSourceHelper.isSupportedFluid(block)) {
                ((BlockSpecAccessor) spec).dragonfix$setMetadata(meta);
            }

            if (spec.skipWhenCopying()) {
                continue;
            }

            PendingBlock pending = spec.instantiate(world, voxel.x, voxel.y, voxel.z);

            if (checkTiles) {
                pending.analyze(
                    world.getTileEntity(voxel.x, voxel.y, voxel.z),
                    PendingBlock.ANALYZE_ALL & ~PendingBlock.ANALYZE_ARCH);
            }

            ((PendingBlockDoorBridge) pending)
                .dragonfix$setDoorAnalysis(DoorAnalysisResult.analyze(world, voxel.x, voxel.y, voxel.z, block, meta));

            if (pending.mp != null) {
                pending.buildOrder = Math.max(pending.buildOrder, 100);
            }
            if (BiomesOPlentyPlacementHelper.isWorldDecor(block)) {
                pending.buildOrder = Math.max(pending.buildOrder, 50);
            }

            pending.x -= a.x;
            pending.y -= a.y;
            pending.z -= a.z;

            analysis.blocks.add(pending);
        }

        long post = System.nanoTime();

        if (DebugConfig.debug) {
            MMMod.LOG.info("Analysis took {} ms", (post - pre) / 1e6);
        }

        return analysis;
    }
}
