package com.dragonfix.mattermanipulator.persistent;

import java.util.UUID;

import org.joml.Vector3i;

import com.google.gson.annotations.SerializedName;
import com.recursive_pineapple.matter_manipulator.common.items.manipulator.Location;

public final class PersistentSchematicConfigData {

    @SerializedName("mode")
    public PersistentSchematicMode mode = PersistentSchematicMode.NONE;

    @SerializedName("file")
    public String file = "";

    @SerializedName("id")
    public UUID id;

    @SerializedName("pasteFile")
    public String pasteFile = "";

    @SerializedName("pasteId")
    public UUID pasteId;

    @SerializedName("pasteRestore")
    public int pasteRestore;

    @SerializedName("pasteRestoreStartedMs")
    public long pasteRestoreStartedMs;

    @SerializedName("normalA")
    public Location normalCoordA;

    @SerializedName("normalB")
    public Location normalCoordB;

    @SerializedName("normalC")
    public Location normalCoordC;

    @SerializedName("normalArray")
    public Vector3i normalArraySpan;

    @SerializedName("persistentCopyA")
    public Location persistentCopyA;

    @SerializedName("persistentCopyB")
    public Location persistentCopyB;

    @SerializedName("persistentPaste")
    public Location persistentPaste;

    @SerializedName("persistentArray")
    public Vector3i persistentArraySpan;
}
