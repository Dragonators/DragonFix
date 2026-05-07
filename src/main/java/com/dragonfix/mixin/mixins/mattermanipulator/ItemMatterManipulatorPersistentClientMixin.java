package com.dragonfix.mixin.mixins.mattermanipulator;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.client.PersistentSchematicClientState;
import com.dragonfix.mattermanipulator.persistent.client.PersistentSchematicGui;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.recursive_pineapple.matter_manipulator.client.gui.RadialMenuBuilder;
import com.recursive_pineapple.matter_manipulator.client.gui.RadialMenuBuilder.RadialMenuOptionBuilder;
import com.recursive_pineapple.matter_manipulator.client.gui.RadialMenuBuilder.RadialMenuOptionBuilderBranch;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.ItemMatterManipulator;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PendingAction;
import com.recursive_pineapple.matter_manipulator.common.networking.Messages;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import cpw.mods.fml.common.FMLCommonHandler;

@SuppressWarnings("ResultOfMethodCallIgnored")
@Mixin(value = ItemMatterManipulator.class, remap = false)
public abstract class ItemMatterManipulatorPersistentClientMixin {

    @Inject(
        method = { "lambda$addCommonOptions$7", "lambda$addCommonOptions$8", "lambda$addCommonOptions$10",
            "lambda$addCommonOptions$11" },
        at = @At("HEAD"),
        remap = false)
    @Dynamic("Targets ItemMatterManipulator's compiler-generated addCommonOptions click handlers.")
    private static void dragonfix$clearPersistentModeWhenSelectingNormalMode(CallbackInfo ci) {
        PersistentSchematicClientState.leaveMode(false);
        PersistentSchematicNetwork.sendModeToServer(PersistentSchematicMode.NONE);
    }

    @Inject(method = "lambda$addCommonOptions$9", at = @At("HEAD"), remap = false)
    @Dynamic("Targets ItemMatterManipulator's compiler-generated addCommonOptions copying click handler.")
    private static void dragonfix$syncPersistentCopyWhenSelectingNormalCopy(CallbackInfo ci) {
        PersistentSchematicClientState.leaveMode(true);
        PersistentSchematicNetwork.sendModeToServer(PersistentSchematicMode.NONE, true);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "addCommonOptions", at = @At("TAIL"), remap = false)
    private void dragonfix$addPersistentSchematicModeOptions(RadialMenuBuilder builder, MMState state,
        CallbackInfo ci) {
        RadialMenuOptionBuilderBranch<RadialMenuBuilder> setModeBranch = dragonfix$getSetModeBranch(builder);

        if (setModeBranch == null) return;

        setModeBranch.option()
            .label(StatCollector.translateToLocal("dragonfix.mm.gui.copying_persistent"))
            .hidden(!state.hasCap(ItemMatterManipulator.ALLOW_COPYING))
            .onClicked(() -> dragonfix$setPersistentMode(PersistentSchematicMode.COPY))
            .done()
            .option()
            .hidden(!state.hasCap(ItemMatterManipulator.ALLOW_COPYING))
            .label(StatCollector.translateToLocal("dragonfix.mm.gui.pasting_persistent"))
            .onClicked(() -> dragonfix$setPersistentMode(PersistentSchematicMode.PASTE))
            .done();
    }

    @Unique
    private static void dragonfix$setPersistentMode(PersistentSchematicMode mode) {
        PersistentSchematicClientState.setMode(mode, null);
        PersistentSchematicNetwork.sendModeToServer(mode);
    }

