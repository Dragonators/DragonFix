package com.dragonfix.mattermanipulator.persistent.network;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.IBlockAccess;

import com.dragonfix.DragonFix;
import com.dragonfix.mattermanipulator.bridge.PersistentSchematicConfigBridge;
import com.dragonfix.mattermanipulator.helper.MatterManipulatorStateAccess;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;
import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;
import com.google.common.io.ByteArrayDataInput;
import com.recursive_pineapple.matter_manipulator.MMMod;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.MMState.PlaceMode;
import com.recursive_pineapple.matter_manipulator.common.networking.MMPacket;
import com.recursive_pineapple.matter_manipulator.common.utils.MMUtils;

import io.netty.buffer.ByteBuf;

final class PersistentSchematicPackets {

    private PersistentSchematicPackets() {}

    static MMPacket[] types() {
        return new MMPacket[] { new ModePacket(), new SavePacket(), new LoadPacket(), new SaveDataPacket() };
    }

    private abstract static class ServerPacket extends MMPacket {

        private EntityPlayerMP player;

        @Override
        public void setINetHandler(INetHandler handler) {
            player = handler instanceof NetHandlerPlayServer server ? server.playerEntity : null;
        }

        @Override
        public void process(IBlockAccess world) {
            if (player == null) return;

            ItemStack held = player.inventory.getCurrentItem();
            if (!MatterManipulatorStateAccess.isMatterManipulator(held)) return;

            MMState state = MatterManipulatorStateAccess.getState(held);

            try {
                handle(player, state);
            } catch (Exception e) {
                DragonFix.LOG.warn("Could not process MatterManipulator schematic packet", e);
                MMUtils.sendErrorToPlayer(player, "Could not process Matter Manipulator schematic: " + e.getMessage());
            }

            MatterManipulatorStateAccess.setState(held, state);
        }

        protected abstract void handle(EntityPlayer player, MMState state) throws Exception;
    }

    static class ModePacket extends ServerPacket {

        PersistentSchematicMode mode = PersistentSchematicMode.NONE;

        @Override
        public byte getPacketID() {
            return PersistentSchematicNetwork.PACKET_MODE;
        }

        @Override
        public void encode(ByteBuf buffer) {
            buffer.writeByte(mode.ordinal());
        }

        @Override
        public MMPacket decode(ByteArrayDataInput buffer) {
            ModePacket packet = new ModePacket();
            int ordinal = buffer.readByte();
            packet.mode = ordinal < 0 || ordinal >= PersistentSchematicMode.values().length
                ? PersistentSchematicMode.NONE
                : PersistentSchematicMode.values()[ordinal];
            return packet;
        }

        @Override
        protected void handle(EntityPlayer player, MMState state) {
            PersistentSchematicConfigBridge config = (PersistentSchematicConfigBridge) state.config;
            config.dragonfix$setPersistentSchematicMode(mode);
            config.dragonfix$setPersistentSchematicId("");

            if (mode != PersistentSchematicMode.NONE) {
                state.config.placeMode = PlaceMode.COPYING;
            }
        }
    }

    protected abstract static class FileNamePacket extends ServerPacket {

        protected String fileName = "";

        @Override
        public void encode(ByteBuf buffer) {
            byte[] bytes = fileName == null ? new byte[0] : fileName.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 0xFFFF) throw new IllegalArgumentException("Schematic file name is too long");
            buffer.writeShort(bytes.length);
            buffer.writeBytes(bytes);
        }

        @Override
        public MMPacket decode(ByteArrayDataInput buffer) {
            FileNamePacket packet = newPacket();
            int length = buffer.readUnsignedShort();
            byte[] bytes = new byte[length];
            buffer.readFully(bytes);
            packet.fileName = new String(bytes, StandardCharsets.UTF_8);
            return packet;
        }

