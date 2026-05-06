package com.dragonfix.mattermanipulator.persistent.client;

import static com.recursive_pineapple.matter_manipulator.common.utils.MMUtils.BLUE;
import static com.recursive_pineapple.matter_manipulator.common.utils.MMUtils.GREEN;
import static com.recursive_pineapple.matter_manipulator.common.utils.MMUtils.RED;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import org.joml.Vector3i;
import org.lwjgl.input.Keyboard;

import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;
import com.gtnewhorizons.modularui.api.drawable.AdaptableUITexture;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.OffsetDrawable;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.CrossAxisAlignment;
import com.gtnewhorizons.modularui.api.math.MainAxisAlignment;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.widget.Column;
import com.gtnewhorizons.modularui.common.widget.DynamicTextWidget;
import com.gtnewhorizons.modularui.common.widget.ListWidget;
import com.gtnewhorizons.modularui.common.widget.MultiChildWidget;
import com.gtnewhorizons.modularui.common.widget.Row;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.VanillaButtonWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.NumericWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;
import com.recursive_pineapple.matter_manipulator.client.gui.DirectionDrawable;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.recursive_pineapple.matter_manipulator.common.networking.Messages;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PersistentSchematicGui {

    private static final IDrawable[] BACKGROUND = { new Rectangle().setColor(0xFF888888),
        new OffsetDrawable(new Rectangle().setColor(0xFF111111), 2, 2, -4, -4), };

    private static final AdaptableUITexture DISPLAY = AdaptableUITexture
        .of("modularui:gui/background/display", 143, 75, 2);

    private PersistentSchematicGui() {}

    public static ModularWindow createSaveWindow(UIBuildContext buildContext) {
        buildContext.setShowNEI(false);

        ModularWindow.Builder builder = ModularWindow.builder(new Size(220, 92));
        String[] name = { "selection" };
        TextFieldWidget nameField = new TextFieldWidget() {

            @Override
            public boolean onKeyPressed(char character, int keyCode) {
                if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                    PersistentSchematicNetwork.sendSaveToServer(getText());
                    PersistentSchematicClientState.setMode(PersistentSchematicMode.COPY, getText());
                    buildContext.getPlayer()
                        .closeScreen();
                    return true;
                }
                return super.onKeyPressed(character, keyCode);
            }
        };

        builder.widget(
            new Column().setAlignment(MainAxisAlignment.CENTER, CrossAxisAlignment.CENTER)
                .widget(
                    new TextWidget(StatCollector.translateToLocal("dragonfix.mm.gui.save_schematic"))
                        .setTextAlignment(Alignment.Center)
                        .setDefaultColor(Color.WHITE.dark(1))
                        .setSize(180, 18))
                .widget(
                    nameField.setGetter(() -> name[0])
                        .setSetter(value -> name[0] = value)
                        .setSynced(false, false)
                        .setMaxLength(80)
                        .setTextColor(Color.WHITE.dark(1))
                        .setBackground(DISPLAY.withOffset(-2, -2, 4, 4))
                        .setSize(180, 18))
                .widget(padding(4, 4))
                .widget(
                    new Row().widgets(
                        new VanillaButtonWidget()
                            .setDisplayString(StatCollector.translateToLocal("dragonfix.mm.gui.ok"))
                            .setOnClick((clickData, widget) -> {
                                PersistentSchematicNetwork.sendSaveToServer(nameField.getText());
                                PersistentSchematicClientState
                                    .setMode(PersistentSchematicMode.COPY, nameField.getText());
                                buildContext.getPlayer()
                                    .closeScreen();
                            })
                            .setSynced(false, false)
                            .setSize(62, 18),
                        padding(8, 8),
                        new VanillaButtonWidget()
                            .setDisplayString(StatCollector.translateToLocal("dragonfix.mm.gui.cancel"))
                            .setOnClick(
                                (clickData, widget) -> buildContext.getPlayer()
                                    .closeScreen())
                            .setSynced(false, false)
                            .setSize(62, 18)))
                .setPos(10, 10)
                .setSize(200, 72));

        return builder.build();
    }

    public static ModularWindow createLoadWindow(UIBuildContext buildContext) {
        buildContext.setShowNEI(false);

        ModularWindow.Builder builder = ModularWindow.builder(new Size(260, 180));
        List<String> files = PersistentSchematic.listFileNames();
        ListWidget fileList = new ListWidget().setMaxHeight(128);

        if (files.isEmpty()) {
            fileList.addChild(
                new TextWidget(StatCollector.translateToLocal("dragonfix.mm.gui.no_schematics"))
                    .setTextAlignment(Alignment.Center)
                    .setDefaultColor(Color.WHITE.dark(1))
                    .setSize(220, 18));
        } else {
            for (String file : files) {
                fileList.addChild(
                    new Row().widgets(
                        new VanillaButtonWidget().setDisplayString(file)
                            .setOnClick((clickData, widget) -> {
                                if (PersistentSchematicNetwork.sendLoadToServer(file)) {
                                    PersistentSchematicClientState.setMode(PersistentSchematicMode.PASTE, file);
                                    buildContext.getPlayer()
                                        .closeScreen();
                                }
                            })
                            .setSynced(false, false)
                            .setSize(220, 18))
                        .setSize(230, 20));
            }
        }

        builder.widget(
            new Column().setAlignment(MainAxisAlignment.START, CrossAxisAlignment.CENTER)
                .widget(
                    new TextWidget(StatCollector.translateToLocal("dragonfix.mm.gui.load_schematic"))
                        .setTextAlignment(Alignment.Center)
                        .setDefaultColor(Color.WHITE.dark(1))
                        .setSize(220, 18))
                .widget(padding(2, 2))
                .widget(
                    fileList.setSize(238, 128)
                        .setBackground(BACKGROUND))
                .widget(padding(4, 4))
                .widget(
                    new VanillaButtonWidget()
                        .setDisplayString(StatCollector.translateToLocal("dragonfix.mm.gui.cancel"))
                        .setOnClick(
                            (clickData, widget) -> buildContext.getPlayer()
                                .closeScreen())
                        .setSynced(false, false)
                        .setSize(62, 18))
                .setPos(10, 8)
                .setSize(240, 164));

        return builder.build();
    }

    public static ModularWindow createTransformWindow(UIBuildContext buildContext, boolean includeCopyCoordinates,
        boolean includePasteCoordinates, boolean includeStackCoordinates) {
        buildContext.setShowNEI(false);

        ModularWindow.Builder builder = ModularWindow.builderFullScreen();
        builder.bindPlayerInventory(buildContext.getPlayer(), 0, -9001);

        DynamicTextWidget rotationInfo = DynamicTextWidget.dynamicString(() -> {
            MMState currState = MatterManipulatorStateAccess.getState(
                buildContext.getPlayer()
                    .getHeldItem());
            Transform transform = currState.getTransform();
            ArrayList<String> flips = new ArrayList<>();

            if (transform.flipX) flips.add("X");
            if (transform.flipY) flips.add("Y");
            if (transform.flipZ) flips.add("Z");

            return StatCollector
                .translateToLocalFormatted(
                    "mm.transform.info",
                    flips.isEmpty() ? "None" : String.join(", ", flips),
                    MMUtils.getDirectionDisplayName(transform.up),
                    MMUtils.getDirectionDisplayName(transform.forward))
                .replace("\\n", "\n");
        });

        Widget[] left = {
            new Row().widgets(
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_x-"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(EAST, false))
                    .setSynced(false, false)
                    .setSize(62, 18),
                padding(6, 6),
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_x+"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(EAST, true))
                    .setSynced(false, false)
                    .setSize(62, 18)),
            padding(10, 10),
            new Row().widgets(
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_y-"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(UP, false))
                    .setSynced(false, false)
                    .setSize(62, 18),
                padding(6, 6),
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_y+"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(UP, true))
                    .setSynced(false, false)
                    .setSize(62, 18)),
            padding(10, 10),
            new Row().widgets(
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_z-"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(SOUTH, false))
                    .setSynced(false, false)
                    .setSize(62, 18),
                padding(6, 6),
                new VanillaButtonWidget()
                    .setDisplayString(StatCollector.translateToLocal("mm.transform.button.rotate_z+"))
                    .setOnClick((clickData, widget) -> Transform.sendRotate(SOUTH, true))
                    .setSynced(false, false)
                    .setSize(62, 18)),
            padding(10, 10),
            new Row().widgets(
                new VanillaButtonWidget().setDisplayString(StatCollector.translateToLocal("mm.transform.button.flip_x"))
                    .setOnClick((clickData, widget) -> Messages.ToggleTransformFlip.sendToServer(Transform.FLIP_X))
                    .setSynced(false, false)
                    .setSize(40, 18),
                padding(5, 5),
                new VanillaButtonWidget().setDisplayString(StatCollector.translateToLocal("mm.transform.button.flip_y"))
                    .setOnClick((clickData, widget) -> Messages.ToggleTransformFlip.sendToServer(Transform.FLIP_Y))
                    .setSynced(false, false)
                    .setSize(40, 18),
                padding(5, 5),
                new VanillaButtonWidget().setDisplayString(StatCollector.translateToLocal("mm.transform.button.flip_z"))
                    .setOnClick((clickData, widget) -> Messages.ToggleTransformFlip.sendToServer(Transform.FLIP_Z))
                    .setSynced(false, false)
                    .setSize(40, 18)),
            padding(10, 10), new Row().widgets(
                new MultiChildWidget().addChild(
                    rotationInfo.setSynced(false)
                        .setTextAlignment(Alignment.CenterLeft)
                        .setDefaultColor(Color.WHITE.dark(1))
                        .setSize(80, 36)
                        .setPos(3, 0))
                    .addChild(
                        new DirectionDrawable().asWidget()
                            .setSize(30, 30)
                            .setPos(3, 36))
                    .addChild(
                        new TextWidget(RED + "X+ " + GREEN + "Y+ " + BLUE + "Z+").setSize(50, 20)
                            .setPos(34, 39))
                    .setBackground(BACKGROUND)
                    .setSize(88, 66),
                padding(2, 2),
                new Column().setAlignment(MainAxisAlignment.CENTER, CrossAxisAlignment.END)
                    .widget(
                        new VanillaButtonWidget()
                            .setDisplayString(StatCollector.translateToLocal("mm.transform.button.reset"))
                            .setOnClick((clickData, widget) -> Messages.ResetTransform.sendToServer())
                            .setSynced(false, false)
                            .setSize(40, 18))
                    .setSize(40, 66)) };

        ArrayList<Widget> right = new ArrayList<>();
        if (includeCopyCoordinates) {
            addCoordinateGroup(right, buildContext.getPlayer(), "mm.transform.header.copy", -1);
            addCoordinateGroup(right, buildContext.getPlayer(), "mm.transform.header.copy_a", 0);
            addCoordinateGroup(right, buildContext.getPlayer(), "mm.transform.header.copy_b", 1);
        }
        if (includePasteCoordinates) {
            addCoordinateGroup(right, buildContext.getPlayer(), "mm.transform.header.paste", 2);
        }
        if (includeStackCoordinates) {
            addCoordinateGroup(right, buildContext.getPlayer(), "mm.transform.header.stacking", 3);
        }

        builder.widget(
            new Row()
                .widgets(
                    padding(10, 10),
                    new Column().setAlignment(MainAxisAlignment.CENTER, CrossAxisAlignment.START)
                        .widgets(left))
                .fillParent());

        Column rightColumn = new Column();
        builder.widget(
            rightColumn.setAlignment(MainAxisAlignment.CENTER, CrossAxisAlignment.END)
                .widgets(right.toArray(new Widget[0]))
                .setPosProvider(
                    (screenSize, window, parent) -> new Pos2d(screenSize.width - rightColumn.getSize().width - 10, 0)));

        return builder.build();
    }

    private static void addCoordinateGroup(List<Widget> widgets, EntityPlayer player, String headerKey, int coord) {
        widgets.add(makeHeader(StatCollector.translateToLocal(headerKey)));
        widgets.add(padding(2, 2));
        widgets.add(makeCoordinateEditor(player, coord, 0));
        widgets.add(padding(2, 2));
        widgets.add(makeCoordinateEditor(player, coord, 1));
        widgets.add(padding(2, 2));
        widgets.add(makeCoordinateEditor(player, coord, 2));
        widgets.add(padding(10, 2));
    }

    private static Widget makeHeader(String text) {
        return new MultiChildWidget()
            .addChild(
                new MultiChildWidget().addChild(
                    new TextWidget(text).setTextAlignment(Alignment.BottomCenter)
                        .setDefaultColor(Color.WHITE.dark(1))
                        .setSize(60, 13))
                    .setBackground(BACKGROUND)
                    .setSize(60, 18)
                    .setPos((130 - 60) / 2, 0))
            .setSize(130, 18);
    }

    private static Vector3i getDefaultLocation(EntityPlayer player) {
        return MMUtils.getLookingAtLocation(player);
    }

    private static Widget makeCoordinateEditor(EntityPlayer player, int coord, int component) {
        IntSupplier getter = () -> {
            MMState currState = MatterManipulatorStateAccess.getState(player.getHeldItem());
            Vector3i location = getCoordinate(currState, player, coord);

            return switch (component) {
                case 0 -> location.x;
                case 1 -> location.y;
                case 2 -> location.z;
                default -> throw new IllegalArgumentException("component");
            };
        };

        IntSupplier getterVisual = () -> {
            int value = getter.getAsInt();

            if (coord == 3 && value >= 0) value++;

            return value;
        };

        IntConsumer setter = value -> {
            MMState currState = MatterManipulatorStateAccess.getState(player.getHeldItem());
            Vector3i location = getCoordinate(currState, player, coord);

            switch (component) {
                case 0 -> location.x = value;
                case 1 -> location.y = value;
                case 2 -> location.z = value;
                default -> throw new IllegalArgumentException("component");
            }

            switch (coord) {
                case -1 -> {
                    if (currState.config.coordA != null) {
                        currState.config.coordA = new Location(
                            player.worldObj,
                            currState.config.coordA.toVec()
                                .add(location));
                        Messages.SetA.sendToServer(currState.config.coordA.toVec());
                    }
                    if (currState.config.coordB != null) {
                        currState.config.coordB = new Location(
                            player.worldObj,
                            currState.config.coordB.toVec()
                                .add(location));
                        Messages.SetB.sendToServer(currState.config.coordB.toVec());
                    }
                }
                case 0 -> {
                    currState.config.coordA = new Location(player.worldObj, location);
                    Messages.SetA.sendToServer(location);
                }
                case 1 -> {
                    currState.config.coordB = new Location(player.worldObj, location);
                    Messages.SetB.sendToServer(location);
                }
                case 2 -> {
                    currState.config.coordC = new Location(player.worldObj, location);
                    Messages.SetC.sendToServer(location);
                }
                case 3 -> {
                    currState.config.arraySpan = location;
                    Messages.SetArray.sendToServer(location);
                }
                default -> throw new IllegalArgumentException("coord");
            }

            MatterManipulatorStateAccess.setState(player.getHeldItem(), currState);
        };

        String componentName = switch (component) {
            case 0 -> "X";
            case 1 -> "Y";
            case 2 -> "Z";
            default -> throw new IllegalArgumentException("component");
        };

        class SizeStorage {

            public int x, y, z;
            public boolean present = false;

            public Vector3i get() {
                if (!present && GuiScreen.isCtrlKeyDown()) {
                    MMState currState = MatterManipulatorStateAccess.getState(player.getHeldItem());
                    MMConfig.VoxelAABB aabb = coord == 2 ? currState.config.getPasteVisualDeltas(player.worldObj, false)
                        : currState.config.getCopyVisualDeltas(player.worldObj);
                    Vector3i size = aabb == null ? new Vector3i(1) : aabb.size();

                    x = size.x;
                    y = size.y;
                    z = size.z;
                    present = true;
                }

                if (!GuiScreen.isCtrlKeyDown()) {
                    present = false;
                }

                return present ? new Vector3i(x, y, z) : new Vector3i(1);
            }

            public int getOffset() {
                int offset = 1;

                if (GuiScreen.isShiftKeyDown()) {
                    offset = 10;
                } else if (coord != 3 && GuiScreen.isCtrlKeyDown()) {
                    Vector3i size = get();

                    offset = switch (component) {
                        case 0 -> size.x;
                        case 1 -> size.y;
                        case 2 -> size.z;
                        default -> throw new IllegalArgumentException("component");
                    };
                } else {
                    present = false;
                }

                return offset;
            }
        }

        SizeStorage storage = new SizeStorage();

        return new Row().widgets(
            new VanillaButtonWidget().setDisplayString(componentName + " - 1")
                .setOnClick((clickData, widget) -> setter.accept(getter.getAsInt() - storage.getOffset()))
                .setSynced(false, false)
                .setSize(40, 18)
                .setTicker(
                    widget -> ((VanillaButtonWidget) widget)
                        .setDisplayString(componentName + " - " + storage.getOffset())),
            padding(5, 5),
            new MultiChildWidget().addChild(
                coord != -1 ? new NumericWidget().setSynced(false, false)
                    .setIntegerOnly(true)
                    .setGetter(getterVisual::getAsInt)
                    .setSetter(value -> setter.accept((int) value))
                    .setBounds(Integer.MIN_VALUE, Integer.MAX_VALUE)
                    .setScrollBar()
                    .setTextColor(Color.WHITE.dark(1))
                    .setBackground(DISPLAY.withOffset(-2, -2, 4, 4))
                    .setSize(36, 14)
                    .setPos(2, 2)
                    .setTicker(widget -> {
                        if (!widget.isFocused()) {
                            ((NumericWidget) widget).setValue(getterVisual.getAsInt());
                        }
                    })
                    : new TextWidget("N/A").setDefaultColor(Color.WHITE.dark(1))
                        .setBackground(BACKGROUND)
                        .setSize(40, 18))
                .setSize(40, 18),
            padding(5, 5),
            new VanillaButtonWidget().setDisplayString(componentName + " + 1")
                .setOnClick((clickData, widget) -> setter.accept(getter.getAsInt() + storage.getOffset()))
                .setSynced(false, false)
                .setSize(40, 18)
                .setTicker(
                    widget -> ((VanillaButtonWidget) widget)
                        .setDisplayString(componentName + " + " + storage.getOffset())))
            .setSize(130, 18);
    }

    private static Vector3i getCoordinate(MMState state, EntityPlayer player, int coord) {
        Vector3i location = switch (coord) {
            case -1 -> new Vector3i(0);
            case 0 -> state.config.coordA == null ? null : state.config.coordA.toVec();
            case 1 -> state.config.coordB == null ? null : state.config.coordB.toVec();
            case 2 -> state.config.coordC == null ? null : state.config.coordC.toVec();
            case 3 -> state.config.arraySpan;
            default -> throw new IllegalArgumentException("coord");
        };

        if (location != null) return location;

        return coord == 3 ? new Vector3i(0) : getDefaultLocation(player);
    }

    private static Widget padding(int width, int height) {
        return new Row().setSize(width, height);
    }
}
