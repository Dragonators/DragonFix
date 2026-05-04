package com.dragonfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = DragonFix.MODID,
    version = Tags.VERSION,
    name = DragonFix.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = DragonFix.DEPENDENCIES)
public class DragonFix {

    public static final String MODID = "dragonfix";
    public static final String NAME = "DragonFix";
    public static final String DEPENDENCIES = "required-after:OpenComputers@[1.11.20-GTNH,);"
        + "required-after:GalaxySpace@[1.1.121-GTNH,);"
        + "required-after:gtnhlib@[0.9.47,);"
        + "after:ae2thing@[v1.2.14,);"
        + "after:programmablehatches@[0.1.3p55,);"
        + "after:MyCTMLib@[v1.2.5_28x,)";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("{} loaded at version {}", NAME, Tags.VERSION);
    }
}
