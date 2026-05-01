package com.dragonfix.mixin.mixins.ae2things;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.enums.ItemList;
import gregtech.nei.GTNEIDefaultHandler;

@Pseudo
@Mixin(targets = "com.asdflj.ae2thing.util.GTUtil", remap = false)
public abstract class GTUtilMixin {

    @Inject(
        method = "getRecipeName(Lcodechicken/nei/recipe/IRecipeHandler;Ljava/util/List;)Ljava/lang/String;",
        at = @At("RETURN"),
        cancellable = true)
    private static void dragonfix$appendNonConsumableName(IRecipeHandler recipe, List<?> in,
        CallbackInfoReturnable<String> cir) {
        if (!(recipe instanceof GTNEIDefaultHandler)) {
            return;
        }

        String recipeName = recipe.getRecipeName();
        for (Object orderStack : in) {
            Object stack = dragonfix$getStack(orderStack);
            if (stack instanceof ItemStack itemStack) {
                if (itemStack.stackSize == 0) {
                    String originalName = String.format("%s %s", recipeName, itemStack.getItemDamage());
                    if (originalName.equals(cir.getReturnValue())) {
                        cir.setReturnValue(
                            String.format("%s %s", recipeName, dragonfix$getNonConsumableName(itemStack)));
                    }
                    return;
                }
            }
        }
    }

    @Unique
    private static Object dragonfix$getStack(Object orderStack) {
        if (orderStack == null) {
            return null;
        }
        try {
            return orderStack.getClass()
                .getMethod("getStack")
                .invoke(orderStack);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }

    @Unique
    private static String dragonfix$getNonConsumableName(ItemStack stack) {
        if (ItemList.Circuit_Integrated.isStackEqual(stack, true, true)) {
            return String.valueOf(stack.getItemDamage());
        }
        String displayName = stack.getDisplayName();
        return displayName.replaceAll("(?i)" + (char) 167 + "[0-9A-FK-OR]", "");
    }
}
