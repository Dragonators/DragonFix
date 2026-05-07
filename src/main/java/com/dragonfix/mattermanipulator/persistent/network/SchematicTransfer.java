package com.dragonfix.mattermanipulator.persistent.network;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

import com.dragonfix.mattermanipulator.persistent.network.packets.LoadPacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.SaveDataPacket;
import com.dragonfix.mattermanipulator.persistent.network.packets.SchematicChunkPacket;
import com.github.bsideup.jabel.Desugar;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class SchematicTransfer {

    public static final int MAX_SCHEMATIC_BYTES = 64 * 1024 * 1024;
    private static final int MAX_CUSTOM_PAYLOAD_BYTES = 0x7FFFFF;
    // DragonFixMM packets use Forge VarShort lengths; keep chunks well below the channel ceiling.
    private static final int TARGET_PACKET_BYTES = Math.min(1024 * 1024, MAX_CUSTOM_PAYLOAD_BYTES);
    private static final int CHUNK_PACKET_OVERHEAD_BYTES = 1 + 2 + 16 + 4 + 4 + 4 + 4;
    private static final long TRANSFER_TIMEOUT_MS = 30L * 1000L;

    private static final Object2ObjectMap<LoadTransferKey, ChunkTransfer> incomingLoadTransfers = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<UUID, ChunkTransfer> incomingSaveTransfers = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<UUID, PendingUpload> pendingClientUploads = new Object2ObjectOpenHashMap<>();

    private SchematicTransfer() {}

    static void rememberPendingUpload(UUID contentId, String fileName, byte[] bytes) {
        synchronized (pendingClientUploads) {
            pendingClientUploads.put(contentId, new PendingUpload(fileName, bytes));
            cleanupPendingClientUploads(System.currentTimeMillis());
        }
    }

    public static @Nullable PendingUpload takePendingUpload(UUID contentId) {
        synchronized (pendingClientUploads) {
            return pendingClientUploads.remove(contentId);
        }
    }

    public static @Nullable byte[] acceptLoadChunk(UUID ownerId, UUID transferId, int totalLength, int chunkIndex,
        int chunkCount, byte[] bytes) {
        LoadTransferKey transferKey = new LoadTransferKey(ownerId, transferId);
        synchronized (incomingLoadTransfers) {
            ChunkTransfer transfer = acceptChunk(
                incomingLoadTransfers,
                transferKey,
                totalLength,
                chunkIndex,
                chunkCount,
                bytes);
            return transfer == null ? null : transfer.combine();
        }
    }

    public static @Nullable byte[] acceptSaveChunk(UUID transferId, int totalLength, int chunkIndex, int chunkCount,
        byte[] bytes) {
        synchronized (incomingSaveTransfers) {
            ChunkTransfer transfer = acceptChunk(
                incomingSaveTransfers,
                transferId,
                totalLength,
                chunkIndex,
                chunkCount,
                bytes);
            return transfer == null ? null : transfer.combine();
        }
    }

    private static <K> @Nullable ChunkTransfer acceptChunk(Object2ObjectMap<K, ChunkTransfer> transfers, K transferKey,
        int totalLength, int chunkIndex, int chunkCount, byte[] bytes) {
        ChunkTransfer transfer = transfers
            .computeIfAbsent(transferKey, key -> new ChunkTransfer(totalLength, chunkCount));
        if (!transfer.matches(totalLength, chunkCount))
            throw new IllegalArgumentException("Persistent schematic transfer metadata changed");
        transfer.add(chunkIndex, bytes);
        if (!transfer.isComplete()) return null;
        transfers.remove(transferKey);
        return transfer;
    }

    static void cleanup(long now) {
        synchronized (incomingLoadTransfers) {
            cleanupExpiredTransfers(incomingLoadTransfers, now);
        }
        synchronized (incomingSaveTransfers) {
            cleanupExpiredTransfers(incomingSaveTransfers, now);
        }
        synchronized (pendingClientUploads) {
            cleanupPendingClientUploads(now);
        }
    }

    private static <K> void cleanupExpiredTransfers(Object2ObjectMap<K, ChunkTransfer> transfers, long now) {
        for (var it = Object2ObjectMaps.fastIterator(transfers); it.hasNext();) {
            if (now - it.next()
                .getValue().lastUpdatedMs > TRANSFER_TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    private static void cleanupPendingClientUploads(long now) {
        for (var it = Object2ObjectMaps.fastIterator(pendingClientUploads); it.hasNext();) {
            if (now - it.next()
                .getValue().lastAccessMs > TRANSFER_TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    public static void sendChunksToServer(String fileName, UUID transferId, byte[] bytes) {
        int chunkBytes = chunkBytes(fileName, 0);
        int chunkCount = chunkCount(bytes.length, chunkBytes);

        for (int i = 0; i < chunkCount; i++) {
            LoadPacket packet = new LoadPacket();
            fillChunkPacket(packet, fileName, transferId, bytes, i, chunkCount, chunkBytes);
            PersistentSchematicNetwork.channel.sendToServer(packet);
        }
    }

    public static void sendChunksToPlayer(String fileName, byte[] bytes, int blocks, EntityPlayerMP player) {
        UUID transferId = UUID.randomUUID();
        int chunkBytes = chunkBytes(fileName, Integer.BYTES);
        int chunkCount = chunkCount(bytes.length, chunkBytes);

        for (int i = 0; i < chunkCount; i++) {
            SaveDataPacket packet = new SaveDataPacket();
            fillChunkPacket(packet, fileName, transferId, bytes, i, chunkCount, chunkBytes);
            packet.blocks = blocks;
            PersistentSchematicNetwork.channel.sendToPlayer(packet, player);
        }
    }

    private static void fillChunkPacket(SchematicChunkPacket packet, String fileName, UUID transferId, byte[] bytes,
        int chunkIndex, int chunkCount, int chunkBytes) {
        int offset = chunkIndex * chunkBytes;
        int length = Math.min(chunkBytes, bytes.length - offset);
        packet.fileName = fileName;
        packet.transferMost = transferId.getMostSignificantBits();
        packet.transferLeast = transferId.getLeastSignificantBits();
        packet.totalLength = bytes.length;
        packet.chunkIndex = chunkIndex;
        packet.chunkCount = chunkCount;
        packet.bytes = bytes;
        packet.bytesOffset = offset;
        packet.bytesLength = length;
    }

    public static int chunkBytes(String fileName, int extraPacketBytes) {
        int fileNameBytes = packetFileNameBytes(fileName);
        int chunkBytes = TARGET_PACKET_BYTES - CHUNK_PACKET_OVERHEAD_BYTES - fileNameBytes - extraPacketBytes;
        if (chunkBytes <= 0)
            throw new IllegalArgumentException("Schematic file name is too long for chunk transfer packet");
        return chunkBytes;
    }

    public static int chunkCount(String fileName, int length, int extraPacketBytes) {
        return chunkCount(length, chunkBytes(fileName, extraPacketBytes));
    }

    private static int chunkCount(int length, int chunkBytes) {
        return Math.max(1, MMUtils.ceilDiv(length, chunkBytes));
    }

    private static int packetFileNameBytes(String fileName) {
        return fileName == null ? 0 : fileName.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    public static final class PendingUpload {

        public final String fileName;
        public final byte[] bytes;
        long lastAccessMs;

        PendingUpload(String fileName, byte[] bytes) {
            this.fileName = fileName;
            this.bytes = bytes;
            touch(System.currentTimeMillis());
        }

        void touch(long now) {
            lastAccessMs = now;
        }
    }

    @Desugar
    private record LoadTransferKey(UUID playerId, UUID transferId) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LoadTransferKey other)) return false;
            return Objects.equals(playerId, other.playerId) && Objects.equals(transferId, other.transferId);
        }

    }

    private static final class ChunkTransfer {

        private final int totalLength;
        private final byte[][] chunks;
        private int received;
        long lastUpdatedMs;

        ChunkTransfer(int totalLength, int chunkCount) {
            this.totalLength = totalLength;
            this.chunks = new byte[chunkCount][];
            touch();
        }

        boolean matches(int expectedTotalLength, int expectedChunkCount) {
            return totalLength == expectedTotalLength && chunks.length == expectedChunkCount;
        }

        void add(int index, byte[] bytes) {
            touch();
            if (chunks[index] != null) return;
            chunks[index] = bytes;
            received++;
        }

        boolean isComplete() {
            return received == chunks.length;
        }

        byte[] combine() {
            byte[] out = new byte[totalLength];
            int offset = 0;

            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, out, offset, chunk.length);
                offset += chunk.length;
            }

            return out;
        }

        private void touch() {
            lastUpdatedMs = System.currentTimeMillis();
        }

    }
}
