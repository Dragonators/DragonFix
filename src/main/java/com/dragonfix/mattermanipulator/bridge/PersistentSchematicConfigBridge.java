package com.dragonfix.mattermanipulator.bridge;

import java.util.UUID;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;

public interface PersistentSchematicConfigBridge {

    PersistentSchematicMode dragonfix$getPersistentSchematicMode();

    void dragonfix$setPersistentSchematicMode(PersistentSchematicMode mode);

    String dragonfix$getPersistentSchematicFile();

    void dragonfix$setPersistentSchematicFile(String fileName);

    UUID dragonfix$getPersistentSchematicId();

    void dragonfix$setPersistentSchematicId(UUID id);

    void dragonfix$capturePersistentSchematic(PersistentSchematicMode mode);

    void dragonfix$activatePersistentSchematic(PersistentSchematicMode mode, String fileName, UUID id);

    void dragonfix$captureNormalSelection();

    void dragonfix$capturePersistentSelection(PersistentSchematicMode mode);

    void dragonfix$syncPersistentCopyFromNormalSelection();

    void dragonfix$activateNormalSelection(boolean syncPersistentCopy);

    void dragonfix$activatePersistentSelection(PersistentSchematicMode mode);

    void dragonfix$resetPersistentPasteSelection();

    void dragonfix$resetPersistentPasteSchematic();

    void dragonfix$clearStoredPersistentPasteSession();

    default boolean dragonfix$isPersistentSchematicCopy() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.COPY;
    }

    default boolean dragonfix$isPersistentSchematicPaste() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.PASTE;
    }
}
