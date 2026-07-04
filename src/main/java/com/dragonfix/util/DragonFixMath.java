package com.dragonfix.util;

public final class DragonFixMath {

    private DragonFixMath() {}

    public static int ceilLog(double value, double factor) {
        if (value <= 1.0D || factor <= 1.0D) return 0;
        return Math.max(0, (int) Math.ceil(Math.log(value) / Math.log(factor)));
    }
}
