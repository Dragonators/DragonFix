package com.dragonfix.mixin.mixins.programmablehatches;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.io.ByteArrayDataInput;

@Pseudo
@Mixin(targets = "reobf.proghatches.gt.cover.WirelessControlCover$Data", remap = false)
public abstract class WirelessControlCoverDataMixin {

    @Shadow(remap = false)
    boolean privateFreq;

    @Shadow(remap = false)
    boolean invert;

    @Shadow(remap = false)
    boolean safe;

    @Shadow(remap = false)
    boolean crashed;

    @Shadow(remap = false)
    boolean useMachineOwnerUUID;

    @Shadow(remap = false)
    int freq;

    @Shadow(remap = false)
    UUID user;

    @Shadow(remap = false)
    public int gateMode;

    @Inject(
        method = "readFromPacket(Lcom/google/common/io/ByteArrayDataInput;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$readPacketIntoCurrentData(ByteArrayDataInput aBuf, CallbackInfo ci) {
        privateFreq = aBuf.readBoolean();
        invert = aBuf.readBoolean();
        safe = aBuf.readBoolean();
        crashed = aBuf.readBoolean();
        freq = aBuf.readInt();
        useMachineOwnerUUID = aBuf.readBoolean();
        gateMode = aBuf.readInt();
        user = aBuf.readBoolean() ? new UUID(aBuf.readLong(), aBuf.readLong()) : null;
        ci.cancel();
    }
}
