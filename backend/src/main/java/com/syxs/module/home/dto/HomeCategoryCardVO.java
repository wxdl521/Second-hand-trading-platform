package com.syxs.module.home.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeCategoryCardVO {

    private String name;
    private Long count;
    private String sample;
}
