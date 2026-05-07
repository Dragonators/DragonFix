package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public class LoadRequestPacket extends FileNamePacket {

    public long contentMost;
    public long contentLeast;

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_LOAD_REQUEST;
    }

    @Override
    public void encode(ByteBuf buffer) {
        super.encode(buffer);
        buffer.writeLong(contentMost);
        buffer.writeLong(contentLeast);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        LoadRequestPacket packet = (LoadRequestPacket) super.decode(buffer);
        packet.contentMost = buffer.readLong();
        packet.contentLeast = buffer.readLong();
        return packet;
    }

    @Override
    protected FileNamePacket newPacket() {
        return new LoadRequestPacket();
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) throws Exception {
        EntityPlayerMP targetPlayer = (EntityPlayerMP) player;
        UUID contentId = new UUID(contentMost, contentLeast);
        UUID ownerId = PersistentSchematicNetwork.playerId(player);
        String schematicFileName = fileName;

        PersistentSchematicNetwork.ioExecutor.execute(() -> {
            PersistentSchematic schematic = PersistentSchematicNetwork.getUploadedSchematic(contentId, ownerId);

            if (schematic != null) {
                PersistentSchematicNetwork.runOnServerThread(() -> {
                    PersistentSchematicNetwork.bindUploadedSchematic(targetPlayer, contentId, schematicFileName);
                    PersistentSchematic.sendLoadResult(targetPlayer, schematicFileName, schematic, true);
                });
                sendResponse(targetPlayer, contentId, false);
                return;
            }

            sendResponse(targetPlayer, contentId, true);
        });
    }

    private void sendResponse(EntityPlayerMP player, UUID contentId, boolean uploadRequired) {
        LoadResponsePacket packet = new LoadResponsePacket();
        packet.fileName = fileName;
        packet.contentMost = contentId.getMostSignificantBits();
        packet.contentLeast = contentId.getLeastSignificantBits();
        packet.uploadRequired = uploadRequired;
        PersistentSchematicNetwork.channel.sendToPlayer(packet, player);
    }
}
