package com.dragonfix.mattermanipulator.bridge;

import java.util.UUID;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;

public interface PersistentSchematicConfigBridge {

    int RESTORE_NONE = 0;
    int RESTORE_PENDING = 1;
    int RESTORE_RESTORING = 2;
    int RESTORE_READY = 3;

    PersistentSchematicMode dragonfix$getPersistentSchematicMode();

    void dragonfix$setPersistentSchematicMode(PersistentSchematicMode mode);

    String dragonfix$getPersistentSchematicFile();

    void dragonfix$setPersistentSchematicFile(String fileName);

    UUID dragonfix$getPersistentSchematicId();

    void dragonfix$setPersistentSchematicId(UUID id);

    int dragonfix$getPersistentPasteRestoreState();

    void dragonfix$setPersistentPasteRestoreState(int state);

    long dragonfix$getPersistentPasteRestoreStartedMs();

    void dragonfix$setPersistentPasteRestoreStartedMs(long startedMs);

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

    boolean dragonfix$refreshPersistentPasteSchematic(String fileName, UUID id);

    default boolean dragonfix$isPersistentSchematicCopy() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.COPY;
    }

    default boolean dragonfix$isPersistentSchematicPaste() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.PASTE;
    }
}
