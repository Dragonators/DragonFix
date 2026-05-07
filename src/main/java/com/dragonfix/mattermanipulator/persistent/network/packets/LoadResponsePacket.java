package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;

import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.dragonfix.mattermanipulator.persistent.network.SchematicTransfer;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public class LoadResponsePacket extends FileNamePacket {

    long contentMost;
    long contentLeast;
    boolean uploadRequired;

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_LOAD_RESPONSE;
    }

    @Override
    public void encode(ByteBuf buffer) {
        super.encode(buffer);
        buffer.writeLong(contentMost);
        buffer.writeLong(contentLeast);
        buffer.writeBoolean(uploadRequired);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        LoadResponsePacket packet = (LoadResponsePacket) super.decode(buffer);
        packet.contentMost = buffer.readLong();
        packet.contentLeast = buffer.readLong();
        packet.uploadRequired = buffer.readBoolean();
        return packet;
    }

    @Override
    protected FileNamePacket newPacket() {
        return new LoadResponsePacket();
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) {}

    @Override
    public void process(IBlockAccess world) {
        UUID contentId = new UUID(contentMost, contentLeast);
        boolean needsUpload = uploadRequired;

        PersistentSchematicNetwork.ioExecutor.execute(() -> {
            var upload = SchematicTransfer.takePendingUpload(contentId);

            if (!needsUpload) return;

            if (upload == null) {
                PersistentSchematicNetwork
                    .sendClientError("Could not upload Matter Manipulator schematic: cached upload expired");
                return;
            }

            SchematicTransfer.sendChunksToServer(upload.fileName, contentId, upload.bytes);
        });
    }
}
