package com.dragonfix.mattermanipulator.persistent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.DragonFixComputerComponentItemProvider;
import com.dragonfix.mattermanipulator.analysis.AE2CondenserAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.AvaritiaddonsExtremeAutoCrafterAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.CarpentersBlocksAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.DragonFixMultipartAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.EnderIOSoulBinderAnalysisResult;
import com.dragonfix.mattermanipulator.analysis.LittleTilesAnalysisResult;
import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;
import com.gtnewhorizon.gtnhlib.util.data.ItemMeta;
import com.recursive_pineapple.matter_manipulator.common.building.AEAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.ArchitectureCraftAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer;
import com.recursive_pineapple.matter_manipulator.common.building.BlockAnalyzer.RegionAnalysis;
import com.recursive_pineapple.matter_manipulator.common.building.BlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.GTAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.ITileAnalysisIntegration;
import com.recursive_pineapple.matter_manipulator.common.building.ImmutableBlockSpec;
import com.recursive_pineapple.matter_manipulator.common.building.MultipartAnalysisResult;
import com.recursive_pineapple.matter_manipulator.common.building.PendingBlock;
import com.recursive_pineapple.matter_manipulator.common.building.PortableItemStack;
import com.recursive_pineapple.matter_manipulator.common.building.providers.AECellItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.BatteryItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.IItemProvider;
import com.recursive_pineapple.matter_manipulator.common.building.providers.PatternItemProvider;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMConfig;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Transform;
import com.recursive_pineapple.matter_manipulator.common.persist.NBTJsonAdapter;
import com.recursive_pineapple.matter_manipulator.common.persist.StaticEnumJsonAdapter;
import com.recursive_pineapple.matter_manipulator.common.persist.UIDJsonAdapter;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PersistentSchematic {

    public static final String EXTENSION = ".mmschematic";
    private static final String DIRECTORY = "matter-manipulator/schematics";
    private static final int DATA_VERSION = 2;
    private static final String ARRAY_MARKER = "dragonfixArray";
    private static final String ARRAY_LENGTH = "length";
    private static final String ARRAY_VALUES = "values";

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UniqueIdentifier.class, new UIDJsonAdapter())
        .registerTypeAdapter(NBTTagCompound.class, new NBTJsonAdapter())
        .registerTypeAdapter(ForgeDirection.class, new StaticEnumJsonAdapter<>(ForgeDirection.class))
        .registerTypeAdapter(ImmutableBlockSpec.class, new ImmutableBlockSpecAdapter())
        .registerTypeAdapter(ImmutableItemMeta.class, new ImmutableItemMetaAdapter())
        .registerTypeAdapter(ITileAnalysisIntegration.class, integrations())
        .registerTypeAdapter(IItemProvider.class, itemProviders())
        .create();

    private static final Map<String, CacheEntry> CACHE = new Object2ObjectOpenHashMap<>();

    public int dataVersion = DATA_VERSION;
    public Vector3i deltas;
    public List<PendingBlock> blocks = new ArrayList<>();

    public static File getDirectory() {
        return new File(
            Loader.instance()
                .getConfigDir(),
            DIRECTORY);
    }

    public static List<String> listFileNames() {
        File[] files = getDirectory().listFiles(
            (dir, name) -> name.toLowerCase(Locale.ROOT)
                .endsWith(EXTENSION));
        ArrayList<String> names = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) names.add(file.getName());
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static String normalizeFileName(String raw) {
        String name = raw == null ? "" : raw.trim();

        name = name.replace('\\', '_')
            .replace('/', '_')
            .replace(':', '_');

        while (name.startsWith(".")) {
            name = name.substring(1);
        }

        if (name.isEmpty()) name = "selection";

        if (!name.toLowerCase(Locale.ROOT)
            .endsWith(EXTENSION)) {
            name += EXTENSION;
        }

        return name;
    }

    public static File resolveFile(String raw) throws IOException {
        File dir = getDirectory().getCanonicalFile();
        File file = new File(dir, normalizeFileName(raw)).getCanonicalFile();

        if (!file.toPath()
            .startsWith(dir.toPath())) {
            throw new IOException("Invalid schematic file path");
        }

        return file;
    }

    public static PersistentSchematic capture(World world, Location coordA, Location coordB) throws IOException {
        if (!Location.areCompatible(coordA, coordB) || !coordA.isInWorld(world)) {
            throw new IOException("Copy region is invalid");
        }

        RegionAnalysis analysis = BlockAnalyzer.analyzeRegion(world, coordA, coordB, true);

        if (analysis == null) throw new IOException("Could not analyze copy region");

        PersistentSchematic schematic = new PersistentSchematic();
        schematic.deltas = analysis.deltas;
        schematic.blocks = analysis.blocks;

        return schematic;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveBytes(String fileName, byte[] bytes) throws IOException {
        File file = resolveFile(fileName);
        File dir = file.getParentFile();

        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Could not create schematic directory: " + dir);

        File temp = File.createTempFile(file.getName(), ".tmp", dir);

        try {
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(bytes);
            }
            try {
                Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            synchronized (CACHE) {
                CACHE
                    .put(file.getCanonicalPath(), new CacheEntry(file.lastModified(), file.length(), fromBytes(bytes)));
            }
        } finally {
            if (temp.exists()) temp.delete();
        }
    }

    public static byte[] readBytes(String fileName) throws IOException {
        return Files.readAllBytes(resolveFile(fileName).toPath());
    }

    public static byte[] toBytes(PersistentSchematic schematic) throws IOException {
        NBTTagCompound tag = (NBTTagCompound) MMUtils.toNbt(encodeArrays(GSON.toJsonTree(schematic)));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CompressedStreamTools.writeCompressed(tag, out);
            return out.toByteArray();
        }
    }

    public static PersistentSchematic fromBytes(byte[] bytes) throws IOException {
        NBTTagCompound tag;
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            tag = CompressedStreamTools.readCompressed(in);
        }

        return fromTag(tag);
    }

    public static PersistentSchematic load(String fileName) throws IOException {
        File file = resolveFile(fileName);

        if (!file.isFile()) throw new IOException("Schematic file does not exist: " + file.getName());

        String key = file.getCanonicalPath();
        long lastModified = file.lastModified();
        long length = file.length();

        synchronized (CACHE) {
            CacheEntry cached = CACHE.get(key);
            if (cached != null && cached.lastModified == lastModified && cached.length == length)
                return cached.schematic;
        }

        NBTTagCompound tag;
        try (FileInputStream in = new FileInputStream(file)) {
            tag = CompressedStreamTools.readCompressed(in);
        }

        PersistentSchematic schematic = fromTag(tag);

        synchronized (CACHE) {
            CACHE.put(key, new CacheEntry(lastModified, length, schematic));
        }

        return schematic;
    }

    private static PersistentSchematic fromTag(NBTTagCompound tag) throws IOException {
        PersistentSchematic schematic = GSON
            .fromJson(decodeArrays(MMUtils.toJsonObject(tag)), PersistentSchematic.class);

        if (schematic == null || schematic.deltas == null || schematic.blocks == null) {
            throw new IOException("Invalid Matter Manipulator schematic");
        }

        if (schematic.dataVersion != DATA_VERSION) {
            throw new IOException("Unsupported schematic data version: " + schematic.dataVersion);
        }

        for (PendingBlock block : schematic.blocks) {
            if (block == null || block.spec == null) throw new IOException("Invalid block entry in schematic");
            block.migrate();
        }

        return schematic;
    }

    public List<PendingBlock> getPendingBlocks(int worldId, Vector3i origin, Transform transform, Vector3i arraySpan) {
        Transform currentTransform = transform == null ? new Transform() : transform;

        currentTransform.cacheRotation();

        try {
            ArrayList<PendingBlock> transformedBase = new ArrayList<>(blocks.size());

            for (PendingBlock original : blocks) {
                try {
                    PendingBlock block = original.clone();
                    Vector3i v = currentTransform.apply(block.toVec());

                    block.worldId = worldId;
                    block.x = v.x;
                    block.y = v.y;
                    block.z = v.z;
                    block.transform(currentTransform);
                    block.x += origin.x;
                    block.y += origin.y;
                    block.z += origin.z;

                    transformedBase.add(block);
                } catch (Exception e) {
                    DragonFix.LOG.debug(
                        "Could not transform persistent schematic block at {},{},{}",
                        original.x,
                        original.y,
                        original.z,
                        e);
                }
            }

            if (arraySpan == null) return transformedBase;

            ArrayList<PendingBlock> out = new ArrayList<>(transformedBase.size() * arrayVolume(arraySpan));

            for (int y = Math.min(arraySpan.y, 0); y <= Math.max(arraySpan.y, 0); y++) {
                for (int z = Math.min(arraySpan.z, 0); z <= Math.max(arraySpan.z, 0); z++) {
                    for (int x = Math.min(arraySpan.x, 0); x <= Math.max(arraySpan.x, 0); x++) {
                        int dx = x * (deltas.x + (deltas.x < 0 ? -1 : 1));
                        int dy = y * (deltas.y + (deltas.y < 0 ? -1 : 1));
                        int dz = z * (deltas.z + (deltas.z < 0 ? -1 : 1));

                        Vector3i d = new Vector3i(dx, dy, dz);
                        currentTransform.apply(d);

                        for (PendingBlock base : transformedBase) {
                            PendingBlock dup = base.clone();
                            dup.x += d.x;
                            dup.y += d.y;
                            dup.z += d.z;
                            out.add(dup);
                        }
                    }
                }
            }

            return out;
        } finally {
            currentTransform.uncacheRotation();
        }
    }

    public MMConfig.VoxelAABB getPasteVisualDeltas(int worldId, Location paste, Transform transform,
        Vector3i arraySpan) {
        if (paste == null || paste.worldId != worldId) return null;

        MMConfig.VoxelAABB aabb = new MMConfig.VoxelAABB(new Vector3i(0), new Vector3i(deltas));
        aabb.moveOrigin(paste.toVec());

        if (arraySpan != null) {
            aabb.scale(arraySpan.x, arraySpan.y, arraySpan.z);
        }

        if (transform != null) {
            transform.apply(aabb);
        }

        return aabb;
    }

    public Vector3i getArrayMult(World world, Location paste, Vector3i lookingAt, Transform transform) {
        if (paste == null || !paste.isInWorld(world)) return new Vector3i(1);

        Location sourceA = new Location(world, 0, 0, 0);
        Location sourceB = new Location(world, deltas.x, deltas.y, deltas.z);

        MMConfig config = new MMConfig();
        config.transform = transform;

        return config.getArrayMult(world, sourceA, sourceB, paste, lookingAt);
    }

    public String describe() {
        return String.format(
            "%d blocks, %s",
            blocks.size(),
            new MMConfig.VoxelAABB(new Vector3i(0), new Vector3i(deltas)).describe());
    }

    public static void sendSaveResult(EntityPlayer player, String name, int blocks) {
        sendInfo(
            player,
            String.format("Saved Matter Manipulator schematic '%s' (%d blocks).", normalizeFileName(name), blocks));
    }

    public static void sendLoadResult(EntityPlayer player, String name, PersistentSchematic schematic) {
        sendInfo(
            player,
            String.format(
                "Loaded Matter Manipulator schematic '%s' (%s).",
                normalizeFileName(name),
                schematic.describe()));
    }

    public static void sendError(EntityPlayer player, String message) {
        if (player != null && message != null) {
            player.addChatComponentMessage(new ChatComponentText(MMUtils.RED + message));
        }
    }

    private static void sendInfo(EntityPlayer player, String message) {
        if (player != null && message != null) {
            player.addChatComponentMessage(new ChatComponentText(MMUtils.GRAY + message));
        }
    }

    private static int arrayVolume(Vector3i span) {
        return (Math.abs(span.x) + 1) * (Math.abs(span.y) + 1) * (Math.abs(span.z) + 1);
    }

    private static JsonElement encodeArrays(JsonElement element) {
        if (element == null || element.isJsonNull()) return JsonNull.INSTANCE;

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonObject out = new JsonObject();
            JsonObject values = new JsonObject();

            out.addProperty(ARRAY_MARKER, true);
            out.addProperty(ARRAY_LENGTH, array.size());

            for (int i = 0; i < array.size(); i++) {
                JsonElement value = array.get(i);

                if (value != null && !value.isJsonNull()) {
                    values.add(Integer.toString(i), encodeArrays(value));
                }
            }

            out.add(ARRAY_VALUES, values);
            return out;
        }

        if (element.isJsonObject()) {
            JsonObject out = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject()
                .entrySet()) {
                out.add(entry.getKey(), encodeArrays(entry.getValue()));
            }

            return out;
        }

        return element;
    }

    private static JsonElement decodeArrays(JsonElement element) {
        if (element == null || element.isJsonNull()) return JsonNull.INSTANCE;

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            if (object.has(ARRAY_MARKER) && object.get(ARRAY_MARKER)
                .getAsBoolean()) {
                int length = object.get(ARRAY_LENGTH)
                    .getAsInt();
                JsonObject values = object.has(ARRAY_VALUES) && object.get(ARRAY_VALUES)
                    .isJsonObject() ? object.getAsJsonObject(ARRAY_VALUES) : new JsonObject();
                JsonArray array = new JsonArray();

                for (int i = 0; i < length; i++) {
                    JsonElement value = values.get(Integer.toString(i));
                    array.add(value == null ? JsonNull.INSTANCE : decodeArrays(value));
                }

                return array;
            }

            JsonObject out = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                out.add(entry.getKey(), decodeArrays(entry.getValue()));
            }

            return out;
        }

        return element;
    }

    private static RuntimeTypeAdapter<ITileAnalysisIntegration> integrations() {
        return new RuntimeTypeAdapter<ITileAnalysisIntegration>().register("gt", GTAnalysisResult.class)
            .register("ae", AEAnalysisResult.class)
            .register("arch", ArchitectureCraftAnalysisResult.class)
            .register("mp", MultipartAnalysisResult.class)
            .register("df_mp", DragonFixMultipartAnalysisResult.class)
            .register("df_lt", LittleTilesAnalysisResult.class)
            .register("df_cb", CarpentersBlocksAnalysisResult.class)
            .register("df_avaritia", AvaritiaddonsExtremeAutoCrafterAnalysisResult.class)
            .register("df_ae2_condenser", AE2CondenserAnalysisResult.class)
            .register("df_enderio_soul_binder", EnderIOSoulBinderAnalysisResult.class);
    }

    private static RuntimeTypeAdapter<IItemProvider> itemProviders() {
        return new RuntimeTypeAdapter<IItemProvider>().register("stack", PortableItemStack.class)
            .register("ae_cell", AECellItemProvider.class)
            .register("battery", BatteryItemProvider.class)
            .register("pattern", PatternItemProvider.class)
            .register("df_oc_component", DragonFixComputerComponentItemProvider.class);
    }

    @Desugar
    private record CacheEntry(long lastModified, long length, PersistentSchematic schematic) {

    }

    private static class RuntimeTypeAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

        private final Map<String, Class<? extends T>> byId = new Object2ObjectOpenHashMap<>();
        private final Map<Class<?>, String> byClass = new Object2ObjectOpenHashMap<>();

        RuntimeTypeAdapter<T> register(String id, Class<? extends T> type) {
            byId.put(id, type);
            byClass.put(type, id);
            return this;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject out = new JsonObject();
            String id = byClass.get(src.getClass());

            if (id == null) throw new JsonParseException(
                "Unsupported schematic data type: " + src.getClass()
                    .getName());

            out.addProperty("type", id);
            out.add("data", context.serialize(src, src.getClass()));
            return out;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            String id = obj.get("type")
                .getAsString();
            Class<? extends T> type = byId.get(id);

            if (type == null) throw new JsonParseException("Unsupported schematic data type: " + id);

            return context.deserialize(obj.get("data"), type);
        }
    }

    private static class ImmutableBlockSpecAdapter
        implements JsonSerializer<ImmutableBlockSpec>, JsonDeserializer<ImmutableBlockSpec> {

        @Override
        public JsonElement serialize(ImmutableBlockSpec src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, BlockSpec.class);
        }

        @Override
        public ImmutableBlockSpec deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonElement normalized = json;

            if (json != null && json.isJsonObject()) {
                JsonObject object = new JsonObject();

                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject()
                    .entrySet()) {
                    object.add(entry.getKey(), entry.getValue());
                }

                JsonElement isBlock = object.get("b");

                if (isBlock != null && isBlock.isJsonPrimitive()) {
                    JsonPrimitive primitive = isBlock.getAsJsonPrimitive();

                    if (primitive.isString()) {
                        object.addProperty("b", Boolean.parseBoolean(primitive.getAsString()));
                    }
                }

                normalized = object;
            }

            BlockSpec spec = context.deserialize(normalized, BlockSpec.class);
            spec.populate();
            return spec;
        }
    }

    private static class ImmutableItemMetaAdapter
        implements JsonSerializer<ImmutableItemMeta>, JsonDeserializer<ImmutableItemMeta> {

        @Override
        public JsonElement serialize(ImmutableItemMeta src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.add(
                "id",
                context.serialize(GameRegistry.findUniqueIdentifierFor(src.getItem()), UniqueIdentifier.class));
            obj.addProperty("meta", src.getItemMeta());
            return obj;
        }

        @Override
        public ImmutableItemMeta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            UniqueIdentifier id = context.deserialize(obj.get("id"), UniqueIdentifier.class);
            Item item = GameRegistry.findItem(id.modId, id.name);

            if (item == null) throw new JsonParseException("Unknown item in schematic: " + id);

            return new ItemMeta(
                item,
                obj.get("meta")
                    .getAsInt());
        }
    }
}
