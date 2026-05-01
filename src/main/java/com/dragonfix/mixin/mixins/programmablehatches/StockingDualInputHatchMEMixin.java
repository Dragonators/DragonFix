package com.dragonfix.mixin.mixins.programmablehatches;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "reobf.proghatches.gt.metatileentity.StockingDualInputHatchME", remap = false)
public abstract class StockingDualInputHatchMEMixin {

    @Shadow(remap = false)
    private boolean additionalConnection;

    @Shadow(remap = false)
    public abstract void l(NBTTagCompound aNBT);

    @Shadow(remap = false)
    public abstract void w(NBTTagCompound aNBT);

    @Shadow(remap = false)
    protected abstract void updateValidGridProxySides();

    @Shadow(remap = false)
    public abstract String getCopiedDataIdentifier(EntityPlayer player);

    @Inject(
        method = "getCopiedData(Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/nbt/NBTTagCompound;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$getCopiedData(EntityPlayer player, CallbackInfoReturnable<NBTTagCompound> cir) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", getCopiedDataIdentifier(player));
        l(tag);
        tag.setBoolean("additionalConnection", additionalConnection);
        cir.setReturnValue(tag);
    }

    @Inject(
        method = "pasteCopiedData(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/nbt/NBTTagCompound;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$pasteCopiedData(EntityPlayer player, NBTTagCompound nbt,
        CallbackInfoReturnable<Boolean> cir) {
        if (nbt == null || !getCopiedDataIdentifier(player).equals(nbt.getString("type"))) {
            cir.setReturnValue(false);
            return;
        }

        w(nbt);
        if (nbt.hasKey("additionalConnection")) {
            additionalConnection = nbt.getBoolean("additionalConnection");
            updateValidGridProxySides();
        }
        cir.setReturnValue(true);
    }
}
