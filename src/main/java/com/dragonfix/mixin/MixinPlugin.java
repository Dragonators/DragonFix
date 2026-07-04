package com.dragonfix.mixin;

import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String MYCTMLIB_MARKER_CLASS = "com.github.wohaopa.MyCTMLib.Textures";
    private static final String MYCTMLIB_MIXIN_PACKAGE = "com.dragonfix.mixin.mixins.myctmlib.";
    private static final String AE2THINGS_MARKER_CLASS = "com.asdflj.ae2thing.util.GTUtil";
    private static final String AE2THINGS_MIXIN_PACKAGE = "com.dragonfix.mixin.mixins.ae2things.";
    private static final String ANGELICA_MARKER_CLASS = "com.gtnewhorizons.angelica.AngelicaMod";
    private static final String ANGELICA_MIXIN_PACKAGE = "com.dragonfix.mixin.mixins.angelica.";
    private static final String TST_MARKER_CLASS = "com.Nxer.TwistSpaceTechnology.TwistSpaceTechnology";
    private static final String TST_MIXIN_PACKAGE = "com.dragonfix.mixin.mixins.tst.";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(MYCTMLIB_MIXIN_PACKAGE)) {
            return isClassPresent(MYCTMLIB_MARKER_CLASS);
        }
        if (mixinClassName.startsWith(AE2THINGS_MIXIN_PACKAGE)) {
            return isClassPresent(AE2THINGS_MARKER_CLASS);
        }
        if (mixinClassName.startsWith(ANGELICA_MIXIN_PACKAGE)) {
            return isClassPresent(ANGELICA_MARKER_CLASS);
        }
        if (mixinClassName.startsWith(TST_MIXIN_PACKAGE)) {
            return isClassPresent(TST_MARKER_CLASS);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, Launch.classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
