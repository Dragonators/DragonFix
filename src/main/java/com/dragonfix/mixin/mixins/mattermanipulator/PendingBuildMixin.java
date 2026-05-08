package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.dragonfix.mattermanipulator.bridge.PendingBlockAvaritiaddonsBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockDoorBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockLittleTilesBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockMachineInventoryBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockMalisisDoorsBridge;
import com.dragonfix.mattermanipulator.bridge.PendingBlockOpenComputersBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorFluidSourceConsumer;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorFluidSourceHelper;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorPlacementHelper;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.recursive_pineapple.matter_manipulator.common.building.AEAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.AbstractBuildable;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.ImmutableBlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.InventoryAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBuild;
import com.recursive_pineapple.matter_manipulator.common.building.ProxiedWorld;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.PatternItemProvider;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator.ManipulatorTier;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;
import com.recursive_pineapple.matter_manipulator.common.networking.SoundResource;
import com.recursive_pineapple.matter_manipulator.common.utils.BigItemStack;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;
import com.recursive_pineapple.matter_manipulator.common.utils.Mods;

@Mixin(value = PendingBuild.class, remap = false)
public abstract class PendingBuildMixin extends AbstractBuildable {

    @Unique
    private static final int dragonfix$MAX_NON_PLACING_CHECKS = 16384;

    @Unique
    private static final long dragonfix$MAX_NON_PLACING_SCAN_NS = 15_000_000L;

    @Unique
    private static final int dragonfix$SCAN_PROGRESS_BLOCKS = 4096;

    @Unique
    private static final long dragonfix$SCAN_PROGRESS_INTERVAL_MS = 2000L;

    @Shadow(remap = false)
    @Final
    private Deque<PendingBlock> pendingBlocks;

    @Shadow(remap = false)
    @Final
    private HashSet<Long> visited;

    @Unique
    private int dragonfix$deferredShuffleCount;

    @Unique
    private int dragonfix$scanProgressSinceMessage;

    @Unique
    private int dragonfix$completedProgressSinceMessage;

    @Unique
    private long dragonfix$lastScanProgressMessageMs;

    @Unique
    private long dragonfix$scanProgressStartedMs;

    public PendingBuildMixin(EntityPlayer player, MMState state, ManipulatorTier tier) {
        super(player, state, tier);
    }

