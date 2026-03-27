package com.syxs.module.admin.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminTrendPointDTO {

    private String label;
    private BigDecimal gmv;
    private Long orderCount;
}
