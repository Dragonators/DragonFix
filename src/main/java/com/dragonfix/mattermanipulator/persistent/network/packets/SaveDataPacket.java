package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.dragonfix.mattermanipulator.persistent.network.SchematicTransfer;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public class SaveDataPacket extends SchematicChunkPacket {

    public int blocks;

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_SAVE_DATA;
    }

    @Override
    public void encode(ByteBuf buffer) {
        super.encode(buffer);
        buffer.writeInt(blocks);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        SaveDataPacket packet = (SaveDataPacket) super.decode(buffer);
        packet.blocks = buffer.readInt();
        return packet;
    }

    @Override
    protected SchematicChunkPacket newPacket() {
        return new SaveDataPacket();
    }

    @Override
    protected int extraPacketBytes() {
        return Integer.BYTES;
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) {}

    @Override
    public void process(IBlockAccess world) {
        UUID transferId = transferId();
        String schematicFileName = fileName;
        int schematicBlocks = blocks;
        int packetTotalLength = totalLength;
        int packetChunkIndex = chunkIndex;
        int packetChunkCount = chunkCount;
        byte[] packetBytes = bytes;

        PersistentSchematicNetwork.ioExecutor.execute(() -> {
            try {
                byte[] schematicBytes = SchematicTransfer
                    .acceptSaveChunk(transferId, packetTotalLength, packetChunkIndex, packetChunkCount, packetBytes);
                if (schematicBytes == null) return;

                PersistentSchematic.saveBytes(schematicFileName, schematicBytes);
                PersistentSchematicNetwork
                    .sendClientInfo(PersistentSchematic.saveResultMessage(schematicFileName, schematicBlocks));
            } catch (Exception e) {
                DragonFix.LOG.warn("Could not save Matter Manipulator schematic on client", e);
                PersistentSchematicNetwork
                    .sendClientError("Could not save Matter Manipulator schematic: " + e.getMessage());
            }
        });
    }
}
