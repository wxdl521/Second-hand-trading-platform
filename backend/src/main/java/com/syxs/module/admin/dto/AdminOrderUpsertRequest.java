package com.syxs.module.admin.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminOrderUpsertRequest {

    private Long goodsId;
    private String buyerName;
    private BigDecimal amount;
    private String status;
    private String createdAt;
}
