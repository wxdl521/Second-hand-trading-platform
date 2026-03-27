package com.syxs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "syxs.jwt")
public class JwtConfig {

    private String secret = "syxs-local-dev-secret-key-syxs-local-dev-secret-key";
    private long accessExpiry = 86400L;
    private long refreshExpiry = 604800L;
}
