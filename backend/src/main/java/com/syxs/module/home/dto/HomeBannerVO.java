package com.syxs.module.home.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeBannerVO {

    private String id;
    private Long goodsId;
    private String imageUrl;
    private String title;
    private String subtitle;
}
