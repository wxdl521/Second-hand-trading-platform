package com.syxs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "syxs.carbon")
public class CarbonConfig {

    private double pointsPerYuan = 0.05D;

    private double co2PerYuan = 0.0008D;
}
