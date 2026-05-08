package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.DragonFixRenderHints;
import com.dragonfix.mattermanipulator.analysis.LittleTilesAnalysisResult;
import com.dragonfix.mattermanipulator.bridge.ArchitectureCraftPreviewBridge;
import com.dragonfix.mattermanipulator.bridge.DragonFixMultipartPreviewBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockLittleTilesBridge;
import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.gtnewhorizon.gtnhlib.util.AboveHotbarHUD;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.recursive_pineapple.matter_manipulator.GlobalMMConfig.RenderingConfig;
import com.recursive_pineapple.matter_manipulator.client.rendering.BoxRenderer;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig.VoxelAABB;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMRenderer;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Adds custom preview rendering hooks for multipart, LittleTiles, and ArchitectureCraft MatterManipulator hints.
 *
 * <p>
 * Preview hook structure adapted from GTNewHorizons/MatterManipulator PR #34 by Luca-Guettinger and
 * RecursivePineapple.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/34">MatterManipulator PR #34</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/9d76ed6e8ec87da8f55404893ea3b5ebe6912759">MatterManipulator
 *      commit 9d76ed6e</a>
 */
@Mixin(value = MMRenderer.class, remap = false)
public abstract class MMRendererMixin {

    @Shadow(remap = false)
    private static List<PendingBlock> analysisCache;

    @Shadow(remap = false)
    private static LongList errors;

    @Shadow(remap = false)
    private static LongList warnings;

    @Shadow(remap = false)
    private static long lastAnalysisMS;

    @Shadow(remap = false)
    private static MMConfig lastAnalyzedConfig;

    @Shadow(remap = false)
    private static Location lastPlayerPosition;

    @Shadow(remap = false)
    private static ItemMatterManipulator lastDrawer;

    @Shadow(remap = false)
    private static boolean wasValid;

    @Shadow(remap = false)
    private static boolean needsHintDraw;

    @Shadow(remap = false)
    private static boolean needsAnalysis;

    @Unique
    private static UUID dragonfix$lastPersistentSchematicId;

    @Shadow(remap = false)
    private static long statusExpiration;

    @Shadow(remap = false)
    @Final
    private static long ANALYSIS_INTERVAL_MS;

    @Shadow(remap = false)
    @Final
    private static short[] WHITE;

    @Shadow(remap = false)
    @Final
    private static short[] WARNING;

    @Shadow(remap = false)
    @Final
    private static short[] ERROR;

    @Shadow(remap = false)
    private static void clear(EntityPlayer player) {}

    @Shadow(remap = false)
    private static void drawRulers(EntityPlayer player, Location l, boolean fromSurface, float partialTickTime) {}

