package com.syxs.module.goods.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoodsDetailVO {

    private Long id;
    private String title;
    private String category;
    private String brand;
    private String conditionLevel;
    private BigDecimal salePrice;
    private BigDecimal originalPrice;
    private BigDecimal aiPrice;
    private String description;
    private String story;
    private String sellerName;
    private String sellerLevel;
    private String city;
    private String status;
    private String auditStatus;
    private String reviewNote;
    private Integer favorCount;
    private Boolean mockCertified;
    private List<String> tags;
    private List<String> gallery;
}
