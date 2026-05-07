package com.dragonfix.mattermanipulator.persistent.network.packets;

import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public abstract class FileNamePacket extends ServerPacket {

    public String fileName = "";

    @Override
    public void encode(ByteBuf buffer) {
        byte[] bytes = fileName == null ? new byte[0] : fileName.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0xFFFF) throw new IllegalArgumentException("Schematic file name is too long");
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        FileNamePacket packet = newPacket();
        int length = buffer.readUnsignedShort();
        byte[] bytes = new byte[length];
        buffer.readFully(bytes);
        packet.fileName = new String(bytes, StandardCharsets.UTF_8);
        return packet;
    }

    protected abstract FileNamePacket newPacket();
}
