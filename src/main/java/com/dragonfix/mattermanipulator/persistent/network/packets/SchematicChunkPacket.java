package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.dragonfix.mattermanipulator.persistent.network.SchematicTransfer;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public abstract class SchematicChunkPacket extends FileNamePacket {

    public long transferMost;
    public long transferLeast;
    public int totalLength;
    public int chunkIndex;
    public int chunkCount;
    public byte[] bytes = new byte[0];
    public int bytesOffset;
    public int bytesLength;

    @Override
    public void encode(ByteBuf buffer) {
        super.encode(buffer);
        byte[] payload = bytes == null ? new byte[0] : bytes;
        int length = bytes == null ? 0 : bytesLength;
        if (totalLength < 0 || totalLength > SchematicTransfer.MAX_SCHEMATIC_BYTES)
            throw new IllegalArgumentException("Invalid schematic payload size: " + totalLength);
        if (bytesOffset < 0 || length < 0 || bytesOffset + length > payload.length)
            throw new IllegalArgumentException("Invalid schematic chunk payload range");
        if (length > SchematicTransfer.chunkBytes(fileName, extraPacketBytes()))
            throw new IllegalArgumentException("Schematic chunk is too large");
        buffer.writeLong(transferMost);
        buffer.writeLong(transferLeast);
        buffer.writeInt(totalLength);
        buffer.writeInt(chunkIndex);
        buffer.writeInt(chunkCount);
        buffer.writeShort(length);
        buffer.writeBytes(payload, bytesOffset, length);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        SchematicChunkPacket packet = newPacket();
        int nameLength = buffer.readUnsignedShort();
        byte[] nameBytes = new byte[nameLength];
        buffer.readFully(nameBytes);
        packet.fileName = new String(nameBytes, StandardCharsets.UTF_8);
        packet.transferMost = buffer.readLong();
        packet.transferLeast = buffer.readLong();
        packet.totalLength = buffer.readInt();
        packet.chunkIndex = buffer.readInt();
        packet.chunkCount = buffer.readInt();
        int length = buffer.readUnsignedShort();
        packet.validateChunk(length);
        packet.bytes = new byte[length];
        packet.bytesOffset = 0;
        packet.bytesLength = length;
        buffer.readFully(packet.bytes);
        return packet;
    }

    protected UUID transferId() {
        return new UUID(transferMost, transferLeast);
    }

    protected int extraPacketBytes() {
        return 0;
    }

    private void validateChunk(int length) {
        if (totalLength < 0 || totalLength > SchematicTransfer.MAX_SCHEMATIC_BYTES)
            throw new IllegalArgumentException("Invalid schematic payload size: " + totalLength);
        int chunkBytes = SchematicTransfer.chunkBytes(fileName, extraPacketBytes());
        if (chunkCount != SchematicTransfer.chunkCount(fileName, totalLength, extraPacketBytes()))
            throw new IllegalArgumentException("Invalid schematic chunk count");
        if (chunkIndex < 0 || chunkIndex >= chunkCount)
            throw new IllegalArgumentException("Invalid schematic chunk index: " + chunkIndex);
        if (length < 0 || length > chunkBytes) throw new IllegalArgumentException("Invalid schematic chunk size");
        if (chunkIndex < chunkCount - 1 && length != chunkBytes)
            throw new IllegalArgumentException("Invalid non-final schematic chunk size");
        if (chunkIndex == chunkCount - 1 && length != totalLength - chunkBytes * (chunkCount - 1))
            throw new IllegalArgumentException("Invalid final schematic chunk size");
    }

    @Override
    protected abstract SchematicChunkPacket newPacket();
}
