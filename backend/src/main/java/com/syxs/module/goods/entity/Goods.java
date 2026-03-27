package com.syxs.module.goods.entity;

import com.syxs.common.enums.GoodsStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "goods")
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String category;

    private String brand;

    private String conditionLevel;

    private BigDecimal salePrice;

    private BigDecimal originalPrice;

    private BigDecimal aiPrice;

    private Integer carbonSavedKg;

    private String city;

    private String sellerName;

    private String sellerPhone;

    private String sellerLevel;

    private String coverUrl;

    private String story;

    private String tags;

    private String description;

    @Enumerated(EnumType.STRING)
    private GoodsStatus status;

    private String auditStatus;

    private String reviewNote;

    private String transferType;

    private Integer viewCount;

    private Integer favorCount;

    private Boolean mockCertified;

    private LocalDateTime createdAt;
}
