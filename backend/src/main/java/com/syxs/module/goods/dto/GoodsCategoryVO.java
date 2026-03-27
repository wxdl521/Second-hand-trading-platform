package com.syxs.module.goods.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoodsCategoryVO {

    private String name;
    private Long count;
}
