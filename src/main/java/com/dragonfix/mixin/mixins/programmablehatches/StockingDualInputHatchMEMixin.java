package com.dragonfix.mixin.mixins.programmablehatches;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;

@Pseudo
@Mixin(targets = "reobf.proghatches.gt.metatileentity.StockingDualInputHatchME", remap = false)
public abstract class StockingDualInputHatchMEMixin extends MTEHatchInputBus {

    public StockingDualInputHatchMEMixin(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Shadow(remap = false)
    boolean additionalConnection;

    @Shadow(remap = false)
    public abstract void l(NBTTagCompound aNBT);

    @Shadow(remap = false)
    public abstract void w(NBTTagCompound aNBT);

    @Shadow(remap = false)
    protected abstract void updateValidGridProxySides();

    @Shadow(remap = false)
    public abstract String getCopiedDataIdentifier(EntityPlayer player);

    @Shadow(remap = false)
    public abstract NBTTagCompound getCopiedData(EntityPlayer player);

    @Shadow(remap = false)
    public abstract boolean pasteCopiedData(EntityPlayer player, NBTTagCompound nbt);

    @Inject(
        method = "onLeftclick(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;Lnet/minecraft/entity/player/EntityPlayer;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$saveConfigurationToDataStick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer,
        CallbackInfo ci) {
        if (!(aPlayer instanceof EntityPlayerMP) || !aPlayer.isSneaking()) {
            return;
        }

        ItemStack dataStick = aPlayer.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            return;
        }

        dataStick.stackTagCompound = getCopiedData(aPlayer);
        dataStick.setStackDisplayName("Stocking Dual Input Hatch Configuration");
        aPlayer.addChatMessage(new ChatComponentText("Saved Stocking Dual Input Hatch Configuration to Data Stick"));
        ci.cancel();
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        if (!(aPlayer instanceof EntityPlayerMP)) {
            return super.onRightclick(aBaseMetaTileEntity, aPlayer, side, aX, aY, aZ);
        }

        ItemStack dataStick = aPlayer.inventory.getCurrentItem();
        if (!ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
            return super.onRightclick(aBaseMetaTileEntity, aPlayer, side, aX, aY, aZ);
        }

        if (!pasteCopiedData(aPlayer, dataStick.stackTagCompound)) {
            return false;
        }

        aPlayer.addChatMessage(new ChatComponentText("Loaded Stocking Dual Input Hatch Configuration from Data Stick"));
        return true;
    }

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
