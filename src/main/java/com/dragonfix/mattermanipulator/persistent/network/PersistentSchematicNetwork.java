package com.dragonfix.mattermanipulator.persistent.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicState;
import com.dragonfix.mattermanipulator.persistent.network.packets.LoadPacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.LoadRequestPacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.LoadResponsePacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.ModePacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.SaveDataPacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.SavePacket;
import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.recursive_pineapple.matter_manipulator.MMMod;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;
import com.recursive_pineapple.matter_manipulator.common.networking.Network;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class PersistentSchematicNetwork {

    public static final byte PACKET_MODE = 0;
    public static final byte PACKET_SAVE = 1;
    public static final byte PACKET_LOAD = 2;
    public static final byte PACKET_SAVE_DATA = 3;
    public static final byte PACKET_LOAD_REQUEST = 4;
    public static final byte PACKET_LOAD_RESPONSE = 5;
    private static final long CLEANUP_INTERVAL_MS = 5 * 60L * 1000L;

    public static Network channel;
    public static final ScheduledExecutorService ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "DragonFix MM schematic IO");
        thread.setDaemon(true);
        return thread;
    });
    private static final PersistentSchematicCache uploadedSchematicCache = new PersistentSchematicCache();
    private static final Set<UUID> clientLoadedSchematics = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private PersistentSchematicNetwork() {}

    public static void init() {
        if (channel == null) {
            channel = new Network("DragonFixMM", packetTypes());
            ioExecutor.scheduleWithFixedDelay(
                PersistentSchematicNetwork::cleanupSafely,
                CLEANUP_INTERVAL_MS,
                CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        }
    }

    private static MMPacket[] packetTypes() {
        return new MMPacket[] { new ModePacket(), new SavePacket(), new LoadPacket(), new SaveDataPacket(),
            new LoadRequestPacket(), new LoadResponsePacket() };
    }

    public static void sendModeToServer(PersistentSchematicMode mode) {
        sendModeToServer(mode, false);
    }

    public static void sendModeToServer(PersistentSchematicMode mode, boolean syncPersistentCopy) {
        init();
        ModePacket packet = new ModePacket();
        packet.mode = mode;
        packet.syncPersistentCopy = syncPersistentCopy;
        channel.sendToServer(packet);
    }

    public static void sendResetPasteSessionToServer() {
        init();
        ModePacket packet = new ModePacket();
        packet.mode = PersistentSchematicMode.PASTE;
        packet.resetPasteSession = true;
        channel.sendToServer(packet);
    }

    public static void sendSaveToServer(String fileName) {
        init();
        SavePacket packet = new SavePacket();
        packet.fileName = fileName;
        channel.sendToServer(packet);
    }

    public static void sendLoadToServer(String fileName) {
        init();
        String normalizedFileName = PersistentSchematic.normalizeFileName(fileName);

        ioExecutor.execute(() -> {
            try {
                byte[] bytes = PersistentSchematic.readBytes(normalizedFileName);
                UUID contentId = contentId(bytes);

                rememberClientLoadedSchematic(contentId);
                SchematicTransfer.rememberPendingUpload(contentId, normalizedFileName, bytes);

                sendLoadRequestToServer(normalizedFileName, contentId);
            } catch (Exception e) {
                sendClientError("Could not read Matter Manipulator schematic: " + e.getMessage());
                DragonFix.LOG.warn("Could not read Matter Manipulator schematic for upload", e);
            }
        });
    }

    public static PersistentSchematic getUploadedSchematic(UUID id) {
        return uploadedSchematicCache.get(id);
    }

    public static PersistentSchematic getUploadedSchematic(UUID id, UUID ownerId) {
        return uploadedSchematicCache.get(id, ownerId);
    }

    public static void cacheUploadedSchematic(UUID id, UUID ownerId, PersistentSchematic schematic) {
        uploadedSchematicCache.put(id, ownerId, schematic);
    }

    public static void rememberClientLoadedSchematic(UUID id) {
        if (id != null) {
            clientLoadedSchematics.add(id);
        }
    }

    public static boolean isClientLoadedSchematic(UUID id) {
        return id != null && clientLoadedSchematics.contains(id);
    }

    public static boolean restoreClientLoadedSchematic(String fileName, @Nullable UUID expectedId,
        Consumer<UUID> onSuccess, Runnable onFailure) {
        if (fileName == null || fileName.isEmpty()) return false;
        if (expectedId != null && isClientLoadedSchematic(expectedId)) return true;

        String normalizedFileName = PersistentSchematic.normalizeFileName(fileName);
        ioExecutor
            .execute(() -> restoreClientLoadedSchematicAsync(normalizedFileName, expectedId, onSuccess, onFailure));

        return true;
    }

    private static void restoreClientLoadedSchematicAsync(String fileName, @Nullable UUID expectedId,
        Consumer<UUID> onSuccess, Runnable onFailure) {
        try {
            byte[] bytes = PersistentSchematic.readBytes(fileName);
            UUID contentId = contentId(bytes);

            if (expectedId != null && !expectedId.equals(contentId)) {
                completeFailedRestore(onFailure);
                return;
            }

            rememberClientLoadedSchematic(contentId);
            SchematicTransfer.rememberPendingUpload(contentId, fileName, bytes);
            sendLoadRequestToServer(fileName, contentId);
            completeSuccessfulRestore(contentId, onSuccess);
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not restore Matter Manipulator schematic upload from local file", e);
            completeFailedRestore(onFailure);
        }
    }

    private static void completeSuccessfulRestore(UUID contentId, Consumer<UUID> onSuccess) {
        runOnClientThread(() -> onSuccess.accept(contentId));
    }

    private static void completeFailedRestore(Runnable onFailure) {
        runOnClientThread(onFailure);
    }

    public static @Nullable PersistentSchematic getAvailableSchematic(UUID id, String fileName, World world)
        throws IOException {
        PersistentSchematic schematic = getUploadedSchematic(id);
        if (schematic != null) return schematic;

        return world != null && world.isRemote && fileName != null && !fileName.isEmpty()
            ? PersistentSchematic.load(fileName)
            : null;
    }

    private static void cleanupSafely() {
        try {
            cleanupCaches();
        } catch (Exception e) {
            DragonFix.LOG.warn("Could not clean Matter Manipulator persistent schematic caches", e);
        }
    }

    private static void cleanupCaches() {
        long now = System.currentTimeMillis();

        SchematicTransfer.cleanup(now);
        uploadedSchematicCache.cleanup(now);
    }

    private static void sendLoadRequestToServer(String fileName, UUID contentId) {
        LoadRequestPacket packet = new LoadRequestPacket();
        packet.fileName = fileName;
        packet.contentMost = contentId.getMostSignificantBits();
        packet.contentLeast = contentId.getLeastSignificantBits();
        channel.sendToServer(packet);
    }

    static UUID contentId(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(bytes);
            ByteBuffer buffer = ByteBuffer.wrap(digest);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static UUID playerId(EntityPlayer player) {
        UUID id = player.getGameProfile()
            .getId();
        return id == null ? UUID.nameUUIDFromBytes(
            player.getCommandSenderName()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
            : id;
    }

    public static void bindUploadedSchematic(EntityPlayerMP player, UUID id, String fileName) {
        ItemStack held = player.inventory.getCurrentItem();
        if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return;

        MMState state = MatterManipulatorStateAccess.getState(held);
        PersistentSchematicState.enterMode(state, player.worldObj, PersistentSchematicMode.PASTE, fileName, id);
        MatterManipulatorStateAccess.setState(held, state);
    }

    public static void runOnServerThread(Runnable action) {
        try {
            ServerThreadUtil.addScheduledTask(action);
        } catch (IllegalStateException e) {
            DragonFix.LOG.warn("Could not schedule Matter Manipulator schematic server task", e);
        }
    }

    public static void runOnClientThread(Runnable action) {
        ClientThread.run(action);
    }

    public static void sendClientInfo(String message) {
        if (message == null) return;
        runOnClientThread(() -> sendClientChat(MMUtils.GRAY + message));
    }

    public static void sendClientError(String message) {
        if (message == null) return;
        runOnClientThread(() -> sendClientChat(MMUtils.RED + message));
    }

    @SideOnly(Side.CLIENT)
    private static class ClientThread {

        static void run(Runnable action) {
            net.minecraft.client.Minecraft.getMinecraft()
                .func_152344_a(action);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void sendClientChat(String message) {
        EntityPlayer player = MMMod.proxy.getThePlayer();
        if (player != null && message != null) {
            player.addChatComponentMessage(new ChatComponentText(message));
        }
    }

}
