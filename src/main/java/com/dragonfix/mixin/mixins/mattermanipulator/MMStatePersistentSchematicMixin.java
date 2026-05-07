package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator.ManipulatorTier;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;

@Mixin(value = MMState.class, remap = false)
public abstract class MMStatePersistentSchematicMixin {

    @Shadow(remap = false)
    public MMConfig config;

    @Shadow(remap = false)
    public abstract Transform getTransform();

    @Shadow(remap = false)
    private List<PendingBlock> getAnalysis(World world) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private List<PendingBlock> getGeomPendingBlocks(World world) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private List<PendingBlock> getExchangeBlocks(ManipulatorTier tier, World world) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private List<PendingBlock> getCableBlocks(World world) {
        throw new AssertionError();
    }

    /**
     * @author DragonFix
     * @reason Persistent schematic copy/paste needs a separate pending-block source, while normal modes keep MM's
     *         original dispatch.
     */
    @Overwrite(remap = false)
    public List<PendingBlock> getPendingBlocks(ManipulatorTier tier, World world) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) config;

        if (bridge.dragonfix$isPersistentSchematicCopy()) {
            return new ArrayList<>();
        }

        if (!bridge.dragonfix$isPersistentSchematicPaste()) {
            return switch (config.placeMode) {
                case COPYING, MOVING -> getAnalysis(world);
                case GEOMETRY -> getGeomPendingBlocks(world);
                case EXCHANGING -> getExchangeBlocks(tier, world);
                case CABLES -> getCableBlocks(world);
            };
        }

        if (world == null || config.coordC == null || !config.coordC.isInWorld(world)) {
            return new ArrayList<>();
        }

        try {
            PersistentSchematic schematic = PersistentSchematicNetwork.getAvailableSchematic(
                bridge.dragonfix$getPersistentSchematicId(),
                bridge.dragonfix$getPersistentSchematicFile(),
                world);
            if (schematic == null) {
                return new ArrayList<>();
            }
            return schematic
                .getPendingBlocks(world.provider.dimensionId, config.coordC.toVec(), getTransform(), config.arraySpan);
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not load persistent Matter Manipulator schematic", e);
            return new ArrayList<>();
        }
    }
}
