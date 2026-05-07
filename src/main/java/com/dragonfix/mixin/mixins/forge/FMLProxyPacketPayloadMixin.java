package com.dragonfix.mixin.mixins.forge;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;

@Mixin(value = FMLProxyPacket.class, remap = false)
public abstract class FMLProxyPacketPayloadMixin {

    private static final String DRAGONFIX_MM_CHANNEL = "DragonFixMM";

    @Shadow
    @Final
    private String channel;

    @Shadow
    @Final
    private ByteBuf payload;

    /**
     * @author DragonFix
     * @reason DragonFixMM packets may use larger Netty buffers than their readable payload. Copy only readable bytes
     *         for this channel while preserving Forge's original backing-array behavior for every other mod channel.
     */
    @Overwrite(remap = false)
    public Packet toC17Packet() {
        return new C17PacketCustomPayload(channel, dragonfix$payloadBytes());
    }

    /**
     * @author DragonFix
     * @reason DragonFixMM packets may use larger Netty buffers than their readable payload. Copy only readable bytes
     *         for this channel while preserving Forge's original backing-array behavior for every other mod channel.
     */
    @Overwrite(remap = false)
    public Packet toS3FPacket() {
        return new S3FPacketCustomPayload(channel, dragonfix$payloadBytes());
    }

    private byte[] dragonfix$payloadBytes() {
        if (!DRAGONFIX_MM_CHANNEL.equals(channel)) {
            return payload.array();
        }

        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);
        return bytes;
    }
}
