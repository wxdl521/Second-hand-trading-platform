package com.syxs.module.home.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeLandingVO {

    private List<HomeBannerVO> banners;
    private List<HomeCategoryCardVO> categories;
    private Long goodsCount;
    private Long orderCount;
    private Integer carbonSavedKg;
    private Integer todayCarbonKg;
}
