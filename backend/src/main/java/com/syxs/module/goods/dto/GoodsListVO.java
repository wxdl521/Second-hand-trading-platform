package com.syxs.module.goods.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoodsListVO {

    private Long id;
    private String title;
    private String category;
    private String brand;
    private String conditionLevel;
    private String coverUrl;
    private BigDecimal salePrice;
    private BigDecimal aiPrice;
    private Integer carbonSavedKg;
    private String story;
    private String sellerName;
    private String sellerLevel;
    private String city;
    private String status;
    private String auditStatus;
    private String reviewNote;
    private Integer favorCount;
    private Boolean mockCertified;
    private LocalDateTime createdAt;
}
