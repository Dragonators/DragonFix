package com.dragonfix.mattermanipulator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

import com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints;

public final class DragonFixRenderHints {

    private static final Map<Object, Bounds> BOUNDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, CustomRenderer> CUSTOM_RENDERERS = Collections
        .synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Integer> CUSTOM_QUAD_COUNTS = Collections.synchronizedMap(new WeakHashMap<>());

    private static Constructor<?> hintConstructor;
    private static Field hintsField;
    private static Field xField;
    private static Field yField;
    private static Field zField;
    private static Field iconsField;
    private static Field tintField;
    private static boolean reflectionFailed;

    private DragonFixRenderHints() {}

    public static void addHint(int x, int y, int z, double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ, Block block, int meta, short[] tint) {
        if (reflectionFailed || !dragonfix$initReflection()) {
            RenderHints.addHint(x, y, z, block, meta, tint);
            return;
        }

        try {
            Object hint = dragonfix$newHint(x, y, z, block, meta, tint);
            BOUNDS.put(hint, new Bounds(minX, minY, minZ, maxX, maxY, maxZ));

            dragonfix$getHints().add(hint);
        } catch (ReflectiveOperationException | ClassCastException e) {
            reflectionFailed = true;
            RenderHints.addHint(x, y, z, block, meta, tint);
        }
    }

    public static void addCustomHint(int x, int y, int z, Block block, int meta, short[] tint,
        CustomRenderer renderer) {
        addCustomHint(x, y, z, block, meta, tint, renderer, 6);
    }

    public static void addCustomHint(int x, int y, int z, Block block, int meta, short[] tint, CustomRenderer renderer,
        int quadCount) {
        if (reflectionFailed || !dragonfix$initReflection()) {
            RenderHints.addHint(x, y, z, block, meta, tint);
            return;
        }

        try {
            Object hint = dragonfix$newHint(x, y, z, block, meta, tint);
            CUSTOM_RENDERERS.put(hint, renderer);
            CUSTOM_QUAD_COUNTS.put(hint, Math.max(6, quadCount));

            dragonfix$getHints().add(hint);
        } catch (ReflectiveOperationException | ClassCastException e) {
            reflectionFailed = true;
            RenderHints.addHint(x, y, z, block, meta, tint);
        }
    }

    public static Bounds getBounds(Object hint) {
        return BOUNDS.get(hint);
    }

    public static CustomRenderer getCustomRenderer(Object hint) {
        return CUSTOM_RENDERERS.get(hint);
    }

    public static long expandVboSize(long originalSize) {
        if (originalSize <= 0 || reflectionFailed || !dragonfix$initReflection()) {
            return originalSize;
        }

        try {
            ArrayList<Object> hints = dragonfix$getHints();
            int hintCount = hints.size();
            if (hintCount == 0) {
                return originalSize;
            }

            long bytesPerHint = originalSize / hintCount;
            if (bytesPerHint <= 0) {
                return originalSize;
            }

            long bytesPerQuad = bytesPerHint / 6;
            if (bytesPerQuad <= 0) {
                return originalSize;
            }

            long extraQuads = 0;
            for (Object hint : hints) {
                Integer quadCount = CUSTOM_QUAD_COUNTS.get(hint);
                if (quadCount != null && quadCount > 6) {
                    long hintExtraQuads = quadCount - 6L;
                    if (extraQuads > Long.MAX_VALUE - hintExtraQuads) {
                        return originalSize;
                    }
                    extraQuads += hintExtraQuads;
                }
            }

            if (extraQuads <= 0) {
                return originalSize;
            }

            if (extraQuads > (Long.MAX_VALUE - originalSize) / bytesPerQuad) {
                return originalSize;
            }

            return originalSize + extraQuads * bytesPerQuad;
        } catch (ReflectiveOperationException | ClassCastException e) {
            reflectionFailed = true;
            return originalSize;
        }
    }

    private static Object dragonfix$newHint(int x, int y, int z, Block block, int meta, short[] tint)
        throws ReflectiveOperationException {
        Object hint = hintConstructor.newInstance();
        IIcon[] icons = new IIcon[6];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = block.getIcon(i, meta);
        }

        xField.setInt(hint, x);
        yField.setInt(hint, y);
        zField.setInt(hint, z);
        iconsField.set(hint, icons);
        tintField.set(hint, tint);
        return hint;
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> dragonfix$getHints() throws ReflectiveOperationException {
        return (ArrayList<Object>) hintsField.get(null);
    }

    private static boolean dragonfix$initReflection() {
        if (hintConstructor != null) {
            return true;
        }

        try {
            Class<?> renderHints = RenderHints.class;
            Class<?> hint = Class
                .forName("com.recursive_pineapple.matter_manipulator.common.items.manipulator.RenderHints$Hint");

            hintConstructor = hint.getDeclaredConstructor();
            hintConstructor.setAccessible(true);

            hintsField = renderHints.getDeclaredField("HINTS");
            hintsField.setAccessible(true);

            xField = dragonfix$getField(hint, "x");
            yField = dragonfix$getField(hint, "y");
            zField = dragonfix$getField(hint, "z");
            iconsField = dragonfix$getField(hint, "icons");
            tintField = dragonfix$getField(hint, "tint");
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            reflectionFailed = true;
            return false;
        }
    }

    private static Field dragonfix$getField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static final class Bounds {

        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;

        private Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    public interface CustomRenderer {

        void draw(net.minecraft.client.renderer.Tessellator tessellator, double eyeX, double eyeY, double eyeZ,
            int eyeXint, int eyeYint, int eyeZint);
    }
}
