package com.syxs.module.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class AdminGoodsUpsertRequest {

    private String title;
    private String category;
    private String brand;
    private String condition;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer carbonSavedKg;
    private BigDecimal aiPrice;
    private String city;
    private String sellerName;
    private String sellerLevel;
    private String story;
    private String description;
    private String status;
    private List<String> tags;
    private String heroImage;
    private List<String> gallery;
    private String createdAt;
    private String auditStatus;
    private String reviewNote;
    private String transferType;
    private Integer viewCount;
    private Integer favorCount;
    private Boolean mockCertified;
}
