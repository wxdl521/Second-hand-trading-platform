package com.syxs.module.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminChartItemDTO {

    private String label;
    private Long value;
}
