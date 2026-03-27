package com.syxs.module.ai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "ai_estimate")
public class AiEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