    /**
     * @author DragonFix
     * @reason Bound the non-placing scan work per invocation and fast-skip simple already-completed blocks.
     */
    @Overwrite(remap = false)
    public void tryPlaceBlocks(ItemStack stack, EntityPlayer player) {
        resetWarnings();
        refillPower(stack);

        List<PendingBlock> toPlace = new ArrayList<>(tier.placeSpeed);

        Integer lastChunkX = null;
        Integer lastChunkZ = null;
        int shuffleCount = 0;
        int nonPlacingChecks = 0;
        int completedChecks = 0;
        boolean pausedForBudget = false;
        long scanStartedNs = System.nanoTime();

        World world = player.worldObj;
        ProxiedWorld proxiedWorld = new ProxiedWorld(world);
        PendingBuild.PendingBuildApplyContext applyContext = ((PendingBuild) (Object) this).new PendingBuildApplyContext(
            stack);
        BlockSpec pooled = new BlockSpec();

        while (toPlace.size() < tier.placeSpeed && !pendingBlocks.isEmpty()) {
            PendingBlock next = pendingBlocks.getFirst();
            int x = next.x;
            int y = next.y;
            int z = next.z;
            int chunkX = x >> 4;
            int chunkZ = z >> 4;

            if (!Objects.equals(chunkX, lastChunkX) || !Objects.equals(chunkZ, lastChunkZ)) {
                if (!world.getChunkProvider()
                    .chunkExists(chunkX, chunkZ)) {
                    pendingBlocks.removeFirst();
                    dragonfix$deferredShuffleCount = 0;
                    pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                    if (pausedForBudget) break;
                    continue;
                }

                lastChunkX = chunkX;
                lastChunkZ = chunkZ;
            }

            if (y < 0 || y > 255) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (pausedForBudget) break;
                continue;
            }

            if (MatterManipulatorFluidSourceHelper.isDisplayOnlyFlow(next)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                continue;
            }

            Block existingBlock = world.getBlock(x, y, z);

            if (next.spec.isAir() && existingBlock.isAir(world, x, y, z)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                continue;
            }

            if (dragonfix$canFastSkipExistingBlock(next, world, x, y, z, existingBlock)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                continue;
            }

            if (!isEditable(world, x, y, z)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (pausedForBudget) break;
                continue;
            }

            if (!toPlace.isEmpty() && !MatterManipulatorFluidSourceConsumer.canBatchTogether(next, toPlace.get(0))) {
                break;
            }

            BlockSpec existingSpec = BlockSpec.fromBlock(pooled, world, x, y, z);

            if (next.spec.isAir() && existingSpec.getBlock()
                .isAir(world, x, y, z)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                continue;
            }

            if (existingSpec.equals(next.spec) && !dragonfix$hasConfigurationPayload(next)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                completedChecks++;
                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (pausedForBudget) break;
                continue;
            }

            PendingBlock existing = PendingBlock.fromBlock(world, x, y, z);
            existing.analyze(world.getTileEntity(x, y, z), dragonfix$getExistingAnalysisFlags(next));

            boolean equivalent = PendingBlock.areEquivalent(existing, next);
            if (equivalent) {
                PendingBlock block = pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                completedChecks++;

                if (dragonfix$supportsConfiguring() && dragonfix$needsEquivalentApply(block, existing, existingSpec)) {
                    applyContext.pendingBlock = block;
                    block.apply(applyContext, world);
                    playSound(world, x, y, z, SoundResource.MOB_ENDERMEN_PORTAL);
                }

                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (pausedForBudget) break;
                continue;
            }

            boolean canPlace = switch (state.config.removeMode) {
                case NONE -> existing.getBlock()
                    .isAir(world, x, y, z);
                case REPLACEABLE -> existing.getBlock()
                    .isReplaceable(world, x, y, z);
                case ALL -> true;
            };

            canPlace &= existing.getBlock()
                .getBlockHardness(world, x, y, z) >= 0;

            if (!canPlace) {
                pendingBlocks.addLast(pendingBlocks.removeFirst());
                shuffleCount++;
                dragonfix$deferredShuffleCount++;

                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (shuffleCount > pendingBlocks.size() || dragonfix$deferredShuffleCount > pendingBlocks.size()) {
                    pausedForBudget = false;
                    break;
                }
                if (pausedForBudget) break;

                continue;
            }

            if (!existing.getBlock()
                .isAir(world, x, y, z)) {
                if (!state.hasCap(ItemMatterManipulator.ALLOW_REMOVING)) {
                    pendingBlocks.removeFirst();
                    dragonfix$deferredShuffleCount = 0;
                    pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                    if (pausedForBudget) break;
                    continue;
                }

                if (!tryConsumePower(stack, world, x, y, z, existing.spec)) {
                    MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.out_of_eu"));
                    break;
                }
            }

            proxiedWorld.airX = x;
            proxiedWorld.airY = y;
            proxiedWorld.airZ = z;

            if (!MatterManipulatorPlacementHelper.canPlaceBlockAt(next, next.getBlock(), proxiedWorld, x, y, z)) {
                pendingBlocks.addLast(pendingBlocks.removeFirst());
                shuffleCount++;
                dragonfix$deferredShuffleCount++;

                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (shuffleCount > pendingBlocks.size() || dragonfix$deferredShuffleCount > pendingBlocks.size()) {
                    pausedForBudget = false;
                    break;
                }
                if (pausedForBudget) break;

                continue;
            }

            if (!tryConsumePower(stack, world, x, y, z, next.spec)) {
                MMUtils.sendErrorToPlayer(player, StatCollector.translateToLocal("mm.info.error.out_of_eu"));
                break;
            }

            long coord = CoordinatePacker.pack(x, y, z);

            if (!visited.add(coord)) {
                pendingBlocks.removeFirst();
                dragonfix$deferredShuffleCount = 0;
                pausedForBudget = dragonfix$shouldPauseScan(scanStartedNs, ++nonPlacingChecks);
                if (pausedForBudget) break;
                continue;
            }

            dragonfix$deferredShuffleCount = 0;
            toPlace.add(pendingBlocks.removeFirst());
        }

        if (toPlace.isEmpty()) {
            if (pausedForBudget) {
                dragonfix$sendScanProgress(player, nonPlacingChecks, completedChecks);
                actuallyGivePlayerStuff();
                playSounds();
                return;
            }

            if (!pendingBlocks.isEmpty()) {
                dragonfix$deferredShuffleCount = 0;
                dragonfix$resetScanProgress();
                MMUtils.sendErrorToPlayer(
                    player,
                    StatCollector.translateToLocalFormatted("mm.info.error.could_not_place", pendingBlocks.size()));
            } else {
                dragonfix$deferredShuffleCount = 0;
                dragonfix$resetScanProgress();
                MMUtils.sendInfoToPlayer(player, StatCollector.translateToLocal("mm.info.finished_placing"));
            }

            actuallyGivePlayerStuff();
            playSounds();
            return;
        }

