package com.dragonfix.mattermanipulator.persistent.network;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;

final class UploadedSchematic {

    final PersistentSchematic schematic;
    long lastAccessMs;

    UploadedSchematic(PersistentSchematic schematic) {
        this.schematic = schematic;
        touch(System.currentTimeMillis());
    }

    void touch(long now) {
        lastAccessMs = now;
    }
}

final class ChunkTransfer {

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
