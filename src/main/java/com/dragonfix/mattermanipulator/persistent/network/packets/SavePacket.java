package com.dragonfix.mattermanipulator.persistent.network.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.dragonfix.mattermanipulator.persistent.network.SchematicTransfer;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

public class SavePacket extends FileNamePacket {

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_SAVE;
    }

    @Override
    protected FileNamePacket newPacket() {
        return new SavePacket();
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) throws Exception {
        PersistentSchematic schematic = PersistentSchematic
            .capture(player.worldObj, state.config.coordA, state.config.coordB);

        PersistentSchematicState.enterMode(state, player.worldObj, PersistentSchematicMode.COPY, fileName, null);

        String schematicFileName = PersistentSchematic.normalizeFileName(fileName);
        int blocks = schematic.blocks.size();
        EntityPlayerMP targetPlayer = (EntityPlayerMP) player;

        PersistentSchematicNetwork.ioExecutor.execute(() -> {
            try {
                SchematicTransfer.sendChunksToPlayer(
                    schematicFileName,
                    PersistentSchematic.toBytes(schematic),
                    blocks,
                    targetPlayer);
            } catch (Exception e) {
                DragonFix.LOG.warn("Could not serialize Matter Manipulator schematic for client", e);
                MMUtils.sendErrorToPlayer(
                    targetPlayer,
                    "Could not serialize Matter Manipulator schematic: " + e.getMessage());
            }
        });
    }
}
