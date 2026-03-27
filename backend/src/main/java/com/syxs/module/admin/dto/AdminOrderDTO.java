package com.syxs.module.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminOrderDTO {

    private Long id;
    private Long goodsId;
    private String buyerName;
    private java.math.BigDecimal amount;
    private String status;
    private String createdAt;
}
