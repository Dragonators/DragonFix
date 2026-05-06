package com.dragonfix.mattermanipulator.persistent.network;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.recursive_pineapple.matter_manipulator.MMMod;
import com.recursive_pineapple.matter_manipulator.common.networking.Network;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class PersistentSchematicNetwork {

    static final byte PACKET_MODE = 0;
    static final byte PACKET_SAVE = 1;
    static final byte PACKET_LOAD = 2;
    static final byte PACKET_SAVE_DATA = 3;
    static final int CHUNK_BYTES = 12 * 1024;
    static final int MAX_SCHEMATIC_BYTES = 64 * 1024 * 1024;
    private static final int MAX_UPLOADED_SCHEMATICS = 4;
    private static final long TRANSFER_TIMEOUT_MS = 5L * 60L * 1000L;
    private static final long UPLOADED_SCHEMATIC_TTL_MS = 30L * 60L * 1000L;
    private static final long CLEANUP_INTERVAL_MS = 30L * 1000L;

    private static Network channel;
    static final ScheduledExecutorService ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "DragonFix MM schematic IO");
        thread.setDaemon(true);
        return thread;
    });
    static final Map<String, UploadedSchematic> uploadedSchematics = new Object2ObjectOpenHashMap<>();
    static final Map<String, ChunkTransfer> incomingLoadTransfers = new Object2ObjectOpenHashMap<>();
    static final Map<String, ChunkTransfer> incomingSaveTransfers = new Object2ObjectOpenHashMap<>();

    private PersistentSchematicNetwork() {}

    public static void init() {
        if (channel == null) {
            channel = new Network("DragonFixMM", PersistentSchematicPackets.types());
            ioExecutor.scheduleWithFixedDelay(
                PersistentSchematicNetwork::cleanupSafely,
                CLEANUP_INTERVAL_MS,
                CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        }
    }

    public static void sendModeToServer(PersistentSchematicMode mode) {
        init();
        PersistentSchematicPackets.ModePacket packet = new PersistentSchematicPackets.ModePacket();
        packet.mode = mode;
        channel.sendToServer(packet);
    }

    public static void sendSaveToServer(String fileName) {
        init();
        PersistentSchematicPackets.SavePacket packet = new PersistentSchematicPackets.SavePacket();
        packet.fileName = fileName;
        channel.sendToServer(packet);
    }

    public static boolean sendLoadToServer(String fileName) {
        init();
        String normalizedFileName = PersistentSchematic.normalizeFileName(fileName);

        ioExecutor.execute(() -> {
            try {
                sendChunksToServer(normalizedFileName, PersistentSchematic.readBytes(normalizedFileName));
            } catch (Exception e) {
                runOnClientThread(() -> {
                    EntityPlayer player = MMMod.proxy.getThePlayer();
                    if (player != null) {
                        PersistentSchematic
                            .sendError(player, "Could not read Matter Manipulator schematic: " + e.getMessage());
                    }
                });
                DragonFix.LOG.warn("Could not read Matter Manipulator schematic for upload", e);
            }
        });
        return true;
    }

    public static PersistentSchematic getUploadedSchematic(String id) {
        synchronized (uploadedSchematics) {
            if (id != null && !id.isEmpty()) {
                UploadedSchematic uploaded = uploadedSchematics.get(id);

                if (uploaded != null) {
                    uploaded.touch(System.currentTimeMillis());
                    return uploaded.schematic;
                }
            }

            return null;
        }
    }

    public static PersistentSchematic getAvailableSchematic(String id, String fileName, World world)
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

        synchronized (incomingLoadTransfers) {
            cleanupExpiredTransfers(incomingLoadTransfers, now);
        }
        synchronized (incomingSaveTransfers) {
            cleanupExpiredTransfers(incomingSaveTransfers, now);
        }
        synchronized (uploadedSchematics) {
            cleanupUploadedSchematics(now);
        }
    }

    private static void cleanupExpiredTransfers(Map<String, ChunkTransfer> transfers, long now) {
        transfers.entrySet()
            .removeIf(entry -> now - entry.getValue().lastUpdatedMs > TRANSFER_TIMEOUT_MS);
    }

    static void cleanupUploadedSchematics(long now) {
        uploadedSchematics.entrySet()
            .removeIf(entry -> now - entry.getValue().lastAccessMs > UPLOADED_SCHEMATIC_TTL_MS);

        while (uploadedSchematics.size() > MAX_UPLOADED_SCHEMATICS) {
            String oldestKey = null;
            long oldestAccess = Long.MAX_VALUE;

            for (Map.Entry<String, UploadedSchematic> entry : uploadedSchematics.entrySet()) {
                long lastAccess = entry.getValue().lastAccessMs;

                if (lastAccess < oldestAccess) {
                    oldestAccess = lastAccess;
                    oldestKey = entry.getKey();
                }
            }

            if (oldestKey == null) return;
            uploadedSchematics.remove(oldestKey);
        }
    }

    private static void sendChunksToServer(String fileName, byte[] bytes) {
        UUID transferId = UUID.randomUUID();
        int chunkCount = chunkCount(bytes.length);

        for (int i = 0; i < chunkCount; i++) {
            PersistentSchematicPackets.LoadPacket packet = new PersistentSchematicPackets.LoadPacket();
            fillChunkPacket(packet, fileName, transferId, bytes, i, chunkCount);
            channel.sendToServer(packet);
        }
    }

    static void sendChunksToPlayer(String fileName, byte[] bytes, int blocks, EntityPlayerMP player) {
        UUID transferId = UUID.randomUUID();
        int chunkCount = chunkCount(bytes.length);

        for (int i = 0; i < chunkCount; i++) {
            PersistentSchematicPackets.SaveDataPacket packet = new PersistentSchematicPackets.SaveDataPacket();
            fillChunkPacket(packet, fileName, transferId, bytes, i, chunkCount);
            packet.blocks = blocks;
            channel.sendToPlayer(packet, player);
        }
    }

    private static void fillChunkPacket(PersistentSchematicPackets.SchematicChunkPacket packet, String fileName,
        UUID transferId, byte[] bytes, int chunkIndex, int chunkCount) {
        int offset = chunkIndex * CHUNK_BYTES;
        int length = Math.min(CHUNK_BYTES, bytes.length - offset);
        packet.fileName = fileName;
        packet.transferMost = transferId.getMostSignificantBits();
        packet.transferLeast = transferId.getLeastSignificantBits();
        packet.totalLength = bytes.length;
        packet.chunkIndex = chunkIndex;
        packet.chunkCount = chunkCount;
        packet.bytes = new byte[length];
        System.arraycopy(bytes, offset, packet.bytes, 0, length);
    }

    static int chunkCount(int length) {
        return Math.max(1, (length + CHUNK_BYTES - 1) / CHUNK_BYTES);
    }

    static void runOnClientThread(Runnable action) {
        ClientThread.run(action);
    }

    @SideOnly(Side.CLIENT)
    private static class ClientThread {

        static void run(Runnable action) {
            net.minecraft.client.Minecraft.getMinecraft()
                .func_152344_a(action);
        }
    }

}
