package com.dragonfix.mattermanipulator.persistent.network.packets;

import net.minecraft.entity.player.EntityPlayer;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;

import io.netty.buffer.ByteBuf;

public class ModePacket extends ServerPacket {

    public PersistentSchematicMode mode = PersistentSchematicMode.NONE;
    public boolean resetPasteSession;

    @Override
    public byte getPacketID() {
        return PersistentSchematicNetwork.PACKET_MODE;
    }

    @Override
    public void encode(ByteBuf buffer) {
        buffer.writeByte(mode.ordinal());
        buffer.writeBoolean(resetPasteSession);
    }

    @Override
    public MMPacket decode(ByteArrayDataInput buffer) {
        ModePacket packet = new ModePacket();
        int ordinal = buffer.readByte();
        packet.mode = ordinal < 0 || ordinal >= PersistentSchematicMode.values().length ? PersistentSchematicMode.NONE
            : PersistentSchematicMode.values()[ordinal];
        packet.resetPasteSession = buffer.readBoolean();
        return packet;
    }

    @Override
    protected void handle(EntityPlayer player, MMState state) {
        if (resetPasteSession) {
            PersistentSchematicState.resetPasteSession(state);
            return;
        }

        PersistentSchematicState.enterMode(state, player.worldObj, mode, null, null);
    }
}
