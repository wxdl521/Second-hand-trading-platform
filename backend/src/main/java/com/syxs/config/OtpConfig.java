package com.syxs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "syxs.otp")
public class OtpConfig {

    private String mode = "console";
    private int expireMinutes = 5;
}
