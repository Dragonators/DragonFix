package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.recursive_pineapple.matter_manipulator.common.compat.BlockPropertyRegistry;
import com.recursive_pineapple.matter_manipulator.common.compat.Orientation;
import com.recursive_pineapple.matter_manipulator.common.compat.OrientationBlockProperty;

import cpw.mods.fml.common.Loader;
import li.cil.oc.common.tileentity.traits.Rotatable;

/**
 * Replaces OpenComputers rotation copying so MatterManipulator can transform rotatable blocks correctly.
 *
 * <p>
 * Adapted from GTNewHorizons/MatterManipulator PR #48 by Vlamonster.
 *
 * @see <a href="https://github.com/GTNewHorizons/MatterManipulator/pull/48">MatterManipulator PR #48</a>
 * @see <a href=
 *      "https://github.com/GTNewHorizons/MatterManipulator/commit/c8835f4b05017dbd03266e715dfdd475d7c8557e">MatterManipulator
 *      commit c8835f4b</a>
 */
@Mixin(value = BlockPropertyRegistry.class, remap = false)
public abstract class BlockPropertyRegistryMixin {

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private static void dragonfix$initOpenComputers(CallbackInfo ci) {
        if (!Loader.isModLoaded("OpenComputers")) return;

        BlockPropertyRegistry.registerTileEntityInterfaceProperty(Rotatable.class, new OrientationBlockProperty() {

            @Override
            public String getName() {
                return "orientation";
            }

            @Override
            public Orientation getValue(World world, int x, int y, int z) {
                if (!(world.getTileEntity(x, y, z) instanceof Rotatable rotatable)) return Orientation.NONE;

                switch (rotatable.yaw()) {
                    case NORTH:
                        switch (rotatable.pitch()) {
                            case UP:
                                return Orientation.UP_SOUTH;
                            case DOWN:
                                return Orientation.DOWN_NORTH;
                            case NORTH:
                                return Orientation.NORTH_UP;
                            default:
                                return Orientation.NONE;
                        }
                    case SOUTH:
                        switch (rotatable.pitch()) {
                            case UP:
                                return Orientation.UP_NORTH;
                            case DOWN:
                                return Orientation.DOWN_SOUTH;
                            case NORTH:
                                return Orientation.SOUTH_UP;
                            default:
                                return Orientation.NONE;
                        }
                    case WEST:
                        switch (rotatable.pitch()) {
                            case UP:
                                return Orientation.UP_EAST;
                            case DOWN:
                                return Orientation.DOWN_WEST;
                            case NORTH:
                                return Orientation.WEST_UP;
                            default:
                                return Orientation.NONE;
                        }
                    case EAST:
                        switch (rotatable.pitch()) {
                            case UP:
                                return Orientation.UP_WEST;
                            case DOWN:
                                return Orientation.DOWN_EAST;
                            case NORTH:
                                return Orientation.EAST_UP;
                            default:
                                return Orientation.NONE;
                        }
                    default:
                        return Orientation.NONE;
                }
            }

            @Override
            public void setValue(World world, int x, int y, int z, Orientation orientation) {
                if (!(world.getTileEntity(x, y, z) instanceof Rotatable rotatable)) return;

                switch (orientation) {
                    case UP_SOUTH:
                        dragonfix$set(rotatable, ForgeDirection.NORTH, ForgeDirection.UP);
                        break;
                    case DOWN_NORTH:
                        dragonfix$set(rotatable, ForgeDirection.NORTH, ForgeDirection.DOWN);
                        break;
                    case NORTH_UP:
                        dragonfix$set(rotatable, ForgeDirection.NORTH, ForgeDirection.NORTH);
                        break;
                    case UP_NORTH:
                        dragonfix$set(rotatable, ForgeDirection.SOUTH, ForgeDirection.UP);
                        break;
                    case DOWN_SOUTH:
                        dragonfix$set(rotatable, ForgeDirection.SOUTH, ForgeDirection.DOWN);
                        break;
                    case SOUTH_UP:
                        dragonfix$set(rotatable, ForgeDirection.SOUTH, ForgeDirection.NORTH);
                        break;
                    case UP_EAST:
                        dragonfix$set(rotatable, ForgeDirection.WEST, ForgeDirection.UP);
                        break;
                    case DOWN_WEST:
                        dragonfix$set(rotatable, ForgeDirection.WEST, ForgeDirection.DOWN);
                        break;
                    case WEST_UP:
                        dragonfix$set(rotatable, ForgeDirection.WEST, ForgeDirection.NORTH);
                        break;
                    case UP_WEST:
                        dragonfix$set(rotatable, ForgeDirection.EAST, ForgeDirection.UP);
                        break;
                    case DOWN_EAST:
                        dragonfix$set(rotatable, ForgeDirection.EAST, ForgeDirection.DOWN);
                        break;
                    case EAST_UP:
                        dragonfix$set(rotatable, ForgeDirection.EAST, ForgeDirection.NORTH);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private static void dragonfix$set(Rotatable rotatable, ForgeDirection yaw, ForgeDirection pitch) {
        rotatable.yaw_$eq(yaw);
        rotatable.pitch_$eq(pitch);
    }
}
