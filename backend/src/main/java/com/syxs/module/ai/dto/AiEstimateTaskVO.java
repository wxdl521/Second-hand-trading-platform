package com.syxs.module.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiEstimateTaskVO {

    private Long id;
    private Long goodsId;
    private String status;
    private String provider;
    private BigDecimal estimatePrice;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Integer confidence;
    private Integer carbonSavedKg;
    private String summary;
    private String rawResponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
