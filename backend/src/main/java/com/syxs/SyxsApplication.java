package com.syxs;

import com.syxs.config.CarbonConfig;
import com.syxs.config.FileStorageConfig;
import com.syxs.config.JwtConfig;
import com.syxs.config.OtpConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = {JwtConfig.class, FileStorageConfig.class, OtpConfig.class, CarbonConfig.class})
public class SyxsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyxsApplication.class, args);
    }
}