        dragonfix$resetScanProgress();

        PendingBlock first = toPlace.get(0);

        FluidStack perSourceFluid = MatterManipulatorFluidSourceHelper.getFluidStack(first, 1000);
        ItemStack perBlock = perSourceFluid == null ? first.getStack() : null;
        long total = 0;
        BigItemStack extracted = null;

        if (perSourceFluid != null) {
            total = toPlace.size() * 1000L;

            if (!MatterManipulatorFluidSourceConsumer.tryConsume(this, perSourceFluid, total)) {
                MMUtils.sendWarningToPlayer(
                    player,
                    StatCollector.translateToLocalFormatted("mm.info.warning.could_not_find", toPlace.size()));
                MMUtils.sendWarningToPlayer(
                    player,
                    String.format("  %s x %d L", perSourceFluid.getLocalizedName(), total));

                for (PendingBlock pending : toPlace) {
                    pendingBlocks.add(pending);
                    visited.remove(CoordinatePacker.pack(pending.x, pending.y, pending.z));
                }

                toPlace.clear();
            }
        } else if (!first.isFree()) {
            total = toPlace.size() * (long) perBlock.stackSize;

            List<BigItemStack> extractedStacks = tryConsumeItems(
                Collections.singletonList(
                    BigItemStack.create(perBlock)
                        .setStackSize(total)),
                CONSUME_PARTIAL).right();

            extracted = extractedStacks.isEmpty() ? null : extractedStacks.get(0);

            if (extracted == null) {
                MMUtils.sendWarningToPlayer(
                    player,
                    StatCollector.translateToLocalFormatted("mm.info.warning.could_not_find", toPlace.size()));
                MMUtils.sendWarningToPlayer(player, String.format("  %s x %d", first.getDisplayName(), total));

                for (PendingBlock pending : toPlace) {
                    pendingBlocks.add(pending);
                    visited.remove(CoordinatePacker.pack(pending.x, pending.y, pending.z));
                }

                toPlace.clear();
            }
        }

        int i = 0;
        for (; i < toPlace.size(); i++) {
            PendingBlock pending = toPlace.get(i);

            int x = pending.x;
            int y = pending.y;
            int z = pending.z;

            playSound(world, x, y, z, SoundResource.MOB_ENDERMEN_PORTAL);

            int metadata = pending.spec.getBlockMeta();

            BlockSpec existing = BlockSpec.fromBlock(pooled, world, x, y, z);

            if (existing.equals(pending.spec)) {
                if (dragonfix$supportsConfiguring()) {
                    applyContext.pendingBlock = pending;
                    pending.apply(applyContext, world);
                }

                world.notifyBlockOfNeighborChange(x, y, z, Blocks.air);
                continue;
            }

            if (extracted != null && extracted.stackSize < perBlock.stackSize) {
                break;
            }

            if (!existing.isAir()) {
                removeBlock(world, x, y, z, existing);
            }

            if (!pending.spec.isAir()) {
                Block block = pending.getBlock();

                if (pending.getItem() instanceof ItemBlock itemBlock
                    && !MatterManipulatorPlacementHelper.shouldPlaceDirectly(block, metadata)) {
                    itemBlock.placeBlockAt(
                        perBlock,
                        player,
                        player.worldObj,
                        x,
                        y,
                        z,
                        dragonfix$getDefaultPlaceSide(pending.spec).ordinal(),
                        0,
                        0,
                        0,
                        metadata);
                } else {
                    if (!MatterManipulatorPlacementHelper.setBlock(world, x, y, z, block, metadata, 3)) {
                        MatterManipulatorFluidSourceConsumer.refund(this, pending);
                        continue;
                    }

                    if (world.getBlock(x, y, z) == block) {
                        block.onBlockPlacedBy(world, x, y, z, player, stack);
                        block.onPostBlockPlaced(world, x, y, z, metadata);
                    }
                }
            }

            if (extracted != null) {
                extracted.stackSize -= perBlock.stackSize;
            }

            applyContext.pendingBlock = pending;
            pending.apply(applyContext, world);
        }

