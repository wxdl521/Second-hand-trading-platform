package com.syxs.module.admin.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCategoryDTO {

    private Long id;
    private String name;
    private String description;
    private List<String> featuredBrands;
    private String coverImage;
}
