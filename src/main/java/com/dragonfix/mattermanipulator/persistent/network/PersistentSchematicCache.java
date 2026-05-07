package com.dragonfix.mattermanipulator.persistent.network;

import java.util.UUID;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematic;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

final class PersistentSchematicCache {

    private static final int MAX_PER_OWNER = 8;
    private static final long TTL_MS = 15L * 60L * 1000L;
    // TODO: Adjust per-owner count and TTL based on uploaded schematic byte size to avoid server OOM from large files.

    private final Object2ObjectMap<UUID, PersistentSchematic> schematics = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<UUID, Object2LongOpenHashMap<UUID>> owners = new Object2ObjectOpenHashMap<>();

    synchronized PersistentSchematic get(UUID id) {
        if (id == null) return null;

        PersistentSchematic schematic = schematics.get(id);

        if (schematic != null) {
            touchOwners(id, System.currentTimeMillis());
        }

        return schematic;
    }

    synchronized PersistentSchematic get(UUID id, UUID ownerId) {
        if (id == null) return null;

        PersistentSchematic schematic = schematics.get(id);

        if (schematic != null) {
            long now = System.currentTimeMillis();
            touchOwner(id, ownerId, now);
            cleanup(now, ownerId);
        }

        return schematic;
    }

    synchronized void put(UUID id, UUID ownerId, PersistentSchematic schematic) {
        long now = System.currentTimeMillis();
        schematics.put(id, schematic);
        touchOwner(id, ownerId, now);
        cleanup(now, ownerId);
    }

    synchronized void cleanup(long now) {
        for (var ownerIt = it.unimi.dsi.fastutil.objects.Object2ObjectMaps.fastIterator(owners); ownerIt.hasNext();) {
            Object2ObjectMap.Entry<UUID, Object2LongOpenHashMap<UUID>> ownerEntry = ownerIt.next();
            Object2LongOpenHashMap<UUID> ownerSchematics = ownerEntry.getValue();

            for (var schematicIt = it.unimi.dsi.fastutil.objects.Object2LongMaps
                .fastIterator(ownerSchematics); schematicIt.hasNext();) {
                var schematicEntry = schematicIt.next();

                if (now - schematicEntry.getLongValue() > TTL_MS || !schematics.containsKey(schematicEntry.getKey())) {
                    schematicIt.remove();
                }
            }

            if (ownerSchematics.isEmpty()) {
                ownerIt.remove();
            }
        }

        for (var schematicIt = it.unimi.dsi.fastutil.objects.Object2ObjectMaps.fastIterator(schematics); schematicIt
            .hasNext();) {
            if (!hasOwner(
                schematicIt.next()
                    .getKey())) {
                schematicIt.remove();
            }
        }
    }

    private void cleanup(long now, UUID ownerId) {
        cleanup(now);
        trimOwner(ownerId);
    }

    private void touchOwner(UUID id, UUID ownerId, long now) {
        if (ownerId == null) return;

        Object2LongOpenHashMap<UUID> ownerSchematics = owners.get(ownerId);

        if (ownerSchematics == null) {
            ownerSchematics = new Object2LongOpenHashMap<>();
            owners.put(ownerId, ownerSchematics);
        }

        ownerSchematics.put(id, now);
    }

    private void touchOwners(UUID id, long now) {
        for (var ownerEntry : it.unimi.dsi.fastutil.objects.Object2ObjectMaps.fastIterable(owners)) {
            Object2LongOpenHashMap<UUID> ownerSchematics = ownerEntry.getValue();

            if (ownerSchematics.containsKey(id)) {
                ownerSchematics.put(id, now);
            }
        }
    }

    private void trimOwner(UUID ownerId) {
        Object2LongOpenHashMap<UUID> ownerSchematics = owners.get(ownerId);

        if (ownerSchematics == null) return;

        while (ownerSchematics.size() > MAX_PER_OWNER) {
            UUID oldest = null;
            long oldestAccess = Long.MAX_VALUE;

            for (var schematicIt = it.unimi.dsi.fastutil.objects.Object2LongMaps
                .fastIterator(ownerSchematics); schematicIt.hasNext();) {
                var entry = schematicIt.next();

                if (entry.getLongValue() < oldestAccess) {
                    oldestAccess = entry.getLongValue();
                    oldest = entry.getKey();
                }
            }

            if (oldest == null) return;
            ownerSchematics.removeLong(oldest);

            if (!hasOwner(oldest)) {
                schematics.remove(oldest);
            }
        }
    }

    private boolean hasOwner(UUID id) {
        for (var ownerEntry : it.unimi.dsi.fastutil.objects.Object2ObjectMaps.fastIterable(owners)) {
            if (ownerEntry.getValue()
                .containsKey(id)) {
                return true;
            }
        }

        return false;
    }
}