        protected abstract FileNamePacket newPacket();
    }

    static class SavePacket extends FileNamePacket {

        @Override
        public byte getPacketID() {
            return PersistentSchematicNetwork.PACKET_SAVE;
        }

        @Override
        protected FileNamePacket newPacket() {
            return new SavePacket();
        }

        @Override
        protected void handle(EntityPlayer player, MMState state) throws Exception {
            PersistentSchematic schematic = PersistentSchematic
                .capture(player.worldObj, state.config.coordA, state.config.coordB);

            PersistentSchematicConfigBridge config = (PersistentSchematicConfigBridge) state.config;
            config.dragonfix$setPersistentSchematicMode(PersistentSchematicMode.COPY);
            config.dragonfix$setPersistentSchematicFile(PersistentSchematic.normalizeFileName(fileName));
            config.dragonfix$setPersistentSchematicId("");
            state.config.placeMode = PlaceMode.COPYING;

            String schematicFileName = PersistentSchematic.normalizeFileName(fileName);
            int blocks = schematic.blocks.size();
            EntityPlayerMP targetPlayer = (EntityPlayerMP) player;

            PersistentSchematicNetwork.ioExecutor.execute(() -> {
                try {
                    PersistentSchematicNetwork.sendChunksToPlayer(
                        schematicFileName,
                        PersistentSchematic.toBytes(schematic),
                        blocks,
                        targetPlayer);
                } catch (Exception e) {
                    DragonFix.LOG.warn("Could not serialize Matter Manipulator schematic for client", e);
                    MMUtils.sendErrorToPlayer(
                        targetPlayer,
                        "Could not serialize Matter Manipulator schematic: " + e.getMessage());
                }
            });
        }
    }

    static class LoadPacket extends SchematicChunkPacket {

        @Override
        public byte getPacketID() {
            return PersistentSchematicNetwork.PACKET_LOAD;
        }

        @Override
        protected SchematicChunkPacket newPacket() {
            return new LoadPacket();
        }

        @Override
        protected void handle(EntityPlayer player, MMState state) throws Exception {
            ChunkTransfer transfer;
            String transferKey = player.getCommandSenderName() + ":" + transferId();

            synchronized (PersistentSchematicNetwork.incomingLoadTransfers) {
                transfer = PersistentSchematicNetwork.incomingLoadTransfers
                    .computeIfAbsent(transferKey, key -> new ChunkTransfer(totalLength, chunkCount));
                if (!transfer.matches(totalLength, chunkCount))
                    throw new IllegalArgumentException("Persistent schematic transfer metadata changed");
                transfer.add(chunkIndex, bytes);
                if (!transfer.isComplete()) return;
                PersistentSchematicNetwork.incomingLoadTransfers.remove(transferKey);
            }

            String id = transferId();
            PersistentSchematicConfigBridge config = (PersistentSchematicConfigBridge) state.config;
            config.dragonfix$setPersistentSchematicMode(PersistentSchematicMode.PASTE);
            config.dragonfix$setPersistentSchematicFile(PersistentSchematic.normalizeFileName(fileName));
            config.dragonfix$setPersistentSchematicId(id);
            state.config.placeMode = PlaceMode.COPYING;

            byte[] schematicBytes = transfer.combine();
            String schematicFileName = fileName;

            PersistentSchematicNetwork.ioExecutor.execute(() -> {
                try {
                    PersistentSchematic schematic = PersistentSchematic.fromBytes(schematicBytes);

                    synchronized (PersistentSchematicNetwork.uploadedSchematics) {
                        PersistentSchematicNetwork.uploadedSchematics.put(id, new UploadedSchematic(schematic));
                        PersistentSchematicNetwork.cleanupUploadedSchematics(System.currentTimeMillis());
                    }

                    PersistentSchematic.sendLoadResult(player, schematicFileName, schematic);
                } catch (Exception e) {
                    DragonFix.LOG.warn("Could not load uploaded Matter Manipulator schematic", e);
                    MMUtils.sendErrorToPlayer(player, "Could not load Matter Manipulator schematic: " + e.getMessage());
                }
            });
        }
    }

    abstract static class SchematicChunkPacket extends FileNamePacket {

        protected long transferMost;
        protected long transferLeast;
        protected int totalLength;
        protected int chunkIndex;
        protected int chunkCount;
        protected byte[] bytes = new byte[0];

        @Override
        public void encode(ByteBuf buffer) {
            super.encode(buffer);
            byte[] payload = bytes == null ? new byte[0] : bytes;
            if (totalLength < 0 || totalLength > PersistentSchematicNetwork.MAX_SCHEMATIC_BYTES)
                throw new IllegalArgumentException("Invalid schematic payload size: " + totalLength);
            if (payload.length > PersistentSchematicNetwork.CHUNK_BYTES)
                throw new IllegalArgumentException("Schematic chunk is too large");
            buffer.writeLong(transferMost);
            buffer.writeLong(transferLeast);
            buffer.writeInt(totalLength);
            buffer.writeInt(chunkIndex);
            buffer.writeInt(chunkCount);
            buffer.writeShort(payload.length);
            buffer.writeBytes(payload);
        }

        @Override
        public MMPacket decode(ByteArrayDataInput buffer) {
            SchematicChunkPacket packet = newPacket();
            int nameLength = buffer.readUnsignedShort();
            byte[] nameBytes = new byte[nameLength];
            buffer.readFully(nameBytes);
            packet.fileName = new String(nameBytes, StandardCharsets.UTF_8);
            packet.transferMost = buffer.readLong();
            packet.transferLeast = buffer.readLong();
            packet.totalLength = buffer.readInt();
            packet.chunkIndex = buffer.readInt();
            packet.chunkCount = buffer.readInt();
            int length = buffer.readUnsignedShort();
            packet.validateChunk(length);
            packet.bytes = new byte[length];
            buffer.readFully(packet.bytes);
            return packet;
        }

        protected String transferId() {
            return new UUID(transferMost, transferLeast).toString();
        }

        private void validateChunk(int length) {
            if (totalLength < 0 || totalLength > PersistentSchematicNetwork.MAX_SCHEMATIC_BYTES)
                throw new IllegalArgumentException("Invalid schematic payload size: " + totalLength);
            if (chunkCount != PersistentSchematicNetwork.chunkCount(totalLength))
                throw new IllegalArgumentException("Invalid schematic chunk count");
            if (chunkIndex < 0 || chunkIndex >= chunkCount)
                throw new IllegalArgumentException("Invalid schematic chunk index: " + chunkIndex);
            if (length < 0 || length > PersistentSchematicNetwork.CHUNK_BYTES)
                throw new IllegalArgumentException("Invalid schematic chunk size");
            if (chunkIndex < chunkCount - 1 && length != PersistentSchematicNetwork.CHUNK_BYTES)
                throw new IllegalArgumentException("Invalid non-final schematic chunk size");
            if (chunkIndex == chunkCount - 1
                && length != totalLength - PersistentSchematicNetwork.CHUNK_BYTES * (chunkCount - 1))
                throw new IllegalArgumentException("Invalid final schematic chunk size");
        }

        @Override
        protected abstract SchematicChunkPacket newPacket();
    }

    static class SaveDataPacket extends SchematicChunkPacket {

        int blocks;

        @Override
        public byte getPacketID() {
            return PersistentSchematicNetwork.PACKET_SAVE_DATA;
        }

        @Override
        public void encode(ByteBuf buffer) {
            super.encode(buffer);
            buffer.writeInt(blocks);
        }

        @Override
        public MMPacket decode(ByteArrayDataInput buffer) {
            SaveDataPacket packet = (SaveDataPacket) super.decode(buffer);
            packet.blocks = buffer.readInt();
            return packet;
        }

        @Override
        protected SchematicChunkPacket newPacket() {
            return new SaveDataPacket();
        }

        @Override
        protected void handle(EntityPlayer player, MMState state) {}

        @Override
        public void process(IBlockAccess world) {
            EntityPlayer player = MMMod.proxy.getThePlayer();
            if (player == null) return;

            try {
                ChunkTransfer transfer;
                byte[] schematicBytes;

                synchronized (PersistentSchematicNetwork.incomingSaveTransfers) {
                    transfer = PersistentSchematicNetwork.incomingSaveTransfers
                        .computeIfAbsent(transferId(), key -> new ChunkTransfer(totalLength, chunkCount));
                    if (!transfer.matches(totalLength, chunkCount))
                        throw new IllegalArgumentException("Persistent schematic transfer metadata changed");
                    transfer.add(chunkIndex, bytes);
                    if (!transfer.isComplete()) return;
                    PersistentSchematicNetwork.incomingSaveTransfers.remove(transferId());
                    schematicBytes = transfer.combine();
                }

                String schematicFileName = fileName;
                int schematicBlocks = blocks;
                PersistentSchematicNetwork.ioExecutor.execute(() -> {
                    try {
                        PersistentSchematic.saveBytes(schematicFileName, schematicBytes);
                        PersistentSchematicNetwork.runOnClientThread(() -> {
                            EntityPlayer currentPlayer = MMMod.proxy.getThePlayer();
                            if (currentPlayer != null) {
                                PersistentSchematic.sendSaveResult(currentPlayer, schematicFileName, schematicBlocks);
                            }
                        });
                    } catch (Exception e) {
                        DragonFix.LOG.warn("Could not save Matter Manipulator schematic on client", e);
                        PersistentSchematicNetwork.runOnClientThread(() -> {
                            EntityPlayer currentPlayer = MMMod.proxy.getThePlayer();
                            if (currentPlayer != null) {
                                PersistentSchematic.sendError(
                                    currentPlayer,
                                    "Could not save Matter Manipulator schematic: " + e.getMessage());
                            }
                        });
                    }
                });
            } catch (Exception e) {
                DragonFix.LOG.warn("Could not save Matter Manipulator schematic on client", e);
                MMUtils.sendErrorToPlayer(player, "Could not save Matter Manipulator schematic: " + e.getMessage());
            }
        }
    }
}
