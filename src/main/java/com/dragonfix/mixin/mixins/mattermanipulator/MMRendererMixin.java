package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.ArchitectureCraftPreviewBridge;
import com.dragonfix.mattermanipulator.DragonFixRenderHints;
import com.dragonfix.mattermanipulator.LittleTilesAnalysisResult;
import com.dragonfix.mattermanipulator.PendingBlockLittleTilesBridge;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.recursive_pineapple.matter_manipulator.GlobalMMConfig.RenderingConfig;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMRenderer;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

@Mixin(value = MMRenderer.class, remap = false)
public abstract class MMRendererMixin {

    @Shadow(remap = false)
    private static List<PendingBlock> analysisCache;

    @Shadow(remap = false)
    private static LongList errors;

    @Shadow(remap = false)
    private static LongList warnings;

    @Shadow(remap = false)
    @Final
    private static short[] WHITE;

    @Shadow(remap = false)
    @Final
    private static short[] WARNING;

    @Shadow(remap = false)
    @Final
    private static short[] ERROR;

    @Inject(method = "drawHints", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$drawHints(RenderWorldLastEvent event, MMState state, EntityPlayer player,
        Location playerLocation, int maxRange, CallbackInfo ci) {
        int buildable = maxRange * maxRange;
        int i = 0;
        BlockSpec pooled = new BlockSpec();
        LongOpenHashSet currentErrors = errors == null ? null : new LongOpenHashSet(errors);
        LongOpenHashSet currentWarnings = warnings == null ? null : new LongOpenHashSet(warnings);
        World world = player.worldObj;

        RenderHints.reset();
        RenderHints.setDrawOnTop(RenderingConfig.hintsOnTop || state.config.placeMode == PlaceMode.EXCHANGING);

        for (PendingBlock pendingBlock : analysisCache) {
            if (!pendingBlock.isInWorld(world)) continue;

            if (maxRange != -1) {
                int dist2 = pendingBlock.distanceTo2(playerLocation);
                if (dist2 > buildable) continue;
            }

            if (pendingBlock.spec.isAir() && world.isAirBlock(pendingBlock.x, pendingBlock.y, pendingBlock.z)) continue;

            Block block = pendingBlock.getBlock();
            if (block == null) continue;

            LittleTilesAnalysisResult littleTiles = dragonfix$getLittleTilesAnalysis(pendingBlock);
            if (littleTiles != null) {
                if (++i > RenderingConfig.maxHints) break;

                short[] tint = dragonfix$getAndRemoveTint(currentWarnings, currentErrors, pendingBlock);
                dragonfix$addLittleTilesHints(pendingBlock, littleTiles, tint);
                continue;
            }

            BlockSpec.fromBlock(pooled, world, pendingBlock.x, pendingBlock.y, pendingBlock.z);

            if (pooled.isEquivalent(pendingBlock.spec)) continue;

            if (++i > RenderingConfig.maxHints) break;

            short[] tint = dragonfix$getAndRemoveTint(currentWarnings, currentErrors, pendingBlock);

            if (pendingBlock.arch instanceof ArchitectureCraftPreviewBridge architectureCraft) {
                architectureCraft.dragonfix$addPreviewHint(pendingBlock, tint);
                continue;
            }

            if (pendingBlock.spec.isAir()) {
                RenderHints.addHint(
                    pendingBlock.x,
                    pendingBlock.y,
                    pendingBlock.z,
                    com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint(),
                    com.gtnewhorizon.structurelib.StructureLibAPI.HINT_BLOCK_META_ERROR,
                    tint);
            } else {
                RenderHints.addHint(
                    pendingBlock.x,
                    pendingBlock.y,
                    pendingBlock.z,
                    block,
                    pendingBlock.spec.getBlockMeta(),
                    tint);
            }
        }

        if (currentWarnings != null) {
            for (long packed : currentWarnings) {
                int x = CoordinatePacker.unpackX(packed);
                int y = CoordinatePacker.unpackY(packed);
                int z = CoordinatePacker.unpackZ(packed);

                RenderHints.addHint(
                    x,
                    y,
                    z,
                    com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint(),
                    com.gtnewhorizon.structurelib.StructureLibAPI.HINT_BLOCK_META_AIR,
                    WARNING);
            }
        }

        if (currentErrors != null) {
            for (long packed : currentErrors) {
                int x = CoordinatePacker.unpackX(packed);
                int y = CoordinatePacker.unpackY(packed);
                int z = CoordinatePacker.unpackZ(packed);

                RenderHints.addHint(
                    x,
                    y,
                    z,
                    com.gtnewhorizon.structurelib.StructureLibAPI.getBlockHint(),
                    com.gtnewhorizon.structurelib.StructureLibAPI.HINT_BLOCK_META_AIR,
                    ERROR);
            }
        }

        ci.cancel();
    }

    @Unique
    private static LittleTilesAnalysisResult dragonfix$getLittleTilesAnalysis(PendingBlock pendingBlock) {
        ITileAnalysisIntegration analysis = ((PendingBlockLittleTilesBridge) pendingBlock)
            .dragonfix$getLittleTilesAnalysis();
        return analysis instanceof LittleTilesAnalysisResult ? (LittleTilesAnalysisResult) analysis : null;
    }

    @Unique
    private static short[] dragonfix$getAndRemoveTint(LongOpenHashSet currentWarnings, LongOpenHashSet currentErrors,
        PendingBlock pendingBlock) {
        long packed = CoordinatePacker.pack(pendingBlock.x, pendingBlock.y, pendingBlock.z);
        short[] tint = WHITE;

        if (currentWarnings != null && currentWarnings.remove(packed)) {
            tint = WARNING;
        }

        if (currentErrors != null && currentErrors.remove(packed)) {
            tint = ERROR;
        }

        return tint;
    }

    @Unique
    private static void dragonfix$addLittleTilesHints(PendingBlock pendingBlock, LittleTilesAnalysisResult littleTiles,
        short[] tint) {
        ArrayList<LittleTilesAnalysisResult.RenderBox> boxes = new ArrayList<>();
        littleTiles.getRenderBoxes(boxes);

        for (LittleTilesAnalysisResult.RenderBox box : boxes) {
            DragonFixRenderHints.addHint(
                pendingBlock.x,
                pendingBlock.y,
                pendingBlock.z,
                box.minX / 16d,
                box.minY / 16d,
                box.minZ / 16d,
                box.maxX / 16d,
                box.maxY / 16d,
                box.maxZ / 16d,
                box.block,
                box.meta,
                tint);
        }
    }

}
