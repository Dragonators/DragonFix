package com.dragonfix.mixin.mixins.mattermanipulator;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mixin(
    targets = "com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator$1SizeStorage",
    remap = false)
public abstract class ItemMatterManipulatorSizeStorageMixin {

    @Shadow(remap = false)
    public int x;

    @Shadow(remap = false)
    public int y;

    @Shadow(remap = false)
    public int z;

    @Shadow(remap = false)
    public boolean present;

    @Shadow(remap = false)
    @Final
    private EntityPlayer val$player;

    @Shadow(remap = false)
    @Final
    private int val$coord;

    /**
     * @author DragonFix
     * @reason MatterManipulator's transform editor asks for Ctrl-size offsets even when the current copy/paste
     *         selection is incomplete. In that state MMConfig correctly has no visual delta, so the editor should fall
     *         back to a one-block step instead of dereferencing a missing selection.
     */
    @Overwrite(remap = false)
    public Vector3i get() {
        if (!present && GuiScreen.isCtrlKeyDown()) {
            MMState currState = ItemMatterManipulator.getState(val$player.getHeldItem());
            MMConfig.VoxelAABB aabb = val$coord == 2 ? currState.config.getPasteVisualDeltas(val$player.worldObj, false)
                : currState.config.getCopyVisualDeltas(val$player.worldObj);
            Vector3i size = aabb == null ? new Vector3i(1) : aabb.size();

            x = size.x;
            y = size.y;
            z = size.z;
            present = true;
        }

        if (!GuiScreen.isCtrlKeyDown()) {
            present = false;
        }

        return present ? new Vector3i(x, y, z) : new Vector3i(1);
    }
}