    @Unique
    private RadialMenuOptionBuilderBranch<RadialMenuBuilder> dragonfix$getSetModeBranch(RadialMenuBuilder builder) {
        if (builder.options.isEmpty()) return null;

        RadialMenuOptionBuilder<RadialMenuBuilder> option = builder.options.get(0);

        if (!(option instanceof RadialMenuOptionBuilderBranch)) return null;

        return (RadialMenuOptionBuilderBranch<RadialMenuBuilder>) option;
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "addCopyingOptions",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void dragonfix$replaceCopyingOptionsForPersistentMode(RadialMenuBuilder builder,
        UIBuildContext buildContext, ItemStack heldStack, MMState initialState, CallbackInfo ci) {
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) initialState.config;

        if (bridge.dragonfix$isPersistentSchematicCopy()) {
            dragonfix$addPersistentCopyingOptions(builder, buildContext);
            ci.cancel();
            return;
        }

        if (bridge.dragonfix$isPersistentSchematicPaste()) {
            dragonfix$addPersistentPastingOptions(builder, buildContext);
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "addInformation", at = @At("TAIL"), remap = false)
    private void dragonfix$addPersistentSchematicTooltip(ItemStack itemStack, EntityPlayer player, List<String> desc,
        boolean advancedItemTooltips, CallbackInfo ci) {
        MMState state = MatterManipulatorStateAccess.getState(itemStack);
        PersistentSchematicConfigBridge bridge = (PersistentSchematicConfigBridge) state.config;

        if (bridge.dragonfix$isPersistentSchematicCopy()) {
            desc.add(StatCollector.translateToLocal("dragonfix.mm.tooltip.mode.copying_persistent"));
            desc.add(
                StatCollector.translateToLocal("dragonfix.mm.tooltip.schematic") + ": "
                    + bridge.dragonfix$getPersistentSchematicFile());
        } else if (bridge.dragonfix$isPersistentSchematicPaste()) {
            desc.add(StatCollector.translateToLocal("dragonfix.mm.tooltip.mode.pasting_persistent"));
            desc.add(
                StatCollector.translateToLocal("dragonfix.mm.tooltip.schematic") + ": "
                    + bridge.dragonfix$getPersistentSchematicFile());
        }
    }

    @Unique
    private void dragonfix$addPersistentCopyingOptions(RadialMenuBuilder builder, UIBuildContext buildContext) {
        builder.option()
            .label(StatCollector.translateToLocal("mm.gui.mark_copy"))
            .onClicked(() -> Messages.MarkCopy.sendToServer())
            .done()
            .option()
            .label(StatCollector.translateToLocal("dragonfix.mm.gui.save_schematic"))
            .onClicked((menu, option, mouseButton, doubleClicked) -> {
                UIBuildContext buildContext2 = new UIBuildContext(buildContext.getPlayer());
                dragonfix$showModularGui(buildContext2, PersistentSchematicGui.createSaveWindow(buildContext2));
            })
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.edit_transform"))
            .onClicked((menu, option, mouseButton, doubleClicked) -> {
                UIBuildContext buildContext2 = new UIBuildContext(buildContext.getPlayer());
                dragonfix$showModularGui(
                    buildContext2,
                    PersistentSchematicGui.createTransformWindow(buildContext2, true, false, false));
            })
            .done();
    }

    @Unique
    private void dragonfix$addPersistentPastingOptions(RadialMenuBuilder builder, UIBuildContext buildContext) {
        builder.option()
            .label(StatCollector.translateToLocal("dragonfix.mm.gui.load_schematic"))
            .onClicked((menu, option, mouseButton, doubleClicked) -> {
                UIBuildContext buildContext2 = new UIBuildContext(buildContext.getPlayer());
                dragonfix$showModularGui(buildContext2, PersistentSchematicGui.createLoadWindow(buildContext2));
            })
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.mark_paste"))
            .onClicked(() -> Messages.MarkPaste.sendToServer())
            .done()
            .branch()
            .label(StatCollector.translateToLocal("mm.gui.edit_stack"))
            .option()
            .label(StatCollector.translateToLocal("mm.gui.reset"))
            .onClicked(() -> Messages.ResetArray.sendToServer())
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.mark"))
            .onClicked(() -> Messages.SetPendingAction.sendToServer(PendingAction.MARK_ARRAY))
            .done()
            .done()
            .branch()
            .label(StatCollector.translateToLocal("mm.gui.planning"))
            .option()
            .label(StatCollector.translateToLocal("mm.gui.cancel_auto_plans"))
            .onClicked(() -> Messages.CancelAutoPlans.sendToServer())
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.plan_all_auto"))
            .onClicked(() -> Messages.GetRequiredItems.sendToServer(MMUtils.PLAN_ALL | MMUtils.PLAN_AUTO_SUBMIT))
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.plan_all_manual"))
            .onClicked(() -> Messages.GetRequiredItems.sendToServer(MMUtils.PLAN_ALL))
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.clear_manual_plans"))
            .onClicked(() -> Messages.ClearManualPlans.sendToServer())
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.plan_missing_manual"))
            .onClicked(() -> Messages.GetRequiredItems.sendToServer(0))
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.plan_missing_auto"))
            .onClicked(() -> Messages.GetRequiredItems.sendToServer(MMUtils.PLAN_AUTO_SUBMIT))
            .done()
            .done()
            .option()
            .label(StatCollector.translateToLocal("mm.gui.edit_transform"))
            .onClicked((menu, option, mouseButton, doubleClicked) -> {
                UIBuildContext buildContext2 = new UIBuildContext(buildContext.getPlayer());
                dragonfix$showModularGui(
                    buildContext2,
                    PersistentSchematicGui.createTransformWindow(buildContext2, false, true, true));
            })
            .done();
    }

    @Unique
    private static void dragonfix$showModularGui(UIBuildContext buildContext, ModularWindow window) {
        GuiScreen screen = new ModularGui(
            new ModularUIContainer(new ModularUIContext(buildContext, null, true), window));
        FMLCommonHandler.instance()
            .showGuiScreen(screen);
    }
}