    @Inject(method = "renderSelectionImpl", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dragonfix$renderPersistentSchematicSelection(RenderWorldLastEvent event, CallbackInfo ci) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        net.minecraft.item.ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemMatterManipulator manipulator)) return;

        MMState state = MatterManipulatorStateAccess.getState(held);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;

        if (!bridge.dragonfix$isPersistentSchematicCopy() && !bridge.dragonfix$isPersistentSchematicPaste()) return;

        if (bridge.dragonfix$isPersistentSchematicPaste() && dragonfix$isStalePersistentSchematicPaste(bridge)) {
            dragonfix$clearStaleHints();
            ci.cancel();
            return;
        }

        dragonfix$renderPersistentSchematic(event, player, state, manipulator, bridge);
        ci.cancel();
    }

    /**
     * @author DragonFix
     * @reason Add multipart, LittleTiles, and ArchitectureCraft preview paths directly in the hint renderer.
     */
    @Overwrite(remap = false)
    private static void drawHints(RenderWorldLastEvent event, MMState state, EntityPlayer player,
        Location playerLocation, int maxRange) {
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
            int meta = pendingBlock.spec.getBlockMeta();
            boolean multipart = pendingBlock.mp instanceof DragonFixMultipartPreviewBridge;
            if (multipart) {
                DragonFixMultipartPreviewBridge preview = (DragonFixMultipartPreviewBridge) pendingBlock.mp;
                Block previewBlock = preview.getPreviewBlock();
                if (previewBlock != null) {
                    block = previewBlock;
                    meta = preview.getPreviewMeta();
                } else {
                    block = Blocks.redstone_wire;
                    meta = 0;
                }
            }
            if (block == null) continue;

            LittleTilesAnalysisResult littleTiles = dragonfix$getLittleTilesAnalysis(pendingBlock);
            if (littleTiles != null) {
                if (++i > RenderingConfig.maxHints) break;

                short[] tint = dragonfix$getAndRemoveTint(currentWarnings, currentErrors, pendingBlock);
                dragonfix$addLittleTilesHints(pendingBlock, littleTiles, tint);
                continue;
            }

            BlockSpec.fromBlock(pooled, world, pendingBlock.x, pendingBlock.y, pendingBlock.z);

            if (multipart) {
                if (pooled.getBlock() == pendingBlock.getBlock()) continue;
            } else if (pooled.isEquivalent(pendingBlock.spec)) {
                continue;
            }

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
                RenderHints.addHint(pendingBlock.x, pendingBlock.y, pendingBlock.z, block, meta, tint);
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
    }

    @Unique
    private static void dragonfix$renderPersistentSchematic(RenderWorldLastEvent event, EntityPlayer player,
        MMState state, ItemMatterManipulator manipulator, PersistentSchematicConfigBridge bridge) {
        Vector3i lookingAt = MMUtils.getLookingAtLocation(player);
        Location sourceA = state.config.coordA;
        Location sourceB = state.config.coordB;
        Location paste = state.config.coordC;
        boolean pasteMode = bridge.dragonfix$isPersistentSchematicPaste();

        if (!pasteMode) {
            dragonfix$clearStaleHints();
        }

        if (state.config.action != null) {
            switch (state.config.action) {
                case MARK_COPY_A -> {
                    sourceA = new Location(player.worldObj, lookingAt);
                    GL11.glColor4f(0.15f, 0.6f, 0.75f, 0.75F);
                    drawRulers(player, sourceA, false, event.partialTicks);
                }
                case MARK_COPY_B -> {
                    sourceB = new Location(player.worldObj, lookingAt);
                    GL11.glColor4f(0.15f, 0.6f, 0.75f, 0.75F);
                    drawRulers(player, sourceB, false, event.partialTicks);
                }
                case MARK_PASTE -> {
                    paste = new Location(player.worldObj, lookingAt);
                    GL11.glColor4f(0.75f, 0.5f, 0.15f, 0.75F);
                    drawRulers(player, paste, false, event.partialTicks);
                }
                case MARK_ARRAY -> {
                    if (!bridge.dragonfix$isPersistentSchematicPaste()) return;

                    GL11.glColor4f(0.4f, 0.75f, 0.15f, 0.75F);
                    drawRulers(player, new Location(player.worldObj, lookingAt), false, event.partialTicks);

                    if (paste != null && paste.isInWorld(player.worldObj)) {
                        try {
                            state.config.arraySpan = PersistentSchematic
                                .load(bridge.dragonfix$getPersistentSchematicFile())
                                .getArrayMult(player.worldObj, paste, lookingAt, state.config.transform);
                        } catch (Exception ignored) {
                            state.config.arraySpan = null;
                        }
                    }
                }
                default -> {
                    return;
                }
            }
        }

        state.config.coordA = sourceA;
        state.config.coordB = sourceB;
        state.config.coordC = paste;

        boolean isSourceAValid = sourceA != null && sourceA.isInWorld(player.worldObj);
        boolean isSourceBValid = sourceB != null && sourceB.isInWorld(player.worldObj);
        boolean isPasteValid = paste != null && paste.isInWorld(player.worldObj);
        boolean isValid = bridge.dragonfix$isPersistentSchematicCopy() ? isSourceAValid && isSourceBValid
            : isPasteValid;

        if (pasteMode && !isPasteValid) {
            dragonfix$clearStaleHints();
        }

        if (!isValid && wasValid) {
            clear(player);
            wasValid = false;
            return;
        }

        wasValid = isValid;

        BoxRenderer.INSTANCE.start(event.partialTicks);

        try {
            VoxelAABB copyDeltas = null;
            VoxelAABB pasteDeltas = null;

            if (bridge.dragonfix$isPersistentSchematicCopy() && isSourceAValid && isSourceBValid) {
                copyDeltas = new VoxelAABB(
                    Objects.requireNonNull(sourceA)
                        .toVec(),
                    Objects.requireNonNull(sourceB)
                        .toVec());
                BoxRenderer.INSTANCE.drawAround(copyDeltas.toBoundingBox(), new Vector3f(0.15f, 0.6f, 0.75f));
            }

            if (bridge.dragonfix$isPersistentSchematicPaste() && isPasteValid
                && dragonfix$hasRenderablePersistentSchematic(bridge)) {
                try {
                    pasteDeltas = PersistentSchematic.load(bridge.dragonfix$getPersistentSchematicFile())
                        .getPasteVisualDeltas(
                            player.worldObj.provider.dimensionId,
                            paste,
                            state.config.transform,
                            state.config.arraySpan);
                } catch (Exception ignored) {
                    pasteDeltas = new VoxelAABB(
                        Objects.requireNonNull(paste)
                            .toVec(),
                        paste.toVec());
                }

                if (pasteDeltas != null) {
                    BoxRenderer.INSTANCE.drawAround(pasteDeltas.toBoundingBox(), new Vector3f(0.75f, 0.5f, 0.15f));
                    dragonfix$updatePersistentSchematicHints(event, player, state, manipulator);
                }
            }

            dragonfix$renderPersistentSchematicStatus(copyDeltas, pasteDeltas, state.config.arraySpan);
        } finally {
            BoxRenderer.INSTANCE.finish();
        }
    }

    @Unique
    private static boolean dragonfix$isStalePersistentSchematicPaste(PersistentSchematicConfigBridge bridge) {
        return bridge.dragonfix$getPersistentSchematicId() != null
            && !PersistentSchematicNetwork.isClientLoadedSchematic(bridge.dragonfix$getPersistentSchematicId());
    }

    @Unique
    private static boolean dragonfix$hasRenderablePersistentSchematic(PersistentSchematicConfigBridge bridge) {
        return !bridge.dragonfix$getPersistentSchematicFile()
            .isEmpty()
            && (bridge.dragonfix$getPersistentSchematicId() == null
                || PersistentSchematicNetwork.isClientLoadedSchematic(bridge.dragonfix$getPersistentSchematicId()));
    }

    @Unique
    private static void dragonfix$clearStaleHints() {
        analysisCache = null;
        lastAnalyzedConfig = null;
        lastPlayerPosition = null;
        lastDrawer = null;
        needsHintDraw = false;
        needsAnalysis = false;
        errors = null;
        warnings = null;
        statusExpiration = 0;
        RenderHints.reset();
    }

    @Unique
    private static void dragonfix$updatePersistentSchematicHints(RenderWorldLastEvent event, EntityPlayer player,
        MMState state, ItemMatterManipulator manipulator) {
        Location playerLocation = new Location(
            player.getEntityWorld(),
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posY),
            MathHelper.floor_double(player.posZ));

        long now = System.currentTimeMillis();

        if (statusExpiration > 0 && now > statusExpiration) {
            errors = null;
            warnings = null;
            statusExpiration = 0;
            needsHintDraw = true;
        }

        needsAnalysis = needsAnalysis || (now - lastAnalysisMS) >= ANALYSIS_INTERVAL_MS
            || lastDrawer != manipulator
            || !Objects.equals(lastAnalyzedConfig, state.config);

        UUID persistentSchematicId = ((PersistentSchematicConfigBridge) state.config)
            .dragonfix$getPersistentSchematicId();
        if (!Objects.equals(persistentSchematicId, dragonfix$lastPersistentSchematicId)) {
            dragonfix$lastPersistentSchematicId = persistentSchematicId;
            dragonfix$clearStaleHints();
            needsAnalysis = true;
            needsHintDraw = true;
        }
        needsHintDraw = needsHintDraw || needsAnalysis
            || (lastPlayerPosition != null && lastPlayerPosition.distanceTo(playerLocation) > 2
                && manipulator.tier.maxRange != -1);

        if (needsAnalysis) {
            lastAnalysisMS = now;
            lastAnalyzedConfig = state.config;
            analysisCache = state.getPendingBlocks(manipulator.tier, player.getEntityWorld());
            analysisCache.removeIf(Objects::isNull);
            analysisCache.sort(Comparator.comparingInt((PendingBlock b) -> b.renderOrder));
            needsAnalysis = false;
        }

        if (needsHintDraw) {
            lastPlayerPosition = playerLocation;
            lastDrawer = manipulator;
            needsHintDraw = false;

            drawHints(event, state, player, playerLocation, manipulator.tier.maxRange);
        }
    }

    @Unique
    private static void dragonfix$renderPersistentSchematicStatus(VoxelAABB copyDeltas, VoxelAABB pasteDeltas,
        Vector3i span) {
        if (pasteDeltas != null) {
            String array = "";
            if (span != null) {
                array = String.format(
                    " stX=%d stY=%d stZ=%d",
                    span.x >= 0 ? span.x + 1 : span.x,
                    span.y >= 0 ? span.y + 1 : span.y,
                    span.z >= 0 ? span.z + 1 : span.z);
            }

            AboveHotbarHUD.renderTextAboveHotbar(
                pasteDeltas.describe() + array,
                (int) (ANALYSIS_INTERVAL_MS * 20 / 1000),
                false,
                false);
        } else if (copyDeltas != null) {
            AboveHotbarHUD
                .renderTextAboveHotbar(copyDeltas.describe(), (int) (ANALYSIS_INTERVAL_MS * 20 / 1000), false, false);
        }
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
