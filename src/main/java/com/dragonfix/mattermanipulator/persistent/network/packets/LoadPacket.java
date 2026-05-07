package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.dragonfix.mattermanipulator.persistent.network.SchematicTransfer;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

public class LoadPacket extends SchematicChunkPacket {

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_LOAD;
    }

    @Override
    protected SchematicChunkPacket newPacket() {
        return new LoadPacket();
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) throws Exception {
        EntityPlayerMP targetPlayer = (EntityPlayerMP) player;
        String schematicFileName = fileName;
        UUID transferId = transferId();
        UUID ownerId = PersistentSchematicNetwork.playerId(player);
        int packetTotalLength = totalLength;
        int packetChunkIndex = chunkIndex;
        int packetChunkCount = chunkCount;
        byte[] packetBytes = bytes;

        PersistentSchematicNetwork.ioExecutor.execute(() -> {
            try {
                byte[] schematicBytes = SchematicTransfer.acceptLoadChunk(
                    ownerId,
                    transferId,
                    packetTotalLength,
                    packetChunkIndex,
                    packetChunkCount,
                    packetBytes);
                if (schematicBytes == null) return;

                PersistentSchematic schematic = PersistentSchematic.fromBytes(schematicBytes);

                PersistentSchematicNetwork.cacheUploadedSchematic(transferId, ownerId, schematic);

                PersistentSchematicNetwork.runOnServerThread(() -> {
                    PersistentSchematicNetwork.bindUploadedSchematic(targetPlayer, transferId, schematicFileName);
                    PersistentSchematic.sendLoadResult(targetPlayer, schematicFileName, schematic);
                });
            } catch (Exception e) {
                DragonFix.LOG.warn("Could not load uploaded Matter Manipulator schematic", e);
                PersistentSchematicNetwork.runOnServerThread(
                    () -> MMUtils.sendErrorToPlayer(
                        targetPlayer,
                        "Could not load Matter Manipulator schematic: " + e.getMessage()));
            }
        });
    }
}
