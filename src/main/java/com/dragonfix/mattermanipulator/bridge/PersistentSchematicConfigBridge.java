package com.dragonfix.mattermanipulator.bridge;

import com.dragonfix.mattermanipulator.persistent.PersistentSchematicMode;

public interface PersistentSchematicConfigBridge {

    PersistentSchematicMode dragonfix$getPersistentSchematicMode();

    void dragonfix$setPersistentSchematicMode(PersistentSchematicMode mode);

    String dragonfix$getPersistentSchematicFile();

    void dragonfix$setPersistentSchematicFile(String fileName);

    String dragonfix$getPersistentSchematicId();

    void dragonfix$setPersistentSchematicId(String id);

    default boolean dragonfix$isPersistentSchematicCopy() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.COPY;
    }

    default boolean dragonfix$isPersistentSchematicPaste() {
        return dragonfix$getPersistentSchematicMode() == PersistentSchematicMode.PASTE;
    }
}
