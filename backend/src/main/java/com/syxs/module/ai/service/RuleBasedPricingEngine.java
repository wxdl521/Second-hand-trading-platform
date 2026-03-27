package com.syxs.module.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedPricingEngine {

    public BigDecimal estimate(String category, int yearsUsed, int rarity) {
        BigDecimal base = switch (category) {
            case "珠宝腕表" -> new BigDecimal("42000");
            case "数码设备" -> new BigDecimal("22000");
            case "艺术收藏" -> new BigDecimal("12000");
            case "家居好物" -> new BigDecimal("9000");
            default -> new BigDecimal("26000");
        };

        BigDecimal yearFactor = BigDecimal.valueOf(Math.max(0.72, 1 - yearsUsed * 0.05));
        BigDecimal rarityFactor = BigDecimal.valueOf(0.88 + rarity * 0.06);
        return base.multiply(yearFactor).multiply(rarityFactor).setScale(0, RoundingMode.HALF_UP);
    }
}
