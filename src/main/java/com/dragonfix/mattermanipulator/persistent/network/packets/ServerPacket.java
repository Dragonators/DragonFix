package com.dragonfix.mattermanipulator.persistent.network.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.IBlockAccess;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

public abstract class ServerPacket extends MMPacket {

    protected EntityPlayerMP player;

    @Override
    public void setINetHandler(INetHandler handler) {
        player = handler instanceof NetHandlerPlayServer server ? server.playerEntity : null;
    }

    @Override
    public void process(IBlockAccess world) {
        if (player == null) return;

        ItemStack held = player.inventory.getCurrentItem();
        if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return;

        MMState state = MatterManipulatorStateAccess.getState(held);

        try {
            handle(player, state);
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not process MatterManipulator schematic packet", e);
            String message = "Could not process Matter Manipulator schematic: " + e.getMessage();
            MMUtils.sendErrorToPlayer(player, message);
        }

        MatterManipulatorStateAccess.setState(held, state);
    }

    protected abstract void handle(EntityPlayer player, MMState state) throws Exception;
}