        if (extracted != null && i < toPlace.size()) {
            MMUtils.sendWarningToPlayer(
                player,
                StatCollector.translateToLocalFormatted("mm.info.warning.could_not_find", toPlace.size() - i));
            MMUtils.sendWarningToPlayer(
                player,
                String.format(
                    "  %s x %d",
                    first.getDisplayName(),
                    total - (long) (toPlace.size() - i) * perBlock.stackSize));
        }

        MMUtils.sendInfoToPlayer(
            player,
            StatCollector.translateToLocalFormatted("mm.info.placed_remaining", i, pendingBlocks.size()));

        if (extracted != null && extracted.stackSize >= perBlock.stackSize) {
            givePlayerItems(
                extracted.toStacks()
                    .toArray(new ItemStack[0]));
        }

        for (; i < toPlace.size(); i++) {
            PendingBlock pending = toPlace.get(i);

            pendingBlocks.add(pending);
            visited.remove(CoordinatePacker.pack(pending.x, pending.y, pending.z));
        }

        actuallyGivePlayerStuff();
        playSounds();
    }

    @Unique
    private boolean dragonfix$supportsConfiguring() {
        if (state.hasCap(ItemMatterManipulator.ALLOW_CONFIGURING)) return true;
        if (state.config.placeMode == PlaceMode.EXCHANGING) return true;
        return state.config.placeMode == PlaceMode.CABLES;
    }

    @Unique
    private static int dragonfix$getExistingAnalysisFlags(PendingBlock block) {
        int flags = PendingBlock.ANALYZE_ARCH;

        if (block.gt != null) flags |= PendingBlock.ANALYZE_GT;
        if (block.ae != null) flags |= PendingBlock.ANALYZE_AE;
        if (block.mp != null) flags |= PendingBlock.ANALYZE_MP;
        if (block.inventory != null) flags |= PendingBlock.ANALYZE_INV;

        return flags;
    }

    @Unique
    private static boolean dragonfix$needsEquivalentApply(PendingBlock block, PendingBlock existing,
        BlockSpec existingSpec) {
        if (!existingSpec.equals(block.spec)) return true;
        if (block.ae instanceof AEAnalysisResult expectedAe) {
            if (!(existing.ae instanceof AEAnalysisResult actualAe)) return true;
            if (!dragonfix$aeEquals(expectedAe, actualAe)) return true;
        }
        if (block.inventory != null && !dragonfix$inventoryEquals(block.inventory, existing.inventory)) return true;
        if (((PendingBlockDoorBridge) block).dragonfix$getDoorAnalysis() != null) return true;
        return ((PendingBlockLittleTilesBridge) block).dragonfix$getCarpentersBlocksAnalysis() != null;
    }

    @Unique
    private static boolean dragonfix$aeEquals(AEAnalysisResult expected, AEAnalysisResult actual) {
        if (expected == actual) return true;
        if (expected == null || actual == null) return false;

        return expected.mAEColour == actual.mAEColour && expected.mAEUp == actual.mAEUp
            && expected.mAEForward == actual.mAEForward
            && Objects.equals(expected.mAEConfig, actual.mAEConfig)
            && Arrays.equals(expected.mAEUpgrades, actual.mAEUpgrades)
            && Objects.equals(expected.mAECustomName, actual.mAECustomName)
            && Arrays.equals(expected.mAEParts, actual.mAEParts)
            && Arrays.equals(expected.mAEFacades, actual.mAEFacades)
            && dragonfix$inventoryEquals(expected.mAECells, actual.mAECells)
            && dragonfix$inventoryEquals(expected.mAEPatterns, actual.mAEPatterns);
    }

    @Unique
    private static boolean dragonfix$inventoryEquals(InventoryAnalysis expected, InventoryAnalysis actual) {
        if (expected == actual) return true;
        if (expected == null || actual == null) return false;
        if (expected.mFuzzy != actual.mFuzzy) return false;

        IItemProvider[] expectedItems = expected.mItems;
        IItemProvider[] actualItems = actual.mItems;
        if (expectedItems == actualItems) return true;
        if (expectedItems == null || actualItems == null) return false;
        if (expectedItems.length != actualItems.length) return false;

        for (int i = 0; i < expectedItems.length; i++) {
            if (!dragonfix$itemProviderEquals(expectedItems[i], actualItems[i])) return false;
        }

        return true;
    }

    @Unique
    private static boolean dragonfix$itemProviderEquals(IItemProvider expected, IItemProvider actual) {
        if (Objects.equals(expected, actual)) return true;
        if (expected == null || actual == null) return false;
        if (expected instanceof PatternItemProvider && actual instanceof PatternItemProvider) {
            return ItemStack.areItemStacksEqual(expected.getStack(null, false), actual.getStack(null, false));
        }
        return false;
    }

    @Unique
    private void dragonfix$sendScanProgress(EntityPlayer player, int checked, int completed) {
        dragonfix$scanProgressSinceMessage += checked;
        dragonfix$completedProgressSinceMessage += completed;

        long now = System.currentTimeMillis();
        if (dragonfix$scanProgressStartedMs == 0L) {
            dragonfix$scanProgressStartedMs = now;
        }

        long lastProgressMs = dragonfix$lastScanProgressMessageMs == 0L ? dragonfix$scanProgressStartedMs
            : dragonfix$lastScanProgressMessageMs;

        if (dragonfix$scanProgressSinceMessage < dragonfix$SCAN_PROGRESS_BLOCKS
            && now - lastProgressMs < dragonfix$SCAN_PROGRESS_INTERVAL_MS) {
            return;
        }

        MMUtils.sendInfoToPlayer(
            player,
            StatCollector.translateToLocalFormatted(
                "dragonfix.mm.info.scan_progress",
                dragonfix$scanProgressSinceMessage,
                dragonfix$completedProgressSinceMessage,
                pendingBlocks.size()));

        dragonfix$scanProgressSinceMessage = 0;
        dragonfix$completedProgressSinceMessage = 0;
        dragonfix$lastScanProgressMessageMs = now;
        dragonfix$scanProgressStartedMs = now;
    }

    @Unique
    private void dragonfix$resetScanProgress() {
        dragonfix$scanProgressSinceMessage = 0;
        dragonfix$completedProgressSinceMessage = 0;
        dragonfix$lastScanProgressMessageMs = 0L;
        dragonfix$scanProgressStartedMs = 0L;
    }

    @Unique
    private static boolean dragonfix$shouldPauseScan(long scanStartedNs, int checks) {
        return checks >= dragonfix$MAX_NON_PLACING_CHECKS
            || System.nanoTime() - scanStartedNs >= dragonfix$MAX_NON_PLACING_SCAN_NS;
    }

    @Unique
    private static boolean dragonfix$canFastSkipExistingBlock(PendingBlock block, World world, int x, int y, int z,
        Block existingBlock) {
        if (dragonfix$hasConfigurationPayload(block)) return false;
        if (!(block.spec instanceof BlockSpec spec)) return false;
        if (spec.properties != null || spec.intrinsicProperties != null) return false;
        if (Mods.ArchitectureCraft.isModLoaded() && spec.arch != null) return false;
        if (spec.getBlock() != existingBlock) return false;

        return ((BlockSpecAccessor) spec).dragonfix$getMetadata() == existingBlock.getDamageValue(world, x, y, z);
    }

    @Unique
    private static boolean dragonfix$hasConfigurationPayload(PendingBlock block) {
        if (block.gt != null || block.ae != null || block.arch != null || block.mp != null || block.inventory != null) {
            return true;
        }

        if (((PendingBlockLittleTilesBridge) block).dragonfix$getLittleTilesAnalysis() != null) return true;
        if (((PendingBlockLittleTilesBridge) block).dragonfix$getCarpentersBlocksAnalysis() != null) return true;
        if (((PendingBlockAvaritiaddonsBridge) block).dragonfix$getAvaritiaddonsExtremeAutoCrafterAnalysis() != null)
            return true;
        if (((PendingBlockMachineInventoryBridge) block).dragonfix$getAE2CondenserAnalysis() != null) return true;
        if (((PendingBlockMachineInventoryBridge) block).dragonfix$getEnderIOSoulBinderAnalysis() != null) return true;
        if (((PendingBlockMalisisDoorsBridge) block).dragonfix$getMalisisCustomDoorAnalysis() != null) return true;
        if (((PendingBlockOpenComputersBridge) block).dragonfix$getOpenComputersMicrocontrollerAnalysis() != null)
            return true;
        return ((PendingBlockDoorBridge) block).dragonfix$getDoorAnalysis() != null;
    }

    @Unique
    private static ForgeDirection dragonfix$getDefaultPlaceSide(ImmutableBlockSpec spec) {
        if (Mods.GregTech.isModLoaded() && MMUtils.isGTCable(spec)) return ForgeDirection.UNKNOWN;

        return ForgeDirection.NORTH;
    }
}
