package com.dragonfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dragonfix.mattermanipulator.persistent.network.PersistentSchematicNetwork;

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
        + "required-after:matter-manipulator@[0.0.51-GTNH];"
        + "required-after:ForgeMultipart@[1.7.2,);"
        + "required-after:CarpentersBlocks@[3.7.0-GTNH,);"
        + "required-after:appliedenergistics2@[rv3-beta-690-GTNH,);"
        + "required-after:EnderIO@[2.9.28,);"
        + "required-after:littletiles@[1.5.14,);"
        + "required-after:avaritiaddons@[1.9.3-GTNH,);"
        + "required-after:ArchitectureCraft@[1.11.6,);"
        + "after:ae2thing@[v1.2.14,);"
        + "required-after:ae2fc@[1.4.120-gtnh,);"
        + "required-after:gregtech_nh@[5.09.51.482,);"
        + "required-after:BiomesOPlenty@[2.1.0,);"
        + "after:malisisdoors@[1.18.2-GTNH,);"
        + "after:programmablehatches@[0.1.3p55,);"
        + "after:angelica@[2.1.30,);"
        + "after:MyCTMLib@[v1.2.5_28x,);"
        + "after:TwistSpaceTechnology@[0.7.15,)";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PersistentSchematicNetwork.init();
        LOG.info("{} loaded at version {}", NAME, Tags.VERSION);
    }
}
