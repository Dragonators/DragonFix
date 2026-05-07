package com.dragonfix.mattermanipulator.persistent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public final class PersistentSchematicSessionHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        clearInventory(event.player);
    }

    private static void clearInventory(EntityPlayer player) {
        clearStacks(player.inventory.mainInventory);
        clearStacks(player.inventory.armorInventory);
    }

    private static void clearStacks(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            PersistentSchematicState.resetPasteSessionStack(stack);
        }
    }
}
