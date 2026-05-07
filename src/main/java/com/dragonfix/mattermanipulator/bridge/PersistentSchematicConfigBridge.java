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

    default boolean dragonfix$isPersistentSchematicCopy() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.COPY;
    }

    default boolean dragonfix$isPersistentSchematicPaste() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.PASTE;
    }
}
