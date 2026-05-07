package com.dragonfix.mixin.mixins.minecraft;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import cpw.mods.fml.common.network.ByteBufUtils;

@Mixin(C17PacketCustomPayload.class)
public abstract class C17PacketCustomPayloadMixin {

    private static final String DRAGONFIX_MM_CHANNEL = "DragonFixMM";
    private static final int VANILLA_MAX_CUSTOM_PAYLOAD_BYTES = 32767;
    private static final int DRAGONFIX_MAX_CUSTOM_PAYLOAD_BYTES = 0x7FFFFF;

    @Shadow
    private String field_149562_a;

    @Shadow
    private int field_149560_b;

    @Shadow
    private byte[] field_149561_c;

    @ModifyConstant(method = "<init>(Ljava/lang/String;[B)V", constant = @Constant(intValue = 32767))
    private int dragonfix$increaseConstructorPayloadLimit(int original) {
        return dragonfix$isDragonFixMMChannel() ? DRAGONFIX_MAX_CUSTOM_PAYLOAD_BYTES + 1 : original;
    }

    /**
     * @author DragonFix
     * @reason DragonFixMM uses Forge's VarShort payload length encoding for client-to-server persistent schematic
     *         packets. Other mod channels keep the vanilla signed-short format and 32 KiB limit.
     */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149562_a = data.readStringFromBuffer(20);
        if (dragonfix$isDragonFixMMChannel()) {
            field_149560_b = ByteBufUtils.readVarShort(data);
            dragonfix$validatePayloadLength(field_149560_b, DRAGONFIX_MAX_CUSTOM_PAYLOAD_BYTES);

            field_149561_c = new byte[field_149560_b];
            data.readBytes(field_149561_c);
            return;
        }

        field_149560_b = data.readShort();
        if (field_149560_b > 0 && field_149560_b < VANILLA_MAX_CUSTOM_PAYLOAD_BYTES) {
            field_149561_c = new byte[field_149560_b];
            data.readBytes(field_149561_c);
        }
    }

    /**
     * @author DragonFix
     * @reason DragonFixMM uses Forge's VarShort payload length encoding for client-to-server persistent schematic
     *         packets. Other mod channels keep the vanilla signed-short format and 32 KiB limit.
     */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(field_149562_a);

        if (dragonfix$isDragonFixMMChannel()) {
            field_149560_b = field_149561_c == null ? 0 : field_149561_c.length;
            dragonfix$validatePayloadLength(field_149560_b, DRAGONFIX_MAX_CUSTOM_PAYLOAD_BYTES);
            ByteBufUtils.writeVarShort(data, field_149560_b);
        } else {
            data.writeShort((short) field_149560_b);
        }

        if (field_149561_c != null) {
            data.writeBytes(field_149561_c);
        }
    }

    private boolean dragonfix$isDragonFixMMChannel() {
        return DRAGONFIX_MM_CHANNEL.equals(field_149562_a);
    }

    private static void dragonfix$validatePayloadLength(int length, int maxLength) throws IOException {
        if (length < 0 || length > maxLength) {
            throw new IOException("Payload may not be larger than " + maxLength + " bytes");
        }
    }
}
