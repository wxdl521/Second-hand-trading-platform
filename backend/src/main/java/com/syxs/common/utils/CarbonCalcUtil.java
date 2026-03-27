package com.syxs.common.utils;

public final class CarbonCalcUtil {

    private CarbonCalcUtil() {
    }

    public static int estimateCarbonSaved(double amount) {
        return Math.max(1, (int) Math.round(amount / 500));
    }
}
