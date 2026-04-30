package com.dragonfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = DragonFix.MODID, version = Tags.VERSION, name = DragonFix.NAME, acceptedMinecraftVersions = "[1.7.10]")
public class DragonFix {

    public static final String MODID = "dragonfix";
    public static final String NAME = "DragonFix";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("{} loaded at version {}", NAME, Tags.VERSION);
    }
}
